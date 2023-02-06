package ru.yandex.market.logistics.lom.service;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.enums.SegmentType;
import ru.yandex.market.logistics.lom.exception.http.base.BadRequestException;
import ru.yandex.market.logistics.lom.jobs.consumer.ProcessUpdateLastMileToPickupConsumer;
import ru.yandex.market.logistics.lom.jobs.exception.DbQueueJobExecutionException;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderRequestPayload;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdAuthorPayload;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdPayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateLastMileToPickupProcessor;
import ru.yandex.market.logistics.lom.utils.MarketIdFactory;
import ru.yandex.market.logistics.lom.utils.TestUtils;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.core.Phone;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSubtypeResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.ydb.integration.YdbTableDescription;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.SERVICE_HEADERS;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Тест на обработку заявки на изменение типа доставки на доставку в ПВЗ")
@ParametersAreNonnullByDefault
@DatabaseSetup("/orders/update_last_mile_to_pickup/before/order_with_courier_delivery.xml")
public class UpdateLastMileToPickupProcessorTest extends AbstractUpdateLastMileProcessorTest {

    private static final Long FF_LOGISTICS_POINT_ID = 10000010736L;
    private static final Long SC_LOGISTICS_POINT_ID = 10001640163L;
    private static final Long SC_PARTNER_ID = 49784L;
    private static final Long MOVEMENT_PARTNER_ID = 1005705L;
    private static final Long MOVEMENT_MARKET_ID = 78234L;
    private static final PartnerResponse SC_PARTNER_RESPONSE = PartnerResponse.newBuilder()
        .id(SC_PARTNER_ID)
        .marketId(SC_MARKET_ID)
        .partnerType(PartnerType.SORTING_CENTER)
        .build();
    private static final PartnerResponse MOVEMENT_PARTNER_RESPONSE = PartnerResponse.newBuilder()
        .id(MOVEMENT_PARTNER_ID)
        .marketId(MOVEMENT_MARKET_ID)
        .partnerType(PartnerType.DELIVERY)
        .subtype(PartnerSubtypeResponse.newBuilder().id(2L).build())
        .params(List.of(
            new PartnerExternalParam("IS_DROPOFF", null, "false"),
            new PartnerExternalParam("DROPSHIP_EXPRESS", null, "false"),
            new PartnerExternalParam("RECIPIENT_UID_ENABLED", null, "false"),
            new PartnerExternalParam("UPDATE_COURIER_NEEDED", null, "false"),
            new PartnerExternalParam("UPDATE_INSTANCES_ENABLED", null, "false"),
            new PartnerExternalParam("ASSESSED_VALUE_TOTAL_CHECK", null, "false"),
            new PartnerExternalParam("ITEM_REMOVING_ENABLED", null, "false"),
            new PartnerExternalParam("INBOUND_VERIFICATION_CODE_REQUIRED", null, "false"),
            new PartnerExternalParam("OUTBOUND_VERIFICATION_CODE_REQUIRED", null, "false"),
            new PartnerExternalParam("ELECTRONIC_ACCEPTANCE_CERTIFICATE_REQUIRED", null, "false")
        ))
        .build();
    private static final LogisticsPointResponse FF_LOGISTICS_POINT_RESPONSE = LogisticsPointResponse.newBuilder()
        .id(FF_LOGISTICS_POINT_ID)
        .type(PointType.WAREHOUSE)
        .partnerId(FF_PARTNER_ID)
        .address(FF_ADDRESS)
        .externalId("3259")
        .schedule(Set.of(new ScheduleDayResponse(101L, 1, LocalTime.MIDNIGHT, LocalTime.NOON, true)))
        .phones(Set.of(Phone.newBuilder().number("8 (800) 234-27-12").build()))
        .build();
    private static final LogisticsPointResponse SC_LOGISTICS_POINT_RESPONSE = LogisticsPointResponse.newBuilder()
        .id(SC_LOGISTICS_POINT_ID)
        .type(PointType.WAREHOUSE)
        .partnerId(SC_PARTNER_ID)
        .address(SC_ADDRESS)
        .externalId("3258")
        .schedule(Set.of(new ScheduleDayResponse(101L, 1, LocalTime.MIDNIGHT, LocalTime.NOON, true)))
        .phones(Set.of(Phone.newBuilder().number("8 (800) 234-27-12").build()))
        .build();
    private static final MarketAccount MOVEMENT_MARKET_ACCOUNT = MarketIdFactory.marketAccount(
        MOVEMENT_MARKET_ID,
        MarketIdFactory.legalInfoBuilder().build()
    );

