package ru.yandex.market.supercontroller.mbologs.dao;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.market.supercontroller.mbologs.Log4jAwareSpringJUnit4ClassRunner;
import ru.yandex.market.supercontroller.mbologs.conf.MboLogsIntegrationTestConfig;
import ru.yandex.market.supercontroller.mbologs.dao.oracle.OracleTestData;
import ru.yandex.market.supercontroller.mbologs.model.SessionConfiguration;

import java.io.File;
import java.nio.file.Path;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * @author amaslak
 */
@RunWith(Log4jAwareSpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = MboLogsIntegrationTestConfig.class)
public class OraclePartitionManagerTest {

    private static final Logger log = Logger.getLogger(OraclePartitionManagerTest.class);

    private static final int JOB_CHECK_INTERVAL = 5;

    @ClassRule
    public static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

    @Autowired
    private OracleTestData oracleTestData;

    @Autowired
    private JdbcTemplate siteCatalogJdbcTemplate;

    @Autowired
    private OraclePartitionManager oraclePartitionManager;

    @BeforeClass
    public static void setUp() throws Exception {
        File confDir = TEMPORARY_FOLDER.newFolder("conf");

        Path tnsNames = confDir.toPath().resolve("tnsnames.ora");
        SqlldrTest.copyResourceContent("/mbo-logs/integration/tnsnames.ora", tnsNames, false);

        System.setProperty("oracle.net.tns_admin", tnsNames.getParent().toString());
    }

    @Before
    public void onSetUp() {
        oraclePartitionManager.setOracleJobCheckInterval(JOB_CHECK_INTERVAL);
        oraclePartitionManager.setIndexCheckInterval(JOB_CHECK_INTERVAL);

        oracleTestData.createTestTable();
        oracleTestData.fillScLogPartitions();
    }

    @After
    public void tearDown() {
    }

    @After
    public void onTearDown() {
        oracleTestData.cleanScLogPartitions();
        oracleTestData.deleteTestTable();
    }

    @Test
    public void testPublish() {
        log.info("[Publish test started]");
        String baseSessionId = oracleTestData.getNopSession();

        HashSet<String> set2 = new HashSet<>();
        set2.add("20100101_0010");
        set2.add("20200101_0020");
        set2.add("20300101_0030");

        SessionConfiguration config = oraclePartitionManager.preparePartitions(
            oracleTestData.getTestTableBase(), baseSessionId, Collections.max(set2), set2, false, true
        );

        log.debug(config);
        printLocked();
        assertStatus(set2, "locked");

        oraclePartitionManager.reindexAndPublishPartitions(oracleTestData.getTestTableBase(), config);
        assertStatus(set2, "published");
    }

    @Test
    public void testFail() {
        log.info("[Fail test started]");
        String baseSessionId = oracleTestData.getNopSession();

        HashSet<String> set1 = new HashSet<>();
        set1.add("20000101_0000");
        set1.add("20000101_0001");
        set1.add("20000101_0002");

        SessionConfiguration config = oraclePartitionManager.preparePartitions(
            oracleTestData.getTestTableBase(), baseSessionId, Collections.max(set1), set1, false, true
        );
        log.debug(config);

        printLocked();
        assertStatus(set1, "locked");

        oraclePartitionManager.failPartitions(oracleTestData.getTestTableBase(), config);
        assertStatus(set1, "failed");
    }

    @Test
    public void testPublishBase() {
        log.info("[Publish test started]");
        String baseSessionId = "20100101_0010";

        HashSet<String> set2 = new HashSet<>();
        set2.add("20100101_0010");
        set2.add("20200101_0020");
        set2.add("20300101_0030");

        SessionConfiguration config = oraclePartitionManager.preparePartitions(
            oracleTestData.getTestTableBase(), baseSessionId, Collections.max(set2), set2, false, true
        );

        log.debug(config);
        printLocked();
        assertStatus(set2, "locked");

        oraclePartitionManager.reindexAndPublishPartitions(oracleTestData.getTestTableBase(), config);
        assertStatus(set2, "published");
    }

    @Test
    public void testFailBase() {
        log.info("[Fail test started]");
        String baseSessionId = "20000101_0000";

        HashSet<String> set1 = new HashSet<>();
        set1.add("20000101_0000");
        set1.add("20000101_0001");
        set1.add("20000101_0002");
        set1.add("20000101_0003");
        set1.add("20000101_0004");

        SessionConfiguration config = oraclePartitionManager.preparePartitions(
            oracleTestData.getTestTableBase(), baseSessionId, Collections.max(set1), set1, false, true
        );
        log.debug(config);

        printLocked();
        assertStatus(set1, "locked");

        oraclePartitionManager.failPartitions(oracleTestData.getTestTableBase(), config);
        assertStatus(set1, "failed");
    }

