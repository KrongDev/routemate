package io.github.krongdev.routemate.core.balancer;

import java.util.List;
import java.util.Map;

public interface LoadBalancer {
    /**
     * Select a data source key from the list of healthy candidates.
     * 
     * @param keys List of available healthy Read Replica keys.
     * @return Selected key, or null if list is empty.
     */
    String select(List<String> keys);

    /**
     * Update the weights configuration for the load balancer.
     * 
     * @param weights map of datasource keys to their weights
     */
    default void updateWeights(Map<String, Integer> weights) {
        // Default no-op
    }
}
