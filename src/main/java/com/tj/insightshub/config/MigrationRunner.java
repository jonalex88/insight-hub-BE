package com.tj.insightshub.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;

@Component
public class MigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigrationRunner.class);
    private final JdbcTemplate jdbc;

    public MigrationRunner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS _migrations (
                    filename   VARCHAR(255) PRIMARY KEY,
                    applied_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                )
                """);

        var resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:migrations/*.sql");
        Arrays.sort(resources, Comparator.comparing(Resource::getFilename));

        for (Resource res : resources) {
            String filename = res.getFilename();
            var applied = jdbc.queryForList(
                    "SELECT 1 FROM _migrations WHERE filename = ?", filename);
            if (!applied.isEmpty()) {
                log.info("  skip  {}", filename);
                continue;
            }
            String sql = res.getContentAsString(StandardCharsets.UTF_8);
            jdbc.execute("BEGIN");
            try {
                jdbc.execute(sql);
                jdbc.update("INSERT INTO _migrations (filename) VALUES (?)", filename);
                jdbc.execute("COMMIT");
                log.info("  apply {}", filename);
            } catch (Exception e) {
                jdbc.execute("ROLLBACK");
                log.error("  FAILED {}: {}", filename, e.getMessage());
                throw e;
            }
        }
        log.info("Migrations complete.");
    }
}
