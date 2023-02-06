package ru.yandex.market.pvz.internal.controller.pi.shipment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.client.model.order.DeliveryServiceType;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.delivery_service.DeliveryService;
import ru.yandex.market.pvz.core.domain.dispatch.model.DispatchType;
import ru.yandex.market.pvz.core.domain.order.OrderAdditionalInfoCommandService;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus;
import ru.yandex.market.pvz.core.domain.order.model.place.OrderPlace;
import ru.yandex.market.pvz.core.domain.order.model.sender.OrderSender;
import ru.yandex.market.pvz.core.domain.order_delivery_result.ItemDeliveryFlow;
import ru.yandex.market.pvz.core.domain.order_delivery_result.service.OrderDeliveryResultCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.InMemoryPickupPointService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointRequestData;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointSimpleParams;
import ru.yandex.market.pvz.core.domain.returns.model.ReturnRequestParams;
import ru.yandex.market.pvz.core.domain.shipment.ShipmentCommandService;
import ru.yandex.market.pvz.core.domain.shipment.model.Shipment;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentCreateItemParams;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentCreateParams;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentStatus;
import ru.yandex.market.pvz.core.domain.shipment.model.ShipmentType;
import ru.yandex.market.pvz.core.domain.yandex.YandexMigrationManager;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestOrderSenderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.core.test.factory.TestReturnRequestFactory;
import ru.yandex.market.pvz.core.test.factory.TestShipmentsFactory;
import ru.yandex.market.pvz.internal.BaseShallowTest;
import ru.yandex.market.pvz.internal.WebLayerTest;
import ru.yandex.market.pvz.internal.controller.pi.shipment.dto.ShipmentActDto;
import ru.yandex.market.pvz.internal.controller.pi.shipment.dto.ShipmentReceiveSummaryDto;
import ru.yandex.market.tpl.common.transferact.client.api.TransferApi;
import ru.yandex.market.tpl.common.transferact.client.model.ActorDto;
import ru.yandex.market.tpl.common.transferact.client.model.ActorTypeDto;
import ru.yandex.market.tpl.common.transferact.client.model.ItemQualifierDto;
import ru.yandex.market.tpl.common.transferact.client.model.PendingTransferDto;
import ru.yandex.market.tpl.common.transferact.client.model.RegistryDirectionDto;
import ru.yandex.market.tpl.common.transferact.client.model.RegistryDto;
import ru.yandex.market.tpl.common.transferact.client.model.RegistryItemDto;
import ru.yandex.market.tpl.common.transferact.client.model.RegistryItemTypeDto;
import ru.yandex.market.tpl.common.transferact.client.model.SignatureDto;
import ru.yandex.market.tpl.common.transferact.client.model.TransferCreateRequestDto;
import ru.yandex.market.tpl.common.transferact.client.model.TransferDto;
import ru.yandex.market.tpl.common.transferact.client.model.TransferQualifierDto;
import ru.yandex.market.tpl.common.transferact.client.model.TransferQualifierTypeDto;
import ru.yandex.market.tpl.common.transferact.client.model.TransferStatus;
import ru.yandex.market.tpl.common.transferact.client.model.TwoActorQualifierDto;
import ru.yandex.market.tpl.common.util.logging.Tracer;
import ru.yandex.market.tpl.common.util.security.QRCodeUtil;
import ru.yandex.market.tpl.common.util.security.QrCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pvz.core.TestUtils.getFileContent;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.OPTIMIZE_SHIPMENT_RECEIVE_ENABLED;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.READY_FOR_RETURN;
import static ru.yandex.market.pvz.core.domain.shipment.model.ShipmentStatus.CANCELLED;
import static ru.yandex.market.pvz.core.domain.shipment.model.ShipmentStatus.FINISHED;
import static ru.yandex.market.pvz.core.domain.shipment.model.ShipmentStatus.PENDING;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.OrganizationTestParams.DEFAULT_FULL_ORGANIZATION_NAME;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.BARCODE_1;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.BARCODE_2;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.BARCODE_3;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.UIT_1_1;
import static ru.yandex.market.pvz.core.test.factory.TestShipmentsFactory.OPERATOR_ID;
import static ru.yandex.market.pvz.core.test.factory.TestShipmentsFactory.OPERATOR_LOGIN;
import static ru.yandex.market.tpl.common.util.StringFormatter.sf;

@WebLayerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ShipmentsControllerTest extends BaseShallowTest {
    private static final long LMS_ID = 123;
    private static final String TRANSFER_ID = "transfer id";
    private static final String TRANSFER_ID_WITH_DISCREPANCIES = "transfer id 2";

    @MockBean
    private ShipmentReportService shipmentReportService;

    @MockBean
    private InMemoryPickupPointService inMemoryPickupPointService;

    @Test
    @SneakyThrows
    void testGetReceiveSummary() {
        doReturn(PickupPointSimpleParams.builder().pvzMarketId(1L).build())
                .when(inMemoryPickupPointService).getByPvzMarketIdOrThrow(anyLong());

        doReturn(ShipmentReceiveSummaryDto.builder()
                .totalPrice(BigDecimal.valueOf(100))
                .leftPrice(BigDecimal.valueOf(10))
                .acceptedPrice(BigDecimal.valueOf(90))
                .totalOrdersCount(3)
                .acceptedOrdersCount(2)
                .leftOrdersCount(1)
                .build())
                .when(shipmentReportService).getReceiveSummary(any(), any());

        mockMvc.perform(
                get("/v1/pi/pickup-points/1/shipments/receive/summary?date=" + LocalDate.now()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("" +
                        "{  " +
                        "   \"totalOrdersCount\":3,  " +
                        "   \"totalPrice\":100,  " +
                        "   \"acceptedOrdersCount\":2,  " +
                        "   \"acceptedPrice\":90,  " +
                        "   \"leftOrdersCount\":1,  " +
                        "   \"leftPrice\":10  " +
                        "}"
                ));
    }

    @Test
    @SneakyThrows
    void testGetReceiveAct() {
        var deliveryService = DeliveryService.builder().name("ASHOT DELIVERY SOLUTIONS UNLIMITED").token("1").build();
        var pickupPoint = PickupPoint.builder().pvzMarketId(1L).build();
        var date = LocalDate.of(2020, 7, 31);

        doReturn(PickupPointSimpleParams.builder().pvzMarketId(1L).build())
                .when(inMemoryPickupPointService).getByPvzMarketIdOrThrow(anyLong());

        doReturn(ShipmentActDto.builder()
                .number("228")
                .date(date)
                .shipments(List.of(
                        ShipmentActDto.Shipment.builder()
                                .id("111")
                                .totalSum(BigDecimal.TEN)
                                .items(2)
                                .build(),

                        ShipmentActDto.Shipment.builder()
                                .id("222")
                                .totalSum(BigDecimal.ONE)
                                .items(1)
                                .build()
                ))
                .totalSum(BigDecimal.valueOf(11))
                .totalItems(3)
                .executor(deliveryService.getName())
                .sender(deliveryService.getName())
                .build())
                .when(shipmentReportService).getReceiveAct(any(), any());

        mockMvc.perform(
                get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() +
                        "/shipments/receive/act?date=" + date))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("" +
                        "{  " +
                        "   \"receiveAct\":{  " +
                        "      \"number\":\"228\",  " +
                        "      \"date\":\"2020-07-31\",  " +
                        "      \"executor\":\"ASHOT DELIVERY SOLUTIONS UNLIMITED\",  " +
                        "      \"sender\":\"ASHOT DELIVERY SOLUTIONS UNLIMITED\",  " +
                        "      \"shipments\":[  " +
                        "         {  " +
                        "            \"id\":\"111\",  " +
                        "            \"items\":2,  " +
                        "            \"totalSum\":10  " +
                        "         },  " +
                        "         {  " +
                        "            \"id\":\"222\",  " +
                        "            \"items\":1,  " +
                        "            \"totalSum\":1  " +
                        "         }  " +
                        "      ],  " +
                        "      \"totalSum\":11,  " +
                        "      \"totalItems\":3  " +
                        "   }  " +
                        "}"
                ));
    }

    @WebLayerTest
    @RequiredArgsConstructor(onConstructor = @__(@Autowired))
    public static class ShipmentsControllerWithoutMocksTest extends BaseShallowTest {

        private static final String OPERATOR_SIGNATURE =
                "ff932f1d7e9b58247fac4f4e14ba14150d2b647997574cd1304205ff7174c287";
        private static final String OPERATOR_NAME =
                "ООО ВАСИЛИЙ ДЕЛИВЕРИ СОЛЮШЕНС АНЛИМИТЕД (ИНН %s), " +
                        "125040, Москва, Ленинградский проспект, дом 5, корпус 1, строение 7, офис 4, vasiliy_pupkin";

        private final TestPickupPointFactory pickupPointFactory;
        private final TestReturnRequestFactory returnRequestFactory;
        private final TestOrderFactory testOrderFactory;
        private final TestShipmentsFactory shipmentsFactory;
        private final TestOrderSenderFactory orderSenderFactory;
        private final ShipmentCommandService shipmentCommandService;
        private final OrderAdditionalInfoCommandService orderAdditionalInfoCommandService;
        private final OrderDeliveryResultCommandService orderDeliveryResultCommandService;
        private final TestableClock clock;
        private final TestOrderFactory orderFactory;
        private final ObjectMapper objectMapper;
        private final ConfigurationGlobalCommandService configurationGlobalCommandService;

        @MockBean
        private TransferApi transferApi;

        @BeforeEach
        void setup() {
            Tracer.putLoginToStatic(OPERATOR_LOGIN);
            Tracer.putUidToStatic(OPERATOR_ID);
        }

        @Test
        @Disabled
        @SneakyThrows
        public void createShipmentDispatchTest() {
            var pickupPoint = pickupPointFactory.createPickupPoint();
            var order = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                    .pickupPoint(pickupPoint)
                    .build());

            testOrderFactory.setStatusAndCheckpoint(order.getId(), READY_FOR_RETURN);

            String inputJson = String.format(
                    getFileContent("shipment/shipment_create_request.json"), order.getExternalId());

            var expectedShipment = String.format(
                    getFileContent("shipment/shipment_create_response.json"),
                    order.getId(), order.getExternalId(), order.getAssessedCost());

            mockMvc.perform(
                    post("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/shipments")
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                            .content(inputJson))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(content().json(expectedShipment, false));
        }

        @Test
        public void createReadyForReturnDispatch() {
            setClock();
            PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
            Order order = testOrderFactory.createReadyForReturnOrder(pickupPoint);

            String inputJson = String.format(
                    getFileContent("shipment/dispatch/dispatch_create_request.json"),
                    order.getExternalId(), "EXPIRED"
            );

            String expectedShipment = String.format(
                    getFileContent("shipment/dispatch/dispatch_create_response.json"),
                    order.getExternalId(), order.getExternalId(), "EXPIRED", 3, "Василий Пупкин", "89992281488",
                    "vasily@pupkin.com", 3, order.getAssessedCost(), LocalDate.now(clock)
            );

            createShipmentAndCheckResult(pickupPoint.getPvzMarketId(), inputJson, expectedShipment);
        }

        @Test
        public void createFashionDispatch() {
            setClock();
            PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
            var order = createFashionOrder(pickupPoint);

            String inputJson = String.format(
                    getFileContent("shipment/dispatch/dispatch_create_request.json"),
                    order.getExternalId(), "SAFE_PACKAGE"
            );

            String expectedShipment = String.format(
                    getFileContent("shipment/dispatch/dispatch_create_response.json"),
                    order.getExternalId(), "FSN_RET_000002734", "SAFE_PACKAGE", 1, "Василий Пупкин", "89992281488",
                    "vasily@pupkin.com", 1, BigDecimal.valueOf(1000), LocalDate.now(clock)
            );

            createShipmentAndCheckResult(pickupPoint.getPvzMarketId(), inputJson, expectedShipment);
        }

        @Test
        public void createReturnRequestDispatch() {
            setClock();
            PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
            ReturnRequestParams returnRequest = returnRequestFactory.createReceivedReturn(pickupPoint);

            String inputJson = String.format(
                    getFileContent("shipment/dispatch/dispatch_create_request.json"),
                    returnRequest.getReturnId(), "RETURN"
            );

            String expectedShipment = String.format(
                    getFileContent("shipment/dispatch/return_dispatch_create_response.json"),
                    returnRequest.getReturnId(), returnRequest.getBarcode(), "RETURN", 1,
                    "Райгородский Андрей Михайлович", 1, BigDecimal.valueOf(300), LocalDate.now(clock)
            );

            createShipmentAndCheckResult(pickupPoint.getPvzMarketId(), inputJson, expectedShipment);
        }

        @SneakyThrows
        private void createShipmentAndCheckResult(Long pvzMarketId, String inputJson, String expectedShipment) {
            mockMvc.perform(
                    post("/v1/pi/pickup-points/" + pvzMarketId + "/shipments-dispatch")
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                            .content(inputJson))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(content().json(expectedShipment, false));
        }

        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        @SneakyThrows
        public void createShipmentReceiveTest(boolean optimizeShipmentReceive) {
            configurationGlobalCommandService.setValue(OPTIMIZE_SHIPMENT_RECEIVE_ENABLED, optimizeShipmentReceive);
            setClock();

            var pickupPoint = pickupPointFactory.createPickupPoint();
            var order = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                    .pickupPoint(pickupPoint)
                    .build());
            var order2 = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                    .pickupPoint(pickupPoint)
                    .build());
            testOrderFactory.cancelOrder(order2.getId());

            String inputJson = String.format(getFileContent("shipment/receive/receive_create_request.json"),
                    order.getExternalId(), order2.getExternalId()
            );

            var totalPrice = order.getAssessedCost().add(order2.getAssessedCost());
            var expectedShipment = String.format(
                    getFileContent("shipment/receive/receive_create_response.json"), order.getId(),
                    order.getExternalId(), order2.getId(), order2.getExternalId(), totalPrice, LocalDate.now(clock)
            );

            mockMvc.perform(
                    post("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/shipments-receive")
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                            .content(inputJson))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(content().json(expectedShipment, false));
        }

        @Test
        @SneakyThrows
        public void createReceiveTransferTest() {
            String transferId = "some_id";
            configurationGlobalCommandService.setValue(OPTIMIZE_SHIPMENT_RECEIVE_ENABLED, true);
            when(transferApi.transferPut(any()))
                    .thenReturn(new TransferDto().id(transferId).status(TransferStatus.CREATED));
            setClock(Instant.parse("2007-12-03T10:15:30.00Z"));

            long lmsId = 123L;
            var pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                    TestPickupPointFactory.CreatePickupPointBuilder.builder()
                            .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                    .lmsId(lmsId).build())
                            .build());
            String taxpayerNumber = pickupPoint.getLegalPartner().getOrganization().getTaxpayerNumber();
            var order = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                    .pickupPoint(pickupPoint)
                    .build());
            String placeCodesJsonArray = getPlaceCodesInJsonArray(order.getOrderAdditionalInfo().getPlaceCodes());

            mockMvc.perform(post(sf("/v1/pi/pickup-points/{}/shipments/receive/transfer-act",
                            pickupPoint.getPvzMarketId()))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                            .content(sf(getFileContent("shipment/receive/receive_create_transfer_request.json"),
                                    order.getExternalId())))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(content().json(sf(
                                    getFileContent("shipment/receive/receive_transfer_response.json"), PENDING,
                                    order.getId(), order.getExternalId(), placeCodesJsonArray, order.getAssessedCost()),
                            false));

            ArgumentCaptor<TransferCreateRequestDto> captor = ArgumentCaptor.forClass(TransferCreateRequestDto.class);
            verify(transferApi).transferPut(captor.capture());
            assertThat(captor.getValue())
                    .usingRecursiveComparison()
                    .ignoringFields("idempotencyKey")
                    .isEqualTo(new TransferCreateRequestDto()
                            .transferQualifier(new TransferQualifierDto()
                                    .type(TransferQualifierTypeDto.TWO_ACTOR)
                                    .twoActorQualifier(new TwoActorQualifierDto()
                                            .actorFrom(new ActorDto()
                                                    .externalId("courier Vasya's ID")
                                                    .type(ActorTypeDto.MARKET_COURIER)
                                                    .name("")
                                                    .companyName(""))
                                            .actorTo(new ActorDto()
                                                    .externalId(String.valueOf(lmsId))
                                                    .name(DEFAULT_FULL_ORGANIZATION_NAME)
                                                    .companyName(YandexMigrationManager.YANDEX_MARKET_ORGANIZATION)
                                                    .type(ActorTypeDto.MARKET_PVZ))
                                            .localDate(LocalDate.parse("2007-12-03"))))
                            .registry(new RegistryDto()
                                    .direction(RegistryDirectionDto.RECEIVER)
                                    .items(mapOrder(order)))
                            .signature(new SignatureDto()
                                    .signerId(OPERATOR_LOGIN)
                                    .signerName(String.format(OPERATOR_NAME, taxpayerNumber))
                                    .signatureData(OPERATOR_SIGNATURE))
                            .autoSign(false));
        }

        @Test
        @SneakyThrows
        public void getReceiveTransferTest() {
            configurationGlobalCommandService.setValue(OPTIMIZE_SHIPMENT_RECEIVE_ENABLED, true);
            setClock(Instant.parse("2007-12-03T10:15:30.00Z"));

            long lmsId = 123L;
            var pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                    TestPickupPointFactory.CreatePickupPointBuilder.builder()
                            .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                    .lmsId(lmsId).build())
                            .build());
            var order1 = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                    .pickupPoint(pickupPoint)
                    .build());
            var order2 = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                    .pickupPoint(pickupPoint)
                    .build());
            Shipment shipment = shipmentsFactory.createPendingShipment(order1);
            Shipment cancelledShipment = shipmentsFactory.createPendingShipment(order2);

            // нет приёмки по id
            mockMvc.perform(get(sf("/v1/pi/pickup-points/{}/shipments/{}/receive",
                            pickupPoint.getPvzMarketId(), Long.MAX_VALUE)))
                    .andExpect(status().is4xxClientError());

            // согласование приёмки в процессе
            expectGetReceiveInStatus(order1, pickupPoint.getPvzMarketId(), shipment.getId(), PENDING);

            // приёмка отменена
            shipmentCommandService.closeReceive(cancelledShipment.getTransferId(), CANCELLED);
            expectGetReceiveInStatus(order2, pickupPoint.getPvzMarketId(), cancelledShipment.getId(), CANCELLED);

            // приёмка завершена
            shipmentCommandService.closeReceive(shipment.getTransferId(), FINISHED);
            expectGetReceiveInStatus(order1, pickupPoint.getPvzMarketId(), shipment.getId(), FINISHED);
        }

        @SneakyThrows
        private void expectGetReceiveInStatus(Order order, long pvzMarketId, long shipmentId, ShipmentStatus status) {
            String placeCodesJsonArray = getPlaceCodesInJsonArray(order.getOrderAdditionalInfo().getPlaceCodes());
            mockMvc.perform(get(sf("/v1/pi/pickup-points/{}/shipments/{}/receive",
                            pvzMarketId, shipmentId)))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(content().json(sf(
                                    getFileContent(status == CANCELLED ?
                                            "shipment/receive/receive_cancelled_transfer_response.json" :
                                            "shipment/receive/receive_transfer_response.json"),
                                    status, order.getId(), order.getExternalId(), placeCodesJsonArray,
                                    order.getAssessedCost()),
                            false));
        }

        private List<RegistryItemDto> mapOrder(Order order) {
            return order.getPlaces().stream()
                    .map(place -> new RegistryItemDto()
                            .itemQualifier(new ItemQualifierDto()
                                    .type(RegistryItemTypeDto.PLACE)
                                    .externalId(order.getExternalId())
                                    .placeId(place.getBarcode()))
                            .placeCount(1)
                            .declaredCost(StreamEx.of(place.getItems())
                                    .map(item -> item.getOrderItem().getSumPrice().setScale(2, RoundingMode.HALF_UP))
                                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                                    .toString()))
                    .collect(Collectors.toList());
        }

        @Test
        @SneakyThrows
        public void cancelReceiveTransferTest() {
            var pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                    TestPickupPointFactory.CreatePickupPointBuilder.builder()
                            .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                    .lmsId(123L).build())
                            .build());
            var order = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                    .pickupPoint(pickupPoint)
                    .build());
            Shipment shipment = shipmentsFactory.createPendingShipment(order);

            mockMvc.perform(delete(sf("/v1/pi/pickup-points/{}/shipments/{}/receive",
                            pickupPoint.getPvzMarketId(), shipment.getId())))
                    .andExpect(status().is2xxSuccessful());

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(transferApi).transferIdDelete(captor.capture());
            assertThat(captor.getValue()).isEqualTo(shipment.getTransferId());
        }

        @Test
        @SneakyThrows
        public void verifyCourierQrTestShouldReturnNoContentWhenCodeCorrect() {
            setClock();
            LocalDate now = LocalDate.now(clock);

            mockMvc.perform(get("/v1/pi/pickup-points/1/shipments/verify")
                            .param("code", encodeCourierQrWithDateTime(now)))
                    .andExpect(status().isNoContent());
        }

        @SneakyThrows
        @Test
        public void verifyCourierQrTestShouldReturnForbiddenWhenCodeIsNotTodays() {
            setClock();
            LocalDate tomorrow = LocalDate.now(clock).plusDays(1);
            LocalDate yesterday = LocalDate.now(clock).minusDays(1);

            mockMvc.perform(get("/v1/pi/pickup-points/1/shipments/verify")
                            .param("code", encodeCourierQrWithDateTime(tomorrow)))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/v1/pi/pickup-points/1/shipments/verify")
                            .param("code", encodeCourierQrWithDateTime(yesterday)))
                    .andExpect(status().isForbidden());
        }

        @SneakyThrows
        @ParameterizedTest
        @CsvSource({"password,bad_salt", "bad_password,salt", "bad_password,bad_salt"})
        public void verifyCourierQrTestShouldReturnForbiddenWhenCredentialsAreInvalid(String password, String salt) {
            setClock();
            LocalDate now = LocalDate.now(clock);
            String invalidQrCode = encodeCourierQrWithDateTimeAndPasswordSalt(now, password, salt);

            mockMvc.perform(get("/v1/pi/pickup-points/1/shipments/verify")
                            .param("code", invalidQrCode))
                    .andExpect(status().isForbidden());
        }

        @Test
        @SneakyThrows
        public void verifyCourierQrTestShouldReturnForbiddenWhenCodeIsInvalid() {
            mockMvc.perform(get("/v1/pi/pickup-points/1/shipments/verify")
                            .param("code", "invalidQrCode"))
                    .andExpect(status().isForbidden());
        }

        private String encodeCourierQrWithDateTime(LocalDate shiftTime) {
            return encodeCourierQrWithDateTimeAndPasswordSalt(shiftTime, "password", "salt");
        }

        private String encodeCourierQrWithDateTimeAndPasswordSalt(LocalDate shiftTime, String password, String salt) {
            return QRCodeUtil.encryptQrCode(new QrCode(123L, shiftTime.toString(), 42L), password, salt);
        }

        private void setClock() {
            setClock(Instant.now());
        }

        private void setClock(Instant instant) {
            clock.setFixed(instant.truncatedTo(ChronoUnit.SECONDS), ZoneId.of("UTC+3"));
        }

        @Test
        @SneakyThrows
        void tryCreateDispatchTransferReturnsPending() {
            PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                    TestPickupPointFactory.CreatePickupPointBuilder.builder()
                            .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                    .lmsId(LMS_ID).build())
                            .build());
            Order expiredOrder = orderFactory.createOrder(
                    TestOrderFactory.CreateOrderBuilder.builder().pickupPoint(pickupPoint).build());
            orderFactory.setStatusAndCheckpoint(expiredOrder.getId(), PvzOrderStatus.STORAGE_PERIOD_EXPIRED);
            ReturnRequestParams returnOrder = returnRequestFactory.createReceivedReturn(pickupPoint);
            Order fashionOrder = orderFactory.createFashionWithPartialReturn(pickupPoint);

            when(transferApi.pendingTransfersGet(any())).thenReturn(List.of());

            mockMvc.perform(post(sf("/v1/pi/pickup-points/{}/shipments/dispatch/transfer-act",
                            pickupPoint.getPvzMarketId()))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                            .content(sf(getFileContent("shipment/dispatch/dispatch_transfer_request.json"),
                                    expiredOrder.getExternalId(), returnOrder.getReturnId(),
                                    fashionOrder.getExternalId())))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(content().json(
                            getFileContent("shipment/dispatch/dispatch_pending_transfer_response.json"), true));
        }

        @Test
        @SneakyThrows
        void tryCreateDispatchTransferReturnsFinished() {
            setClock(Instant.parse("2007-09-03T12:00:00Z"));
            PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                    TestPickupPointFactory.CreatePickupPointBuilder.builder()
                            .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                    .lmsId(LMS_ID).build())
                            .build());
            Order expiredOrder = orderFactory.createOrder(
                    TestOrderFactory.CreateOrderBuilder.builder().pickupPoint(pickupPoint).build());
            orderFactory.setStatusAndCheckpoint(expiredOrder.getId(), PvzOrderStatus.STORAGE_PERIOD_EXPIRED);
            ReturnRequestParams returnOrder = returnRequestFactory.createReceivedReturn(pickupPoint);
            Order fashionOrder = orderFactory.createFashionWithPartialReturn(pickupPoint);
            Order fashionOrder2 = orderFactory.createFashionWithPartialReturn(pickupPoint,
                    List.of(BARCODE_3));

            when(transferApi.pendingTransfersGet(String.valueOf(LMS_ID)))
                    .thenReturn(List.of(
                            new PendingTransferDto().id(TRANSFER_ID_WITH_DISCREPANCIES),
                            new PendingTransferDto().id(TRANSFER_ID)));

            when(transferApi.transferIdGet(TRANSFER_ID_WITH_DISCREPANCIES))
                    .thenReturn(ofOrders(EntryStream.of(
                            returnOrder.getReturnId(), returnOrder.getBarcode(),
                            fashionOrder2.getExternalId(), BARCODE_3).toList())
                            .id(TRANSFER_ID_WITH_DISCREPANCIES));
            when(transferApi.transferIdGet(TRANSFER_ID))
                    .thenReturn(ofOrders(orderToEntries(expiredOrder)
                            .append(returnOrder.getReturnId(), returnOrder.getBarcode(),
                                    fashionOrder.getExternalId(), BARCODE_1,
                                    fashionOrder.getExternalId(), BARCODE_2).toList())
                            .id(TRANSFER_ID));

            mockMvc.perform(post(sf("/v1/pi/pickup-points/{}/shipments/dispatch/transfer-act",
                            pickupPoint.getPvzMarketId()))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                            .content(sf(getFileContent("shipment/dispatch/dispatch_transfer_request.json"),
                                    expiredOrder.getExternalId(), returnOrder.getReturnId(),
                                    fashionOrder.getExternalId())))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(content().json(sf(
                            getFileContent("shipment/dispatch/dispatch_finished_transfer_response.json"),
                            expiredOrder.getExternalId(), getBarcodes(expiredOrder), BARCODE_2 + "\\\n" + BARCODE_1,
                            getPlaceCodesInJsonArray(List.of(BARCODE_2, BARCODE_1)), returnOrder.getBarcode(),
                            getPlaceCodesInJsonArray(List.of(returnOrder.getBarcode()))), false));
        }

        @Test
        void tryCreateDispatchTransferReturnsDiscrepancies() {
            PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                    TestPickupPointFactory.CreatePickupPointBuilder.builder()
                            .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                    .lmsId(LMS_ID).build())
                            .build());
            Order expiredOrder = orderFactory.createOrder(
                    TestOrderFactory.CreateOrderBuilder.builder().pickupPoint(pickupPoint).build());
            orderFactory.setStatusAndCheckpoint(expiredOrder.getId(), PvzOrderStatus.STORAGE_PERIOD_EXPIRED);
            ReturnRequestParams returnOrder = returnRequestFactory.createReceivedReturn(pickupPoint);
            ReturnRequestParams returnOrder2 = returnRequestFactory.createReceivedReturn(pickupPoint);
            Order fashionOrder = orderFactory.createFashionWithPartialReturn(pickupPoint);

            when(transferApi.pendingTransfersGet(String.valueOf(LMS_ID)))
                    .thenReturn(List.of(new PendingTransferDto().id(TRANSFER_ID)));

            List<Entry<String, String>> deficiencyOrders = EntryStream.of(
                    returnOrder.getReturnId(), returnOrder.getBarcode(),
                    fashionOrder.getExternalId(), BARCODE_1,
                    fashionOrder.getExternalId(), BARCODE_2).toList();
            List<Entry<String, String>> excessOrders = orderToEntries(expiredOrder)
                    .append(returnOrder.getReturnId(), returnOrder.getBarcode(),
                            returnOrder2.getReturnId(), returnOrder2.getBarcode())
                    .append(fashionOrder.getExternalId(), BARCODE_1,
                            fashionOrder.getExternalId(), BARCODE_2)
                    .toList();
            List<Entry<String, String>> placesWithBoth = StreamEx.of(deficiencyOrders)
                    .append(Map.entry(returnOrder2.getReturnId(), returnOrder2.getBarcode()))
                    .toList();

            testDiscrepancy(pickupPoint.getPvzMarketId(), expiredOrder, returnOrder, fashionOrder,
                    deficiencyOrders, List.of(), List.of(expiredOrder.getExternalId()));
            testDiscrepancy(pickupPoint.getPvzMarketId(), expiredOrder, returnOrder, fashionOrder,
                    excessOrders, List.of(returnOrder2.getReturnId()), List.of());
            testDiscrepancy(pickupPoint.getPvzMarketId(), expiredOrder, returnOrder, fashionOrder,
                    placesWithBoth, List.of(returnOrder2.getReturnId()), List.of(expiredOrder.getExternalId()));
        }

        private EntryStream<String, String> orderToEntries(Order order) {
            return StreamEx.of(order.getPlaces())
                    .mapToEntry(place -> place.getOrder().getExternalId(), OrderPlace::getBarcode);
        }

        @SneakyThrows
        private void testDiscrepancy(
                long pvzMarketId, Order expiredOrder, ReturnRequestParams returnOrder, Order fashionOrder,
                List<Entry<String, String>> ordersScannedByCourier, List<String> deficiencies, List<String> excesses) {
            when(transferApi.transferIdGet(TRANSFER_ID))
                    .thenReturn(ofOrders(ordersScannedByCourier).id(TRANSFER_ID));
            mockMvc.perform(post(sf("/v1/pi/pickup-points/{}/shipments/dispatch/transfer-act", pvzMarketId))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                            .content(sf(getFileContent("shipment/dispatch/dispatch_transfer_request.json"),
                                    expiredOrder.getExternalId(), returnOrder.getReturnId(),
                                    fashionOrder.getExternalId())))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(content().json(sf(
                            getFileContent("shipment/dispatch/dispatch_discrepancy_transfer_response.json"),
                            getPlaceCodesInJsonArray(deficiencies),
                            getPlaceCodesInJsonArray(excesses)), true));
        }

        @Deprecated(forRemoval = true, since = "https://st.yandex-team.ru/MARKETTPLPVZ-2447")
        private TransferDto ofOrders(List<Entry<String, String>> orderPlaces) {
            List<ItemQualifierDto> items = StreamEx.of(orderPlaces)
                    .map(orderPlace -> new ItemQualifierDto()
                            .type(RegistryItemTypeDto.PLACE)
                            .externalId(orderPlace.getKey())
                            .placeId(orderPlace.getValue()))
                    .toList();
            return new TransferDto().receivedItems(items);
        }

        @Test
        @SneakyThrows
        public void getReceivesOnDate() {
            var pickupPoint = pickupPointFactory.createPickupPoint();
            var order = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                    .pickupPoint(pickupPoint)
                    .build());
            var pickupPointAuthInfo = new PickupPointRequestData(order.getPickupPoint().getId(),
                    order.getPickupPoint().getPvzMarketId(), order.getPickupPoint().getName(), 1L,
                    order.getPickupPoint().getTimeOffset(), order.getPickupPoint().getStoragePeriod()
            );
            var shipment = shipmentCommandService.createShipment(pickupPointAuthInfo, new ShipmentCreateParams(
                    ShipmentType.RECEIVE, FINISHED,
                    List.of(new ShipmentCreateItemParams(order.getExternalId()))));

            var expectedReceives = String.format(
                    getFileContent("shipment/receive/receive_on_date.json"),
                    shipment.getId(), shipment.getCreatedAt(), order.getId(), order.getExternalId(), getBarcodes(order));

            mockMvc.perform(
                    get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/shipments-receive?date=" +
                            shipment.getShipmentDate()))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(content().json(expectedReceives, true));
        }

        @Test
        @SneakyThrows
        public void getReceivesOnDateWithCourier() {
            var pickupPoint = pickupPointFactory.createPickupPoint();
            var order = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                    .pickupPoint(pickupPoint)
                    .build());
            String courierId = "The best courier";
            orderAdditionalInfoCommandService.updateCourierIdByExternalIds(List.of(order.getExternalId()), courierId);

            var pickupPointAuthInfo = new PickupPointRequestData(order.getPickupPoint().getId(),
                    order.getPickupPoint().getPvzMarketId(), order.getPickupPoint().getName(), 1L,
                    order.getPickupPoint().getTimeOffset(), order.getPickupPoint().getStoragePeriod()
            );
            var shipment = shipmentCommandService.createShipment(pickupPointAuthInfo, new ShipmentCreateParams(
                    ShipmentType.RECEIVE, FINISHED,
                    List.of(new ShipmentCreateItemParams(order.getExternalId()))));

            var expectedReceives = String.format(
                    getFileContent("shipment/receive/receive_on_date.json"),
                    shipment.getId(), shipment.getCreatedAt(), order.getId(), order.getExternalId(), getBarcodes(order));

            mockMvc.perform(
                    get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/shipments-receive?date=" +
                            shipment.getShipmentDate()))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(content().json(expectedReceives, true));
        }

        @SneakyThrows
        private String getBarcodes(Order order) {
            List<String> barcodes = StreamEx.of(order.getPlaces()).map(OrderPlace::getBarcode).toList();
            return getPlaceCodesInJsonArray(barcodes);
        }

        private String getPlaceCodesInJsonArray(List<String> order) throws JsonProcessingException {
            return objectMapper.writeValueAsString(order);
        }

        @Test
        @SneakyThrows
        public void getReceivesOnDateWithPendingReceive() {
            var pickupPoint = pickupPointFactory.createPickupPoint();
            var order = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                    .pickupPoint(pickupPoint)
                    .build());
            var order2 = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                    .pickupPoint(pickupPoint)
                    .build());
            var pickupPointAuthInfo = new PickupPointRequestData(order.getPickupPoint().getId(),
                    order.getPickupPoint().getPvzMarketId(), order.getPickupPoint().getName(), 1L,
                    order.getPickupPoint().getTimeOffset(), order.getPickupPoint().getStoragePeriod()
            );
            var finishedShipment = shipmentCommandService.createShipment(pickupPointAuthInfo, new ShipmentCreateParams(
                    ShipmentType.RECEIVE, FINISHED,
                    List.of(new ShipmentCreateItemParams(order.getExternalId()))));
            var pendingShipment = shipmentCommandService.createShipment(pickupPointAuthInfo, new ShipmentCreateParams(
                    ShipmentType.RECEIVE, PENDING,
                    List.of(new ShipmentCreateItemParams(order2.getExternalId()))));

            var expectedReceives = String.format(
                    getFileContent("shipment/receive/receive_on_date_with_pending.json"),
                    finishedShipment.getId(), finishedShipment.getCreatedAt(), order.getId(), order.getExternalId(),
                    getBarcodes(order), pendingShipment.getId(), pendingShipment.getCreatedAt(), order2.getId(),
                    order2.getExternalId(), getBarcodes(order2));

            mockMvc.perform(
                    get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/shipments-receive?date=" +
                            finishedShipment.getShipmentDate()))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(content().json(expectedReceives, true));
        }

        @Test
        @SneakyThrows
        public void wrongDateToGetReceivesOnDate() {
            var pickupPoint = pickupPointFactory.createPickupPoint();
            var order = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                    .pickupPoint(pickupPoint)
                    .build());
            var pickupPointAuthInfo = new PickupPointRequestData(order.getPickupPoint().getId(),
                    order.getPickupPoint().getPvzMarketId(), order.getPickupPoint().getName(), 1L,
                    order.getPickupPoint().getTimeOffset(), order.getPickupPoint().getStoragePeriod()
            );
            Shipment shipment = shipmentCommandService.createShipment(pickupPointAuthInfo, new ShipmentCreateParams(
                    ShipmentType.RECEIVE, FINISHED,
                    List.of(new ShipmentCreateItemParams(order.getExternalId()))));

            var expectedReceives = getFileContent("shipment/receive/empty_receive_on_date.json");

            mockMvc.perform(
                    get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() +
                            "/shipments-receive?date=" + shipment.getShipmentDate().plusDays(10)))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(content().json(expectedReceives, false));
        }

        @Test
        @SneakyThrows
        public void wrongDateToGetDispatchesOnDate() {
            var pickupPoint = pickupPointFactory.createPickupPoint();
            var order = testOrderFactory.createReadyForReturnOrder(pickupPoint);
            var pickupPointAuthInfo = new PickupPointRequestData(
                    pickupPoint.getId(), pickupPoint.getPvzMarketId(), pickupPoint.getName(), 1L,
                    pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod()
            );
            shipmentCommandService.createShipment(
                    pickupPointAuthInfo,
                    new ShipmentCreateParams(
                            ShipmentType.DISPATCH, FINISHED,
                            List.of(new ShipmentCreateItemParams(order.getExternalId(), DispatchType.RETURN))
                    )
            );

            var expectedReceives = getFileContent("shipment/dispatch/empty_dispatch_on_date.json");

            mockMvc.perform(
                    get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() +
                            "/shipments-dispatch?date=" + LocalDate.EPOCH))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(content().json(expectedReceives, false));
        }

        @Test
        @SneakyThrows
        public void getDispatchesOnDate() {
            clock.setFixed(Instant.now(), clock.getZone());

            var pickupPoint = pickupPointFactory.createPickupPoint();
            var pickupPointAuthInfo = new PickupPointRequestData(
                    pickupPoint.getId(), pickupPoint.getPvzMarketId(), pickupPoint.getName(), 1L,
                    pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod()
            );

            var order = testOrderFactory.createReadyForReturnOrder(pickupPoint);
            var shipment = orderFactory.createShipmentDispatch(
                    pickupPointAuthInfo, order.getExternalId(), DispatchType.EXPIRED
            );

            var order2 = createFashionOrder(pickupPoint);
            var shipment2 = orderFactory.createShipmentDispatch(
                    pickupPointAuthInfo, order2.getExternalId(), DispatchType.SAFE_PACKAGE
            );

            var returnRequest = returnRequestFactory.createReceivedReturn(pickupPoint);
            var shipment3 = orderFactory.createShipmentDispatch(
                    pickupPointAuthInfo, returnRequest.getReturnId(), DispatchType.RETURN
            );

            var order4 = createDbsOrder(pickupPoint);
            var shipment4 = orderFactory.createShipmentDispatch(
                    pickupPointAuthInfo, order4.getExternalId(), DispatchType.EXPIRED
            );

            var expectedDispatches = String.format(
                    getFileContent("shipment/dispatch/dispatch_on_date.json"), shipment.getId(),
                    shipment.getItems().get(0).getCreatedAt(), order.getExternalId(), order.getExternalId(),
                    shipment2.getId(), shipment2.getItems().get(0).getCreatedAt(), order2.getExternalId(),
                    "FSN_RET_000002734", shipment3.getId(), shipment3.getItems().get(0).getCreatedAt(),
                    returnRequest.getReturnId(), returnRequest.getBarcode(), shipment4.getId(),
                    shipment4.getItems().get(0).getCreatedAt(), order4.getExternalId(), order4.getExternalId()
            );

            mockMvc.perform(
                            get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/shipments-dispatch?date=" +
                                    LocalDate.now(clock)))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(content().json(expectedDispatches, false));
        }

        private Order createFashionOrder(PickupPoint pickupPoint) {
            var fashionOrder = orderFactory.createSimpleFashionOrder(false, pickupPoint);
            fashionOrder = orderFactory.receiveOrder(fashionOrder.getId());

            orderDeliveryResultCommandService.startFitting(fashionOrder.getId());
            orderDeliveryResultCommandService.updateItemFlow(fashionOrder.getId(), UIT_1_1, ItemDeliveryFlow.RETURN);
            orderDeliveryResultCommandService.finishFitting(fashionOrder.getId());
            orderDeliveryResultCommandService.pay(fashionOrder.getId());
            orderDeliveryResultCommandService.packageReturn(
                    fashionOrder.getId(), List.of("FSN_RET_000002734")
            );

            return fashionOrder;
        }

        private Order createDbsOrder(PickupPoint pickupPoint) {
            OrderSender orderSender = orderSenderFactory.createOrderSender(
                    TestOrderSenderFactory.OrderSenderParams.builder()
                            .incorporation("KFC")
                            .build()
            );
            var order = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                    .pickupPoint(pickupPoint)
                    .orderSender(orderSender)
                    .params(TestOrderFactory.OrderParams.builder()
                            .deliveryServiceType(DeliveryServiceType.DBS)
                            .build())
                    .build());
            return testOrderFactory.readyForReturn(order.getId());
        }

        @Test
        @SneakyThrows
        public void getAllTypesOfDispatch() {
            var pickupPoint = pickupPointFactory.createPickupPoint();
            var returnRequest = returnRequestFactory.createReturnRequest(
                    TestReturnRequestFactory.CreateReturnRequestBuilder.builder()
                            .pickupPoint(pickupPoint)
                            .build()
            );
            returnRequest = returnRequestFactory.receiveReturnRequest(returnRequest.getReturnId());

            var order = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                    .pickupPoint(pickupPoint).build());
            var orderReadyToReturn = testOrderFactory.readyForReturn(order.getId());

            Order fashionOrder = testOrderFactory.createSimpleFashionOrder(false, pickupPoint);
            orderFactory.receiveOrder(fashionOrder.getId());
            orderDeliveryResultCommandService.startFitting(fashionOrder.getId());
            orderDeliveryResultCommandService.updateItemFlow(fashionOrder.getId(), UIT_1_1, ItemDeliveryFlow.RETURN);
            orderDeliveryResultCommandService.finishFitting(fashionOrder.getId());
            orderDeliveryResultCommandService.pay(fashionOrder.getId());
            orderDeliveryResultCommandService.packageReturn(fashionOrder.getId(), List.of("Package1", "Package2"));

            var expectedDispatches = String.format(
                    getFileContent("shipment/dispatch/dispatch_with_return_and_expired.json"),
                    returnRequest.getReturnId(), orderReadyToReturn.getExternalId());

            mockMvc.perform(
                    get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/returns-and-expired"))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(content().json(expectedDispatches, false));
        }

        @Test
        @Disabled
        @SneakyThrows
        void futureDateToLoadDispatchAct() {
            var order = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());

            mockMvc.perform(
                    get("/v1/pi/pickup-points/" + order.getPickupPoint().getPvzMarketId() +
                            "/shipments/dispatch/act-binary?date=" + LocalDate.MAX))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @SneakyThrows
        void wrongShipmentIdToLoadReceiveAct() {
            var order = testOrderFactory.createOrder(
                    TestOrderFactory.CreateOrderBuilder.builder()
                            .params(TestOrderFactory.OrderParams.builder()
                                    .shipmentDate(LocalDate.of(2021, 7, 21))
                                    .build())
                            .build());

            mockMvc.perform(
                    get("/v1/pi/pickup-points/" +
                            order.getPickupPoint().getPvzMarketId() +
                            "/shipments/1/receive/act?date=" +
                            order.getShipmentDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
                    .andExpect(status().is4xxClientError());
        }
    }
}
