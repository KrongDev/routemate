package io.geon.routemate.core.routing;

import lombok.Getter;
import lombok.Setter;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.geon.routemate.core.balancer.LoadBalancer;
import io.geon.routemate.core.balancer.RoundRobinLoadBalancer;

/**
 * Dynamic DataSource router.
 * Routes to the target DataSource based on the current RoutingContext.
 */
public class DataSourceRouter extends AbstractRoutingDataSource {

    private static final Logger log = LoggerFactory.getLogger(DataSourceRouter.class);

    // Safe to return COWAL
    @Getter
    private final List<String> readDataSourceKeys = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final Set<String> unhealthyKeys = ConcurrentHashMap.newKeySet();
    @Setter
    private LoadBalancer loadBalancer = new RoundRobinLoadBalancer(); // Default

    @Getter
    private final Map<Object, DataSource> dataSourceMap = new ConcurrentHashMap<>();

    @Override
    protected Object determineCurrentLookupKey() {
        String key = RoutingContext.get();
        if (RoutingContext.READ.equals(key)) {
            // Use snapshot for safety (CopyOnWriteArrayList iterator is safe)
            List<String> healthyKeys = new ArrayList<>();
            for (String k : readDataSourceKeys) {
                if (!unhealthyKeys.contains(k)) {
                    healthyKeys.add(k);
                }
            }

            if (healthyKeys.isEmpty()) {
                log.warn("No healthy read replicas available. Falling back to MASTER.");
                return null; // Fallback to default (Master)
            }

            // Delegate availability logic to LoadBalancer
            return loadBalancer.select(healthyKeys);
        }
        return key;
    }

    // Configuration helper to easily add data sources
    public void setTargetDataSourcesMap(Map<Object, Object> targetDataSources) {
        super.setTargetDataSources(targetDataSources);
        // Store explicit references for HealthChecker
        this.dataSourceMap.clear();
        targetDataSources.forEach((k, v) -> {
            if (v instanceof DataSource) {
                this.dataSourceMap.put(k, (DataSource) v);
            }
        });
    }

    public DataSource getDataSource(String key) {
        return dataSourceMap.get(key);
    }

    public void setReadDataSourceKeys(List<String> keys) {
        this.readDataSourceKeys.clear();
        if (keys != null) {
            this.readDataSourceKeys.addAll(keys);
        }
    }

    // Health Check Management
    public void markUnhealthy(String key) {
        if (unhealthyKeys.add(key)) {
            log.warn("Marking DataSource [{}] as UNHEALTHY", key);
        }
    }

    public void markHealthy(String key) {
        if (unhealthyKeys.remove(key)) {
            log.info("Marking DataSource [{}] as HEALTHY", key);
        }
    }

    // Dynamic Management
    private final Map<String, Integer> weights = new ConcurrentHashMap<>();

    public synchronized void addDataSource(String key, DataSource dataSource, int weight) {
        log.info("Adding DataSource [{}] with weight [{}]", key, weight);
        this.dataSourceMap.put(key, dataSource);
        this.readDataSourceKeys.add(key); // Assumes all added dynamic DBs are read replicas
        this.weights.put(key, weight);
        refreshRouting();
    }

    public synchronized void removeDataSource(String key) {
        // Protect MASTER
        if ("MASTER".equalsIgnoreCase(key)) {
            throw new IllegalArgumentException("MASTER DataSource cannot be removed dynamically");
        }

        log.info("Removing DataSource [{}]", key);
        this.dataSourceMap.remove(key);
        this.readDataSourceKeys.remove(key);
        this.weights.remove(key);
        this.unhealthyKeys.remove(key); // Cleanup
        refreshRouting();
    }

    private void refreshRouting() {
        // Create new targetDataSources map
        Map<Object, Object> newTargetDataSources = new HashMap<>(this.dataSourceMap);
        super.setTargetDataSources(newTargetDataSources);
        super.afterPropertiesSet(); // Trigger Spring internal refresh

        // Refresh LoadBalancer weights
        if (loadBalancer != null) {
            loadBalancer.updateWeights(new HashMap<>(this.weights));
        }
    }

    public void setWeights(Map<String, Integer> weights) {
        this.weights.putAll(weights);
    }
}
