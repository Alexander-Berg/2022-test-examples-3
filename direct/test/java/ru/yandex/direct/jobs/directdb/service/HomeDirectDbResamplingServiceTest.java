package ru.yandex.direct.jobs.directdb.service;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.bolts.collection.impl.EmptyMap;
import ru.yandex.direct.ytwrapper.YtPathUtil;
import ru.yandex.direct.ytwrapper.client.YtClusterConfig;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.direct.ytwrapper.model.YtDynamicOperator;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeEntityNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeMapNodeImpl;
import ru.yandex.yt.ytclient.proxy.ApiServiceTransaction;
import ru.yandex.yt.ytclient.proxy.request.LockNodeResult;

import static java.util.stream.Collectors.toSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.jobs.util.yt.YtEnvPath.relativePart;

@ParametersAreNonnullByDefault
class HomeDirectDbResamplingServiceTest {
    private final static String YT_HOME = "//home/direct";
    private static final String HOME_DB_PATH = "db-archive";

    @Mock
    private YtProvider ytProvider;


    @Mock
    private YtClusterConfig clusterConfig;

    @Mock
    private Yt yt;

    @Mock
    private Cypress cypress;

    @Mock
    private YtDynamicOperator ytDynamicOperator;

    @Mock
    private ApiServiceTransaction transaction;

    @Mock
    private HomeDirectDbArchivingService archivingService;

    @InjectMocks
    private HomeDirectDbResamplingService service;

    private GUID guid = GUID.create();

    @BeforeEach
    void setUp() {
        initMocks(this);

        given(ytProvider.getClusterConfig(any())).willReturn(clusterConfig);
        given(clusterConfig.getHome()).willReturn(YT_HOME);
        given(ytProvider.get(any())).willReturn(yt);
        given(yt.cypress()).willReturn(cypress);

        given(transaction.getId()).willReturn(guid);
        given(transaction.existsNode(anyString())).willReturn(CompletableFuture.completedFuture(true));
        LockNodeResult lockNodeResult = mock(LockNodeResult.class);
        given(transaction.lockNode(any(), any())).willReturn(CompletableFuture.completedFuture(lockNodeResult));
        doAnswer(invocation -> {
            Consumer<ApiServiceTransaction> consumer = invocation.getArgument(0);
            consumer.accept(transaction);
            return null;
        }).when(ytDynamicOperator).runInTransaction(any(), any());
        given(ytProvider.getDynamicOperator(any())).willReturn(ytDynamicOperator);
    }

    @Test
    @DisplayName("Прореживание должно происходить корректно")
    void shouldMakeCorrectResampling() {
        var folder = new YTreeMapNodeImpl(new EmptyMap<>());
        var end = LocalDate.of(2020, 1, 15);
        var start = end.minusMonths(3);
        start.datesUntil(end).forEach(date -> folder.put(date.toString(), new YTreeEntityNodeImpl(new EmptyMap<>())));
        given(cypress.get(any(YPath.class))).willReturn(folder);

        service.resample(LocalDate.of(2020, 1, 15), YtCluster.HAHN);

        // Должны остаться:
        var expectedLeft = Set.of(
                "2020-01-14",
                "2020-01-13",
                "2020-01-12",
                "2020-01-11",
                "2020-01-10",
                "2020-01-09",
                "2020-01-08",
                "2020-01-06",
                "2020-01-01",
                "2019-12-30",
                "2019-12-23",
                "2019-12-16",
                "2019-12-09",
                "2019-12-02",
                "2019-12-01",
                "2019-11-25",
                "2019-11-18",
                "2019-11-11",
                "2019-11-06",
                "2019-11-01",
                "2019-10-15"
        );
        expectedLeft.forEach(date -> verifyRemove(verify(cypress, never()), date));
        // Чтобы тест падал, если реализация метода пустая
        start
                .datesUntil(end)
                .filter(date -> !expectedLeft.contains(date.toString()))
                .forEach(date -> verifyRemove(verify(cypress), date.toString()));
    }

