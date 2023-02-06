package ru.yandex.market.logistics.nesu.jobs.processor;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.logistic.segment.CreateMovementSegmentRequest;
import ru.yandex.market.logistics.management.entity.request.logistic.segment.CreateWarehouseSegmentRequest;
import ru.yandex.market.logistics.management.entity.request.partner.PartnerExternalParamRequest;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSubtypeResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.management.entity.type.ServiceCodeName;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.exception.http.BadRequestException;
import ru.yandex.market.logistics.nesu.exception.http.MissingResourceException;
import ru.yandex.market.logistics.nesu.exception.http.ResourceNotFoundException;
import ru.yandex.market.logistics.nesu.jobs.model.DropoffRegistrationPayload;
import ru.yandex.market.logistics.nesu.model.LmsFactory;
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;
import ru.yandex.market.pvz.client.logistics.PvzLogisticsClient;
import ru.yandex.market.pvz.client.logistics.dto.CreateDropOffDto;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Регистрация дропоффа")
@ParametersAreNonnullByDefault
@DatabaseSetup("/repository/dropoff/dropoff_registration_state_enqueued.xml")
public class DropoffRegistrationProcessorTest extends AbstractContextualTest {
    private static final long DROPOFF_LOGISTIC_POINT_ID = 100L;
    private static final long ANOTHER_DROPOFF_LOGISTIC_POINT_ID = 101L;
    private static final long DROPOFF_LOGISTIC_POINT_EXTERNAL_ID = 111L;
    private static final long ANOTHER_DROPOFF_LOGISTIC_POINT_EXTERNAL_ID = 121L;
    private static final long DROPOFF_PARTNER_ID = 1L;
    private static final long SORTING_CENTER_WAREHOUSE_ID = 200L;
    private static final long SORTING_CENTER_ID = 20L;
    private static final long DELIVERY_SERVICE_ID = 10L;

    private static final LogisticsPointFilter ACTIVE_LOGISTIC_POINTS_FILTER = LmsFactory.createLogisticsPointsFilter(
        Set.of(SORTING_CENTER_WAREHOUSE_ID, DROPOFF_LOGISTIC_POINT_ID),
        true
    );

    private static final LogisticsPointResponse SORTING_CENTER_WAREHOUSE = LmsFactory.createLogisticsPointResponse(
        SORTING_CENTER_WAREHOUSE_ID,
        SORTING_CENTER_ID,
        "Sorting center warehouse",
        PointType.WAREHOUSE
    );
    private static final LogisticsPointResponse DROPOFF_PICKUP_POINT = LmsFactory.createLogisticsPointResponseBuilder(
        DROPOFF_LOGISTIC_POINT_ID,
        DROPOFF_PARTNER_ID,
        "Dropoff logistic point",
        PointType.PICKUP_POINT
    )
        .externalId(String.valueOf(DROPOFF_LOGISTIC_POINT_EXTERNAL_ID))
        .build();

