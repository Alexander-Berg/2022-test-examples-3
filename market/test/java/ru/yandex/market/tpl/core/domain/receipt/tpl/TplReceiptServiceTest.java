package ru.yandex.market.tpl.core.domain.receipt.tpl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderChequeRemoteDto;
import ru.yandex.market.tpl.api.model.order.OrderChequeType;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.receipt.ReceiptAgentType;
import ru.yandex.market.tpl.api.model.receipt.ReceiptItemType;
import ru.yandex.market.tpl.api.model.receipt.ReceiptNotificationType;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.api.model.user.UserMode;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderCommand;
import ru.yandex.market.tpl.core.domain.order.OrderCommandService;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderItem;
import ru.yandex.market.tpl.core.domain.order.OrderItemInstance;
import ru.yandex.market.tpl.core.domain.order.OrderItemInstancePurchaseStatus;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.VendorArticle;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptData;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptDataItem;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptDataRepository;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptFfd12KktResolver;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptServiceClientRepository;
import ru.yandex.market.tpl.core.domain.receipt.lifepay.LifePayService;
import ru.yandex.market.tpl.core.domain.receipt.queue.alert.ReceiptFiscalizeAlertTicketService;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.events.OrderDeliveryTaskChequePrintedEvent;
import ru.yandex.market.tpl.core.external.lifepay.LifePayClient;
import ru.yandex.market.tpl.core.external.lifepay.request.LifePayCreateReceiptRequestFfd12;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static ru.yandex.market.tpl.api.model.receipt.ReceiptItemType.PRODUCT_WITHOUT_MARKING;
import static ru.yandex.market.tpl.api.model.receipt.ReceiptItemType.PRODUCT_WITH_MARKING;
import static ru.yandex.market.tpl.api.model.receipt.ReceiptItemType.SERVICE;
import static ru.yandex.market.tpl.api.model.user.UserMode.SOFT_MODE;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.ORDERS_FOR_FFD_1_2_TEST_LIST;
import static ru.yandex.market.tpl.core.domain.order.OrderGenerateService.DEFAULT_EMAIL_PERSONAL_ID;
import static ru.yandex.market.tpl.core.domain.order.OrderGenerateService.DEFAULT_PHONE_PERSONAL_ID;
import static ru.yandex.market.tpl.core.domain.receipt.tpl.TplReceiptService.YANDEX_CUSTOMER_SUPPORT_PHONE;

/**
 * @author valter
 */
@RequiredArgsConstructor
class TplReceiptServiceTest extends TplAbstractTest {

    private static final long TEST_DELIVERY_SERVICE_1 = 240;
    private static final long TEST_DELIVERY_SERVICE_2 = 241;
    private static final String KKT_1 = "00106100670746";
    private static final String KKT_3 = "00106126251211";
    private static final String KKT_2 = "00106100674492";

    private final ApplicationEventPublisher eventPublisher;
    private final ReceiptServiceClientRepository receiptServiceClientRepository;
    private final ReceiptDataRepository receiptDataRepository;
    private final LifePayService lifePayService;
    private final TestUserHelper testUserHelper;
    private final OrderGenerateService orderGenerateService;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final OrderRepository orderRepository;
    private final OrderCommandService orderCommandService;
    private final UserPropertyService userPropertyService;
    private final ConfigurationProviderAdapter configurationProviderAdapter;
    private final TransactionTemplate transactionTemplate;
    private final LifePayClient lifePayClient;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final ReceiptFiscalizeAlertTicketService receiptFiscalizeAlertTicketService;
    private final ReceiptFfd12KktResolver receiptFfd12KktResolver;


    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = testUserHelper.findOrCreateUser(1L);
        user2 = testUserHelper.findOrCreateUser(2L);