    @Test(expected = RuntimeException.class)
    public void testNotEnoughCleanSubpartitions() {
        log.info("[Fail test started]");
        String baseSessionId = oracleTestData.getNopSession();

        HashSet<String> set1 = new HashSet<>();
        set1.add("20000101_0000");
        set1.add("20000101_0001");
        set1.add("20000101_0002");
        set1.add("20000101_0003");

        oraclePartitionManager.preparePartitions(
            oracleTestData.getTestTableBase(), baseSessionId, Collections.max(set1), set1, false, true
        );
    }

    @Test(expected = RuntimeException.class)
    public void testNoCleanPartition() {
        log.info("[Fail test started]");
        String baseSessionId = "20000101_0000";

        HashSet<String> set1 = new HashSet<>();
        set1.add("20000101_0000");
        set1.add("20000101_0001");
        set1.add("20000101_0002");
        set1.add("20000101_0003");
        set1.add("20000101_0004");

        // Set all the clean partitions to be outdated
        siteCatalogJdbcTemplate.update(
            "update sc_log_partitions set status = 2 where table_name = ? and status = 0",
            oracleTestData.getTestTableBase());

        oraclePartitionManager.preparePartitions(
            oracleTestData.getTestTableBase(), baseSessionId, Collections.max(set1), set1, false, true
        );
    }

    public void printLocked() {
        log.debug("Locked partitions count: " + countLockedPartitions());
        log.debug("Locked subpartitions count: " + countLockedSubPartitions());
    }

    public void assertStatus(Collection<String> sessions, String status) {

        List<Map<String, Object>> list = siteCatalogJdbcTemplate.queryForList(
                "select partition_id, subpartition_id, session_id, status, text, updated" +
                        " from sc_log_partitions lp join sc_partition_status ps on lp.status = ps.id" +
                        " where table_name = ?" +
                        " order by partition_id, subpartition_id", oracleTestData.getTestTableBase());

        for (Map<String, Object> map : list) {
            //noinspection SuspiciousMethodCalls
            if (sessions.contains(map.get("session_id"))) {
                Assert.assertEquals("Session status mismatch in " + map,
                        status,
                        map.get("text")
                );
            }
        }
    }

    public void prinPartitions() {
        log.debug("Partitions: partition_id, subpartition_id, session_id, status, text, updated");

        List<Map<String, Object>> list = siteCatalogJdbcTemplate.queryForList(
                "select partition_id, subpartition_id, session_id, status, text, updated" +
                        " from sc_log_partitions lp join sc_partition_status ps on lp.status = ps.id" +
                        " where table_name = ?" +
                        " order by partition_id, subpartition_id", oracleTestData.getTestTableBase());

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

        for (Map<String, Object> map : list) {
            StringBuilder sb = new StringBuilder();
            sb.append(map.get("partition_id")).append("\t");
            sb.append(map.get("subpartition_id")).append("\t");
            sb.append(map.get("session_id")).append("\t");
            sb.append(map.get("status")).append("\t");
            sb.append(String.format("%20s", map.get("text"))).append("\t");
            Object o = map.get("updated");
            if (o instanceof java.sql.Timestamp) {
                Date date = new Date(((java.sql.Timestamp) o).getTime());
                sb.append(sdf.format(date));
            } else {
                sb.append(o.getClass());
            }
            log.debug(sb);
        }
        log.debug("-------------------------------------------------------------");
    }


    public int countLockedPartitions() {
        return siteCatalogJdbcTemplate.queryForObject("select count(1)" +
                        " from sc_log_partitions lp join sc_partition_status ps on lp.status = ps.id" +
                        " where table_name = ? and ps.deletion_priority < 0 and subpartition_id = 0",
                Integer.class,
                oracleTestData.getTestTableBase()
        );
    }

    public int countLockedSubPartitions() {
        return siteCatalogJdbcTemplate.queryForObject("select count(1)" +
                        " from sc_log_partitions lp join sc_partition_status ps on lp.status = ps.id" +
                        " where table_name = ? and ps.deletion_priority < 0",
                Integer.class,
                oracleTestData.getTestTableBase()
        );
    }

}
