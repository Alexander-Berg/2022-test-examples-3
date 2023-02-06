package ru.yandex.market.pers.history.tms;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.PostConstruct;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.pers.history.MockedDbTest;
import ru.yandex.market.pers.history.socialecom.model.UserType;
import ru.yandex.market.pers.history.socialecom.model.ViewStatistics;
import ru.yandex.market.pers.history.socialecom.service.ViewImportService;
import ru.yandex.market.pers.history.socialecom.service.ViewStatsService;
import ru.yandex.market.pers.service.common.util.PersUtils;
import ru.yandex.market.pers.yt.YtClient;
import ru.yandex.market.pers.yt.yqlgen.YqlProcessor;
import ru.yandex.market.pers.yt.yqlgen.YqlTableGenResult;
import ru.yandex.market.pers.yt.yqlgen.YqlTableGenTask;
import ru.yandex.market.util.ListUtils;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

public class ViewImportServiceTest extends MockedDbTest {

    private static final List<String> MOCKED_LOGS_TABLES = List.of("2022-07-25T15:15:00",
        "2022-07-25T15:20:00",
        "2022-07-25T15:25:00",
        "2022-07-25T15:30:00",
        "2022-07-25T15:35:00",
        "2022-07-25T15:40:00");

    private static final List<String> MOCKED_TABLES_TO_IMPORT = List.of(
        Long.toString(ZonedDateTime.now().minus(5, ChronoUnit.MINUTES).toEpochSecond()),
        Long.toString(ZonedDateTime.now().minus(10, ChronoUnit.MINUTES).toEpochSecond()),
        Long.toString(ZonedDateTime.now().minus(15, ChronoUnit.MINUTES).toEpochSecond())
    );

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private ViewStatsService viewStatsService;

    @Autowired
    private ViewImportService viewImportService;

    @Autowired
    private YqlProcessor yqlProcessor;

    @Autowired
    private YtClient ytClient;

    @Autowired
    private ConfigurationService configurationService;

    @Value("${pers.yt.front-events-logs.path}") String importPath;

    @Value("${pers.history.yt.base.path}") String basePath;

    @PostConstruct
    public void prepare() {
        mockTableRemove();
        mockTableRead();
        mockLogsTables();
        mockImportTables();
    }

    @Test
    public void testImportViews() {
        mockLogsTables();
        mockImportTables();
        List<ViewStatistics> viewStatistics = List.of(
            new ViewStatistics("1", 10L),
            new ViewStatistics("2", 20L),
            new ViewStatistics("3", 30L),
            new ViewStatistics("1", UserType.BRAND, 10L),
            new ViewStatistics("2", UserType.BRAND, 20L),
            new ViewStatistics("2", UserType.BUSINESS,40L)
        );

        String expectedTable = MOCKED_TABLES_TO_IMPORT.get(2);

        mockViewsQuery(expectedTable, viewStatistics, this::prepareViewStatisticsNode);

        viewImportService.importViews();
        assertLastViewsTableName(expectedTable);
        expectedTable = MOCKED_TABLES_TO_IMPORT.get(1);

        mockViewsQuery(expectedTable, viewStatistics, this::prepareViewStatisticsNode);

        viewImportService.importViews();
        assertLastViewsTableName(expectedTable);

        List<ViewStatistics> resultStats = viewStatsService.getPostViewsByPostIds("1", "2", "3");
        assertEquals(3, resultStats.size());
        assertTrue(resultStats.stream().anyMatch(e -> e.getCount() == 20L));
        assertTrue(resultStats.stream().anyMatch(e -> e.getCount() == 40L));
        assertTrue(resultStats.stream().anyMatch(e -> e.getCount() == 60L));

        ViewStatistics result = viewStatsService.getAuthorViewsByAuthorId("1", UserType.BRAND);
        assertEquals(20L, (long) result.getCount());

        result = viewStatsService.getAuthorViewsByAuthorId("2", UserType.BRAND);
        assertEquals(40L, (long) result.getCount());

        result = viewStatsService.getAuthorViewsByAuthorId("2", UserType.BUSINESS);
        assertEquals(80L, (long) result.getCount());
    }

    @Test
    public void testReadLogsTables() {
        mockLogsTables();
        mockTableRead();
        viewImportService.readViewsFromLogfeller();
        assertLastLogsTableName(MOCKED_LOGS_TABLES.get(0));
        viewImportService.readViewsFromLogfeller();
        assertLastLogsTableName(MOCKED_LOGS_TABLES.get(1));
    }

    private void assertLastLogsTableName(String name) {
        assertEquals(name, configurationService.getValue(ViewImportService.LOGS_LAST_READ_TABLE_KEY));
    }

    private void assertLastViewsTableName(String name) {
        assertEquals(name, configurationService.getValue(ViewImportService.VIEW_STATS_LAST_IMPORTED_KEY));
    }

    private void mockTableRead() {
        when(yqlProcessor.generateOrFail(any(YqlTableGenTask.class)))
            .thenReturn(new YqlTableGenResult(Collections.emptyList()));
    }

    private void mockTableRemove() {
        doAnswer(invocation -> {
            return null;
        }).when(ytClient).remove(any());
    }

    private void mockViewsQuery(String scriptName,
                                List<ViewStatistics> stats,
                                Function<List<ViewStatistics>, List<JsonNode>> mapper) {
        doAnswer(invocation -> {
            if (invocation.getArgument(1) == null) {
                return null;
            }
            Function<JsonNode, ViewStatistics> parser = invocation.getArgument(2);
            Consumer<List<ViewStatistics>> consumer = invocation.getArgument(3);

            consumer.accept(ListUtils.toList(mapper.apply(stats), parser));
            return null;
        }).when(ytClient).consumeTableBatched(
            ArgumentMatchers.argThat(argument ->
                argument.toString().contains(scriptName)),
            anyInt(),
            any(Function.class),
            any(Consumer.class)
        );
    }

    private <T> List<JsonNode> toNodes(List<T> items) {
        try {
            String json = mapper.writeValueAsString(items);
            JsonNode node = mapper.readTree(json);
            List<JsonNode> result = new ArrayList<>();
            node.iterator().forEachRemaining(result::add);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<JsonNode> prepareViewStatisticsNode(List<ViewStatistics> statistics) {
        return toNodes(ListUtils.toList(statistics, this::viewStatisticsToMap));
    }

    private Map<String, Object> viewStatisticsToMap(ViewStatistics statistics) {
        int entityType = statistics.getEntityType().getValue();
        return PersUtils.buildMap(ViewStatistics.YT_ENTITY_ID, statistics.getId(),
            ViewStatistics.YT_ENTITY_TYPE, entityType,
            ViewStatistics.AUTHOR_TYPE, statistics.getUserTypeName(),
            ViewStatistics.VIEWS_COUNT, statistics.getCount()
        );
    }

    private void mockLogsTables() {
        when(ytClient.list(viewImportService.getImportPath())).thenReturn(
            MOCKED_LOGS_TABLES);
    }

    private void mockImportTables() {
        when(ytClient.list(viewImportService.getViewsPath())).thenReturn(MOCKED_TABLES_TO_IMPORT);
    }
}
