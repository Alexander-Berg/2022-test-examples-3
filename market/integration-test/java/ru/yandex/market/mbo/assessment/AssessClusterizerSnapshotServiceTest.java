package ru.yandex.market.mbo.assessment;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.mbo.gwt.models.assessment.clusterization.ClusterizerDiffSnapshotFilter;
import ru.yandex.market.mbo.gwt.models.assessment.clusterization.ClusterizerDiffSnapshotFilter.PairStatus;
import ru.yandex.market.mbo.gwt.models.assessment.clusterization.ClusterizerSnapshot;
import ru.yandex.market.mbo.gwt.models.assessment.clusterization.OffersPairChange;

import javax.annotation.Resource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by ayratgdl on 19.08.15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:tool-config/tool-manager-config.xml"})
@Ignore("https://st.yandex-team.ru/MBO-11851")
@SuppressWarnings("checkstyle:magicNumber")
public class AssessClusterizerSnapshotServiceTest {

    @Resource(name = "assessClusterizerSnapshotService")
    private AssessClusterizerSnapshotService snapshotService;

    @Resource(name = "namedScatJdbcTemplate")
    private NamedParameterJdbcTemplate namedScatJdbcTemplate;

    private long createdSnapshotId;

    @Before
    public void setUp() {
        createdSnapshotId = 0;
    }

    @After
    public void tearDown() {
        if (createdSnapshotId != 0) {
            deleteSnapshot(createdSnapshotId);
        }
    }

    @Test
    public void createSnapshotTest() {
        int actualRowsCount = getSnapshotRowsCount(0);

        // создать снепшот
        createdSnapshotId = snapshotService.createSnapshot("Name snapshot", "Test snapshot", true);

        // проверить, что запись в AS_CLUSTERIZER_SNAPSHOT создана
        assertTrue(doesSnapshotExist(createdSnapshotId));
        // проверить, что снепшот создан
        assertTrue(actualRowsCount == getSnapshotRowsCount(createdSnapshotId));
        // проверить, что количество записей AS_CLUSTERIZER с snapshot_id = 0 не изменилось
        assertTrue(actualRowsCount == getSnapshotRowsCount(0));
    }

    @Test
    public void deleteSnapshotTest() {
        // создать снепшот
        createdSnapshotId = snapshotService.createSnapshot("Name", "Test snapshot", true);
        // удалить снепшот
        snapshotService.deleteSnapshot(createdSnapshotId);
        // убедиться что снепшот удален
        assertFalse(doesSnapshotExist(createdSnapshotId));
        assertTrue(getSnapshotRowsCount(createdSnapshotId) == 0);
    }

    @Test
    public void deleteNonExistentSnapshotTest() {
        boolean existed = snapshotService.deleteSnapshot(-1);
        assertFalse(existed);
    }

    @Test
    public void deleteID0SnapshotTest() {
        try {
            snapshotService.deleteSnapshot(0);
            fail();
        } catch (IllegalArgumentException e) {

        }
    }

    @Test
    public void getAllSnapshotsTest() {
        createdSnapshotId = snapshotService.createSnapshot("Name", "Test snapshot", true);
        List<ClusterizerSnapshot> list = snapshotService.getAllSnapshots();

        assertTrue(list.size() == snapshotsCount());

        ClusterizerSnapshot testSnapshot = null;
        for (ClusterizerSnapshot s : list) {
            if (s.getId() == createdSnapshotId) {
                testSnapshot = s;
                break;
            }
        }

        assertNotNull(testSnapshot);
        assertEquals("Test snapshot", testSnapshot.getDescriptor());
    }

    @Test
    public void getDiffSnapshots() {
        createdSnapshotId = snapshotService.createSnapshot("Name", "Test snapshot", true);
        List<OffersPairChange> changes = snapshotService.getDiffSnapshots(0, createdSnapshotId, null, 0, 10);
        assertEquals(10, changes.size());
    }

    @Test
    public void getDiffSnapshotsFilterListId() {
        createdSnapshotId = snapshotService.createSnapshot("Name", "Test snapshot", true);
        long listId = getListIds(0).get(0);

        ClusterizerDiffSnapshotFilter filter = new ClusterizerDiffSnapshotFilter();
        filter.setListId(listId);

        List<OffersPairChange> changes = snapshotService.getDiffSnapshots(0, createdSnapshotId, filter, 0, -1);
        assertEquals(getRowsCountByListId(0, listId), changes.size());

    }

    @Test
    public void getDiffSnapshot2FilterNotExistListId() {
        ClusterizerDiffSnapshotFilter filter = new ClusterizerDiffSnapshotFilter();
        filter.setListId(-1L);

        List<OffersPairChange> changes = snapshotService.getDiffSnapshots(0, 0, filter, 0, 10);
        assertTrue(changes.isEmpty());
    }

    @Test
    public void getDiffSnapshotFilterAbsencePairs() {
        ClusterizerDiffSnapshotFilter filter = new ClusterizerDiffSnapshotFilter();
        filter.setOldStatus(PairStatus.ABSENCE);

        List<OffersPairChange> changes = snapshotService.getDiffSnapshots(-1, 0, filter, 0, -1);
        assertEquals(getSnapshotRowsCount(0), changes.size());
    }