    @Autowired
    private UpdateLastMileToPickupProcessor processor;

    @Autowired
    private ProcessUpdateLastMileToPickupConsumer consumer;

    @DisplayName("Успешная обработка заявки для заказа")
    @Test
    @ExpectedDatabase(
        value = "/orders/update_last_mile_to_pickup/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void payloadProcessedSuccessfullyForOrder() {
        mockUpdateLastMileToPickup();
        ProcessingResult processingResult = processor.processPayload(CHANGE_ORDER_REQUEST_PAYLOAD);
        verifyUpdateLastMileToPickup(processingResult);
    }

    @DisplayName("Обогащение сегментов при смене последней мили на ПВЗ совпадает с обогащением сегментов при создании "
        + "заказа с исходной последней милей ПВЗ")
    @Test
    @ExpectedDatabase(
        value = "/orders/update_last_mile_to_pickup/after/change_last_mile_and_new_order_create_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void waybillSegmentsValidEnrichment() throws Exception {
        mockUpdateLastMileToPickup();
        ProcessingResult processingResult = processor.processPayload(CHANGE_ORDER_REQUEST_PAYLOAD);
        verifyUpdateLastMileToPickup(processingResult);

        when(marketIdService.findAccountById(PICKUP_MARKET_ID)).thenReturn(Optional.of(PICKUP_MARKET_ACCOUNT));
        processNewOrderCreation();

        transactionTemplate.execute(tx -> {
            WaybillSegment changeLastMileMovement = findSegmentByOrderIdAndSegmentType(101L, SegmentType.MOVEMENT);
            WaybillSegment changeLastMilePickup = findSegmentByOrderIdAndSegmentType(101L, SegmentType.PICKUP);
            WaybillSegment newPickupOrderMovement = findSegmentByOrderIdAndSegmentType(1L, SegmentType.MOVEMENT);
            WaybillSegment newPickupOrderPickup = findSegmentByOrderIdAndSegmentType(1L, SegmentType.PICKUP);

            softly.assertThat(changeLastMileMovement.getExternalId()).isEqualTo(newPickupOrderMovement.getExternalId());
            softly.assertThat(changeLastMilePickup.getTransferCodes()).usingRecursiveComparison().isEqualTo(
                newPickupOrderPickup.getTransferCodes()
            );
            assertWaybillSegmentEquallyEnriched(changeLastMileMovement, newPickupOrderMovement);
            assertWaybillSegmentEquallyEnriched(changeLastMilePickup, newPickupOrderPickup);

            return null;
        });
    }

