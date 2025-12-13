package io.geon.routemate.autoconfigure;

import io.geon.routemate.core.aop.RoutingAspect;
import io.geon.routemate.core.routing.DataSourceRouter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import com.zaxxer.hikari.HikariDataSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.geon.routemate.core.balancer.LoadBalancer;
import io.geon.routemate.core.balancer.RoundRobinLoadBalancer;
import io.geon.routemate.core.balancer.RandomLoadBalancer;
import io.geon.routemate.core.balancer.WeightedRoundRobinLoadBalancer;
import io.geon.routemate.core.health.DataSourceHealthChecker;

@AutoConfiguration
@EnableConfigurationProperties(DataSourceConfigurationProperties.class)
@ConditionalOnProperty(prefix = "routemate", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RoutemateAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LoadBalancer loadBalancer(DataSourceConfigurationProperties properties) {
        String strategy = properties.getRouting().getLoadBalanceStrategy();

        if ("random".equalsIgnoreCase(strategy)) {
            return new RandomLoadBalancer();
        }

        if ("weighted-round-robin".equalsIgnoreCase(strategy)) {
            Map<String, Integer> weights = new HashMap<>();
            properties.getDatasources().forEach((key, props) -> {
                // Only consider read datasources? Or all?
                // Router filters by readDataSourceKeys anyway.
                // We just pass all weights.
                weights.put(key, props.getWeight());
            });
            return new WeightedRoundRobinLoadBalancer(weights);
        }

        return new RoundRobinLoadBalancer();
    }

    @Bean
    public RoutingAspect routingAspect() {
        return new RoutingAspect();
    }

    @Bean
    @Primary
    public DataSourceRouter routemateDataSource(DataSourceConfigurationProperties properties,
            LoadBalancer loadBalancer) {
        DataSourceRouter router = new DataSourceRouter();
        router.setLoadBalancer(loadBalancer);
        Map<Object, Object> targetDataSources = new HashMap<>();

        // Create DataSources from properties
        properties.getDatasources().forEach((key, props) -> {
            HikariDataSource ds = new HikariDataSource();
            ds.setJdbcUrl(props.getUrl());
            ds.setUsername(props.getUsername());
            ds.setPassword(props.getPassword());
            ds.setDriverClassName(props.getDriverClassName());

            // Pool Optimization
            if (props.getPool() != null) {
                ds.setMaximumPoolSize(props.getPool().getMaximumPoolSize());
                ds.setMinimumIdle(props.getPool().getMinimumIdle());
                ds.setConnectionTimeout(props.getPool().getConnectionTimeout());
                ds.setIdleTimeout(props.getPool().getIdleTimeout());
                ds.setMaxLifetime(props.getPool().getMaxLifetime());
            }
            targetDataSources.put(key, ds);
        });

        router.setTargetDataSourcesMap(targetDataSources);

        // Pass initial weights to router for future management
        Map<String, Integer> initialWeights = new HashMap<>();
        properties.getDatasources().forEach((k, v) -> initialWeights.put(k, v.getWeight()));
        router.setWeights(initialWeights);

        if (properties.getRouting().getReadDatasources() != null) {
            router.setReadDataSourceKeys(properties.getRouting().getReadDatasources());
        }

        // Set default target
        if (properties.getDefaultDatasource() != null
                && targetDataSources.containsKey(properties.getDefaultDatasource())) {
            router.setDefaultTargetDataSource(
                    Objects.requireNonNull(targetDataSources.get(properties.getDefaultDatasource())));
        } else if (!targetDataSources.isEmpty()) {
            // Fallback to first available if no default specified or default key not found
            Object firstKey = targetDataSources.keySet().iterator().next();
            router.setDefaultTargetDataSource(Objects.requireNonNull(targetDataSources.get(firstKey)));
        }

        return router;
    }

    @Bean
    @ConditionalOnProperty(prefix = "routemate.health-check", name = "enabled", havingValue = "true", matchIfMissing = true)
    public DataSourceHealthChecker dataSourceHealthChecker(
            DataSourceRouter router,
            DataSourceConfigurationProperties properties) {

        DataSourceHealthChecker checker = new DataSourceHealthChecker(
                router,
                properties.getHealthCheck().getInterval(),
                properties.getHealthCheck().getTimeout(),
                properties.getHealthCheck().getValidationQuery());
        checker.start();
        return checker;
    }

    @Bean
    @ConditionalOnProperty(prefix = "routemate.management", name = "enabled", havingValue = "true", matchIfMissing = false)
    public io.geon.routemate.management.DataSourceManager dataSourceManager(
            DataSourceRouter router,
            DataSourceConfigurationProperties properties) {

        // Resolve Pool Template
        DataSourceConfigurationProperties.PoolProperties template = properties.getPoolTemplate();

        if (template == null) {
            // Fallback: Try to use the default datasource's pool config as template
            if (properties.getDefaultDatasource() != null) {
                DataSourceConfigurationProperties.DataSourceProperties defaultProps = properties.getDatasources()
                        .get(properties.getDefaultDatasource());
                if (defaultProps != null) {
                    template = defaultProps.getPool();
                }
            }
        }

        if (template == null && !properties.getDatasources().isEmpty()) {
            // Fallback: First available
            DataSourceConfigurationProperties.DataSourceProperties first = properties.getDatasources().values()
                    .iterator().next();
            if (first != null) {
                template = first.getPool();
            }
        }

        return new io.geon.routemate.management.DataSourceManager(router, template);
    }

    @Bean
    @ConditionalOnProperty(prefix = "routemate.management", name = "enabled", havingValue = "true", matchIfMissing = false)
    @org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
    public io.geon.routemate.management.DataSourceManagementController dataSourceManagementController(
            io.geon.routemate.management.DataSourceManager manager) {
        return new io.geon.routemate.management.DataSourceManagementController(manager);
    }
}
