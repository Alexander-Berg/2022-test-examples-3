package ru.yandex.market.core.testing;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.common.util.db.DbUtil;
import ru.yandex.market.core.history.HistoryService;
import ru.yandex.market.core.history.TableEntityFinder;
import ru.yandex.market.core.shop.ShopActionContext;
import ru.yandex.market.mbi.util.db.jdbc.TNumberTbl;

public class DefaultTestingStatusDao implements TestingStatusDao {

    private static final String SELECT_TESTING_STATUS = "select *  from shops_web.datasources_in_testing";

    private static final String BY_DATASOURCE_ID = " where datasource_id = ?";

    private static final String UPDATE_DATASOURCES_IN_TESTING =
            "UPDATE shops_web.datasources_in_testing "
                    + "   SET ready=?, "
                    + "       approved=?, "
                    + "       in_progress=?, "
                    + "       cancelled=?, "
                    + "       push_ready_count=?, "
                    + "       fatal_cancelled=?, "
                    + "       recommendations=?, "
                    + "       start_date=?, "
                    + "       updated_at=sysdate, "
                    + "       testing_type=?, "
                    + "       status=?, "
                    + "       attempt_num=?, "
                    + "       iter_count=?, "
                    + "       quality_check_required=?, "
                    + "       clone_check_required=?, "
                    + "       claim_link=?, "
                    + "       shop_program = ?"
                    + " where id = ?";

    private static final String INSERT_STATUS =
            "INSERT "
                    + "  INTO shops_web.datasources_in_testing ("
                    + "       id, "
                    + "       datasource_id, "
                    + "       ready, "
                    + "       approved, "
                    + "       in_progress, "
                    + "       cancelled, "
                    + "       push_ready_count, "
                    + "       fatal_cancelled, "
                    + "       start_date, "
                    + "       updated_at,"
                    + "       testing_type,"
                    + "       status,"
                    + "       attempt_num,"
                    + "       iter_count,"
                    + "       quality_check_required,"
                    + "       clone_check_required,"
                    + "       claim_link,"
                    + "       shop_program"
                    + "  ) VALUES ( "
                    + "       ?, "
                    + "       ?, "
                    + "       ?, "
                    + "       ?, "
                    + "       ?, "
                    + "       ?, "
                    + "       ?, "
                    + "       ?, "
                    + "       ?, "
                    + "       sysdate, "
                    + "       ?, "
                    + "       ?, "
                    + "       ?, "
                    + "       ?, "
                    + "       ?, "
                    + "       ?, "
                    + "       ?, "
                    + "       ? "
                    + "  )";

    private static final RowMapper<TestingState> TESTING_STATE_ROW_MAPPER =
            new TestingStatusExtractor();
    private static final ResultSetExtractor<TestingState> TESTING_STATE_RESULT_SET_EXTRACTOR =
            new TestingStatusExtractor();

    private final JdbcOperations jdbcTemplate;
    private final HistoryService historyService;

    public DefaultTestingStatusDao(JdbcOperations jdbcTemplate, HistoryService historyService) {
        this.jdbcTemplate = jdbcTemplate;
        this.historyService = historyService;
    }

    @Nullable
    @Override
    public TestingState load(long shopId, ShopProgram shopProgram) {
        return jdbcTemplate.query(
                SELECT_TESTING_STATUS + BY_DATASOURCE_ID + " and shop_program = ?",
                new Object[]{shopId, shopProgram.name()},
                TESTING_STATE_RESULT_SET_EXTRACTOR);
    }

    @Override
    public List<TestingState> load(long shopId) {
        return jdbcTemplate.query(
                SELECT_TESTING_STATUS + BY_DATASOURCE_ID,
                new Object[]{shopId},
                TESTING_STATE_ROW_MAPPER);
    }

    @Nullable
    @Override
    public TestingState loadById(long testingId) {
        return jdbcTemplate.query(
                SELECT_TESTING_STATUS + " where id = ?",
                TESTING_STATE_RESULT_SET_EXTRACTOR,
                testingId);
    }

    @Nonnull
    @Override
    public Map<Long, Collection<TestingState>> loadMany(Collection<Long> keys) {
        return getShopTestingStateMap(keys, null);
    }

