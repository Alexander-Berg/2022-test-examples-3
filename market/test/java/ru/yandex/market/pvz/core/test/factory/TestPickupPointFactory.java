package ru.yandex.market.pvz.core.test.factory;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.shops.SimpleShopRegistrationResponse;
import ru.yandex.market.pvz.client.model.approve.PrePickupPointApproveStatus;
import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.core.domain.approve.crm.PickupPointCreateService;
import ru.yandex.market.pvz.core.domain.approve.crm_pre_pickup_point.CrmPrePickupPointLocationParams;
import ru.yandex.market.pvz.core.domain.approve.crm_pre_pickup_point.CrmPrePickupPointParams;
import ru.yandex.market.pvz.core.domain.delivery_service.DeliveryService;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointParams;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.pvz.core.domain.pickup_point.branding.BrandRegionParams;
import ru.yandex.market.pvz.core.domain.pickup_point.branding.PickupPointBrandingDataParams;
import ru.yandex.market.pvz.core.domain.pickup_point.calendar.override.PickupPointCalendarOverrideParams;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.DeactivationReason;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.DeactivationReasonRepository;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.PickupPointDeactivationCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.location.PickupPointLocationParams;
import ru.yandex.market.pvz.core.domain.pickup_point.schedule.PickupPointScheduleParams;
import ru.yandex.market.pvz.core.domain.pickup_point.schedule.PickupPointScheduleParams.PickupPointScheduleDayParams;
import ru.yandex.market.pvz.core.domain.pickup_point.timezone.PickupPointTimezone;
import ru.yandex.market.pvz.core.domain.pickup_point.timezone.PickupPointTimezoneCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.timezone.PickupPointTimezoneService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint.DEFAULT_FIRST_DEACTIVATION_REASON;
import static ru.yandex.market.pvz.core.test.factory.TestBrandRegionFactory.DEFAULT_REGIONS;
import static ru.yandex.market.pvz.core.test.factory.TestCrmPrePickupPointFactory.CrmPrePickupPointTestParams.DEFAULT_BRANDING_DATA;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_CLIENT_AREA;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_WAREHOUSE_AREA;

@Transactional
public class TestPickupPointFactory {

    @Autowired
    private PickupPointCommandService pickupPointCommandService;

    @Autowired
    private TestLegalPartnerFactory legalPartnerFactory;

    @Autowired
    private TestCrmPrePickupPointFactory crmPrePickupPointFactory;

    @Autowired
    private PickupPointRepository pickupPointRepository;

    @Autowired
    private PickupPointCreateService pickupPointCreateService;

    @Autowired
    private PickupPointDeactivationCommandService pickupPointDeactivationCommandService;

    @Autowired
    private TestPickupPointCourierMappingFactory pickupPointCourierMappingFactory;

    @Autowired
    private TestBrandRegionFactory brandRegionFactory;

    @Autowired
    private PickupPointTimezoneCommandService pickupPointTimezoneCommandService;

    @Autowired
    private PickupPointTimezoneService pickupPointTimezoneService;

    @Autowired
    private DeactivationReasonRepository deactivationReasonRepository;

    @Autowired
    private MbiApiClient mbiApiClient;

    @Deprecated
    public PickupPoint createPickupPoint(CreatePickupPointBuilder builder) {
        addFirstDeactivationReason();

        if (builder.getLegalPartner() == null) {
            builder.setLegalPartner(legalPartnerFactory.createLegalPartner());
        }
        PickupPointParams params = buildPickupPoint(builder.getParams());
        PickupPoint pickupPoint = pickupPointCommandService.create(
                params, builder.getTicket(), builder.getLegalPartner().getId());

        if (pickupPoint.getLegalPartner().getPickupPoints() == null) {
            pickupPoint.getLegalPartner().setPickupPoints(new ArrayList<>());
        }
        if (!pickupPoint.getLegalPartner().getPickupPoints().contains(pickupPoint)) {
            pickupPoint.getLegalPartner().getPickupPoints().add(pickupPoint);
        }
        return pickupPoint;
    }

