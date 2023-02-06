package ru.yandex.market.global.checkout.domain.promo;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.actualize.OrderActualization;
import ru.yandex.market.global.checkout.domain.order.OrderModel;
import ru.yandex.market.global.checkout.domain.promo.apply.first_order_discount.FirstOrderDiscountArgs;
import ru.yandex.market.global.checkout.domain.promo.apply.first_order_discount.FirstOrderDiscountCommonState;
import ru.yandex.market.global.checkout.domain.promo.apply.first_order_discount.FirstOrderDiscountPromoApplyHandler;
import ru.yandex.market.global.checkout.domain.promo.model.PromoType;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.factory.TestOrderFactory.CreateOrderBuilder;
import ru.yandex.market.global.checkout.factory.TestPromoFactory;
import ru.yandex.market.global.checkout.util.RandomDataGenerator;
import ru.yandex.market.global.db.jooq.enums.EOrderState;
import ru.yandex.market.global.db.jooq.enums.EPromoAccessType;
import ru.yandex.market.global.db.jooq.enums.EPromoApplicationType;
import ru.yandex.market.global.db.jooq.tables.pojos.OrderItem;
import ru.yandex.market.global.db.jooq.tables.pojos.Promo;
import ru.yandex.market.global.db.jooq.tables.pojos.PromoUser;

