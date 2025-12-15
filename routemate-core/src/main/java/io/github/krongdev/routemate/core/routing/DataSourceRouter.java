package io.github.krongdev.routemate.core.routing;

import lombok.Getter;
import lombok.Setter;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.krongdev.routemate.core.balancer.LoadBalancer;
import io.github.krongdev.routemate.core.balancer.RoundRobinLoadBalancer;

/**
 * Dynamic DataSource router.
 * Routes to the target DataSource based on the current RoutingContext.
 */
public class DataSourceRouter extends AbstractRoutingDataSource {

    private static final Logger log = LoggerFactory.getLogger(DataSourceRouter.class);
    private final DataSource writeDataSource;
    @Getter
    private final Map<String, DataSource> readDataSources = new ConcurrentHashMap<>();

    // Safe to return COWAL
    @Getter
    private final List<String> readDataSourceKeys = new CopyOnWriteArrayList<>();
    private final Set<String> unhealthyKeys = ConcurrentHashMap.newKeySet();
    @Setter
    private LoadBalancer loadBalancer;

    public DataSourceRouter(DataSource writeDataSource, LoadBalancer loadBalancer) {
        log.error("WRITE DS CLASS = {}", writeDataSource.getClass());
        this.writeDataSource = writeDataSource;
        this.loadBalancer = loadBalancer != null ? loadBalancer : new RoundRobinLoadBalancer();
        refreshRouting();
    }

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
                log.warn("No healthy read replicas available. Falling back to WRITE DataSource.");
                return "WRITE";
            }

            // Delegate availability logic to LoadBalancer
            return loadBalancer.select(healthyKeys);
        }
        return "WRITE";
    }

    public void setReadDataSources(Map<String, DataSource> readDataSources) {
        this.readDataSources.clear();
        this.readDataSourceKeys.clear();
        if (readDataSources != null) {
            this.readDataSources.putAll(readDataSources);
            this.readDataSourceKeys.addAll(readDataSources.keySet());
        }
        refreshRouting();
    }

    public DataSource getDataSource(String key) {
        if ("WRITE".equals(key)) {
            return writeDataSource;
        }
        return readDataSources.get(key);
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
    // Weights are delegated to LoadBalancer

    public synchronized void addReadDataSource(String key, DataSource dataSource, int weight) {
        log.info("Adding Read DataSource [{}] with weight [{}]", key, weight);
        this.readDataSources.put(key, dataSource);
        this.readDataSourceKeys.add(key);
        refreshRouting();

        // Notify LoadBalancer about new weight
        if (loadBalancer != null) {
            loadBalancer.updateWeights(Map.of(key, weight)); // This merges usually
        }
    }

    public synchronized void removeReadDataSource(String key) {
        log.info("Removing Read DataSource [{}]", key);
        DataSource ds = this.readDataSources.remove(key);
        this.readDataSourceKeys.remove(key);
        this.unhealthyKeys.remove(key);

        // Close if managed
        if (ds instanceof com.zaxxer.hikari.HikariDataSource) {
            ((com.zaxxer.hikari.HikariDataSource) ds).close();
        } else if (ds instanceof java.io.Closeable) {
            try {
                ((java.io.Closeable) ds).close();
            } catch (java.io.IOException e) {
                log.warn("Error closing DataSource [{}]", key, e);
            }
        }

        refreshRouting();
    }

    private void refreshRouting() {
        Map<Object, Object> targetDataSources = new HashMap<>();
        if (writeDataSource != null) {
            targetDataSources.put("WRITE", writeDataSource);
        }
        targetDataSources.putAll(readDataSources);

        super.setTargetDataSources(targetDataSources);
        if (writeDataSource != null) {
            super.setDefaultTargetDataSource(writeDataSource);
        }
        super.afterPropertiesSet();
    }

    public void updateWeights(Map<String, Integer> newWeights) {
        if (loadBalancer != null) {
            loadBalancer.updateWeights(newWeights);
        }
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return true;
        }

        DataSource target = unwrapTarget(writeDataSource);

        if (target == this) {
            return false;
        }

        return target.isWrapperFor(iface);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }

        DataSource target = unwrapTarget(writeDataSource);

        if (target == this) {
            throw new SQLException("Cannot unwrap to " + iface);
        }

        return target.unwrap(iface);
    }

    private DataSource unwrapTarget(DataSource ds) {
        if (ds instanceof org.springframework.aop.framework.Advised advised) {
            try {
                Object target = advised.getTargetSource().getTarget();
                if (target instanceof DataSource) {
                    return (DataSource) target;
                }
            } catch (Exception e) {
                // ignore and fallback
                log.warn("Error unwrapping DataSource [{}]", ds, e);
            }
        }
        return ds;
    }

}