    @Test
    @DisplayName("Прореживание пожилого db-archive должно происходить корректно")
    void shouldKeepOldDbArchiveFolders() {
        var oldDbArchiveFolders = Set.of(
                "2014-07-25",
                "2014-08-01",
                "2014-08-25",
                "2014-09-01",
                "2014-10-01",
                "2014-10-29",
                "2014-11-01",
                "2014-12-01",
                "2015-01-01",
                "2015-02-03",
                "2015-02-25",
                "2015-03-01",
                "2015-03-06",
                "2015-03-07",
                "2015-04-01",
                "2015-04-03",
                "2015-04-15",
                "2015-05-07",
                "2015-06-01",
                "2015-07-01",
                "2015-07-08",
                "2015-08-01",
                "2015-08-18",
                "2015-08-19",
                "2015-09-02",
                "2015-09-10",
                "2015-10-01",
                "2015-11-01",
                "2015-11-03",
                "2015-12-01",
                "2016-01-01",
                "2016-02-01",
                "2016-03-01",
                "2016-03-18",
                "2016-04-01",
                "2016-04-05",
                "2016-05-01",
                "2016-05-27",
                "2016-06-01",
                "2016-06-05",
                "2016-07-01",
                "2016-08-01",
                "2016-09-01",
                "2016-09-27",
                "2016-10-02",
                "2016-10-04",
                "2016-10-07",
                "2016-10-14",
                "2016-11-01",
                "2016-12-01",
                "2016-12-27",
                "2017-01-01",
                "2017-02-01",
                "2017-03-01",
                "2017-04-01",
                "2017-05-01",
                "2017-06-01",
                "2017-06-22",
                "2017-06-23",
                "2017-06-29",
                "2017-07-01",
                "2017-07-13",
                "2017-08-01",
                "2017-08-29",
                "2017-09-01",
                "2017-10-01",
                "2017-11-02",
                "2017-12-01",
                "2017-12-05",
                "2018-01-01",
                "2018-02-01",
                "2018-03-01",
                "2018-03-16",
                "2018-04-01",
                "2018-05-01",
                "2018-05-08",
                "2018-05-11",
                "2018-06-02",
                "2018-07-01",
                "2018-07-04",
                "2018-08-01",
                "2018-08-18",
                "2018-09-01",
                "2018-09-19",
                "2018-10-01",
                "2018-10-23",
                "2018-11-02",
                "2018-12-01",
                "2019-01-01",
                "2019-02-01",
                "2019-03-01",
                "2019-03-06",
                "2019-03-14",
                "2019-04-01",
                "2019-05-01",
                "2019-05-09",
                "2019-06-01",
                "2019-06-21",
                "2019-07-01",
                "2019-07-10",
                "2019-08-02",
                "2019-09-01",
                "2019-10-02",
                "2019-11-01",
                "2019-12-01",
                "2020-01-01",
                "2020-02-01",
                "2020-03-02",
                "2020-04-01",
                "2020-05-01",
                "2020-06-02",
                "2020-07-02",
                "2020-07-20",
                "2020-07-28",
                "2020-08-01",
                "2020-08-03",
                "2020-08-11",
                "2020-08-17",
                "2020-08-25",
                "2020-08-31",
                "2020-09-01",
                "2020-09-07",
                "2020-09-14",
                "2020-09-19",
                "2020-09-20",
                "2020-09-21",
                "2020-09-22",
                "2020-09-23",
                "2020-09-24",
                "2020-09-25",
                "2020-09-26",
                "2020-09-27"
        );
        // Должны удалиться:
        var expectedRemoved = Set.of(
                "2014-08-25", "2014-10-29", "2015-02-25", "2015-03-06", "2015-03-07", "2015-04-03", "2015-04-15",
                "2015-07-08", "2015-08-18", "2015-08-19", "2015-09-10", "2015-11-03", "2016-03-18", "2016-04-05",
                "2016-05-27", "2016-06-05", "2016-09-27", "2016-10-04", "2016-10-07", "2016-10-14", "2016-12-27",
                "2017-06-22", "2017-06-23", "2017-06-29", "2017-07-13", "2017-08-29", "2017-12-05", "2018-03-16",
                "2018-05-08", "2018-05-11", "2018-07-04", "2018-08-18", "2018-09-19", "2018-10-23", "2019-03-06",
                "2019-03-14", "2019-05-09", "2019-06-21", "2019-07-10");
        var expectedLeft = oldDbArchiveFolders.stream().filter(f -> !expectedRemoved.contains(f)).collect(toSet());

        var folder = new YTreeMapNodeImpl(new EmptyMap<>());
        expectedLeft.forEach(date -> folder.put(date, new YTreeEntityNodeImpl(new EmptyMap<>())));
        given(cypress.get(any(YPath.class))).willReturn(folder);

        service.resample(LocalDate.of(2020, 1, 15), YtCluster.HAHN);

        expectedLeft.forEach(date -> verifyRemove(verify(cypress, never()), date));
    }

    @Test
    @DisplayName("Должен корректно отработать, если начало недели пропущено, должен не удалять другой день из недели")
    void shouldChooseAnotherDateFromWeekForMissedStartOfWeek() {
        var folder = new YTreeMapNodeImpl(new EmptyMap<>());
        var end = LocalDate.of(2020, 1, 15);
        var start = end.minusMonths(3);
        start
                .datesUntil(end)
                .filter(date -> !date.equals(LocalDate.of(2020, 1, 6)))
                .forEach(date -> folder.put(date.toString(), new YTreeEntityNodeImpl(new EmptyMap<>())));
        given(cypress.get(any(YPath.class))).willReturn(folder);

        service.resample(LocalDate.of(2020, 1, 15), YtCluster.HAHN);

        // Должны остаться:
        var expectedLeft = Set.of(
                "2020-01-14",
                "2020-01-13",
                "2020-01-12",
                "2020-01-11",
                "2020-01-10",
                "2020-01-09",
                "2020-01-08",
                "2020-01-07",
                "2020-01-01",
                "2019-12-30",
                "2019-12-23",
                "2019-12-16",
                "2019-12-09",
                "2019-12-02",
                "2019-12-01",
                "2019-11-25",
                "2019-11-18",
                "2019-11-11",
                "2019-11-06",
                "2019-11-01",
                "2019-10-15"
        );
        expectedLeft.forEach(date -> verifyRemove(verify(cypress, never()), date));
        // Чтобы тест падал, если реализация метода пустая
        start
                .datesUntil(end)
                .filter(date -> !expectedLeft.contains(date.toString()) && !date.equals(LocalDate.of(2020, 1, 6)))
                .forEach(date -> verifyRemove(verify(cypress), date.toString()));
    }

    @Test
    @DisplayName("Текущий симлинк удалять нельзя")
    void shouldNotRemoveCurrent() {
        var folder = new YTreeMapNodeImpl(new EmptyMap<>());
        folder.put("current", new YTreeEntityNodeImpl(new EmptyMap<>()));
        given(cypress.get(any(YPath.class))).willReturn(folder);

        service.resample(LocalDate.of(2020, 1, 15), YtCluster.HAHN);

        verifyRemove(verify(cypress, never()), "current");
    }

    private void verifyRemove(Cypress verify, String s) {
        verify.remove(eq(YPath.simple(YtPathUtil.generatePath(YT_HOME, relativePart(), HOME_DB_PATH, s))));
    }

}
