package ru.yandex.market.checkout.checkouter.promo.yandexemployee;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.promo.AbstractPromoTestBase;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType.YANDEX_MARKET;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoType.YANDEX_EMPLOYEE;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoType.YANDEX_PLUS;
import static ru.yandex.market.checkout.test.providers.ActualDeliveryProvider.DELIVERY_PRICE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

public class YandexEmployeeFreeDeliveryApiTest extends AbstractPromoTestBase {

    private Parameters parameters;

    @BeforeEach
    public void init() {
        parameters = BlueParametersProvider.defaultBlueOrderParameters();
        patchAddressForFreeDelivery(parameters);
        parameters.setShopId(SHOP_ID_WITH_SORTING_CENTER);
        parameters.setYandexEmployee(true);
        parameters.setCheckCartErrors(false);
        parameters.setEmptyPushApiDeliveryResponse();
        parameters.setMockLoyalty(true);
        parameters.getLoyaltyParameters().addDeliveryDiscount(
                DeliveryType.COURIER,
                new LoyaltyDiscount(new BigDecimal(Integer.MAX_VALUE), PromoType.YANDEX_EMPLOYEE)
        );
    }

    @Test
    @DisplayName("Проверяем прямой сценарий бесплатной доставки сотруднику")
    public void testYandexEmployeeFreeDeliveryPromo() {
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addPickup(100501L, 2, Collections.singletonList(12312303L))
                        .addDelivery(MOCK_DELIVERY_SERVICE_ID, 3).build()
        );

        parameters.cartResultActions()
                .andExpect(jsonPath("$.carts[*].deliveryOptions[?(@.type=='%s')].buyerPrice", "DELIVERY")
                        .value(0))
                .andExpect(jsonPath("$.carts[*].deliveryOptions[?(@.type=='%s')].promos[*].type", "DELIVERY")
                        .value(YANDEX_EMPLOYEE.getCode()))
                .andExpect(jsonPath("$.carts[*].deliveryOptions[?(@.type=='%s')].promos[*].buyerDiscount", "DELIVERY")
                        .value(DELIVERY_PRICE.intValue()));

        MultiCart multiCart = orderCreateHelper.cart(parameters);

        List<? extends Delivery> filteredOptions = multiCart.getCarts().get(0).getDeliveryOptions().stream()
                .filter(o -> CollectionUtils.isNotEmpty(o.getPromos()))
                .collect(Collectors.toList());

        assertThat(filteredOptions, hasSize(greaterThanOrEqualTo(1)));

        Optional<? extends ItemPromo> optionalItemPromo = filteredOptions.stream()
                .filter(Delivery::isFree)
                .flatMap(d -> d.getPromos().stream())
                .filter(promo -> promo.getType() == YANDEX_EMPLOYEE)
                .findFirst();

