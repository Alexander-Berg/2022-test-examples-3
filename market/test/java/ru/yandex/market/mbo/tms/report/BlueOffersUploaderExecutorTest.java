package ru.yandex.market.mbo.tms.report;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.http.MboAudit;
import ru.yandex.market.mbo.yt.TestYtWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class BlueOffersUploaderExecutorTest {

    private final Timestamp firstFrom = new Timestamp(0L);
    private final YPath table = YPath.simple("//home/test/audit/blue_offers");
    private final YPath auditTable = YPath.simple("//home/test/audit/action_log");
    private NamedParameterJdbcTemplate auditJdbcTemplate;
    private BlueOffersUploaderYtExecutor blueOffersUploaderYtExecutor;
    private Yt yt;
    private JdbcTemplate yqlJdbcTemplate;

    @Before
    public void setup() {
        auditJdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        yt = new TestYtWrapper();
        yqlJdbcTemplate = Mockito.mock(JdbcTemplate.class);
        blueOffersUploaderYtExecutor = spy(
            new BlueOffersUploaderYtExecutor(
                auditJdbcTemplate, yt, yqlJdbcTemplate,
                table, "hahn", auditTable)
        );
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public void generalTest() throws Exception {
        Timestamp to = new Timestamp(10L);
        List<YTreeMapNode> actions = prepareAuditJdbc(firstFrom, 0L, Arrays.asList(
            Pair.of(4L, 3L),
            Pair.of(3L, 2L),
            Pair.of(8L, 4L),
            Pair.of(1L, 1L)));
        actions.addAll(prepareAuditJdbc(new Timestamp(firstFrom.getTime() +
            BlueOffersUploaderYtExecutor.INTERVAL_IN_MILLIS), 4L, Arrays.asList(
            Pair.of(10L, 5L),
            Pair.of(13L, 8L),
            Pair.of(11L, 6L),
            Pair.of(12L, 7L))));
        prepareCurrentTime(to);
        prepareLastTimestampAndActionIdSelect(0L, 0L);
        blueOffersUploaderYtExecutor.doRealJob(null);

        assertThat(yt.cypress().exists(table)).isTrue();
        List<YTreeMapNode> rows = read(table);
        assertRows(actions, rows);
    }


    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public void noRowsInOnInterval() throws Exception {
        Timestamp to = new Timestamp(10L);
        List<YTreeMapNode> actions = prepareAuditJdbc(firstFrom, 0L, Arrays.asList(
            Pair.of(4L, 3L),
            Pair.of(3L, 2L),
            Pair.of(8L, 4L),
            Pair.of(1L, 1L)));
        prepareAuditJdbc(new Timestamp(firstFrom.getTime() +
            BlueOffersUploaderYtExecutor.INTERVAL_IN_MILLIS), 4L, Collections.emptyList());
        actions.addAll(prepareAuditJdbc(new Timestamp(firstFrom.getTime() +
            BlueOffersUploaderYtExecutor.INTERVAL_IN_MILLIS * 2), 4L, Arrays.asList(
            Pair.of(10L, 5L),
            Pair.of(13L, 8L),
            Pair.of(11L, 6L),
            Pair.of(12L, 7L))));
        prepareCurrentTime(to);
        prepareLastTimestampAndActionIdSelect(0L, 0L);
        blueOffersUploaderYtExecutor.doRealJob(null);

        assertThat(yt.cypress().exists(table)).isTrue();
        List<YTreeMapNode> rows = read(table);
        assertRows(actions, rows);
    }

    private void assertRows(List<YTreeMapNode> expected, List<YTreeMapNode> actual) {
        assertThat(actual.size()).isEqualTo(expected.size());
        for (YTreeMapNode action : expected) {
            assertThat(actual.contains(action)).isTrue();
        }
        assertThat(actual.stream().sorted(Comparator.comparingLong(left ->
                left.getLong(BlueOffersUploaderYtExecutor.TIMESTAMP)))
            .collect(Collectors.toList())).isEqualTo(actual);
    }

    private List<YTreeMapNode> prepareAuditJdbc(Timestamp from, long actionId, List<Pair<Long, Long>> actions) {
        List<YTreeMapNode> toReturn = actions.stream()
            .map(this::createDefaultAction)
            .sorted(Comparator.comparingLong((YTreeMapNode row) ->
                    row.getLong(BlueOffersUploaderYtExecutor.TIMESTAMP))
                .thenComparingLong(row -> row.getLong(BlueOffersUploaderYtExecutor.ACTION_ID)))
            .collect(Collectors.toList());
        when(auditJdbcTemplate.query(Mockito.anyString(), argThat(new MapSqlParameterSourceMatcher(from, actionId,
                AuditAction.EntityType.CM_BLUE_OFFER)),
            (RowMapper<YTreeMapNode>) Mockito.any())).thenReturn(toReturn);
        return toReturn;
    }

    private YTreeMapNode createDefaultAction(Pair<Long, Long> action) {
        return YTree.mapBuilder()
            .key(BlueOffersUploaderYtExecutor.TIMESTAMP).value(action.getFirst())
            .key(BlueOffersUploaderYtExecutor.USER_ID).value(1L)
            .key(BlueOffersUploaderYtExecutor.STAFF_LOGIN).value("staff_login")
            .key(BlueOffersUploaderYtExecutor.ACTION_TYPE).value(MboAudit.ActionType.CREATE.name())
            .key(BlueOffersUploaderYtExecutor.ENTITY_ID).value(1L)
            .key(BlueOffersUploaderYtExecutor.ENTITY_NAME).value("entity")
            .key(BlueOffersUploaderYtExecutor.PROPERTY_NAME).value("property")
            .key(BlueOffersUploaderYtExecutor.OLD_VALUE).value("old_value")
            .key(BlueOffersUploaderYtExecutor.NEW_VALUE).value("new_value")
            .key(BlueOffersUploaderYtExecutor.EVENT_ID).value(1L)
            .key(BlueOffersUploaderYtExecutor.CATEGORY_ID).value(1L)
            .key(BlueOffersUploaderYtExecutor.PARAMETER_ID).value(1L)
            .key(BlueOffersUploaderYtExecutor.BILLING_MODE).value(MboAudit.BillingMode.BILLING_MODE_NONE.name())
            .key(BlueOffersUploaderYtExecutor.ACTION_ID).value(action.getSecond())
            .key(BlueOffersUploaderYtExecutor.SOURCE).value(MboAudit.Source.MDM.name())
            .key(BlueOffersUploaderYtExecutor.SOURCE_ID).value("source_id")
            .buildMap();
    }

    private void prepareCurrentTime(Timestamp timestamp) {
        when(blueOffersUploaderYtExecutor.getCurrentTimestampWithDelta(anyLong())).thenReturn(timestamp);
    }

    private void prepareLastTimestampAndActionIdSelect(long lastTimestamp, long lastActionId) {
        doReturn(Pair.of(lastTimestamp, lastActionId)).when(blueOffersUploaderYtExecutor)
            .readLastTimestampAndActionId();
    }

    private List<YTreeMapNode> read(YPath table) {
        List<YTreeMapNode> rows = new ArrayList<>();
        yt.tables().read(table, YTableEntryTypes.YSON, (Consumer<YTreeMapNode>) rows::add);
        return rows;
    }

    public static class MapSqlParameterSourceMatcher implements ArgumentMatcher<MapSqlParameterSource> {

        private final MapSqlParameterSource left;

        public MapSqlParameterSourceMatcher(Timestamp from, long actionId, AuditAction.EntityType entityType) {
            Timestamp to = new Timestamp(from.getTime() + BlueOffersUploaderYtExecutor.INTERVAL_IN_MILLIS);
            left = new MapSqlParameterSource()
                .addValue("from", from)
                .addValue("to", to)
                .addValue("fromDate", new Timestamp(from.getTime()).toLocalDateTime().toLocalDate().toString())
                .addValue("toDate", new Timestamp(to.getTime()).toLocalDateTime().toLocalDate().toString())
                .addValue("action_id", actionId)
                .addValue("limit", (long) BlueOffersUploaderYtExecutor.BATCH_SIZE)
                .addValue("entity_type", entityType.name());
        }

        @Override
        public boolean matches(MapSqlParameterSource right) {
            if (right == null ^ left == null) {
                return false;
            } else if (right == null) {
                return true;
            }
            return Objects.equals(left.getValue("from"), right.getValue("from")) &&
                Objects.equals(left.getValue("limit"), right.getValue("limit")) &&
                Objects.equals(left.getValue("action_id"), right.getValue("action_id")) &&
                Objects.equals(left.getValue("fromDate"), right.getValue("fromDate")) &&
                Objects.equals(left.getValue("toDate"), right.getValue("toDate")) &&
                Objects.equals(left.getValue("entity_type"), right.getValue("entity_type"));
        }
    }

}
