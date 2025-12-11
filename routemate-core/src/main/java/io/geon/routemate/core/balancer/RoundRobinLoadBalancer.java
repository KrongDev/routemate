package io.geon.routemate.core.balancer;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinLoadBalancer implements LoadBalancer {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public String select(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return null;
        }
        int size = keys.size();
        int idx = counter.getAndIncrement();

        if (idx > 1_000_000_000) {
            counter.set(0);
            idx = 0;
        }

        if (idx < 0) {
            idx = Math.abs(idx);
        }

        return keys.get(idx % size);
    }
}
