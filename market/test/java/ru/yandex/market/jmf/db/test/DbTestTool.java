package ru.yandex.market.jmf.db.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * @author apershukov
 */
public class DbTestTool {
    private static final Logger LOG = LoggerFactory.getLogger(DbTestTool.class);

    private final DataSource dataSource;
    private final String[] clearScripts;

    DbTestTool(DataSource dataSource, String... clearScripts) {
        this.dataSource = dataSource;
        this.clearScripts = clearScripts;
    }

    public void clearDatabase() {
        for (String path : clearScripts) {
            runScript(path);
        }
    }

    public void execute(String script) {
        try (Connection connection = dataSource.getConnection()) {
            connection.prepareStatement(script).execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void runScript(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            LOG.info("#DbTestTool, Execute script: {}", path);
            execute(IOUtils.toString(in, StandardCharsets.UTF_8));
            LOG.info("#DbTestTool, complete script execution");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
