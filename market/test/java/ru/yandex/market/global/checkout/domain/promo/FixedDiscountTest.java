package ru.yandex.market.global.checkout.domain.promo;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;

import ru.yandex.market.global.checkout.domain.actualize.actualizer.OrderDeliveryCostsActualizer;
import ru.yandex.market.global.checkout.domain.promo.apply.fixed_discount.FixedDiscountArgs;
import ru.yandex.market.global.checkout.domain.promo.apply.fixed_discount.FixedDiscountCommonState;
import ru.yandex.market.global.checkout.domain.promo.model.PromoType;
import ru.yandex.market.global.checkout.factory.TestCartFactory;
import ru.yandex.market.global.checkout.factory.TestPromoFactory;
import ru.yandex.market.global.db.jooq.enums.EPromoAccessType;
import ru.yandex.market.global.db.jooq.enums.EPromoApplicationType;
import ru.yandex.market.global.db.jooq.tables.pojos.Promo;
import ru.yandex.mj.generated.server.model.CartActualizeDto;
import ru.yandex.mj.generated.server.model.CartActualizeResultDto;
import ru.yandex.mj.generated.server.model.CartItemActualizeDto;
import ru.yandex.mj.generated.server.model.CartItemDto;
import ru.yandex.mj.generated.server.model.OrderCartDto;
import ru.yandex.mj.generated.server.model.OrderDto;

import static ru.yandex.market.global.common.test.TestUtil.createCheckedUserTicket;
import static ru.yandex.market.global.common.test.TestUtil.mockRequestAttributes;
import static ru.yandex.market.starter.tvm.filters.UserTicketFilter.CHECKED_USER_TICKET_ATTRIBUTE;

public class FixedDiscountTest extends BasePromoTest {
    private static final String SORRY_PROMOCODE = "SORRY20";

