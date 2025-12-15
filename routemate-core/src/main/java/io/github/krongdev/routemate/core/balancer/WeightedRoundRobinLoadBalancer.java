package io.github.krongdev.routemate.core.balancer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Weighted Round-Robin Load Balancer.
 * Uses a pre-calculated distribution list where keys are repeated by their
 * weight.
 * e.g. A(2), B(1) -> [A, A, B]
 * Supports dynamic updates via AtomicReference.
 */
public class WeightedRoundRobinLoadBalancer implements LoadBalancer {

    private final AtomicReference<List<String>> distributionListRef = new AtomicReference<>();
    private final AtomicInteger index = new AtomicInteger(0);

    public WeightedRoundRobinLoadBalancer(Map<String, Integer> weights) {
        updateWeights(weights);
    }

    @Override
    public void updateWeights(Map<String, Integer> weights) {
        List<String> builtList = new ArrayList<>();
        if (weights != null) {
            weights.forEach((key, weight) -> {
                if (weight < 1) weight = 1;
                for (int i = 0; i < weight; i++) {
                    builtList.add(key);
                }
            });
        }

        this.distributionListRef.set(Collections.unmodifiableList(builtList));
        this.index.set(0); // reset on weight update
    }

    @Override
    public String select(List<String> healthyKeys) {
        if (healthyKeys == null || healthyKeys.isEmpty()) {
            return null;
        }

        // overflow protection
        int idx = index.getAndIncrement();
        if (idx > 1_000_000_000) {
            index.set(0);
            idx = 0;
        }
        idx = Math.abs(idx);

        List<String> distributionList = distributionListRef.get();
        if (distributionList == null || distributionList.isEmpty()) {
            return healthyKeys.get(idx % healthyKeys.size());
        }

        int size = distributionList.size();
        int startIndex = idx % size;

        for (int i = 0; i < size; i++) {
            String candidate = distributionList.get((startIndex + i) % size);
            if (healthyKeys.contains(candidate)) {
                return candidate;
            }
        }

        return healthyKeys.get(idx % healthyKeys.size());
    }
}
