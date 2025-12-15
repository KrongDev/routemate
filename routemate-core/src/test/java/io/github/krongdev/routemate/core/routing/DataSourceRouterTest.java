package io.github.krongdev.routemate.core.routing;

import io.github.krongdev.routemate.core.balancer.RoundRobinLoadBalancer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DataSourceRouterTest {

    private DataSourceRouter router;
    private DataSource writeDataSource;
    private DataSource readDataSource1;
    private DataSource readDataSource2;

    @AfterEach
    void tearDown() {
        RoutingContext.clear();
    }

    @BeforeEach
    void setUp() {
        writeDataSource = mock(DataSource.class);
        readDataSource1 = mock(DataSource.class);
        readDataSource2 = mock(DataSource.class);

        Map<String, DataSource> readDataSources = new HashMap<>();
        readDataSources.put("read1", readDataSource1);
        readDataSources.put("read2", readDataSource2);

        // Constructor Injection
        router = new DataSourceRouter(writeDataSource, new RoundRobinLoadBalancer());
        router.setReadDataSources(readDataSources);
    }

    @Test
    void testWriteRouting() {
        RoutingContext.clear();
        Object key = router.determineCurrentLookupKey();
        assertEquals("WRITE", key);
    }

    @Test
    void testReadRouting() {
        RoutingContext.set(RoutingContext.READ);
        Object key = router.determineCurrentLookupKey();
        assertTrue(key.equals("read1") || key.equals("read2"));
    }

    @Test
    void testFallbackToWrite() {
        // No Read Data Sources
        DataSourceRouter emptyRouter = new DataSourceRouter(writeDataSource, new RoundRobinLoadBalancer());
        RoutingContext.set(RoutingContext.READ);
        Object key = emptyRouter.determineCurrentLookupKey();
        assertEquals("WRITE", key);
    }
}
