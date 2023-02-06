package ru.yandex.market.abo.core.quality_monitoring.recheck;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import one.util.streamex.LongStreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.checkorder.CheckOrderDbService;
import ru.yandex.market.abo.core.exception.ExceptionalShopReason;
import ru.yandex.market.abo.core.exception.ExceptionalShopsService;
import ru.yandex.market.abo.core.quality_monitoring.recheck.unique.FFUniqueOffersMonitoring;
import ru.yandex.market.abo.core.quality_monitoring.startrek.lowprice.MonitoringSettingsService;
import ru.yandex.market.abo.core.quality_monitoring.startrek.model.MonitoringSettings;
import ru.yandex.market.abo.core.quality_monitoring.yt.idx.YtIdxMonitoringManager;
import ru.yandex.market.abo.core.quality_monitoring.yt.idx.model.MonitoringValue;
import ru.yandex.market.abo.core.shop.on.ShopOnService;
import ru.yandex.market.abo.core.supplier.SupplierService;
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicket;
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketManager;
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketSearch;
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketType;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static ru.yandex.common.util.IOUtils.readInputStream;

class FFUniqueOffersMonitoringTest {

    private final ObjectMapper MAPPER = new ObjectMapper();
    private static final String MONITORING_VALUES_JSON = loadFileToString("/monitorings/ff_unique_monitoring.json");

    @InjectMocks
    FFUniqueOffersMonitoring ffUniqueOffersMonitoring;

    @Mock
    private SupplierService supplierService;
    @Mock
    private CheckOrderDbService checkOrderDbService;
    @Mock
    private ConfigurationService coreCounterService;
    @Mock
    private YtIdxMonitoringManager ytIdxMonitoringManager;
    @Mock
    private MonitoringSettingsService monitoringSettingsService;
    @Mock
    private RecheckTicketManager recheckTicketManager;
    @Mock
    private ExceptionalShopsService exceptionalShopsService;
    @Mock
    private ShopOnService shopOnService;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        when(ytIdxMonitoringManager.loadLastMonitoringGeneration(any(), any())).thenReturn("latest");
        when(coreCounterService.getValue(any())).thenReturn("not-latest");
        when(ytIdxMonitoringManager.loadAll(any(), any(), any())).thenReturn(
                MAPPER.readValue(MONITORING_VALUES_JSON, new TypeReference<List<MonitoringValue>>() {
                })
        );
        when(monitoringSettingsService.getSettings(any())).thenReturn(new MonitoringSettings());
    }

    @Test
    void createTicketsTest() {
        ffUniqueOffersMonitoring.createTickets();
        assertEquals(2, ffUniqueOffersMonitoring.createTickets().size());
    }

    @Test
    void exceptionalFilterTest() {
        when(exceptionalShopsService.loadShops(any(ExceptionalShopReason.class))).thenReturn(Set.of(1L));
        var actual = ffUniqueOffersMonitoring.createTickets();
        assertEquals(1, actual.size());
        assertTrue(actual.containsKey(2L));
    }

    @Test
    void hasNoModerationAssortmentTicketTest() {
        doAnswer(invocation -> {
            RecheckTicketSearch search = (RecheckTicketSearch) invocation.getArguments()[0];
            return search.getTypes().contains(RecheckTicketType.SUPPLIER_ASSORTMENT) && search.getShopIds().contains(1L);
        }).when(recheckTicketManager).ticketExists(any());
        var actual = ffUniqueOffersMonitoring.createTickets();
        assertEquals(1, actual.size());
        assertTrue(actual.containsKey(2L));
    }

    @Test
    void hasNewTicketTest() {
        doAnswer(invocation -> {
            RecheckTicketSearch search = (RecheckTicketSearch) invocation.getArguments()[0];
            return search.getTypes().contains(RecheckTicketType.FF_MODERATION) && search.getShopIds().contains(1L);
        }).when(recheckTicketManager).ticketExists(any());
        var actual = ffUniqueOffersMonitoring.createTickets();
        assertEquals(1, actual.size());
        assertTrue(actual.containsKey(2L));
    }

    @Test
    void limitTest() {
        var tickets = LongStreamEx.range(100).boxed()
                .toMap(x -> new RecheckTicket(x, RecheckTicketType.FF_MODERATION, "-"));
        assertEquals(50, ffUniqueOffersMonitoring.limit(tickets).size());
    }

    private static String loadFileToString(String fileName) {
        try {
            System.out.println(FFUniqueOffersMonitoringTest.class.getResourceAsStream(fileName));
            return readInputStream(FFUniqueOffersMonitoringTest.class.getResourceAsStream(fileName));
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    @Test
    void buildExampleAll() {
        var example = new MonitoringValue.MonitoringOffer(1L, 1L, "ware-md5", 42000.4299, "айфон", "описание");
        var expected = "Название: айфон\n" +
                "Цена в поставке: 42000.43\n" +
                "Ссылка по msku: https://pokupki.market.yandex.ru/product/1\n" +
                "Ссылка по model id: https://market.yandex.ru/product/1/offers";
        assertEquals(expected, ffUniqueOffersMonitoring.buildExampleDescription(example));
    }

    @Test
    void buildExampleEmpty() {
        var example = new MonitoringValue.MonitoringOffer(null, 2L, null, null, null, null);
        var expected = "Название: -\n" +
                "Цена в поставке: -\n" +
                "Ссылка по msku: https://pokupki.market.yandex.ru/product/2";
        assertEquals(expected, ffUniqueOffersMonitoring.buildExampleDescription(example));
    }

}