    @Autowired
    private DropoffRegistrationProcessor dropoffRegistrationProcessor;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private PvzLogisticsClient pvzLogisticsClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient, pvzLogisticsClient);
    }

    @Test
    @DisplayName("Успешная регистрация дропоффа")
    @ExpectedDatabase(
        value = "/repository/dropoff/after/dropoff_registration_state_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @JpaQueriesCount(8)
    void success() {
        mockGetLogisticPoints();
        mockGetPartner();

        dropoffRegistrationProcessor.processPayload(createPayload(1));
        verifyGetLogisticPoints();
        verifyGetPartner();
        verifyPvzLogisticClient(DROPOFF_LOGISTIC_POINT_EXTERNAL_ID);
        verifyCreateWarehouseSegment(DROPOFF_LOGISTIC_POINT_ID, Set.of(300), null);
        verifyCreateDropoffMovementSegment(DROPOFF_LOGISTIC_POINT_ID, 360);
        verifySetIsDropoffPartnerExternalParam();
    }

    @Test
    @DisplayName("Успешная регистрация дропоффа с пустым списком карго-типов")
    @DatabaseSetup("/repository/dropoff/dropoff_registration_state_enqueued_with_empty_cargo_types.xml")
    @ExpectedDatabase(
        value = "/repository/dropoff/after/dropoff_registration_state_success_with_empty_cargo_types.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @JpaQueriesCount(7)
    void successWithEmptyForbiddenCargoTypes() {
        mockGetLogisticPoints();
        mockGetPartner();

        dropoffRegistrationProcessor.processPayload(createPayload(2));
        verifyGetLogisticPoints();
        verifyGetPartner();
        verifyPvzLogisticClient(DROPOFF_LOGISTIC_POINT_EXTERNAL_ID);
        verifyCreateWarehouseSegment(DROPOFF_LOGISTIC_POINT_ID, null, null);
        verifyCreateDropoffMovementSegment(DROPOFF_LOGISTIC_POINT_ID, 540);
        verifySetIsDropoffPartnerExternalParam();
    }

    @Test
    @DisplayName("Успешная регистрация дропоффа с длительностью перемещения 0")
    @DatabaseSetup("/repository/dropoff/dropoff_registration_state_enqueued_with_0_movement_duration.xml")
    @ExpectedDatabase(
        value = "/repository/dropoff/after/dropoff_registration_state_success_with_0_movement_duration.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @JpaQueriesCount(7)
    void successWithZeroMovementDuration() {
        mockGetLogisticPoints();
        mockGetPartner();

        dropoffRegistrationProcessor.processPayload(createPayload(2));
        verifyGetLogisticPoints();
        verifyGetPartner();
        verifyPvzLogisticClient(DROPOFF_LOGISTIC_POINT_EXTERNAL_ID);
        verifyCreateWarehouseSegment(DROPOFF_LOGISTIC_POINT_ID, null, null);
        verifyCreateDropoffMovementSegment(DROPOFF_LOGISTIC_POINT_ID, 0);
        verifySetIsDropoffPartnerExternalParam();
    }

    @Test
    @DisplayName("Успешная регистрация дропоффа с возвратным складом СЦ")
    @ExpectedDatabase(
        value = "/repository/dropoff/after/dropoff_registration_state_success_return_to_sc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @JpaQueriesCount(8)
    void successReturnWarehouseSc() {
        LogisticsPointFilter filter = LmsFactory.createLogisticsPointsFilter(
            Set.of(SORTING_CENTER_WAREHOUSE_ID, ANOTHER_DROPOFF_LOGISTIC_POINT_ID),
            true
        );

        when(lmsClient.getLogisticsPoints(filter))
            .thenReturn(List.of(
                SORTING_CENTER_WAREHOUSE,
                LmsFactory.createLogisticsPointResponseBuilder(
                    ANOTHER_DROPOFF_LOGISTIC_POINT_ID,
                    DROPOFF_PARTNER_ID,
                    "Another dropoff logistic point",
                    PointType.PICKUP_POINT
                )
                    .externalId(String.valueOf(ANOTHER_DROPOFF_LOGISTIC_POINT_EXTERNAL_ID))
                    .build()
            ));

        mockGetPartner();

        dropoffRegistrationProcessor.processPayload(createPayload(2));
        verify(lmsClient).getLogisticsPoints(filter);
        verifyGetPartner();
        verifyPvzLogisticClient(ANOTHER_DROPOFF_LOGISTIC_POINT_EXTERNAL_ID);
        verifyCreateWarehouseSegment(ANOTHER_DROPOFF_LOGISTIC_POINT_ID, Set.of(300), SORTING_CENTER_ID);
        verifyCreateDropoffMovementSegment(ANOTHER_DROPOFF_LOGISTIC_POINT_ID, 420);
        verifySetIsDropoffPartnerExternalParam();
    }

    @Test
    @DisplayName("Ошибка при регистрации дропоффа")
    @ExpectedDatabase(
        value = "/repository/dropoff/after/dropoff_registration_state_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void finalFailure() {
        dropoffRegistrationProcessor.processFinalFailure(createPayload(1), new RuntimeException("error message"));
    }

    @Test
    @DisplayName("Не найдена заявка на регистрацию дропоффа по идентификатору")
    void dropoffRegistrationStateNotFound() {
        softly.assertThatThrownBy(() -> dropoffRegistrationProcessor.processPayload(createPayload(3)))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [DROPOFF_REGISTRATION_STATE] with ids [3]");
    }

    @Test
    @DisplayName("Вместо склада СЦ найден ПВЗ")
    void sortingCenterWarehouseIsInactive() {
        mockGetLogisticPoints(List.of(
            LmsFactory.createLogisticsPointResponse(
                SORTING_CENTER_WAREHOUSE_ID,
                SORTING_CENTER_ID,
                "Sorting center warehouse",
                PointType.PICKUP_POINT
            ),
            DROPOFF_PICKUP_POINT
        ));

        softly.assertThatThrownBy(() -> dropoffRegistrationProcessor.processPayload(createPayload(1)))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [WAREHOUSE] with ids [200]");

        verifyGetLogisticPoints();
    }

    @Test
    @DisplayName("Вместо ПВЗ дропоффа найден склад")
    void dropoffLogisticPointIsInactive() {
        mockGetLogisticPoints(List.of(
            SORTING_CENTER_WAREHOUSE,
            LmsFactory.createLogisticsPointResponse(
                DROPOFF_LOGISTIC_POINT_ID,
                DROPOFF_PARTNER_ID,
                "Dropoff logistic point",
                PointType.WAREHOUSE
            )
        ));

        softly.assertThatThrownBy(() -> dropoffRegistrationProcessor.processPayload(createPayload(1)))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [PICKUP_POINT] with ids [100]");

        verifyGetLogisticPoints();
    }

    @Test
    @DisplayName("Не найден партнёр дропоффа")
    void dropoffPartner() {
        mockGetLogisticPoints();

        softly.assertThatThrownBy(() -> dropoffRegistrationProcessor.processPayload(createPayload(1)))
            .isInstanceOf(MissingResourceException.class)
            .hasMessage("Missing [PARTNER] with ids [1]");

        verifyGetLogisticPoints();
        verifyGetPartner();
    }

    @Test
    @DisplayName("Точка дропоффа не является своим ПВЗ маркета")
    void dropoffLogisticPointIsNotMarketPickupPoint() {
        mockGetLogisticPoints();
        when(lmsClient.getPartner(DROPOFF_PARTNER_ID)).thenReturn(Optional.of(
            LmsFactory.createPartner(DROPOFF_PARTNER_ID, PartnerType.DELIVERY)
        ));

        softly.assertThatThrownBy(() -> dropoffRegistrationProcessor.processPayload(createPayload(1)))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Дропоффом может быть только ПВЗ Маркета.");

        verifyGetLogisticPoints();
        verifyGetPartner();
    }

    @Test
    @DisplayName("Внешний идентификатор точки дропоффа не является числом")
    void dropoffExternalIdIsNotNumeric() {
        mockGetLogisticPoints(List.of(
            SORTING_CENTER_WAREHOUSE,
            LmsFactory.createLogisticsPointResponse(
                DROPOFF_LOGISTIC_POINT_ID,
                DROPOFF_PARTNER_ID,
                "Dropoff logistic point",
                PointType.PICKUP_POINT
            )
        ));
        mockGetPartner();

        softly.assertThatThrownBy(() -> dropoffRegistrationProcessor.processPayload(createPayload(1)))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Внешний id точки id=100 (externalId) не является числом.");

        verifyGetLogisticPoints();
        verifyGetPartner();
    }

    @Test
    @DisplayName("Конфигурация доступности для точки дропоффа уже существует и выключена")
    @DatabaseSetup(
        value = "/repository/dropoff/dropoff_logistic_point_availability_disable.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/repository/dropoff/after/dropoff_registration_state_success_availability_already_exists.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successLogisticPointAvailabilityAlreadyExistsAndDisabled() {
        alreadyExistsAvailabilityProcessing();
    }

    @Test
    @DisplayName("Конфигурация доступности для точки дропоффа уже существует")
    @DatabaseSetup(
        value = "/repository/dropoff/dropoff_logistic_point_availability.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/repository/dropoff/after/dropoff_registration_state_success_availability_already_exists.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successLogisticPointAvailabilityAlreadyExists() {
        alreadyExistsAvailabilityProcessing();
    }

    private void alreadyExistsAvailabilityProcessing() {
        mockGetLogisticPoints();
        mockGetPartner();

        dropoffRegistrationProcessor.processPayload(createPayload(1));
        verifyGetLogisticPoints();
        verifyGetPartner();
        verifyPvzLogisticClient(DROPOFF_LOGISTIC_POINT_EXTERNAL_ID);
        verifyCreateWarehouseSegment(DROPOFF_LOGISTIC_POINT_ID, Set.of(300), null);
        verifyCreateDropoffMovementSegment(DROPOFF_LOGISTIC_POINT_ID, 360);
        verifySetIsDropoffPartnerExternalParam();
    }

    @Nonnull
    private DropoffRegistrationPayload createPayload(long dropoffRegistrationStateId) {
        return new DropoffRegistrationPayload("1", dropoffRegistrationStateId);
    }

    private void mockGetLogisticPoints() {
        mockGetLogisticPoints(List.of(SORTING_CENTER_WAREHOUSE, DROPOFF_PICKUP_POINT));
    }

    private void mockGetLogisticPoints(List<LogisticsPointResponse> logisticsPointResponses) {
        when(lmsClient.getLogisticsPoints(ACTIVE_LOGISTIC_POINTS_FILTER))
            .thenReturn(logisticsPointResponses);
    }

    private void mockGetPartner() {
        when(lmsClient.getPartner(DROPOFF_PARTNER_ID)).thenReturn(Optional.of(
            LmsFactory.createPartnerResponseBuilder(DROPOFF_PARTNER_ID, PartnerType.DELIVERY, 1L)
                .subtype(
                    PartnerSubtypeResponse.newBuilder()
                        .id(3)
                        .build()
                )
                .build()
        ));
    }

    private void verifyGetLogisticPoints() {
        verify(lmsClient).getLogisticsPoints(ACTIVE_LOGISTIC_POINTS_FILTER);
    }

    private void verifyGetPartner() {
        verify(lmsClient).getPartner(DROPOFF_PARTNER_ID);
    }

    private void verifyPvzLogisticClient(long dropoffLogisticPointExternalId) {
        verify(pvzLogisticsClient).createDropOff(
            dropoffLogisticPointExternalId,
            CreateDropOffDto.builder()
                .sortingCenterPartnerId(SORTING_CENTER_ID)
                .marketCourierPartnerId(DELIVERY_SERVICE_ID)
                .build()
        );
    }

    private void verifyCreateWarehouseSegment(
        long dropoffLogisticPointId,
        @Nullable Set<Integer> cargoTypes,
        @Nullable Long returnWarehousePartnerId
    ) {
        verify(lmsClient).createWarehouseLogisticSegment(
            CreateWarehouseSegmentRequest.builder()
                .logisticPointId(dropoffLogisticPointId)
                .returnWarehousePartnerId(returnWarehousePartnerId)
                .cargoTypes(cargoTypes == null ? null : Map.of(ServiceCodeName.PROCESSING, cargoTypes))
                .build()
        );
    }

    private void verifyCreateDropoffMovementSegment(
        long dropoffLogisticPointId,
        int movementDuration
    ) {
        verify(lmsClient).createDropoffMovementLogisticSegments(
            CreateMovementSegmentRequest.builder()
                .logisticsPointFromId(dropoffLogisticPointId)
                .logisticsPointToId(SORTING_CENTER_WAREHOUSE_ID)
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .tmMovementSchedule(Set.of(
                    new ScheduleDayResponse(null, 1, LocalTime.of(13, 0), LocalTime.of(18, 0)),
                    new ScheduleDayResponse(null, 2, LocalTime.of(14, 0), LocalTime.of(17, 0))
                ))
                .freezeTmMovementService(true)
                .movementDuration(movementDuration)
                .startAtRightBorder(true)
                .build()
        );
    }

    private void verifySetIsDropoffPartnerExternalParam() {
        verify(lmsClient).addOrUpdatePartnerExternalParam(
            DROPOFF_PARTNER_ID,
            new PartnerExternalParamRequest(PartnerExternalParamType.IS_DROPOFF, "1")
        );
    }
}
