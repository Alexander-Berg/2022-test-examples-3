package ru.yandex.market.checkout.pushapi.service.shop.postprocessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryInterval;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryIntervalsCollection;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutlet;
import ru.yandex.market.checkout.checkouter.order.VatType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.pushapi.client.entity.Cart;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.pushapi.service.shop.CartContext;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.util.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;

/**
 * @author gelvy
 * Created on: 24.11.2020
 **/
public class DeliveryIntervalsPostprocessorTest {

    private final DeliveryIntervalsPostprocessor postprocessor = new DeliveryIntervalsPostprocessor();

    private final Cart cart = new Cart();
    private final CartContext cartContext = new CartContext(123);

    @Test
    public void shouldSeparateAndCloneProperlyDeliveryOptionsWithIntervalsForMultipleDates() {
        CartResponse cartResponse = new CartResponse();
        DeliveryResponse deliveryOption = new DeliveryResponse();
        deliveryOption.setId("123");
        deliveryOption.setType(DeliveryType.DELIVERY);
        deliveryOption.setPrice(BigDecimal.TEN);
        deliveryOption.setVat(VatType.VAT_10);
        deliveryOption.setRegionId(213L);
        deliveryOption.setServiceName("Доставка курьером");
        deliveryOption.setPaymentAllow(true);
        deliveryOption.setDeliveryServiceId(1L);
        deliveryOption.setDeliveryPartnerType(DeliveryPartnerType.SHOP);

        ShopOutlet singleOutlet = new ShopOutlet();
        singleOutlet.setId(390L);
        singleOutlet.setCode("111");
        deliveryOption.setOutletId(580L);
        deliveryOption.setOutletCode("222");
        deliveryOption.setOutlet(singleOutlet);

        ShopOutlet outletForCollection = new ShopOutlet();
        outletForCollection.setId(4321L);
        outletForCollection.setCode("333");
        deliveryOption.setOutlets(singletonList(outletForCollection));
        deliveryOption.setOutletIds(singleton(678L));
        deliveryOption.setOutletCodes(singleton("444"));

        AddressImpl address = new AddressImpl();
        address.setPostcode("123");
        deliveryOption.setShopAddress(address);

        deliveryOption.setPaymentOptions(singleton(PaymentMethod.YANDEX));

        Date fromDate = new Date();
        Date toDate = Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        deliveryOption.setDeliveryDates(new DeliveryDates(fromDate, toDate));
        RawDeliveryIntervalsCollection deliveryIntervals = deliveryOption.getRawDeliveryIntervals();
        LocalTime Am9 = LocalTime.of(9, 0);
        LocalTime Pm6 = LocalTime.of(18, 0);
        LocalTime Pm10 = LocalTime.of(22, 0);
        deliveryIntervals.add(new RawDeliveryInterval(fromDate, Am9, Pm6, true));
        deliveryIntervals.add(new RawDeliveryInterval(fromDate, Pm6, Pm10));
        deliveryIntervals.add(new RawDeliveryInterval(toDate, Am9, Pm10));

        cartResponse.setDeliveryOptions(singletonList(deliveryOption));

        postprocessor.process(cart, cartResponse, cartContext);

        List<DeliveryResponse> actualDeliveryOptions = cartResponse.getDeliveryOptions();
        assertThat(actualDeliveryOptions, hasSize(2));
        assertThat(actualDeliveryOptions, everyItem(hasProperty("id", is("123"))));
        assertThat(actualDeliveryOptions, everyItem(hasProperty("type", is(DeliveryType.DELIVERY))));
        assertThat(actualDeliveryOptions, everyItem(hasProperty("price", is(BigDecimal.TEN))));
        assertThat(actualDeliveryOptions, everyItem(hasProperty("vat", is(VatType.VAT_10))));
        assertThat(actualDeliveryOptions, everyItem(hasProperty("regionId", is(213L))));
        assertThat(actualDeliveryOptions, everyItem(hasProperty("serviceName", is("Доставка курьером"))));
        assertThat(actualDeliveryOptions, everyItem(hasProperty("paymentAllow", is(true))));
        assertThat(actualDeliveryOptions, everyItem(hasProperty("deliveryServiceId", is(1L))));
        assertThat(actualDeliveryOptions, everyItem(hasProperty("deliveryPartnerType", is(DeliveryPartnerType.SHOP))));
        assertThat(actualDeliveryOptions, everyItem(hasProperty("outletId", is(580L))));
        assertThat(actualDeliveryOptions, everyItem(hasProperty("outletCode", is("222"))));
        assertThat(actualDeliveryOptions, everyItem(hasProperty("outlet", sameInstance(singleOutlet))));
        assertThat(actualDeliveryOptions, everyItem(hasProperty("outlets", hasSize(1))));
        assertThat(actualDeliveryOptions, everyItem(hasProperty("outlets",
                hasItem(sameInstance(outletForCollection)))));
        assertThat(actualDeliveryOptions, everyItem(hasProperty("outletIds", hasSize(1))));
        assertThat(actualDeliveryOptions, everyItem(hasProperty("outletIds", hasItem(678L))));
        assertThat(actualDeliveryOptions, everyItem(hasProperty("outletCodes", hasSize(1))));
        assertThat(actualDeliveryOptions, everyItem(hasProperty("outletCodes", hasItem("444"))));
        assertThat(actualDeliveryOptions, everyItem(hasProperty("shopAddress", sameInstance(address))));
        assertThat(actualDeliveryOptions, everyItem(hasProperty("paymentOptions", hasSize(1))));
        assertThat(actualDeliveryOptions, everyItem(hasProperty("paymentOptions", hasItem(PaymentMethod.YANDEX))));

        actualDeliveryOptions.sort(Comparator.comparing(o -> o.getDeliveryDates().getFromDate()));
        DeliveryResponse option1 = actualDeliveryOptions.get(0);
        assertThat(option1.getDeliveryDates().getFromDate(), is(fromDate));
        assertThat(option1.getDeliveryDates().getToDate(), is(fromDate));

        RawDeliveryIntervalsCollection intervals1 = option1.getRawDeliveryIntervals();
        assertThat(intervals1.getDates(), hasSize(1));
        assertThat(intervals1.getDates(), hasItem(fromDate));

        Set<RawDeliveryInterval> intervalsByDate1 = intervals1.getIntervalsByDate(fromDate);
        assertThat(intervalsByDate1, hasSize(2));
        assertThat(intervalsByDate1, everyItem(hasProperty("date", is(fromDate))));
        assertThat(intervalsByDate1, hasItem(hasProperty("fromTime", is(Am9))));
        assertThat(intervalsByDate1, hasItem(hasProperty("toTime", is(Pm6))));
        assertThat(intervalsByDate1, hasItem(hasProperty("fromTime", is(Pm6))));
        assertThat(intervalsByDate1, hasItem(hasProperty("toTime", is(Pm10))));
        assertThat(intervalsByDate1, hasItem(hasProperty("default", is(true))));
        assertThat(intervalsByDate1, hasItem(hasProperty("default", is(false))));

        DeliveryResponse option2 = actualDeliveryOptions.get(1);
        assertThat(option2.getDeliveryDates().getFromDate(), is(toDate));
        assertThat(option2.getDeliveryDates().getToDate(), is(toDate));

        RawDeliveryIntervalsCollection intervals2 = option2.getRawDeliveryIntervals();
        assertThat(intervals2.getDates(), hasSize(1));
        assertThat(intervals2.getDates(), hasItem(toDate));

        Set<RawDeliveryInterval> intervalsByDate2 = intervals2.getIntervalsByDate(toDate);
        assertThat(intervalsByDate2, hasSize(1));
        assertThat(intervalsByDate2, hasItem(hasProperty("date", is(toDate))));
        assertThat(intervalsByDate2, hasItem(hasProperty("fromTime", is(Am9))));
        assertThat(intervalsByDate2, hasItem(hasProperty("toTime", is(Pm10))));
        assertThat(intervalsByDate2, hasItem(hasProperty("default", is(false))));
    }

