package io.geon.routemate.core.routing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Deque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoutingContextTest {

    @AfterEach
    void tearDown() {
        // Ensure cleanup between tests
        while (RoutingContext.get() != null) {
            RoutingContext.clear();
        }
    }

    @Test
    @DisplayName("Should push and pop context correctly")
    void testPushPop() {
        RoutingContext.set("A");
        assertThat(RoutingContext.get()).isEqualTo("A");

        RoutingContext.set("B");
        assertThat(RoutingContext.get()).isEqualTo("B");

        RoutingContext.clear();
        assertThat(RoutingContext.get()).isEqualTo("A");

        RoutingContext.clear();
        assertThat(RoutingContext.get()).isNull();
    }

    @Test
    @DisplayName("Should validate input keys")
    void testValidation() {
        assertThatThrownBy(() -> RoutingContext.set(null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> RoutingContext.set(""))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> RoutingContext.set("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should support try-with-resources via use()")
    void testTryWithResources() {
        try (RoutingContext.ContextToken token = RoutingContext.use("OUTER")) {
            assertThat(RoutingContext.get()).isEqualTo("OUTER");

            try (RoutingContext.ContextToken inner = RoutingContext.use("INNER")) {
                assertThat(RoutingContext.get()).isEqualTo("INNER");
            }

            assertThat(RoutingContext.get()).isEqualTo("OUTER");
        }

        assertThat(RoutingContext.get()).isNull();
    }

    @Test
    @DisplayName("Should provide snapshot of stack")
    void testSnapshot() {
        RoutingContext.set("A");
        RoutingContext.set("B");

        Deque<String> snapshot = RoutingContext.getStackSnapshot();
        assertThat(snapshot).containsExactly("B", "A");

        // Modifying snapshot should not affect actual context
        snapshot.clear();
        assertThat(RoutingContext.get()).isEqualTo("B");
    }
}
