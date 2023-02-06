package ru.yandex.market.checkout.checkouter.pay;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.assertj.core.api.SoftAssertions;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.BnplInfo;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.cashback.model.Cashback;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOptions;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.installments.InstallmentsInfo;
import ru.yandex.market.checkout.checkouter.installments.InstallmentsOption;
import ru.yandex.market.checkout.checkouter.order.ItemService;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.OfferItem;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplItem;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplOrderCreateRequestBody;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplOrderService;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplPlanCheckRequestBody;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplServiceType;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.checkouter.viewmodel.CreatePaymentResponse;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.helpers.utils.PaymentParameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.bnpl.BnplMockConfigurer;
import ru.yandex.market.checkout.util.report.ItemInfo;
import ru.yandex.market.loyalty.api.model.CashbackType;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.BNPL_PLAN_CONSTRUCTOR;
import static ru.yandex.market.checkout.checkouter.pay.PaymentSubmethod.TINKOFF_INSTALLMENTS_6;
import static ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplItem.BnplItemBuilder.BNPL_ITEM_DELIVERY_ITEM_CODE_PREFIX;
import static ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplItem.BnplItemBuilder.BNPL_ITEM_ITEM_CODE_PREFIX;
import static ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplItem.BnplItemBuilder.BNPL_ITEM_ITEM_SERVICE_CODE_PREFIX;
import static ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplUserId.YANDEX_UID_HEADER;
import static ru.yandex.market.checkout.checkouter.pay.builders.AbstractPaymentBuilder.DELIVERY_TITLE;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParametersWithItems;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.prepaidBlueOrderParameters;
import static ru.yandex.market.checkout.providers.BnplTestProvider.CASHBACK_AMOUNT;
import static ru.yandex.market.checkout.providers.BnplTestProvider.defaultBnplParameters;
import static ru.yandex.market.checkout.providers.FulfilmentProvider.FF_SHOP_ID;
import static ru.yandex.market.checkout.providers.FulfilmentProvider.TEST_SHOP_SKU;
import static ru.yandex.market.checkout.providers.FulfilmentProvider.TEST_SKU;
import static ru.yandex.market.checkout.providers.WhiteParametersProvider.WHITE_SHOP_ID;
import static ru.yandex.market.checkout.providers.WhiteParametersProvider.defaultWhiteParameters;
import static ru.yandex.market.checkout.providers.WhiteParametersProvider.dsbsOrderItem;

/**
 * @author : poluektov
 * date: 2021-06-21.
 */
public class BnplPaymentTest extends AbstractWebTestBase {

    public static final List<String> SHOW_INFO_LIST = List.of(
            OrderItemProvider.SHOW_INFO,
            OrderItemProvider.ANOTHER_SHOW_INFO,
            OrderItemProvider.OTHER_SHOW_INFO
    );

    private static final List<Integer> HARDCODED_ALLOWED_HID = List.of(
            90864,
            90857,
            90855
    );

    private static final Long ANOTHER_SUPPLIER_ID = 999L;

    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private BnplMockConfigurer bnplMockConfigurer;
    @Autowired
    private RefundService refundService;
    @Autowired
    private QueuedCallService queuedCallService;

    @Resource(name = "checkouterAnnotationObjectMapper")
    private ObjectMapper checkouterAnnotationObjectMapper;

    public static Arguments createArguments(
            String testName,
            List<OrderItem> orderItems,
            Set<String> bnplOrderItemOfferIdNames,
            boolean bnplDelivery
    ) {
        return Arguments.of(testName, orderItems, bnplOrderItemOfferIdNames, bnplDelivery);
    }

    public static Stream<Arguments> parameterizedTestData() {
        var index = 0;
        // bnpl deposit is 344.00
        return Stream.of(
                createArguments(
                        "Все товары в рассрочку",
                        List.of(
                                createOrderItem(index++, "bnpl-1", 100.00, 1, SupplierType.THIRD_PARTY),
                                createOrderItem(index++, "bnpl-2", 50.00, 1, SupplierType.THIRD_PARTY),
                                createOrderItem(index++, "bnpl-3", 150.00, 1, SupplierType.FIRST_PARTY)
                        ),
                        Set.of("bnpl-1", "bnpl-2", "bnpl-3"),
                        true
                ),
                createArguments(
                        "Все 3P товары в рассрочку",
                        List.of(
                                createOrderItem(index++, "bnpl-1", 50.00, 3, SupplierType.THIRD_PARTY),
                                createOrderItem(index++, "bnpl-2", 100.00, 2, SupplierType.THIRD_PARTY),
                                createOrderItem(index++, "payment-1", 150.00, 1, SupplierType.FIRST_PARTY)
                        ),
                        Set.of("bnpl-1", "bnpl-2"),
                        false
                ),
                createArguments(
                        "В рассрочку только товар с max(price*count)",
                        List.of(
                                createOrderItem(index++, "bnpl-1", 100.00, 4, SupplierType.THIRD_PARTY),
                                createOrderItem(index++, "payment-1", 50.00, 3, SupplierType.THIRD_PARTY),
                                createOrderItem(index++, "payment-2", 550.00, 1, SupplierType.FIRST_PARTY)
                        ),
                        Set.of("bnpl-1"),
                        false
                ),
                createArguments(
                        "В рассрочку только товар с max(price*count), цена товара занижена и кол-во завышено",
                        List.of(
                                createOrderItem(index++, "bnpl-1", 30.00, 15, SupplierType.THIRD_PARTY),
                                createOrderItem(index++, "payment-1", 50.00, 3, SupplierType.THIRD_PARTY),
                                createOrderItem(index++, "payment-2", 550.00, 1, SupplierType.FIRST_PARTY)
                        ),
                        Set.of("bnpl-1"),
                        false
                ),
                createArguments(
                        "Все 1P товары в рассрочку",
                        List.of(
                                createOrderItem(index++, "bnpl-1", 30.00, 1, SupplierType.FIRST_PARTY),
                                createOrderItem(index++, "bnpl-2", 50.00, 1, SupplierType.FIRST_PARTY),
                                createOrderItem(index++, "bnpl-3", 100.00, 1, SupplierType.FIRST_PARTY)
                        ),
                        Set.of("bnpl-1", "bnpl-2", "bnpl-3"),
                        true
                ),
                createArguments(
                        "1P товар c max(price*count) в рассрочку",
                        List.of(
                                createOrderItem(index++, "payment-1", 30.00, 1, SupplierType.FIRST_PARTY),
                                createOrderItem(index++, "payment-2", 50.00, 1, SupplierType.FIRST_PARTY),
                                createOrderItem(index++, "bnpl-1", 500.00, 1, SupplierType.FIRST_PARTY)
                        ),
                        Set.of("bnpl-1"),
                        false
                ),
                createArguments(
                        "Все 1P товары в рассрочку, кроме товара с categoryId == 1",
                        List.of(
                                createOrderItem(index++, "bnpl-1", 30.00, 1, SupplierType.FIRST_PARTY),
                                createOrderItem(index++, "bnpl-2", 50.00, 1, SupplierType.FIRST_PARTY),
                                createOrderItem(index++, "payment-1", 100.00, 1, SupplierType.FIRST_PARTY, 1)
                        ),
                        Set.of("bnpl-1", "bnpl-2"),
                        true
                )
        );
    }

