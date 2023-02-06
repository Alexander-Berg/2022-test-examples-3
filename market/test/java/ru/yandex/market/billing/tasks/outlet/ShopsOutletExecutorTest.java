package ru.yandex.market.billing.tasks.outlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.xml.stream.XMLStreamException;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import ru.yandex.common.util.IOUtils;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.common.util.phone.PhoneType;
import ru.yandex.market.common.mds.s3.client.service.api.NamedHistoryMdsS3Client;
import ru.yandex.market.core.calendar.DatePeriod;
import ru.yandex.market.core.delivery.DeliveryRule;
import ru.yandex.market.core.delivery.DeliveryServiceInfo;
import ru.yandex.market.core.ds.DatasourceService;
import ru.yandex.market.core.geobase.RegionService;
import ru.yandex.market.core.outlet.OutletInfo;
import ru.yandex.market.core.outlet.OutletType;
import ru.yandex.market.core.outlet.PhoneNumber;
import ru.yandex.market.core.outlet.calendar.OutletCalendarInfo;
import ru.yandex.market.core.outlet.calendar.OutletCalendarService;
import ru.yandex.market.core.outlet.moderation.ManageOutletInfoService;
import ru.yandex.market.core.outlet.moderation.OutletExportSettings;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.BooleanParamValue;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.partner.PartnerService;
import ru.yandex.market.core.schedule.Schedule;
import ru.yandex.market.core.schedule.ScheduleLine;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link ShopsOutletExecutor}.
 */
@ExtendWith(MockitoExtension.class)
class ShopsOutletExecutorTest {
    private static final long SHOP_ID_1 = 11;
    private static final long SHOP_ID_2 = 22;
    private static final long SHOP_ID_3 = 33;
    private static final long SHOP_ID_4 = 44;
    private static final long SHOP_ID_5 = 55;
    private static final long SHOP_ID_6 = 66;

    private static final long REGION_ID_1 = 149;
    private static final long REGION_ID_2 = 159;

    private static final Currency CURRENCY_1 = Currency.BYN;
    private static final Currency CURRENCY_2 = Currency.KZT;

    private static final List<Long> ALL_SHOPS = Arrays.asList(SHOP_ID_1, SHOP_ID_2, SHOP_ID_3, SHOP_ID_4, SHOP_ID_5,
            SHOP_ID_6);

    private static final int WORKING_DAYS_FORWARD = 65;
    private static final DatePeriod PERIOD = DatePeriod.of(LocalDate.of(2019, 12, 30), WORKING_DAYS_FORWARD);

    private static final int HIDDEN_OUTLET_ID = 555;
    private static final List<OutletInfo> SHOP_OWN_OUTLETS = Arrays.asList(
            ownOutlet(1, SHOP_ID_1, "own1"),
            ownOutlet(4, SHOP_ID_2, "own2"),
            ownOutlet(HIDDEN_OUTLET_ID, SHOP_ID_1, "own555") // hidden via param_value
    );

    private static final Map<Long, Long> SHOP_REGION = ImmutableMap.of(
            SHOP_ID_1, REGION_ID_1,
            SHOP_ID_2, REGION_ID_2
    );

    private static final Map<Long, Currency> REGION_CURRENCY = ImmutableMap.of(
            REGION_ID_1, CURRENCY_1,
            REGION_ID_2, CURRENCY_2
    );

    @Mock
    private ManageOutletInfoService outletInfoService;
    @Mock
    private OutletExportSettings outletExportSettings;
    @Mock
    private ParamService paramService;
    @Mock
    private OutletCalendarService outletCalendarService;
    @Mock
    private NamedHistoryMdsS3Client historyMdsS3Client;
    @Mock
    private DatePeriodProvider periodProvider;
    @Mock
    private DatasourceService datasourceService;
    @Mock
    private RegionService regionService;
    @Mock
    private PartnerService partnerService;

