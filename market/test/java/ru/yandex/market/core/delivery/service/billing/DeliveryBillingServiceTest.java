package ru.yandex.market.core.delivery.service.billing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import ru.yandex.market.core.balance.model.delivery.DeliveryBalance;
import ru.yandex.market.core.balance.model.delivery.DeliverySpent;
import ru.yandex.market.core.order.model.DeliveryBillingType;
import ru.yandex.market.core.order.model.DeliveryRoute;
import ru.yandex.market.core.order.model.MbiOrderBuilder;
import ru.yandex.market.core.order.model.MbiOrderItem;
import ru.yandex.market.core.order.model.MbiOrderStatus;
import ru.yandex.market.core.order.model.OrderDelivery;
import ru.yandex.market.core.order.model.OrderDeliveryCosts;
import ru.yandex.market.core.order.model.OrderDeliveryDeclaredValue;
import ru.yandex.market.core.order.model.OrderDeliveryType;
import ru.yandex.market.core.order.model.Parcel;
import ru.yandex.market.core.order.model.ParcelBox;
import ru.yandex.market.core.order.model.WeightAndSize;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.order.model.MbiOrderStatus.CANCELLED_IN_DELIVERY;
import static ru.yandex.market.core.order.model.MbiOrderStatus.CANCELLED_IN_PROCESSING;
import static ru.yandex.market.core.order.model.MbiOrderStatus.DELIVERED;
import static ru.yandex.market.core.order.model.MbiOrderStatus.DELIVERY;

/**
 * Тесты для {@link DeliveryBillingService}.
 */
@ExtendWith(MockitoExtension.class)
class DeliveryBillingServiceTest {
    private static final BigDecimal DELIVERY_COST = BigDecimal.valueOf(12345);
    private static final BigDecimal INSURANCE_COST = BigDecimal.valueOf(1234);
    private static final BigDecimal RETURN_COST = BigDecimal.valueOf(250);
    private static final double CHARGEABLE_WEIGHT = 1000;

    private static final OrderDeliveryCosts DELIVERED_DELIVERY_COSTS = OrderDeliveryCosts.builder()
            .setDeliveryCost(DELIVERY_COST)
            .setInsuranceCost(INSURANCE_COST)
            .setReturnCost(RETURN_COST)
            .setFinalCost(DELIVERY_COST.add(RETURN_COST))
            .setChargeableWeight(CHARGEABLE_WEIGHT)
            .build();

    private static final long CONTRACT_ID = 404L;
    private static final long SELLER_ID = 707L;
    private static final long SHOP_ID_1 = 2;
    private static final long SHOP_ID_2 = 22;
    private static final long BALANCE_ORDER_ID_1 = 321L;
    private static final long BALANCE_ORDER_ID_2 = 321123L;
    private static final long DELIVERY_SERVICE_ID1 = 123L;
    private static final long REGION_ID = 213L;
    private static final int WAREHOUSE_ID = 147;
    private static final Long WEIGHT = 100L;
    private static final Long HEIGHT = 1L;
    private static final Long WIDTH = 5L;
    private static final Long DEPTH = 12L;

    private static final WeightAndSize PARCEL_BOX_WEIGHT_AND_SIZE = WeightAndSize.builder()
            .withWeight(2000L)
            .withHeight(220L)
            .withWidth(230L)
            .withDepth(240L)
            .build();

    private static final long PARCEL_BOX_ID = 2L;
    private static final long PARCEL_BOX_ID_NOT_BILLABLE = 3L;

    private static final ParcelBox PARCEL_BOX = ParcelBox.builder(PARCEL_BOX_ID)
            .withWeightAndSize(PARCEL_BOX_WEIGHT_AND_SIZE)
            .build();

    private static final ParcelBox PARCEL_BOX_NOT_BILLABLE = ParcelBox.builder(PARCEL_BOX_ID_NOT_BILLABLE)
            .withWeightAndSize(WeightAndSize.builder().build())
            .build();

