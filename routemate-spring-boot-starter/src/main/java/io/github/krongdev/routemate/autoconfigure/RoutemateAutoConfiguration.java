package io.github.krongdev.routemate.autoconfigure;

import com.zaxxer.hikari.HikariDataSource;
import io.github.krongdev.routemate.core.aop.RoutingAspect;
import io.github.krongdev.routemate.core.balancer.LoadBalancer;
import io.github.krongdev.routemate.core.balancer.RandomLoadBalancer;
import io.github.krongdev.routemate.core.balancer.RoundRobinLoadBalancer;
import io.github.krongdev.routemate.core.balancer.WeightedRoundRobinLoadBalancer;
import io.github.krongdev.routemate.core.health.DataSourceHealthChecker;
import io.github.krongdev.routemate.core.routing.DataSourceRouter;
import io.github.krongdev.routemate.management.DataSourceManagementController;
import io.github.krongdev.routemate.management.DataSourceManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

import org.springframework.beans.factory.annotation.Qualifier;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@AutoConfiguration
@EnableConfigurationProperties({ DataSourceConfigurationProperties.class, DataSourceProperties.class })
@ConditionalOnProperty(prefix = "routemate", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RoutemateAutoConfiguration {

    @Bean(name = "writeDataSource")
    @ConditionalOnMissingBean(name = "writeDataSource")
    public DataSource writeDataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean
    @ConditionalOnMissingBean
    public LoadBalancer loadBalancer(DataSourceConfigurationProperties properties) {
        String strategy = properties.getRouting().getLoadBalanceStrategy();

        if ("random".equalsIgnoreCase(strategy)) {
            return new RandomLoadBalancer();
        }

        if ("weighted-round-robin".equalsIgnoreCase(strategy)) {
            Map<String, Integer> weights = new HashMap<>();
            properties.getReads().forEach((key, props) -> {
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
    @ConditionalOnMissingBean(DataSourceRouter.class)
    public DataSourceRouter routemateDataSource(
            @Qualifier("writeDataSource") @org.springframework.context.annotation.Lazy DataSource writeDataSource,
            DataSourceConfigurationProperties properties,
            LoadBalancer loadBalancer) {

        // 1. Wrap the existing Write DataSource
        DataSourceRouter router = new DataSourceRouter(writeDataSource, loadBalancer);

        Map<String, DataSource> readDataSources = new HashMap<>();

        // Create Read DataSources from properties
        properties.getReads().forEach((key, props) -> {
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
            readDataSources.put(key, ds);
        });

        router.setReadDataSources(readDataSources);

        // Pass initial weights to router for future management
        Map<String, Integer> initialWeights = new HashMap<>();
        properties.getReads().forEach((k, v) -> initialWeights.put(k, v.getWeight()));
        router.updateWeights(initialWeights);

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
        // checker.start() removed; handled by SmartLifecycle
        return checker;
    }

    @Bean
    @ConditionalOnProperty(prefix = "routemate.management", name = "enabled", havingValue = "true", matchIfMissing = true)
    public DataSourceManager dataSourceManager(
            DataSourceRouter router,
            DataSourceConfigurationProperties properties) {

        // Resolve Pool Template
        DataSourceConfigurationProperties.PoolProperties template = properties.getPoolTemplate();

        if (template == null && !properties.getReads().isEmpty()) {
            // Fallback: First available read datasource
            DataSourceConfigurationProperties.DataSourceProperties first = properties.getReads().values()
                    .iterator().next();
            if (first != null) {
                template = first.getPool();
            }
        }

        if (template == null) {
            template = new DataSourceConfigurationProperties.PoolProperties();
        }

        return new DataSourceManager(router, template);
    }

    @Bean
    @ConditionalOnProperty(prefix = "routemate.management", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnWebApplication
    public DataSourceManagementController dataSourceManagementController(
            DataSourceManager manager) {
        return new DataSourceManagementController(manager);
    }
}