    private static OutletInfo ownOutlet(long id, long shopId, String name) {
        OutletInfo outlet = new OutletInfo(id, shopId, OutletType.DEPOT, name, false, String.valueOf(id));
        outlet.setName("Аутлет собственный");
        outlet.setSchedule(new Schedule(1, Arrays.asList(
                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 0, 0, 480),
                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 0, 480, 480),
                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 0, 960, 480),
                new ScheduleLine(ScheduleLine.DayOfWeek.TUESDAY, 2, 480, 600),
                new ScheduleLine(ScheduleLine.DayOfWeek.FRIDAY, 0, 540, 480),
                new ScheduleLine(ScheduleLine.DayOfWeek.SATURDAY, 0, 540, 480)
        )
        ));
        outlet.setPhones(Collections.singletonList(
                new PhoneNumber("countryCode", null, null, null, null, PhoneType.PHONE)
        ));

        DeliveryServiceInfo deliveryServiceInfo = new DeliveryServiceInfo();
        DeliveryRule deliveryRule = new DeliveryRule();
        deliveryRule.setDeliveryServiceInfo(deliveryServiceInfo);
        outlet.addDeliveryRule(deliveryRule);

        return outlet;
    }

    @BeforeEach
    void init() {
        when(periodProvider.provide()).thenReturn(PERIOD);

        when(outletInfoService.listShopOwnOutletInfos(any())).thenReturn(SHOP_OWN_OUTLETS);
        when(outletInfoService.getShopsWithOwnOutletsForExport()).thenReturn(new HashSet<>(ALL_SHOPS));

        when(outletCalendarService.getOutletCalendarInfoFactory(any(DatePeriod.class)))
                .thenAnswer(a ->
                        (Function<OutletInfo, OutletCalendarInfo>) outletInfo ->
                                generateOutletCalendarInfo(Collections.singletonList(outletInfo)).get(
                                        outletInfo.getId())
                );

        when(paramService.listParams(ParamType.SHOW_OUTLETS))
                .thenReturn(Collections.singletonList(
                        new BooleanParamValue(ParamType.SHOW_OUTLETS, HIDDEN_OUTLET_ID, false)
                ));

        when(datasourceService.getHomeCountryRegion(anyLong())).thenReturn(null);
        SHOP_REGION.forEach((key, value) -> when(datasourceService.getHomeCountryRegion(key)).thenReturn(value));

        REGION_CURRENCY.forEach((key, value) -> when(regionService.getRegionCurrency(key)).thenReturn(value));
    }

    @Test
    void testCustomOutlet() {
        Map<Long, Map<String, Set<LocalDate>>> expected = Map.of(1003937L, Map.of("587S", Set.of(
                LocalDate.parse("2020-01-01"), LocalDate.parse("2019-12-31"), LocalDate.parse("2020-01-08"),
                LocalDate.parse("2020-01-07"), LocalDate.parse("2020-01-06"), LocalDate.parse("2020-01-05"),
                LocalDate.parse("2020-01-04"), LocalDate.parse("2020-01-03"), LocalDate.parse("2020-01-02")
        )), 123L, Map.of("3_ds_code_123",
                Set.of(LocalDate.parse("2020-01-01"), LocalDate.parse("2019-12-31")),
                "4_ds_code_123", Collections.emptySet()));
        ShopsOutletExecutor shopsOutletExecutor = new ShopsOutletExecutor(
                historyMdsS3Client,
                datasourceService,
                regionService,
                paramService,
                outletInfoService,
                outletExportSettings,
                outletCalendarService,
                periodProvider,
                partnerService,
                getClass().getResource("customOutlet.xml")
        );
        Map<Long, Map<String, Set<LocalDate>>> actual = shopsOutletExecutor.getCustomOutletsCalendar();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testDoJobWithOutlets() throws Exception {
        ShopsOutletExecutor shopsOutletExecutor = new ShopsOutletExecutor(
                historyMdsS3Client,
                datasourceService,
                regionService,
                paramService,
                outletInfoService,
                outletExportSettings,
                outletCalendarService,
                periodProvider,
                partnerService,
                getClass().getResource("customOutlet.xml")
        );
        final String actual = generateXmlData(shopsOutletExecutor);
        checkData(actual, "outlets.xml");
    }

    private void checkData(String actual, String expectedFileName) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream(expectedFileName)) {
            final String expected = IOUtils.readInputStream(Preconditions.checkNotNull(stream, expectedFileName));
            MbiAsserts.assertXmlEquals(expected, actual);
        }
    }

    private String generateXmlData(ShopsOutletExecutor shopsOutletExecutor) throws XMLStreamException {
        StringWriter writer = new StringWriter();
        shopsOutletExecutor.generateOutletsXmlFile(writer);

        final String actual = writer.toString();
        System.out.println(actual);
        return actual;
    }

    private Map<Long, OutletCalendarInfo> generateOutletCalendarInfo(Collection<OutletInfo> outlets) {
        List<LocalDate> allDays = PERIOD.toDays();
        Set<LocalDate> holiday = Collections.singleton(allDays.get(2));

        Map<Long, OutletCalendarInfo> outletCalendarInfoMap = new HashMap<>();
        for (OutletInfo outletInfo : outlets) {
            outletCalendarInfoMap.put(outletInfo.getId(), new OutletCalendarInfo(PERIOD, holiday, false));
        }
        return outletCalendarInfoMap;
    }

}