    private static final WeightAndSize PARCEL_WEIGHT_AND_SIZE = WeightAndSize.builder()
            .withWeight(WEIGHT).withHeight(HEIGHT).withWidth(WIDTH).withDepth(DEPTH).build();
    private static final Parcel PARCEL = new Parcel.Builder()
            .withWeightAndSize(PARCEL_WEIGHT_AND_SIZE)
            .withBoxes(List.of(PARCEL_BOX, PARCEL_BOX_NOT_BILLABLE))
            .build();

    private static final DeliveryRoute ROUTE = new DeliveryRoute(REGION_ID, REGION_ID);
    private static final BigDecimal ORDER_AMOUNT = BigDecimal.valueOf(15000);
    private static final int DECLARED_VALUE_PERCENT = 50;
    private static final BigDecimal DECLARED_VALUE = BigDecimal.valueOf(7500); //посчитано из ORDER_AMOUNT и
    // DECLARED_VALUE_PERCENT

    private static final DeliveryBalanceOrder DELIVERY_BALANCE_ORDER1 = new DeliveryBalanceOrder(
            SHOP_ID_1, 321123L, BALANCE_ORDER_ID_1, "", "",
            "", "", "", "", "", "",
            BalanceContractType.OFFER, SELLER_ID, CONTRACT_ID);

    private static final DeliveryBalanceOrder DELIVERY_BALANCE_ORDER2 = new DeliveryBalanceOrder(
            SHOP_ID_2, 321123L, BALANCE_ORDER_ID_2, "", "",
            "", "", "", "", "", "",
            BalanceContractType.OFFER, SELLER_ID, CONTRACT_ID);
    @Mock
    private DeliveryBalanceOrderService deliveryBalanceOrderService;
    private DeliveryBillingService deliveryBillingService;

    @BeforeEach
    public void init() {
        deliveryBillingService = new DeliveryBillingService(
                deliveryBalanceOrderService
        );
        when(deliveryBalanceOrderService.getDeliveryBalanceOrder(SHOP_ID_1)).thenReturn(DELIVERY_BALANCE_ORDER1);
        when(deliveryBalanceOrderService.getDeliveryBalanceOrder(SHOP_ID_2)).thenReturn(DELIVERY_BALANCE_ORDER2);
    }

    @Test
    void calcSpent() {
        Map<Long, DeliverySpent> spents = getSpents();

        assertThat(spents).as("We should get spent for two shops").hasSize(2);
        assertThat(spents.get(BALANCE_ORDER_ID_1).getSpent())
                .as("Wrong spent for balanceOrderId = " + BALANCE_ORDER_ID_1)
                .isEqualTo(2765800);
        assertThat(spents.get(BALANCE_ORDER_ID_1).getBlocked())
                .as("Wrong blocked for balanceOrderId = " + BALANCE_ORDER_ID_1)
                .isEqualTo(1382900);
        assertThat(spents.get(BALANCE_ORDER_ID_2).getSpent())
                .as("Wrong spent for balanceOrderId = " + BALANCE_ORDER_ID_2)
                .isEqualTo(1382900);
        assertThat(spents.get(BALANCE_ORDER_ID_2).getBlocked())
                .as("Wrong blocked for balanceOrderId = " + BALANCE_ORDER_ID_2)
                .isEqualTo(0);
    }

