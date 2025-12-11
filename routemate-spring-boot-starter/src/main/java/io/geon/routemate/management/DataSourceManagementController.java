package io.geon.routemate.management;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/routemate/api/datasources")
public class DataSourceManagementController {

    private final DataSourceManager manager;

    public DataSourceManagementController(DataSourceManager manager) {
        this.manager = manager;
    }

    @PostMapping
    public ResponseEntity<String> addDataSource(@RequestBody AddDataSourceRequest request) {
        manager.addDataSource(
                request.getKey(),
                request.getUrl(),
                request.getUsername(),
                request.getPassword(),
                request.getWeight());
        return ResponseEntity.ok("DataSource added successfully");
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<String> removeDataSource(@PathVariable String key) {
        manager.removeDataSource(key);
        return ResponseEntity.ok("DataSource removed successfully");
    }

    @Setter
    @Getter
    public static class AddDataSourceRequest {
        private String key;
        private String url;
        private String username;
        private String password;
        private int weight = 1;

    }
}
