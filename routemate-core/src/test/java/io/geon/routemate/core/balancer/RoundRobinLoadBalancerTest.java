package io.geon.routemate.core.balancer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RoundRobinLoadBalancerTest {

    @Test
    @DisplayName("Should return null for empty or null list")
    void testEmptyList() {
        RoundRobinLoadBalancer balancer = new RoundRobinLoadBalancer();
        assertNull(balancer.select(null));
        assertNull(balancer.select(Collections.emptyList()));
    }

    @Test
    @DisplayName("Should cycle through keys round-robin")
    void testRoundRobin() {
        RoundRobinLoadBalancer balancer = new RoundRobinLoadBalancer();
        List<String> keys = Arrays.asList("read-1", "read-2", "read-3");

        assertEquals("read-1", balancer.select(keys)); // 0 % 3 = 0 -> read-1
        assertEquals("read-2", balancer.select(keys)); // 1 % 3 = 1 -> read-2
        assertEquals("read-3", balancer.select(keys)); // 2 % 3 = 2 -> read-3
        assertEquals("read-1", balancer.select(keys)); // 3 % 3 = 0 -> read-1

        RoundRobinLoadBalancer b2 = new RoundRobinLoadBalancer();
        assertEquals("read-1", b2.select(keys));
    }

    @Test
    @DisplayName("Should be thread-safe")
    void testThreadSafety() throws InterruptedException {
        RoundRobinLoadBalancer balancer = new RoundRobinLoadBalancer();
        List<String> keys = Arrays.asList("A", "B", "C");
        int threads = 100;
        int requestsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        AtomicInteger[] counts = new AtomicInteger[3];
        for (int i = 0; i < 3; i++)
            counts[i] = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    String selected = balancer.select(keys);
                    if ("A".equals(selected))
                        counts[0].incrementAndGet();
                    else if ("B".equals(selected))
                        counts[1].incrementAndGet();
                    else if ("C".equals(selected))
                        counts[2].incrementAndGet();
                }
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        int total = threads * requestsPerThread;

        int sum = counts[0].get() + counts[1].get() + counts[2].get();
        assertEquals(total, sum);

        // Check balance (approximate is also fine, but strict RR should be exact or off
        // by 1)
        assertTrue(Math.abs(counts[0].get() - counts[1].get()) <= 1);
        assertTrue(Math.abs(counts[1].get() - counts[2].get()) <= 1);
    }
}
