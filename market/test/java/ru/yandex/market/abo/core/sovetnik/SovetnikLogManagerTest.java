package ru.yandex.market.abo.core.sovetnik;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.abo.core.offer.report.OfferService;
import ru.yandex.market.abo.core.offer.report.ShopSwitchedOffException;
import ru.yandex.market.abo.core.yt.YtService;
import ru.yandex.market.abo.test.TestHelper;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author valeriashanti
 * @data 26/02/2020
 */
class SovetnikLogManagerTest {

    private static final List<String> ALL_YT_TABLES = List.of("2020-01-10T00:00:00", "2020-01-20T00:00:00", "2020-02-21T00:00:00",
            "2020-03-23T00:00:00", "2020-04-00T00:00:00");
    private static final List<String> EXPECTED_TABLES_BY_LIMIT = List.of("2020-04-00T00:00:00", "2020-03-23T00:00:00",
            "2020-02-21T00:00:00");

    private static final long WRONG_PRICE = 2500L;
    private static final long BIGGER_PRICE = 3002L;
    private static final long CORRECT_PRICE = 3000L;
    private static final long SMALLER_PRICE = 2998L;
    private static final int TABLES_LIMIT = 3;

    private static Offer offer = new Offer();

    @InjectMocks
    private SovetnikLogManager sovetnikLogManager;
    @Mock
    private YtService ytService;
    @Mock
    private OfferService offerService;
    @Mock
    private ConfigurationService countersConfigurationService;
    @Mock
    private ExecutorService pool;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        when(ytService.list(any())).thenReturn(ALL_YT_TABLES);
        offer.setPrice(new BigDecimal(CORRECT_PRICE));
        TestHelper.mockExecutorService(pool);
    }

    @ParameterizedTest(name = "getLatestUnprocessedTable_{index}")
    @MethodSource("getLatestUnprocessedTableMethodSource")
    void getLatestUnprocessedTable(String lastProcessedTable, String expectedUnprocessedTable) {
        when(countersConfigurationService.getValue(any())).thenReturn(lastProcessedTable);
        var sovetnikLogs = sovetnikLogManager.getUnprocessedTablesByLimit(TABLES_LIMIT);
        assertFalse(sovetnikLogs.contains(expectedUnprocessedTable));
    }

    static Stream<Arguments> getLatestUnprocessedTableMethodSource() {
        return Stream.of(
                Arguments.of(ALL_YT_TABLES.get(1), ALL_YT_TABLES.get(1)),
                Arguments.of(ALL_YT_TABLES.get(2), ALL_YT_TABLES.get(2)),
                Arguments.of(ALL_YT_TABLES.get(2), ALL_YT_TABLES.get(2))
        );
    }

    @ParameterizedTest(name = "loadAllLogsWithSuspiciousPrice_{index}")
    @MethodSource("loadAllLogsWithSuspiciousPriceMethodSource")
    void loadAllLogsWithSuspiciousPrice(
            Offer offer,
            List<SovetnikLog> sovetnikLogs,
            List<SovetnikLog> expectedLogs
    ) throws ShopSwitchedOffException {
        when(offerService.findFirstWithParams(any())).thenReturn(offer);
        List<SovetnikLog> logsWithSuspiciousPrice = sovetnikLogManager.filterLogsSuspiciousPrice(sovetnikLogs);
        assertEquals(expectedLogs, logsWithSuspiciousPrice);
    }

    static Stream<Arguments> loadAllLogsWithSuspiciousPriceMethodSource() {
        return Stream.of(
                Arguments.of(offer, createSovetnikLogByPrice(CORRECT_PRICE), Collections.emptyList()),
                Arguments.of(offer, createSovetnikLogByPrice(CORRECT_PRICE + 1), Collections.emptyList()),
                Arguments.of(offer, createSovetnikLogByPrice(CORRECT_PRICE - 1), Collections.emptyList()),
                Arguments.of(offer, createSovetnikLogByPrice(BIGGER_PRICE), createSovetnikLogByPrice(BIGGER_PRICE)),
                Arguments.of(offer, createSovetnikLogByPrice(WRONG_PRICE), createSovetnikLogByPrice(WRONG_PRICE)),
                Arguments.of(offer, createSovetnikLogByPrice(SMALLER_PRICE), createSovetnikLogByPrice(SMALLER_PRICE)),
                Arguments.of(null, Collections.emptyList(), Collections.emptyList()),
                Arguments.of(offer, Collections.emptyList(), Collections.emptyList())
        );
    }

    private static List<SovetnikLog> createSovetnikLogByPrice(long price) {
        return Collections.singletonList(new SovetnikLog(null, null, price,null, 0));
    }

    @Test
    void addReportOfferPrice() throws ShopSwitchedOffException {
        when(offerService.findFirstWithParams(any())).thenReturn(offer);
        SovetnikLog sovetnikLog = createSovetnikLogByPrice(BIGGER_PRICE).get(0);
        sovetnikLogManager.validateAndPopulate(offer, sovetnikLog);
        assertEquals(sovetnikLog.getReportPrice(), offer.getPrice().longValue());
    }

    @Test
    void noUnprocessedTablesFound() {
        when(countersConfigurationService.getValue(any())).thenReturn(ALL_YT_TABLES.get(4));
        var sovetnikLogs = sovetnikLogManager.getUnprocessedTablesByLimit(TABLES_LIMIT);
        assertTrue(sovetnikLogs.isEmpty());
    }

    @Test
    void allTableIsUnprocessed() {
        when(countersConfigurationService.getValue(any())).thenReturn(null);
        var sovetnikLogs = sovetnikLogManager.getUnprocessedTablesByLimit(TABLES_LIMIT);
        assertEquals(EXPECTED_TABLES_BY_LIMIT, sovetnikLogs);
    }

    @Test
    void getUnprocessedTablesByLimit() {
        when(countersConfigurationService.getValue(any())).thenReturn(ALL_YT_TABLES.get(0));
        assertEquals(TABLES_LIMIT, sovetnikLogManager.getUnprocessedTablesByLimit(TABLES_LIMIT).size());
    }

}
