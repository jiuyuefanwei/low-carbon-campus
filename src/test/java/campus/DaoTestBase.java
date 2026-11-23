package campus;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 每个测试使用一份执行过 sql/init.sql 的临时数据库。
 */
public abstract class DaoTestBase {

    protected Connection connection;
    private Path directory;

    @BeforeEach
    void openDatabase() throws Exception {
        directory = Files.createTempDirectory("campus-dao-");
        Path dbFile = directory.resolve("test.db");
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.toAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        execScript(Paths.get("sql", "init.sql"));
    }

    @AfterEach
    void closeDatabase() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
        if (directory != null && Files.exists(directory)) {
            try (Stream<Path> files = Files.list(directory)) {
                for (Path file : files.collect(Collectors.toList())) {
                    Files.deleteIfExists(file);
                }
            }
            Files.deleteIfExists(directory);
        }
    }

    private void execScript(Path script) throws Exception {
        String sql = new String(Files.readAllBytes(script), StandardCharsets.UTF_8).replaceAll("(?m)^--.*$", "");
        for (String part : sql.split(";")) {
            String statementText = part.trim();
            if (statementText.isEmpty()) {
                continue;
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute(statementText);
            }
        }
    }
}
