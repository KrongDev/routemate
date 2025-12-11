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

        int index = counter.getAndIncrement() % keys.size();
        return keys.get(Math.abs(index));
    }
}
