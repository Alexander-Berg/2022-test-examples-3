package ru.yandex.market.pvz.internal.controller.pi.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.logistic.api.model.delivery.CargoType;
import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.order.OrderQueryService;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.OrderDeliveryType;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentType;
import ru.yandex.market.pvz.core.domain.order.model.params.OrderSimpleParams;
import ru.yandex.market.pvz.core.domain.order_delivery_result.ItemDeliveryFlow;
import ru.yandex.market.pvz.core.domain.order_delivery_result.ItemDeliveryScanType;
import ru.yandex.market.pvz.core.domain.order_delivery_result.PartialDeliveryStatus;
import ru.yandex.market.pvz.core.domain.order_delivery_result.params.CodeType;
import ru.yandex.market.pvz.core.domain.order_delivery_result.params.OrderDeliveryResultItemParams;
import ru.yandex.market.pvz.core.domain.order_delivery_result.params.OrderDeliveryResultParams;
import ru.yandex.market.pvz.core.domain.order_delivery_result.service.OrderDeliveryResultBarcodeQueryService;
import ru.yandex.market.pvz.core.domain.order_delivery_result.service.OrderDeliveryResultCommandService;
import ru.yandex.market.pvz.core.domain.order_delivery_result.service.OrderDeliveryResultQueryService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.returns.model.ReturnType;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.internal.BaseShallowTest;
import ru.yandex.market.pvz.internal.WebLayerTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pvz.core.TestUtils.getFileContent;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.GENERATE_SAFE_PACKAGE_BARCODE_FOR_FBY_ENABLED;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.SCAN_CIS_FOR_DELIVERY_ENABLED;
import static ru.yandex.market.pvz.core.domain.order.model.OrderDeliveryType.VERIFICATION_CODE;
import static ru.yandex.market.pvz.core.domain.order.model.OrderPaymentType.PREPAID;
import static ru.yandex.market.pvz.core.domain.order_delivery_result.ItemDeliveryFlow.RETURN;
import static ru.yandex.market.pvz.core.domain.order_delivery_result.ItemDeliveryScanType.SCAN;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.BARCODE_1;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.BARCODE_2;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.BARCODE_3;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.BARCODE_4_1;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.CIS_1_1;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.CIS_2_1;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.CIS_2_2;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.UIT_1_1;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.UIT_2_1;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.UIT_2_2;
import static ru.yandex.market.tpl.common.util.StringFormatter.formatVars;
import static ru.yandex.market.tpl.common.util.StringFormatter.sf;

@WebLayerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OrdersControllerPartialDeliveryTest extends BaseShallowTest {

    private final TestOrderFactory orderFactory;
    private final TestPickupPointFactory pickupPointFactory;
    private final OrderQueryService orderQueryService;
    private final OrderDeliveryResultQueryService orderDeliveryResultQueryService;
    private final OrderDeliveryResultBarcodeQueryService orderDeliveryResultBarcodeQueryService;
    private final OrderDeliveryResultCommandService orderDeliveryResultCommandService;
    private final TestableClock clock;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;

    @Test
    @SneakyThrows
    void testGetPartialDeliveryPage() {
        OrderSimpleParams order = createAndReceiveFashionOrder();
        OrderDeliveryResultParams deliveryResult = orderDeliveryResultCommandService.startFitting(order.getId());
        long iii = deliveryResult.getItems().get(0).getItemInstanceId();
        mockMvc.perform(
                get("/v1/pi/pickup-points/" + order.getPvzMarketId() + "/orders/" + order.getId() +
                        "/deliver/partial"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(
                        formatVars(
                                getFileContent("order/partial_delivery/response_get_page.json"),
                                Map.of(
                                        "orderId", order.getId(),
                                        "externalId", order.getExternalId(),
                                        "iii1", iii,
                                        "iii2", iii + 1,
                                        "iii3", iii + 2
                                )
                        ), false
                ));
    }

    @Test
    @SneakyThrows
    void testGetPartialDeliveryPageForFbsOrder() {
        OrderSimpleParams order = createAndReceiveFbsOrder(true);
        OrderDeliveryResultParams deliveryResult = orderDeliveryResultCommandService.startFitting(order.getId());
        long iii = deliveryResult.getItems().get(0).getItemInstanceId();
        mockMvc.perform(
                get("/v1/pi/pickup-points/" + order.getPvzMarketId() + "/orders/" + order.getId() +
                        "/deliver/partial"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(
                        formatVars(
                                getFileContent("order/partial_delivery/response_get_fbs_page.json"),
                                Map.of(
                                        "orderId", order.getId(),
                                        "externalId", order.getExternalId(),
                                        "iii1", iii,
                                        "iii2", iii + 1,
                                        "iii3", iii + 2
                                )
                        ), false
                ));
    }

    @Test
    @SneakyThrows
    void testGetPartialDeliveryPageForOrderWithNoFittingItems() {
        Order createdOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .paymentType(OrderPaymentType.PREPAID)
                        .partialDeliveryAllowed(true)
                        .partialDeliveryAvailable(true)
                        .fbs(false)
                        .items(List.of(
                                TestOrderFactory.OrderItemParams.builder()
                                        .count(1)
                                        .name("Футболка")
                                        .photoUrl("//futbolka.ru/photo.jpg")
                                        .cargoTypeCodes(
                                                List.of(CargoType.FASHION.getCode(), CargoType.NO_TRYING.getCode()))
                                        .isService(false)
                                        .uitValues(List.of(UIT_1_1))
                                        .cisValues(List.of(CIS_1_1))
                                        .price(BigDecimal.valueOf(1000))
                                        .supplierTaxpayerNumber("item_1_sup")
                                        .size("xl")
                                        .color("black")
                                        .build()
                        ))
                        .build())
                .build());
        orderFactory.receiveOrder(createdOrder.getId());
        var order = orderQueryService.getSimple(createdOrder.getId());
        orderDeliveryResultCommandService.startFitting(order.getId());
        mockMvc.perform(
                get("/v1/pi/pickup-points/" + order.getPvzMarketId() + "/orders/" + order.getId() +
                        "/deliver/partial"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(
                        formatVars(
                                getFileContent("order/partial_delivery/response_get_page_with_no_fitting.json"),
                                Map.of(
                                        "orderId", order.getId(),
                                        "externalId", order.getExternalId()
                                )
                        )
                ));
    }

    @Test
    @SneakyThrows
    void testGetPartialDeliveryPageWithSiblings() {
        List<OrderSimpleParams> orderAndSibling = createAndReceiveFashionOrderWithSiblings();
        OrderSimpleParams order = orderAndSibling.get(0);
        OrderSimpleParams sibling = orderAndSibling.get(2);
        OrderDeliveryResultParams deliveryResult = orderDeliveryResultCommandService.startFitting(order.getId());
        long iii = deliveryResult.getItems().get(0).getItemInstanceId();
        mockMvc.perform(
                        get("/v1/pi/pickup-points/" + order.getPvzMarketId() + "/orders/" + order.getId() +
                                "/deliver/partial"))
                .andExpect(status().is2xxSuccessful())
                .andDo(print())
                .andExpect(content().json(
                        formatVars(getFileContent("order/partial_delivery/" +
                                        "response_get_page_with_simplified_delivery.json"),
                                Map.of(
                                        "orderId", order.getId(),
                                        "externalId", order.getExternalId(),
                                        "siblingExternalId", sibling.getExternalId(),
                                        "iii1", iii,
                                        "iii2", iii + 1,
                                        "iii3", iii + 2
                                )
                        ), false
                ));
    }

    @Test
    @SneakyThrows
    void testScanSecondUit() {
        OrderSimpleParams order = createAndReceiveFashionOrder();
        OrderDeliveryResultParams deliveryResult = orderDeliveryResultCommandService.startFitting(order.getId());
        long iii = deliveryResult.getItems().get(0).getItemInstanceId();
        mockMvc.perform(
                patch("/v1/pi/pickup-points/" + order.getPvzMarketId() + "/orders/" + order.getId() +
                        "/deliver/partial")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("order/partial_delivery/request_scan_second_uit.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(
                        formatVars(
                                getFileContent("order/partial_delivery/response_scan_second_uit.json"),
                                Map.of(
                                        "orderId", order.getId(),
                                        "externalId", order.getExternalId(),
                                        "iii1", iii,
                                        "iii2", iii + 1,
                                        "iii3", iii + 2
                                )
                        ), false
                ));
    }

    @Test
    @SneakyThrows
    void testScanSecondItemInstanceId() {
        OrderSimpleParams order = createAndReceiveFashionOrder();
        OrderDeliveryResultParams deliveryResult = orderDeliveryResultCommandService.startFitting(order.getId());
        long iii = deliveryResult.getItems().get(0).getItemInstanceId();
        OrderDeliveryResultItemParams secondItem = deliveryResult.getItems().get(1);

        mockMvc.perform(
                patch("/v1/pi/pickup-points/" + order.getPvzMarketId() + "/orders/" + order.getId() +
                        "/deliver/partial/" + secondItem.getItemInstanceId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("order/partial_delivery/" +
                                "request_scan_by_id.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(
                        formatVars(
                                getFileContent("order/partial_delivery/response_scan_second_item_by_id.json"),
                                Map.of(
                                        "orderId", order.getId(),
                                        "externalId", order.getExternalId(),
                                        "iii1", iii,
                                        "iii2", iii + 1,
                                        "iii3", iii + 2
                                )
                        ), false
                ));
    }

    @Test
    @SneakyThrows
    void testScanSecondThenFirstUit() {
        OrderSimpleParams order = createAndReceiveFashionOrder();
        OrderDeliveryResultParams deliveryResult = orderDeliveryResultCommandService.startFitting(order.getId());
        long iii = deliveryResult.getItems().get(0).getItemInstanceId();
        mockMvc.perform(
                patch("/v1/pi/pickup-points/" + order.getPvzMarketId() + "/orders/" + order.getId() +
                        "/deliver/partial")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("order/partial_delivery/request_scan_second_uit.json")));

        mockMvc.perform(
                patch("/v1/pi/pickup-points/" + order.getPvzMarketId() + "/orders/" + order.getId() +
                        "/deliver/partial")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("order/partial_delivery/request_scan_first_uit.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(
                        formatVars(
                                getFileContent("order/partial_delivery/response_scan_first_and_second_uit.json"),
                                Map.of(
                                        "orderId", order.getId(),
                                        "externalId", order.getExternalId(),
                                        "iii1", iii,
                                        "iii2", iii + 1,
                                        "iii3", iii + 2
                                )
                        ), false
                ));
    }

    @Test
    @SneakyThrows
    void testSetCommentForSecondUit() {
        OrderSimpleParams order = createAndReceiveFashionOrder();
        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_1, RETURN);

        mockMvc.perform(
                post("/v1/pi/pickup-points/" + order.getPvzMarketId() + "/orders/" + order.getId() +
                        "/deliver/partial/comment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("order/partial_delivery/request_comment_second_uit.json")))
                .andExpect(status().is2xxSuccessful());

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(order.getId());
        List<OrderDeliveryResultItemParams> items = orderDeliveryResult.getItems();
        items.forEach(item -> {
            item.setOrderItemId(null);
            item.setPriorityNumber(0);
            item.setItemInstanceId(0);
        });

        assertThat(List.of(
                OrderDeliveryResultItemParams.builder()
                        .uit(UIT_1_1)
                        .cis(CIS_1_1)
                        .codeType(CodeType.UIT)
                        .flow(ItemDeliveryFlow.DELIVERY)
                        .build(),
                OrderDeliveryResultItemParams.builder()
                        .uit(UIT_2_1)
                        .cis(CIS_2_1)
                        .codeType(CodeType.UIT)
                        .flow(RETURN)
                        .scanType(SCAN)
                        .returnReason(ReturnType.UNSUITABLE)
                        .comment("клиенту не подошёл размер")
                        .build(),
                OrderDeliveryResultItemParams.builder()
                        .uit(UIT_2_2)
                        .cis(CIS_2_2)
                        .codeType(CodeType.UIT)
                        .flow(ItemDeliveryFlow.DELIVERY)
                        .build()
        )).containsExactlyInAnyOrderElementsOf(items);
    }

    @Test
    @SneakyThrows
    void testSetCommentViaItemInstanceId() {
        OrderSimpleParams order = createAndReceiveFashionOrder();
        orderDeliveryResultCommandService.startFitting(order.getId());

        List<OrderDeliveryResultItemParams> items = orderDeliveryResultQueryService.get(order.getId()).getItems();
        OrderDeliveryResultItemParams orderItem = items.get(2);
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_1, RETURN);

        mockMvc.perform(
                post("/v1/pi/pickup-points/" + order.getPvzMarketId() + "/orders/" + order.getId() +
                        "/deliver/partial/comment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(formatVars(getFileContent("order/partial_delivery" +
                                        "/request_comment_item_instance_id.json"),
                                Map.of("itemInstanceId", orderItem.getItemInstanceId()))))
                .andExpect(status().is2xxSuccessful());

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(order.getId());
        List<OrderDeliveryResultItemParams> actualItems = orderDeliveryResult.getItems();
        actualItems.forEach(item -> {
            item.setOrderItemId(null);
            item.setPriorityNumber(0);
        });

        assertThat(actualItems).containsExactlyInAnyOrderElementsOf(List.of(
                OrderDeliveryResultItemParams.builder()
                        .itemInstanceId(items.get(0).getItemInstanceId())
                        .uit(UIT_1_1)
                        .cis(CIS_1_1)
                        .codeType(CodeType.UIT)
                        .flow(ItemDeliveryFlow.DELIVERY)
                        .build(),
                OrderDeliveryResultItemParams.builder()
                        .itemInstanceId(items.get(1).getItemInstanceId())
                        .uit(UIT_2_2)
                        .cis(CIS_2_2)
                        .codeType(CodeType.UIT)
                        .flow(ItemDeliveryFlow.DELIVERY)
                        .build(),
                OrderDeliveryResultItemParams.builder()
                        .itemInstanceId(orderItem.getItemInstanceId())
                        .uit(UIT_2_1)
                        .cis(CIS_2_1)
                        .codeType(CodeType.UIT)
                        .flow(RETURN)
                        .scanType(SCAN)
                        .returnReason(ReturnType.UNSUITABLE)
                        .comment("клиенту не подошёл размер")
                        .build()
        ));
        assertThat(orderItem.getUit()).isEqualTo(UIT_2_1);
    }

    @Test
    @SneakyThrows
    void testDeliverFullReturn() {
        OrderSimpleParams order = createAndReceiveFashionOrder();
        OrderDeliveryResultParams deliveryResult = orderDeliveryResultCommandService.startFitting(order.getId());
        long iii = deliveryResult.getItems().get(0).getItemInstanceId();
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_1_1, RETURN);
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_1, RETURN);
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_2, RETURN);
        orderDeliveryResultCommandService.finishFitting(order.getId());

        mockMvc.perform(
                post("/v1/pi/pickup-points/" + order.getPvzMarketId() + "/orders/" + order.getId() +
                        "/deliver/partial/full-return")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(
                        formatVars(
                                getFileContent("order/partial_delivery/response_deliver_full_return.json"),
                                Map.of(
                                        "orderId", order.getId(),
                                        "externalId", order.getExternalId(),
                                        "iii1", iii,
                                        "iii2", iii + 1,
                                        "iii3", iii + 2
                                )
                        ))
                );
    }

    @Test
    @SneakyThrows
    void testDeliverFullReturnWithSibling() {
        clock.setFixed(Instant.parse("2021-01-10T11:40:00.00Z"), clock.getZone());
        List<OrderSimpleParams> orders = createAndReceiveFashionOrderWithSiblings();
        OrderSimpleParams orderWithSiblings = orders.get(0);
        OrderSimpleParams sibling = orders.get(2);

        mockMvc.perform(get("/v1/pi/pickup-points/{id}/orders/{id}" , orderWithSiblings.getPvzMarketId(), orderWithSiblings.getId()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sf(
                        getFileContent("order/partial_delivery/response_get_fashion_order_page_with_sibling.json"),
                        orderWithSiblings.getExternalId(), sibling.getExternalId())));

        OrderDeliveryResultParams deliveryResult = orderDeliveryResultCommandService.startFitting(sibling.getId());
        StreamEx.of(deliveryResult.getItems())
                .map(OrderDeliveryResultItemParams::getItemInstanceId)
                .forEach(iii -> orderDeliveryResultCommandService.updateItemFlow(sibling.getId(), null, iii,
                        ItemDeliveryFlow.RETURN, ItemDeliveryScanType.SCAN, null));
        orderDeliveryResultCommandService.finishFitting(sibling.getId());

        mockMvc.perform(post("/v1/pi/pickup-points/{id}/orders/{id}/deliver/partial/full-return",
                        sibling.getPvzMarketId(), sibling.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(get("/v1/pi/pickup-points/{id}/orders/{id}" , orderWithSiblings.getPvzMarketId(), orderWithSiblings.getId()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sf(
                        getFileContent("order/partial_delivery/response_get_fashion_order_page_without_sibling.json"),
                        orderWithSiblings.getExternalId())));
    }

    @Test
    @SneakyThrows
    void testPackage() {
        OrderSimpleParams order = createAndReceiveFashionOrder();
        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_1, RETURN);
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_2, RETURN);
        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderDeliveryResultCommandService.pay(order.getId());

        mockMvc.perform(
                post("/v1/pi/pickup-points/" + order.getPvzMarketId() + "/orders/" + order.getId() +
                        "/deliver/partial/package")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("order/partial_delivery/request_package.json")))
                .andExpect(status().is2xxSuccessful());

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(order.getId());
        assertThat(OrderDeliveryResultParams.builder()
                .orderId(order.getId())
                .status(PartialDeliveryStatus.PACKAGED)
                .barcodes(List.of("1234567890", "5471236123"))
                .build())
                .usingRecursiveComparison()
                .ignoringFields("items", "id", "deliveryItemsToCheckCodeIds")
                .isEqualTo(orderDeliveryResult);
    }

    @Test
    @SneakyThrows
    void testPackageSafePackage() {
        OrderSimpleParams order = createAndReceiveFashionOrder();
        OrderDeliveryResultParams deliveryResult = orderDeliveryResultCommandService.startFitting(order.getId());
        long iii = deliveryResult.getItems().get(0).getItemInstanceId();
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_1, RETURN);
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_2, RETURN);
        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderDeliveryResultCommandService.pay(order.getId());

        mockMvc.perform(
                post("/v1/pi/pickup-points/" + order.getPvzMarketId() + "/orders/" + order.getId() +
                        "/deliver/partial/safe-package")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("order/partial_delivery/request_safe_package_first.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(formatVars(
                        getFileContent("order/partial_delivery/response_scan_first_safe_package.json"),
                        Map.of(
                                "orderId", order.getId(),
                                "externalId", order.getExternalId(),
                                "packageId1",
                                orderDeliveryResultBarcodeQueryService.getPackages(List.of(BARCODE_1)).get(0).getId(),
                                "iii1", iii,
                                "iii2", iii + 1,
                                "iii3", iii + 2
                        )
                )));

        var packageId = orderDeliveryResultBarcodeQueryService.getPackages(List.of(BARCODE_1)).get(0).getId();
        mockMvc.perform(
                delete("/v1/pi/pickup-points/" + order.getPvzMarketId() + "/orders/" + order.getId() +
                        "/deliver/partial/safe-package?id=" + packageId))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(formatVars(
                        getFileContent("order/partial_delivery/response_delete_first_safe_package.json"),
                        Map.of(
                                "orderId", order.getId(),
                                "externalId", order.getExternalId(),
                                "iii1", iii,
                                "iii2", iii + 1,
                                "iii3", iii + 2
                        )
                )));

        mockMvc.perform(
                post("/v1/pi/pickup-points/" + order.getPvzMarketId() + "/orders/" + order.getId() +
                        "/deliver/partial/safe-package")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("order/partial_delivery/request_safe_package_second.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(formatVars(
                        getFileContent("order/partial_delivery/response_scan_second_safe_package.json"),
                        Map.of(
                                "orderId", order.getId(),
                                "externalId", order.getExternalId(),
                                "packageId2",
                                orderDeliveryResultBarcodeQueryService.getPackages(List.of(BARCODE_2)).get(0).getId(),
                                "iii1", iii,
                                "iii2", iii + 1,
                                "iii3", iii + 2
                        )
                )));

        mockMvc.perform(
                post("/v1/pi/pickup-points/" + order.getPvzMarketId() + "/orders/" + order.getId() +
                        "/deliver/partial/safe-package/commit"))
                .andExpect(status().is2xxSuccessful());

        OrderDeliveryResultParams orderDeliveryResult = orderDeliveryResultQueryService.get(order.getId());
        assertThat(OrderDeliveryResultParams.builder()
                .orderId(order.getId())
                .status(PartialDeliveryStatus.PACKAGED)
                .barcodes(List.of(BARCODE_2))
                .build())
                .usingRecursiveComparison()
                .ignoringFields("items", "id", "deliveryItemsToCheckCodeIds")
                .isEqualTo(orderDeliveryResult);
    }

    @ParameterizedTest
    @ValueSource(strings = {BARCODE_3, BARCODE_4_1})
    @SneakyThrows
    void testAlternativeBarcodeMask(String barcode) {
        OrderSimpleParams order = createAndReceiveFashionOrder();
        OrderDeliveryResultParams deliveryResult = orderDeliveryResultCommandService.startFitting(order.getId());
        long iii = deliveryResult.getItems().get(0).getItemInstanceId();
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_1, RETURN);
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_2, RETURN);
        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderDeliveryResultCommandService.pay(order.getId());

        mockMvc.perform(
                post("/v1/pi/pickup-points/" + order.getPvzMarketId() + "/orders/" + order.getId() +
                        "/deliver/partial/safe-package")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(formatVars(
                                getFileContent("order/partial_delivery/request_safe_package_alternative.json"),
                                Map.of("alternativeBarcode", barcode))))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(formatVars(
                        getFileContent("order/partial_delivery/response_scan_alternative_safe_package.json"),
                        Map.of(
                                "orderId", order.getId(),
                                "externalId", order.getExternalId(),
                                "packageId1",
                                orderDeliveryResultBarcodeQueryService.getPackages(List.of(barcode)).get(0).getId(),
                                "alternativeBarcode", barcode,
                                "iii1", iii,
                                "iii2", iii + 1,
                                "iii3", iii + 2
                        )
                )));
    }

    @Test
    @SneakyThrows
    void tryToScanSafePackageWithExistingBarcode() {
        OrderSimpleParams order = createAndReceiveFashionOrder();
        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_1, RETURN);
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_2, RETURN);
        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderDeliveryResultCommandService.pay(order.getId());

        mockMvc.perform(
                post("/v1/pi/pickup-points/" + order.getPvzMarketId() + "/orders/" + order.getId() +
                        "/deliver/partial/safe-package")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("order/partial_delivery/request_safe_package_first.json")))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(
                post("/v1/pi/pickup-points/" + order.getPvzMarketId() + "/orders/" + order.getId() +
                        "/deliver/partial/safe-package")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("order/partial_delivery/request_safe_package_first.json")))
                .andExpect(status().is4xxClientError());
    }

    private OrderSimpleParams createAndReceiveFashionOrder() {
        Order order = orderFactory.createSimpleFashionOrder();
        orderFactory.receiveOrder(order.getId());
        return orderQueryService.getSimple(order.getId());
    }

    private List<OrderSimpleParams> createAndReceiveFashionOrderWithSiblings() {
        PickupPoint pickupPoint =
                pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                .brandingType(PickupPointBrandingType.FULL)
                                .build())
                        .build());

        Order orderWithSibling = orderFactory.createSimpleFashionOrder(true, pickupPoint);
        Order deliveredOrder = orderFactory.createSimpleFashionOrder(true, pickupPoint);
        Order siblingOrder = orderFactory.createSimpleFashionOrder(true, pickupPoint);

        orderFactory.receiveOrder(deliveredOrder.getId());
        orderFactory.receiveOrder(orderWithSibling.getId());
        orderFactory.receiveOrder(siblingOrder.getId());

        orderFactory.verifyOrder(deliveredOrder.getId());
        orderFactory.deliverOrder(deliveredOrder.getId(), VERIFICATION_CODE, PREPAID);
        return List.of(orderQueryService.getSimple(orderWithSibling.getId()),
                orderQueryService.getSimple(deliveredOrder.getId()),
                orderQueryService.getSimple(siblingOrder.getId()));
    }

    @ParameterizedTest
    @CsvSource({"true, PVZ_FBS_RET_1", "false, PVZ_FBY_RET_1"})
    @SneakyThrows
    void testGenerateBarcodeForAllOrders(boolean isFbs, String generatedBarcode) {
        configurationGlobalCommandService.setValue(GENERATE_SAFE_PACKAGE_BARCODE_FOR_FBY_ENABLED, true);
        OrderSimpleParams order = createAndReceiveFbsOrder(isFbs);
        OrderDeliveryResultParams deliveryResult = orderDeliveryResultCommandService.startFitting(order.getId());
        long iii1 = deliveryResult.getItems().get(0).getItemInstanceId();
        long iii2 = deliveryResult.getItems().get(1).getItemInstanceId();
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_1, iii1, RETURN, SCAN, null);
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_2, iii2, RETURN, SCAN, null);
        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderDeliveryResultCommandService.pay(order.getId());

        mockMvc.perform(
                post("/v1/pi/pickup-points/" + order.getPvzMarketId() + "/orders/" + order.getId() +
                        "/deliver/partial/safe-package/generate"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(formatVars(
                        getFileContent("order/partial_delivery/response_generate_safe_package_barcode.json"),
                        Map.of(
                                "orderId", order.getId(),
                                "externalId", order.getExternalId(),
                                "packageId1",
                                orderDeliveryResultBarcodeQueryService
                                        .getPackages(List.of(generatedBarcode)).get(0).getId(),
                                "brandNewBarcode", generatedBarcode,
                                "iii1", iii1,
                                "iii2", iii1 + 1,
                                "iii3", iii1 + 2
                        )
                )));
    }

    private OrderSimpleParams createAndReceiveFbsOrder(boolean fbs) {
        Order order = orderFactory.createSimpleFbsFashionOrder(fbs);
        orderFactory.receiveOrder(order.getId());
        return orderQueryService.getSimple(order.getId());
    }

    @Test
    @SneakyThrows
    void testHasNotCancelDeliveryActionForNotFitFashionOrder() {
        ZoneOffset zone = ZoneOffset.ofHours(PickupPoint.DEFAULT_TIME_OFFSET);
        LocalDateTime dateTime = LocalDateTime.of(
                LocalDate.of(2021, 1, 10),
                LocalTime.of(21, 40));
        clock.setFixed(dateTime.toInstant(zone), zone);

        OrderSimpleParams order = createAndReceiveFashionOrder();
        orderFactory.verifyOrder(order.getId());

        mockMvc.perform(
                post("/v1/pi/pickup-points/" + order.getPvzMarketId() + "/orders/" + order.getId() +
                        "/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deliveryType\": \"VERIFICATION_CODE\"}"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("order/response_deliver_partial.json"),
                        order.getId(), order.getExternalId(), order.getPickupPointId(),
                        order.getPvzMarketId()), false
                ));
    }

    @Test
    @SneakyThrows
    void testHasNotCancelDeliveryActionForFitFashionOrder() {
        ZoneOffset zone = ZoneOffset.ofHours(PickupPoint.DEFAULT_TIME_OFFSET);
        LocalDateTime dateTime = LocalDateTime.of(
                LocalDate.of(2021, 1, 10),
                LocalTime.of(21, 40));
        clock.setFixed(dateTime.toInstant(zone), zone);

        OrderSimpleParams order = createAndReceiveFashionOrder();
        orderFactory.verifyOrder(order.getId());
        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.finishFitting(order.getId());

        mockMvc.perform(
                post("/v1/pi/pickup-points/" + order.getPvzMarketId() + "/orders/" + order.getId() +
                        "/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deliveryType\": \"VERIFICATION_CODE\"}"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("order/response_deliver_fit_fashion_order.json"),
                        order.getId(), order.getExternalId(), order.getPickupPointId(),
                        order.getPvzMarketId()), false
                ));
        mockMvc.perform(
                post("/v1/pi/pickup-points/" + order.getPvzMarketId() + "/orders/" + order.getId() +
                        "/cancel-delivery")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @SneakyThrows
    void testHasPartialDeliverOrderActionAfterDeliveryWithoutFittingAndCancelling() {
        ZoneOffset zone = ZoneOffset.ofHours(PickupPoint.DEFAULT_TIME_OFFSET);
        LocalDateTime dateTime = LocalDateTime.of(
                LocalDate.of(2021, 1, 10),
                LocalTime.of(21, 40));
        clock.setFixed(dateTime.toInstant(zone), zone);

        OrderSimpleParams order = createAndReceiveFashionOrder();
        orderFactory.verifyOrder(order.getId());
        orderFactory.deliverOrder(order.getId(), OrderDeliveryType.PAYMENT, OrderPaymentType.CARD);

        mockMvc.perform(
                post("/v1/pi/pickup-points/" + order.getPvzMarketId() + "/orders/" + order.getId() +
                        "/cancel-delivery")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("order/response_deliver_partial_delivery_cancelled.json"),
                        order.getId(), order.getExternalId(), order.getPickupPointId(),
                        order.getPvzMarketId()), false
                ));
    }

    @Test
    @SneakyThrows
    void testScanItemCisForDelivery() {
        configurationGlobalCommandService.setValue(SCAN_CIS_FOR_DELIVERY_ENABLED, true);

        Order order = orderFactory.createFashionOrder(CIS_1_1, CIS_2_1);
        orderFactory.receiveOrder(order.getId());
        orderDeliveryResultCommandService.startFitting(order.getId());

        OrderDeliveryResultParams deliveryResult = orderDeliveryResultCommandService.finishFitting(order.getId());
        long itemId = deliveryResult.getItems().get(0).getItemInstanceId();

        String itemsToCheck = deliveryResult.getItems().stream()
                .map(OrderDeliveryResultItemParams::getItemInstanceId)
                .map(String::valueOf)
                .collect(Collectors.joining(", "));

        OrderSimpleParams orderParams = orderQueryService.getSimple(order.getId());
        mockMvc.perform(
                patch("/v1/pi/pickup-points/" + orderParams.getPvzMarketId() + "/orders/" + order.getId() +
                        "/deliver/partial/" + itemId + "/check-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("order/partial_delivery/request_scan_cis_for_delivery.json"))
        )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(
                        formatVars(
                                getFileContent("order/partial_delivery/response_get_page_after_delivery_scan.json"),
                                Map.of(
                                        "orderId", order.getId(),
                                        "externalId", order.getExternalId(),
                                        "iii1", itemId,
                                        "i1flow", "DELIVERY",
                                        "i1checked", "true",
                                        "itemToCheck", itemsToCheck
                                )
                        ), false
                ));
    }

    @Test
    @SneakyThrows
    void testNotScanItemCisForDelivery() {
        configurationGlobalCommandService.setValue(SCAN_CIS_FOR_DELIVERY_ENABLED, true);

        Order order = orderFactory.createFashionOrder(CIS_1_1, CIS_2_1);
        orderFactory.receiveOrder(order.getId());
        orderDeliveryResultCommandService.startFitting(order.getId());

        OrderDeliveryResultParams deliveryResult = orderDeliveryResultCommandService.finishFitting(order.getId());
        long itemId = deliveryResult.getItems().get(0).getItemInstanceId();

        OrderSimpleParams orderParams = orderQueryService.getSimple(order.getId());
        mockMvc.perform(
                patch("/v1/pi/pickup-points/" + orderParams.getPvzMarketId() + "/orders/" + order.getId() +
                        "/deliver/partial/" + itemId + "/check-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("order/partial_delivery/request_not_scan_cis_for_delivery.json"))
        )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(
                        formatVars(
                                getFileContent("order/partial_delivery/response_get_page_after_delivery_scan.json"),
                                Map.of(
                                        "orderId", order.getId(),
                                        "externalId", order.getExternalId(),
                                        "iii1", itemId,
                                        "i1flow", "RETURN",
                                        "i1checked", "false",
                                        "itemToCheck", deliveryResult.getItems().get(1).getItemInstanceId()
                                )
                        ), false
                ));


    }

}