    @Test
    public void shouldNotSeparateDeliveryOptionWithIntervalsForSingleDate() {
        CartResponse cartResponse = new CartResponse();
        DeliveryResponse deliveryOption = new DeliveryResponse();

        Date date = new Date();
        deliveryOption.setDeliveryDates(new DeliveryDates(date, date));
        RawDeliveryIntervalsCollection deliveryIntervals = deliveryOption.getRawDeliveryIntervals();
        LocalTime Am9 = LocalTime.of(9, 0);
        LocalTime Pm6 = LocalTime.of(18, 0);
        LocalTime Pm10 = LocalTime.of(22, 0);
        deliveryIntervals.add(new RawDeliveryInterval(date, Am9, Pm6, true));
        deliveryIntervals.add(new RawDeliveryInterval(date, Pm6, Pm10));

        cartResponse.setDeliveryOptions(singletonList(deliveryOption));

        postprocessor.process(cart, cartResponse, cartContext);

        assertThat(cartResponse.getDeliveryOptions(), hasSize(1));
        assertThat(cartResponse.getDeliveryOptions(), hasItem(deliveryOption));
    }

    @Test
    public void shouldNotSeparateDeliveryOptionWithoutIntervals() {
        CartResponse cartResponse = new CartResponse();
        DeliveryResponse deliveryOption = new DeliveryResponse();

        Date date = new Date();
        deliveryOption.setDeliveryDates(new DeliveryDates(date, date));

        cartResponse.setDeliveryOptions(singletonList(deliveryOption));

        postprocessor.process(cart, cartResponse, cartContext);

        assertThat(cartResponse.getDeliveryOptions(), hasSize(1));
        assertThat(cartResponse.getDeliveryOptions(), hasItem(deliveryOption));
    }

    @Test
    public void shouldProperlySetDeliveryOptionsWithAndWithoutIntervalsForMultipleDates() {
        CartResponse cartResponse = new CartResponse();
        DeliveryResponse deliveryOption1 = new DeliveryResponse();

        Date date = new Date();
        deliveryOption1.setDeliveryDates(new DeliveryDates(date, date));

        DeliveryResponse deliveryOption2 = new DeliveryResponse();
        Date date2 = Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        deliveryOption2.setDeliveryDates(new DeliveryDates(date, date2));
        RawDeliveryIntervalsCollection deliveryIntervals = deliveryOption2.getRawDeliveryIntervals();
        LocalTime Am9 = LocalTime.of(9, 0);
        LocalTime Pm6 = LocalTime.of(18, 0);
        LocalTime Pm10 = LocalTime.of(22, 0);
        deliveryIntervals.add(new RawDeliveryInterval(date, Am9, Pm6));
        deliveryIntervals.add(new RawDeliveryInterval(date2, Am9, Pm10));

        cartResponse.setDeliveryOptions(newArrayList(deliveryOption1, deliveryOption2));

        postprocessor.process(cart, cartResponse, cartContext);

        assertThat(cartResponse.getDeliveryOptions(), hasSize(3));
        assertThat(cartResponse.getDeliveryOptions(), hasItem(deliveryOption1));
        assertThat(cartResponse.getDeliveryOptions(), not(hasItem(deliveryOption2)));
    }
}
