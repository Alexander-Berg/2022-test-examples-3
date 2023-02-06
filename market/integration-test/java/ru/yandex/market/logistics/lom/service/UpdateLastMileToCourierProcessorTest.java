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

import ru.yandex.market.id.LegalInfo;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.enums.SegmentType;
import ru.yandex.market.logistics.lom.jobs.exception.DbQueueJobExecutionException;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdAuthorPayload;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdPayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResultStatus;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateLastMileToCourierProcessor;
import ru.yandex.market.logistics.lom.model.enums.PartnerSubtype;
import ru.yandex.market.logistics.lom.utils.MarketIdFactory;
import ru.yandex.market.logistics.lom.utils.TestUtils;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.SERVICE_HEADERS;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Тест на обработку заявки на изменение типа доставки на Курьерскую")
@ParametersAreNonnullByDefault
@DatabaseSetup("/orders/update_last_mile_to_courier/before/order_with_pickup_delivery.xml")
public class UpdateLastMileToCourierProcessorTest extends AbstractUpdateLastMileProcessorTest {

    private static final Long FF_LOGISTICS_POINT_ID = 10000004403L;
    private static final Long SC_LOGISTICS_POINT_ID = 10001073009L;
    private static final Long SC_PARTNER_ID = 63119L;
    private static final Long COURIER_PARTNER_ID = 63132L;
    private static final Long COURIER_MARKET_ID = 303502380L;

