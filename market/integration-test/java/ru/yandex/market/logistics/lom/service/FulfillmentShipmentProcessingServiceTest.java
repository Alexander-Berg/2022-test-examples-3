package ru.yandex.market.logistics.lom.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.exception.http.ResourceNotFoundException;
import ru.yandex.market.logistics.lom.jobs.model.ShipmentApplicationIdPayload;
import ru.yandex.market.logistics.lom.jobs.processor.FulfillmentShipmentProcessingService;
import ru.yandex.market.logistics.lom.service.marketid.MarketIdService;
import ru.yandex.market.logistics.lom.utils.LmsFactory;
import ru.yandex.market.logistics.lom.utils.MarketIdFactory;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.converter.lgw.LgwClientRequestMetaConverter.convertSequenceIdToClientRequestMeta;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwCommonEntitiesUtils.createPartner;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwFulfillmentEntitiesUtils.createIntakeBuilder;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwFulfillmentEntitiesUtils.createSelfExport;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwFulfillmentEntitiesUtils.createWarehouseBuilder;

@DisplayName("Тесты создания отгрузки в СЦ")
class FulfillmentShipmentProcessingServiceTest extends AbstractContextualTest {
    private static final ShipmentApplicationIdPayload SHIPMENT_APPLICATION_ID_PAYLOAD =
        PayloadFactory.createShipmentApplicationIdPayload(1, "1", 1);

    @Autowired
    private FulfillmentShipmentProcessingService fulfillmentShipmentProcessingService;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private MarketIdService marketIdService;

    @Autowired
    private FulfillmentClient fulfillmentClient;

    @AfterEach
    void afterEach() {
        verifyNoMoreInteractions(fulfillmentClient, lmsClient, marketIdService);
    }

    @Test
    @DisplayName("Создание отгрузки без заявки")
    void createWithdrawWithoutApplication() {
        softly.assertThatThrownBy(
                () -> fulfillmentShipmentProcessingService.processPayload(SHIPMENT_APPLICATION_ID_PAYLOAD)
            )
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [SHIPMENT_APPLICATION] with id [1]");
    }

