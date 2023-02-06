package ru.yandex.market.pvz.internal.domain.pickup_point;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import ru.yandex.market.pvz.client.model.approve.PrePickupPointApproveStatus;
import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.core.domain.approve.crm_pre_pickup_point.CrmPrePickupPointCommandService;
import ru.yandex.market.pvz.core.domain.approve.pre_legal_partner.PreLegalPartnerParams;
import ru.yandex.market.pvz.core.domain.approve.pre_legal_partner.PreLegalPartnerQueryService;
import ru.yandex.market.pvz.core.domain.consumable.ConsumableQueryService;
import ru.yandex.market.pvz.core.domain.consumable.request.item.ConsumableRequestItemParams;
import ru.yandex.market.pvz.core.domain.consumable.type.ConsumableTypeParams;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerParams;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerQueryService;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerRequestData;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointQueryService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointSimpleParams;
import ru.yandex.market.pvz.core.domain.pickup_point.PrePickupPointRequestData;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.DeactivationReasonCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.PickupPointDeactivationCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.params.DeactivationReasonParams;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestBrandRegionFactory;
import ru.yandex.market.pvz.core.test.factory.TestConsumableTypeFactory;
import ru.yandex.market.pvz.core.test.factory.TestCrmPrePickupPointFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointCourierMappingFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.CreatePickupPointBuilder;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams;
import ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory;
import ru.yandex.market.pvz.internal.PvzIntTest;
import ru.yandex.market.pvz.internal.controller.pi.consumable.dto.ConsumableItemDto;
import ru.yandex.market.pvz.internal.controller.pi.consumable.dto.ConsumableRequestDto;
import ru.yandex.market.pvz.internal.controller.pi.pickup_point.dto.IncompletePrePickupPointDto;
import ru.yandex.market.pvz.internal.controller.pi.pickup_point.dto.LegalPartnerPickupPointDto;
import ru.yandex.market.pvz.internal.controller.pi.pickup_point.dto.LegalPartnerPickupPointsInfoDto;
import ru.yandex.market.pvz.internal.controller.pi.pickup_point.dto.PagePickupPointDto;
import ru.yandex.market.pvz.internal.controller.pi.pickup_point.dto.PickupPointActionDto;
import ru.yandex.market.pvz.internal.controller.pi.pickup_point.dto.PickupPointActionType;
import ru.yandex.market.pvz.internal.controller.pi.pickup_point.dto.PickupPointActivationStatus;
import ru.yandex.market.pvz.internal.controller.pi.pickup_point.dto.PickupPointBrandingTypeDto;
import ru.yandex.market.pvz.internal.controller.pi.pickup_point.dto.PickupPointLocationDto;
import ru.yandex.market.pvz.internal.controller.pi.pickup_point.dto.PickupPointReportParamsDto;
import ru.yandex.market.pvz.internal.controller.pi.pickup_point.dto.PickupPointScheduleDaysDto;
import ru.yandex.market.pvz.internal.controller.pi.pickup_point.dto.PrePickupPointDto;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType.FULL;
import static ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType.NONE;
import static ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint.DEFAULT_ACTIVE;
import static ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint.DEFAULT_CAPACITY;
import static ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint.DEFAULT_FIRST_DEACTIVATION_REASON;
import static ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint.DEFAULT_MAX_HEIGHT;
import static ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint.DEFAULT_MAX_LENGTH;
import static ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint.DEFAULT_MAX_WEIGHT;
import static ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint.DEFAULT_MAX_WIDTH;
import static ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint.DEFAULT_STORAGE_PERIOD;
import static ru.yandex.market.pvz.core.test.factory.TestCrmPrePickupPointFactory.CrmPrePickupPointTestParams;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_BUILDING;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_COUNTRY;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_FLOOR;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_HOUSE;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_HOUSING;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_INTERCOM;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_LAT;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_LNG;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_LOCALITY;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_METRO;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_OFFICE;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_PORCH;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_REGION;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_STREET;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_ZIP_CODE;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointScheduleDayTestParams.DEFAULT_IS_WORKING_DAY;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointScheduleDayTestParams.DEFAULT_TIME_FROM;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointScheduleDayTestParams.DEFAULT_TIME_TO;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_CARD_ALLOWED;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_CASH_ALLOWED;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_HEIGHT;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_INSTRUCTION;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_LENGTH;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_MAX_SIDES_SUM;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_PHONE;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_PREPAY_ALLOWED;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_WEIGHT;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_WIDTH;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_PICKUP_POINT_LAT;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_PICKUP_POINT_LOCALITY;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_PICKUP_POINT_LON;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_PICKUP_POINT_REGION;
import static ru.yandex.market.pvz.internal.controller.pi.pickup_point.dto.PickupPointActivationStatus.ACTIVATED;
import static ru.yandex.market.pvz.internal.controller.pi.pickup_point.dto.PickupPointActivationStatus.DEACTIVATED;
import static ru.yandex.market.pvz.internal.controller.pi.pickup_point.dto.PickupPointActivationStatus.LIMITED_ACTIVATED;
import static ru.yandex.market.pvz.internal.controller.pi.pickup_point.dto.PickupPointActivationStatus.WAITING_FOR_ACTIVATION;
import static ru.yandex.market.pvz.internal.controller.pi.pickup_point.spec.PickupPointListViewV2Specification.ACTIVATION_STATUS_PROPERTY_FOR_SORT;
import static ru.yandex.market.pvz.internal.domain.pickup_point.LegalPartnerPickupPointService.INCOMPLETE_BRANDING_TYPE;
import static ru.yandex.market.pvz.internal.domain.pickup_point.LegalPartnerPickupPointService.INCOMPLETE_STATUS;