    @Test
    public void getDiffSnapshotFilterCategory() {
        List<Long> cIds = getCategoryIds();
        cIds = cIds.subList(0, cIds.size() / 2);

        ClusterizerDiffSnapshotFilter filter = new ClusterizerDiffSnapshotFilter();
        filter.setCategoryIds(cIds);

        List<OffersPairChange> changes = snapshotService.getDiffSnapshots(0, 0, filter, 0, 10);
        for (OffersPairChange c : changes) {
            assertTrue(cIds.contains(c.getFirstOffer().getHid()));
            assertTrue(cIds.contains(c.getSecondOffer().getHid()));
        }
    }

    @Test
    public void getDiffSnapshotsSize() {
        int count = snapshotService.getDiffSnapshotsSize(0, 0, null);
        assertEquals(getSnapshotRowsCount(0), count);
    }

    @Test
    public void getAllLists() {
        int listsCount = getListsCount();
        assertEquals(listsCount, snapshotService.getAllLists().size());

    }

    @Test
    public void getAllSnapshotsIsManualCreated() {
        createdSnapshotId = snapshotService.createSnapshot("Тест", "Тест", true);
        List<ClusterizerSnapshot> snps = snapshotService.getAllSnapshots();
        for (ClusterizerSnapshot snp : snps) {
            if (snp.getId() == 0) {
                assertFalse(snp.isManualCreated());
            }
            if (snp.getId() == createdSnapshotId) {
                assertTrue(snp.isManualCreated());
            }
        }
    }

    private int getSnapshotRowsCount(long snapshotId) {
        return namedScatJdbcTemplate.getJdbcOperations().query(
                "SELECT count(*) FROM as_clusterizer WHERE snapshot_id = ?",
                new Object[]{snapshotId},
                new ResultSetExtractor<Integer>() {
                    @Override
                    public Integer extractData(ResultSet rs) throws SQLException, DataAccessException {
                        if (rs.next()) {
                            return rs.getInt(1);
                        }
                        return 0;
                    }
                });
    }

    private boolean doesSnapshotExist(long snapshotId) {
        return namedScatJdbcTemplate.getJdbcOperations().query(
                "SELECT 1 FROM as_clusterizer_snapshot WHERE id = ?",
                new Object[]{snapshotId},
                new ResultSetExtractor<Boolean>() {
                    @Override
                    public Boolean extractData(ResultSet rs) throws SQLException, DataAccessException {
                        return rs.next();
                    }
                }
        );
    }

    private void deleteSnapshot(long snapshotId) {
        JdbcOperations jdbcOperations = namedScatJdbcTemplate.getJdbcOperations();
        jdbcOperations.update("DELETE FROM as_clusterizer WHERE snapshot_id = ?", snapshotId);
        jdbcOperations.update("DELETE FROM as_clusterizer_snapshot WHERE id = ?", snapshotId);
    }

    private int snapshotsCount() {
        return namedScatJdbcTemplate.getJdbcOperations().query(
                "SELECT count(*) FROM as_clusterizer_snapshot",
                new ResultSetExtractor<Integer>() {
                    @Override
                    public Integer extractData(ResultSet rs) throws SQLException, DataAccessException {
                        if (rs.next()) {
                            return rs.getInt(1);
                        }
                        return 0;
                    }
                }
        );
    }

    private List<Long> getListIds(long snapshotId) {
        return namedScatJdbcTemplate.getJdbcOperations().query(
                "SELECT list_id FROM as_clusterizer WHERE snapshot_id = ? GROUP BY list_id",
                new RowMapper<Long>() {
                    @Override
                    public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return rs.getLong("list_id");
                    }
                },
                snapshotId
        );
    }

    private int getRowsCountByListId(long snapshotId, long listId) {
        return namedScatJdbcTemplate.getJdbcOperations().query(
                "SELECT count(*) FROM as_clusterizer WHERE snapshot_id = ? AND list_id = ?",
                new Object[]{snapshotId, listId},
                new ResultSetExtractor<Integer>() {
                    @Override
                    public Integer extractData(ResultSet rs) throws SQLException, DataAccessException {
                        if (rs.next()) {
                            return rs.getInt(1);
                        }
                        return 0;
                    }
                });
    }

    private int getListsCount() {
        return namedScatJdbcTemplate.getJdbcOperations().query(
                "SELECT count(*) FROM as_offer_list_info",
                new ResultSetExtractor<Integer>() {
                    @Override
                    public Integer extractData(ResultSet rs) throws SQLException, DataAccessException {
                        if (rs.next()) {
                            return rs.getInt(1);
                        }
                        return -1;
                    }
                }
        );
    }

    private List<Long> getCategoryIds() {
        return namedScatJdbcTemplate.getJdbcOperations().query(
                "SELECT DISTINCT o.category_id " +
                        "FROM as_offers o " +
                        "INNER JOIN as_offer_list_info l ON l.id = o.list_id " +
                        "WHERE l.assess_type = 2",
                new RowMapper<Long>() {
                    @Override
                    public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return rs.getLong("category_id");
                    }
                }
        );
    }
}
