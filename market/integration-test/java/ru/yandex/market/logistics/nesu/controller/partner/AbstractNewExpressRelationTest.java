package ru.yandex.market.logistics.nesu.controller.partner;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationCreateDto;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationUpdateDto;
import ru.yandex.market.logistics.management.entity.request.radialZone.RadialLocationZoneFilter;
import ru.yandex.market.logistics.management.entity.response.CutoffResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSubtypeResponse;
import ru.yandex.market.logistics.management.entity.response.partner.RadialLocationZoneResponse;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.ShipmentType;
import ru.yandex.market.logistics.nesu.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.nesu.dto.ScheduleDayDto;
import ru.yandex.market.logistics.nesu.dto.partner.CpaPartnerInterfaceRelationRequest.CpaPartnerInterfaceRelationRequestBuilder;
import ru.yandex.market.logistics.nesu.enums.ShopShipmentType;
import ru.yandex.market.logistics.nesu.jobs.model.RemovePartnerExternalParamData;
import ru.yandex.market.logistics.nesu.jobs.model.UpdatePartnerExternalParamValueData;
import ru.yandex.market.logistics.nesu.jobs.producer.RemoveDropoffShopBannerProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.UpdatePartnerExternalParamValueProducer;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData.ErrorType;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData.ValidationErrorDataBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType.DROPSHIP_EXPRESS;
import static ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType.ELECTRONIC_ACCEPTANCE_CERTIFICATE_REQUIRED;
import static ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType.EXPRESS_RETURN_SORTING_CENTER_ID;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldErrorBuilder;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public abstract class AbstractNewExpressRelationTest extends AbstractPartnerControllerNewTest {

    private static final long EXPRESS_DELIVERY_WAREHOUSE_ID = 6360;
    private static final long EXPRESS_DELIVERY_PARTNER_ID = 1006360;
    private static final RadialLocationZoneFilter RADIAL_LOCATION_ZONE_FILTER = RadialLocationZoneFilter.newBuilder()
        .regionId(213)
        .isPrivate(false)
        .build();
    private static final Set<CutoffResponse> DEFAULT_EXPRESS_CUTOFF = Set.of(
        CutoffResponse.newBuilder()
            .locationId(225)
            .cutoffTime(LocalTime.of(23, 59))
            .build()
    );

    @Autowired
    private RemoveDropoffShopBannerProducer removeDropoffShopBannerProducer;
    @Autowired
    private UpdatePartnerExternalParamValueProducer updatePartnerExternalParamValueProducer;
    @Autowired
    private FeatureProperties featureProperties;

    @BeforeEach
    void setupMocks() {
        mockGetPartner(DROPSHIP_PARTNER_ID, PartnerType.DROPSHIP);

        mockGetSingleWarehouse(DROPSHIP_PARTNER_ID, 213);

        mockGetDeliveryWarehouse(true, DELIVERY_WAREHOUSE_ID);
        mockGetPartner(DELIVERY_PARTNER_ID, PartnerType.DELIVERY);

        mockGetDeliveryWarehouse(true, EXPRESS_DELIVERY_WAREHOUSE_ID, EXPRESS_DELIVERY_PARTNER_ID);
        mockGetPartner(
            EXPRESS_DELIVERY_PARTNER_ID,
            PartnerType.DELIVERY,
            partner -> partner.subtype(PartnerSubtypeResponse.newBuilder().id(34L).name("TAXI_EXPRESS").build())
        );

        // По умолчанию дропшип уже подключен к заборной доступности
        mockGetRelation(
            DROPSHIP_PARTNER_ID,
            defaultRelation(DROPSHIP_PARTNER_ID, DELIVERY_PARTNER_ID).build()
        );
        mockGetCapacity(DROPSHIP_PARTNER_ID);

        mockCreateCapacity();
        mockUpdateCapacity();
        mockCreateRelation();
        mockUpdateRelation();

        doNothing().when(removeDropoffShopBannerProducer).produceTask(anyLong());
        doNothing().when(updatePartnerExternalParamValueProducer).produceTask(any(), any());
        featureProperties.setEnableProcessReturnScThroughPi(true);
    }

    @AfterEach
    void tearDown() {
        featureProperties.setExpressDefaultHandlingTimeMinutes(30);
        featureProperties.setExpressAvailableHandlingTimesMinutes(List.of(10, 20, 30, 40, 50, 60));
        verifyNoMoreInteractions(lmsClient, updatePartnerExternalParamValueProducer);
        featureProperties.setEnableProcessReturnScThroughPi(true);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Валидация запроса")
    void requestValidation(
        ValidationErrorDataBuilder error,
        CpaPartnerInterfaceRelationRequestBuilder request
    ) throws Exception {
        saveRelation(1, 1, request)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(error.forObject("cpaPartnerInterfaceRelationRequest")));
    }

    @Nonnull
    private static Stream<Arguments> requestValidation() {
        return Stream.of(
            Arguments.of(
                fieldErrorBuilder("partnerSchedules", ErrorType.NOT_EMPTY),
                defaultExpressRequest().partnerSchedules(List.of())
            ),
            Arguments.of(
                fieldErrorBuilder("useElectronicReceptionTransferAct", ErrorType.NOT_NULL),
                defaultExpressRequest().useElectronicReceptionTransferAct(null)
            )
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Валидация запроса — расписания")
    void shipmentScheduleValidation(
        @SuppressWarnings("unused") String displayName,
        ValidationErrorDataBuilder error,
        CpaPartnerInterfaceRelationRequestBuilder request
    ) throws Exception {
        saveRelation(1, 1, request)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(error.forObject("cpaPartnerInterfaceRelation")));

        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
        verifyGetRelation();
        verifyGetPartner(DELIVERY_PARTNER_ID);
    }

    @Nonnull
    private static Stream<Arguments> shipmentScheduleValidation() {
        return Stream.of(
            Arguments.of(
                "Слишком короткое расписание",
                fieldErrorBuilder("shipmentSchedule", ERROR_TYPE_SCHEDULE_DAYS_COUNT),
                getExpressRequest(List.of(scheduleDay(1)))
            ),
            Arguments.of(
                "Нет подходящего дня в расписании склада для Экспресса",
                fieldErrorBuilder("shipmentSchedule", ERROR_TYPE_VALID_SHIPMENT_SCHEDULE),
                getExpressRequest(List.of(
                    scheduleDay(1),
                    scheduleDay(2),
                    scheduleDay(3),
                    scheduleDay(4),
                    scheduleDay(5),
                    scheduleDay(6)
                ))
            ),
            Arguments.of(
                "Расписание склада начинается позже расписания отгрузки Экспресса",
                fieldErrorBuilder("shipmentSchedule", ERROR_TYPE_VALID_SHIPMENT_SCHEDULE),
                getExpressRequest(List.of(
                    scheduleDay(1),
                    scheduleDay(2),
                    scheduleDay(3),
                    scheduleDay(4),
                    scheduleDay(5, LocalTime.of(8, 0), LocalTime.of(18, 0))
                ))
            ),
            Arguments.of(
                "Расписание склада заканчивается раньше расписания отгрузки Экспресса",
                fieldErrorBuilder("shipmentSchedule", ERROR_TYPE_VALID_SHIPMENT_SCHEDULE),
                getExpressRequest(List.of(
                    scheduleDay(1),
                    scheduleDay(2),
                    scheduleDay(3),
                    scheduleDay(4),
                    scheduleDay(5, LocalTime.of(10, 0), LocalTime.of(20, 0))
                ))
            )
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("handlingTimeArgs")
    @DisplayName("Новая связка дропшипа с Экспрессом — ранее не подключен")
    void newExpressRelationCustomHandlingTime(
        @SuppressWarnings("unused") String name,
        Integer handlingTime,
        Integer defaultHandlingTime,
        Integer passedHandlingTime,
        String customHandlingTimeEnabledValue,
        String responsePath
    ) throws Exception {
        featureProperties.setExpressDefaultHandlingTimeMinutes(defaultHandlingTime);
        mockGetRadialZones();
        mockNoRelation(DROPSHIP_PARTNER_ID);
        mockNoCapacity(DROPSHIP_PARTNER_ID);
        if (customHandlingTimeEnabledValue != null) {
            mockGetPartner(
                DROPSHIP_PARTNER_ID,
                PartnerType.DROPSHIP,
                partner -> partner.params(List.of(new PartnerExternalParam(
                    "EXPRESS_CUSTOM_HANDLING_TIME_ENABLED",
                    "Разрешено кастомное время",
                    customHandlingTimeEnabledValue
                )))
            );
        }

        saveRelation(
            1,
            DROPSHIP_PARTNER_ID,
            defaultExpressRequest()
                .useElectronicReceptionTransferAct(true)
                .handlingTimeExpressMinutes(handlingTime)
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));

        verify(updatePartnerExternalParamValueProducer).produceTask(
            List.of(
                new UpdatePartnerExternalParamValueData(DROPSHIP_PARTNER_ID, DROPSHIP_EXPRESS, "1"),
                new UpdatePartnerExternalParamValueData(
                    DROPSHIP_PARTNER_ID,
                    ELECTRONIC_ACCEPTANCE_CERTIFICATE_REQUIRED,
                    "1"
                )
            ),
            List.of()
        );

        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
        verifyGetRelation(DROPSHIP_PARTNER_ID, 2);
        verifyGetLogisticPoint(EXPRESS_DELIVERY_WAREHOUSE_ID);
        verifyGetPartner(EXPRESS_DELIVERY_PARTNER_ID);
        verifyCreateRelation(createExpressRelationDto());
        verifyGetCapacity(DROPSHIP_PARTNER_ID);
        verifyCreateCapacity(DROPSHIP_PARTNER_ID, 1000);
        verifyUpdateHandlingTime(DROPSHIP_PARTNER_ID, Duration.ofMinutes(passedHandlingTime));
        verifyGetRadialZones();
        verifyLinkRadialZones();
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("handlingTimeArgs")
    @DisplayName("Обновляется значение признака необходимости получения электронного АПП и время сборки для Экспресса")
    void updateElectronicAcceptanceCertificateRequired(
        @SuppressWarnings("unused") String name,
        Integer handlingTime,
        Integer defaultHandlingTime,
        Integer passedHandlingTime,
        String customHandlingTimeEnabledValue,
        String responsePath
    ) throws Exception {
        featureProperties.setExpressDefaultHandlingTimeMinutes(defaultHandlingTime);
        mockShopAsDropshipExpress(false, customHandlingTimeEnabledValue, null);

        saveRelation(
            1,
            DROPSHIP_PARTNER_ID,
            defaultExpressRequest()
                .handlingTimeExpressMinutes(handlingTime)
                .useElectronicReceptionTransferAct(true)
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));

        verify(updatePartnerExternalParamValueProducer).produceTask(
            List.of(new UpdatePartnerExternalParamValueData(
                DROPSHIP_PARTNER_ID,
                ELECTRONIC_ACCEPTANCE_CERTIFICATE_REQUIRED,
                "1"
            )),
            List.of()
        );

        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
        verifyGetRelation(DROPSHIP_PARTNER_ID, 2);
        verifyGetLogisticPoint(EXPRESS_DELIVERY_WAREHOUSE_ID);
        verifyGetPartner(EXPRESS_DELIVERY_PARTNER_ID, 2);
        verifyUpdateExpressRelation(updateExpressRelationDto(true));
        verifyGetCapacity(DROPSHIP_PARTNER_ID);
        verifyUpdateCapacity(1000L);
        verifyUpdateHandlingTime(DROPSHIP_PARTNER_ID, Duration.ofMinutes(passedHandlingTime));
        verifyGetRadialZones();
    }

    @Nonnull
    private static Stream<Arguments> handlingTimeArgs() {
        return Stream.of(
            Arguments.of(
                "Кастомное время включено, null с фронта, есть дефолтное в проперти",
                null,
                20,
                20,
                "1",
                "controller/partner/relation/new_dropship_express_withdraw_default_handling_time.json"
            ),
            Arguments.of(
                "Кастомное время включено, null с фронта, нет дефолтного в проперти",
                null,
                null,
                30,
                "1",
                "controller/partner/relation/new_dropship_express_withdraw.json"
            ),
            Arguments.of(
                "Кастомное время включено, содержится в списке доступных, есть дефолтное в проперти",
                40,
                20,
                40,
                "1",
                "controller/partner/relation/new_dropship_express_withdraw_custom_handling_time.json"
            ),
            Arguments.of(
                "Кастомное время включено, содержится в списке доступных, нет дефолтного в проперти",
                40,
                null,
                40,
                "1",
                "controller/partner/relation/new_dropship_express_withdraw_custom_handling_time.json"
            ),
            Arguments.of(
                "Кастомное время включено, не содержится в доступных, есть дефолтное в проперти",
                41,
                20,
                41,
                "1",
                "controller/partner/relation/new_dropship_express_withdraw_custom_not_from_list_handling_time.json"
            ),
            Arguments.of(
                "Кастомное время включено, не содержится в доступных, нет дефолтного в проперти",
                41,
                null,
                41,
                "1",
                "controller/partner/relation/new_dropship_express_withdraw_custom_not_from_list_handling_time.json"
            ),
            Arguments.of(
                "Кастомное время включено через null в проперти, null с фронта, есть дефолтное в проперти",
                null,
                20,
                20,
                null,
                "controller/partner/relation/new_dropship_express_withdraw_default_handling_time.json"
            ),
            Arguments.of(
                "Кастомное время включено через null в проперти, null с фронта, нет дефолтного в проперти",
                null,
                null,
                30,
                null,
                "controller/partner/relation/new_dropship_express_withdraw.json"
            ),
            Arguments.of(
                "Кастомное время включено через null в проперти, "
                    + "содержится в списке доступных, есть дефолтное в проперти",
                40,
                20,
                40,
                null,
                "controller/partner/relation/new_dropship_express_withdraw_custom_handling_time.json"
            ),
            Arguments.of(
                "Кастомное время включено через null в проперти, "
                    + "содержится в списке доступных, нет дефолтного в проперти",
                40,
                null,
                40,
                null,
                "controller/partner/relation/new_dropship_express_withdraw_custom_handling_time.json"
            ),
            Arguments.of(
                "Кастомное время включено через null в проперти, не содержится в доступных, есть дефолтное в проперти",
                41,
                20,
                41,
                null,
                "controller/partner/relation/new_dropship_express_withdraw_custom_not_from_list_handling_time.json"
            ),
            Arguments.of(
                "Кастомное время включено через null в проперти, не содержится в доступных, нет дефолтного в проперти",
                41,
                null,
                41,
                null,
                "controller/partner/relation/new_dropship_express_withdraw_custom_not_from_list_handling_time.json"
            ),
            Arguments.of(
                "Кастомное время выключено, null с фронта, есть дефолтное в проперти",
                null,
                20,
                20,
                "0",
                "controller/partner/relation/new_dropship_express_withdraw_default_handling_time.json"
            ),
            Arguments.of(
                "Кастомное время выключено, null с фронта, нет дефолтного в проперти",
                null,
                null,
                30,
                "0",
                "controller/partner/relation/new_dropship_express_withdraw.json"
            ),
            Arguments.of(
                "Кастомное время выключено, содержится в списке доступных, есть дефолтное в проперти",
                40,
                20,
                20,
                "0",
                "controller/partner/relation/new_dropship_express_withdraw_default_handling_time.json"
            ),
            Arguments.of(
                "Кастомное время выключено, содержится в списке доступных, нет дефолтного в проперти",
                40,
                null,
                30,
                "0",
                "controller/partner/relation/new_dropship_express_withdraw.json"
            ),
            Arguments.of(
                "Кастомное время выключено, не содержится в доступных, есть дефолтное в проперти",
                41,
                20,
                20,
                "0",
                "controller/partner/relation/new_dropship_express_withdraw_default_handling_time.json"
            ),
            Arguments.of(
                "Кастомное время выключено, не содержится в доступных, нет дефолтного в проперти",
                41,
                null,
                30,
                "0",
                "controller/partner/relation/new_dropship_express_withdraw.json"
            )
        );
    }

    @Test
    @DisplayName("Новая связка дропшипа с Экспрессом — ранее не подключен, null вместо доступных времен сборки")
    void newExpressRelationCustomHandlingTimeNullList() throws Exception {
        featureProperties.setExpressDefaultHandlingTimeMinutes(20);
        featureProperties.setExpressAvailableHandlingTimesMinutes(null);
        mockGetRadialZones();
        mockNoRelation(DROPSHIP_PARTNER_ID);
        mockNoCapacity(DROPSHIP_PARTNER_ID);
        mockGetPartner(
            DROPSHIP_PARTNER_ID,
            PartnerType.DROPSHIP,
            partner -> partner.params(List.of(new PartnerExternalParam(
                "EXPRESS_CUSTOM_HANDLING_TIME_ENABLED",
                "Разрешено кастомное время",
                "1"
            )))
        );

        saveRelation(
            1,
            DROPSHIP_PARTNER_ID,
            defaultExpressRequest()
                .useElectronicReceptionTransferAct(true)
                .handlingTimeExpressMinutes(40)
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/partner/relation/new_dropship_express_withdraw_custom_handling_time.json"
            ));

        verify(updatePartnerExternalParamValueProducer).produceTask(
            List.of(
                new UpdatePartnerExternalParamValueData(DROPSHIP_PARTNER_ID, DROPSHIP_EXPRESS, "1"),
                new UpdatePartnerExternalParamValueData(
                    DROPSHIP_PARTNER_ID,
                    ELECTRONIC_ACCEPTANCE_CERTIFICATE_REQUIRED,
                    "1"
                )
            ),
            List.of()
        );

        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
        verifyGetRelation(DROPSHIP_PARTNER_ID, 2);
        verifyGetLogisticPoint(EXPRESS_DELIVERY_WAREHOUSE_ID);
        verifyGetPartner(EXPRESS_DELIVERY_PARTNER_ID);
        verifyCreateRelation(createExpressRelationDto());
        verifyGetCapacity(DROPSHIP_PARTNER_ID);
        verifyCreateCapacity(DROPSHIP_PARTNER_ID, 1000);
        verifyUpdateHandlingTime(DROPSHIP_PARTNER_ID, Duration.ofMinutes(40));
        verifyGetRadialZones();
        verifyLinkRadialZones();
    }

    @Test
    @DisplayName("Связка дропшипа с Экспрессом - нет радиальных зон для региона, капасити уже больше дефолтного")
    void expressRelationNoRadialZones() throws Exception {
        mockNoRelation(DROPSHIP_PARTNER_ID);
        doReturn(List.of(capacityDtoBuilder(DROPSHIP_PARTNER_ID).value(1500L).build()))
            .when(lmsClient)
            .searchCapacity(capacityFilter(DROPSHIP_PARTNER_ID));

        saveRelation(
            1,
            DROPSHIP_PARTNER_ID,
            defaultExpressRequest()
                .toPartnerLogisticsPointId(100L)
                .useElectronicReceptionTransferAct(true)
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/partner/relation/new_dropship_express_withdraw_do_not_change_capacity.json"));

        verify(updatePartnerExternalParamValueProducer).produceTask(
            List.of(
                new UpdatePartnerExternalParamValueData(DROPSHIP_PARTNER_ID, DROPSHIP_EXPRESS, "1"),
                new UpdatePartnerExternalParamValueData(
                    DROPSHIP_PARTNER_ID,
                    ELECTRONIC_ACCEPTANCE_CERTIFICATE_REQUIRED,
                    "1"
                )
            ),
            List.of()
        );

        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
        verifyGetRelation(DROPSHIP_PARTNER_ID, 2);
        verifyGetLogisticPoint(EXPRESS_DELIVERY_WAREHOUSE_ID);
        verifyGetPartner(EXPRESS_DELIVERY_PARTNER_ID);
        verifyCreateRelation(createExpressRelationDto());
        verifyGetCapacity(DROPSHIP_PARTNER_ID);
        verifyUpdateHandlingTime(DROPSHIP_PARTNER_ID, HALF_HOUR);
        verifyGetRadialZones();
    }

    @Test
    @DisplayName("Связка дропшипа с Экспрессом, капасити есть в запросе")
    void expressRelationCapacityInRequest() throws Exception {
        mockNoRelation(DROPSHIP_PARTNER_ID);
        doReturn(List.of(capacityDtoBuilder(DROPSHIP_PARTNER_ID).id(CAPACITY_ID).value(1500L).build()))
            .when(lmsClient)
            .searchCapacity(capacityFilter(DROPSHIP_PARTNER_ID));

        doReturn(capacityDtoBuilder(DROPSHIP_PARTNER_ID).value(700L).build())
            .when(lmsClient).updateCapacityValue(CAPACITY_ID, 700L);

        saveRelation(
            1,
            DROPSHIP_PARTNER_ID,
            defaultExpressRequest()
                .toPartnerLogisticsPointId(100L)
                .capacityValue(700)
                .useElectronicReceptionTransferAct(true)
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/partner/relation/new_dropship_express_withdraw_capacity_in_request.json"
            ));

        verify(updatePartnerExternalParamValueProducer).produceTask(
            List.of(
                new UpdatePartnerExternalParamValueData(DROPSHIP_PARTNER_ID, DROPSHIP_EXPRESS, "1"),
                new UpdatePartnerExternalParamValueData(
                    DROPSHIP_PARTNER_ID,
                    ELECTRONIC_ACCEPTANCE_CERTIFICATE_REQUIRED,
                    "1"
                )
            ),
            List.of()
        );

        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
        verifyGetRelation(DROPSHIP_PARTNER_ID, 2);
        verifyGetLogisticPoint(EXPRESS_DELIVERY_WAREHOUSE_ID);
        verifyGetPartner(EXPRESS_DELIVERY_PARTNER_ID);
        verifyCreateRelation(createExpressRelationDto());
        verifyGetCapacity(DROPSHIP_PARTNER_ID);
        verifyUpdateCapacity(700L);
        verifyUpdateHandlingTime(DROPSHIP_PARTNER_ID, HALF_HOUR);
        verifyGetRadialZones();
    }

    @Test
    @DisplayName("Удаляются признаки экспресса при смене типа доставки")
    void updateExpressExternalParamsWhenShipmentTypeWasChanged() throws Exception {
        mockShopAsDropshipExpress(true, null, 100L);

        saveRelation(1, DROPSHIP_PARTNER_ID, defaultRequest())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/relation/new_dropship_withdraw.json"));

        verify(updatePartnerExternalParamValueProducer).produceTask(
            List.of(
                new UpdatePartnerExternalParamValueData(DROPSHIP_PARTNER_ID, DROPSHIP_EXPRESS, "0"),
                new UpdatePartnerExternalParamValueData(
                    DROPSHIP_PARTNER_ID,
                    ELECTRONIC_ACCEPTANCE_CERTIFICATE_REQUIRED,
                    "0"
                )
            ),
            List.of(new RemovePartnerExternalParamData(DROPSHIP_PARTNER_ID, EXPRESS_RETURN_SORTING_CENTER_ID))
        );

        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
        verifyGetRelation(DROPSHIP_PARTNER_ID, 2);
        verifyGetLogisticPoint(DELIVERY_WAREHOUSE_ID);
        verifyGetPartner(EXPRESS_DELIVERY_PARTNER_ID);
        verifyGetPartner(DELIVERY_PARTNER_ID);
        verifyUpdateExpressRelation(updateExpressRelationDto(false));
        verifyCreateRelation(defaultCreateRelationDto(DROPSHIP_PARTNER_ID).build());
        verifyGetCapacity(DROPSHIP_PARTNER_ID);
        verifyUpdateHandlingTime(DROPSHIP_PARTNER_ID, ONE_DAY);
    }

    @Test
    @DisplayName("Признаки экспресса и ЭАПП не перевыставляется если они не изменяются для связки ДШ-Экспресс")
    void expressExternalParamsDontUpdateIfShipmentTypeIsntExpress() throws Exception {

        saveRelation(1, DROPSHIP_PARTNER_ID, defaultRequest().useElectronicReceptionTransferAct(true))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/relation/new_dropship_withdraw.json"));

        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
        verifyGetRelation(DROPSHIP_PARTNER_ID, 2);
        verifyGetLogisticPoint(DELIVERY_WAREHOUSE_ID);
        verifyGetPartner(DELIVERY_PARTNER_ID, 2);
        verifyUpdateRelation(defaultUpdateRelationDto(DROPSHIP_PARTNER_ID, true).build());
        verifyGetCapacity(DROPSHIP_PARTNER_ID);
        verifyUpdateHandlingTime(DROPSHIP_PARTNER_ID, ONE_DAY);
        verify(updatePartnerExternalParamValueProducer).produceTask(List.of(), List.of());
    }

    @Test
    @DisplayName("Не обновляется значение склада для невыкупов Экспресса - параметр совпадает")
    void doNotUpdateReturnScId() throws Exception {
        mockShopAsDropshipExpress(false, null, 100L);

        saveRelation(1, DROPSHIP_PARTNER_ID, defaultExpressRequest().expressReturnSortingCenterId(100L))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/relation/return_sc_id_for_express.json"));

        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
        verifyGetRelation(DROPSHIP_PARTNER_ID, 2);
        verifyGetLogisticPoint(EXPRESS_DELIVERY_WAREHOUSE_ID);
        verifyGetPartner(EXPRESS_DELIVERY_PARTNER_ID, 2);
        verifyUpdateExpressRelation(updateExpressRelationDto(true));
        verifyGetCapacity(DROPSHIP_PARTNER_ID);
        verifyUpdateCapacity(1000L);
        verifyUpdateHandlingTime(DROPSHIP_PARTNER_ID, Duration.ofMinutes(30));
        verifyGetRadialZones();
        verify(updatePartnerExternalParamValueProducer).produceTask(List.of(), List.of());
    }

    @Test
    @DisplayName("Не обновляется значение склада для невыкупов Экспресса - флаг выключен")
    void doNotUpdateReturnScIdDisabled() throws Exception {
        featureProperties.setEnableProcessReturnScThroughPi(false);
        mockShopAsDropshipExpress(false, null, 100L);

        saveRelation(1, DROPSHIP_PARTNER_ID, defaultExpressRequest().expressReturnSortingCenterId(200L))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/relation/return_sc_id_for_express.json"));

        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
        verifyGetRelation(DROPSHIP_PARTNER_ID, 2);
        verifyGetLogisticPoint(EXPRESS_DELIVERY_WAREHOUSE_ID);
        verifyGetPartner(EXPRESS_DELIVERY_PARTNER_ID, 2);
        verifyUpdateExpressRelation(updateExpressRelationDto(true));
        verifyGetCapacity(DROPSHIP_PARTNER_ID);
        verifyUpdateCapacity(1000L);
        verifyUpdateHandlingTime(DROPSHIP_PARTNER_ID, Duration.ofMinutes(30));
        verifyGetRadialZones();
        verify(updatePartnerExternalParamValueProducer).produceTask(List.of(), List.of());
    }

    @Test
    @DisplayName("Удалять значение склада для невыкупов Экспресса под флагом")
    void removeReturnSc() throws Exception {
        mockShopAsDropshipExpress(false, null, 100L);

        saveRelation(1, DROPSHIP_PARTNER_ID, defaultExpressRequest().expressReturnSortingCenterId(null))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/relation/return_sc_id_for_express.json"));

        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
        verifyGetRelation(DROPSHIP_PARTNER_ID, 2);
        verifyGetLogisticPoint(EXPRESS_DELIVERY_WAREHOUSE_ID);
        verifyGetPartner(EXPRESS_DELIVERY_PARTNER_ID, 2);
        verifyUpdateExpressRelation(updateExpressRelationDto(true));
        verifyGetCapacity(DROPSHIP_PARTNER_ID);
        verifyUpdateCapacity(1000L);
        verifyUpdateHandlingTime(DROPSHIP_PARTNER_ID, Duration.ofMinutes(30));
        verifyGetRadialZones();
        verify(updatePartnerExternalParamValueProducer).produceTask(
            List.of(),
            List.of(new RemovePartnerExternalParamData(1, EXPRESS_RETURN_SORTING_CENTER_ID))
        );
    }

    @Test
    @DisplayName("Не удалять значение склада для невыкупов Экспресса под флагом")
    void doNotRemoveReturnSc() throws Exception {
        featureProperties.setEnableProcessReturnScThroughPi(false);
        mockShopAsDropshipExpress(false, null, 100L);

        saveRelation(1, DROPSHIP_PARTNER_ID, defaultExpressRequest().expressReturnSortingCenterId(null))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/relation/return_sc_id_for_express.json"));

        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
        verifyGetRelation(DROPSHIP_PARTNER_ID, 2);
        verifyGetLogisticPoint(EXPRESS_DELIVERY_WAREHOUSE_ID);
        verifyGetPartner(EXPRESS_DELIVERY_PARTNER_ID, 2);
        verifyUpdateExpressRelation(updateExpressRelationDto(true));
        verifyGetCapacity(DROPSHIP_PARTNER_ID);
        verifyUpdateCapacity(1000L);
        verifyUpdateHandlingTime(DROPSHIP_PARTNER_ID, Duration.ofMinutes(30));
        verifyGetRadialZones();
        verify(updatePartnerExternalParamValueProducer).produceTask(List.of(), List.of());
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Обновляется значение склада для невыкупов Экспресса")
    void updateReturnScId(@SuppressWarnings("unused") String name, @Nullable Long returnScId) throws Exception {
        mockShopAsDropshipExpress(false, null, returnScId);

        saveRelation(1, DROPSHIP_PARTNER_ID, defaultExpressRequest().expressReturnSortingCenterId(100L))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/relation/return_sc_id_for_express.json"));

        verify(updatePartnerExternalParamValueProducer).produceTask(
            List.of(
                new UpdatePartnerExternalParamValueData(DROPSHIP_PARTNER_ID, EXPRESS_RETURN_SORTING_CENTER_ID, "100")
            ),
            List.of()
        );

        verifyGetPartner(DROPSHIP_PARTNER_ID);
        verifyGetSingleWarehouse(DROPSHIP_PARTNER_ID);
        verifyGetRelation(DROPSHIP_PARTNER_ID, 2);
        verifyGetLogisticPoint(EXPRESS_DELIVERY_WAREHOUSE_ID);
        verifyGetPartner(EXPRESS_DELIVERY_PARTNER_ID, 2);
        verifyUpdateExpressRelation(updateExpressRelationDto(true));
        verifyGetCapacity(DROPSHIP_PARTNER_ID);
        verifyUpdateCapacity(1000L);
        verifyUpdateHandlingTime(DROPSHIP_PARTNER_ID, Duration.ofMinutes(30));
        verifyGetRadialZones();
    }

    @Nonnull
    private static Stream<Arguments> updateReturnScId() {
        return Stream.of(Arguments.of("Не было СЦ", null), Arguments.of("Другой СЦ", 200L));
    }

    private void mockShopAsDropshipExpress(
        boolean ertaEnabled,
        String customHandlingTimeEnabledValue,
        @Nullable Long returnScId
    ) {
        List<PartnerExternalParam> params = createParams(
            Stream.of(
                    DROPSHIP_EXPRESS,
                    ertaEnabled ? ELECTRONIC_ACCEPTANCE_CERTIFICATE_REQUIRED : null
                )
                .filter(Objects::nonNull)
                .toArray(PartnerExternalParamType[]::new)
        );
        if (customHandlingTimeEnabledValue != null) {
            params.add(
                new PartnerExternalParam(
                    "EXPRESS_CUSTOM_HANDLING_TIME_ENABLED",
                    "",
                    customHandlingTimeEnabledValue
                )
            );
        }
        if (returnScId != null) {
            params.add(
                new PartnerExternalParam(
                    EXPRESS_RETURN_SORTING_CENTER_ID.name(),
                    "",
                    returnScId.toString()
                )
            );
        }
        mockGetPartner(
            DROPSHIP_PARTNER_ID,
            PartnerType.DROPSHIP,
            partner -> partner.params(params)
        );

        mockGetRelation(DROPSHIP_PARTNER_ID, expressRelation().build());
    }

    private void mockGetRadialZones(List<RadialLocationZoneResponse> zones) {
        when(lmsClient.getRadialLocationZones(RADIAL_LOCATION_ZONE_FILTER)).thenReturn(zones);
    }

    private void mockGetRadialZones() {
        mockGetRadialZones(List.of(
            RadialLocationZoneResponse.newBuilder()
                .isPrivate(false)
                .id(1L)
                .regionId(213)
                .build(),
            RadialLocationZoneResponse.newBuilder()
                .isPrivate(false)
                .id(2L)
                .regionId(213)
                .build()
        ));
    }

    private void verifyGetRadialZones() {
        verify(lmsClient).getRadialLocationZones(RADIAL_LOCATION_ZONE_FILTER);
    }

    private void verifyLinkRadialZones() {
        verify(lmsClient).linkRadialZonesToLogisticPoint(10L, Set.of(1L, 2L));
    }

    private void verifyUpdateExpressRelation(PartnerRelationUpdateDto updateDto) {
        verifyUpdateRelation(DROPSHIP_PARTNER_ID, EXPRESS_DELIVERY_PARTNER_ID, updateDto);
    }

    @Nonnull
    private static PartnerRelationEntityDto.Builder expressRelation() {
        return defaultRelation(DROPSHIP_PARTNER_ID, EXPRESS_DELIVERY_PARTNER_ID, ShipmentType.WITHDRAW)
            .toPartnerLogisticsPointId(EXPRESS_DELIVERY_WAREHOUSE_ID)
            .cutoffs(DEFAULT_EXPRESS_CUTOFF);
    }

    @Nonnull
    private static PartnerRelationCreateDto createExpressRelationDto() {
        return defaultCreateRelationDto(DROPSHIP_PARTNER_ID)
            .toPartnerId(EXPRESS_DELIVERY_PARTNER_ID)
            .toPartnerLogisticsPointId(EXPRESS_DELIVERY_WAREHOUSE_ID)
            .cutoffs(DEFAULT_EXPRESS_CUTOFF)
            .build();
    }

    @Nonnull
    private static PartnerRelationUpdateDto updateExpressRelationDto(boolean enabled) {
        return defaultUpdateRelationDto(DROPSHIP_PARTNER_ID, enabled)
            .toPartnerId(EXPRESS_DELIVERY_PARTNER_ID)
            .toPartnerLogisticsPointId(EXPRESS_DELIVERY_WAREHOUSE_ID)
            .cutoffs(DEFAULT_EXPRESS_CUTOFF)
            .build();
    }

    @Nonnull
    private static CpaPartnerInterfaceRelationRequestBuilder getExpressRequest(List<ScheduleDayDto> partnerSchedule) {
        return defaultExpressRequest().partnerSchedules(partnerSchedule);
    }

    @Nonnull
    private List<PartnerExternalParam> createParams(PartnerExternalParamType... types) {
        return Stream.of(types)
            .map(type -> new PartnerExternalParam(type.name(), "", "1"))
            .collect(Collectors.toList());
    }

    @Nonnull
    private static CpaPartnerInterfaceRelationRequestBuilder defaultExpressRequest() {
        return defaultRequest(ShopShipmentType.WITHDRAW_EXPRESS);
    }
}
