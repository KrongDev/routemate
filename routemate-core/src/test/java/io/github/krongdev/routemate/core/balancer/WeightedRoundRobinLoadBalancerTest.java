package io.github.krongdev.routemate.core.balancer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WeightedRoundRobinLoadBalancerTest {

    @Test
    @DisplayName("Should respect weights in selection frequency")
    void testWeightedFreq() {
        Map<String, Integer> weights = new HashMap<>();
        weights.put("A", 3);
        weights.put("B", 1);

        WeightedRoundRobinLoadBalancer lb = new WeightedRoundRobinLoadBalancer(weights);
        List<String> keys = Arrays.asList("A", "B");

        Map<String, Integer> counts = new HashMap<>();
        counts.put("A", 0);
        counts.put("B", 0);

        // Cycle 4 times (total weight 4)
        for (int i = 0; i < 4; i++) {
            String selected = lb.select(keys);
            counts.put(selected, counts.get(selected) + 1);
        }

        // Should be exactly 3 As and 1 B
        assertEquals(3, counts.get("A"));
        assertEquals(1, counts.get("B"));
    }

    @Test
    @DisplayName("Should handle missing healthy keys gracefully")
    void testMissingHealthyKey() {
        Map<String, Integer> weights = new HashMap<>();
        weights.put("A", 3);
        weights.put("B", 1);

        WeightedRoundRobinLoadBalancer lb = new WeightedRoundRobinLoadBalancer(weights);

        // Only B is healthy
        List<String> keys = Arrays.asList("B");

        // Should always select B
        for (int i = 0; i < 10; i++) {
            assertEquals("B", lb.select(keys));
        }
    }

    @Test
    @DisplayName("Should default to weight 1 for invalid weights")
    void testInvalidWeights() {
        Map<String, Integer> weights = new HashMap<>();
        weights.put("A", 0); // Invalid, should be treated as 1
        weights.put("B", -5); // Invalid, should be treated as 1

        WeightedRoundRobinLoadBalancer lb = new WeightedRoundRobinLoadBalancer(weights);
        List<String> keys = Arrays.asList("A", "B");

        // Total weight effectively 2 (1 each)
        Map<String, Integer> counts = new HashMap<>();
        counts.put("A", 0);
        counts.put("B", 0);

        for (int i = 0; i < 2; i++) {
            String selected = lb.select(keys);
            counts.put(selected, counts.getOrDefault(selected, 0) + 1);
        }

        assertEquals(1, counts.get("A"));
        assertEquals(1, counts.get("B"));
    }

    @Test
    @DisplayName("Should detect dynamic weight updates")
    void testDynamicUpdates() {
        Map<String, Integer> weights = new HashMap<>();
        weights.put("A", 1);
        weights.put("B", 1);

        WeightedRoundRobinLoadBalancer lb = new WeightedRoundRobinLoadBalancer(weights);
        List<String> keys = Arrays.asList("A", "B");

        // Initial: 50/50
        // ... verified by testWeightedFreq

        // Dynamic Update: A=10, B=0 (should be 1)
        Map<String, Integer> newWeights = new HashMap<>();
        newWeights.put("A", 9);
        newWeights.put("B", 1);

        lb.updateWeights(newWeights);

        // Cycle 10 times. A(9), B(1) -> Total 10.
        // Expect 9 A's and 1 B.
        Map<String, Integer> counts = new HashMap<>();
        counts.put("A", 0);
        counts.put("B", 0);

        for (int i = 0; i < 10; i++) {
            String selected = lb.select(keys);
            counts.put(selected, counts.get(selected) + 1);
        }

        assertEquals(9, counts.get("A"));
        assertEquals(1, counts.get("B"));
    }
}