    @DisplayName("Ошибка при обработке заявки из-за ошибки при запросе в LMS")
    @Test
    @ExpectedDatabase(
        value = "/orders/update_last_mile_to_pickup/before/order_with_courier_delivery.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void errorDuringPayloadProcessingOnLmsApiCallException() {
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute(
                "orders/update_last_mile_to_pickup/before/combined_route_pickup.json",
                NEW_SAVED_UUID
            )),
            converter::mapToItem
        );
        when(lmsClient.getPartner(PICKUP_PARTNER_ID)).thenThrow(BadRequestException.class);
        softly.assertThatThrownBy(() -> processor.processPayload(CHANGE_ORDER_REQUEST_PAYLOAD))
            .isInstanceOf(DbQueueJobExecutionException.class)
            .hasMessage("Error while UpdateLastMileToPickupProcessor processing, changeOrderRequestId=101")
            .getCause()
            .hasMessage(null);
        queueTaskChecker.assertNoQueueTasksCreated();
        verify(lmsClient).getPartner(PICKUP_PARTNER_ID);
    }

    @DisplayName("Ошибка при обработке заявки в невалидном статусе")
    @Test
    @DatabaseSetup(
        value = "/orders/update_last_mile_to_pickup/before/change_request_invalid_status.xml",
        type = DatabaseOperation.UPDATE
    )
    void processingFailsOnInvalidChangeOrderRequestStatus() {
        testFail("Cannot process change request with status INFO_RECEIVED. Processable status is CREATED");
        queueTaskChecker.assertNoQueueTasksCreated();
        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=ERROR\t" +
                "format=plain\t" +
                "code=UPDATE_LAST_MILE_INVALID_REQUEST_STATUS\t" +
                "payload=Cannot process change request with required status\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "entity_types=order,lom_order\t" +
                "entity_values=order:1001,lom_order:101\t" +
                "extra_keys=actualStatus,requiredStatus,changeOrderRequest\t" +
                "extra_values=INFO_RECEIVED,CREATED,101"
        );
    }

    @DisplayName("Ошибка при обработке пэйлоуда заявки в некорректном статусе")
    @Test
    @DatabaseSetup(
        value = "/orders/update_last_mile_to_pickup/before/payload_invalid_status.xml",
        type = DatabaseOperation.UPDATE
    )
    void processingFailOnInvalidPayloadStatus() {
        testFail("No available payload for change request 101 in status INFO_RECEIVED");
        queueTaskChecker.assertNoQueueTasksCreated();
        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=ERROR\t" +
                "format=plain\t" +
                "code=UPDATE_LAST_MILE_INVALID_REQUEST_STATUS\t" +
                "payload=Failed to get UpdateLastMileToPickupPayload in status INFO_RECEIVED\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "entity_types=order,lom_order,changeOrderRequest\t" +
                "entity_values=order:1001,lom_order:101,changeOrderRequest:101"
        );
    }

    @DisplayName("Не переводить заявку в статус TECH_FAIL при неудачной первой попытке")
    @Test
    @DatabaseSetup(
        value = "/orders/update_last_mile_to_pickup/before/add_business_process_state.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/orders/update_last_mile_to_pickup/after/no_tech_fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void doNotSetCorStatusToTechFailOnFirstAttempt() {
        TaskExecutionResult taskExecutionResult = consumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_UPDATE_LAST_MILE_TO_PICKUP,
            queueTaskChecker.getProducedTaskPayload(
                QueueType.PROCESS_UPDATE_LAST_MILE_TO_PICKUP,
                ChangeOrderRequestPayload.class
            ),
            1
        ));
        queueTaskChecker.assertNoQueueTasksCreated();
        softly.assertThat(taskExecutionResult).isEqualTo(TaskExecutionResult.fail());
    }

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(routeHistoryTable, businessProcessStateStatusHistoryTableDescription);
    }

    private void processNewOrderCreation() throws Exception {
        mockExternalForNewOrderCreation();

        mockMvc.perform(
            post("/orders/with-route")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(TestUtils.toHttpHeaders(SERVICE_HEADERS))
                .content(extractFileContent(
                    "orders/update_last_mile_to_pickup/before/create_order_with_route_pickup.json"
                ))
        )
            .andExpect(status().isOk());

        convertRouteToWaybillConsumer.execute(TaskFactory.createTask(queueTaskChecker.getProducedTaskPayload(
            QueueType.CONVERT_ROUTE_TO_WAYBILL,
            OrderIdAuthorPayload.class
        )));
        commitOrderConsumer.execute(TaskFactory.createTask(queueTaskChecker.getProducedTaskPayload(
            QueueType.COMMIT_ORDER,
            OrderIdAuthorPayload.class
        )));
        orderExternalValidationConsumer.execute(TaskFactory.createTask(queueTaskChecker.getProducedTaskPayload(
            QueueType.VALIDATE_ORDER_EXTERNAL,
            OrderIdPayload.class
        )));

        processWaybillCreateOrder(
            6L,
            6L,
            "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/2/1/1/1/1",
            "external-id-pvz"
        );
        processWaybillCreateOrder(
            5L,
            10L,
            "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/2/1/1/1/1/1/3/1",
            "external-id-mk"
        );

        verifyExternalAfterNewOrderCreation();
    }

    private void mockExternalForNewOrderCreation() {
        mockCommonExternalForNewOrderCreation();

        when(lmsClient.getLogisticsPoint(SC_LOGISTICS_POINT_ID)).thenReturn(Optional.of(SC_LOGISTICS_POINT_RESPONSE));
        when(lmsClient.searchPartners(SearchPartnerFilter.builder()
            .setIds(Set.of(FF_PARTNER_ID, SC_PARTNER_ID, MOVEMENT_PARTNER_ID, PICKUP_PARTNER_ID))
            .build()
        ))
            .thenReturn(List.of(
                FF_PARTNER_RESPONSE,
                SC_PARTNER_RESPONSE,
                MOVEMENT_PARTNER_RESPONSE,
                PICKUP_PARTNER_RESPONSE
            ));
        when(lmsClient.searchPartners(SearchPartnerFilter.builder()
            .setIds(Set.of(FF_PARTNER_ID, SC_PARTNER_ID, PICKUP_PARTNER_ID))
            .build())
        )
            .thenReturn(List.of(FF_PARTNER_RESPONSE, SC_PARTNER_RESPONSE, PICKUP_PARTNER_RESPONSE));
        when(lmsClient.getLogisticsPoints(LogisticsPointFilter.newBuilder()
            .ids(Set.of(
                PICKUP_LOGISTICS_POINT_ID,
                FF_LOGISTICS_POINT_ID,
                SC_LOGISTICS_POINT_ID
            ))
            .build())
        )
            .thenReturn(List.of(
                PICKUP_LOGISTICS_POINT_RESPONSE,
                FF_LOGISTICS_POINT_RESPONSE,
                SC_LOGISTICS_POINT_RESPONSE
            ));

        when(marketIdService.findAccountById(MOVEMENT_MARKET_ID)).thenReturn(Optional.of(MOVEMENT_MARKET_ACCOUNT));
    }

    private void verifyExternalAfterNewOrderCreation() {
        verifyCommonExternalAfterNewOrderCreation();

        verify(lmsClient).searchPartners(SearchPartnerFilter.builder()
            .setIds(Set.of(FF_PARTNER_ID, SC_PARTNER_ID, MOVEMENT_PARTNER_ID, PICKUP_PARTNER_ID))
            .build()
        );
        verify(lmsClient).searchPartners(SearchPartnerFilter.builder()
            .setIds(Set.of(FF_PARTNER_ID, SC_PARTNER_ID, PICKUP_PARTNER_ID))
            .build()
        );
        verify(lmsClient, times(2)).getLogisticsPoint(PICKUP_LOGISTICS_POINT_ID);
        verify(lmsClient).getLogisticsPoint(SC_LOGISTICS_POINT_ID);
        verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder()
            .ids(Set.of(PICKUP_LOGISTICS_POINT_ID, FF_LOGISTICS_POINT_ID, SC_LOGISTICS_POINT_ID))
            .build()
        );
        verify(lmsClient).searchPartnerRelation(PartnerRelationFilter.newBuilder()
            .fromPartnersIds(Set.of(FF_PARTNER_ID, SC_PARTNER_ID))
            .build()
        );
        verify(marketIdService).findAccountById(MOVEMENT_MARKET_ID);
        verify(marketIdService, times(1)).findAccountById(PICKUP_MARKET_ID);
    }

    private void testFail(String message) {
        softly.assertThatThrownBy(() -> processor.processPayload(CHANGE_ORDER_REQUEST_PAYLOAD))
            .isInstanceOf(DbQueueJobExecutionException.class)
            .hasMessage("Error while UpdateLastMileToPickupProcessor processing, changeOrderRequestId=101")
            .getCause()
            .hasMessage(message);
    }
}
