package ru.yandex.market.pers.qa.tms.toloka.receive;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentMatchers;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.model.TolokaModerationResult;
import ru.yandex.market.pers.qa.model.TolokaModerationStatus;
import ru.yandex.market.pers.qa.service.AnswerService;
import ru.yandex.market.pers.qa.service.ModerationLogService;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.pers.qa.service.toloka.TolokaModerationService;
import ru.yandex.market.pers.qa.tms.toloka.TolokaTestsBase;
import ru.yandex.market.pers.yt.YtClient;
import ru.yandex.market.pers.yt.YtClientProvider;
import ru.yandex.market.pers.yt.YtClusterType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

abstract class TolokaReceiveResultsExecutorTest extends TolokaTestsBase {

    @Autowired
    @Qualifier("yqlJdbcTemplate")
    protected JdbcTemplate yqJdbcTemplate;
    @Autowired
    @Qualifier("pgJdbcTemplate")
    protected JdbcTemplate qaJdbcTemplate;

    @Autowired
    protected YtClientProvider ytClientProvider;

    @Autowired
    protected AnswerService answerService;
    @Autowired
    protected QuestionService questionService;

    @Autowired
    private ModerationLogService moderationLogService;

    @Autowired
    protected TolokaModerationService tolokaModerationService;

    @Override
    protected void resetMocks() {
        super.resetMocks();
        MockitoAnnotations.initMocks(this);
    }

