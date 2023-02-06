package ru.yandex.market.reporting.generator.dao;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.reporting.config.IntegrationTestConfig;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IntegrationTestConfig.class)
public class PgLocksTest implements ApplicationContextAware {
    @Autowired
    private NamedParameterJdbcTemplate metadataJdbcTemplate;
    private ApplicationContext applicationContext;

    @Before
    public void init() {
        metadataJdbcTemplate.getJdbcOperations().execute("select pg_advisory_unlock_all()");
    }

    @Test
    public void testBasicLockingUnlocking() {
        String lockName = rndLockName();
        assertTrue(queryForBool("select is_free_lock('" + lockName + "')"));
        assertTrue(queryForBool("select is_free_lock('" + lockName + "')"));
        assertTrue(queryForBool("select get_lock('" + lockName + "')"));
        assertFalse(queryForBool("select is_free_lock('" + lockName + "')"));
        assertFalse(queryForBool("select is_free_lock('" + lockName + "')"));
        assertTrue(queryForBool("select release_lock('" + lockName + "')"));
        assertTrue(queryForBool("select is_free_lock('" + lockName + "')"));
        assertTrue(queryForBool("select is_free_lock('" + lockName + "')"));
        assertTrue(queryForBool("select get_lock('" + lockName + "')"));
        assertFalse(queryForBool("select is_free_lock('" + lockName + "')"));
        assertTrue(queryForBool("select release_lock('" + lockName + "')"));
        assertTrue(queryForBool("select is_free_lock('" + lockName + "')"));
    }

    @Test
    public void testLockFromOtherPgSession() throws SQLException {
        String lockName = rndLockName();
        DataSource dataSource = (DataSource) applicationContext.getBean("metadataDataSource");
        try(Connection conn = dataSource.getConnection(); Connection other = dataSource.getConnection()){
            assertTrue(queryForBool(conn, "select is_free_lock('" + lockName + "')"));
            assertTrue(queryForBool(conn, "select get_lock('" + lockName + "')"));
            assertFalse(queryForBool(conn, "select is_free_lock('" + lockName + "')"));
            assertFalse(queryForBool(other, "select is_free_lock('" + lockName + "')"));
            assertFalse(queryForBool(other, "select get_lock('" + lockName + "')"));
            assertTrue(queryForBool(conn, "select release_lock('" + lockName + "')"));
            assertTrue(queryForBool(other, "select is_free_lock('" + lockName + "')"));
            assertTrue(queryForBool(other, "select get_lock('" + lockName + "')"));
            assertFalse(queryForBool(conn, "select is_free_lock('" + lockName + "')"));
            assertFalse(queryForBool(conn, "select release_lock('" + lockName + "')"));
            assertTrue(queryForBool(other, "select release_lock('" + lockName + "')"));
            assertTrue(queryForBool(conn, "select is_free_lock('" + lockName + "')"));
        }
    }

    @Test
    @Ignore
    public void testBasicLockingUnlockingMultipleIterations() {
        for(int i = 0; i < 100; i++) {
            testBasicLockingUnlocking();
        }
    }

    private boolean queryForBool(String query) {
        return metadataJdbcTemplate.queryForObject(query, Collections.emptyMap(), Boolean.class);
    }

    private boolean queryForBool(Connection connection, String query) throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(query);
        rs.next();
        return rs.getBoolean(1);
    }

    private String rndLockName() {
        return "lock_" + ThreadLocalRandom.current().nextInt() + "_" + System.currentTimeMillis();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