@PvzIntTest
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LegalPartnerPickupPointServiceTest {

    private static final String NEW_NAME = "Новое имя";
    private static final String INCOMPLETE_PRE_PICKUP_POINT_NAME = "Дозаполните информацию";
    private static final BigDecimal WAREHOUSE_AREA = BigDecimal.valueOf(12.3);
    private static final BigDecimal CLIENT_AREA = BigDecimal.valueOf(66.3);

    private final TestBrandRegionFactory brandRegionFactory;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestPreLegalPartnerFactory preLegalPartnerFactory;
    private final TestCrmPrePickupPointFactory crmPrePickupPointFactory;
    private final TestPickupPointCourierMappingFactory pickupPointCourierMappingFactory;

    private final LegalPartnerPickupPointService legalPartnerPickupPointService;
    private final CrmPrePickupPointCommandService crmPrePickupPointCommandService;
    private final LegalPartnerQueryService legalPartnerQueryService;
    private final PreLegalPartnerQueryService preLegalPartnerQueryService;
    private final DeactivationReasonCommandService deactivationReasonCommandService;
    private final PickupPointDeactivationCommandService pickupPointDeactivationCommandService;
    private final PickupPointQueryService pickupPointQueryService;
    private final ConsumableQueryService consumableQueryService;
    private final TestConsumableTypeFactory consumableTypeFactory;

    @Test
    void getCrmPrePickupPoint() {
        var legalPartner = legalPartnerFactory.createLegalPartner();
        var prePickupPoint = crmPrePickupPointFactory.create(
                TestCrmPrePickupPointFactory.CrmPrePickupPointTestParamsBuilder.builder()
                        .legalPartnerId(legalPartner.getId())
                        .build());

        var actual = legalPartnerPickupPointService.get(prePickupPoint.getId());
        LegalPartnerPickupPointDto expected = LegalPartnerPickupPointDto.builder()
                .prePickupPointId(prePickupPoint.getId())
                .pvzMarketId(null)
                .deliveryServiceId(null)
                .legalPartnerId(legalPartner.getId())
                .approveStatus(PrePickupPointApproveStatus.CHECKING)
                .active(false)
                .prePickupPoint(buildExpectedCrmPrePickupPoint(actual))
                .capacity(DEFAULT_CAPACITY)
                .storagePeriod(DEFAULT_STORAGE_PERIOD)
                .brandType(PickupPointBrandingTypeDto.REQUEST)
                .warehouseArea(actual.getWarehouseArea())
                .clientArea(actual.getClientArea())
                .build();

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void getIncompletePrePickupPoint() {
        var preLegalPartner = preLegalPartnerFactory.createPreLegalPartner(
                TestPreLegalPartnerFactory.PreLegalPartnerTestParams.builder()
                        .wantBrand(true)
                        .build());

        preLegalPartner = preLegalPartnerFactory.createLegalPartnerForLead(preLegalPartner.getId());

        var incompletePickupPoint = legalPartnerPickupPointService.getIncomplete(preLegalPartner.getLegalPartnerId());

        assertThat(incompletePickupPoint).isEqualTo(IncompletePrePickupPointDto.builder()
                .approveStatus(INCOMPLETE_STATUS)
                .locality(DEFAULT_PICKUP_POINT_LOCALITY)
                .region(DEFAULT_PICKUP_POINT_REGION)
                .brandType(INCOMPLETE_BRANDING_TYPE)
                .lat(DEFAULT_PICKUP_POINT_LAT)
                .lon(DEFAULT_PICKUP_POINT_LON)
                .build());
    }

    private PrePickupPointDto buildExpectedPrePickupPoint(LegalPartnerPickupPointDto actual) {
        return PrePickupPointDto.builder()
                .name(actual.getPrePickupPoint().getName())
                .location(buildExpectedLocation())
                .scheduleDays(buildExpectedScheduleDays())
                .phone(DEFAULT_PHONE)
                .prepayAllowed(DEFAULT_PREPAY_ALLOWED)
                .cashAllowed(DEFAULT_CASH_ALLOWED)
                .cardAllowed(DEFAULT_CARD_ALLOWED)
                .instruction(DEFAULT_INSTRUCTION)
                .maxWeight(DEFAULT_WEIGHT)
                .maxLength(DEFAULT_LENGTH)
                .maxWidth(DEFAULT_WIDTH)
                .maxHeight(DEFAULT_HEIGHT)
                .maxSidesSum(DEFAULT_MAX_SIDES_SUM)
                .warehouseArea(actual.getWarehouseArea())
                .clientArea(actual.getClientArea())
                .build();
    }

    private PrePickupPointDto buildExpectedCrmPrePickupPoint(LegalPartnerPickupPointDto actual) {
        return PrePickupPointDto.builder()
                .name(actual.getPrePickupPoint().getName())
                .location(buildExpectedLocation())
                .scheduleDays(buildExpectedScheduleDays())
                .phone(CrmPrePickupPointTestParams.DEFAULT_PHONE)
                .prepayAllowed(CrmPrePickupPointTestParams.DEFAULT_PREPAY_ALLOWED)
                .cashAllowed(CrmPrePickupPointTestParams.DEFAULT_CASH_ALLOWED)
                .cardAllowed(CrmPrePickupPointTestParams.DEFAULT_CARD_ALLOWED)
                .instruction(CrmPrePickupPointTestParams.DEFAULT_INSTRUCTION)
                .maxWeight(PickupPoint.DEFAULT_MAX_WEIGHT)
                .maxLength(PickupPoint.DEFAULT_MAX_LENGTH)
                .maxWidth(PickupPoint.DEFAULT_MAX_WIDTH)
                .maxHeight(PickupPoint.DEFAULT_MAX_HEIGHT)
                .maxSidesSum(PickupPoint.DEFAULT_MAX_SIDES_SUM)
                .brandingTypeWanted(CrmPrePickupPointTestParams.DEFAULT_WANT_TO_BE_BRANDED)
                .square(CrmPrePickupPointTestParams.DEFAULT_SQUARE)
                .ceilingHeight(CrmPrePickupPointTestParams.DEFAULT_CEILING_HEIGHT)
                .photoUrl(CrmPrePickupPointTestParams.DEFAULT_PHOTO_URL)
                .comment(CrmPrePickupPointTestParams.DEFAULT_COMMENT)
                .scoring(CrmPrePickupPointTestParams.DEFAULT_SCORING)
                .polygonId(CrmPrePickupPointTestParams.DEFAULT_POLYGON_ID)
                .hasStreetEntrance(CrmPrePickupPointTestParams.DEFAULT_HAS_STREET_ENTRANCE)
                .hasSeparateEntrance(CrmPrePickupPointTestParams.DEFAULT_HAS_SEPARATE_ENTRANCE)
                .hasWindows(CrmPrePickupPointTestParams.DEFAULT_HAS_WINDOWS)
                .warehouseArea(actual.getWarehouseArea())
                .clientArea(actual.getClientArea())
                .build();
    }

    private PickupPointLocationDto buildExpectedLocation() {
        return PickupPointLocationDto.builder()
                .country(DEFAULT_COUNTRY)
                .locality(DEFAULT_LOCALITY)
                .region(DEFAULT_REGION)
                .street(DEFAULT_STREET)
                .house(DEFAULT_HOUSE)
                .building(DEFAULT_BUILDING)
                .housing(DEFAULT_HOUSING)
                .zipCode(DEFAULT_ZIP_CODE)
                .porch(DEFAULT_PORCH)
                .intercom(DEFAULT_INTERCOM)
                .floor(DEFAULT_FLOOR)
                .metro(DEFAULT_METRO)
                .office(DEFAULT_OFFICE)
                .lat(DEFAULT_LAT)
                .lng(DEFAULT_LNG)
                .build();
    }

    private List<PickupPointScheduleDaysDto> buildExpectedScheduleDays() {
        return List.of(
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.MONDAY)
                        .isWorkingDay(DEFAULT_IS_WORKING_DAY)
                        .timeFrom(DEFAULT_TIME_FROM)
                        .timeTo(DEFAULT_TIME_TO)
                        .build(),
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.TUESDAY)
                        .isWorkingDay(DEFAULT_IS_WORKING_DAY)
                        .timeFrom(DEFAULT_TIME_FROM)
                        .timeTo(DEFAULT_TIME_TO)
                        .build(),
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.WEDNESDAY)
                        .isWorkingDay(DEFAULT_IS_WORKING_DAY)
                        .timeFrom(DEFAULT_TIME_FROM)
                        .timeTo(DEFAULT_TIME_TO)
                        .build(),
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.THURSDAY)
                        .isWorkingDay(DEFAULT_IS_WORKING_DAY)
                        .timeFrom(DEFAULT_TIME_FROM)
                        .timeTo(DEFAULT_TIME_TO)
                        .build(),
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.FRIDAY)
                        .isWorkingDay(DEFAULT_IS_WORKING_DAY)
                        .timeFrom(DEFAULT_TIME_FROM)
                        .timeTo(DEFAULT_TIME_TO)
                        .build(),
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.SATURDAY)
                        .isWorkingDay(DEFAULT_IS_WORKING_DAY)
                        .timeFrom(DEFAULT_TIME_FROM)
                        .timeTo(DEFAULT_TIME_TO)
                        .build(),
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.SUNDAY)
                        .isWorkingDay(DEFAULT_IS_WORKING_DAY)
                        .timeFrom(DEFAULT_TIME_FROM)
                        .timeTo(DEFAULT_TIME_TO)
                        .build()
        );
    }

    @Test
    void tryToGetNotExistentPrePickupPoint() {
        assertThatThrownBy(() -> legalPartnerPickupPointService.get(1L))
                .isExactlyInstanceOf(TplEntityNotFoundException.class);
    }

    @Test
    void createInDifferentFlows() {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();

        var result = legalPartnerPickupPointService.createPrePickupPoint(
                buildRequestData(legalPartner),
                generateDto().toBuilder().warehouseArea(WAREHOUSE_AREA).clientArea(CLIENT_AREA).build());

        assertThat(result).isEqualTo(buildExpectedDto(result, legalPartner));
        assertThat(result.getPrePickupPointId()).isPositive();
    }

    @Test
    void testGetAll() {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();

        var requestData = buildRequestData(legalPartner);

        var result = legalPartnerPickupPointService.createPrePickupPoint(requestData, generateDto());
        var pickupPoints = legalPartnerPickupPointService.getAll(requestData, null, PageRequest.of(0, 1000))
                .get()
                .collect(Collectors.toList());

        assertThat(pickupPoints).hasSize(1);
        assertThat(pickupPoints.get(0)).isEqualTo(PagePickupPointDto.builder()
                .id(result.getPrePickupPointId())
                .active(result.isActive())
                .name(result.getPrePickupPoint().getName())
                .pvzMarketId(result.getPvzMarketId())
                .brandingType(PickupPointBrandingType.FULL)
                .requestStatus(result.getApproveStatus())
                .actions(Collections.emptyList())
                .activationStatus(WAITING_FOR_ACTIVATION)
                .build());
    }

    @Test
    void testAllFilters() {
        var legalPartner = legalPartnerFactory.createLegalPartner();
        var deactivationReason = deactivationReasonCommandService.createDeactivationReason(
                "Ворует наши заказы",
                "Ворует ВСЕ наши заказы",
                true, true,
                "Ворует заказы и в логистике тоже"
        );
        var activePickupPoint = pickupPointFactory.createPickupPointFromCrm(CreatePickupPointBuilder.builder()
                .legalPartner(legalPartner)
                .params(PickupPointTestParams.builder().name("ПВЗ 1").brandingType(NONE).build())
                .build());
        var dropOffPickupPoint = pickupPointFactory.createPickupPointFromCrm(CreatePickupPointBuilder.builder()
                .legalPartner(legalPartner)
                .params(PickupPointTestParams.builder().name("ПВЗ 2").build())
                .activateImmediately(false)
                .build());
        var fdrPickupPoint = pickupPointFactory.createPickupPointFromCrm(CreatePickupPointBuilder.builder()
                .legalPartner(legalPartner)
                .params(PickupPointTestParams.builder().name("ПВЗ 3").build())
                .activateImmediately(false)
                .build());
        var deactPickupPoint = pickupPointFactory.createPickupPointFromCrm(CreatePickupPointBuilder.builder()
                .legalPartner(legalPartner)
                .params(PickupPointTestParams.builder().name("ПВЗ 4").build())
                .build());

        pickupPointFactory.createDropOff(dropOffPickupPoint.getId());
        pickupPointDeactivationCommandService.deactivate(deactPickupPoint.getId(), deactivationReason.getReason());

        var requestData = buildRequestData(legalPartner);
        var nullAsDto = getAllPickupPoints(requestData, null);

        var approvedPickupPoints = getAllPickupPoints(requestData, new PickupPointReportParamsDto("", null, "APPROVED", null));
        var activePickupPoints = getAllPickupPoints(requestData, new PickupPointReportParamsDto("", "ACTIVATED", null, null));
        var allActivateStatusesPP = getAllPickupPoints(requestData, new PickupPointReportParamsDto("", "ACTIVATED,WAITING_FOR_ACTIVATION,LIMITED_ACTIVATED,DEACTIVATED", null, null));
        var deactivatedPP =
                getAllPickupPoints(requestData, new PickupPointReportParamsDto("", "DEACTIVATED", null, null));
        var limitedPP = getAllPickupPoints(requestData, new PickupPointReportParamsDto("", "LIMITED_ACTIVATED", null, null));
        var waitingForActivationPP = getAllPickupPoints(requestData, new PickupPointReportParamsDto("", "WAITING_FOR_ACTIVATION", null, null));
        var searchByNamePP = getAllPickupPoints(requestData, new PickupPointReportParamsDto("ПВЗ 3", null, null, null));
        var noneBrandTypePP = getAllPickupPoints(requestData, new PickupPointReportParamsDto("", null, null, "NONE"));

        assertThat(approvedPickupPoints).hasSize(4);
        assertThat(allActivateStatusesPP).hasSize(4);
        assertThat(limitedPP).hasSize(1);
        assertThat(deactivatedPP).hasSize(1);

        assertThat(searchByNamePP.get(0).getName()).isEqualTo("ПВЗ 3");

        assertThat(noneBrandTypePP.get(0).getBrandingType()).isEqualTo(NONE);

        assertThat(allActivateStatusesPP.get(1).getActivationStatus()).isEqualTo(LIMITED_ACTIVATED);
        assertThat(activePickupPoints.get(0).getActivationStatus()).isEqualTo(PickupPointActivationStatus.ACTIVATED);
        assertThat(limitedPP.get(0).getActivationStatus()).isEqualTo(LIMITED_ACTIVATED);
        assertThat(deactivatedPP.get(0).getActivationStatus()).isEqualTo(DEACTIVATED);
        assertThat(waitingForActivationPP.get(0).getActivationStatus()).isEqualTo(WAITING_FOR_ACTIVATION);
    }

    private List<PagePickupPointDto> getAllPickupPoints(LegalPartnerRequestData requestData, PickupPointReportParamsDto reportParamDto) {
        return legalPartnerPickupPointService.getAll(requestData, reportParamDto, PageRequest.of(0, 1000)).get()
                .collect(Collectors.toList());
    }

    @Test
    void testGetInDifferentStatuses() {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        DeactivationReasonParams deactivationReason = deactivationReasonCommandService.createDeactivationReason(
                "Ворует наши заказы",
                "Ворует ВСЕ наши заказы",
                true, true,
                "Ворует заказы и в логистике тоже"
        );

        PickupPoint activePickupPoint = pickupPointFactory.createPickupPointFromCrm(CreatePickupPointBuilder.builder()
                .legalPartner(legalPartner)
                .params(PickupPointTestParams.builder().name("ПВЗ 1").build())
                .build());
        PickupPoint dropOffPickupPoint = pickupPointFactory.createPickupPointFromCrm(CreatePickupPointBuilder.builder()
                .legalPartner(legalPartner)
                .params(PickupPointTestParams.builder().name("ПВЗ 2").build())
                .activateImmediately(false)
                .build());
        PickupPoint fdrPickupPoint = pickupPointFactory.createPickupPointFromCrm(CreatePickupPointBuilder.builder()
                .legalPartner(legalPartner)
                .params(PickupPointTestParams.builder().name("ПВЗ 3").build())
                .activateImmediately(false)
                .build());
        PickupPoint deactPickupPoint = pickupPointFactory.createPickupPointFromCrm(CreatePickupPointBuilder.builder()
                .legalPartner(legalPartner)
                .params(PickupPointTestParams.builder().name("ПВЗ 4").build())
                .build());

        pickupPointFactory.createDropOff(dropOffPickupPoint.getId());
        pickupPointDeactivationCommandService.deactivate(deactPickupPoint.getId(), deactivationReason.getReason());

        Map<Long, PickupPointActivationStatus> pickupPointStatuses = legalPartnerPickupPointService.getAll(
                buildRequestData(legalPartner), null, PageRequest.of(0, 1000)
        ).get().collect(Collectors.toMap(
                PagePickupPointDto::getPvzMarketId,
                PagePickupPointDto::getActivationStatus
        ));

        assertThat(pickupPointStatuses).containsExactlyInAnyOrderEntriesOf(Map.of(
                activePickupPoint.getPvzMarketId(), ACTIVATED,
                dropOffPickupPoint.getPvzMarketId(), LIMITED_ACTIVATED,
                fdrPickupPoint.getPvzMarketId(), WAITING_FOR_ACTIVATION,
                deactPickupPoint.getPvzMarketId(), DEACTIVATED
        ));
    }

    @Test
    void testGetAllWithoutPrePickupPoints() {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();

        var result = legalPartnerPickupPointService.createPrePickupPoint(buildRequestData(legalPartner), generateDto());
        var pickupPoints = legalPartnerPickupPointService.getAll(buildRequestData(legalPartner), null, PageRequest.of(0, 1000))
                .get()
                .collect(Collectors.toList());

        assertThat(pickupPoints).hasSize(1);
        assertThat(pickupPoints.get(0)).isEqualTo(PagePickupPointDto.builder()
                .id(result.getPrePickupPointId())
                .active(result.isActive())
                .name(result.getPrePickupPoint().getName())
                .pvzMarketId(result.getPvzMarketId())
                .brandingType(PickupPointBrandingType.FULL)
                .requestStatus(result.getApproveStatus())
                .actions(Collections.emptyList())
                .activationStatus(WAITING_FOR_ACTIVATION)
                .build());
    }

    @Test
    void testGetAllWithIncomplete() {
        var preLegalPartner = preLegalPartnerFactory.createPreLegalPartner(
                TestPreLegalPartnerFactory.PreLegalPartnerTestParams.builder()
                        .wantBrand(true)
                        .build());

        preLegalPartner = preLegalPartnerFactory.createLegalPartnerForLead(preLegalPartner.getId());
        LegalPartnerParams legalPartner = legalPartnerQueryService.get(preLegalPartner.getLegalPartnerId());

        var pickupPoints = legalPartnerPickupPointService.getAll(buildRequestData(legalPartner), null, PageRequest.of(0, 1000))
                .get()
                .collect(Collectors.toList());

        assertThat(pickupPoints).hasSize(1);
        assertThat(pickupPoints.get(0)).isEqualTo(PagePickupPointDto.builder()
                .id(0L)
                .active(false)
                .name(INCOMPLETE_PRE_PICKUP_POINT_NAME)
                .pvzMarketId(null)
                .brandingType(PickupPointBrandingType.FULL)
                .requestStatus(PrePickupPointApproveStatus.DATA_COMPLETION_REQUIRED)
                .actions(List.of(new PickupPointActionDto(PickupPointActionType.COMPLETE_DATA)))
                .activationStatus(WAITING_FOR_ACTIVATION)
                .build());
    }

    @Test
    void getAllPickupPointsPageable() {
        brandRegionFactory.createDefaults();

        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        DeactivationReasonParams deactivationReason = deactivationReasonCommandService.createDeactivationReason(
                "Ворует наши заказы",
                "Ворует ВСЕ наши заказы",
                true, true,
                "Ворует заказы и в логистике тоже"
        );

        PickupPoint activePickupPoint = pickupPointFactory.createPickupPointFromCrm(CreatePickupPointBuilder.builder()
                .legalPartner(legalPartner)
                .params(PickupPointTestParams.builder().name("ПВЗ Петров").build())
                .build());
        PickupPoint dropOffPickupPoint = pickupPointFactory.createPickupPointFromCrm(CreatePickupPointBuilder.builder()
                .legalPartner(legalPartner)
                .params(PickupPointTestParams.builder()
                        .name("ПВЗ Сидоров")
                        .brandingType(FULL)
                        .build())
                .activateImmediately(false)
                .build());
        PickupPoint fdrPickupPoint = pickupPointFactory.createPickupPointFromCrm(CreatePickupPointBuilder.builder()
                .legalPartner(legalPartner)
                .params(PickupPointTestParams.builder().name("ПВЗ Иванов").build())
                .activateImmediately(false)
                .build());
        PickupPoint deactPickupPoint = pickupPointFactory.createPickupPointFromCrm(CreatePickupPointBuilder.builder()
                .legalPartner(legalPartner)
                .params(PickupPointTestParams.builder().name("ПВЗ Степанов").build())
                .build());

        pickupPointFactory.createDropOff(dropOffPickupPoint.getId());
        pickupPointDeactivationCommandService.deactivate(deactPickupPoint.getId(), deactivationReason.getReason());

        var pageable = legalPartnerPickupPointService.getAll(
                buildRequestData(legalPartner), null, PageRequest.of(0, 1));
        assertThat(pageable.getTotalElements()).isEqualTo(4L);
        assertThat(pageable.getTotalPages()).isEqualTo(4);
        assertThat(pageable.getSize()).isEqualTo(1);
        assertThat(pageable.getNumberOfElements()).isEqualTo(1);
        assertThat(pageable.isLast()).isFalse();
        assertThat(pageable.isFirst()).isTrue();

        pageable = legalPartnerPickupPointService.getAll(buildRequestData(legalPartner), null, PageRequest.of(3, 1));
        assertThat(pageable.isLast()).isTrue();

        pageable = legalPartnerPickupPointService.getAll(
                buildRequestData(legalPartner), null, PageRequest.of(0, 1000, Sort.by("name")));
        var iter = pageable.iterator();
        List<Long> actualIds = new ArrayList<>();
        while (iter.hasNext()) {
            actualIds.add(iter.next().getPvzMarketId());
        }
        assertThat(actualIds).containsExactly(
                fdrPickupPoint.getPvzMarketId(),
                activePickupPoint.getPvzMarketId(),
                dropOffPickupPoint.getPvzMarketId(),
                deactPickupPoint.getPvzMarketId()
        );

        pageable = legalPartnerPickupPointService.getAll(
                buildRequestData(legalPartner), null, PageRequest.of(0, 1000, Sort.by("brandingType")));
        iter = pageable.iterator();
        actualIds = new ArrayList<>();
        while (iter.hasNext()) {
            actualIds.add(iter.next().getPvzMarketId());
        }
        assertThat(actualIds).containsExactly(
                dropOffPickupPoint.getPvzMarketId(),
                activePickupPoint.getPvzMarketId(),
                fdrPickupPoint.getPvzMarketId(),
                deactPickupPoint.getPvzMarketId()
        );

        pageable = legalPartnerPickupPointService.getAll(
                buildRequestData(legalPartner), null, PageRequest.of(0, 1000, Sort.by("approveStatus")));
        iter = pageable.iterator();
        actualIds = new ArrayList<>();
        while (iter.hasNext()) {
            actualIds.add(iter.next().getPvzMarketId());
        }
        assertThat(actualIds).containsExactly(
                activePickupPoint.getPvzMarketId(),
                dropOffPickupPoint.getPvzMarketId(),
                fdrPickupPoint.getPvzMarketId(),
                deactPickupPoint.getPvzMarketId()
        );

        pageable = legalPartnerPickupPointService.getAll(
                buildRequestData(legalPartner), null, PageRequest.of(0, 1000,
                        Sort.by(ACTIVATION_STATUS_PROPERTY_FOR_SORT)));
        iter = pageable.iterator();
        actualIds = new ArrayList<>();
        while (iter.hasNext()) {
            actualIds.add(iter.next().getPvzMarketId());
        }
        assertThat(actualIds).containsExactly(
                activePickupPoint.getPvzMarketId(),
                deactPickupPoint.getPvzMarketId(),
                dropOffPickupPoint.getPvzMarketId(),
                fdrPickupPoint.getPvzMarketId()
        );

        pageable = legalPartnerPickupPointService.getAll(
                buildRequestData(legalPartner), null,
                PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, ACTIVATION_STATUS_PROPERTY_FOR_SORT)));
        iter = pageable.iterator();
        actualIds = new ArrayList<>();
        while (iter.hasNext()) {
            actualIds.add(iter.next().getPvzMarketId());
        }
        assertThat(actualIds).containsExactly(
                fdrPickupPoint.getPvzMarketId(),
                dropOffPickupPoint.getPvzMarketId(),
                deactPickupPoint.getPvzMarketId(),
                activePickupPoint.getPvzMarketId()
        );
    }

    private LegalPartnerRequestData buildRequestData(LegalPartner legalPartner) {
        return new LegalPartnerRequestData(
                legalPartner.getId(),
                legalPartner.getPartnerId(),
                legalPartner.getOrganization().getFullName(),
                legalPartner.getOwnerUid()
        );
    }

    private LegalPartnerRequestData buildRequestData(LegalPartnerParams legalPartner) {
        return new LegalPartnerRequestData(
                legalPartner.getId(),
                legalPartner.getPartnerId(),
                legalPartner.getOrganization().getFullName(),
                legalPartner.getOwnerUid()
        );
    }

    @Test
    void getPickupPointsInfoWithNoPickupPoints() {
        var legalPartner = legalPartnerFactory.createLegalPartner();

        var expected = LegalPartnerPickupPointsInfoDto.builder().hasNoPickupPoints(true).build();
        var actual = legalPartnerPickupPointService.getPickupPointsInfo(legalPartner.getId());
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void getPickupPointsInfoWithCrmPrePickupPoint() {
        var legalPartner = legalPartnerFactory.createLegalPartner();
        var prePickupPoint = crmPrePickupPointFactory.create(
                TestCrmPrePickupPointFactory.CrmPrePickupPointTestParamsBuilder.builder()
                        .legalPartnerId(legalPartner.getId())
                        .build()
        );
        var expected = LegalPartnerPickupPointsInfoDto.builder().hasNoPickupPoints(false).build();
        var actual = legalPartnerPickupPointService.getPickupPointsInfo(legalPartner.getId());
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void getPickupPointsInfoWithPickupPoint() {
        var legalPartner = legalPartnerFactory.createLegalPartner();
        var pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                CreatePickupPointBuilder.builder()
                        .legalPartner(legalPartner)
                        .build()
        );
        var expected = LegalPartnerPickupPointsInfoDto.builder().hasNoPickupPoints(false).build();
        var actual = legalPartnerPickupPointService.getPickupPointsInfo(legalPartner.getId());
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void getPickupPointsInfoWithIncompletePrePickupPoint() {
        var preLegalPartner = preLegalPartnerFactory.createPreLegalPartner(
                TestPreLegalPartnerFactory.PreLegalPartnerTestParams.builder()
                        .wantBrand(true)
                        .build());

        preLegalPartner = preLegalPartnerFactory.createLegalPartnerForLead(preLegalPartner.getId());
        var legalPartner = legalPartnerQueryService.get(preLegalPartner.getLegalPartnerId());

        var expected = LegalPartnerPickupPointsInfoDto.builder().hasNoPickupPoints(false).build();
        var actual = legalPartnerPickupPointService.getPickupPointsInfo(legalPartner.getId());
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void testUpdateInDifferentFlows() {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();

        var result = legalPartnerPickupPointService.createPrePickupPoint(buildRequestData(legalPartner), generateDto());
        var expected = buildExpectedDto(result, legalPartner);

        // To allow editing
        rejectPickupPoint(result);

        result = legalPartnerPickupPointService.updatePrePickupPoint(
                buildRequestData(expected),
                generateDto().toBuilder()
                        .name(NEW_NAME)
                        .clientArea(CLIENT_AREA)
                        .warehouseArea(WAREHOUSE_AREA)
                        .build()
        );

        expected.getPrePickupPoint().setName(NEW_NAME);
        expected.setApproveStatus(PrePickupPointApproveStatus.REJECTED);
        expected.setWarehouseArea(WAREHOUSE_AREA);
        expected.setClientArea(CLIENT_AREA);
        var prePickupPoint = expected.getPrePickupPoint();
        prePickupPoint.setClientArea(CLIENT_AREA);
        prePickupPoint.setWarehouseArea(WAREHOUSE_AREA);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void testSendToCheck() {
        PreLegalPartnerParams preLegalPartner = preLegalPartnerFactory.createPreLegalPartner();
        preLegalPartner = preLegalPartnerFactory.createLegalPartnerForLead(preLegalPartner.getId());
        LegalPartnerParams legalPartner = legalPartnerQueryService.get(preLegalPartner.getLegalPartnerId());

        var result = legalPartnerPickupPointService.createPrePickupPoint(buildRequestData(legalPartner), generateDto());
        rejectPickupPoint(result);

        result = legalPartnerPickupPointService.sendPrePickupPointToCheck(getId(result));
        assertThat(result.getApproveStatus()).isEqualTo(PrePickupPointApproveStatus.CHECKING);
    }

    private void rejectPickupPoint(LegalPartnerPickupPointDto result) {
        crmPrePickupPointCommandService.changeStatus(
                getId(result),
                PrePickupPointApproveStatus.REJECTED,
                false
        );
    }

    private PrePickupPointRequestData buildRequestData(LegalPartnerPickupPointDto dto) {
        return new PrePickupPointRequestData(
                getId(dto),
                dto.getLegalPartnerId(),
                dto.getPvzMarketId(),
                dto.getPrePickupPoint().getName(),
                null
        );
    }

    private PrePickupPointDto generateDto() {
        return PrePickupPointDto.builder()
                .name(PickupPointTestParams.DEFAULT_NAME)
                .phone(CrmPrePickupPointTestParams.DEFAULT_PHONE)
                .prepayAllowed(CrmPrePickupPointTestParams.DEFAULT_PREPAY_ALLOWED)
                .cardAllowed(CrmPrePickupPointTestParams.DEFAULT_CARD_ALLOWED)
                .cashAllowed(CrmPrePickupPointTestParams.DEFAULT_CASH_ALLOWED)
                .comment(CrmPrePickupPointTestParams.DEFAULT_COMMENT)
                .photoUrl(CrmPrePickupPointTestParams.DEFAULT_PHOTO_URL)
                .ceilingHeight(CrmPrePickupPointTestParams.DEFAULT_CEILING_HEIGHT)
                .square(CrmPrePickupPointTestParams.DEFAULT_SQUARE)
                .brandingTypeWanted(CrmPrePickupPointTestParams.DEFAULT_WANT_TO_BE_BRANDED)
                .instruction(CrmPrePickupPointTestParams.DEFAULT_INSTRUCTION)
                .polygonId(CrmPrePickupPointTestParams.DEFAULT_POLYGON_ID)
                .hasWindows(CrmPrePickupPointTestParams.DEFAULT_HAS_WINDOWS)
                .hasSeparateEntrance(CrmPrePickupPointTestParams.DEFAULT_HAS_STREET_ENTRANCE)
                .hasStreetEntrance(CrmPrePickupPointTestParams.DEFAULT_HAS_STREET_ENTRANCE)
                .location(PickupPointLocationDto.builder()
                        .building(PickupPointLocationTestParams.DEFAULT_BUILDING)
                        .country(PickupPointLocationTestParams.DEFAULT_COUNTRY)
                        .floor(PickupPointLocationTestParams.DEFAULT_FLOOR)
                        .house(PickupPointLocationTestParams.DEFAULT_HOUSE)
                        .housing(PickupPointLocationTestParams.DEFAULT_HOUSING)
                        .intercom(PickupPointLocationTestParams.DEFAULT_INTERCOM)
                        .lat(PickupPointLocationTestParams.DEFAULT_LAT)
                        .lng(PickupPointLocationTestParams.DEFAULT_LNG)
                        .locality(PickupPointLocationTestParams.DEFAULT_LOCALITY)
                        .metro(PickupPointLocationTestParams.DEFAULT_METRO)
                        .office(PickupPointLocationTestParams.DEFAULT_OFFICE)
                        .porch(PickupPointLocationTestParams.DEFAULT_PORCH)
                        .region(PickupPointLocationTestParams.DEFAULT_REGION)
                        .street(PickupPointLocationTestParams.DEFAULT_STREET)
                        .zipCode(PickupPointLocationTestParams.DEFAULT_ZIP_CODE)
                        .build())
                .scheduleDays(List.of(
                        dayDto(DayOfWeek.MONDAY, true),
                        dayDto(DayOfWeek.TUESDAY, true),
                        dayDto(DayOfWeek.WEDNESDAY, true),
                        dayDto(DayOfWeek.THURSDAY, true),
                        dayDto(DayOfWeek.FRIDAY, true),
                        dayDto(DayOfWeek.SATURDAY, false),
                        dayDto(DayOfWeek.SUNDAY, false)
                ))
                .maxWidth(DEFAULT_MAX_WIDTH)
                .maxHeight(DEFAULT_MAX_HEIGHT)
                .maxLength(DEFAULT_MAX_LENGTH)
                .maxWeight(DEFAULT_MAX_WEIGHT)
                .maxSidesSum(PickupPoint.DEFAULT_MAX_SIDES_SUM)
                .build();
    }

    private PickupPointScheduleDaysDto dayDto(DayOfWeek dayOfWeek, boolean isWorkingDay) {
        return PickupPointScheduleDaysDto.builder()
                .dayOfWeek(dayOfWeek)
                .isWorkingDay(isWorkingDay)
                .timeFrom(LocalTime.of(10, 0))
                .timeTo(LocalTime.of(22, 0))
                .build();
    }

    private LegalPartnerPickupPointDto buildExpectedDto(LegalPartnerPickupPointDto actual, LegalPartner legalPartner) {
        return buildExpectedDto(actual, legalPartner.getId());
    }

    private LegalPartnerPickupPointDto buildExpectedDto(LegalPartnerPickupPointDto actual, long legalPartnerId) {
        var dto = LegalPartnerPickupPointDto.builder()
                .prePickupPointId(actual.getPrePickupPointId())
                .legalPartnerId(legalPartnerId)
                .active(DEFAULT_ACTIVE)
                .capacity(DEFAULT_CAPACITY)
                .storagePeriod(DEFAULT_STORAGE_PERIOD)
                .approveStatus(PrePickupPointApproveStatus.CHECKING)
                .brandType(CrmPrePickupPointTestParams.DEFAULT_WANT_TO_BE_BRANDED ?
                        PickupPointBrandingTypeDto.REQUEST : PickupPointBrandingTypeDto.NONE)
                .legalPartnerId(legalPartnerId)
                .prePickupPoint(generateDto())
                .build();

        var prePickupPoint = dto.getPrePickupPoint();

        dto.setClientArea(actual.getClientArea());
        dto.setWarehouseArea(actual.getWarehouseArea());
        prePickupPoint.setWarehouseArea(actual.getWarehouseArea());
        prePickupPoint.setClientArea(actual.getClientArea());
        return dto;
    }

    private Long getId(LegalPartnerPickupPointDto dto) {
        return dto.getPrePickupPointId();
    }

    @Test
    void testCompletePrePickupPointWithPreLegalPartnerArea() {
        var preLegalPartner = preLegalPartnerFactory.createPreLegalPartner(
                TestPreLegalPartnerFactory.PreLegalPartnerTestParams.builder()
                        .wantBrand(true)
                        .clientArea(BigDecimal.valueOf(66.6))
                        .warehouseArea(BigDecimal.valueOf(120.0))
                        .build());

        preLegalPartner = preLegalPartnerFactory.createLegalPartnerForLead(preLegalPartner.getId());
        LegalPartnerParams legalPartner = legalPartnerQueryService.get(preLegalPartner.getLegalPartnerId());

        LegalPartnerPickupPointDto result = legalPartnerPickupPointService
                .completePrePickupPointData(buildRequestData(legalPartner), generateDto());

        LegalPartnerPickupPointDto expected = buildExpectedDto(result, legalPartner.getId()).toBuilder()
                .approveStatus(PrePickupPointApproveStatus.CHECKING)
                .build();

        assertThat(result).isEqualTo(expected);

        preLegalPartner = preLegalPartnerQueryService.getById(preLegalPartner.getId());

        assertThat(preLegalPartner.hasIncompletePrePickupPoint()).isFalse();
        assertThat(preLegalPartner.getCompletedCrmPrePickupPointId()).isEqualTo(result.getPrePickupPointId());

    }

    @Test
    void testCompletePrePickupPointWithoutPreLegalPartnerArea() {
        var preLegalPartner = preLegalPartnerFactory.createPreLegalPartner(
                TestPreLegalPartnerFactory.PreLegalPartnerTestParams.builder()
                        .wantBrand(true)
                        .build());

        preLegalPartner = preLegalPartnerFactory.createLegalPartnerForLead(preLegalPartner.getId());
        LegalPartnerParams legalPartner = legalPartnerQueryService.get(preLegalPartner.getLegalPartnerId());

        LegalPartnerPickupPointDto result = legalPartnerPickupPointService
                .completePrePickupPointData(buildRequestData(legalPartner), generateDto());

        LegalPartnerPickupPointDto expected = buildExpectedDto(result, legalPartner.getId()).toBuilder()
                .approveStatus(PrePickupPointApproveStatus.CHECKING)
                .build();

        assertThat(result).isEqualTo(expected);

        preLegalPartner = preLegalPartnerQueryService.getById(preLegalPartner.getId());

        assertThat(preLegalPartner.hasIncompletePrePickupPoint()).isFalse();
        assertThat(preLegalPartner.getCompletedCrmPrePickupPointId()).isEqualTo(result.getPrePickupPointId());

    }

    @Test
    void testActiveMappedPickupPointHasConsumableRequestAction() {
        var legalPartner = legalPartnerFactory.createLegalPartner();

        var activeMappedPickupPoint = pickupPointFactory.createPickupPointFromCrm(CreatePickupPointBuilder.builder()
                .activateImmediately(true)
                .legalPartner(legalPartner)
                .params(PickupPointTestParams.builder().name("Рабочий ПВЗ").build())
                .build());
        addCourierMapping(activeMappedPickupPoint);

        var requestData = buildRequestData(legalPartner);
        var activeMappedPickupPoints = getAllPickupPoints(requestData, new PickupPointReportParamsDto("Рабочий ПВЗ", "", null, null));
        assertThat(activeMappedPickupPoints).hasSize(1);
        assertThat(activeMappedPickupPoints.get(0).getActions().stream()
                .anyMatch(e -> e.getType().equals(PickupPointActionType.CONSUMABLE_REQUEST))).isTrue();
    }

    @Test
    void testPickupPointsWithoutRequestAction() {
        var legalPartner = legalPartnerFactory.createLegalPartner();

        var inactiveMappedPickupPoint = pickupPointFactory.createPickupPointFromCrm(CreatePickupPointBuilder.builder()
                .activateImmediately(false)
                .legalPartner(legalPartner)
                .params(PickupPointTestParams.builder().name("Неактинвый ПВЗ с курьером").build())
                .build());
        addCourierMapping(inactiveMappedPickupPoint);

        var activeUnmappedPickupPoint = pickupPointFactory.createPickupPointFromCrm(CreatePickupPointBuilder.builder()
                .activateImmediately(true)
                .legalPartner(legalPartner)
                .params(PickupPointTestParams.builder().name("Активный ПВЗ без курьера").build())
                .build());

        var requestData = buildRequestData(legalPartner);
        var pickupPoints = getAllPickupPoints(requestData, new PickupPointReportParamsDto("", "", null, null));

        pickupPoints.forEach(p -> assertThat(
                p.getActions().stream()
                .anyMatch(e -> e.getType().equals(PickupPointActionType.CONSUMABLE_REQUEST))).isFalse()
        );
    }

    private void addCourierMapping(PickupPoint activeMappedPickupPoint) {
        pickupPointCourierMappingFactory.create(
                TestPickupPointCourierMappingFactory.PickupPointCourierMappingTestParamsBuilder.builder()
                        .pickupPoint(activeMappedPickupPoint)
                        .build());
    }

    @Test
    void testOrderSomeConsumables() {
        PickupPointSimpleParams pickupPoint = createPickupPoint();
        ConsumableTypeParams typeNotOrder = consumableTypeFactory.create();
        ConsumableTypeParams typeOrderPartially = consumableTypeFactory.create();
        ConsumableTypeParams typeOrderFully = consumableTypeFactory.create();

        legalPartnerPickupPointService.createConsumableOrder(pickupPoint.getPvzMarketId(), new ConsumableRequestDto(List.of(
                ConsumableItemDto.builder()
                        .typeId(typeOrderFully.getId())
                        .count(typeOrderFully.getCountPerPeriod())
                        .build(),

                ConsumableItemDto.builder()
                        .typeId(typeOrderPartially.getId())
                        .count(1)
                        .build()
        )));

        assertThat(legalPartnerPickupPointService.getConsumablesCapacity(pickupPoint.getPvzMarketId()).getCapacity())
                .containsExactlyInAnyOrder(
                        ConsumableItemDto.builder()
                                .typeId(typeNotOrder.getId())
                                .name(typeNotOrder.getName())
                                .count(typeNotOrder.getCountPerPeriod())
                                .maxCount(typeNotOrder.getCountPerPeriod())
                                .build(),

                        ConsumableItemDto.builder()
                                .typeId(typeOrderPartially.getId())
                                .name(typeOrderPartially.getName())
                                .count(typeOrderPartially.getCountPerPeriod() - 1)
                                .maxCount(typeOrderPartially.getCountPerPeriod())
                                .build()
                );
    }

    @Test
    void testShipWithFirstOrder() {
        PickupPointSimpleParams pickupPoint = createPickupPoint();
        ConsumableTypeParams typeToOrder = consumableTypeFactory.create();
        ConsumableTypeParams typeToShipWithFirstOrder = consumableTypeFactory.create(TestConsumableTypeFactory.ConsumableTypeTestParams.builder()
                .shipWithFirstOrder(true)
                .build());

        legalPartnerPickupPointService.createConsumableOrder(pickupPoint.getPvzMarketId(), new ConsumableRequestDto(List.of(
                ConsumableItemDto.builder()
                        .typeId(typeToOrder.getId())
                        .count(typeToOrder.getCountPerPeriod())
                        .build()
        )));

        var requests = consumableQueryService.getPickupPointRequests(pickupPoint.getId());
        assertThat(requests).hasSize(1);

        long requestId = requests.get(0).getId();
        List<Long> actuallyOrderedTypeIds = consumableQueryService.getRequestItems(requestId).stream()
                .map(ConsumableRequestItemParams::getConsumableTypeId)
                .collect(Collectors.toList());

        assertThat(actuallyOrderedTypeIds).containsExactlyInAnyOrder(
                typeToOrder.getId(), typeToShipWithFirstOrder.getId());
    }

    @Test
    void testOrderMoreThanAvailable() {
        PickupPointSimpleParams pickupPoint = createPickupPoint();
        ConsumableTypeParams type = consumableTypeFactory.create();

        legalPartnerPickupPointService.createConsumableOrder(pickupPoint.getPvzMarketId(), new ConsumableRequestDto(List.of(
                ConsumableItemDto.builder()
                        .typeId(type.getId())
                        .count(type.getCountPerPeriod())
                        .build()
        )));

        assertThatThrownBy(() -> legalPartnerPickupPointService.createConsumableOrder(pickupPoint.getPvzMarketId(),
                new ConsumableRequestDto(List.of(ConsumableItemDto.builder()
                        .typeId(type.getId())
                        .count(type.getCountPerPeriod())
                        .build()
                ))
        )).hasMessageContaining("доступно не более");
    }

    private PickupPointSimpleParams createPickupPoint() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointWithCourierMapping(TestPickupPointFactory.PickupPointTestParams.builder()
                .active(true)
                .build());

        pickupPointDeactivationCommandService.cancelDeactivation(
                pickupPoint.getId(), DEFAULT_FIRST_DEACTIVATION_REASON);

        return pickupPointQueryService.get(pickupPoint.getId());
    }
}