        assertThat(optionalItemPromo.isPresent(), is(true));
    }

    @Test
    @DisplayName("Проверяем какие промо будут в случае, если доступно несколько одновременных промо, при этом промо " +
            "яндекс плюс НЕ на всю сумму")
    public void testYandexEmployeePromoPriorityWithPartialOtherPromoDiscount() {
        parameters.setYandexPlus(true);
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addPickup(100501L, 2, Collections.singletonList(12312303L))
                        .addDelivery(MOCK_DELIVERY_SERVICE_ID, 3)
                        .build()
        );
        parameters.getLoyaltyParameters().addDeliveryDiscount(
                DeliveryType.COURIER,
                new LoyaltyDiscount(BigDecimal.valueOf(100), YANDEX_PLUS)
        );

        parameters.getLoyaltyParameters().addDeliveryDiscount(
                DeliveryType.PICKUP,
                new LoyaltyDiscount(BigDecimal.valueOf(100), YANDEX_PLUS)
        );

        //для доставки если в репорте была промо не на всю сумму, то на доставку должна появиться промо YANDEX_EMPLOYEE
        parameters.cartResultActions()
                .andExpect(jsonPath("$.carts[*].deliveryOptions[?(@.type=='%s')].buyerPrice", "DELIVERY")
                        .value(0))
                .andExpect(jsonPath("$.carts[*].deliveryOptions[?(@.type=='%s')].promos[*].type", "DELIVERY")
                        .value(YANDEX_EMPLOYEE.getCode()))
                .andExpect(jsonPath("$.carts[*].deliveryOptions[?(@.type=='%s')].promos[*].buyerDiscount", "DELIVERY")
                        .value(DELIVERY_PRICE.intValue()))

                .andExpect(jsonPath("$.carts[*].deliveryOptions[?(@.type=='%s')].buyerPrice", "PICKUP")
                        .value(0))
                .andExpect(jsonPath("$.carts[*].deliveryOptions[?(@.type=='%s')].promos[*].type", "PICKUP")
                        .value(YANDEX_PLUS.getCode()))
                .andExpect(jsonPath("$.carts[*].deliveryOptions[?(@.type=='%s')].promos[*].buyerDiscount", "PICKUP")
                        .value(DELIVERY_PRICE.intValue()));

        MultiCart multiCart = orderCreateHelper.cart(parameters);

        List<? extends Delivery> filteredOptions = multiCart.getCarts().get(0).getDeliveryOptions().stream()
                .filter(o -> CollectionUtils.isNotEmpty(o.getPromos()))
                .collect(Collectors.toList());


        Assertions.assertEquals(1, filteredOptions.stream().flatMap(d -> d.getPromos().stream()).
                filter(promo -> promo.getType() == YANDEX_EMPLOYEE).count());
        Assertions.assertEquals(1, filteredOptions.stream().flatMap(d -> d.getPromos().stream()).
                filter(promo -> promo.getType() == YANDEX_PLUS).count());
    }

    @Test
    @DisplayName("Проверяем какие промо будут в случае, если доступно несколько одновременных промо, при этом промо " +
            "яндекс плюс на всю сумму")
    public void testYandexEmployeePromoPriorityWithFullOtherPromoDiscount() {
        parameters.setYandexPlus(true);
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addPickup(100501L, 2, Collections.singletonList(12312303L))
                        .addDelivery(MOCK_DELIVERY_SERVICE_ID, 3)
                        .build()
        );

        parameters.getLoyaltyParameters().addDeliveryDiscount(
                DeliveryType.COURIER,
                new LoyaltyDiscount(BigDecimal.valueOf(100), YANDEX_PLUS)
        );

        parameters.getLoyaltyParameters().addDeliveryDiscount(
                DeliveryType.PICKUP,
                new LoyaltyDiscount(BigDecimal.valueOf(100), YANDEX_PLUS)
        );

        // для доставки если в репорте была промо на всю сумму, то на доставку
        // должна появиться промо YANDEX_EMPLOYEE или YANDEX_PLUS в зависимости от приоритета (сейчас YANDEX_EMPLOYEE )
        // см. ru.yandex.market.checkout.checkouter.delivery
        // .YandexMarketDeliveryActualizerImpl#fillBlueOrdersDeliveryDiscount
        parameters.cartResultActions()
                .andExpect(jsonPath("$.carts[*].deliveryOptions[?(@.type=='%s')].buyerPrice", "DELIVERY")
                        .value(0))
                .andExpect(jsonPath("$.carts[*].deliveryOptions[?(@.type=='%s')].promos[*].type", "DELIVERY")
                        .value(YANDEX_EMPLOYEE.getCode()))
                .andExpect(jsonPath("$.carts[*].deliveryOptions[?(@.type=='%s')].promos[*].buyerDiscount", "DELIVERY")
                        .value(DELIVERY_PRICE.intValue()))

                .andExpect(jsonPath("$.carts[*].deliveryOptions[?(@.type=='%s')].buyerPrice", "PICKUP")
                        .value(0))
                .andExpect(jsonPath("$.carts[*].deliveryOptions[?(@.type=='%s')].promos[*].type", "PICKUP")
                        .value(YANDEX_PLUS.getCode()))
                .andExpect(jsonPath("$.carts[*].deliveryOptions[?(@.type=='%s')].promos[*].buyerDiscount", "PICKUP")
                        .value(DELIVERY_PRICE.intValue()));

        MultiCart multiCart = orderCreateHelper.cart(parameters);

        List<? extends Delivery> filteredOptions = multiCart.getCarts().get(0).getDeliveryOptions().stream()
                .filter(o -> CollectionUtils.isNotEmpty(o.getPromos()))
                .collect(Collectors.toList());

        Assertions.assertEquals(1, filteredOptions.stream().flatMap(d -> d.getPromos().stream()).
                filter(promo -> promo.getType() == YANDEX_EMPLOYEE).count());
        Assertions.assertEquals(1, filteredOptions.stream().flatMap(d -> d.getPromos().stream()).
                filter(promo -> promo.getType() == YANDEX_PLUS).count());
    }

    @Test
    @DisplayName("Проверяем какие промо и цены будут в случае, если по заказу и так бесплатная доставка, т.к. большая" +
            " сумма заказа")
    public void testFreeDeliveryWithYandexEmployeePromo() {
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder().withFreeDelivery().addDelivery(MOCK_DELIVERY_SERVICE_ID, 3).build()
        );

        parameters.cartResultActions()
                .andExpect(jsonPath("$.carts[*].deliveryOptions[?(@.type=='%s')].buyerPrice", "DELIVERY")
                        .value(0))
                .andExpect(jsonPath("$.carts[*].deliveryOptions[?(@.type=='%s')].promos[*]", "DELIVERY")
                        .doesNotExist());


        MultiCart multiCart = orderCreateHelper.cart(parameters);


        List<? extends Delivery> filteredOptions = multiCart.getCarts().get(0).getDeliveryOptions().stream()
                .filter(o -> CollectionUtils.isNotEmpty(o.getPromos()))
                .collect(Collectors.toList());
        Assertions.assertTrue(filteredOptions.isEmpty());
    }


    @Test
    @DisplayName("Проверяем чекаут с промо \"бесплатная доставка сотруднику\", сохранение и чтение его из бд")
    public void testYandexEmployeeFreeDeliverySave() throws Exception {
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addPickup(100501L, 2, Collections.singletonList(12312303L))
                        .addDelivery(MOCK_DELIVERY_SERVICE_ID, 3).build()
        );

        parameters.cartResultActions()
                .andExpect(jsonPath("$.carts[*].deliveryOptions[?(@.type=='%s')].buyerPrice", "DELIVERY")
                        .value(0))
                .andExpect(jsonPath("$.carts[*].deliveryOptions[?(@.type=='%s')].promos[*].type", "DELIVERY")
                        .value(YANDEX_EMPLOYEE.getCode()))
                .andExpect(jsonPath("$.carts[*].deliveryOptions[?(@.type=='%s')].promos[*].buyerDiscount", "DELIVERY")
                        .value(DELIVERY_PRICE.intValue()));

        MultiCart multiCart = orderCreateHelper.cart(parameters);

        parameters.setDeliveryPartnerType(YANDEX_MARKET);
        parameters.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);

        MultiOrder checkout = orderCreateHelper.checkout(multiCart, parameters);

        Order order = checkout.getOrders().get(0);
        Order persistedOrder = client.getOrder(order.getId(), ClientRole.SYSTEM, null);
        assertThat(persistedOrder.getDelivery().getPromos(), hasSize(1));
        assertThat(persistedOrder.getDelivery().getPromos().iterator().next().getType(), equalTo(YANDEX_EMPLOYEE));
    }
}
