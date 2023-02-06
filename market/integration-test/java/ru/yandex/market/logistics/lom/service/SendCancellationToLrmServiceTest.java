package ru.yandex.market.logistics.lom.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.SendCancellationToLrmService;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lrm.client.api.ReturnsApi;
import ru.yandex.market.logistics.lrm.client.model.CancellationReturnBox;
import ru.yandex.market.logistics.lrm.client.model.CancellationReturnItem;
import ru.yandex.market.logistics.lrm.client.model.CreateCancellationReturnRequest;
import ru.yandex.market.logistics.lrm.client.model.CreateDeliveryServiceCancellationReturnRequest;
import ru.yandex.market.logistics.lrm.client.model.CreateExpressCancellationReturnRequest;
import ru.yandex.market.logistics.lrm.client.model.CreateReturnResponse;
import ru.yandex.market.logistics.lrm.client.model.Dimensions;
import ru.yandex.market.logistics.lrm.client.model.LogisticPointType;
import ru.yandex.market.logistics.lrm.client.model.OrderItemInfo;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createOrderIdPayload;

class SendCancellationToLrmServiceTest extends AbstractContextualTest {

    @Autowired
    private SendCancellationToLrmService sendCancellationToLrmService;

    @Autowired
    private ReturnsApi returnsApi;

    @Autowired
    private LMSClient lmsClient;

