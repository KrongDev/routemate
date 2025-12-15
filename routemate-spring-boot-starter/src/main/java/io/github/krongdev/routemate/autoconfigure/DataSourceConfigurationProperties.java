package io.github.krongdev.routemate.autoconfigure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Setter
@Getter
@ConfigurationProperties(prefix = "routemate")
public class DataSourceConfigurationProperties {

    private boolean enabled = true;
    private Map<String, DataSourceProperties> reads = new HashMap<>();
    private RoutingProperties routing = new RoutingProperties();

    private HealthCheckProperties healthCheck = new HealthCheckProperties();
    private PoolProperties poolTemplate;

    @Setter
    @Getter
    public static class DataSourceProperties {
        private String url;
        private String username;
        private String password;
        private String driverClassName;
        private int weight = 1;
        private PoolProperties pool = new PoolProperties();

    }

    @Setter
    @Getter
    public static class PoolProperties {
        private int maximumPoolSize = 10;
        private int minimumIdle = 10;
        private long connectionTimeout = 30000;
        private long idleTimeout = 600000;
        private long maxLifetime = 1800000;

    }

    @Setter
    @Getter
    public static class RoutingProperties {
        private List<String> readDatasources;
        private String writeDatasource;
        private String loadBalanceStrategy = "round-robin";

    }

    @Setter
    @Getter
    public static class HealthCheckProperties {
        private boolean enabled = true;
        private Duration interval = Duration.ofSeconds(5);
        private Duration timeout = Duration.ofSeconds(2);
        private String validationQuery;

    }
}
