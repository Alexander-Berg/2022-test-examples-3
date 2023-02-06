package ru.yandex.market.api.cpa;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.retry.support.RetryTemplate;

import ru.yandex.common.util.collections.MultiMap;
import ru.yandex.market.api.cpa.yam.dao.PrepayRequestDao;
import ru.yandex.market.api.cpa.yam.dao.impl.PrepayRequestDaoImpl;
import ru.yandex.market.api.cpa.yam.entity.PrepayRequest;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.checkout.checkouter.shop.PaymentClass;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.core.business.BusinessService;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.campaign.model.CampaignInfo;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.delivery.calendar.DeliveryCalendarService;
import ru.yandex.market.core.delivery.repository.ShopSelfDeliveryDao;
import ru.yandex.market.core.feature.db.FeatureDao;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.feature.model.ShopFeature;
import ru.yandex.market.core.geobase.RegionService;
import ru.yandex.market.core.geobase.TimezoneService;
import ru.yandex.market.core.geobase.model.Region;
import ru.yandex.market.core.geobase.model.RegionConstants;
import ru.yandex.market.core.geobase.model.RegionType;
import ru.yandex.market.core.geobase.model.Timezone;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.ParamValueFactory;
import ru.yandex.market.core.param.model.NumberParamValue;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.partner.PartnerLinkService;
import ru.yandex.market.core.partner.PartnerService;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramService;
import ru.yandex.market.core.partner.placement.PartnerTypePlacementPrograms;
import ru.yandex.market.core.schedule.Schedule;
import ru.yandex.market.core.schedule.ScheduleDao;
import ru.yandex.market.core.schedule.ScheduleLine;
import ru.yandex.market.core.shipment.ShipmentDateCalculationRule;
import ru.yandex.market.core.shipment.dao.ShipmentDateCalculationRulesDao;
import ru.yandex.market.core.supplier.SupplierBasicAttributes;
import ru.yandex.market.core.supplier.SupplierService;
import ru.yandex.market.core.supplier.SupplierState;
import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.core.warehouse.service.WarehouseLinkService;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.schedule.ScheduleUtils.convertToCheckouterSchedule;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class CPADataPusherTest {
    private static final Timezone MOSCOW_TIMEZONE = new Timezone("Europe/Moscow", 0);
    private static final Timezone LONDON_TIMEZONE = new Timezone("Europe/London", 1);

    private final CPADataPusher cpaDataPusher;
    private final CampaignService campaignService = mock(CampaignService.class);
    private final PrepayRequestDao prepayRequestDao = mock(PrepayRequestDaoImpl.class);
    private final CheckouterClient checkouterClient = mock(CheckouterClient.class);
    private final CheckouterShopApi mockedShops = mock(CheckouterShopApi.class);
    private final EnvironmentService environmentService = mock(EnvironmentService.class);
    private final SupplierService supplierService = mock(SupplierService.class);
    private final FeatureDao featureDao = mock(FeatureDao.class);
    private final TimezoneService timezoneService = mock(TimezoneService.class);
    private final RegionService regionService = mock(RegionService.class);
    private final ParamService paramService = mock(ParamService.class, Answers.CALLS_REAL_METHODS);
    private final PartnerTypeAwareService partnerTypeAwareService = mock(PartnerTypeAwareService.class);
    private final ShipmentDateCalculationRulesDao shipmentDateCalculationRulesDao =
            mock(ShipmentDateCalculationRulesDao.class);
    private final DeliveryCalendarService deliveryCalendarService = mock(DeliveryCalendarService.class);
    private final ShopSelfDeliveryDao shopSelfDeliveryDao = mock(ShopSelfDeliveryDao.class);
    private final BusinessService businessService = mock(BusinessService.class);
    private final PartnerLinkService partnerLinkService = mock(PartnerLinkService.class);
    private final WarehouseLinkService warehouseLinkService = mock(WarehouseLinkService.class);
    private final PartnerService partnerService = mock(PartnerService.class);
    private final PartnerPlacementProgramService partnerPlacementProgramService =
            mock(PartnerPlacementProgramService.class);
    private final ScheduleDao cpaScheduleDao = mock(ScheduleDao.class);

    CPADataPusherTest() {
        cpaDataPusher = new CPADataPusher(
                new PartnerMetaDataCreator(
                        new ShopMetaDataCreator(prepayRequestDao,
                                environmentService,
                                shopSelfDeliveryDao,
                                paramService,
                                businessService,
                                featureDao,
                                partnerPlacementProgramService),
                        new SupplierMetaDataCreator(prepayRequestDao, featureDao,
                                supplierService, environmentService, paramService,
                                businessService, warehouseLinkService, partnerLinkService),
                        campaignService
                ),
                checkouterClient,
                paramService,
                cpaScheduleDao,
                regionService,
                timezoneService,
                shipmentDateCalculationRulesDao,
                deliveryCalendarService,
                partnerService,
                partnerPlacementProgramService,
                new RetryTemplate());
        when(checkouterClient.shops()).thenReturn(mockedShops);
        when(featureDao.getFeature(anyLong(), eq(FeatureType.ORDER_AUTO_ACCEPT)))
                .thenReturn(Optional.of(
                        new ShopFeature(-1, -1, FeatureType.ORDER_AUTO_ACCEPT, ParamCheckStatus.DONT_WANT)
                ));
    }

    @BeforeEach
    void init() {
        initBaseMocksForSupplier();
    }

    @Test
    void pushSupplierInfoToCheckout() {
        initSupplierService(SupplierType.THIRD_PARTY);

        cpaDataPusher.pushShopInfoToCheckout(774);

        verify(mockedShops).updateShopData(eq(774L), ArgumentMatchers.argThat(data ->
                        data.getClientId() == 5L
                                && data.getCampaignId() == 10774L
                                && data.getPrepayType() == PrepayType.YANDEX_MARKET &&
                                data.getPaymentClass(false) == PaymentClass.YANDEX
                                && data.getPaymentClass(true) == PaymentClass.YANDEX
                                && data.getAgencyCommission() != null && data.getAgencyCommission() == 200
                                && data.getMigrationMapping() == null
                )
        );
    }

    @Test
    void pushSupplierInfoWithMigrationMappingToCheckout() {
        initSupplierService(SupplierType.THIRD_PARTY);
        when(warehouseLinkService.getDonorWarehouseIdByPartnerId(12345)).thenReturn(999L);
        when(businessService.getBusinessIdByPartner(774L)).thenReturn(100L);
        when(paramService.getParam(ParamType.HAS_WAREHOUSE_MAPPING, 774)).thenReturn(
                ParamValueFactory.makeParam(ParamType.HAS_WAREHOUSE_MAPPING, 774, "MIGRATED_API"));
        cpaDataPusher.pushShopInfoToCheckout(774);

        verify(mockedShops).updateShopData(eq(774L), ArgumentMatchers.argThat(data ->
                data.getClientId() == 5L
                        && data.getCampaignId() == 10774L
                        && data.getPrepayType() == PrepayType.YANDEX_MARKET &&
                        data.getPaymentClass(false) == PaymentClass.YANDEX
                        && data.getPaymentClass(true) == PaymentClass.YANDEX
                        && data.getAgencyCommission() != null && data.getAgencyCommission() == 200
                        && data.getMigrationMapping() != null)
        );
    }

    @Test
    void zeroAgencyCommissionForFirstPartySupplier() {
        initSupplierService(SupplierType.FIRST_PARTY);

        cpaDataPusher.pushShopInfoToCheckout(774);
        verify(mockedShops).updateShopData(eq(774L), ArgumentMatchers.argThat(data ->
                data.getAgencyCommission() != null && data.getAgencyCommission() == 0
        ));

    }

    @Test
    void pushShopInfoToCheckout() {
        when(campaignService.getCampaignByDatasource(774)).thenReturn(new CampaignInfo(10774, 774, 2, 0));
        when(prepayRequestDao.getSellerClientId(774)).thenReturn(Optional.of(5L));
        when(prepayRequestDao.findActiveByDatasource(774)).thenReturn(Collections.singletonMap(
                1L, Collections.singletonList(new PrepayRequest(1L, PrepayType.YANDEX_MARKET,
                        PartnerApplicationStatus.COMPLETED, 774L, 5L,
                        "+7(999)999-99-99"))
        ));
        when(environmentService.getIntValue(anyString())).thenReturn(200);
        PartnerTypePlacementPrograms program = PartnerTypePlacementPrograms.builder()
                .partnerId(new PartnerId(CampaignType.SUPPLIER, 774l))
                .isFulfillment(true)
                .build();
        when(partnerPlacementProgramService.getPartnerTypePlacementPrograms(any())).thenReturn(program);
        cpaDataPusher.pushShopInfoToCheckout(774);

        verify(mockedShops).updateShopData(eq(774L), ArgumentMatchers.argThat(data ->
                data.getClientId() == 5L
                        && data.getCampaignId() == 10774L
                        && data.getPrepayType() == PrepayType.YANDEX_MARKET
                        && data.getAgencyCommission() == 0
        ));
    }

    @Test
    void pushSupplierSchedule() {
        when(timezoneService.getDefaultTimezone()).thenReturn(MOSCOW_TIMEZONE);
        when(timezoneService.getTimezone(LONDON_TIMEZONE.getName())).thenReturn(LONDON_TIMEZONE);
        long datasourceId = 774L;
        Schedule schedule = createTestScheduleForShop(datasourceId);

        doReturn(mock(ShopMetaData.class)).when(mockedShops).getShopData(datasourceId);

        cpaDataPusher.pushSupplierSchedule(datasourceId, schedule, LONDON_TIMEZONE.getName());

        MultiMap<Long, ru.yandex.market.checkout.checkouter.shop.ScheduleLine> expectedSchedule = new MultiMap<>();
        expectedSchedule.put(datasourceId, convertToCheckouterSchedule(schedule, LONDON_TIMEZONE, MOSCOW_TIMEZONE));
        verify(mockedShops).pushSchedules(eq(expectedSchedule));
    }

    @Test
    void testPushShipmentDateCalculationRuleByPartnerId() {
        long partnerId = 100L;
        int hoursBefore = 10;

        ShipmentDateCalculationRule rule = ShipmentDateCalculationRule.builder()
                .withPartnerId(partnerId)
                .withHourBefore(hoursBefore)
                .build();

        List<LocalDate> holidays = List.of(LocalDate.parse("2021-10-01"), LocalDate.parse("2021-10-02"));

        when(checkouterClient.shops()).thenReturn(mockedShops);
        when(shipmentDateCalculationRulesDao.getForPartner(eq(partnerId))).thenReturn(Optional.of(rule));
        when(deliveryCalendarService.getHolidaysForNextMonth(eq(partnerId))).thenReturn(holidays);
        when(mockedShops.getShopData(eq(partnerId))).thenReturn(ShopMetaData.DEFAULT);
        PartnerTypePlacementPrograms program = PartnerTypePlacementPrograms.builder()
                .partnerId(new PartnerId(CampaignType.SHOP, partnerId))
                .isDropshipBySeller(true)
                .build();
        when(partnerPlacementProgramService.getPartnerTypePlacementPrograms(any())).thenReturn(program);

        ArgumentCaptor<ru.yandex.market.checkout.checkouter.shop.shipment.ShipmentDateCalculationRule> argument =
                ArgumentCaptor.forClass(ru.yandex.market.checkout.checkouter.shop.shipment.ShipmentDateCalculationRule.class);

        cpaDataPusher.pushShipmentDateCalculationRule(partnerId);

        verify(mockedShops).saveShipmentDateCalculationRules(eq(partnerId), argument.capture());

        assertEquals(hoursBefore, argument.getValue().getHourBefore());
        assertEquals(holidays, argument.getValue().getHolidays());
    }

    @Test
    void testPushShipmentDateCalculationRuleIsNotDropship() {
        Long partnerId = 100L;

        when(checkouterClient.shops()).thenReturn(mockedShops);
        when(shipmentDateCalculationRulesDao.getForPartner(eq(partnerId))).thenReturn(Optional.empty());

        PartnerTypePlacementPrograms program = PartnerTypePlacementPrograms.builder()
                .partnerId(new PartnerId(CampaignType.SHOP, 100L))
                .isDropshipBySeller(false)
                .build();
        when(partnerPlacementProgramService.getPartnerTypePlacementPrograms(any())).thenReturn(program);
        when(mockedShops.getShopData(eq(partnerId))).thenReturn(ShopMetaData.DEFAULT);

        cpaDataPusher.pushShipmentDateCalculationRule(partnerId);

        verify(deliveryCalendarService, never()).getHolidaysForNextMonth(anyLong());
        verify(mockedShops, never()).saveShipmentDateCalculationRules(anyLong(), any());
    }

    @Test
    void testPushShipmentDateCalculationRuleHasNoShopMetadata() {
        Long partnerId = 100L;

        when(checkouterClient.shops()).thenReturn(mockedShops);
        when(mockedShops.getShopData(eq(partnerId))).thenReturn(null);

        when(shipmentDateCalculationRulesDao.getForPartner(eq(partnerId))).thenReturn(Optional.empty());
        PartnerTypePlacementPrograms program = PartnerTypePlacementPrograms.builder()
                .partnerId(new PartnerId(CampaignType.SHOP, 100L))
                .isDropshipBySeller(true)
                .build();
        when(partnerPlacementProgramService.getPartnerTypePlacementPrograms(any())).thenReturn(program);

        cpaDataPusher.pushShipmentDateCalculationRule(partnerId);

        verify(deliveryCalendarService, never()).getHolidaysForNextMonth(anyLong());
        verify(mockedShops, never()).saveShipmentDateCalculationRules(anyLong(), any());
    }

    @Test
    void doNotPushScheduleWhenNotExistingShop() {
        when(timezoneService.getDefaultTimezone()).thenReturn(MOSCOW_TIMEZONE);
        when(timezoneService.getTimezone(LONDON_TIMEZONE.getName())).thenReturn(LONDON_TIMEZONE);
        long datasourceId = 774L;
        Schedule schedule = createTestScheduleForShop(datasourceId);

        cpaDataPusher.pushSupplierSchedule(datasourceId, schedule, LONDON_TIMEZONE.getName());

        verify(mockedShops, never()).pushSchedules(any(MultiMap.class));
    }

    @Test
    void pushSupplierScheduleViaDefaultMethod() {
        when(timezoneService.getDefaultTimezone()).thenReturn(MOSCOW_TIMEZONE);
        when(timezoneService.getTimezone(0)).thenReturn(MOSCOW_TIMEZONE);
        long datasourceId = 774L;
        Schedule schedule = createTestScheduleForShop(datasourceId);

        when(paramService.getParam(ParamType.LOCAL_DELIVERY_REGION, datasourceId)).thenReturn(
                ParamValueFactory.makeParam(ParamType.LOCAL_DELIVERY_REGION, datasourceId, "213"));
        doReturn(mock(ShopMetaData.class)).when(mockedShops).getShopData(datasourceId);
        cpaDataPusher.pushSchedule(datasourceId, schedule, null);

        MultiMap<Long, ru.yandex.market.checkout.checkouter.shop.ScheduleLine> expectedSchedule = new MultiMap<>();
        expectedSchedule.put(datasourceId, convertToCheckouterSchedule(schedule, MOSCOW_TIMEZONE, MOSCOW_TIMEZONE));
        verify(mockedShops).pushSchedules(eq(expectedSchedule));
    }

    @Test
    void doNotPushScheduleZeroMinutes() {
        when(timezoneService.getDefaultTimezone()).thenReturn(MOSCOW_TIMEZONE);
        long partnerId = 774L;
        when(paramService.getParam(ParamType.LOCAL_DELIVERY_REGION, partnerId))
                .thenReturn(new NumberParamValue(ParamType.LOCAL_DELIVERY_REGION, partnerId, RegionConstants.MOSCOW));
        when(regionService.getRegion(anyLong()))
                .thenReturn(new Region(RegionConstants.MOSCOW, "Moscow", RegionConstants.RUSSIA, RegionType.CITY,
                        10800, 0));
        ScheduleLine scheduleLine = new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 1, 1, 0);
        Schedule schedule = new Schedule(partnerId, Collections.singletonList(scheduleLine));
        doReturn(mock(ShopMetaData.class)).when(mockedShops).getShopData(partnerId);
        when(timezoneService.getTimezone(0)).thenReturn(MOSCOW_TIMEZONE);
        when(cpaScheduleDao.getScheduleOrDefault(partnerId)).thenReturn(schedule);

        cpaDataPusher.pushSchedules(List.of(partnerId));

        verify(checkouterClient.shops(), never()).pushSchedules(any(MultiMap.class));
    }

    private void initBaseMocksForSupplier() {
        when(environmentService.getIntValue(anyString())).thenReturn(200);
        when(campaignService.getCampaignByDatasource(774)).thenReturn(new CampaignInfo(10774, 774, 2, 0,
                CampaignType.SUPPLIER));
        when(prepayRequestDao.getSellerClientId(774)).thenReturn(Optional.of(5L));
        when(prepayRequestDao.findLastActiveRequest(774)).thenCallRealMethod();
        when(prepayRequestDao.findActiveByDatasource(774)).thenReturn(Collections.singletonMap(
                1L, Collections.singletonList(new PrepayRequest(1L, PrepayType.YANDEX_MARKET,
                        PartnerApplicationStatus.COMPLETED, 774L, 5L, "+7(999)999-99-99"))
        ));
        when(featureDao.getFeature(anyLong(), eq(FeatureType.PREPAY)))
                .then(invocation -> Optional.of(
                        new ShopFeature(
                                -1, invocation.getArgument(0), invocation.getArgument(1), ParamCheckStatus.SUCCESS
                        )
                ));
        when(regionService.getRegion(eq(RegionConstants.MOSCOW)))
                .thenReturn(new Region(RegionConstants.MOSCOW, "Moscow", RegionConstants.RUSSIA, RegionType.CITY,
                        10800, 0));
    }

    private void initSupplierService(SupplierType type) {
        when(supplierService.getStateBySupplierId(anyLong()))
                .thenReturn(Optional.of(
                        SupplierState.newBuilder()
                                .setCampaignId(10774L)
                                .setDatasourceId(774L)
                                .setClientId(1L)
                                .setInfo(SupplierBasicAttributes.of("supplier", "domain"))
                                .setSupplierType(type)
                                .build()));
    }

    private Schedule createTestScheduleForShop(long partnerId) {
        ScheduleLine scheduleLine = new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 1, 1, 1);
        return new Schedule(partnerId, Collections.singletonList(scheduleLine));
    }
}
