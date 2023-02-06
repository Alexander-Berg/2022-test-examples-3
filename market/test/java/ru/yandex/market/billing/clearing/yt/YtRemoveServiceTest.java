package ru.yandex.market.billing.clearing.yt;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.transactions.Transaction;
import ru.yandex.inside.yt.kosher.transactions.YtTransactions;
import ru.yandex.inside.yt.kosher.ytree.YTreeStringNode;
import ru.yandex.market.FunctionalTest;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Тесты для {@link YtRemoveService}.
 */
@ExtendWith(MockitoExtension.class)
class YtRemoveServiceTest extends FunctionalTest {

    private static final String YT_PATH = "//home/market/some_path/strategy";
    private static final List<TestTableInfo> TEST_DATA = Arrays.asList(
            new TestTableInfo(
                    "20191017_1400",
                    LocalDateTime.of(2019, 10, 17, 14, 0),
                    "table"
            ),
            new TestTableInfo(
                    "20191018_1500",
                    LocalDateTime.of(2019, 10, 18, 15, 0),
                    "table"
            ),
            new TestTableInfo(
                    "latest",
                    LocalDateTime.of(2019, 10, 18, 13, 30),
                    "link"
            ),
            new TestTableInfo(
                    "link2",
                    LocalDateTime.of(2019, 10, 15, 13, 30),
                    "link"
            ),
            new TestTableInfo(
                    "tmp-path",
                    LocalDateTime.MIN,
                    "map_node"
            )
    );
    private static final List<Matcher<? super String>> TEST_DATA_TO_REMOVE = Collections.singletonList(
            Matchers.equalTo(YT_PATH + '/' + "20191017_1400")
    );
    private static final Clock CLOCK = Clock.fixed(
            LocalDateTime.of(2019, 10, 19, 15, 0).toInstant(ZoneOffset.UTC),
            ZoneId.systemDefault()
    );

    @Mock
    private Yt ytMock;

    @Mock
    private Cypress cypressMock;

    @Mock
    private Transaction transaction;

    @Mock
    private GUID transactionId;

    private YtRemoveService ytRemoveService;

    static Stream<Arguments> removeTablesArgs() {
        return Stream.of(
                Arguments.of(
                        "Удаление устаревших таблиц в узле",
                        1L,
                        ChronoUnit.DAYS,
                        YtRemoveService.RemoveMode.ALL_EXPIRED
                ),
                Arguments.of(
                        "Удаление устаревших таблиц в узле, кроме самой новой",
                        2L,
                        ChronoUnit.HOURS,
                        YtRemoveService.RemoveMode.EXCEPT_LATEST
                )
        );
    }

    private static ListF<YTreeStringNode> prepareData(List<TestTableInfo> nodeNames) {
        return Cf.wrap(
                nodeNames.stream()
                        .map(tableInfo -> new YTreeStringNodeImpl(
                                        tableInfo.name,
                                        Cf.map(
                                                "creation_time",
                                                new YTreeStringNodeImpl(tableInfo.creationTime.toString(), null),
                                                "type",
                                                new YTreeStringNodeImpl(tableInfo.type, null)
                                        )
                                )
                        )
                        .collect(Collectors.toList())
        );
    }

    @BeforeEach
    void setup() {
        YtTransactions transactions = Mockito.mock(YtTransactions.class);
        Mockito.when(
                transactions.startAndGet(
                        Mockito.any(),
                        Mockito.eq(false),
                        Mockito.any())
        ).thenReturn(transaction);
        Mockito.when(transaction.getId()).thenReturn(transactionId);
        Mockito.when(ytMock.cypress()).thenReturn(cypressMock);
        Mockito.when(ytMock.transactions()).thenReturn(transactions);
        Mockito.when(
                ytMock.cypress().list(
                        Mockito.eq(Optional.of(transactionId)),
                        Mockito.anyBoolean(),
                        Mockito.eq(YPath.simple(YT_PATH)),
                        Mockito.anyCollection()
                )
        ).thenReturn(prepareData(TEST_DATA));

        ytRemoveService = new YtRemoveService(ytMock, CLOCK);
    }

    @MethodSource("removeTablesArgs")
    @ParameterizedTest(name = "{0}")
    void testRemoveTables(
            @SuppressWarnings("unused") String description,
            long storagePeriod,
            ChronoUnit chronoUnit,
            YtRemoveService.RemoveMode removeMode
    ) {
        ytRemoveService.removeExpiredTables(
                YT_PATH,
                new YtStorageInterval(storagePeriod, chronoUnit),
                removeMode
        );
        ArgumentCaptor<YPath> removedYPathCaptor = ArgumentCaptor.forClass(YPath.class);

        Mockito.verify(cypressMock, Mockito.times(1))
                .list(
                        Mockito.eq(Optional.of(transactionId)),
                        Mockito.anyBoolean(),
                        Mockito.eq(YPath.simple(YT_PATH)),
                        Mockito.anyCollection()
                );

        Mockito.verify(cypressMock, Mockito.times(TEST_DATA_TO_REMOVE.size()))
                .remove(
                        Mockito.eq(transactionId),
                        Mockito.anyBoolean(),
                        removedYPathCaptor.capture()
                );

        Mockito.verifyNoMoreInteractions(cypressMock);

        List<String> paths = removedYPathCaptor.getAllValues().stream()
                .map(YPath::toString)
                .collect(Collectors.toList());

        assertThat(paths, Matchers.contains(TEST_DATA_TO_REMOVE));
    }

    private static class TestTableInfo {

        private final String name;
        private final Instant creationTime;
        private final String type;

        TestTableInfo(String name, LocalDateTime creationTime, String type) {
            this.name = name;
            this.creationTime = creationTime.toInstant(ZoneOffset.UTC);
            this.type = type;
        }
    }
}
