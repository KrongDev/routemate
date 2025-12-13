package io.geon.routemate.core.health;

import io.geon.routemate.core.routing.DataSourceRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PreDestroy;
import org.springframework.context.SmartLifecycle;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement; // Import needed
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DataSourceHealthChecker implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(DataSourceHealthChecker.class);

    private final DataSourceRouter router;
    private final ScheduledExecutorService executor;
    private final java.time.Duration interval;
    private final java.time.Duration timeout;
    private final String validationQuery;

    private volatile boolean running = false;

    public DataSourceHealthChecker(DataSourceRouter router,
            java.time.Duration interval,
            java.time.Duration timeout,
            String validationQuery) {
        this.router = router;
        this.interval = interval;
        this.timeout = timeout;
        this.validationQuery = validationQuery;

        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "routemate-health-checker");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public void start() {
        if (isRunning()) {
            return;
        }
        log.info("Starting DataSourceHealthChecker with interval={}ms, query='{}'", interval.toMillis(),
                validationQuery);
        this.executor.scheduleAtFixedRate(this::checkHealth, interval.toMillis(), interval.toMillis(),
                TimeUnit.MILLISECONDS);
        this.running = true;
    }

    @Override
    public void stop() {
        if (!isRunning()) {
            return;
        }
        log.info("Stopping DataSourceHealthChecker...");
        this.running = false;
        // Executor shutdown managed in @PreDestroy or separate hook if needed.
        // But SmartLifecycle stop is logical stop.
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down DataSourceHealthChecker executor...");
        executor.shutdownNow();
    }

    // ... checkHealth and isHealthy methods ...

    private void checkHealth() {
        Map<String, DataSource> dataSources = router.getReadDataSources();

        for (String key : router.getReadDataSourceKeys()) {
            DataSource ds = dataSources.get(key);
            if (ds == null)
                continue;

            if (isHealthy(ds, key)) {
                router.markHealthy(key);
            } else {
                router.markUnhealthy(key);
            }
        }
    }

    private boolean isHealthy(DataSource ds, String key) {
        try (Connection conn = ds.getConnection()) {
            if (validationQuery != null && !validationQuery.trim().isEmpty()) {
                // Use validation query
                try (PreparedStatement ps = conn.prepareStatement(validationQuery)) {
                    ps.setQueryTimeout((int) timeout.toSeconds());
                    ps.execute();
                    return true;
                }
            } else {
                // Use isValid
                int timeoutSec = (int) timeout.toSeconds();
                if (timeoutSec < 1)
                    timeoutSec = 1;
                return conn.isValid(timeoutSec);
            }
        } catch (SQLException e) {
            log.warn("Health check failed for [{}]: {}", key, e.getMessage());
            return false;
        }
    }
}