    @Test
    @DisplayName("Создание забора для отменённой заявки")
    @DatabaseSetup("/service/shipmentapplication/before/setup.xml")
    @DatabaseSetup(
        value = "/service/shipmentapplication/before/application_is_cancelled.xml",
        type = DatabaseOperation.REFRESH
    )
    void createWithdrawForCancelledApplication() {
        fulfillmentShipmentProcessingService.processPayload(SHIPMENT_APPLICATION_ID_PAYLOAD);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DisplayName("Создание забора без существующего склада")
    @DatabaseSetup("/service/shipmentapplication/before/setup.xml")
    void createWithdrawWithoutLogisticPoint() {
        Throwable throwable = catchThrowable(
            () -> fulfillmentShipmentProcessingService.processPayload(SHIPMENT_APPLICATION_ID_PAYLOAD)
        );

        softly.assertThat(throwable.getCause())
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [LOGISTICS_POINT] with id [3]");
        verify(lmsClient).getLogisticsPoint(3L);
    }

    @Test
    @DisplayName("Создание забора без маркет-аккаунта")
    @DatabaseSetup("/service/shipmentapplication/before/setup.xml")
    void createWithdrawWithoutMarketAccount() {
        mockLmsClient(3L);
        when(marketIdService.findAccountById(1L)).thenReturn(Optional.empty());
        Throwable throwable = catchThrowable(
            () -> fulfillmentShipmentProcessingService.processPayload(SHIPMENT_APPLICATION_ID_PAYLOAD)
        );

        softly.assertThat(throwable.getCause())
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [LEGAL_INFO] with id [1]");
        verify(lmsClient).getLogisticsPoint(3L);
        verify(marketIdService).findAccountById(1L);
    }

    @Test
    @DisplayName("Успешное создание забора в СЦ")
    @DatabaseSetup("/service/shipmentapplication/before/setup.xml")
    void createWithdrawSuccess() throws Exception {
        mockLmsClient(3L);

        fulfillmentShipmentProcessingService.processPayload(SHIPMENT_APPLICATION_ID_PAYLOAD);
        verify(fulfillmentClient).createIntake(
            eq(createIntakeBuilder().build()),
            eq(createPartner(5)),
            eq(convertSequenceIdToClientRequestMeta(1L))
        );
        verify(lmsClient).getLogisticsPoint(3L);
        verify(marketIdService).findAccountById(1L);
    }

    @Test
    @DisplayName("Успешное создание забора в СЦ с отсутствующим контактом")
    @DatabaseSetup("/service/shipmentapplication/before/setup.xml")
    void createWithdrawWithoutContact() throws Exception {
        when(lmsClient.getLogisticsPoint(3L)).thenReturn(
            Optional.of(
                LmsFactory.createLogisticsPointResponse(3L, 5L, "Warehouse 3", PointType.WAREHOUSE)
                    .contact(null)
                    .build()
            )
        );
        when(marketIdService.findAccountById(1L)).thenReturn(Optional.of(MarketIdFactory.marketAccount()));

        fulfillmentShipmentProcessingService.processPayload(SHIPMENT_APPLICATION_ID_PAYLOAD);
        verify(fulfillmentClient).createIntake(
            eq(createIntakeBuilder(createWarehouseBuilder("3").setContact(null)).build()),
            eq(createPartner(5)),
            eq(convertSequenceIdToClientRequestMeta(1L))
        );
        verify(lmsClient).getLogisticsPoint(3L);
        verify(marketIdService).findAccountById(1L);
    }

    @Test
    @DisplayName("Успешное создание забора в СЦ с отсутствующими телефонами")
    @DatabaseSetup("/service/shipmentapplication/before/setup.xml")
    void createWithdrawWithoutPhones() throws Exception {
        when(lmsClient.getLogisticsPoint(3L)).thenReturn(
            Optional.of(
                LmsFactory.createLogisticsPointResponse(3L, 5L, "Warehouse 3", PointType.WAREHOUSE)
                    .phones(null)
                    .build()
            )
        );
        when(marketIdService.findAccountById(1L)).thenReturn(Optional.of(MarketIdFactory.marketAccount()));

        fulfillmentShipmentProcessingService.processPayload(SHIPMENT_APPLICATION_ID_PAYLOAD);
        verify(fulfillmentClient).createIntake(
            eq(createIntakeBuilder(createWarehouseBuilder("3").setPhones(null)).build()),
            eq(createPartner(5)),
            eq(convertSequenceIdToClientRequestMeta(1L))
        );
        verify(lmsClient).getLogisticsPoint(3L);
        verify(marketIdService).findAccountById(1L);
    }

    @Test
    @DisplayName("Создание самопривоза без существующего склада")
    @DatabaseSetup("/service/shipmentapplication/before/setup.xml")
    @DatabaseSetup(
        value = "/service/shipmentapplication/before/import_shipment_type.xml",
        type = DatabaseOperation.REFRESH
    )
    void createImportWithoutLogisticPoint() {
        Throwable throwable = catchThrowable(
            () -> fulfillmentShipmentProcessingService.processPayload(SHIPMENT_APPLICATION_ID_PAYLOAD)
        );

        softly.assertThat(throwable.getCause())
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [LOGISTICS_POINT] with id [3, 4]");
        verify(lmsClient).getLogisticsPoints(pointFilter(Set.of(3L, 4L)));
    }

    @Test
    @DisplayName("Создание самопривоза без маркет-аккаунта")
    @DatabaseSetup("/service/shipmentapplication/before/setup.xml")
    @DatabaseSetup(
        value = "/service/shipmentapplication/before/import_shipment_type.xml",
        type = DatabaseOperation.REFRESH
    )
    void createImportWithoutMarketAccount() {
        mockLmsClient(3L, 4L);
        when(marketIdService.findAccountById(500L)).thenReturn(Optional.empty());

        Throwable throwable = catchThrowable(
            () -> fulfillmentShipmentProcessingService.processPayload(SHIPMENT_APPLICATION_ID_PAYLOAD)
        );

        softly.assertThat(throwable.getCause())
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [LEGAL_INFO] with id [500]");
        verify(lmsClient).getPartner(5L);
        verify(lmsClient).getLogisticsPoints(pointFilter(Set.of(3L, 4L)));
        verify(marketIdService).findAccountById(500L);
    }

    @Test
    @DisplayName("Успешное создание самопривоза в СЦ")
    @DatabaseSetup("/service/shipmentapplication/before/setup.xml")
    @DatabaseSetup(
        value = "/service/shipmentapplication/before/import_shipment_type.xml",
        type = DatabaseOperation.REFRESH
    )
    void createImportSuccess() throws Exception {
        mockLmsClient(3L, 4L);
        when(marketIdService.findAccountById(1L)).thenReturn(Optional.of(MarketIdFactory.marketAccount()));

        fulfillmentShipmentProcessingService.processPayload(SHIPMENT_APPLICATION_ID_PAYLOAD);
        verify(fulfillmentClient).createSelfExport(
            eq(createSelfExport()),
            eq(createPartner(5)),
            eq(convertSequenceIdToClientRequestMeta(1L))
        );
        verify(lmsClient).getLogisticsPoints(pointFilter(Set.of(3L, 4L)));
        verify(lmsClient).getPartner(5L);
        verify(marketIdService).findAccountById(500L);
    }

    @Test
    @DisplayName("Создание самопривоза - партнер не найден")
    @DatabaseSetup("/service/shipmentapplication/before/setup.xml")
    @DatabaseSetup(
        value = "/service/shipmentapplication/before/import_shipment_type.xml",
        type = DatabaseOperation.REFRESH
    )
    void createImportWithoutPartner() {
        mockLmsClient(3L, 4L);
        when(lmsClient.getPartner(5L)).thenReturn(Optional.empty());

        Throwable throwable = catchThrowable(
            () -> fulfillmentShipmentProcessingService.processPayload(SHIPMENT_APPLICATION_ID_PAYLOAD)
        );

        softly.assertThat(throwable.getCause())
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [PARTNER] with id [5]");
        verify(lmsClient).getPartner(5L);
        verify(lmsClient).getLogisticsPoints(pointFilter(Set.of(3L, 4L)));
    }

    @Test
    @DisplayName("Создание самопривоза - у партнера нет marketId")
    @DatabaseSetup("/service/shipmentapplication/before/setup.xml")
    @DatabaseSetup(
        value = "/service/shipmentapplication/before/import_shipment_type.xml",
        type = DatabaseOperation.REFRESH
    )
    void createImportWithoutPartnerMarketId() {
        mockLmsClient(3L, 4L);
        when(lmsClient.getPartner(5L)).thenReturn(partner(5L, null));

        Throwable throwable = catchThrowable(
            () -> fulfillmentShipmentProcessingService.processPayload(SHIPMENT_APPLICATION_ID_PAYLOAD)
        );

        softly.assertThat(throwable.getCause())
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Cannot find MARKET_ID for partner with id [5]");
        verify(lmsClient).getPartner(5L);
        verify(lmsClient).getLogisticsPoints(pointFilter(Set.of(3L, 4L)));
    }

    @Test
    @DisplayName("Создание забора - ЯДо")
    @DatabaseSetup({"/service/shipmentapplication/before/setup.xml", "/service/shipmentapplication/before/waybill.xml"})
    void createWithdrawDaas() throws GatewayApiException {
        when(lmsClient.getLogisticsPoint(3L)).thenReturn(Optional.of(
            LmsFactory.createLogisticsPointResponse(3L, null, "Warehouse", PointType.WAREHOUSE).build()
        ));
        when(marketIdService.findAccountById(1L)).thenReturn(Optional.of(MarketIdFactory.marketAccount()));

        fulfillmentShipmentProcessingService.processPayload(SHIPMENT_APPLICATION_ID_PAYLOAD);
        verify(fulfillmentClient).createIntake(
            eq(createIntakeBuilder(createWarehouseBuilder("3")).build()),
            eq(createPartner(5)),
            eq(convertSequenceIdToClientRequestMeta(1L))
        );
        verify(lmsClient).getLogisticsPoint(3L);
        verify(marketIdService).findAccountById(1L);
    }

    private void mockLmsClient(long id) {
        when(lmsClient.getLogisticsPoint(id)).thenReturn(
            Optional.of(
                LmsFactory.createLogisticsPointResponse(id, 6L, "Warehouse", PointType.WAREHOUSE).build())
        );
        when(lmsClient.getPartner(eq(6L))).thenReturn(partner(6L, 600L));
        when(marketIdService.findAccountById(1L)).thenReturn(Optional.of(MarketIdFactory.marketAccount()));
    }

    private void mockLmsClient(long id1, long id2) {
        when(lmsClient.getLogisticsPoints(pointFilter(Set.of(id1, id2))))
            .thenReturn(List.of(
                LmsFactory.createLogisticsPointResponse(id2, 5L, "Warehouse", PointType.WAREHOUSE)
                    .build(),
                LmsFactory.createLogisticsPointResponse(id1, 6L, "Warehouse", PointType.WAREHOUSE)
                    .build()
            ));
        when(lmsClient.getPartner(eq(5L))).thenReturn(partner(5L, 500L));
        when(marketIdService.findAccountById(500L)).thenReturn(Optional.of(MarketIdFactory.marketAccount()));
        when(lmsClient.getPartner(eq(6L))).thenReturn(partner(6L, 600L));
        when(marketIdService.findAccountById(600L)).thenReturn(Optional.of(MarketIdFactory.marketAccount()));
    }

    @Nonnull
    private Optional<PartnerResponse> partner(long id, @Nullable Long marketId) {
        return Optional.of(
            PartnerResponse.newBuilder()
                .id(id)
                .marketId(marketId)
                .partnerType(PartnerType.DELIVERY)
                .build()
        );
    }

    @Nonnull
    private LogisticsPointFilter pointFilter(Set<Long> longs) {
        return LogisticsPointFilter.newBuilder().ids(longs).build();
    }
}