import static ru.yandex.market.global.checkout.factory.TestOrderFactory.CreateOrderActualizationBuilder;
import static ru.yandex.market.global.checkout.factory.TestOrderFactory.buildOrderActualization;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FirstOrderDiscountTest extends BaseFunctionalTest {
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(
            FirstOrderDiscountTest.class
    ).build();
    private static final long MIN_ITEMS_PRICE = 100_00L;
    private static final long DISCOUNT = 30_00L;
    private static final long USER_UID = 2L;

    private static final long FRAUDER_UID_1 = 3L;
    private static final long FRAUDER_UID_2 = 4L;

    private final FirstOrderDiscountPromoApplyHandler firstOrderDiscountPromoApplyHandler;

    private final Clock clock;
    private final TestOrderFactory testOrderFactory;
    private final TestPromoFactory testPromoFactory;

    @Test
    public void testDiscountAvailableForFirstOrder() {
        Promo promo = createFirstOrderDiscount();
        OrderActualization actualization = createOrderActualization(USER_UID, MIN_ITEMS_PRICE);
        PromoUser promoUser =  createPromoUser(actualization.getOrder().getUid());

        firstOrderDiscountPromoApplyHandler.apply(promo, promoUser, actualization);

        Assertions.assertThat(actualization.getOrderItems().get(0).getTotalCost())
                .isEqualTo(actualization.getOrderItems().get(0).getTotalCostWithoutPromo() - DISCOUNT);
    }

    @Test
    public void testDiscountNotAvailableForSmallOrder() {
        Promo promo = createFirstOrderDiscount();
        OrderActualization actualization = createOrderActualization(USER_UID, MIN_ITEMS_PRICE - 1);
        PromoUser promoUser = createPromoUser(actualization.getOrder().getUid());

        firstOrderDiscountPromoApplyHandler.apply(promo, promoUser, actualization);

        Assertions.assertThat(actualization.getOrderItems().get(0).getTotalCost())
                .isEqualTo(actualization.getOrderItems().get(0).getTotalCostWithoutPromo());
    }

    @Test
    public void testDiscountNotAvailableForSecondOrderByUid() {
        Promo promo = createFirstOrderDiscount();

        OrderModel model = createFinishedOrder(USER_UID);
        OrderActualization actualization = createOrderActualization(USER_UID, MIN_ITEMS_PRICE);

        PromoUser promoUser = createPromoUser(actualization.getOrder().getUid());

        firstOrderDiscountPromoApplyHandler.apply(promo, promoUser, actualization);

        Assertions.assertThat(actualization.getOrderItems().get(0).getTotalCost())
                .isEqualTo(actualization.getOrderItems().get(0).getTotalCostWithoutPromo());
    }

    @Test
    public void testDiscountNotAvailableForSecondOrderByPhone() {
        Promo promo = createFirstOrderDiscount();

        createFinishedOrder(
                FRAUDER_UID_1, "+12345", "CARD_123", "TAXI_USER_ID_123", "DEVICE_123"
        );
        OrderActualization actualization = createOrderActualization(
                FRAUDER_UID_2, "+12345", "CARD_321", "TAXI_USER_ID_321", "DEVICE_321", MIN_ITEMS_PRICE
        );

        PromoUser promoUser = createPromoUser(actualization.getOrder().getUid());

        firstOrderDiscountPromoApplyHandler.apply(promo, promoUser, actualization);

        Assertions.assertThat(actualization.getOrderItems().get(0).getTotalCost())
                .isEqualTo(actualization.getOrderItems().get(0).getTotalCostWithoutPromo());
    }

    @Test
    public void testDiscountNotAvailableForSecondOrderByPaymentMethod() {
        Promo promo = createFirstOrderDiscount();

        createFinishedOrder(
                FRAUDER_UID_1, "+12345", "CARD_123", "TAXI_USER_ID_123", "DEVICE_123"
        );
        OrderActualization actualization = createOrderActualization(
                FRAUDER_UID_2, "+54321", "CARD_123", "TAXI_USER_ID_321", "DEVICE_321", MIN_ITEMS_PRICE
        );

        PromoUser promoUser = createPromoUser(actualization.getOrder().getUid());

        firstOrderDiscountPromoApplyHandler.apply(promo, promoUser, actualization);

        Assertions.assertThat(actualization.getOrderItems().get(0).getTotalCost())
                .isEqualTo(actualization.getOrderItems().get(0).getTotalCostWithoutPromo());
    }

    @Test
    public void testDiscountNotAvailableForSecondOrderByTaxiUserId() {
        Promo promo = createFirstOrderDiscount();

        createFinishedOrder(
                FRAUDER_UID_1, "+12345", "CARD_123", "TAXI_USER_ID_123", "DEVICE_123"
        );
        OrderActualization actualization = createOrderActualization(
                FRAUDER_UID_2, "+54321", "CARD_321", "TAXI_USER_ID_123", "DEVICE_321", MIN_ITEMS_PRICE
        );

        PromoUser promoUser = createPromoUser(actualization.getOrder().getUid());
        firstOrderDiscountPromoApplyHandler.apply(promo, promoUser, actualization);

        Assertions.assertThat(actualization.getOrderItems().get(0).getTotalCost())
                .isEqualTo(actualization.getOrderItems().get(0).getTotalCostWithoutPromo());
    }

    @Test
    public void testDiscountNotAvailableForSecondOrderByDeviceId() {
        Promo promo = createFirstOrderDiscount();

        createFinishedOrder(
                FRAUDER_UID_1, "+12345", "CARD_123", "TAXI_USER_ID_123", "DEVICE_123"
        );
        OrderActualization actualization = createOrderActualization(
                FRAUDER_UID_2, "+54321", "CARD_321", "TAXI_USER_ID_321", "DEVICE_123", MIN_ITEMS_PRICE
        );

        PromoUser promoUser = createPromoUser(actualization.getOrder().getUid());
        firstOrderDiscountPromoApplyHandler.apply(promo, promoUser, actualization);

        Assertions.assertThat(actualization.getOrderItems().get(0).getTotalCost())
                .isEqualTo(actualization.getOrderItems().get(0).getTotalCostWithoutPromo());
    }

    @Test
    public void testDiscountSplitsOnToItems() {
        Promo promo = createFirstOrderDiscount();

        OrderActualization actualization = buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupOrder(o -> o
                                .setUid(USER_UID)
                                .setDeliveryCostForRecipient(1000L)
                                .setTotalItemsCostWithPromo(MIN_ITEMS_PRICE * 2)
                                .setTotalItemsCost(MIN_ITEMS_PRICE * 2)
                        )
                        .setupItems(l -> List.of(
                                RANDOM.nextObject(OrderItem.class)
                                        .setTotalCostWithoutPromo(MIN_ITEMS_PRICE)
                                        .setTotalCost(MIN_ITEMS_PRICE)
                                        .setPrice(MIN_ITEMS_PRICE)
                                        .setCount(1L),
                                RANDOM.nextObject(OrderItem.class)
                                        .setTotalCostWithoutPromo(MIN_ITEMS_PRICE)
                                        .setTotalCost(MIN_ITEMS_PRICE)
                                        .setPrice(MIN_ITEMS_PRICE)
                                        .setCount(1L)
                        ))
                        .build()
        );

        PromoUser promoUser = createPromoUser(actualization.getOrder().getUid());

        firstOrderDiscountPromoApplyHandler.apply(promo, promoUser, actualization);

        Assertions.assertThat(actualization.getOrderItems())
                .allMatch(i -> i.getTotalCostWithoutPromo() - i.getTotalCost() == DISCOUNT / 2);
    }

    private OrderModel createFinishedOrder(long uid) {
        return createFinishedOrder(uid, null, null, null, null);
    }

    private OrderModel createFinishedOrder(
            long uid, String phone, String trustPaymethodId, String yaTaxiUserId, String deviceId
    ) {
        CreateOrderBuilder.CreateOrderBuilderBuilder createOrderBuilder = CreateOrderBuilder.builder()
                .setupOrder(o -> o
                        .setOrderState(EOrderState.FINISHED)
                        .setYaTaxiUserId(yaTaxiUserId != null ? yaTaxiUserId : o.getYaTaxiUserId())
                        .setDeviceId(deviceId != null ? deviceId : o.getDeviceId())
                        .setUid(uid)
                );

        if (phone != null) {
            createOrderBuilder.setupDelivery(d -> d.setRecipientPhone(phone));
        }

        if (trustPaymethodId != null) {
            createOrderBuilder.setupPayment(p -> p.setTrustPaymethodId(trustPaymethodId));
        }

        return testOrderFactory.createOrder(createOrderBuilder
                .build()
        );
    }

    private PromoUser createPromoUser(long uid) {
        return new PromoUser()
                .setUsed(true)
                .setUsedAt(OffsetDateTime.now(clock))
                .setUid(uid);
    }

    private OrderActualization createOrderActualization(long uid, long itemPrice) {
        return createOrderActualization(uid, null, null, null, null, itemPrice);
    }

    private OrderActualization createOrderActualization(
            long uid, String phone, String trustPaymethodId, String yaTaxiUserId, String deviceId, long itemPrice
    ) {
        CreateOrderActualizationBuilder.CreateOrderActualizationBuilderBuilder createOrderActualizationBuilder =
                CreateOrderActualizationBuilder.builder()
                .setupOrder(o -> o
                        .setUid(uid)
                        .setDeliveryCostForRecipient(1000L)
                        .setTotalItemsCostWithPromo(itemPrice)
                        .setTotalItemsCost(itemPrice)
                        .setYaTaxiUserId(yaTaxiUserId != null ? yaTaxiUserId : o.getYaTaxiUserId())
                        .setDeviceId(deviceId != null ? deviceId : o.getDeviceId())
                )
                .setupItems(l -> List.of(
                        RANDOM.nextObject(OrderItem.class)
                                .setTotalCostWithoutPromo(itemPrice)
                                .setTotalCost(itemPrice)
                                .setPrice(itemPrice)
                                .setCount(1L)
                ));

        if (phone != null) {
            createOrderActualizationBuilder.setupDelivery(d -> d.setRecipientPhone(phone));
        }

        if (trustPaymethodId != null) {
            createOrderActualizationBuilder.setupPayment(p -> p.setTrustPaymethodId(trustPaymethodId));
        }

        return buildOrderActualization(
                createOrderActualizationBuilder
                        .build()
        );
    }

    private Promo createFirstOrderDiscount() {
        return testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(p -> p
                        .setName("FIRST_ORDER_PROMO_30")
                        .setAccessType(EPromoAccessType.ALL_UNLIMITED)
                        .setType(PromoType.FIRST_ORDER_DISCOUNT.name())
                        .setApplicationType(EPromoApplicationType.PROMOCODE)
                        .setValidTill(OffsetDateTime.now(clock))
                )
                .setupState(() -> new FirstOrderDiscountCommonState()
                        .setBudgetUsed(0)
                )
                .setupArgs((a) -> new FirstOrderDiscountArgs()
                        .setDiscount(DISCOUNT)
                        .setBudget(1000_00L)
                        .setMinTotalItemsCost(MIN_ITEMS_PRICE)
                ).build()
        );
    }
}
