package ru.yandex.market.global.checkout.domain.promo;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.actualize.ActualizationError;
import ru.yandex.market.global.checkout.domain.actualize.OrderActualization;
import ru.yandex.market.global.checkout.domain.promo.apply.first_order_discount.FirstOrderDiscountCommonState;
import ru.yandex.market.global.checkout.domain.promo.apply.welcome.WelcomeArgs;
import ru.yandex.market.global.checkout.domain.promo.apply.welcome.WelcomePromoApplyHandler;
import ru.yandex.market.global.checkout.domain.promo.model.PromoType;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.factory.TestPromoFactory;
import ru.yandex.market.global.checkout.util.RandomDataGenerator;
import ru.yandex.market.global.common.elastic.dictionary.DictionaryQueryService;
import ru.yandex.market.global.db.jooq.enums.EPromoAccessType;
import ru.yandex.market.global.db.jooq.enums.EPromoApplicationType;
import ru.yandex.market.global.db.jooq.tables.pojos.OrderItem;
import ru.yandex.market.global.db.jooq.tables.pojos.Promo;
import ru.yandex.market.global.db.jooq.tables.pojos.PromoUser;
import ru.yandex.mj.generated.server.model.OfferDto;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class WelcomeTest extends BaseFunctionalTest {
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(
            WelcomeTest.class
    ).build();

    private static final long MIN_ITEMS_PRICE = 100_00L;
    private static final long DISCOUNT = 50_00L;
    private static final long USER_UID = 2L;


    private static final OfferDto SPECIAL_OFFER_1 = RANDOM.nextObject(OfferDto.class)
            .id("11_111_SPECIAL_OFFER_1")
            .businessId(11L)
            .shopId(111L)
            .offerId("SPECIAL_OFFER_1")
            .promos(List.of("WELCOME"))
            .price(31_00L);

    private static final OfferDto SPECIAL_OFFER_20 = RANDOM.nextObject(OfferDto.class)
            .id("22_222_SPECIAL_OFFER_20")
            .businessId(22L)
            .shopId(222L)
            .offerId("SPECIAL_OFFER_20")
            .promos(List.of("WELCOME"))
            .price(52_00L);

    private static final OfferDto SPECIAL_OFFER_50 = RANDOM.nextObject(OfferDto.class)
            .id("22_222_SPECIAL_OFFER_50")
            .businessId(22L)
            .shopId(222L)
            .offerId("SPECIAL_OFFER_50")
            .promos(List.of("WELCOME"))
            .price(81_00L);

    private static final OfferDto SPECIAL_OFFER_NO_DISCOUNT = RANDOM.nextObject(OfferDto.class)
            .id("22_222_SPECIAL_OFFER_NO_DISCOUNT")
            .businessId(22L)
            .shopId(222L)
            .offerId("SPECIAL_OFFER_NO_DISCOUNT")
            .promos(List.of("WELCOME"))
            .price(121_00L);

    private static final OfferDto OTHER_OFFER = RANDOM.nextObject(OfferDto.class)
            .id("33_333_OTHER")
            .businessId(33L)
            .shopId(333L)
            .offerId("OTHER")
            .promos(List.of("SOME_NOT_WELCOME"))
            .price(60_00L);

    private static final long BUDGET = 1000_00L;

    private final WelcomePromoApplyHandler promoApplyHandler;
    private final DictionaryQueryService<OfferDto> offersDictionary;

    private final Clock clock;
    private final TestOrderFactory testOrderFactory;
    private final TestPromoFactory testPromoFactory;

    @Test
    public void testDiscountForSpecialOffer1() {
        Promo promo = createPromo();
        OrderActualization actualization = createOrderActualization(1, true, SPECIAL_OFFER_1);
        PromoUser promoUser = createPromoUser(actualization.getOrder().getUid());

        promoApplyHandler.apply(promo, promoUser, actualization);

        Assertions.assertThat(actualization.getOrderItems().get(0).getTotalCost())
                .isEqualTo(1_00L);
    }

    @Test
    public void testDiscountForSpecialOffer20() {
        Promo promo = createPromo();
        OrderActualization actualization = createOrderActualization(1, true, SPECIAL_OFFER_20);
        PromoUser promoUser = createPromoUser(actualization.getOrder().getUid());

        promoApplyHandler.apply(promo, promoUser, actualization);

        Assertions.assertThat(actualization.getOrderItems().get(0).getTotalCost())
                .isEqualTo(20_00L);
    }

    @Test
    public void testDiscountForSpecialOffer50() {
        Promo promo = createPromo();
        OrderActualization actualization = createOrderActualization(1, true, SPECIAL_OFFER_50);
        PromoUser promoUser = createPromoUser(actualization.getOrder().getUid());

        promoApplyHandler.apply(promo, promoUser, actualization);

        Assertions.assertThat(actualization.getOrderItems().get(0).getTotalCost())
                .isEqualTo(50_00L);
    }

    @Test
    public void testDiscountForSpecialOfferNoDiscount() {
        Promo promo = createPromo();
        OrderActualization actualization = createOrderActualization(1, true, SPECIAL_OFFER_NO_DISCOUNT);
        PromoUser promoUser = createPromoUser(actualization.getOrder().getUid());

        promoApplyHandler.apply(promo, promoUser, actualization);

        Assertions.assertThat(actualization.getOrderItems().get(0).getTotalCost())
                .isEqualTo(SPECIAL_OFFER_NO_DISCOUNT.getPrice());
    }

    @Test
    public void testDiscountOnlyForOneItem() {
        Promo promo = createPromo();
        OrderActualization actualization = createOrderActualization(2, true, SPECIAL_OFFER_1);
        PromoUser promoUser = createPromoUser(actualization.getOrder().getUid());

        promoApplyHandler.apply(promo, promoUser, actualization);

        Assertions.assertThat(actualization.getOrderItems().get(0).getTotalCost())
                .isEqualTo(100L + SPECIAL_OFFER_1.getPrice());
    }

    @Test
    public void testDiscountOnlyForExpensiveItem() {
        Promo promo = createPromo();
        OrderActualization actualization = createOrderActualization(
                1, true, SPECIAL_OFFER_1, SPECIAL_OFFER_20
        );
        PromoUser promoUser = createPromoUser(actualization.getOrder().getUid());

        promoApplyHandler.apply(promo, promoUser, actualization);

        Assertions.assertThat(actualization.getOrderItems().get(0).getTotalCost())
                .isEqualTo(SPECIAL_OFFER_1.getPrice());
        Assertions.assertThat(actualization.getOrderItems().get(1).getTotalCost())
                .isEqualTo(20_00L);
    }

    @Test
    public void testBudgetLimit() {
        Promo promo = createPromo(BUDGET - 1);
        OrderActualization actualization = createOrderActualization(
                1, true, SPECIAL_OFFER_20
        );
        PromoUser promoUser = createPromoUser(actualization.getOrder().getUid());

        promoApplyHandler.apply(promo, promoUser, actualization);

        Assertions.assertThat(actualization.getWarnings())
                .map(ActualizationError::getMessageTankerKey)
                .contains("promocode_bottom_sheet.invalid_error");

        Assertions.assertThat(actualization.getOrder().getTotalItemsCost())
                .isEqualTo(actualization.getOrder().getTotalItemsCostWithPromo());
    }

    @Test
    public void testFixedDiscount() {
        Promo promo = createPromo();
        OrderActualization actualization = createOrderActualization(
                2, false, OTHER_OFFER
        );
        PromoUser promoUser = createPromoUser(actualization.getOrder().getUid());

        promoApplyHandler.apply(promo, promoUser, actualization);

        Assertions.assertThat(actualization.getErrors()).isEmpty();
        Assertions.assertThat(actualization.getWarnings()).isEmpty();
        Assertions.assertThat(actualization.getOrderItems().get(0).getTotalCost())
                .isEqualTo(OTHER_OFFER.getPrice() * 2 - ((WelcomeArgs) promo.getArgs()).getDiscount());
    }

    @Test
    public void testNoDiscount() {
        Promo promo = createPromo();
        OrderActualization actualization = createOrderActualization(
                1, false, OTHER_OFFER
        );
        PromoUser promoUser = createPromoUser(actualization.getOrder().getUid());

        promoApplyHandler.apply(promo, promoUser, actualization);

        Assertions.assertThat(actualization.getOrderItems().get(0).getTotalCost())
                .isEqualTo(OTHER_OFFER.getPrice());
    }

    private PromoUser createPromoUser(long uid) {
        return new PromoUser()
                .setUsed(true)
                .setUsedAt(OffsetDateTime.now(clock))
                .setUid(uid);
    }

    private void mockOffersDictionaryAnswer(List<OfferDto> offers) {
        //noinspection unchecked
        Mockito.when(offersDictionary.get(Mockito.any(List.class))).thenReturn(offers);
    }

    private OrderActualization createOrderActualization(int count, boolean mosck, OfferDto... offers) {
        mockOffersDictionaryAnswer(mosck ? List.of(offers) : List.of());

        long totalCost = Arrays.stream(offers).mapToLong(o -> o.getPrice() * count).sum();
        List<OrderItem> orderItems = Arrays.stream(offers)
                .flatMap(o -> RANDOM.objects(OrderItem.class, count)
                        .peek(i -> i
                                .setOfferId(o.getOfferId())
                                .setTotalCostWithoutPromo(o.getPrice() * count)
                                .setTotalCost(o.getPrice() * count)
                                .setPrice(o.getPrice())
                                .setCount((long) count)
                        )
                ).collect(Collectors.toList());

        return TestOrderFactory.buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupOrder(o -> o
                                .setUid(WelcomeTest.USER_UID)
                                .setTotalItemsCostWithPromo(totalCost)
                                .setTotalItemsCost(totalCost)
                        )
                        .setupItems(l -> orderItems)
                        .build()
        );
    }

    private Promo createPromo() {
        return createPromo(0);
    }

    private Promo createPromo(long budgetUsed) {
        return testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(p -> p
                        .setName("WELCOME")
                        .setAccessType(EPromoAccessType.ALL_LIMITED)
                        .setLimitedUsagesCount(1)
                        .setType(PromoType.WELCOME.name())
                        .setApplicationType(EPromoApplicationType.PROMOCODE)
                )
                .setupState(() -> new FirstOrderDiscountCommonState()
                        .setBudgetUsed(budgetUsed)
                )
                .setupArgs((a) -> new WelcomeArgs()
                        .setDiscount(DISCOUNT)
                        .setBudget(BUDGET)
                        .setMinTotalItemsCost(MIN_ITEMS_PRICE)
                ).build()
        );
    }
}
