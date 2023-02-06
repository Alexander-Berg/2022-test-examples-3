package ru.yandex.market.shopadminstub.sax.serialize;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.util.XpathExpectationsHelper;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.common.xml.outlets.Outlet;
import ru.yandex.market.shopadminstub.model.CartResponse;
import ru.yandex.market.shopadminstub.model.DeliveryResponse;
import ru.yandex.market.shopadminstub.model.Item;
import ru.yandex.market.shopadminstub.model.ItemDeliveryOption;
import ru.yandex.market.shopadminstub.model.ReportOutlet;

public class CartResponseBuilderTest {

    private static final int MOSCOW_REGION_ID = 213;
    private static final long FEED_ID = 1L;
    private static final String FIRST_OFFER_ID = "1";
    private static final String SECOND_OFFER_ID = "2";

    private static final long MARKET_OUTLET_ID = 123L;
    private static final Set<PaymentMethod> PAYMENT_METHODS = Sets.newHashSet(PaymentMethod.CASH_ON_DELIVERY);
    private static final BigDecimal REPORT_OUTLET_PRICE = BigDecimal.TEN;
    private static final int REPORT_OUTLET_MIN_DELIVERY_DAYS = 0;
    private static final int REPORT_OUTLET_MAX_DELIVERY_DAYS = 2;
    private static final DateTimeFormatter DELIVERY_DATES_FORMATTER = DateTimeFormatter.ofPattern("dd-MM" +
            "-yyyy");
    private static final String PICKUP = "PICKUP";

    @Test
    public void testBuildResponseIfOnlyOneItemHasDeliveryAndOtherHasNot() throws Exception {
        Outlet outlet = new Outlet();
        outlet.marketPointId = MARKET_OUTLET_ID;
        outlet.cityRegionId = MOSCOW_REGION_ID;

        ObjectMapper xmlMapper = Jackson2ObjectMapperBuilder
                .xml()
                .build();

        String xml = xmlMapper.writeValueAsString(
                new CartResponseBuilder(
                        Clock.systemDefaultZone(), new ItemDeliveryOptionsBuilder(Clock.systemDefaultZone())
                ).response(
                        MOSCOW_REGION_ID,
                        ImmutableMap.of(
                                OfferItemKey.of(FIRST_OFFER_ID, FEED_ID, null), getFirstItem(),
                                OfferItemKey.of(SECOND_OFFER_ID, FEED_ID, null), getSecondItem()
                        ),
                        new Outlet[]{outlet},
                        Currency.RUR
                )
        );

        System.out.println(xml);

        new XpathExpectationsHelper("/cart/items/item[@offer-id='1']/@delivery", null)
                .assertBoolean(xml.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8.name(), false);

        new XpathExpectationsHelper("/cart/items/item[@offer-id='2']/@delivery", null)
                .assertBoolean(xml.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8.name(), true);

        new XpathExpectationsHelper("/cart/delivery-options/delivery", null)
                .assertNodeCount(xml.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8.name(), 2);
    }

    @Test
    public void shouldTakePickupCostAndDatesFromItems() {
        Outlet outlet = new Outlet();
        outlet.marketPointId = MARKET_OUTLET_ID;
        outlet.cityRegionId = MOSCOW_REGION_ID;
        outlet.cost = BigDecimal.ONE;
        outlet.minDeliveryDays = 1;
        outlet.minDeliveryDays = 7;

        CartResponse response = new CartResponseBuilder(
                Clock.systemDefaultZone(), new ItemDeliveryOptionsBuilder(Clock.systemDefaultZone())
        ).response(
                MOSCOW_REGION_ID,
                ImmutableMap.of(
                        OfferItemKey.of(SECOND_OFFER_ID, FEED_ID, null), getSecondItem()
                ),
                new Outlet[]{outlet},
                Currency.RUR
        );

        DeliveryResponse deliveryResponse = response.getDeliveryOptions().stream()
                .filter(option -> PICKUP.equals(option.getType()))
                .findFirst().orElseThrow();
        Assertions.assertEquals(REPORT_OUTLET_PRICE, deliveryResponse.getPrice());
        Assertions.assertEquals(
                LocalDate.now().plusDays(REPORT_OUTLET_MIN_DELIVERY_DAYS),
                deliveryResponse.getDeliveryDates().getFromDate());
        Assertions.assertEquals(
                LocalDate.now().plusDays(REPORT_OUTLET_MAX_DELIVERY_DAYS),
                deliveryResponse.getDeliveryDates().getToDate());
    }

    private Item getFirstItem() {
        Item firstItem = new Item();
        firstItem.setCount(0);
        firstItem.setFeedId(FEED_ID);
        firstItem.setOfferId(FIRST_OFFER_ID);
        return firstItem;
    }

    private Item getSecondItem() {
        ItemDeliveryOption option = new ItemDeliveryOption();
        option.setFromDay(0);
        option.setToDay(2);
        option.setPrice(BigDecimal.ONE);
        option.setPaymentMethods(Arrays.asList(PaymentMethod.CASH_ON_DELIVERY, PaymentMethod.CARD_ON_DELIVERY));

        Item secondItem = new Item();
        secondItem.setFeedId(FEED_ID);
        secondItem.setOfferId(SECOND_OFFER_ID);
        secondItem.setCount(1);
        secondItem.setPickupPossible(true);
        secondItem.setDeliveryOptions(ImmutableList.of(option));
        secondItem.setReportOutlets(ImmutableMap.of(
                MARKET_OUTLET_ID, new ReportOutlet(MARKET_OUTLET_ID, REPORT_OUTLET_MIN_DELIVERY_DAYS,
                        REPORT_OUTLET_MAX_DELIVERY_DAYS, Collections.emptySet(), REPORT_OUTLET_PRICE)
        ));
        return secondItem;
    }
}