        jdbcTemplate.update("UPDATE partner SET kkt = ? WHERE id = ?",
                new String[]{KKT_1, KKT_2}, TEST_DELIVERY_SERVICE_1);
        jdbcTemplate.update("UPDATE partner SET kkt = ? WHERE id = ?",
                new String[]{KKT_3}, TEST_DELIVERY_SERVICE_2);

    }

    @AfterEach
    void cleanUp() {
        Mockito.reset(lifePayClient, configurationProviderAdapter);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void createReceiptLifePayKktDeterminedByDsAndUser(boolean isFfd12) {
        var event = event(user1, OrderPaymentType.CARD, TEST_DELIVERY_SERVICE_1);
        setupOrderAsFfd12Test(event, isFfd12);
        createReceiptLifePayKktDetermined(user1, user1.getId() % 2 == 0 ? KKT_1 : KKT_2, event);
        event = event(user2, OrderPaymentType.CARD, TEST_DELIVERY_SERVICE_1);
        setupOrderAsFfd12Test(event, isFfd12);
        createReceiptLifePayKktDetermined(user2, user2.getId() % 2 == 0 ? KKT_1 : KKT_2, event);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void createReceiptLifePayKktDeterminedByDs(boolean isFfd12) {
        var event = event(user1, OrderPaymentType.CARD, TEST_DELIVERY_SERVICE_2);
        setupOrderAsFfd12Test(event, isFfd12);
        createReceiptLifePayKktDetermined(user1, KKT_3, event);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void createReceiptLifePayKktDeterminedByDs2(boolean isFfd12) {
        var event = event(user2, OrderPaymentType.CARD, TEST_DELIVERY_SERVICE_2);
        setupOrderAsFfd12Test(event, isFfd12);
        createReceiptLifePayKktDetermined(user2, KKT_3, event);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void createReceiptLifePayKktDeterminedByDsAndUserWithCisValuesCorrectSize(boolean isFfd12) {
        var event = event(user1, OrderPaymentType.CARD, TEST_DELIVERY_SERVICE_1);
        setupOrderAsFfd12Test(event, isFfd12);
        OrderItem orderItem = transactionTemplate.execute(
                ts -> orderRepository.findByIdOrThrow(event.getTask().getOrderId()).getItems().get(0)
        );

        addCisValuesToOrderItem(orderItem.getOrder(), orderItem, orderItem.getCount());

        createReceiptLifePayKktDetermined(user1, user1.getId() % 2 == 0 ? KKT_1 : KKT_2, event);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void createAlertTicketTitle(boolean isFfd12) {
        var event = event(user1, OrderPaymentType.CARD, TEST_DELIVERY_SERVICE_1);
        setupOrderAsFfd12Test(event, isFfd12);
        createReceiptLifePayKktDetermined(user1, user1.getId() % 2 == 0 ? KKT_1 : KKT_2, event);

        ReceiptData receiptData = receiptDataRepository.findAll().stream().findFirst().orElseThrow();
        String title = receiptFiscalizeAlertTicketService.createTitleForTicketWithoutCis(receiptData);
        assertThat(title).contains("Количество кизов не совпадает с количеством единиц товара для ReceiptData");
        assertThat(title).contains(receiptData.getId().toString());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void createAlertTicketBody(boolean isFfd12) {
        var event = event(user1, OrderPaymentType.CARD, TEST_DELIVERY_SERVICE_1);
        setupOrderAsFfd12Test(event, isFfd12);
        OrderItem orderItem = transactionTemplate.execute(
                ts -> orderRepository.findByIdOrThrow(event.getTask().getOrderId()).getItems().get(0)
        );
        addCisValuesToOrderItem(orderItem.getOrder(), orderItem, orderItem.getCount());
        createReceiptLifePayKktDetermined(user1, user1.getId() % 2 == 0 ? KKT_1 : KKT_2, event);

        var receiptData = receiptDataRepository.findAll().stream().findFirst().orElseThrow();
        var receiptDataItem =
                receiptData.getItems().stream().filter(i -> i.getName().equals(orderItem.getName())).findFirst().orElseThrow();

        String body = receiptFiscalizeAlertTicketService.createBodyForTicketWithoutCis(
                receiptDataItem
        );
        assertThat(body).contains(receiptDataItem.getId().toString());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void createReceiptLifePayKktDeterminedByDsAndUserWithCisValuesUncorrectSize(boolean isFfd12) {
        var event = event(user1, OrderPaymentType.CARD, TEST_DELIVERY_SERVICE_1);
        setupOrderAsFfd12Test(event, isFfd12);

        OrderItem orderItem = transactionTemplate.execute(
                ts -> orderRepository.findByIdOrThrow(event.getTask().getOrderId()).getItems().get(0)
        );

        addCisValuesToOrderItem(orderItem.getOrder(), orderItem, orderItem.getCount() - 1);

        createReceiptLifePayKktDetermined(user1, user1.getId() % 2 == 0 ? KKT_1 : KKT_2, event);

        dbQueueTestUtil.assertQueueHasSize(QueueType.RECEIPT_FISCALIZE_ALERT, 1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void chequesForPrepaidOrdersDoNotFiscalize(boolean isFfd12) {
        var user = testUserHelper.findOrCreateUser(1L);
        var event = event(user, OrderPaymentType.PREPAID, TEST_DELIVERY_SERVICE_1);
        setupOrderAsFfd12Test(event, isFfd12);
        eventPublisher.publishEvent(event);
        assertThat(receiptDataRepository.findAll()).isEmpty();
        verifyNoInteractions(lifePayClient);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void createReceiptWithoutReceiptAgentTypeForDelivery(boolean isFfd12) {
        var event = event(user1, OrderPaymentType.CARD, TEST_DELIVERY_SERVICE_1, new BigDecimal(50));
        setupOrderAsFfd12Test(event, isFfd12);

        eventPublisher.publishEvent(event);
        ReceiptData receiptData = receiptDataRepository.findByReceiptIdAndServiceClient(
                String.format("%d-SELL-0", event.getTask().getOrderId()),
                receiptServiceClientRepository.findByIdOrThrow("tpl")).orElseThrow();


        ReceiptDataItem receiptDataItem = StreamEx.of(receiptData.getItems())
                .filter(this::isProduct)
                .findFirst()
                .orElseThrow();
        assertThat(receiptDataItem.getSupplierInn()).isEqualTo("7705432475");
        assertThat(receiptDataItem.getAgentItemType()).isEqualTo(ReceiptAgentType.AGENT);

        ReceiptDataItem deliveryReceiptDataItem = StreamEx.of(receiptData.getItems())
                .filter(i -> i.getType() == SERVICE)
                .findFirst()
                .orElseThrow();
        assertThat(deliveryReceiptDataItem.getSupplierInn()).isNull();
        assertThat(deliveryReceiptDataItem.getAgentItemType()).isNull();
    }

    @Test
    void createReceiptWithAgentTypeWithDefaultPhoneNumber() {
        var event = event(user1, OrderPaymentType.CARD, TEST_DELIVERY_SERVICE_1, new BigDecimal(50));
        setupOrderAsFfd12Test(event, true);

        eventPublisher.publishEvent(event);
        ReceiptData receiptData = receiptDataRepository.findByReceiptIdAndServiceClient(
                String.format("%d-SELL-0", event.getTask().getOrderId()),
                receiptServiceClientRepository.findByIdOrThrow("tpl")).orElseThrow();

        ReceiptDataItem receiptDataItem = StreamEx.of(receiptData.getItems())
                .filter(this::isProduct)
                .findFirst()
                .orElseThrow();
        assertThat(receiptDataItem.getSupplierInn()).isEqualTo("7705432475");
        assertThat(receiptDataItem.getAgentItemType()).isEqualTo(ReceiptAgentType.AGENT);
        assertThat(receiptDataItem.getSupplierPhoneNumber()).asList().containsExactly(YANDEX_CUSTOMER_SUPPORT_PHONE);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void createReceiptWithoutReceiptAgentTypeFor1p(boolean isFfd12) {
        var order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentType(OrderPaymentType.CARD)
                        .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                                .supplierInn(OrderItem.YANDEX_INN)
                                .itemsPrice(BigDecimal.TEN)
                                .itemsCount(1)
                                .build())
                        .externalOrderId(String.valueOf(user1.getId()))
                        .flowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                        .deliveryServiceId(TEST_DELIVERY_SERVICE_1)
                        .deliveryPrice(BigDecimal.ZERO)
                        .build()
        );
        var event = event(user1, order);
        setupOrderAsFfd12Test(event, isFfd12);

        eventPublisher.publishEvent(event);
        ReceiptData receiptData = receiptDataRepository.findByReceiptIdAndServiceClient(
                String.format("%d-SELL-0", event.getTask().getOrderId()),
                receiptServiceClientRepository.findByIdOrThrow("tpl")).orElseThrow();

        ReceiptDataItem receiptDataItem = StreamEx.of(receiptData.getItems())
                .filter(this::isProduct)
                .findFirst()
                .orElseThrow();
        assertThat(receiptDataItem.getSupplierInn()).isNull();
        assertThat(receiptDataItem.getAgentItemType()).isNull();
        assertThat(receiptDataItem.getSupplierPhoneNumber()).isNull();
        assertThat(receiptDataItem.getSupplierName()).isNull();
    }

    @Test
    void createReceiptWithReceiptAgentType() {
        var order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentType(OrderPaymentType.CARD)
                        .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                                .supplierInn("123456789")
                                .supplierPhoneNumber("+70001234567")
                                .supplierName("ООО Поставщик")
                                .itemsPrice(BigDecimal.TEN)
                                .itemsCount(1)
                                .build())
                        .externalOrderId(String.valueOf(user1.getId()))
                        .flowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                        .deliveryServiceId(TEST_DELIVERY_SERVICE_1)
                        .deliveryPrice(BigDecimal.ZERO)
                        .build()
        );
        var event = event(user1, order);
        setupOrderAsFfd12Test(event, true);

        eventPublisher.publishEvent(event);
        ReceiptData receiptData = receiptDataRepository.findByReceiptIdAndServiceClient(
                String.format("%d-SELL-0", event.getTask().getOrderId()),
                receiptServiceClientRepository.findByIdOrThrow("tpl")).orElseThrow();

        ReceiptDataItem receiptDataItem = StreamEx.of(receiptData.getItems())
                .filter(this::isProduct)
                .findFirst()
                .orElseThrow();
        assertThat(receiptDataItem.getSupplierInn()).isEqualTo("123456789");
        assertThat(receiptDataItem.getAgentItemType()).isEqualTo(ReceiptAgentType.AGENT);
        assertThat(receiptDataItem.getSupplierPhoneNumber()).asList().containsExactly("+70001234567");
        assertThat(receiptDataItem.getSupplierName()).isEqualTo("ООО Поставщик");
    }

    @ParameterizedTest
    @ValueSource(strings = {YANDEX_CUSTOMER_SUPPORT_PHONE, "+70001234567"})
    void createReceiptWithReceiptAgentTypeWithoutName(String supplierPhoneNumber) {
        var order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentType(OrderPaymentType.CARD)
                        .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                                .supplierInn("123456789")
                                .supplierPhoneNumber(
                                        supplierPhoneNumber.equals(YANDEX_CUSTOMER_SUPPORT_PHONE)
                                                ? null
                                                : supplierPhoneNumber
                                )
                                .itemsPrice(BigDecimal.TEN)
                                .itemsCount(1)
                                .build())
                        .externalOrderId(String.valueOf(user1.getId()))
                        .flowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                        .deliveryServiceId(TEST_DELIVERY_SERVICE_1)
                        .deliveryPrice(BigDecimal.ZERO)
                        .build()
        );
        var event = event(user1, order);
        setupOrderAsFfd12Test(event, true);

        eventPublisher.publishEvent(event);
        ReceiptData receiptData = receiptDataRepository.findByReceiptIdAndServiceClient(
                String.format("%d-SELL-0", event.getTask().getOrderId()),
                receiptServiceClientRepository.findByIdOrThrow("tpl")).orElseThrow();

        ReceiptDataItem receiptDataItem = StreamEx.of(receiptData.getItems())
                .filter(this::isProduct)
                .findFirst()
                .orElseThrow();
        assertThat(receiptDataItem.getSupplierInn()).isEqualTo("123456789");
        assertThat(receiptDataItem.getAgentItemType()).isEqualTo(ReceiptAgentType.AGENT);
        assertThat(receiptDataItem.getSupplierPhoneNumber()).asList().containsExactly(supplierPhoneNumber);
        assertThat(receiptDataItem.getSupplierName()).isNull();
        doReturn("3498234-" + user1.getId()).when(lifePayClient).createReceiptFfd12(any());
        assertThat(lifePayService.registerCheque(receiptData)).isTrue();
        verify(lifePayClient).createReceiptFfd12(argThat(r -> r.getPurchase().getProducts().stream()
                .filter(product -> product.getAgentItemType().getAgent() == 1)
                .anyMatch(product ->
                        Objects.requireNonNull(product.getSupplierData())
                                .getSupplierPhoneNumber().get(0)
                                .equals(supplierPhoneNumber))
        ));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void createReceiptCis(boolean hasCis) {
        var order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentType(OrderPaymentType.CARD)
                        .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                                .itemsPrice(BigDecimal.TEN)
                                .itemsCount(1)
                                .build())
                        .externalOrderId(String.valueOf(user1.getId()))
                        .flowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                        .deliveryServiceId(TEST_DELIVERY_SERVICE_1)
                        .deliveryPrice(BigDecimal.ZERO)
                        .withItemInstance(hasCis)
                        .build()
        );
        var event = event(user1, order);
        setupOrderAsFfd12Test(event, true);

        eventPublisher.publishEvent(event);
        ReceiptData receiptData = receiptDataRepository.findByReceiptIdAndServiceClient(
                String.format("%d-SELL-0", event.getTask().getOrderId()),
                receiptServiceClientRepository.findByIdOrThrow("tpl")).orElseThrow();
        doReturn("3498234-" + user1.getId()).when(lifePayClient).createReceiptFfd12(any());
        assertThat(lifePayService.registerCheque(receiptData)).isTrue();
        verify(lifePayClient).createReceiptFfd12(argThat(r -> r.getPurchase().getProducts().stream()
                .allMatch(product -> checkProduct(product, hasCis))
        ));
    }

    private boolean checkProduct(LifePayCreateReceiptRequestFfd12.Product product, boolean hasCis) {
        if (product.getItemType().equals(SERVICE.getLifePayId())) {
            return product.getMarkingCode() == null && product.getMarkingCodeStatus() == null;
        } else {
            if (hasCis) {
                assertThat(product.getMarkingCode()).isNotNull().containsPattern("\u001D");
                return Objects.requireNonNull(product.getItemType()).equals(PRODUCT_WITH_MARKING.getLifePayId())
                        && product.getMarkingCode() != null
                        && Objects.requireNonNull(product.getMarkingCodeStatus()).equals(1);
            } else {
                return Objects.requireNonNull(product.getItemType()).equals(PRODUCT_WITHOUT_MARKING.getLifePayId())
                        && product.getMarkingCode() == null
                        && product.getMarkingCodeStatus() == null;
            }
        }
    }

    private void setupOrderAsFfd12Test(OrderDeliveryTaskChequePrintedEvent event, boolean isFfd12) {
        if (isFfd12) {
            var orderId = Objects.requireNonNull(event.getTask().getOrderId());
            doReturn(Set.of(orderId)).when(configurationProviderAdapter).getValueAsLongs(ORDERS_FOR_FFD_1_2_TEST_LIST);
        }
    }

    private void createReceiptLifePayKktDetermined(User user, String kktSn,
                                                   OrderDeliveryTaskChequePrintedEvent event) {
        eventPublisher.publishEvent(event);
        ReceiptData receiptData = receiptDataRepository.findByReceiptIdAndServiceClient(
                String.format("%d-SELL-0", event.getTask().getOrderId()),
                receiptServiceClientRepository.findByIdOrThrow("tpl")).orElseThrow();
        ReceiptDataItem receiptDataItem = receiptData.getItems().get(0);
        assertThat(receiptData.getNotifications()).hasSize(1);
        assertThat(receiptData.getNotifications().get(0).getPhonePersonalId()).isEqualTo(DEFAULT_PHONE_PERSONAL_ID);
        if (receiptData.getNotifications().get(0).getType().equals(ReceiptNotificationType.EMAIL)) {
            assertThat(receiptData.getNotifications().get(0).getEmailPersonalId()).isEqualTo(DEFAULT_EMAIL_PERSONAL_ID);
        }
        assertThat(receiptDataItem.getSupplierInn()).isEqualTo("7705432475");
        assertThat(receiptDataItem.getAgentItemType()).isEqualTo(ReceiptAgentType.AGENT);
        doReturn("3498234-" + user.getId()).when(lifePayClient).createReceipt(any());
        doReturn("3498234-" + user.getId()).when(lifePayClient).createReceiptFfd12(any());
        assertThat(lifePayService.registerCheque(receiptData)).isTrue();
        if (receiptFfd12KktResolver.isFfd12Receipt(receiptData)) {
            verify(lifePayClient).createReceiptFfd12(argThat(r -> kktSn.equals(r.getTargetSerial())));
        } else {
            verify(lifePayClient).createReceipt(argThat(r -> kktSn.equals(r.getTargetSerial())));
        }
    }

    private OrderDeliveryTaskChequePrintedEvent event(
            User user,
            OrderPaymentType orderPaymentType,
            long deliveryServiceId
    ) {
        return event(user, orderPaymentType, deliveryServiceId, BigDecimal.ZERO);
    }

    private OrderDeliveryTaskChequePrintedEvent event(
            User user,
            OrderPaymentType orderPaymentType,
            long deliveryServiceId,
            BigDecimal deliveryPrice
    ) {
        var order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentType(orderPaymentType)
                        .externalOrderId(String.valueOf(user.getId()))
                        .flowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                        .deliveryServiceId(deliveryServiceId)
                        .deliveryPrice(deliveryPrice)
                        .items(OrderGenerateService.OrderGenerateParam.Items.builder().itemsItemCount(2).build())
                        .build()
        );

        var userShift = testUserHelper.createShiftWithDeliveryTask(user, UserShiftStatus.ON_TASK, order);
        RoutePoint routePoint = userShift.streamDeliveryRoutePoints().findFirst().orElseThrow();
        OrderDeliveryTask orderDeliveryTask = routePoint.streamTasks(OrderDeliveryTask.class).findFirst().orElseThrow();

        return new OrderDeliveryTaskChequePrintedEvent(
                orderDeliveryTask,
                new UserShiftCommand.PrintOrReturnCheque(
                        userShift.getId(),
                        routePoint.getId(),
                        orderDeliveryTask.getId(),
                        new OrderChequeRemoteDto(orderPaymentType, OrderChequeType.SELL),
                        Instant.now(clock),
                        SOFT_MODE.equals(UserMode.valueOf(
                                userPropertyService.findPropertyForUser(UserProperties.USER_MODE, user))),
                        null,
                        Optional.empty()
                ),
                false
        );
    }

    private OrderDeliveryTaskChequePrintedEvent event(
            User user,
            Order order
    ) {
        var userShift = testUserHelper.createShiftWithDeliveryTask(user, UserShiftStatus.ON_TASK, order);
        RoutePoint routePoint = userShift.streamDeliveryRoutePoints().findFirst().orElseThrow();
        OrderDeliveryTask orderDeliveryTask = routePoint.streamTasks(OrderDeliveryTask.class).findFirst().orElseThrow();

        return new OrderDeliveryTaskChequePrintedEvent(
                orderDeliveryTask,
                new UserShiftCommand.PrintOrReturnCheque(
                        userShift.getId(),
                        routePoint.getId(),
                        orderDeliveryTask.getId(),
                        new OrderChequeRemoteDto(order.getPaymentType(), OrderChequeType.SELL),
                        Instant.now(clock),
                        SOFT_MODE.equals(UserMode.valueOf(
                                userPropertyService.findPropertyForUser(UserProperties.USER_MODE, user))),
                        null,
                        Optional.empty()
                ),
                false
        );
    }

    private void addCisValuesToOrderItem(Order order, OrderItem orderItem, int cisValuesSize) {
        orderCommandService.updateItemsInstances(new OrderCommand.UpdateItemsInstances(
                order.getId(),
                Map.of(
                        new VendorArticle(orderItem.getVendorArticle().getVendorId(),
                                orderItem.getVendorArticle().getArticle()),
                        IntStream.range(0, cisValuesSize)
                                .boxed().map(i ->
                                        OrderItemInstance.builder()
                                                .purchaseStatus(OrderItemInstancePurchaseStatus.PURCHASED)
                                                .cis("cis" + i)
                                                .cisFull("cisFull\u001D" + i)
                                                .build()
                                )
                                .collect(Collectors.toList())))
        );
    }

    private boolean isProduct(ReceiptDataItem item) {
        var type = item.getType();
        return type == ReceiptItemType.PRODUCT ||
                type == PRODUCT_WITH_MARKING ||
                type == PRODUCT_WITHOUT_MARKING;
    }
}
