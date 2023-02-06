package ru.yandex.market.logistics.lom.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import yandex.market.combinator.common.Common;
import yandex.market.combinator.v0.CombinatorOuterClass;

import ru.yandex.market.logistics.lom.converter.RouteConverter;
import ru.yandex.market.logistics.lom.converter.combinator.CombinatorConverter;
import ru.yandex.market.logistics.lom.exception.http.base.BadRequestException;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderRequestPayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResultStatus;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateLastMileFromPickupToPickupProcessor;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute;
import ru.yandex.market.logistics.lom.service.order.combinator.CombinatorGrpcClient;
import ru.yandex.market.logistics.lom.utils.UuidGenerator;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Тест на обработку заявки на изменение последней мили с самовывоза на самовывоз")
@ParametersAreNonnullByDefault
@DatabaseSetup("/orders/update_last_mile_from_pickup_to_pickup/before/order.xml")
public class UpdateLastMileFromPickupToPickupProcessorTest extends AbstractUpdateLastMileProcessorTest {

    private static final ChangeOrderRequestPayload CHANGE_ORDER_REQUEST_PAYLOAD =
        PayloadFactory.createChangeOrderRequestPayload(101, null);
    private static final UUID EXISTING_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private UpdateLastMileFromPickupToPickupProcessor processor;

    @Autowired
    private CombinatorGrpcClient combinatorGrpcClient;

    @Autowired
    private RouteConverter routeConverter;

    @Autowired
    private CombinatorConverter combinatorConverter;

    @Autowired
    private UuidGenerator uuidGenerator;

    @BeforeEach
    void setup() {
        doReturn(NEW_SAVED_UUID).when(uuidGenerator).randomUuid();
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(combinatorGrpcClient, lmsClient, marketIdService, mbiApiClient);
    }