    public static String convertOfferNameToItemCode(String offerName, Order order) {
        OrderItem orderItem = order.getItems().stream()
                .filter(item -> item.getOfferName().equals(offerName)).findAny().get();

        return "item-" + orderItem.getId();
    }

    public static OrderItem createOrderItem(
            int index,
            String offerId,
            double price,
            int count,
            SupplierType supplierType
    ) {
        return createOrderItem(index,
                offerId,
                price,
                count,
                supplierType,
                HARDCODED_ALLOWED_HID.get(index % HARDCODED_ALLOWED_HID.size()));
    }

    public static OrderItem createOrderItem(
            int index,
            String offerId,
            double price,
            int count,
            SupplierType supplierType,
            Integer categoryId
    ) {
        var orderItem = OrderItemProvider.buildOrderItem(offerId, new BigDecimal(price), count);
        orderItem.setId(null);
        orderItem.setOfferName(offerId);
        orderItem.setCategoryId(categoryId);
        orderItem.setFeedId(System.currentTimeMillis());
        orderItem.setMsku(System.currentTimeMillis());
        orderItem.setShopSku("sku-" + offerId);
        orderItem.setSku(orderItem.getMsku().toString());
        orderItem.setSupplierId(System.currentTimeMillis());

        orderItem.setWareMd5(OrderItemProvider.DEFAULT_WARE_MD5);
        orderItem.setShowInfo(SHOW_INFO_LIST.get(index % SHOW_INFO_LIST.size()));
        orderItem.setSupplierType(supplierType);
        return orderItem;
    }

    @BeforeEach
    public void mockBnpl() {
        checkouterProperties.setEnableServicesPrepay(true);
        checkouterProperties.setEnableBnpl(true);
        bnplMockConfigurer.mockWholeBnpl();
    }

