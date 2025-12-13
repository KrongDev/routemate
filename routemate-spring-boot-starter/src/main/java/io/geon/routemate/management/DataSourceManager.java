package io.geon.routemate.management;

import com.zaxxer.hikari.HikariDataSource;
import io.geon.routemate.autoconfigure.DataSourceConfigurationProperties.PoolProperties;
import io.geon.routemate.core.routing.DataSourceRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Service to manage dynamic DataSource operations.
 * Handles creation, validation, and safe removal of DataSources.
 * <p>
 * Management APIs never affect the write data source.
 */
public class DataSourceManager {

    private static final Logger log = LoggerFactory.getLogger(DataSourceManager.class);

    private final DataSourceRouter router;
    private final PoolProperties poolTemplate;

    public DataSourceManager(DataSourceRouter router, PoolProperties poolTemplate) {
        this.router = router;
        this.poolTemplate = poolTemplate != null ? poolTemplate : new PoolProperties();
    }

    public void addReadDataSource(String key, String url, String username, String password, int weight) {
        if (key == null || key.trim().isEmpty())
            throw new IllegalArgumentException("key must not be empty");
        if (url == null || url.trim().isEmpty())
            throw new IllegalArgumentException("url must not be empty");

        // Prevent overwriting existing datasource (potential leak)
        if (router.getDataSource(key) != null) {
            throw new IllegalStateException("DataSource key already exists: " + key);
        }

        log.info("Request to add DataSource key=[{}]", key); // Keep INFO for request receipt, WARN for actual change?
                                                             // User asked for WARN.
        log.warn("Adding new Read DataSource: key=[{}], url=[{}]", key, url);

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);

        // Apply template settings
        ds.setMaximumPoolSize(poolTemplate.getMaximumPoolSize());
        ds.setMinimumIdle(poolTemplate.getMinimumIdle());
        ds.setConnectionTimeout(poolTemplate.getConnectionTimeout());
        ds.setIdleTimeout(poolTemplate.getIdleTimeout());
        ds.setMaxLifetime(poolTemplate.getMaxLifetime());

        // Essential validation before adding to rotation
        try (Connection conn = ds.getConnection()) {
            log.info("DataSource [{}] connection test successful.", key);
        } catch (SQLException e) {
            log.error("Failed to validate new DataSource key={} url={} : {}", key, url, e.getMessage());
            ds.close();
            throw new RuntimeException("Failed to connect to new DataSource", e);
        }

        try {
            router.addReadDataSource(key, ds, weight);
        } catch (Exception e) {
            log.error("Failed to register DataSource with Router key={}", key, e);
            ds.close(); // Safety close
            throw e;
        }
    }

    public void removeReadDataSource(String key) {
        if (key == null || key.trim().isEmpty())
            throw new IllegalArgumentException("key must not be empty");

        log.warn("Removing Read DataSource: key=[{}]", key);

        DataSource ds = router.getDataSource(key);
        if (ds == null) {
            log.warn("DataSource key=[{}] not found, nothing to remove.", key);
            return;
        }

        // Check if trying to remove WRITE (obsolete check if only readDataSources
        // managed, but safe)
        if ("WRITE".equals(key)) {
            throw new IllegalArgumentException("Cannot remove WRITE DataSource");
        }

        router.removeReadDataSource(key);

        if (ds instanceof HikariDataSource) {
            log.info("Closing HikariDataSource for key={}", key);
            ((HikariDataSource) ds).close();
        } else {
            // Try to close if it implements AutoCloseable (though DataSource interface
            // doesn't extend it)
            if (ds instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) ds).close();
                } catch (Exception e) {
                    log.warn("Error closing DataSource [{}]", key, e);
                }
            }
        }
    }
}
