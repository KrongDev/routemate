package io.geon.routemate.core.routing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DataSourceRouterTest {

    @AfterEach
    void tearDown() {
        RoutingContext.clear();
    }

    @Test
    void shouldReturnNullWhenContextIsEmpty() {
        DataSourceRouter router = new DataSourceRouter();
        assertThat(router.determineCurrentLookupKey()).isNull();
    }

    @Test
    void shouldReturnContextValue() {
        DataSourceRouter router = new DataSourceRouter();
        router.setReadDataSourceKeys(List.of("READ-1", "READ-2"));

        RoutingContext.set("MASTER");
        assertThat(router.determineCurrentLookupKey()).isEqualTo("MASTER");

        RoutingContext.set(RoutingContext.READ);
        assertThat(router.determineCurrentLookupKey()).isEqualTo("READ-1");
        assertThat(router.determineCurrentLookupKey()).isEqualTo("READ-2"); // Round-robin
        assertThat(router.determineCurrentLookupKey()).isEqualTo("READ-1");

        RoutingContext.clear();
        assertThat(router.determineCurrentLookupKey()).isEqualTo("MASTER");
    }

    @Test
    void shouldFallbackWhenNoReadReplicas() {
        DataSourceRouter router = new DataSourceRouter();
        // No read keys set

        RoutingContext.set(RoutingContext.READ);
        assertThat(router.determineCurrentLookupKey()).isNull(); // Fallback to default
    }
}
