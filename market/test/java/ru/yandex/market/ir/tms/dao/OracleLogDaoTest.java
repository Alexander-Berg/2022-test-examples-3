package ru.yandex.market.ir.tms.dao;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.market.ir.tms.utils.Log4jAwareClassRunner;

import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

/**
 * @author amaslak
 */
@RunWith(Log4jAwareClassRunner.class)
public class OracleLogDaoTest {

    private OracleLogDao oracleLogDao;

    @Before
    public void setUp() throws Exception {
        String dbName = getClass().getSimpleName() + UUID.randomUUID().toString();
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl(
            "jdbc:h2:mem:" + dbName + ";INIT=RUNSCRIPT FROM 'classpath:ir-tms/test/sc_log_partitions.sql'");

        oracleLogDao = new OracleLogDao(new JdbcTemplate(dataSource));
    }

    @Test
    public void testCanAccept() throws Exception {
        boolean canAcceptValidSession = oracleLogDao.canAcceptDiff(
            "sc_offer_params", "20171124_1959", "20171124_1959"
        );
        Assert.assertTrue(canAcceptValidSession);

        boolean canAcceptInvalidSession = oracleLogDao.canAcceptDiff(
            "sc_offer_params", "20171124_1958", "20171124_1959"
        );
        Assert.assertFalse(canAcceptInvalidSession);
    }

    @Test
    public void testCanAcceptOldSessions() throws Exception {
        boolean canAcceptValidSession = oracleLogDao.canAcceptDiff(
            "sc_offer_params", "20171124_1959", "20171127_1959"
        );
        Assert.assertTrue(canAcceptValidSession);

        // + 1 day
        boolean canAcceptInvalidSession = oracleLogDao.canAcceptDiff(
            "sc_offer_params", "20171124_1959", "20171128_1959"
        );
        Assert.assertFalse(canAcceptInvalidSession);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testCountFreeSubpartitions() throws Exception {
        int freeSubpartitions = oracleLogDao.countFreeSubpartitions("sc_offer_params", "20171124_1959");
        Assert.assertEquals(21, freeSubpartitions);
    }

    @Test
    public void testGetPublishedSessions() throws Exception {
        List<String> sessions = oracleLogDao.getPublishedSessions("sc_offer_params", "20171124_1959");
        List<String> expectedSessions = Arrays.asList("20171124_1959");
        Assert.assertEquals(expectedSessions, sessions);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testCanAcceptSessions() throws Exception {
        Assert.assertNotNull(oracleLogDao);

        TreeMap<String, Long> sessions = new TreeMap<>();

        sessions.put("20171123_2143", 1L);
        sessions.put("20171123_2144", 1_000_000L);

        boolean canAcceptValidSessions = oracleLogDao.canAcceptSessions(
            "sc_offer_params", "20171123_2143", sessions
        );
        Assert.assertTrue(canAcceptValidSessions);

        sessions.put("20171123_2145", 25_000_000L);
        sessions.put("20171123_2146", 6_000_000L);

        boolean canAcceptInvalidSessions = oracleLogDao.canAcceptSessions(
            "sc_offer_params", "20171123_2143", sessions
        );
        Assert.assertFalse(canAcceptInvalidSessions);
    }

}