    @Test
    void testSorryPromocodeGiveDiscount() {
        Promo promo = createPromo();
        promoCommandSerivce.grantUsage(promo, UID);
        OrderCartDto cart = createCartDto();

        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));
        OrderDto orderDto = orderApiService.apiV1OrderCreatePost(
                SOME_USER_TICKET, YA_TAXI_USERID, null, cart
        ).getBody();

        Assertions.assertThat(orderDto)
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withIgnoreAllExpectedNullFields(true)
                        .build()
                )
                .isEqualTo(new OrderDto()
                        .totalCost(247_00L) // 123 * 2 + 16 -15
                        .deliveryCostForRecipient(16_00L)
                        .deliveryCostForShop(OrderDeliveryCostsActualizer.DEFAULT_DELIVERY_COST_FOR_SHOP)
                        .items(null)
                );
    }

    @Test
    void testSorryPromocodeAllowUsageIfGrantedSeveralTimes() {
        Promo promo = createPromo();
        promoCommandSerivce.grantUsage(promo, UID);
        promoCommandSerivce.grantUsage(promo, UID);

        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));

        Assertions.assertThatCode(() -> cartApiService.apiV1CartActualizePost(
                SOME_USER_TICKET, YA_TAXI_USERID, createCartActualizeDto()
        )).doesNotThrowAnyException();

        Assertions.assertThatCode(() -> orderApiService.apiV1OrderCreatePost(
                SOME_USER_TICKET, YA_TAXI_USERID, null, createCartDto()
        )).doesNotThrowAnyException();
    }


    @Test
    void testSorryPromocodeInActualize() {
        CartActualizeDto cartActualize = createCartActualizeDto();
        Promo promo = createPromo();
        promoCommandSerivce.grantUsage(promo, UID);

        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));

        CartActualizeResultDto cartActualizeResultDto = cartApiService.apiV1CartActualizePost(
                SOME_USER_TICKET, YA_TAXI_USERID, cartActualize
        ).getBody();

        Assertions.assertThat(cartActualizeResultDto)
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withIgnoreAllExpectedNullFields(true)
                        .build()
                )
                .isEqualTo(new OrderDto()
                        .totalCost(241_00L) // 123 * 2 + 10 -15
                        .deliveryCostForRecipient(1000L)
                        .deliveryCostForShop(OrderDeliveryCostsActualizer.DEFAULT_DELIVERY_COST_FOR_SHOP)
                        .items(null)
                );
    }

    @Test
    void testSorryPromocodeApplyaOnlyToGrantedUser() {
        OrderCartDto cart = createCartDto();
        Promo promo = createPromo();
        promoCommandSerivce.grantUsage(promo, UID);

        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID + 1)));

        OrderDto orderDto = orderApiService.apiV1OrderCreatePost(
                SOME_USER_TICKET, YA_TAXI_USERID, null, cart
        ).getBody();

        //noinspection ConstantConditions
        Assertions.assertThat(orderDto.getTotalCost())
                .isEqualTo(262_00L); // 123 * 2 + 16 (but no 15 discount)
    }

    @Test
    void testSorryPromocodeCanBeUsedOnlyOneTime() {
        OrderCartDto cart = createCartDto();
        Promo promo = createPromo();
        promoCommandSerivce.grantUsage(promo, UID);
        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(UID)));

        OrderDto firstOrder = orderApiService.apiV1OrderCreatePost(
                SOME_USER_TICKET, YA_TAXI_USERID, null, cart
        ).getBody();
        //noinspection ConstantConditions
        Assertions.assertThat(firstOrder.getTotalCost())
                .isEqualTo(247_00L); // 123 * 2 - 16 - 15

        OrderDto secondOrder = orderApiService.apiV1OrderCreatePost(
                SOME_USER_TICKET, YA_TAXI_USERID, null, cart
        ).getBody();
        //noinspection ConstantConditions
        Assertions.assertThat(secondOrder.getTotalCost())
                .isEqualTo(262_00L); // 123 * 2 - 16 - (but no 15 discount)
    }

    private Promo createPromo() {
        return testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(p -> p
                        .setName(SORRY_PROMOCODE)
                        .setDescription("15 ILS off for your next order when using this promocode")
                        .setType(PromoType.FIXED_DISCOUNT.name())
                        .setAccessType(EPromoAccessType.ISSUED)
                        .setApplicationType(EPromoApplicationType.PROMOCODE)
                )
                .setupArgs((a) -> new FixedDiscountArgs()
                        .setDiscount(1500)
                        .setMinTotalItemsCost(0)
                        .setBudget(9999999)
                )
                .setupState(() -> new FixedDiscountCommonState()
                        .setBudgetUsed(0)
                )
                .build()
        );
    }

    private OrderCartDto createCartDto() {
        return testCartFactory.createOrderCartDto(TestCartFactory.CreateOrderCartDtoBuilder.builder()
                .setupCart(c -> new OrderCartDto()
                        .businessId(BUSINESS_ID)
                        .shopId(SHOP_ID)
                        .promocodes(List.of(SORRY_PROMOCODE))
                        .recipientFirstName(c.getRecipientFirstName())
                        .recipientLastName(c.getRecipientLastName())
                        .recipientPhone(c.getRecipientPhone())
                        .trustPaymethodId(c.getTrustPaymethodId())
                        .items(List.of(new CartItemDto()
                                .shopId(SHOP_ID)
                                .businessId(BUSINESS_ID)
                                .offerId(OFFER_ID)
                                .count(2L)
                        ))
                        .paymentReturnUrl("https://ya.ru/")
                )
                .build()
        );
    }

    private CartActualizeDto createCartActualizeDto() {
        return testCartFactory.createCartActualizeDto(TestCartFactory.CreateCartActualizeDtoBuilder.builder()
                .setupCartActualize(c -> new CartActualizeDto()
                        .businessId(BUSINESS_ID)
                        .shopId(SHOP_ID)
                        .promocodes(List.of(SORRY_PROMOCODE))
                        .recipientFirstName(c.getRecipientFirstName())
                        .recipientLastName(c.getRecipientLastName())
                        .recipientPhone(c.getRecipientPhone())
                        .items(List.of(new CartItemActualizeDto()
                                .shopId(SHOP_ID)
                                .businessId(BUSINESS_ID)
                                .offerId(OFFER_ID)
                                .count(2L)
                        ))
                ).build()
        );
    }
}