    @Test
    @JpaQueriesCount(29)
    @DisplayName("Успешная обработка заявки в статусе CREATED")
    @DatabaseSetup("/orders/update_last_mile_from_pickup_to_pickup/before/change_request_created.xml")
    @ExpectedDatabase(
        value = "/orders/update_last_mile_from_pickup_to_pickup/after/created_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createdChangeOrderRequestProcessedSuccessfully() {
        // given:
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute(
                "orders/update_last_mile_from_pickup_to_pickup/before/locker_route.json",
                EXISTING_UUID
            )),
            converter::mapToItem
        );
        when(combinatorGrpcClient.getRedeliveryRouteToClosestPickupPoint(buildRedeliveryToPickupPointRequest()))
            .thenReturn(buildRedeliveryRouteResponse());

        // when:
        ProcessingResult processingResult = processor.processPayload(CHANGE_ORDER_REQUEST_PAYLOAD);

        // then:
        softly.assertThat(processingResult.getStatus()).isEqualTo(ProcessingResultStatus.SUCCESS);
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_UPDATE_LAST_MILE_FROM_PICKUP_TO_PICKUP,
            PayloadFactory.createChangeOrderRequestPayload(101, "1", 1)
        );
        verify(combinatorGrpcClient).getRedeliveryRouteToClosestPickupPoint(buildRedeliveryToPickupPointRequest());
    }

    /**
     * Изначальный путевой лист заказа FF - S_C - MOVEMENT - PICKUP - FF.
     * Итоговый путевой лист заказа FF - S_C - MOVEMENT - PICKUP - MOVEMENT(PREPARING) - PICKUP(PREPARING) - FF.
     * Статус заявки переходит в PROCESSING.
     */
    @Test
    @JpaQueriesCount(41)
    @DisplayName("Успешная обработка заявки в статусе INFO_RECEIVED")
    @DatabaseSetup(value = {
        "/orders/update_last_mile_from_pickup_to_pickup/before/change_request_info_received.xml",
        "/orders/update_last_mile_from_pickup_to_pickup/before/info_received_payload.xml"

    })
    @ExpectedDatabase(
        value = "/orders/update_last_mile_from_pickup_to_pickup/after/info_received_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void infoReceivedChangeOrderRequestProcessedSuccessfully() {
        // given:
        mockUpdateLastMileToPickup();

        // when:
        ProcessingResult processingResult = processor.processPayload(CHANGE_ORDER_REQUEST_PAYLOAD);

        // then:
        verifyUpdateLastMileToPickup(processingResult);
    }

    @Test
    @DisplayName("Ошибка обработки заявки в статусе INFO_RECEIVED без payload'а в статусе INFO_RECEIVED")
    @DatabaseSetup("/orders/update_last_mile_from_pickup_to_pickup/before/change_request_info_received.xml")
    void invalidPayloadStatusProcessingFailure() {
        testFail("No available payload for change request 101 in status INFO_RECEIVED");
        queueTaskChecker.assertNoQueueTasksCreated();
        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=ERROR\t" +
                "format=plain\t" +
                "code=UPDATE_LAST_MILE_INVALID_REQUEST_STATUS\t" +
                "payload=Failed to get ChangeLastMilePayload in status INFO_RECEIVED\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "entity_types=order,lom_order,changeOrderRequest\t" +
                "entity_values=order:1001,lom_order:101,changeOrderRequest:101"
        );
    }

    @Test
    @DisplayName("Невалидный маршрут с другой МК приводит к исключению")
    @DatabaseSetup(value = {
        "/orders/update_last_mile_from_pickup_to_pickup/before/change_request_info_received.xml",
        "/orders/update_last_mile_from_pickup_to_pickup/before/info_received_payload.xml"
    })
    void invalidRouteProcessingFailure() {
        // given:
        when(lmsClient.getPartner(PICKUP_PARTNER_ID)).thenReturn(Optional.of(PICKUP_PARTNER_RESPONSE));
        when(marketIdService.findLegalInfoByMarketIdOrThrow(PICKUP_MARKET_ID)).thenReturn(PICKUP_LEGAL_INFO);
        when(lmsClient.getLogisticsPoint(PICKUP_LOGISTICS_POINT_ID))
            .thenReturn(Optional.of(PICKUP_LOGISTICS_POINT_RESPONSE));
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute(
                "orders/update_last_mile_from_pickup_to_pickup/before/different_mk.json",
                NEW_SAVED_UUID
            )),
            converter::mapToItem
        );

        // then:
        testFail(
            "Invalid route to update last mile from PICKUP to PICKUP, different MK partner 1005705, current MK 1005706"
        );
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Ошибка обработки заявки в невалидном статусе")
    @DatabaseSetup(value = "/orders/update_last_mile_from_pickup_to_pickup/before/change_request_invalid_status.xml")
    void invalidStatusProcessingFailure() {
        testFail(
            "Pickup to pickup last mile change is not possible for change order request 101 with status PROCESSING"
        );
    }

    @Test
    @DisplayName("Отмена заказа при отсутствии нового маршрута в ПВЗ в ответе Комбинатора")
    @DatabaseSetup("/orders/update_last_mile_from_pickup_to_pickup/before/change_request_created.xml")
    @ExpectedDatabase(
        value = "/orders/update_last_mile_from_pickup_to_pickup/after/change_request_order_cancelled.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void orderCancellationOnRouteAbsence() {
        // given:
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute(
                "orders/update_last_mile_from_pickup_to_pickup/before/locker_route.json",
                EXISTING_UUID
            )),
            converter::mapToItem
        );
        when(combinatorGrpcClient.getRedeliveryRouteToClosestPickupPoint(buildRedeliveryToPickupPointRequest()))
            .thenReturn(CombinatorOuterClass.RedeliveryRouteResponse.newBuilder().build());

        // when:
        ProcessingResult processingResult = processor.processPayload(CHANGE_ORDER_REQUEST_PAYLOAD);

        // then:
        softly.assertThat(processingResult.getStatus()).isEqualTo(ProcessingResultStatus.SUCCESS);
        queueTaskChecker.assertQueueTasksCreated(QueueType.CREATE_SEGMENT_CANCELLATION_REQUESTS, 1);
        verify(combinatorGrpcClient).getRedeliveryRouteToClosestPickupPoint(buildRedeliveryToPickupPointRequest());
    }

    @Test
    @DisplayName("Ошибка обработки заявки при ошибке получения нового маршрута в ПВЗ от Комбинатора")
    @DatabaseSetup("/orders/update_last_mile_from_pickup_to_pickup/before/change_request_created.xml")
    void processingFailureOnRedeliveryRouteError() {
        // given:
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute(
                "orders/update_last_mile_from_pickup_to_pickup/before/locker_route.json",
                EXISTING_UUID
            )),
            converter::mapToItem
        );
        when(combinatorGrpcClient.getRedeliveryRouteToClosestPickupPoint(buildRedeliveryToPickupPointRequest()))
            .thenThrow(BadRequestException.class);

        // then:
        testFail(null);
        verify(combinatorGrpcClient).getRedeliveryRouteToClosestPickupPoint(buildRedeliveryToPickupPointRequest());
    }

    private void testFail(@Nullable String message) {
        softly.assertThatThrownBy(() -> processor.processPayload(CHANGE_ORDER_REQUEST_PAYLOAD))
            .hasMessage(message);
    }

    @Nonnull
    private CombinatorOuterClass.RedeliveryToPickupPointRequest buildRedeliveryToPickupPointRequest() {
        return CombinatorOuterClass.RedeliveryToPickupPointRequest.newBuilder()
            .setRoute(toDeliveryRoute("orders/update_last_mile_from_pickup_to_pickup/before/locker_route.json"))
            .setPaymentMethod(Common.PaymentMethod.CARD)
            .addAllPlaces(List.of(CombinatorOuterClass.Box.newBuilder()
                .addAllDimensions(List.of(1, 3, 2))
                .setWeight(4000)
                .build()))
            .build();
    }

    @Nonnull
    private CombinatorOuterClass.RedeliveryRouteResponse buildRedeliveryRouteResponse() {
        return CombinatorOuterClass.RedeliveryRouteResponse.newBuilder()
            .setRoute(toDeliveryRoute("orders/update_last_mile_to_pickup/before/combined_route_pickup.json"))
            .build();
    }

    @Nonnull
    @SneakyThrows
    private CombinatorOuterClass.DeliveryRoute toDeliveryRoute(String path) {
        JsonNode jsonNode = objectMapper.readTree(IntegrationTestUtils.extractFileContent(path));
        CombinatorRoute combinatorRoute = routeConverter.convertToCombinatorRoute(jsonNode);
        return Objects.requireNonNull(combinatorConverter.convertDeliveryRoute(combinatorRoute));
    }

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(routeHistoryTable, businessProcessStateStatusHistoryTableDescription);
    }
}