    @Test
    public void createBnplPayment() throws IOException {
        Parameters parameters = prepaidBlueOrderParameters();
        parameters.getItems().forEach(item -> item.setCategoryId(90864));
        parameters.addItemService();
        parameters.getOrder().getDelivery().setPrice(BigDecimal.ONE);
        parameters.getOrder().getDelivery().setBuyerPrice(BigDecimal.ONE);
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        parameters.getBuiltMultiCart().setBnplInfo(new BnplInfo());
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(true);
        Order order = orderCreateHelper.createOrder(parameters);
        CreatePaymentResponse paymentResponse = orderPayHelper.payWithRealResponse(order);
        assertTrue(paymentResponse.getBnpl());
        assertEquals("http://bnpl.yandex.ru", paymentResponse.getPaymentUrl());

        Payment payment = paymentService.getPayment(paymentResponse.getId(), ClientInfo.SYSTEM);
        assertEquals(PaymentGoal.BNPL, payment.getType());
        assertEquals(PaymentStatus.IN_PROGRESS, payment.getStatus());
        assertNotNull(payment.getBasketKey().getBasketId());
        assertEquals("split_6_month", payment.getProperties().getBnplPlanConstructor());

        var bnplOrderCreateRequestBody = bnplMockConfigurer.getBnplRequestBody(BnplMockConfigurer.POST_ORDER_CREATE);
        var request = checkouterAnnotationObjectMapper.readValue(bnplOrderCreateRequestBody,
                BnplOrderCreateRequestBody.class);
        var bnplItem = request.getServices()
                .stream()
                .map(BnplOrderService::getItems)
                .flatMap(Collection::stream)
                .filter(item -> !item.getItemCode().startsWith("delivery"))
                .findFirst()
                .orElseGet(() -> fail("no bnpl item found"));
        var orderItem = order.getItems()
                .stream()
                .findFirst()
                .orElseGet(() -> fail("No order item found"));
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(bnplItem.getItemCode()).isEqualTo("item-" + orderItem.getId());
            softly.assertThat(bnplItem.getCount()).isEqualTo(orderItem.getCount());
            softly.assertThat(bnplItem.getCategory()).isEqualTo(orderItem.getCategoryId().toString());
            softly.assertThat(bnplItem.getShopId()).isEqualTo(orderItem.getSupplierId().toString());
        });

        var containsBnplItemWithEmptyTitle = request.getServices().stream()
                .map(BnplOrderService::getItems)
                .flatMap(Collection::stream)
                .map(BnplItem::getTitle)
                .anyMatch(title -> title == null || title.isBlank());
        assertThat(containsBnplItemWithEmptyTitle).isFalse();

        assertNotNull(request.getOrderMeta().getConsumerMeta().getOrderIds());
        assertEquals("split_4_month", request.getOrderMeta().getConstructor());
    }

    @Test
    void shouldClearBnplOnDelivery() {
        Parameters parameters = prepaidBlueOrderParameters();
        parameters.getItems().forEach(item -> item.setCategoryId(90864));
        parameters.addItemService();
        parameters.getOrder().getDelivery().setPrice(BigDecimal.ONE);
        parameters.getOrder().getDelivery().setBuyerPrice(BigDecimal.ONE);
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        parameters.getBuiltMultiCart().setBnplInfo(new BnplInfo());
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(true);
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);


        order = orderService.getOrder(order.getId(), ClientInfo.SYSTEM);
        Payment payment = paymentService.getPayment(order.getPaymentId(), ClientInfo.SYSTEM);
        List<ServeEvent> events = bnplMockConfigurer.findEventsByStubName(BnplMockConfigurer.POST_ORDER_DELIVER);
        assertEquals(1, events.size());
        assertEquals(PaymentStatus.CLEARED, payment.getStatus());

        Collection<Payment> notSyncedWithBalancePayments = paymentService.findPaymentsNotSyncedWithBalance(
                StoragePaymentService.PAYMENT_STATUSES_TO_CHECK, payment.getId(), payment.getId() + 1, 1, null);
        assertThat(notSyncedWithBalancePayments)
                .withFailMessage("BNPL payment must be excluded from payments that are not synced with balance")
                .isEmpty();
    }

    @Test
    void shouldNotClearBnplOnOrderDeliverError() {
        Parameters parameters = prepaidBlueOrderParameters();
        parameters.getItems().forEach(item -> item.setCategoryId(90864));
        parameters.addItemService();
        parameters.getOrder().getDelivery().setPrice(BigDecimal.ONE);
        parameters.getOrder().getDelivery().setBuyerPrice(BigDecimal.ONE);
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        parameters.getBuiltMultiCart().setBnplInfo(new BnplInfo());
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(true);
        Order order = orderCreateHelper.createOrder(parameters);

        bnplMockConfigurer.mockOrderDeliverBadRequest();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        order = orderService.getOrder(order.getId(), ClientInfo.SYSTEM);
        Payment payment = paymentService.getPayment(order.getPaymentId(), ClientInfo.SYSTEM);
        assertEquals(PaymentStatus.HOLD, payment.getStatus());
    }

    @Test
    public void createBnplPaymentPlanCheckIsEmpty() throws Exception {
        Parameters parameters = prepaidBlueOrderParameters();
        parameters.getItems().forEach(item -> item.setCategoryId(90864));
        parameters.addItemService();
        parameters.getOrder().getDelivery().setPrice(BigDecimal.ONE);
        parameters.getOrder().getDelivery().setBuyerPrice(BigDecimal.ONE);
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        parameters.getBuiltMultiCart().setBnplInfo(new BnplInfo());
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(true);
        Order order = orderCreateHelper.createOrder(parameters);

        bnplMockConfigurer.mockEmptyPlanCheck(YANDEX_UID_HEADER);
        MockHttpServletRequestBuilder builder = post("/orders/{orderId}/payment", order.getId())
                .param(CheckouterClientParams.UID, String.valueOf(order.getBuyer().getUid()))
                .param("returnPath", new PaymentParameters().getReturnPath())
                .param(BNPL_PLAN_CONSTRUCTOR, "split_4_month");
        var response = mockMvc.perform(builder).andDo(log()).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString())
                .isEqualTo("{\"status\":400,\"code\":\"empty_bnpl_plan\",\"message\":" +
                        "\"Bnpl plan 'split_4_month' not found\"}");
    }

    @Test
    public void createBnplPaymentPlanCheckIsNotFound() throws Exception {
        Parameters parameters = prepaidBlueOrderParameters();
        parameters.getItems().forEach(item -> item.setCategoryId(90864));
        parameters.addItemService();
        parameters.getOrder().getDelivery().setPrice(BigDecimal.ONE);
        parameters.getOrder().getDelivery().setBuyerPrice(BigDecimal.ONE);
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        parameters.getBuiltMultiCart().setBnplInfo(new BnplInfo());
        parameters.getBuiltMultiCart().getBnplInfo().setSelectedPlan("fake_plan");
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(true);
        Order order = orderCreateHelper.createOrder(parameters);

        MockHttpServletRequestBuilder builder = post("/orders/{orderId}/payment", order.getId())
                .param(CheckouterClientParams.UID, String.valueOf(order.getBuyer().getUid()))
                .param("returnPath", new PaymentParameters().getReturnPath())
                .param(BNPL_PLAN_CONSTRUCTOR, "fake_plan");
        var response = mockMvc.perform(builder).andDo(log()).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString())
                .isEqualTo("{\"status\":400,\"code\":\"empty_bnpl_plan\",\"message\":" +
                        "\"Bnpl plan 'fake_plan' not found\"}");
    }

    @Test
    public void createBnplPaymentPlanCheckIsMissing() throws Exception {
        Parameters parameters = prepaidBlueOrderParameters();
        parameters.getItems().forEach(item -> item.setCategoryId(90864));
        parameters.addItemService();
        parameters.getOrder().getDelivery().setPrice(BigDecimal.ONE);
        parameters.getOrder().getDelivery().setBuyerPrice(BigDecimal.ONE);
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        parameters.getBuiltMultiCart().setBnplInfo(new BnplInfo());
        parameters.getBuiltMultiCart().getBnplInfo().setSelectedPlan("fake_plan");
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(true);
        Order order = orderCreateHelper.createOrder(parameters);
        bnplMockConfigurer.mockEmptyPlanCheck(YANDEX_UID_HEADER);

        MockHttpServletRequestBuilder builder = post("/orders/{orderId}/payment", order.getId())
                .param(CheckouterClientParams.UID, String.valueOf(order.getBuyer().getUid()))
                .param("returnPath", new PaymentParameters().getReturnPath());
        var response = mockMvc.perform(builder).andDo(log()).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString())
                .isEqualTo("{\"status\":400,\"code\":\"empty_bnpl_plan\",\"message\":" +
                        "\"Bnpl plan constructor is missing\"}");
    }

    @Test
    public void createBnplPaymentWithCashback() throws IOException {
        trustMockConfigurer.mockListWalletBalanceResponse();

        Parameters parameters = prepaidBlueOrderParameters();
        parameters.getItems().forEach(item -> item.setCategoryId(90864));
        parameters.addItemService(builder -> builder.price(BigDecimal.valueOf(7)));
        parameters.getBuiltMultiCart().setBnplInfo(new BnplInfo());
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(true);
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.SPEND);
        parameters.getOrder().getDelivery().setPrice(BigDecimal.ONE);
        parameters.getOrder().getDelivery().setBuyerPrice(BigDecimal.ONE);
        parameters.setDeliveryType(DeliveryType.DELIVERY);

        Order order = orderCreateHelper.createOrder(parameters);
        CreatePaymentResponse paymentResponse = orderPayHelper.payWithRealResponse(order);
        assertTrue(paymentResponse.getBnpl());
        assertEquals("http://bnpl.yandex.ru", paymentResponse.getPaymentUrl());

        Payment payment = paymentService.getPayment(paymentResponse.getId(), ClientInfo.SYSTEM);
        validatePayment(payment);

        var bnplOrderCreateRequestBody = bnplMockConfigurer.getBnplRequestBody(BnplMockConfigurer.POST_ORDER_CREATE);
        var request = checkouterAnnotationObjectMapper.readValue(bnplOrderCreateRequestBody,
                BnplOrderCreateRequestBody.class);
        var bnplItem = request.getServices()
                .stream()
                .map(BnplOrderService::getItems)
                .flatMap(Collection::stream)
                .filter(item -> item.getItemCode().startsWith("item"))
                .findFirst()
                .orElseGet(() -> fail("no bnpl item found"));
        var orderItem = order.getItems()
                .stream()
                .findFirst()
                .orElseGet(() -> fail("No order item found"));
        var bnplItemService = request.getServices()
                .stream()
                .map(BnplOrderService::getItems)
                .flatMap(Collection::stream)
                .filter(item -> item.getItemCode().startsWith("service"))
                .findFirst()
                .orElseGet(() -> fail("no bnpl itemService found"));
        ItemService itemService = orderItem.getServices()
                .stream()
                .findFirst()
                .orElseGet(() -> fail("No itemService found"));
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(request.getServices()).hasSize(1);
            softly.assertThat(request.getServices().stream().map(BnplOrderService::getAmount).reduce(BigDecimal.ZERO,
                    BigDecimal::add)).isEqualTo(new BigDecimal("327.0000"));
            softly.assertThat(bnplItem.getItemCode()).isEqualTo("item-" + orderItem.getId());
            // ya plus discount
            softly.assertThat(bnplItem.getPrice()).isLessThan(orderItem.getBuyerPrice());
            softly.assertThat(bnplItem.getCount()).isEqualTo(orderItem.getCount());
            softly.assertThat(bnplItem.getCategory()).isEqualTo(orderItem.getCategoryId().toString());
            softly.assertThat(bnplItem.getShopId()).isEqualTo(orderItem.getSupplierId().toString());
            softly.assertThat(bnplItemService.getItemCode()).isEqualTo("service-" + itemService.getId());
            softly.assertThat(bnplItemService.getPrice().compareTo(itemService.getPrice())).isEqualTo(0);
            softly.assertThat(bnplItemService.getCount()).isEqualTo(itemService.getCount());
        });

        var containsBnplItemWithEmptyTitle = request.getServices().stream()
                .map(BnplOrderService::getItems)
                .flatMap(Collection::stream)
                .map(BnplItem::getTitle)
                .anyMatch(title -> title == null || title.isBlank());
        assertThat(containsBnplItemWithEmptyTitle).isFalse();
    }

    @Test
    public void createBnplPaymentWithItemCountGreaterThanOneAndCashback() throws IOException {
        trustMockConfigurer.mockListWalletBalanceResponse();
        checkouterFeatureWriter.writeValue(BooleanFeatureType.SINGLE_SERVICE_PER_MULTIPLE_ORDER_ITEMS, false);

        Parameters parameters = defaultBnplParameters(
                createOrderItem(0, "bnpl-1", 50.00, 3, SupplierType.FIRST_PARTY, 90864)
        );
        parameters.addItemService(builder -> builder.price(BigDecimal.valueOf(7)));
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.SPEND);
        parameters.getBuiltMultiCart().setCashback(new Cashback(null, CashbackOptions.allowed(CASHBACK_AMOUNT, "1")));

        Order order = orderCreateHelper.createOrder(parameters);
        CreatePaymentResponse paymentResponse = orderPayHelper.payWithRealResponse(order);
        assertTrue(paymentResponse.getBnpl());
        assertEquals("http://bnpl.yandex.ru", paymentResponse.getPaymentUrl());

        Payment payment = paymentService.getPayment(paymentResponse.getId(), ClientInfo.SYSTEM);
        validatePayment(payment);

        var bnplOrderCreateRequestBody = bnplMockConfigurer.getBnplRequestBody(BnplMockConfigurer.POST_ORDER_CREATE);
        var request = checkouterAnnotationObjectMapper.readValue(bnplOrderCreateRequestBody,
                BnplOrderCreateRequestBody.class);
        var bnplItem = request.getServices()
                .stream()
                .map(BnplOrderService::getItems)
                .flatMap(Collection::stream)
                .filter(item -> item.getItemCode().startsWith("item"))
                .findFirst()
                .orElseGet(() -> fail("no bnpl item found"));
        var orderItem = order.getItems()
                .stream()
                .findFirst()
                .orElseGet(() -> fail("No order item found"));
        var bnplItemService = request.getServices()
                .stream()
                .map(BnplOrderService::getItems)
                .flatMap(Collection::stream)
                .filter(item -> item.getItemCode().startsWith("service"))
                .findFirst()
                .orElseGet(() -> fail("no bnpl itemService found"));
        ItemService itemService = orderItem.getServices()
                .stream()
                .findFirst()
                .orElseGet(() -> fail("No itemService found"));
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(request.getServices()).hasSize(1);
            // 40 price x 3 count + 100 delivery + 7 servicePrice x 3 count = 241.00
            softly.assertThat(request.getServices().stream().map(BnplOrderService::getAmount).reduce(BigDecimal.ZERO,
                    BigDecimal::add)).isEqualTo(new BigDecimal("241.0000"));
            softly.assertThat(bnplItem.getItemCode()).isEqualTo("item-" + orderItem.getId());
            // ya plus discount
            // 50 price - (30 max cashbask spent / 3 count) = 40.00
            softly.assertThat(bnplItem.getPrice()).isEqualTo(new BigDecimal("40.0000"));
            softly.assertThat(bnplItem.getCount()).isEqualTo(orderItem.getCount());
            softly.assertThat(bnplItem.getCategory()).isEqualTo(orderItem.getCategoryId().toString());
            softly.assertThat(bnplItem.getShopId()).isEqualTo(orderItem.getSupplierId().toString());
            softly.assertThat(bnplItemService.getItemCode()).isEqualTo("service-" + itemService.getId());
            softly.assertThat(bnplItemService.getPrice().compareTo(itemService.getPrice())).isEqualTo(0);
            softly.assertThat(bnplItemService.getCount()).isEqualTo(itemService.getCount());
        });

        var containsBnplItemWithEmptyTitle = request.getServices().stream()
                .map(BnplOrderService::getItems)
                .flatMap(Collection::stream)
                .map(BnplItem::getTitle)
                .anyMatch(title -> title == null || title.isBlank());
        assertThat(containsBnplItemWithEmptyTitle).isFalse();
    }

    @Test
    public void createBnplPaymentWithItemCountGreaterThanOneAndCashbackWithSingleServicePerItems() throws IOException {
        trustMockConfigurer.mockListWalletBalanceResponse();
        checkouterFeatureWriter.writeValue(BooleanFeatureType.SINGLE_SERVICE_PER_MULTIPLE_ORDER_ITEMS, true);

        Parameters parameters = defaultBnplParameters(
                createOrderItem(0, "bnpl-1", 50.00, 3, SupplierType.FIRST_PARTY, 90864)
        );
        parameters.addItemService(builder -> builder.price(BigDecimal.valueOf(7)));
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.SPEND);
        parameters.getBuiltMultiCart().setCashback(new Cashback(null, CashbackOptions.allowed(CASHBACK_AMOUNT, "1")));

        Order order = orderCreateHelper.createOrder(parameters);
        CreatePaymentResponse paymentResponse = orderPayHelper.payWithRealResponse(order);
        assertTrue(paymentResponse.getBnpl());
        assertEquals("http://bnpl.yandex.ru", paymentResponse.getPaymentUrl());

        Payment payment = paymentService.getPayment(paymentResponse.getId(), ClientInfo.SYSTEM);
        validatePayment(payment);

        var bnplOrderCreateRequestBody = bnplMockConfigurer.getBnplRequestBody(BnplMockConfigurer.POST_ORDER_CREATE);
        var request = checkouterAnnotationObjectMapper.readValue(bnplOrderCreateRequestBody,
                BnplOrderCreateRequestBody.class);
        var bnplItem = request.getServices()
                .stream()
                .map(BnplOrderService::getItems)
                .flatMap(Collection::stream)
                .filter(item -> item.getItemCode().startsWith("item"))
                .findFirst()
                .orElseGet(() -> fail("no bnpl item found"));
        var orderItem = order.getItems()
                .stream()
                .findFirst()
                .orElseGet(() -> fail("No order item found"));
        var bnplItemService = request.getServices()
                .stream()
                .map(BnplOrderService::getItems)
                .flatMap(Collection::stream)
                .filter(item -> item.getItemCode().startsWith("service"))
                .findFirst()
                .orElseGet(() -> fail("no bnpl itemService found"));
        ItemService itemService = orderItem.getServices()
                .stream()
                .findFirst()
                .orElseGet(() -> fail("No itemService found"));
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(request.getServices()).hasSize(1);
            // 40 price x 3 count + 100 delivery + 7 servicePrice x 1 count = 227.00
            softly.assertThat(request.getServices().stream().map(BnplOrderService::getAmount).reduce(BigDecimal.ZERO,
                    BigDecimal::add)).isEqualTo(new BigDecimal("227.0000"));
            softly.assertThat(bnplItem.getItemCode()).isEqualTo("item-" + orderItem.getId());
            // ya plus discount
            // 50 price - (30 max cashbask spent / 3 count) = 40.00
            softly.assertThat(bnplItem.getPrice()).isEqualTo(new BigDecimal("40.0000"));
            softly.assertThat(bnplItem.getCount()).isEqualTo(orderItem.getCount());
            softly.assertThat(bnplItem.getCategory()).isEqualTo(orderItem.getCategoryId().toString());
            softly.assertThat(bnplItem.getShopId()).isEqualTo(orderItem.getSupplierId().toString());
            softly.assertThat(bnplItemService.getItemCode()).isEqualTo("service-" + itemService.getId());
            softly.assertThat(bnplItemService.getPrice().compareTo(itemService.getPrice())).isEqualTo(0);
            softly.assertThat(bnplItemService.getCount()).isEqualTo(itemService.getCount());
        });

        var containsBnplItemWithEmptyTitle = request.getServices().stream()
                .map(BnplOrderService::getItems)
                .flatMap(Collection::stream)
                .map(BnplItem::getTitle)
                .anyMatch(title -> title == null || title.isBlank());
        assertThat(containsBnplItemWithEmptyTitle).isFalse();
    }

    private void validatePayment(Payment payment) {
        assertEquals(PaymentGoal.BNPL, payment.getType());
        assertEquals(PaymentStatus.IN_PROGRESS, payment.getStatus());
        assertNotNull(payment.getBasketKey().getBasketId());

        PaymentPartition yandexCashbackPart = payment.getPartitions().stream()
                .filter(part -> part.getPaymentAgent() == PaymentAgent.YANDEX_CASHBACK)
                .findAny()
                .orElseThrow();

        PaymentPartition bnplPaymentPart = payment.getPartitions().stream()
                .filter(part -> part.getPaymentAgent() == PaymentAgent.DEFAULT)
                .findAny()
                .orElseThrow();

        MatcherAssert.assertThat(yandexCashbackPart.getAmount(), greaterThanOrEqualTo(BigDecimal.ONE));


        MatcherAssert.assertThat(bnplPaymentPart.getAmount(), greaterThanOrEqualTo(BigDecimal.ONE));

        MatcherAssert.assertThat(
                yandexCashbackPart.getAmount()
                        .add(bnplPaymentPart.getAmount()),
                Matchers.comparesEqualTo(payment.getTotalAmount())
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedTestData")
    public void createBnplPaymentOrderItemSeparationTest(
            String testName,
            List<OrderItem> orderItems,
            Set<String> bnplOrderItemOfferIdNames,
            boolean bnplDelivery
    ) throws Exception {
        Parameters parameters = defaultBlueOrderParametersWithItems(orderItems.toArray(OrderItem[]::new));
        parameters.addItemService(builder -> builder.price(BigDecimal.valueOf(7)));
        parameters.getBuiltMultiCart().setBnplInfo(new BnplInfo());
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(true);
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        validateBnplFlagOnOrderItem(multiCart, bnplOrderItemOfferIdNames);
        var multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        validateBnplFlagOnOrderItem(multiCart, bnplOrderItemOfferIdNames);

        Order order = multiOrder.getOrders().stream().findFirst().orElseThrow();

        CreatePaymentResponse paymentResponse = orderPayHelper.payWithRealResponse(order);
        assertTrue(paymentResponse.getBnpl());
        assertEquals("http://bnpl.yandex.ru", paymentResponse.getPaymentUrl());

        Payment payment = paymentService.getPayment(paymentResponse.getId(), ClientInfo.SYSTEM);
        assertEquals(PaymentGoal.BNPL, payment.getType());
        assertEquals(PaymentStatus.IN_PROGRESS, payment.getStatus());
        assertNotNull(payment.getBasketKey().getBasketId());

        order = orderService.getOrder(order.getId());
        var bnplOfferIds = order.getItems().stream()
                .filter(OrderItem::isBnpl)
                .map(OrderItem::getOfferId)
                .collect(Collectors.toSet());
        assertThat(bnplOfferIds).isEqualTo(bnplOrderItemOfferIdNames);

        var bnplOrderCreateRequestBody = bnplMockConfigurer.getBnplRequestBody(BnplMockConfigurer.POST_ORDER_CREATE);

        var request = checkouterAnnotationObjectMapper.readValue(bnplOrderCreateRequestBody,
                BnplOrderCreateRequestBody.class);

        var loanItems = getBnplItemsByType(request, BnplServiceType.LOAN);
        var paymentItems = getBnplItemsByType(request, BnplServiceType.PAYMENT);

        Order finalOrder = order;
        Set<String> bnplItemCodes =
                bnplOfferIds.stream().map(name -> convertOfferNameToItemCode(name, finalOrder))
                        .collect(Collectors.toSet());

        assertThat(loanItems).containsAll(bnplItemCodes);
        assertThat(paymentItems).containsAll(orderItems.stream()
                .map(item -> convertOfferNameToItemCode(item.getOfferName(), finalOrder))
                .filter(offerId -> !bnplItemCodes.contains(offerId))
                .collect(Collectors.toSet())
        );

        assertThat(loanItems.stream().anyMatch(
                item -> item.startsWith(BNPL_ITEM_DELIVERY_ITEM_CODE_PREFIX))).isEqualTo(bnplDelivery);
        assertThat(paymentItems.stream().anyMatch(
                item -> item.startsWith(BNPL_ITEM_DELIVERY_ITEM_CODE_PREFIX))).isEqualTo(!bnplDelivery);

        var bnplPlanCheckRequestBody = bnplMockConfigurer.getBnplRequestBody(BnplMockConfigurer.POST_PLAN_CHECK);
        var planCheckRequest = checkouterAnnotationObjectMapper.readValue(bnplPlanCheckRequestBody,
                BnplPlanCheckRequestBody.class);

        var planCheckLoanItems = getBnplItemsByType(planCheckRequest, BnplServiceType.LOAN);
        var planCheckPaymentItems = getBnplItemsByType(planCheckRequest, BnplServiceType.PAYMENT);

        var expectedLoanBnplItems = finalOrder.getItems().stream()
                .filter(item -> HARDCODED_ALLOWED_HID.contains(item.getCategoryId()))
                .map(OfferItem::getOfferId)
                .collect(Collectors.toSet());

        assertThat(planCheckLoanItems).containsAll(expectedLoanBnplItems);
        assertThat(planCheckPaymentItems).containsAll(orderItems.stream()
                .map(OfferItem::getOfferId)
                .filter(offerId -> !expectedLoanBnplItems.contains(offerId))
                .collect(Collectors.toSet())
        );

        assertTrue(planCheckLoanItems.stream().anyMatch(item -> item.startsWith(DELIVERY_TITLE)));
    }

    private void validateBnplFlagOnOrderItem(MultiCart multiCart, Set<String> bnplOrderItemOfferIdNames) {
        var bnplOfferNames = multiCart.getCarts()
                .stream()
                .map(Order::getItems)
                .flatMap(Collection::stream)
                .filter(OrderItem::isBnpl)
                .map(OrderItem::getOfferName)
                .collect(Collectors.toSet());
        assertThat(bnplOfferNames).isEqualTo(bnplOrderItemOfferIdNames);
    }

    @Test
    public void checkoutMultiCartWithPostpaidAndPrepaidOrdersTest() throws Exception {
        // 1st cart
        Parameters parameters = defaultBnplParameters();
        parameters.addItemService();
        // 2nd cart
        OrderItem anotherOrderItem = OrderItemProvider.getAnotherOrderItem();
        anotherOrderItem.setSupplierId(ANOTHER_SUPPLIER_ID);
        Parameters cashOnlyParameters = BlueParametersProvider.defaultBlueOrderParametersWithItems(anotherOrderItem);
        cashOnlyParameters.addItemService();
        cashOnlyParameters.getReportParameters().setDeliveryPartnerTypes(singletonList("SHOP"));
        cashOnlyParameters.getOrder().setItems(Collections.singleton(anotherOrderItem));
        cashOnlyParameters.setupFulfillment(new ItemInfo.Fulfilment(ANOTHER_SUPPLIER_ID, TEST_SKU, TEST_SHOP_SKU));
        cashOnlyParameters.getOrder().getItems().forEach(oi -> oi.setSupplierId(ANOTHER_SUPPLIER_ID));
        cashOnlyParameters.addShopMetaData(ANOTHER_SUPPLIER_ID, ShopSettingsHelper.getPostpayMeta());
        cashOnlyParameters.makeCashOnly();
        cashOnlyParameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        parameters.addOrder(cashOnlyParameters);

        MultiCart multiCart = orderCreateHelper.cart(parameters);

        multiCart.getBnplInfo().setSelected(true);

        var multiOrder = checkoutWithPostpayPessimization(multiCart, parameters);
        var prepaidOrder = multiOrder.getOrders()
                .stream()
                .filter(order -> order.getPaymentType() == PaymentType.PREPAID)
                .findFirst()
                .orElseThrow();

        orderPayHelper.payForOrder(prepaidOrder);

        var bnplOrderCreateRequestBody = bnplMockConfigurer.getBnplRequestBody(BnplMockConfigurer.POST_ORDER_CREATE);
        var request = checkouterAnnotationObjectMapper.readValue(bnplOrderCreateRequestBody,
                BnplOrderCreateRequestBody.class);
        var bnplItem = request.getServices()
                .stream()
                .map(BnplOrderService::getItems)
                .flatMap(Collection::stream)
                .filter(item -> !item.getItemCode().startsWith("delivery"))
                .findFirst()
                .orElseGet(() -> fail("no bnpl item found"));
        var orderItem = prepaidOrder.getItems()
                .stream()
                .findFirst()
                .orElseGet(() -> fail("No order item found"));
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(bnplItem.getItemCode()).isEqualTo("item-" + orderItem.getId());
            softly.assertThat(bnplItem.getCount()).isEqualTo(orderItem.getCount());
            softly.assertThat(bnplItem.getCategory()).isEqualTo(orderItem.getCategoryId().toString());
            softly.assertThat(bnplItem.getShopId()).isEqualTo(orderItem.getSupplierId().toString());
        });

        validateBnplServiceAmount(request);
    }

    @Test
    public void checkoutMultiCartWithDsbsOrderTest() throws Exception {
        // 1st cart
        Parameters parameters = defaultWhiteParameters();
        parameters.setShopId(FF_SHOP_ID);
        parameters.getOrder().setItems(Collections.singletonList(
                dsbsOrderItem()
                        .offer("DSBS")
                        .name("DSBS")
                        .atSupplierWarehouse(true)
                        .supplierType(SupplierType.THIRD_PARTY)
                        .build()));
        parameters.addItemService(builder -> builder.price(BigDecimal.valueOf(7)));
        parameters.setupFulfillment(new ItemInfo.Fulfilment(FF_SHOP_ID, TEST_SKU, TEST_SHOP_SKU, null, false));
        // 2nd cart
        Parameters defaultBnplParameters = defaultBnplParameters();
        defaultBnplParameters.addItemService(builder -> builder.price(BigDecimal.valueOf(7)));
        parameters.addOrder(defaultBnplParameters);

        MultiCart multiCart = orderCreateHelper.cart(parameters);

        multiCart.getBnplInfo().setSelected(true);

        multiCart.setPaymentMethod(PaymentMethod.YANDEX);
        multiCart.getCarts().forEach(order -> order.setPaymentMethod(PaymentMethod.YANDEX));

        var multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        var bnplOrder = multiOrder.getOrders()
                .stream()
                .filter(order -> order.getItems()
                        .stream()
                        .noneMatch(orderItem -> "DSBS".equals(orderItem.getOfferId())))
                .findFirst()
                .orElseThrow();
        var dsbsOrder = multiOrder.getOrders()
                .stream()
                .filter(order -> order.getItems()
                        .stream()
                        .allMatch(orderItem -> "DSBS".equals(orderItem.getOfferId())))
                .findFirst()
                .orElseThrow();
        shopService.updateMeta(WHITE_SHOP_ID, parameters.getShopMetaData().values().stream().findAny().orElseThrow());
        orderCreateHelper.setupShopsMetadata(parameters);

        orderPayHelper.payForOrders(multiOrder.getOrders());

        var bnplOrderCreateRequestBody = bnplMockConfigurer.getBnplRequestBody(BnplMockConfigurer.POST_ORDER_CREATE);
        var request = checkouterAnnotationObjectMapper.readValue(bnplOrderCreateRequestBody,
                BnplOrderCreateRequestBody.class);
        var orderItem = bnplOrder.getItems()
                .stream()
                .findFirst()
                .orElseGet(() -> fail("No order item found"));
        var itemService = orderItem.getServices()
                .stream()
                .findFirst()
                .orElseGet(() -> fail("No itemService found"));
        var dsbsItem = dsbsOrder.getItems()
                .stream()
                .findFirst()
                .orElseGet(() -> fail("No order item found"));
        var dsbsItemService = dsbsItem.getServices()
                .stream()
                .findFirst()
                .orElseGet(() -> fail("No itemService found"));
        SoftAssertions.assertSoftly(softly -> {
            validateBnplItems(softly,
                    request,
                    bnplOrderService -> bnplOrderService.getType() == BnplServiceType.LOAN,
                    4,
                    bnplOrder,
                    orderItem,
                    itemService,
                    dsbsItemService);
            validateBnplItems(softly,
                    request,
                    bnplOrderService -> bnplOrderService.getType() == BnplServiceType.PAYMENT,
                    2,
                    dsbsOrder,
                    dsbsItem,
                    null,
                    null);
        });

        validateBnplServiceAmount(request);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private void validateBnplItems(
            SoftAssertions softly,
            BnplOrderCreateRequestBody request,
            Predicate<BnplOrderService> bnplOrderServicePredicate,
            int expectedSize,
            Order order,
            OrderItem orderItem,
            ItemService itemService1,
            ItemService itemService2) {
        List<BnplItem> bnplItems = request.getServices()
                .stream()
                .filter(bnplOrderServicePredicate)
                .map(BnplOrderService::getItems)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        softly.assertThat(bnplItems).hasSize(expectedSize);
        bnplItems.forEach(bnplItem -> {
            if (bnplItem.getItemCode().startsWith(BNPL_ITEM_ITEM_CODE_PREFIX)) {
                softly.assertThat(bnplItem.getItemCode()).isEqualTo("item-" + orderItem.getId());
                softly.assertThat(bnplItem.getCount()).isEqualTo(orderItem.getCount());
                softly.assertThat(bnplItem.getCategory()).isEqualTo(orderItem.getCategoryId().toString());
                softly.assertThat(bnplItem.getShopId()).isEqualTo(orderItem.getSupplierId().toString());
            } else if (bnplItem.getItemCode().startsWith(BNPL_ITEM_DELIVERY_ITEM_CODE_PREFIX)) {
                softly.assertThat(bnplItem.getItemCode()).isEqualTo("delivery-" + order.getId());
            } else if (itemService1 != null &&
                    bnplItem.getItemCode().equals(BNPL_ITEM_ITEM_SERVICE_CODE_PREFIX + itemService1.getId())) {
                softly.assertThat(bnplItem.getCount()).isEqualTo(itemService1.getCount());
                softly.assertThat(bnplItem.getPrice().compareTo(itemService1.getPrice())).isEqualTo(0);
            } else if (itemService2 != null &&
                    bnplItem.getItemCode().equals(BNPL_ITEM_ITEM_SERVICE_CODE_PREFIX + itemService2.getId())) {
                softly.assertThat(bnplItem.getCount()).isEqualTo(itemService2.getCount());
                softly.assertThat(bnplItem.getPrice().compareTo(itemService2.getPrice())).isEqualTo(0);
            }
        });
    }

    @Test
    void restrictPayForAlreadyPaid() {
        var order = orderCreateHelper.createOrder(defaultBnplParameters());
        orderPayHelper.payForOrder(order);
        assertThrows(ErrorCodeException.class,
                () -> client.payments().payOrder(order.getId(), order.getBuyer().getUid(), "", null, false, null));
    }

    @Test
    void restrictPayForAlreadyPaidInProgress() {
        var order = orderCreateHelper.createOrder(defaultBnplParameters());

        Payment payment1 = orderPayHelper.payForOrderWithoutNotification(order);
        Payment payment2 = orderPayHelper.payForOrderWithoutNotification(order);

        queuedCallService.executeQueuedCallBatch(CheckouterQCType.CREATE_VIRTUAL_PAYMENT);

        orderPayHelper.notifyPayment(payment1);
        orderPayHelper.notifyPayment(payment2);

        assertThrows(ErrorCodeException.class,
                () -> client.payments().payOrder(order.getId(), order.getBuyer().getUid(), "", null, false, null));
    }

    @Test
    void restrictPayForCancelled() {
        var order = orderCreateHelper.createOrder(defaultBnplParameters());
        orderStatusHelper.proceedOrderFromUnpaidToCancelled(order);
        assertThrows(ErrorCodeException.class,
                () -> client.payments().payOrder(order.getId(), order.getBuyer().getUid(), "", null, false, null));
    }

    @Test
    void shouldSavePaymentSubtype() {
        checkouterProperties.setEnableInstallments(true);
        Order order = orderCreateHelper.createOrder(defaultBnplParameters());
        order = orderService.getOrder(order.getId());
        assertThat(order.getPaymentSubmethod()).isEqualTo(PaymentSubmethod.BNPL);
    }

    @Test
    void bnplSelectedShouldNotAffectPaymentSubTypeIfAvailableFalse() throws Exception {
        checkouterProperties.setEnableInstallments(true);
        Parameters parameters = BlueParametersProvider.bluePrepaidWithCustomPrice(BigDecimal.valueOf(5000));
        parameters.setShowInstallments(true);
        parameters.getItems().forEach(item -> item.setCategoryId(90864));
        parameters.addItemService();
        parameters.getOrder().getDelivery().setPrice(BigDecimal.ONE);
        parameters.getOrder().getDelivery().setBuyerPrice(BigDecimal.ONE);
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        parameters.getBuiltMultiCart().setBnplInfo(new BnplInfo());
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(true);

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        var order = multiCart.getCarts().stream().findFirst().orElseThrow();
        assertTrue(order.isBnpl());
        assertTrue(multiCart.getBnplInfo().isSelected());
        assertTrue(multiCart.getBnplInfo().isAvailable());

        parameters.setPaymentMethod(PaymentMethod.TINKOFF_INSTALLMENTS);
        parameters.getBuiltMultiCart().setInstallmentsInfo(
                new InstallmentsInfo(emptyList(), new InstallmentsOption("6", null))
        );
        multiCart.setPaymentMethod(PaymentMethod.TINKOFF_INSTALLMENTS);

        var multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        order = multiOrder.getOrders().stream().findFirst().orElseThrow();
        assertFalse(order.isBnpl());
        assertTrue(multiOrder.getBnplInfo().isSelected());
        assertFalse(multiOrder.getBnplInfo().isAvailable());

        orderPayHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());
        Payment payment = paymentService.getPayment(order.getPaymentId(), ClientInfo.SYSTEM);
        assertEquals(PaymentGoal.TINKOFF_CREDIT, order.getPayment().getType());
        assertEquals(TINKOFF_INSTALLMENTS_6, order.getPaymentSubmethod());
        assertEquals(TINKOFF_INSTALLMENTS_6, payment.getProperties().getPaymentSubmethod());
        assertNotNull(order.getPayment().getBasketKey().getBasketId());
    }

    @ParameterizedTest
    @CsvSource({
            "true,271,PREPAID", //items + services + delivery = 50 * 3 + 7 * 3 + 100 = 271
            "false,250,POSTPAID" //items + delivery = 50 * 3 + 100 = 250
    })
    public void createBnplPaymentWithItemService(boolean servicesPrepayEnabled,
                                                 BigDecimal expectedPrepayAmount,
                                                 PaymentType itemServicePaymentType) throws IOException {
        checkouterProperties.setEnableServicesPrepay(servicesPrepayEnabled);
        checkouterFeatureWriter.writeValue(BooleanFeatureType.SINGLE_SERVICE_PER_MULTIPLE_ORDER_ITEMS, false);

        Parameters parameters = defaultBnplParameters(
                createOrderItem(0, "bnpl-1", 50.00, 3, SupplierType.FIRST_PARTY, 90864)
        );
        parameters.addItemService(builder -> builder.price(BigDecimal.valueOf(7)));

        Order order = orderCreateHelper.createOrder(parameters);
        CreatePaymentResponse paymentResponse = orderPayHelper.payWithRealResponse(order);
        assertTrue(paymentResponse.getBnpl());
        assertEquals("http://bnpl.yandex.ru", paymentResponse.getPaymentUrl());

        var bnplOrderCreateRequestBody = bnplMockConfigurer.getBnplRequestBody(BnplMockConfigurer.POST_ORDER_CREATE);
        var request = checkouterAnnotationObjectMapper.readValue(bnplOrderCreateRequestBody,
                BnplOrderCreateRequestBody.class);
        var orderItem = order.getItems()
                .stream()
                .findFirst()
                .orElseGet(() -> fail("No order item found"));
        ItemService itemService = orderItem.getServices()
                .stream()
                .findFirst()
                .orElseGet(() -> fail("No itemService found"));
        List<BnplItem> bnplItems = request.getServices()
                .stream()
                .map(BnplOrderService::getItems)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        SoftAssertions.assertSoftly(softly -> {
            BigDecimal actualPrepayAmount = request.getServices()
                    .stream()
                    .map(BnplOrderService::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            softly.assertThat(actualPrepayAmount.compareTo(expectedPrepayAmount)).isEqualTo(0);
            softly.assertThat(itemService.getPaymentType()).isEqualTo(itemServicePaymentType);
            if (servicesPrepayEnabled) {
                softly.assertThat(bnplItems).anyMatch(item -> item.getItemCode().startsWith("service"));
            } else {
                softly.assertThat(bnplItems).noneMatch(item -> item.getItemCode().startsWith("service"));
            }
        });
    }

    @ParameterizedTest
    @CsvSource({
            "true,257,PREPAID", //items + services + delivery = 50 * 3 + 7 * 1 + 100 = 257
            "false,250,POSTPAID" //items + delivery = 50 * 3 + 100 = 250
    })
    public void createBnplPaymentWithItemServiceWithSingleServicePerItems(boolean servicesPrepayEnabled,
                                                 BigDecimal expectedPrepayAmount,
                                                 PaymentType itemServicePaymentType) throws IOException {
        checkouterProperties.setEnableServicesPrepay(servicesPrepayEnabled);
        checkouterFeatureWriter.writeValue(BooleanFeatureType.SINGLE_SERVICE_PER_MULTIPLE_ORDER_ITEMS, true);

        Parameters parameters = defaultBnplParameters(
                createOrderItem(0, "bnpl-1", 50.00, 3, SupplierType.FIRST_PARTY, 90864)
        );
        parameters.addItemService(builder -> builder.price(BigDecimal.valueOf(7)));

        Order order = orderCreateHelper.createOrder(parameters);
        CreatePaymentResponse paymentResponse = orderPayHelper.payWithRealResponse(order);
        assertTrue(paymentResponse.getBnpl());
        assertEquals("http://bnpl.yandex.ru", paymentResponse.getPaymentUrl());

        var bnplOrderCreateRequestBody = bnplMockConfigurer.getBnplRequestBody(BnplMockConfigurer.POST_ORDER_CREATE);
        var request = checkouterAnnotationObjectMapper.readValue(bnplOrderCreateRequestBody,
                BnplOrderCreateRequestBody.class);
        var orderItem = order.getItems()
                .stream()
                .findFirst()
                .orElseGet(() -> fail("No order item found"));
        ItemService itemService = orderItem.getServices()
                .stream()
                .findFirst()
                .orElseGet(() -> fail("No itemService found"));
        List<BnplItem> bnplItems = request.getServices()
                .stream()
                .map(BnplOrderService::getItems)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        SoftAssertions.assertSoftly(softly -> {
            BigDecimal actualPrepayAmount = request.getServices()
                    .stream()
                    .map(BnplOrderService::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            softly.assertThat(actualPrepayAmount.compareTo(expectedPrepayAmount)).isEqualTo(0);
            softly.assertThat(itemService.getPaymentType()).isEqualTo(itemServicePaymentType);
            if (servicesPrepayEnabled) {
                softly.assertThat(bnplItems).anyMatch(item -> item.getItemCode().startsWith("service"));
            } else {
                softly.assertThat(bnplItems).noneMatch(item -> item.getItemCode().startsWith("service"));
            }
        });
    }

    @Test
    public void createVBnplWithOriginPaymentId() {
        // создаем заказ
        Parameters parameters = defaultBnplParameters();
        Order order = orderCreateHelper.createOrder(parameters);

        // делаем оплату
        Payment bnplPayment = orderPayHelper.payForOrderWithoutNotification(order);

        // заказ еще в статусе UNPAID
        order = orderService.getOrder(order.getId(), ClientInfo.SYSTEM);
        assertEquals(OrderStatus.UNPAID, order.getStatus());

        // приходит HOLD по первой оплате
        orderPayHelper.notifyPayment(bnplPayment);
        bnplPayment = paymentService.getPayment(bnplPayment.getId(), ClientInfo.SYSTEM);
        assertEquals(PaymentStatus.HOLD, bnplPayment.getStatus());

        // заказ ушел в PROCESSING
        order = orderService.getOrder(order.getId(), ClientInfo.SYSTEM);
        assertEquals(OrderStatus.PROCESSING, order.getStatus());

        // создаются виртуальные оплаты, но создастся только одна
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.CREATE_VIRTUAL_PAYMENT);
        List<Payment> virtualPayments = paymentService.getPayments(
                order.getId(), ClientInfo.SYSTEM, PaymentGoal.VIRTUAL_BNPL);
        assertEquals(1, virtualPayments.size());
        Payment virtualPayment = virtualPayments.get(0);

        assertEquals(bnplPayment.getId(), virtualPayment.getProperties().getOriginPaymentId());
    }

    @Nonnull
    private Set<String> getBnplItemsByType(BnplOrderCreateRequestBody request, BnplServiceType loan) {
        return request.getServices().stream()
                .filter(bnplOrderService -> bnplOrderService.getType() == loan)
                .map(BnplOrderService::getItems)
                .flatMap(Collection::stream)
                .map(BnplItem::getItemCode)
                .collect(Collectors.toSet());
    }

    @Nonnull
    private Set<String> getBnplItemsByType(BnplPlanCheckRequestBody request, BnplServiceType loan) {
        return request.getServices().stream()
                .filter(bnplOrderService -> bnplOrderService.getType() == loan)
                .map(BnplOrderService::getItems)
                .flatMap(Collection::stream)
                .map(BnplItem::getTitle)
                .collect(Collectors.toSet());
    }

    private MultiOrder checkoutWithPostpayPessimization(MultiCart cart, Parameters parameters) throws Exception {
        cart.setPaymentMethod(null);
        cart.setPaymentType(null);
        cart.getCarts().forEach(o -> {
            PaymentMethod paymentMethod = o.getPaymentOptions().stream()
                    // Фильтрую по методу, а не по типу, чтобы в тесте не было экзотики вроде APPLE_PAY
                    .filter(po -> po == PaymentMethod.YANDEX)
                    .findAny()
                    .orElse(
                            o.getPaymentOptions().stream()
                                    .findAny()
                                    .orElseThrow(() -> new RuntimeException("No payment options"))
                    );
            o.setPaymentMethod(paymentMethod);
            o.setPaymentType(paymentMethod.getPaymentType());
        });
        return orderCreateHelper.checkout(cart, parameters);
    }

    private void validateBnplServiceAmount(BnplOrderCreateRequestBody request) {
        var itemSum = request.getServices().stream()
                .map(BnplOrderService::getItems)
                .flatMap(Collection::stream)
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getCount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var amountSum = request.getServices().stream()
                .map(BnplOrderService::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(amountSum).isEqualTo(itemSum);
    }
}
