package io.geon.routemate.core.balancer;

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
                if (weight < 1)
                    weight = 1; // Minimum weight 1
                for (int i = 0; i < weight; i++) {
                    builtList.add(key);
                }
            });
        }
        this.distributionListRef.set(Collections.unmodifiableList(builtList));
    }

    @Override
    public String select(List<String> healthyKeys) {
        if (healthyKeys == null || healthyKeys.isEmpty()) {
            return null;
        }

        List<String> distributionList = distributionListRef.get();
        if (distributionList == null || distributionList.isEmpty()) {
            // Fallback to simple Random/RR if no distribution list available
            return healthyKeys.get(Math.abs(index.getAndIncrement() % healthyKeys.size()));
        }

        int size = distributionList.size();
        int startIndex = Math.abs(index.getAndIncrement() % size);

        // Loop at most 'size' times to find a healthy candidate in the distribution
        // list
        for (int i = 0; i < size; i++) {
            String candidate = distributionList.get((startIndex + i) % size);
            if (healthyKeys.contains(candidate)) {
                return candidate;
            }
        }

        // Fallback: if none of the keys in distribution list are healthy
        return healthyKeys.get(Math.abs(index.getAndIncrement() % healthyKeys.size()));
    }
}