    @Nonnull
    @Override
    public Map<Long, TestingState> loadMany(Collection<Long> keys, ShopProgram shopProgram) {
        return getShopTestingStateMap(keys, shopProgram).entrySet().stream().
                collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().iterator().next()));
    }

    @Nonnull
    private Map<Long, Collection<TestingState>> getShopTestingStateMap(Collection<Long> keys, ShopProgram shopProgram) {
        final Map<Long, Collection<TestingState>> result = new HashMap<>(keys.size());

        List<Object> args = new ArrayList<>();
        args.add(new TNumberTbl(keys));
        if (shopProgram != null) {
            args.add(shopProgram.name());
        }

        jdbcTemplate.query(
                SELECT_TESTING_STATUS +
                        " where datasource_id  in " +
                        "(select value(t) from table (cast(? as shops_web.t_number_tbl)) t)" +
                        (shopProgram != null ? "and shop_program = ?" : ""),
                rs -> {
                    final TestingState testingStatus = TESTING_STATE_ROW_MAPPER.mapRow(rs, 0);
                    result.computeIfAbsent(testingStatus.getDatasourceId(), key -> new ArrayList<>()).
                            add(testingStatus);
                }, args.toArray()
        );
        return result;
    }

    @Override
    public List<TestingState> loadAll() {
        return jdbcTemplate.query(SELECT_TESTING_STATUS, TESTING_STATE_ROW_MAPPER);
    }

    @Override
    public void loadAllToConvert(Consumer<TestingState> stateConsumer) {
        jdbcTemplate.query(
                "SELECT * " +
                        "FROM shops_web.datasources_in_testing " +
                        "WHERE status = 0 AND datasource_id IN (SELECT id FROM shops_web.datasource) " +
                        "and shop_program = 'CPC' " +
                        "ORDER BY " +
                        "(50 * ready + 50 * approved + 50 * cancelled + fatal_cancelled + 100 * in_progress) DESC, " +
                        "updated_at DESC",
                (rs) -> {
                    stateConsumer.accept(TESTING_STATE_ROW_MAPPER.mapRow(rs, 0));
                }
        );
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void makeNeedTesting(ShopActionContext ctx, TestingState state) {
        long id = DbUtil.createEntityBySequence(jdbcTemplate, "shops_web.s_datasources_in_testing");
        jdbcTemplate.update(
                INSERT_STATUS,
                id,
                state.getDatasourceId(),
                state.isReady() ? 1 : 0,
                state.isApproved() ? 1 : 0,
                state.isInProgress() ? 1 : 0,
                state.isCancelled() ? 1 : 0,
                state.getPushReadyButtonCount(),
                state.isFatalCancelled() ? 1 : 0,
                state.getStartDate(),
                state.getTestingType().ordinal(),
                state.getStatus().getId(),
                state.getAttemptNum(),
                state.getIterationNum(),
                state.isQualityCheckRequired() ? 1 : 0,
                state.isCloneCheckRequired() ? 1 : 0,
                state.getCutoffId(),
                state.getTestingType().getShopProgram().name());
        TableEntityFinder finder = createFinder();
        HistoryService.Record.Builder historyRecord = historyService.buildCreateRecord(finder);
        historyRecord.setEntityID(id);
        historyRecord.setActionID(ctx.getActionId());
        historyRecord.setDatasourceID(state.getDatasourceId());
        historyService.addRecord(historyRecord.build());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void removeFromTesting(long actionId, long datasourceId, long testingId) {
        HistoryService.Record historyRecord = historyService.buildDeleteRecord(createFinder())
                .setActionID(actionId)
                .setDatasourceID(datasourceId)
                .setEntityID(testingId)
                .build();

        jdbcTemplate.update("DELETE FROM shops_web.datasources_in_testing WHERE id = ?", testingId);

        historyService.addRecord(historyRecord);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void update(ShopActionContext ctx, TestingState state) {
        update(ctx.getActionId(), state);
    }

    private void update(long actionId, TestingState state) {
        jdbcTemplate.update(
                UPDATE_DATASOURCES_IN_TESTING,
                state.isReady() ? 1 : 0,
                state.isApproved() ? 1 : 0,
                state.isInProgress() ? 1 : 0,
                state.isCancelled() ? 1 : 0,
                state.getPushReadyButtonCount(),
                state.isFatalCancelled() ? 1 : 0,
                state.getRecommendations(),
                state.getStartDate(),
                state.getTestingType().getId(),
                state.getStatus().getId(),
                state.getAttemptNum(),
                state.getIterationNum(),
                state.isQualityCheckRequired() ? 1 : 0,
                state.isCloneCheckRequired() ? 1 : 0,
                state.getCutoffId(),
                state.getTestingType().getShopProgram().name(),
                state.getId());
        TableEntityFinder finder = createFinder();
        HistoryService.Record.Builder historyRecord = historyService.buildUpdateRecord(finder);
        historyRecord.setActionID(actionId);
        historyRecord.setEntityID(state.getId());
        historyRecord.setDatasourceID(state.getDatasourceId());
        historyService.addRecord(historyRecord.build());
    }

    private TableEntityFinder createFinder() {
        return new TableEntityFinder(jdbcTemplate, "datasources_in_testing", "shops_web.datasources_in_testing", "id");
    }

    private static class TestingStatusExtractor implements RowMapper<TestingState>, ResultSetExtractor<TestingState> {

        public TestingState mapRow(ResultSet rs, int rowNum) throws SQLException {
            return extract(rs);
        }

        public TestingState extractData(ResultSet rs) throws SQLException {
            TestingState testingStatus = null;
            if (rs.next()) {
                testingStatus = extract(rs);
            }
            return testingStatus;
        }

        private TestingState extract(ResultSet rs) throws SQLException {
            TestingState state = new TestingState();
            state.setId(rs.getLong("id"));
            state.setDatasourceId(rs.getLong("datasource_id"));
            state.setApproved(rs.getInt("approved") > 0);
            state.setCancelled(rs.getInt("cancelled") > 0);
            state.setInProgress(rs.getInt("in_progress") > 0);
            state.setReady(rs.getInt("ready") > 0);
            state.setFatalCancelled(rs.getInt("fatal_cancelled") > 0);
            state.setPushReadyButtonCount(rs.getInt("push_ready_count"));
            state.setRecommendations(rs.getString("recommendations"));
            state.setStartDate(rs.getTimestamp("start_date"));
            state.setUpdatedAt(rs.getTimestamp("updated_at"));
            state.setTestingType(TestingType.findTestingType(rs.getInt("testing_type")));
            state.setStatus(TestingStatus.valueOf(rs.getInt("status")));
            state.setCutoffId(rs.getLong("claim_link"));
            state.setAttemptNum(rs.getInt("attempt_num"));
            state.setIterationNum(rs.getInt("iter_count"));
            state.setCloneCheckRequired(rs.getBoolean("clone_check_required"));
            state.setQualityCheckRequired(rs.getBoolean("quality_check_required"));
            return state;
        }
    }
}