    public PickupPoint createPickupPointFromCrm(CreatePickupPointBuilder builder) {
        addFirstDeactivationReason();

        if (builder.getLegalPartner() == null) {
            builder.setLegalPartner(legalPartnerFactory.createLegalPartner());
        }
        PickupPointParams params = buildPickupPoint(builder.getParams());
        if (builder.getCrmPrePickupPoint() == null) {
            builder.setCrmPrePickupPoint(crmPrePickupPointFactory.create(
                    TestCrmPrePickupPointFactory.CrmPrePickupPointTestParamsBuilder.builder()
                            .legalPartnerId(builder.getLegalPartner().getId())
                            .params(TestCrmPrePickupPointFactory.CrmPrePickupPointTestParams.builder()
                                    .status(PrePickupPointApproveStatus.CHECKING)
                                    .name(params.getName())
                                    .phone(params.getPhone())
                                    .prepayAllowed(params.getPrepayAllowed())
                                    .cardAllowed(params.getCardAllowed())
                                    .cashAllowed(params.getCashAllowed())
                                    .instruction(params.getInstruction())
                                    .wantToBeBranded(params.getBrandingType().isBrand())
                                    .brandingData(DEFAULT_BRANDING_DATA)
                                    .location(mapToCrmTestLocation(builder.getParams().getLocation()))
                                    .scheduleDays(mapCrmScheduleDays(builder.getParams().getSchedule().getScheduleDays()))
                                    .warehouseArea(params.getWarehouseArea())
                                    .clientArea(params.getClientArea())
                                    .build())
                            .build()));
        }
        var crmPrePickupPointParams = crmPrePickupPointFactory.approve(
                builder.getCrmPrePickupPoint().getId(),
                TestCrmPrePickupPointFactory.CrmPrePickupPointTestParams.builder()
                        .cardCompensation(params.getCardCompensationRate())
                        .cashCompensation(params.getCashCompensationRate())
                        .orderTransmissionReward(params.getTransmissionReward())
                        .brandingType(params.getBrandingType())
                        .brandedSince(params.getBrandingData().getBrandedSince())
                        .brandRegion(params.getBrandingData().getBrandRegion().getRegion())
                        .brandedSince(params.getBrandingData().getBrandedSince())
                        .build());

        var shopInfo = new SimpleShopRegistrationResponse();
        shopInfo.setCampaignId(params.getPvzMarketId());
        when(mbiApiClient.simpleRegisterShop(anyLong(), anyLong(), any())).thenReturn(shopInfo);
        PickupPoint pickupPoint = pickupPointCreateService.createPickupPoint(crmPrePickupPointParams);
        pickupPoint.disable();
        if (pickupPoint.getLegalPartner().getPickupPoints() == null) {
            pickupPoint.getLegalPartner().setPickupPoints(new ArrayList<>());
        }
        if (!pickupPoint.getLegalPartner().getPickupPoints().contains(pickupPoint)) {
            pickupPoint.getLegalPartner().getPickupPoints().add(pickupPoint);
        }

        if (builder.isActivateImmediately()) {
            pickupPointDeactivationCommandService.cancelDeactivation(
                    pickupPoint.getId(), DEFAULT_FIRST_DEACTIVATION_REASON);
        }
        pickupPoint = pickupPointRepository.findByIdOrThrow(pickupPoint.getId());
        pickupPoint.setLmsId(builder.getParams().getLmsId());
        return pickupPointRepository.save(pickupPoint);
    }

    public void addFirstDeactivationReason() {
        if (deactivationReasonRepository.findFirstByReason(DEFAULT_FIRST_DEACTIVATION_REASON).isEmpty()) {
            deactivationReasonRepository.save(DeactivationReason.builder()
                    .reason(DEFAULT_FIRST_DEACTIVATION_REASON)
                    .details(PickupPoint.DEFAULT_FIRST_DEACTIVATION_REASON_DETAILS)
                    .fullDeactivation(PickupPoint.DEFAULT_FIRST_DEACTIVATION_REASON_FULL_DEACTIVATION)
                    .canBeCancelled(PickupPoint.DEFAULT_FIRST_DEACTIVATION_REASON_CAN_BE_CANCELLED)
                    .build());
        }
    }

