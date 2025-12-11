package io.geon.routemate.core.balancer;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RandomLoadBalancer implements LoadBalancer {

    @Override
    public String select(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return null;
        }

        int index = ThreadLocalRandom.current().nextInt(keys.size());
        return keys.get(index);
    }
}