    private static final PartnerResponse SC_PARTNER_RESPONSE = PartnerResponse.newBuilder()
        .id(SC_PARTNER_ID)
        .marketId(SC_MARKET_ID)
        .partnerType(PartnerType.SORTING_CENTER)
        .build();
    private static final PartnerResponse COURIER_PARTNER_RESPONSE = PartnerResponse.newBuilder()
        .id(COURIER_PARTNER_ID)
        .marketId(COURIER_MARKET_ID)
        .name("МК Сестрица Сорока")
        .readableName("МК Сестрица Сорока")
        .domain("https://url.stub")
        .partnerType(PartnerType.DELIVERY)
        .subtype(PartnerSubtypeResponse.newBuilder()
            .id(2)
            .name(PartnerSubtype.MARKET_COURIER.name())
            .build()
        )
        .params(List.of(
            new PartnerExternalParam("CAN_UPDATE_INSTANCES", null, "true"),
            new PartnerExternalParam("IS_DROPOFF", null, "false"),
            new PartnerExternalParam("DROPSHIP_EXPRESS", null, "false"),
            new PartnerExternalParam("RECIPIENT_UID_ENABLED", null, "true"),
            new PartnerExternalParam("UPDATE_COURIER_NEEDED", null, "false"),
            new PartnerExternalParam("GET_DELIVERY_DATE_ENABLED", null, "true"),
            new PartnerExternalParam("UPDATE_INSTANCES_ENABLED", null, "true"),
            new PartnerExternalParam("ASSESSED_VALUE_TOTAL_CHECK", null, "false"),
            new PartnerExternalParam("AUTO_ITEM_REMOVING_ENABLED", null, "true"),
            new PartnerExternalParam("ITEM_REMOVING_ENABLED", null, "false"),
            new PartnerExternalParam("UPDATE_ORDER_WITH_ONE_BOX_ENABLED", null, "true"),
            new PartnerExternalParam("INBOUND_VERIFICATION_CODE_REQUIRED", null, "false"),
            new PartnerExternalParam("UPDATE_ORDER_WITH_MANY_BOXES_ENABLED", null, "true"),
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
    private static final MarketAccount COURIER_MARKET_ACCOUNT = MarketIdFactory.marketAccount(
        COURIER_MARKET_ID,
        LegalInfo.newBuilder()
            .setLegalName("Яндекс.Маркет")
            .setType("OOO")
            .setLegalAddress("г Москва, Новинский б-р, д 8, пом 9")
            .setRegistrationNumber("1167746491395")
            .setInn("7715805253")
            .build()
    );

    @Autowired
    private UpdateLastMileToCourierProcessor processor;

    @Test
    @DisplayName("Успешная обработка заявки для заказа, создание PREPARING сегмента COURIER для новой последней мили")
    @DatabaseSetup("/orders/update_last_mile_to_courier/before/change_order_request.xml")
    @ExpectedDatabase(
        value = "/orders/update_last_mile_to_courier/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void payloadProcessedSuccessfullyForOrder() {
        processUpdateLastMileToCourier();
    }

    @Test
    @DisplayName("Обогащение сегментов при смене последней мили на Курьерку совпадает с обогащением сегментов при "
        + "создании заказа с исходной последней милей Курьерка")
    @DatabaseSetup("/orders/update_last_mile_to_courier/before/change_order_request.xml")
    @ExpectedDatabase(
        value = "/orders/update_last_mile_to_courier/after/change_last_mile_and_new_order_create_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void waybillSegmentsValidEnrichment() throws Exception {
        processUpdateLastMileToCourier();
        processNewOrderCreation();

        transactionTemplate.execute(tx -> {
            WaybillSegment changeLastMileCourier = findSegmentByOrderIdAndSegmentType(101L, SegmentType.COURIER);
            WaybillSegment newPickupOrderCourier = findSegmentByOrderIdAndSegmentType(1L, SegmentType.COURIER);

            softly.assertThat(changeLastMileCourier.getExternalId()).isEqualTo(newPickupOrderCourier.getExternalId());
            assertWaybillSegmentEquallyEnriched(changeLastMileCourier, newPickupOrderCourier);

            return null;
        });
    }

    @Test
    @DisplayName("Некорректный маршрут приводит к исключению (другой партнер МК)")
    @DatabaseSetup("/orders/update_last_mile_to_courier/before/change_order_request.xml")
    @ExpectedDatabase(
        value = "/orders/update_last_mile_to_courier/before/order_with_pickup_delivery.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void exceptionThrownUponInvalidRoute() {
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute(
                "orders/update_last_mile_to_courier/before/invalid_route.json",
                NEW_SAVED_UUID
            )),
            converter::mapToItem
        );
        testFail("Invalid route to update last mile to COURIER, different MK partner 63135, current MK 63132");
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Ошибка при обработке заявки в невалидном статусе")
    @DatabaseSetup(value = "/orders/update_last_mile_to_courier/before/change_request_invalid_status.xml")
    @ExpectedDatabase(
        value = "/orders/update_last_mile_to_courier/before/order_with_pickup_delivery.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
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

    @Test
    @DisplayName("Ошибка при обработке пэйлоуда заявки в некорректном статусе")
    @DatabaseSetup("/orders/update_last_mile_to_courier/before/change_order_request.xml")
    @DatabaseSetup(
        value = "/orders/update_last_mile_to_pickup/before/payload_invalid_status.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/orders/update_last_mile_to_courier/before/order_with_pickup_delivery.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
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

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(routeHistoryTable, businessProcessStateStatusHistoryTableDescription);
    }

    private void processUpdateLastMileToCourier() {
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute(
                "orders/update_last_mile_to_courier/before/combined_route_courier.json",
                NEW_SAVED_UUID
            )),
            converter::mapToItem
        );
        ProcessingResult processingResult = processor.processPayload(CHANGE_ORDER_REQUEST_PAYLOAD);
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CHANGE_ORDER_REQUEST,
            PayloadFactory.createChangeOrderRequestPayload(101, "1", 1)
        );
        softly.assertThat(processingResult.getStatus()).isEqualTo(ProcessingResultStatus.SUCCESS);
    }

    private void processNewOrderCreation() throws Exception {
        mockExternalForNewOrderCreation();

        mockMvc.perform(
            post("/orders/with-route")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(TestUtils.toHttpHeaders(SERVICE_HEADERS))
                .content(extractFileContent(
                    "orders/update_last_mile_to_courier/before/create_order_with_route_courier.json"
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
            4L,
            6L,
            "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/2/1/1/1/1",
            "mk-external-id"
        );

        verifyExternalAfterNewOrderCreation();
    }

    private void mockExternalForNewOrderCreation() {
        mockCommonExternalForNewOrderCreation();

        when(lmsClient.getLogisticsPoint(SC_LOGISTICS_POINT_ID)).thenReturn(Optional.of(SC_LOGISTICS_POINT_RESPONSE));
        when(lmsClient.searchPartners(SearchPartnerFilter.builder()
            .setIds(Set.of(FF_PARTNER_ID, SC_PARTNER_ID, COURIER_PARTNER_ID))
            .build()
        ))
            .thenReturn(List.of(
                FF_PARTNER_RESPONSE,
                SC_PARTNER_RESPONSE,
                COURIER_PARTNER_RESPONSE
            ));
        when(lmsClient.searchPartners(SearchPartnerFilter.builder()
                .setIds(Set.of(FF_PARTNER_ID, SC_PARTNER_ID))
                .build())
        )
                .thenReturn(List.of(FF_PARTNER_RESPONSE, SC_PARTNER_RESPONSE));
        when(lmsClient.getLogisticsPoints(LogisticsPointFilter.newBuilder()
                .ids(Set.of(
                        FF_LOGISTICS_POINT_ID,
                        SC_LOGISTICS_POINT_ID
                ))
                .build())
        )
                .thenReturn(List.of(
                        FF_LOGISTICS_POINT_RESPONSE,
                        SC_LOGISTICS_POINT_RESPONSE
                ));

        when(marketIdService.findAccountById(COURIER_MARKET_ID)).thenReturn(Optional.of(COURIER_MARKET_ACCOUNT));
    }

    private void verifyExternalAfterNewOrderCreation() {
        verifyCommonExternalAfterNewOrderCreation();

        verify(lmsClient).searchPartners(SearchPartnerFilter.builder()
            .setIds(Set.of(FF_PARTNER_ID, SC_PARTNER_ID, COURIER_PARTNER_ID))
            .build()
        );
        verify(lmsClient).searchPartners(SearchPartnerFilter.builder()
            .setIds(Set.of(FF_PARTNER_ID, SC_PARTNER_ID))
            .build()
        );
        verify(lmsClient).getLogisticsPoint(SC_LOGISTICS_POINT_ID);
        verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder()
            .ids(Set.of(FF_LOGISTICS_POINT_ID, SC_LOGISTICS_POINT_ID))
            .build()
        );
        verify(lmsClient).searchPartnerRelation(PartnerRelationFilter.newBuilder()
            .fromPartnersIds(Set.of(FF_PARTNER_ID, SC_PARTNER_ID))
            .build()
        );
        verify(marketIdService).findAccountById(COURIER_MARKET_ID);
    }

    private void testFail(String message) {
        softly.assertThatThrownBy(() -> processor.processPayload(CHANGE_ORDER_REQUEST_PAYLOAD))
            .isInstanceOf(DbQueueJobExecutionException.class)
            .hasMessage("Error while UpdateLastMileToCourierProcessor processing, changeOrderRequestId=101")
            .getCause()
            .hasMessage(message);
    }
}