    @Deprecated
    public PickupPoint createPickupPoint() {
        return createPickupPoint(CreatePickupPointBuilder.builder()
                .build());
    }

    @Deprecated
    public PickupPoint createPickupPoint(PickupPointTestParams params) {
        return createPickupPoint(CreatePickupPointBuilder.builder()
                .params(params)
                .build());
    }

    public PickupPoint createPickupPointFromCrm() {
        return createPickupPointFromCrm(CreatePickupPointBuilder.builder().build());
    }

    public PickupPoint createPickupPointWithCourierMapping(PickupPointTestParams params) {
        var pickupPoint = createPickupPoint(params);
        pickupPointCourierMappingFactory.create(
                TestPickupPointCourierMappingFactory.PickupPointCourierMappingTestParamsBuilder.builder()
                        .pickupPoint(pickupPoint)
                        .build());
        return pickupPoint;
    }

    public PickupPoint createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
            PickupPointBrandingType brandingType, Boolean dropOffFeature, LegalPartner partner, Integer timeOffset
    ) {
        if (partner == null) {
            partner = legalPartnerFactory.createLegalPartner();
        }
        partner = legalPartnerFactory
                .forceApprove(partner.getId(), LocalDate.of(2021, 1, 1));

        String region = null;
        if (brandingType.isBrand()) {
            region = UUID.randomUUID().toString();
            brandRegionFactory.create(TestBrandRegionFactory.BrandRegionTestParams.builder()
                    .region(region)
                    .dailyTransmissionThreshold(5)
                    .build());
        }

        PickupPoint pickupPoint = createPickupPointFromCrm(
                CreatePickupPointBuilder.builder()
                        .legalPartner(partner)
                        .params(PickupPointTestParams.builder()
                                .name("ПВЗ" + Arrays.toString(RandomUtils.nextBytes(8)))
                                .build())
                        .build());

        List<TestPickupPointFactory.PickupPointScheduleDayTestParams> scheduleDays = List.of(
                TestPickupPointFactory.PickupPointScheduleDayTestParams.builder().dayOfWeek(DayOfWeek.MONDAY).build(),
                TestPickupPointFactory.PickupPointScheduleDayTestParams.builder().dayOfWeek(DayOfWeek.TUESDAY).build(),
                TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                        .dayOfWeek(DayOfWeek.WEDNESDAY)
                        .build(),
                TestPickupPointFactory.PickupPointScheduleDayTestParams.builder().dayOfWeek(DayOfWeek.THURSDAY).build(),
                TestPickupPointFactory.PickupPointScheduleDayTestParams.builder().dayOfWeek(DayOfWeek.FRIDAY).build(),
                TestPickupPointFactory.PickupPointScheduleDayTestParams.builder().dayOfWeek(DayOfWeek.SATURDAY).build(),
                TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                        .dayOfWeek(DayOfWeek.SUNDAY)
                        .isWorkingDay(false)
                        .build()
        );
        pickupPoint = updatePickupPoint(
                pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .active(true)
                        .brandingType(brandingType)
                        .brandRegion(region)
                        .schedule(TestPickupPointFactory.PickupPointScheduleTestParams.builder()
                                .scheduleDays(scheduleDays)
                                .build())
                        .build());

        if (dropOffFeature) {
            createDropOff(pickupPoint.getId());
        }

        if (timeOffset != null) {
            var timeZone = new PickupPointTimezone((long) RandomUtils.nextInt(1, 1_000_000), timeOffset);
            pickupPointTimezoneCommandService.create(timeZone.getRegionId(), timeZone.getTimeOffset());

            pickupPointTimezoneService.updateTzOffset(pickupPoint.getId(), timeZone.getRegionId());
        }

        return pickupPointRepository.findByIdOrThrow(pickupPoint.getId());
    }

    @Data
    @Builder
    public static class CreatePickupPointBuilder {

        @Builder.Default
        private PickupPointTestParams params = PickupPointTestParams.builder().build();

        private LegalPartner legalPartner;

        private CrmPrePickupPointParams crmPrePickupPoint;

        private String ticket;

        @Builder.Default
        private boolean activateImmediately = true;
    }

    public PickupPoint updatePickupPoint(long id, PickupPointTestParams params) {
        if (params.getActive() != null) {
            var pickupPoint = pickupPointRepository.findByIdOrThrow(id);
            pickupPoint.setActive(params.getActive());
            pickupPoint.save(pickupPoint.getLegalPartner());
            pickupPointRepository.save(pickupPoint);
        }
        return pickupPointCommandService.update(id, buildPickupPoint(params));
    }

    public PickupPoint updateCalendarOverrides(long id,
                                               boolean workOnHolidays,
                                               List<DayOfWeek> weekHolidays,
                                               List<PickupPointCalendarOverrideParams> overrides) {
        pickupPointCommandService.updateCalendarOverrides(id, workOnHolidays, weekHolidays, overrides);
        return pickupPointRepository.findByIdOrThrow(id);
    }

    // TODO: we need it only for december 2020 where we calculate reward by working_days_count column
    public PickupPoint setDecemberWorkingDays(long id, int workingDaysCount) {
        var pickupPoint = pickupPointRepository.findByIdOrThrow(id);
        pickupPoint.getActualBrandData().setWorkingDaysCount(workingDaysCount);
        return pickupPointRepository.save(pickupPoint);
    }

    public PickupPoint createDropOff(long pickupPointId) {
        return pickupPointCommandService.makeDropOff(pickupPointId);
    }

    public PickupPointLocationParams mapLocation(PickupPointLocationTestParams locationTestParams) {
        return PickupPointLocationParams.builder()
                .country(locationTestParams.getCountry())
                .locality(locationTestParams.getLocality())
                .locationId(locationTestParams.getLocationId())
                .region(locationTestParams.getRegion())
                .street(locationTestParams.getStreet())
                .house(locationTestParams.getHouse())
                .building(locationTestParams.getBuilding())
                .housing(locationTestParams.getHousing())
                .zipCode(locationTestParams.getZipCode())
                .porch(locationTestParams.getPorch())
                .intercom(locationTestParams.getIntercom())
                .floor(locationTestParams.getFloor())
                .metro(locationTestParams.getMetro())
                .office(locationTestParams.getOffice())
                .lat(locationTestParams.getLat())
                .lng(locationTestParams.getLng())
                .build();
    }


    public CrmPrePickupPointLocationParams mapToCrmLocation(PickupPointLocationTestParams locationTestParams) {
        return CrmPrePickupPointLocationParams.builder()
                .country(locationTestParams.getCountry())
                .locality(locationTestParams.getLocality())
                .locationId(locationTestParams.getLocationId())
                .region(locationTestParams.getRegion())
                .street(locationTestParams.getStreet())
                .house(locationTestParams.getHouse())
                .building(locationTestParams.getBuilding())
                .housing(locationTestParams.getHousing())
                .zipCode(locationTestParams.getZipCode())
                .porch(locationTestParams.getPorch())
                .intercom(locationTestParams.getIntercom())
                .floor(locationTestParams.getFloor())
                .metro(locationTestParams.getMetro())
                .office(locationTestParams.getOffice())
                .lat(locationTestParams.getLat())
                .lng(locationTestParams.getLng())
                .build();
    }

    public PickupPointLocationTestParams mapToCrmTestLocation(PickupPointLocationTestParams locationTestParams) {
        return PickupPointLocationTestParams.builder()
                .country(locationTestParams.getCountry())
                .locality(locationTestParams.getLocality())
                .locationId(locationTestParams.getLocationId())
                .region(locationTestParams.getRegion())
                .street(locationTestParams.getStreet())
                .house(locationTestParams.getHouse())
                .building(locationTestParams.getBuilding())
                .housing(locationTestParams.getHousing())
                .zipCode(locationTestParams.getZipCode())
                .porch(locationTestParams.getPorch())
                .intercom(locationTestParams.getIntercom())
                .floor(locationTestParams.getFloor())
                .metro(locationTestParams.getMetro())
                .office(locationTestParams.getOffice())
                .lat(locationTestParams.getLat())
                .lng(locationTestParams.getLng())
                .build();
    }

    public List<PickupPointScheduleDayParams> mapScheduleDays(List<PickupPointScheduleDayTestParams> testParamsList) {
        return testParamsList.stream().map(p ->
                PickupPointScheduleDayParams.builder()
                        .dayOfWeek(p.getDayOfWeek())
                        .isWorkingDay(p.getIsWorkingDay())
                        .timeFrom(p.getTimeFrom())
                        .timeTo(p.getTimeTo())
                        .build())
                .collect(Collectors.toList());
    }

    public List<PickupPointScheduleDayTestParams> mapCrmScheduleDays(List<PickupPointScheduleDayTestParams> testParamsList) {
        return testParamsList.stream().map(p ->
                PickupPointScheduleDayTestParams.builder()
                        .dayOfWeek(p.getDayOfWeek())
                        .isWorkingDay(p.getIsWorkingDay())
                        .timeFrom(p.getTimeFrom())
                        .timeTo(p.getTimeTo())
                        .build())
                .collect(Collectors.toList());
    }

    private PickupPointParams buildPickupPoint(PickupPointTestParams params) {
        return PickupPointParams.builder()
                .name(params.getName())
                .pvzMarketId(params.getPvzMarketId())
                .location(mapLocation(params.getLocation()))
                .schedule(PickupPointScheduleParams.builder()
                        .worksOnHoliday(params.getSchedule().getWorkOnHoliday())
                        .scheduleDays(mapScheduleDays(params.getSchedule().getScheduleDays()))
                        .calendarOverrides(params.getSchedule().getOverrideDays().stream().map(p ->
                                        PickupPointCalendarOverrideParams.builder()
                                                .date(p.getDate())
                                                .isHoliday(p.getIsHoliday())
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .phone(params.getPhone())
                .cashAllowed(params.getCashAllowed())
                .prepayAllowed(params.getPrepayAllowed())
                .cardAllowed(params.getCardAllowed())
                .instruction(params.getInstruction())
                .returnAllowed(params.getReturnAllowed())
                .storagePeriod(params.getStoragePeriod())
                .maxWeight(params.getMaxWeight())
                .maxLength(params.getMaxLength())
                .maxWidth(params.getMaxWidth())
                .maxHeight(params.getMaxHeight())
                .maxSidesSum(params.getMaxSidesSum())
                .timeOffset(params.getTimeOffset())
                .active(params.getActive())
                .capacity(params.getCapacity())
                .transmissionReward(params.getTransmissionReward())
                .cashCompensationRate(params.getCashCompensationRate())
                .cardCompensationRate(params.getCardCompensationRate())
                .brandingType(params.getBrandingType())
                .brandingData(PickupPointBrandingDataParams.builder()
                        .brandedSince(params.getBrandDate())
                        .brandRegion(BrandRegionParams.builder()
                                .region(params.getBrandRegion())
                                .build())
                        .build())
                .lmsId(params.getLmsId())
                .marketShopId(params.getMarketShopId())
                .partialReturnAllowed(params.getPartialReturnAllowed())
                .clientArea(params.getClientArea())
                .warehouseArea(params.getWarehouseArea())
                .cashboxUrl(params.getCashboxUrl())
                .cashboxToken(params.getCashboxToken())
                .cashboxApiSupportAnswer(params.getCashboxApiSupportAnswer())
                .cashboxId(params.getCashboxId())
                .build();
    }

    private PickupPointParams buildPickupPoint() {
        return buildPickupPoint(PickupPointTestParams.builder().build());
    }

    @Data
    @Builder
    public static class PickupPointBuilder {

        @Builder.Default
        private PickupPointTestParams params = PickupPointTestParams.builder().build();

        private LegalPartner legalPartner;

        private DeliveryService deliveryService;

    }

    @Data
    @Builder
    public static class PickupPointTestParams {

        public static final String DEFAULT_NAME = "Подвал дома Колотушкина на улице Пушкина";
        public static final String DEFAULT_PHONE = "+7(965)127-89-48";
        public static final Boolean DEFAULT_CASH_ALLOWED = true;
        public static final Boolean DEFAULT_PREPAY_ALLOWED = true;
        public static final Boolean DEFAULT_CARD_ALLOWED = true;
        public static final String DEFAULT_INSTRUCTION = "Вход с торца здания";
        public static final Boolean DEFAULT_RETURN_ALLOWED = PickupPoint.DEFAULT_RETURN_ALLOWED;
        public static final Integer DEFAULT_STORAGE_PERIOD = PickupPoint.DEFAULT_STORAGE_PERIOD;
        public static final BigDecimal DEFAULT_WEIGHT = PickupPoint.REGION_SPECIFIC_MAX_WEIGHT;
        public static final BigDecimal DEFAULT_LENGTH = PickupPoint.REGION_SPECIFIC_MAX_LENGTH;
        public static final BigDecimal DEFAULT_WIDTH = PickupPoint.REGION_SPECIFIC_MAX_WIDTH;
        public static final BigDecimal DEFAULT_HEIGHT = PickupPoint.REGION_SPECIFIC_MAX_HEIGHT;
        public static final BigDecimal DEFAULT_MAX_SIDES_SUM = PickupPoint.DEFAULT_MAX_SIDES_SUM;
        public static final Integer DEFAULT_OFFSET = PickupPoint.DEFAULT_TIME_OFFSET;
        public static final Boolean DEFAULT_ACTIVE = PickupPoint.DEFAULT_ACTIVE;
        public static final Integer DEFAULT_CAPACITY = PickupPoint.DEFAULT_CAPACITY;
        public static final BigDecimal DEFAULT_TRANSMISSION_REWARD = BigDecimal.valueOf(45.0);
        public static final BigDecimal DEFAULT_CASH_COMPENSATION_RATE = BigDecimal.valueOf(0.003);
        public static final BigDecimal DEFAULT_CARD_COMPENSATION_RATE = BigDecimal.valueOf(0.019);
        public static final List<String> DEFAULT_PHOTOS = List.of("");
        public static final PickupPointBrandingType DEFAULT_BRANDING_TYPE = PickupPoint.DEFAULT_BRANDING_TYPE;
        public static final String DEFAULT_BRAND_REGION = DEFAULT_REGIONS.get(0).getRegion();
        public static final LocalDate DEFAULT_BRAND_DATE = LocalDate.of(2020, 12, 23);
        public static final boolean DEFAULT_DROP_OFF_FEATURE = false;
        public static final boolean DEFAULT_PARTIAL_RETURN_ALLOWED = false;
        public static final boolean DEFAULT_CASHBOX_API_SUPPORT_ANSWER = true;
        public static final String DEFAULT_CASHBOX_ID = "aqsi-329483294";

        @Builder.Default
        private String name = DEFAULT_NAME;

        @Builder.Default
        private Long pvzMarketId = RandomUtils.nextLong();

        @Builder.Default
        private PickupPointLocationTestParams location = PickupPointLocationTestParams.builder().build();

        @Builder.Default
        private PickupPointScheduleTestParams schedule = PickupPointScheduleTestParams.builder().build();

        @Builder.Default
        private String phone = DEFAULT_PHONE;

        @Builder.Default
        private Boolean cashAllowed = DEFAULT_CASH_ALLOWED;

        @Builder.Default
        private Boolean prepayAllowed = DEFAULT_PREPAY_ALLOWED;

        @Builder.Default
        private Boolean cardAllowed = DEFAULT_CARD_ALLOWED;

        @Builder.Default
        private String instruction = DEFAULT_INSTRUCTION;

        @Builder.Default
        private Boolean returnAllowed = DEFAULT_RETURN_ALLOWED;

        @Builder.Default
        private Integer storagePeriod = DEFAULT_STORAGE_PERIOD;

        @Builder.Default
        private BigDecimal maxWeight = DEFAULT_WEIGHT;

        @Builder.Default
        private BigDecimal maxLength = DEFAULT_LENGTH;

        @Builder.Default
        private BigDecimal maxWidth = DEFAULT_WIDTH;

        @Builder.Default
        private BigDecimal maxHeight = DEFAULT_HEIGHT;

        @Builder.Default
        private BigDecimal maxSidesSum = DEFAULT_MAX_SIDES_SUM;

        @Builder.Default
        private Integer timeOffset = DEFAULT_OFFSET;

        @Builder.Default
        private Boolean active = DEFAULT_ACTIVE;

        @Builder.Default
        private Integer capacity = DEFAULT_CAPACITY;

        @Builder.Default
        private List<String> photos = DEFAULT_PHOTOS;

        @Builder.Default
        private BigDecimal transmissionReward = DEFAULT_TRANSMISSION_REWARD;

        @Builder.Default
        private BigDecimal cashCompensationRate = DEFAULT_CASH_COMPENSATION_RATE;

        @Builder.Default
        private BigDecimal cardCompensationRate = DEFAULT_CARD_COMPENSATION_RATE;

        @Builder.Default
        private PickupPointBrandingType brandingType = DEFAULT_BRANDING_TYPE;

        @Builder.Default
        private String brandRegion = DEFAULT_BRAND_REGION;

        @Builder.Default
        private LocalDate brandDate = DEFAULT_BRAND_DATE;

        @Builder.Default
        private Long lmsId = RandomUtils.nextLong();

        @Builder.Default
        private Long marketShopId = RandomUtils.nextLong();

        @Builder.Default
        private Boolean partialReturnAllowed = DEFAULT_PARTIAL_RETURN_ALLOWED;

        @Builder.Default
        private BigDecimal warehouseArea = DEFAULT_WAREHOUSE_AREA;

        @Builder.Default
        private BigDecimal clientArea = DEFAULT_CLIENT_AREA;

        @Builder.Default
        private String cashboxUrl = null;

        @Builder.Default
        private String cashboxToken = null;

        @Builder.Default
        private Boolean dropOffFeature = DEFAULT_DROP_OFF_FEATURE;

        @Builder.Default
        private Boolean cashboxApiSupportAnswer = DEFAULT_CASHBOX_API_SUPPORT_ANSWER;

        @Builder.Default
        private String cashboxId = DEFAULT_CASHBOX_ID;
    }

    @Data
    @Builder
    public static class PickupPointLocationTestParams {

        public static final String DEFAULT_COUNTRY = "Российская Федерация";
        public static final String DEFAULT_LOCALITY = "Москва";
        public static final Long DEFAULT_LOCATION_ID = 213L;
        public static final String DEFAULT_REGION = "Москва и Московская область";
        public static final String DEFAULT_STREET = "Ленинградский проспект";
        public static final String DEFAULT_HOUSE = "5";
        public static final String DEFAULT_BUILDING = "7";
        public static final String DEFAULT_HOUSING = "1";
        public static final String DEFAULT_ZIP_CODE = "125040";
        public static final String DEFAULT_PORCH = "3";
        public static final String DEFAULT_INTERCOM = "1";
        public static final Integer DEFAULT_FLOOR = 1;
        public static final String DEFAULT_METRO = "Белорусская";
        public static final String DEFAULT_OFFICE = "4";
        public static final BigDecimal DEFAULT_LAT = BigDecimal.valueOf(55.77914);
        public static final BigDecimal DEFAULT_LNG = BigDecimal.valueOf(37.577921);

        @Builder.Default
        private String country = DEFAULT_COUNTRY;

        @Builder.Default
        private String locality = DEFAULT_LOCALITY;

        @Builder.Default
        private String region = DEFAULT_REGION;

        @Builder.Default
        private String street = DEFAULT_STREET;

        @Builder.Default
        private String house = DEFAULT_HOUSE;

        @Builder.Default
        private String building = DEFAULT_BUILDING;

        @Builder.Default
        private String housing = DEFAULT_HOUSING;

        @Builder.Default
        private String zipCode = DEFAULT_ZIP_CODE;

        @Builder.Default
        private String porch = DEFAULT_PORCH;

        @Builder.Default
        private String intercom = DEFAULT_INTERCOM;

        @Builder.Default
        private Integer floor = DEFAULT_FLOOR;

        @Builder.Default
        private String metro = DEFAULT_METRO;

        @Builder.Default
        private String office = DEFAULT_OFFICE;

        @Builder.Default
        private BigDecimal lat = DEFAULT_LAT;

        @Builder.Default
        private BigDecimal lng = DEFAULT_LNG;

        @Builder.Default
        private Long locationId = DEFAULT_LOCATION_ID;

    }

    @Data
    @Builder
    public static class PickupPointScheduleTestParams {

        public static final Boolean DEFAULT_WORKS_ON_HOLIDAY = PickupPoint.DEFAULT_WORKS_ON_HOLIDAY;
        public static final List<PickupPointScheduleDayTestParams> DEFAULT_SCHEDULE_DAYS = List.of(
                PickupPointScheduleDayTestParams.builder().dayOfWeek(DayOfWeek.MONDAY).build(),
                PickupPointScheduleDayTestParams.builder().dayOfWeek(DayOfWeek.TUESDAY).build(),
                PickupPointScheduleDayTestParams.builder().dayOfWeek(DayOfWeek.WEDNESDAY).build(),
                PickupPointScheduleDayTestParams.builder().dayOfWeek(DayOfWeek.THURSDAY).build(),
                PickupPointScheduleDayTestParams.builder().dayOfWeek(DayOfWeek.FRIDAY).build(),
                PickupPointScheduleDayTestParams.builder().dayOfWeek(DayOfWeek.SATURDAY).build(),
                PickupPointScheduleDayTestParams.builder().dayOfWeek(DayOfWeek.SUNDAY).build()
        );

        @Builder.Default
        private Boolean workOnHoliday = DEFAULT_WORKS_ON_HOLIDAY;

        @Builder.Default
        private List<PickupPointScheduleDayTestParams> scheduleDays = DEFAULT_SCHEDULE_DAYS;

        @Builder.Default
        private List<PickupPointCalendarOverrideTestParams> overrideDays = Collections.emptyList();

    }

    @Data
    @Builder
    public static class PickupPointScheduleDayTestParams {

        public static final DayOfWeek DEFAULT_DAY_OF_WEEK = DayOfWeek.MONDAY;
        public static final Boolean DEFAULT_IS_WORKING_DAY = true;
        public static final LocalTime DEFAULT_TIME_FROM = LocalTime.of(9, 0);
        public static final LocalTime DEFAULT_TIME_TO = LocalTime.of(21, 0);

        @Builder.Default
        private DayOfWeek dayOfWeek = DEFAULT_DAY_OF_WEEK;

        @Builder.Default
        private Boolean isWorkingDay = DEFAULT_IS_WORKING_DAY;

        @Builder.Default
        private LocalTime timeFrom = DEFAULT_TIME_FROM;

        @Builder.Default
        private LocalTime timeTo = DEFAULT_TIME_TO;
    }

    @Data
    @Builder
    public static class PickupPointCalendarOverrideTestParams {

        @Builder.Default
        private LocalDate date = LocalDate.of(2020, 4, 1);

        @Builder.Default
        private Boolean isHoliday = false;

    }

}
