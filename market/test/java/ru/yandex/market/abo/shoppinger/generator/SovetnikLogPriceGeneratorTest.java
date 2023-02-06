package ru.yandex.market.abo.shoppinger.generator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.pinger.model.Checker;
import ru.yandex.market.abo.core.pinger.model.MpGeneratorType;
import ru.yandex.market.abo.core.pinger.model.Platform;
import ru.yandex.market.abo.core.sovetnik.SovetnikLog;
import ru.yandex.market.abo.core.sovetnik.SovetnikLogManager;
import ru.yandex.market.abo.shoppinger.MarketUrlCheckerService;
import ru.yandex.market.abo.shoppinger.Task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.shoppinger.generator.SovetnikLogPriceGenerator.TABLES_LIMIT;

/**
 * @author valeriashanti
 * @data 28/02/2020
 */
class SovetnikLogPriceGeneratorTest {

    @InjectMocks
    private SovetnikLogPriceGenerator sovetnikLogPriceGenerator;
    @Mock
    private SovetnikLogManager sovetnikLogManager;
    @Mock
    private MarketUrlCheckerService marketUrlCheckerService;
    @Captor
    private ArgumentCaptor<List<Task>> argumentTaskCaptor;
    @Captor
    private ArgumentCaptor<String> argumentTableCaptor;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        sovetnikLogPriceGenerator.setGen(MpGeneratorType.SOVETNIK_PRICE);
    }

    @ParameterizedTest(name = "addTasksFromSovetnikLogs_{index}")
    @MethodSource("addTasksFromSovetnikLogsMethodSource")
    void addTasksFromSovetnikLogs(List<Task> expectedTasks, List<SovetnikLog> sovetnikLogs) {
        sovetnikLogPriceGenerator.addTasks(sovetnikLogs);
        verify(marketUrlCheckerService).addNewTasks(argumentTaskCaptor.capture());
        assertEquals(expectedTasks, argumentTaskCaptor.getValue());
    }

    static Stream<Arguments> addTasksFromSovetnikLogsMethodSource() {
        return Stream.of(
            Arguments.of(getTaskList(Arrays.asList(1L, 2L, 3L))
                , getSovetnikLogsList(Arrays.asList(1L, 2L, 2L, 2L, 3L))),
            Arguments.of(getTaskList(Collections.singletonList(1L))
                , getSovetnikLogsList(Arrays.asList(1L, 1L, 1L, 1L, 1L)))
        );
    }

    @ParameterizedTest(name = "addNewTasks_{index}")
    @MethodSource("addNewTasksMethodSource")
    void addNewTasks(
        List<String> unprocessedTables,
        List<SovetnikLog> sovetnikLogs,
        List<SovetnikLog> logsWithSuspiciousPrice
    ) {
        when(sovetnikLogManager.getUnprocessedTablesByLimit(TABLES_LIMIT)).thenReturn(unprocessedTables);
        when(sovetnikLogManager.loadLogsByRange(anyString(), anyString())).thenReturn(sovetnikLogs);
        when(sovetnikLogManager.filterLogsSuspiciousPrice(any())).thenReturn(logsWithSuspiciousPrice);
        sovetnikLogPriceGenerator.addNewTasks();

        verify(marketUrlCheckerService).addNewTasks(argumentTaskCaptor.capture());
        assertEquals(logsWithSuspiciousPrice.size(), argumentTaskCaptor.getValue().size());
    }

    static Stream<Arguments> addNewTasksMethodSource() {
        var sovetnikLogs = getSovetnikLogsList(Arrays.asList(1L, 2L, 2L, 2L, 3L));
        var logsWithSuspiciousPrice = getSovetnikLogsList(Arrays.asList(2L, 3L));
        var allTables = List.of("2020-02-21T00:00:00", "2020-03-23T00:00:00", "2020-04-00T00:00:00");
        return Stream.of(
            Arguments.of(allTables, sovetnikLogs, logsWithSuspiciousPrice),
            Arguments.of(allTables, sovetnikLogs, Collections.emptyList()),
            Arguments.of(allTables, Collections.emptyList(), Collections.emptyList()),
            Arguments.of(Collections.emptyList(), Collections.emptyList(), Collections.emptyList())
        );
    }

    private static List<Task> getTaskList(List<Long> shopIds) {
        return StreamEx.of(shopIds).map(SovetnikLogPriceGeneratorTest::createTask).toList();
    }

    private static Task createTask(long shopId) {
        return new Task(
                "url", shopId, 0, MpGeneratorType.SOVETNIK_PRICE, Checker.PRICE_RANGE, null, null, null, 0L, null, Platform.DESKTOP
        );
    }

    private static List<SovetnikLog> getSovetnikLogsList(List<Long> shopIds) {
        return StreamEx.of(shopIds).map(SovetnikLogPriceGeneratorTest::createSovetnikLog).toList();
    }

    private static SovetnikLog createSovetnikLog(long shopId) {
        return new SovetnikLog("url", null, null, null, shopId);
    }
}