    protected void mockYql(TableInfo table) {
        doAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains(table.getTableName())) {
                RowMapper<Pair<Long, TolokaModerationResult>> rowMapper = invocation.getArgument(1);
                List<Pair<Long, TolokaModerationResult>> results = new ArrayList<>();
                UploadItems items = table.getItems();
                results.addAll(mapToTolokaResult(rowMapper, items.ok, TolokaModerationResult.APPROVE));
                results.addAll(mapToTolokaResult(rowMapper, items.unknown, TolokaModerationResult.UNKNOWN));
                results.addAll(mapToTolokaResult(rowMapper, items.rejected, TolokaModerationResult.REJECTED));
                results.addAll(mapToTolokaResult(rowMapper, items.notQuestions, TolokaModerationResult.NOT_QUESTION));
                results.addAll(mapToTolokaResult(rowMapper, items.shopQuestions, TolokaModerationResult.SHOP_QUESTION));
                return results;
            } else {
                throw new IllegalArgumentException(sql);
            }
        }).when(yqJdbcTemplate)
            .query(
                ArgumentMatchers.contains(String.format("FROM %s.`", table.clusterType.getCode())),
                any(RowMapper.class)
            );
    }

    @NotNull
    private List<Pair<Long, TolokaModerationResult>> mapToTolokaResult(RowMapper<Pair<Long, TolokaModerationResult>> rowMapper,
                                                                       Set<Long> idList,
                                                                       TolokaModerationResult result) {

        return idList.stream()
            .map(id -> {
                try {
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getObject("id")).thenReturn(id);
                    when(rs.getString("result")).thenReturn(result.getName());
                    return rowMapper.mapRow(rs, 1);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            })
            .collect(Collectors.toList());
    }

    protected void assertModStates(QaEntityType entityType,
                                   List<Pair<Long, ModState>> modStates,
                                   UploadItems items) {
        modStates.forEach(idModStatePair -> {
            long id = idModStatePair.getLeft();
            if (items.ok.contains(id)) {
                assertModStateAndModerationLog(entityType, id, ModState.CONFIRMED, idModStatePair.getRight());
            } else if (items.rejected.contains(id)) {
                assertModStateAndModerationLog(entityType, id, ModState.TOLOKA_REJECTED, idModStatePair.getRight());
            } else if (items.unknown.contains(id)) {
                assertModStateAndModerationLog(entityType, id, ModState.TOLOKA_UNKNOWN, idModStatePair.getRight());
            } else if (items.notQuestions.contains(id)) {
                assertModStateAndModerationLog(entityType, id, ModState.TOLOKA_REJECTED, idModStatePair.getRight());
            } else if (items.shopQuestions.contains(id)) {
                assertModStateAndModerationLog(entityType, id, ModState.TOLOKA_REJECTED, idModStatePair.getRight());
            } else if (items.unprocessed.contains(id)) {
                assertModStateAndModerationLog(entityType, id, ModState.TOLOKA_UPLOADED, idModStatePair.getRight());
            } else {
                Assertions.fail("never happened");
            }
        });
    }

    private void assertModStateAndModerationLog(QaEntityType entityType, Long id, ModState expected, ModState actual) {
        Assertions.assertEquals(expected, actual);
        assertEquals(1, moderationLogService.getModerationLogRecordsCount(entityType, id, actual));
    }

    @NotNull
    protected TableInfo createTolokaTableForTest(YtClusterType clusterType,
                                                 QaEntityType entityType,
                                                 UploadItems items) {
        String tableName = UUID.randomUUID().toString();
        long tableId = tolokaModerationService.markTablesUploaded(
            entityType,
            clusterType,
            tableName,
            new ArrayList<>(items.getAll())
        );
        return new TableInfo(clusterType, tableName, tableId, items);
    }

    protected void mockTableExists(TableInfo table) {
        YtClient ytClient = getYtClient(table.getClusterType());
        doAnswer(invocation -> {
            YPath path = invocation.getArgument(0);
            return path.name().equals(table.getTableName());
        }).when(ytClient).exists(any(YPath.class));
    }

    @SafeVarargs
    protected final void markEntities(Set<Long> source, Set<Long>... targets) {
        for (Set<Long> target : targets) {
            target.addAll(source);
        }
    }

    protected void assertTablesState(List<TableInfo> tables, TolokaModerationStatus state) {
        tables.forEach(table -> {
            assertEquals((Integer) state.getValue(),
                tolokaModerationService.getModerationTableStatus(table.getTableId()));
        });
    }

    protected UploadItems getItems(List<TableInfo> tables) {
        List<UploadItems> allItemsList = tables.stream()
            .map(TableInfo::getItems)
            .collect(Collectors.toList());
        return mergeItems(allItemsList);
    }

    public static UploadItems mergeItems(UploadItems... items) {
        return mergeItems(Arrays.asList(items));
    }

    public static UploadItems mergeItems(List<UploadItems> items) {
        UploadItems result = new UploadItems();
        for (UploadItems item : items) {
            result.unprocessed.addAll(item.unprocessed);
            result.ok.addAll(item.ok);
            result.unknown.addAll(item.unknown);
            result.rejected.addAll(item.rejected);
            result.notQuestions.addAll(item.notQuestions);
            result.shopQuestions.addAll(item.shopQuestions);
        }
        return result;
    }

    protected static class TableInfo {
        private final YtClusterType clusterType;
        private final String tableName;
        private final long tableId;
        private final UploadItems items;

        public TableInfo(YtClusterType clusterType,
                         String tableName,
                         long tableId,
                         UploadItems items) {
            this.clusterType = clusterType;
            this.tableName = tableName;
            this.tableId = tableId;
            this.items = items;
        }

        public YtClusterType getClusterType() {
            return clusterType;
        }

        public String getTableName() {
            return tableName;
        }

        public long getTableId() {
            return tableId;
        }

        public UploadItems getItems() {
            return items;
        }
    }

    protected static class UploadItems {
        private final Set<Long> unprocessed = new HashSet<>();
        private final Set<Long> ok = new HashSet<>();
        private final Set<Long> unknown = new HashSet<>();
        private final Set<Long> rejected = new HashSet<>();
        private final Set<Long> notQuestions = new HashSet<>();
        private final Set<Long> shopQuestions = new HashSet<>();

        public Set<Long> getAll() {
            HashSet<Long> result = new HashSet<>();
            result.addAll(unprocessed);
            result.addAll(ok);
            result.addAll(unknown);
            result.addAll(rejected);
            result.addAll(notQuestions);
            result.addAll(shopQuestions);
            return result;
        }

        public Set<Long> getUnprocessed() {
            return unprocessed;
        }

        public Set<Long> getOk() {
            return ok;
        }

        public Set<Long> getUnknown() {
            return unknown;
        }

        public Set<Long> getRejected() {
            return rejected;
        }

        public Set<Long> getNotQuestions() {
            return notQuestions;
        }

        public Set<Long> getShopQuestions() {
            return shopQuestions;
        }
    }
}
