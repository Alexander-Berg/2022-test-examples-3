package ru.yandex.market.loyalty.core.trigger.restrictions;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;

import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.loyalty.core.dao.exclusions.offers.OffersExclusionsRules;
import ru.yandex.market.loyalty.core.dao.exclusions.offers.OffersFilter;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.Experiments;
import ru.yandex.market.loyalty.core.model.order.Item;
import ru.yandex.market.loyalty.core.model.order.ItemKey;
import ru.yandex.market.loyalty.core.model.trigger.UnresolvableException;
import ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusUpdatedEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.data.OrderEventInfo;
import ru.yandex.market.loyalty.core.service.exclusions.ExcludedOffersService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;

import static ru.yandex.market.loyalty.core.utils.CheckouterUtils.DEFAULT_ORDER_ID;

public class RestrictedItemsCostCalculationTest extends MarketLoyaltyCoreMockedDbTestBase {

    @Autowired
    private OrderAmountRestrictionFactory orderAmountRestrictionFactory;
    @Autowired
    ExcludedOffersService excludedOffersService;
    @Value("classpath:${ENVIRONMENT:local}/excludedOffers.json")
    String excludedOffersPath;
    @Autowired
    ResourceLoader resourceLoader;
    @Autowired
    OrderCategoryRestrictionFactory orderCategoryRestrictionFactory;


    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int ALLOWED_ITEM_HID = 100;
    private static final BigDecimal VERY_LARGE_PRICE = BigDecimal.valueOf(Integer.MAX_VALUE);
    private static final BigDecimal ALLOWED_ITEM_PRICE = BigDecimal.valueOf(100);

    @Test
    public void shouldFitAmountConditionExcludingRestrictedItemsPrice() throws UnresolvableException {

        OrderAmountRestrictionFactory.OrderAmountRestriction restriction =
                orderAmountRestrictionFactory.create(
                        Long.MIN_VALUE,
                        new AmountRangeDto(ALLOWED_ITEM_PRICE, ALLOWED_ITEM_PRICE)
                );

        Assert.assertTrue(
                restriction.fitCondition(
                        createUpdateEventWithAllowedAndRestrictedItems(),
                        null,
                        null
                ).isMatched()
        );
    }


    @Test
    public void shouldFitRestrictionOrderTotalRangeExcludingRestrictedItemsPrice() throws UnresolvableException {
        OrderCategoryRestrictionFactory.OrderCategoryRestriction orderRestriction = createOrderRestriction();

        Assert.assertTrue(
                orderRestriction.fitCondition(
                        createUpdateEventWithAllowedAndRestrictedItems(),
                        null,
                        null
                ).isMatched()
        );
    }

    private OrderCategoryRestrictionFactory.OrderCategoryRestriction createOrderRestriction() {
        try {
            SetWithRelationDto<Integer> orderRestrictionDto = new SetWithRelationDto<>();
            orderRestrictionDto.setMaxTotal(ALLOWED_ITEM_PRICE);
            orderRestrictionDto.setMinTotal(ALLOWED_ITEM_PRICE);
            orderRestrictionDto.setSetRelation(SetRelation.AT_LEAST_ONE_INCLUDED_IN_SET);
            orderRestrictionDto.setGivenSet(Collections.singleton(ALLOWED_ITEM_HID));

            return orderCategoryRestrictionFactory.create(1L, objectMapper.writeValueAsString(orderRestrictionDto));
        } catch (Exception e) {
            throw new AssertionError(e.getMessage());
        }
    }

    private OrderStatusUpdatedEvent createUpdateEventWithAllowedAndRestrictedItems() {
        OrderEventInfo.Builder builder = OrderEventInfo.builder()
                .setPlatform(CoreMarketPlatform.BLUE)
                .setPaymentType(PaymentType.PREPAID)
                .setNoAuth(false)
                .setExperiments(Experiments.EMPTY)
                .setOrderId(DEFAULT_ORDER_ID);
        createRestrictedItems().forEach(builder::addItem);
        builder.addItem(Item.Builder
                .create()
                .withQuantity(BigDecimal.valueOf(1))
                .withKey(ItemKey.ofFeedOffer(123L, "123"))
                .withHyperCategoryId(ALLOWED_ITEM_HID)
                .withPrice(ALLOWED_ITEM_PRICE)
                .build()
        );
        return OrderStatusUpdatedEvent.builder()
                .addPersistentData(builder.build())
                .build();
    }


    private Stream<Item> createRestrictedItems() {
        return getRestrictionExclusion().getHids()
                .stream()
                .map(hid -> Item.Builder.create()
                        .withQuantity(BigDecimal.valueOf(1))
                        .withKey(ItemKey.ofFeedOffer(1234L, "1234"))
                        .withHyperCategoryId(Math.toIntExact(hid))
                        .withPrice(VERY_LARGE_PRICE)
                        .build()
                );
    }


    private OffersFilter getRestrictionExclusion() {
        try {
            OffersExclusionsRules offersExclusionsRules = objectMapper.readValue(
                    resourceLoader.getResource(excludedOffersPath).getURL(),
                    OffersExclusionsRules.class
            );
            return offersExclusionsRules.restrictionExclusion;
        } catch (IOException e) {
            throw new AssertionError(e.getMessage());
        }
    }

}