    @BeforeEach
    void setup() {
        when(lmsClient.getLogisticsPoint(10000000003L))
            .thenReturn(Optional.of(LogisticsPointResponse.newBuilder().partnerId(9000L).build()));
        when(returnsApi.createAndCommitExpressCancellationReturn(any(CreateExpressCancellationReturnRequest.class)))
            .thenReturn(new CreateReturnResponse().id(100L));
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient, returnsApi);
    }

    @Test
    @DisplayName("Успешная отправка")
    @DatabaseSetup("/service/send_cancellation_to_lrm/before/setup.xml")
    @DatabaseSetup(
        value = "/service/send_cancellation_to_lrm/before/root.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/service/send_cancellation_to_lrm/before/place_with_dimensions.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/service/send_cancellation_to_lrm/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @JpaQueriesCount(21)
    void success() {
        when(returnsApi.createAndCommitCancellationReturn(any(CreateCancellationReturnRequest.class)))
            .thenReturn(new CreateReturnResponse().id(100L));

        sendCancellationToLrmService.processPayload(createOrderIdPayload(1, 1));

        verifyCreate(
            new Dimensions()
                .length(1)
                .height(2)
                .width(3)
                .weight(4354),
            "storage-unit-external-id"
        );
    }

    @Test
    @DisplayName("Успешная отправка. Возврат для данного заказа уже существует")
    @DatabaseSetup("/service/send_cancellation_to_lrm/before/setup.xml")
    @DatabaseSetup(
        value = "/service/send_cancellation_to_lrm/before/root.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/service/send_cancellation_to_lrm/before/place_with_dimensions.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/service/send_cancellation_to_lrm/after/success.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/service/send_cancellation_to_lrm/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @JpaQueriesCount(7)
    void successAlreadyExist() {
        when(returnsApi.createAndCommitCancellationReturn(any(CreateCancellationReturnRequest.class)))
            .thenReturn(new CreateReturnResponse().id(100L));

        sendCancellationToLrmService.processPayload(createOrderIdPayload(1, 1));

        verifyCreate(
            new Dimensions()
                .length(1)
                .height(2)
                .width(3)
                .weight(4354),
            "storage-unit-external-id"
        );
    }

    @Test
    @DisplayName("Успешная отправка. Не заполнены весогабариты места, берем из рутового юнита")
    @DatabaseSetup("/service/send_cancellation_to_lrm/before/setup.xml")
    @DatabaseSetup(
        value = "/service/send_cancellation_to_lrm/before/root.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/service/send_cancellation_to_lrm/before/place.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/service/send_cancellation_to_lrm/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @JpaQueriesCount(21)
    void successNoPlaceDimensions() {
        when(returnsApi.createAndCommitCancellationReturn(any(CreateCancellationReturnRequest.class)))
            .thenReturn(new CreateReturnResponse().id(100L));

        sendCancellationToLrmService.processPayload(createOrderIdPayload(1, 1));

        verifyCreate(
            new Dimensions()
                .length(10)
                .height(20)
                .width(30)
                .weight(40352),
            "storage-unit-external-id"
        );
    }

    @Test
    @DisplayName("Успешная отправка. Нет плейсов, берем рутовый юнит и создаем из него одну коробку")
    @DatabaseSetup("/service/send_cancellation_to_lrm/before/setup.xml")
    @DatabaseSetup(
        value = "/service/send_cancellation_to_lrm/before/root.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/service/send_cancellation_to_lrm/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @JpaQueriesCount(21)
    void successNoPlaces() {
        when(returnsApi.createAndCommitCancellationReturn(any(CreateCancellationReturnRequest.class)))
            .thenReturn(new CreateReturnResponse().id(100L));

        sendCancellationToLrmService.processPayload(createOrderIdPayload(1, 1));

        verifyCreate(
            new Dimensions()
                .length(10)
                .height(20)
                .width(30)
                .weight(40352),
            "order-barcode"
        );
    }

    @Test
    @DisplayName("Успешная отправка. Несколько плейсов, не привязываем товары к коробкам")
    @DatabaseSetup({
        "/service/send_cancellation_to_lrm/before/setup.xml",
        "/service/send_cancellation_to_lrm/before/root.xml",
        "/service/send_cancellation_to_lrm/before/multiple_places.xml",
    })
    @ExpectedDatabase(
        value = "/service/send_cancellation_to_lrm/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @JpaQueriesCount(21)
    void successMultiplePlaces() {
        when(returnsApi.createAndCommitCancellationReturn(any(CreateCancellationReturnRequest.class)))
            .thenReturn(new CreateReturnResponse().id(100L));

        sendCancellationToLrmService.processPayload(createOrderIdPayload(1, 1));

        verifyCreate(
            new CancellationReturnBox()
                .externalId("first-place-external-id")
                .dimensions(
                    new Dimensions()
                        .length(1)
                        .height(2)
                        .width(3)
                        .weight(4000)
                ),
            new CancellationReturnBox()
                .externalId("second-place-external-id")
                .dimensions(
                    new Dimensions()
                        .length(2)
                        .height(4)
                        .width(6)
                        .weight(8000)
                )
        );
    }

    @Test
    @DisplayName("Ошибка. Нет ни плейсов, ни рутового юнита")
    @DatabaseSetup("/service/send_cancellation_to_lrm/before/setup.xml")
    @JpaQueriesCount(4)
    void errorNoRootUnit() {
        softly.assertThatThrownBy(() -> sendCancellationToLrmService.processPayload(createOrderIdPayload(1, 1)))
            .hasMessage("Not found root unit for order 1");
    }

    @Test
    @DisplayName("Успешная отправка - экспресс")
    @DatabaseSetup("/service/send_cancellation_to_lrm/before/setup_express.xml")
    @DatabaseSetup(
        value = "/service/send_cancellation_to_lrm/before/root.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/service/send_cancellation_to_lrm/before/place_with_dimensions.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/service/send_cancellation_to_lrm/after/success_express.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @JpaQueriesCount(24)
    void successExpress() {
        sendCancellationToLrmService.processPayload(createOrderIdPayload(1, 1));

        verifyCreateExpress(new Dimensions().length(1).height(2).width(3).weight(4354), "storage-unit-external-id");

        verify(lmsClient).getLogisticsPoint(10000000003L);
    }

    @Test
    @DisplayName("Успешная отправка - экспресс. Не заполнены весогабариты места, берем из рутового юнита")
    @DatabaseSetup("/service/send_cancellation_to_lrm/before/setup_express.xml")
    @DatabaseSetup(
        value = "/service/send_cancellation_to_lrm/before/root.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/service/send_cancellation_to_lrm/before/place.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/service/send_cancellation_to_lrm/after/success_express.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @JpaQueriesCount(24)
    void successNoPlaceDimensionsExpress() {
        sendCancellationToLrmService.processPayload(createOrderIdPayload(1, 1));

        verifyCreateExpress(new Dimensions().length(10).height(20).width(30).weight(40352), "storage-unit-external-id");

        verify(lmsClient).getLogisticsPoint(10000000003L);
    }

    @Test
    @DisplayName("Успешная отправка - экспресс. Нет плейсов, берем рутовый юнит и создаем из него одну коробку")
    @DatabaseSetup("/service/send_cancellation_to_lrm/before/setup_express.xml")
    @DatabaseSetup(
        value = "/service/send_cancellation_to_lrm/before/root.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/service/send_cancellation_to_lrm/after/success_express.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @JpaQueriesCount(24)
    void successNoPlacesExpress() {
        sendCancellationToLrmService.processPayload(createOrderIdPayload(1, 1));

        verifyCreateExpress(new Dimensions().length(10).height(20).width(30).weight(40352), "order-barcode");

        verify(lmsClient).getLogisticsPoint(10000000003L);
    }

    @Test
    @DisplayName("Ошибка - экспресс. Нет ни плейсов, ни рутового юнита")
    @DatabaseSetup("/service/send_cancellation_to_lrm/before/setup_express.xml")
    @JpaQueriesCount(4)
    void errorNoRootUnitExpress() {
        softly.assertThatThrownBy(() -> sendCancellationToLrmService.processPayload(createOrderIdPayload(1, 1)))
            .hasMessage("Not found root unit for order 1");
    }

    @Test
    @DisplayName("Ошибка - экспресс. Не найден склад")
    @DatabaseSetup("/service/send_cancellation_to_lrm/before/setup_express.xml")
    @DatabaseSetup(
        value = "/service/send_cancellation_to_lrm/before/root.xml",
        type = DatabaseOperation.INSERT
    )
    @JpaQueriesCount(8)
    void noWarehouseExpress() {
        when(lmsClient.getLogisticsPoint(10000000003L)).thenReturn(Optional.empty());

        softly.assertThatThrownBy(() -> sendCancellationToLrmService.processPayload(createOrderIdPayload(1, 1)))
            .hasMessage("Failed to find [LOGISTICS_POINT] with id [10000000003]");

        verify(lmsClient).getLogisticsPoint(10000000003L);
    }

    @Test
    @DisplayName("Ничего не делаем, если нет сц для невыкупов")
    @DatabaseSetup("/service/send_cancellation_to_lrm/before/setup_express.xml")
    @DatabaseSetup(
        value = "/service/send_cancellation_to_lrm/before/drop_return_sc.xml",
        type = DatabaseOperation.UPDATE
    )
    @JpaQueriesCount(3)
    void withoutReturnScExpress() {
        sendCancellationToLrmService.processPayload(createOrderIdPayload(1, 1));
    }

    @Test
    @DisplayName("Успешная отправка - DBS")
    @DatabaseSetup("/service/send_cancellation_to_lrm/before/setup_dbs.xml")
    @ExpectedDatabase(
        value = "/service/send_cancellation_to_lrm/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @JpaQueriesCount(27)
    void successDbs() {
        when(returnsApi.createAndCommitDeliveryServiceCancellationReturn(any(
            CreateDeliveryServiceCancellationReturnRequest.class
        )))
            .thenReturn(new CreateReturnResponse().id(100L));

        LogisticsPointFilter logisticsPointFilter = LogisticsPointFilter.newBuilder()
            .ids(Set.of(10001774736L, 10005716456L))
            .build();
        when(lmsClient.getLogisticsPoints(logisticsPointFilter))
            .thenReturn(List.of(
                LogisticsPointResponse.newBuilder()
                    .id(10001774736L)
                    .externalId("4885")
                    .partnerId(102L)
                    .name("pickup-point-name")
                    .build(),
                LogisticsPointResponse.newBuilder()
                    .id(10005716456L)
                    .externalId("2cf84556-89aa-4916-b66a-a52e4e626cb6")
                    .partnerId(101L)
                    .build()
            ));

        sendCancellationToLrmService.processPayload(createOrderIdPayload(1, 1));

        verifyCreateDbs();
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter);
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PUSH_CANCELLATION_RETURN_DELIVERY_SERVICE_STATUSES,
            PayloadFactory.createOrderIdPayload(1L, "1", 1)
        );
    }

    private void verifyCreateDbs() {
        verify(returnsApi).createAndCommitDeliveryServiceCancellationReturn(
            new CreateDeliveryServiceCancellationReturnRequest()
                .orderExternalId("order-barcode")
                .logisticPointId(10001774736L)
                .logisticPointExternalId("4885")
                .logisticPointPartnerId(102L)
                .logisticPointType(LogisticPointType.PICKUP)
                .logisticPointName("pickup-point-name")
                .shopLogisticPointId(10005716456L)
                .shopLogisticPointExternalId("2cf84556-89aa-4916-b66a-a52e4e626cb6")
                .shopPartnerId(101L)
                .shopId(1L)
                .shopName("Сантех Строй")
                .boxes(List.of(
                    new CancellationReturnBox()
                        .externalId("order-barcode")
                        .dimensions(
                            new Dimensions()
                                .weight(1000)
                                .length(10)
                                .width(30)
                                .height(20)
                        )
                ))
                .items(List.of(
                    new CancellationReturnItem()
                        .supplierId(1L)
                        .vendorCode("art-1")
                        .assessedCost(BigDecimal.ONE)
                ))
                .orderItemsInfo(List.of(
                    new OrderItemInfo()
                        .supplierId(1L)
                        .vendorCode("art-1")
                        .instances(List.of())
                ))
        );
    }

    private void verifyCreateExpress(Dimensions dimensions, String boxExternalId) {
        verify(returnsApi).createAndCommitExpressCancellationReturn(
            new CreateExpressCancellationReturnRequest()
                .shopId(1L)
                .shopPartnerId(100L)
                .returnPartnerSortingCenterId(9000L)
                .returnSortingCenterId(10000000003L)
                .orderExternalId("order-barcode")
                .boxes(List.of(
                    new CancellationReturnBox()
                        .externalId(boxExternalId)
                        .dimensions(dimensions)
                ))
                .items(List.of(
                    new CancellationReturnItem()
                        .supplierId(1L)
                        .vendorCode("test-item-article-1")
                        .assessedCost(BigDecimal.valueOf(200)),
                    new CancellationReturnItem()
                        .supplierId(1L)
                        .vendorCode("test-item-article-1")
                        .assessedCost(BigDecimal.valueOf(200)),
                    new CancellationReturnItem()
                        .supplierId(2L)
                        .vendorCode("test-item-article-2")
                        .assessedCost(BigDecimal.valueOf(400))
                ))
                .orderItemsInfo(List.of(
                    new OrderItemInfo()
                        .supplierId(1L)
                        .vendorCode("test-item-article-1")
                        .instances(List.of(
                            Map.of("SN", "SC02DX3V9Q6LD-1"),
                            Map.of("SN", "SC02DX3V9Q6LD-2")
                        )),
                    new OrderItemInfo()
                        .supplierId(2L)
                        .vendorCode("test-item-article-2")
                        .instances(List.of(
                            Map.of("UIT", "BTYFVUINI")
                        ))
                ))
        );
    }

    private void verifyCreate(Dimensions dimensions, String boxExternalId) {
        verifyCreate(
            new CancellationReturnBox()
                .externalId(boxExternalId)
                .dimensions(dimensions)
        );
    }

    private void verifyCreate(CancellationReturnBox... boxes) {
        verify(returnsApi).createAndCommitCancellationReturn(
            new CreateCancellationReturnRequest()
                .orderExternalId("order-barcode")
                .boxes(List.of(boxes))
                .items(List.of(
                    new CancellationReturnItem()
                        .supplierId(1L)
                        .vendorCode("test-item-article-1"),
                    new CancellationReturnItem()
                        .supplierId(1L)
                        .vendorCode("test-item-article-1"),
                    new CancellationReturnItem()
                        .supplierId(2L)
                        .vendorCode("test-item-article-2")
                ))
                .orderItemsInfo(List.of(
                    new OrderItemInfo()
                        .supplierId(1L)
                        .vendorCode("test-item-article-1")
                        .instances(List.of(
                            Map.of("SN", "SC02DX3V9Q6LD-1"),
                            Map.of("SN", "SC02DX3V9Q6LD-2")
                        )),
                    new OrderItemInfo()
                        .supplierId(2L)
                        .vendorCode("test-item-article-2")
                        .instances(List.of(
                            Map.of("UIT", "BTYFVUINI")
                        ))
                ))
        );
    }
}
