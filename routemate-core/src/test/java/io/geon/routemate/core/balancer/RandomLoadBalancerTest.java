package io.geon.routemate.core.balancer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RandomLoadBalancerTest {

    @Test
    @DisplayName("Should return null for empty or null list")
    void testEmptyList() {
        RandomLoadBalancer balancer = new RandomLoadBalancer();
        assertNull(balancer.select(null));
        assertNull(balancer.select(Collections.emptyList()));
    }

    @Test
    @DisplayName("Should return valid key from list")
    void testSelect() {
        RandomLoadBalancer balancer = new RandomLoadBalancer();
        List<String> keys = Arrays.asList("read-1", "read-2");

        String selected = balancer.select(keys);
        assertTrue(keys.contains(selected));
    }

    @Test
    @DisplayName("Should eventually select different keys")
    void testRandomness() {
        RandomLoadBalancer balancer = new RandomLoadBalancer();
        List<String> keys = Arrays.asList("read-1", "read-2", "read-3");
        Set<String> selectedKeys = new HashSet<>();

        for (int i = 0; i < 100; i++) {
            selectedKeys.add(balancer.select(keys));
        }

        // Probability of picking only 1 key out of 3 in 100 tries is astronomically
        // low.
        // So we expect at least 2, likely 3.
        assertTrue(selectedKeys.size() > 1);
    }
}