    @Test
    void calcSpentForUnknownStatus() {
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> deliveryBillingService.calcSpent(List.of(getMbiOrder5().build())));
    }

    @Test
    void calcDeliveryBalances() {
        Map<Long, Long> paids = getPaids();
        Map<Long, DeliveryBalance> deliveryBalances =
                deliveryBillingService.calcDeliveryBalances(getSpents(), getBalances(), paids, Map.of()).stream()
                        .collect(toMap(DeliveryBalance::getBalanceOrderId, identity()));

        assertThat(deliveryBalances).as("We should get spent for two shops").hasSize(2);
        assertThat(deliveryBalances.get(BALANCE_ORDER_ID_1).getBalance())
                .as("Wrong balance for balanceOrderId = " + BALANCE_ORDER_ID_1)
                .isEqualTo(7234193);
        assertThat(deliveryBalances.get(BALANCE_ORDER_ID_1).getSpent())
                .as("Wrong spent for balanceOrderId = " + BALANCE_ORDER_ID_1)
                .isEqualTo(2765807);
        assertThat(deliveryBalances.get(BALANCE_ORDER_ID_1).getBlocked())
                .as("Wrong blocked for balanceOrderId = " + BALANCE_ORDER_ID_1)
                .isEqualTo(1382900);
        assertThat(deliveryBalances.get(BALANCE_ORDER_ID_2).getBalance())
                .as("Wrong balance for balanceOrderId = " + BALANCE_ORDER_ID_2)
                .isEqualTo(28617093);
        assertThat(deliveryBalances.get(BALANCE_ORDER_ID_2).getSpent())
                .as("Wrong spent for balanceOrderId = " + BALANCE_ORDER_ID_2)
                .isEqualTo(1382907);
        assertThat(deliveryBalances.get(BALANCE_ORDER_ID_2).getBlocked())
                .as("Wrong blocked for balanceOrderId = " + BALANCE_ORDER_ID_2)
                .isEqualTo(0);
    }

    private Map<Long, DeliverySpent> getSpents() {
        return deliveryBillingService.calcSpent(Arrays.asList(
                getMbiOrder1().build(),
                getMbiOrder2().build(),
                getMbiOrder3().build(),
                getMbiOrder4().build()
        ));
    }

    @Nonnull
    private static List<DeliveryBalance> getBalances() {
        return List.of(
                new DeliveryBalance(BALANCE_ORDER_ID_1, 500000L, 5L, 6L, 7L, LocalDate.now()),
                new DeliveryBalance(BALANCE_ORDER_ID_2, 600000L, 5L, 6L, 7L, LocalDate.now())
        );
    }

    @Nonnull
    private static Map<Long, Long> getPaids() {
        return Map.of(
                BALANCE_ORDER_ID_1, 10000000L,
                BALANCE_ORDER_ID_2, 30000000L
        );
    }


    private static MbiOrderBuilder getMbiOrder1() {
        return getMbiOrder(SHOP_ID_1, DELIVERED);
    }

    private static MbiOrderBuilder getMbiOrder2() {
        return getMbiOrder(SHOP_ID_2, DELIVERED);
    }

    private static MbiOrderBuilder getMbiOrder3() {
        return getMbiOrder(SHOP_ID_1, CANCELLED_IN_DELIVERY);
    }

    private static MbiOrderBuilder getMbiOrder4() {
        return getMbiOrder(SHOP_ID_1, DELIVERY);
    }

    private static MbiOrderBuilder getMbiOrder5() {
        return getMbiOrder(SHOP_ID_1, CANCELLED_IN_PROCESSING);
    }

    @Nonnull
    private static MbiOrderBuilder getMbiOrder(long shopId, MbiOrderStatus status) {
        Date now = new Date();
        OrderDelivery orderDelivery = new OrderDelivery(
                now,
                DELIVERED_DELIVERY_COSTS,
                DeliveryBillingType.fromMbiOrderStatus(status),
                ROUTE,
                new OrderDeliveryDeclaredValue(DECLARED_VALUE_PERCENT, DECLARED_VALUE),
                List.of(PARCEL));
        return new MbiOrderBuilder()
                .setId(1)
                .setShopId(shopId)
                .setCampaignId(3)
                .setCreationDate(now)
                .setStatus(status)
                .setTrantime(now)
                .setTotal(ORDER_AMOUNT)
                .setDeliveryServiceId(DELIVERY_SERVICE_ID1)
                .setMarketDelivery(true)
                .setDeliveryType(OrderDeliveryType.PICKUP)
                .setItems(List.of(
                        MbiOrderItem.builder()
                                .setWarehouseId(WAREHOUSE_ID)
                                .build()
                ))
                .setOrderDelivery(orderDelivery);
    }
}
