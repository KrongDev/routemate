package io.geon.routemate.core.balancer;

import java.util.List;

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
    default void updateWeights(java.util.Map<String, Integer> weights) {
        // Default no-op
    }
}
