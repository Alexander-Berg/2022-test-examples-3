package ru.yandex.market.shopadminstub.stub;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.apache.commons.validator.Arg;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.helpers.CartHelper;
import ru.yandex.market.helpers.CartParameters;
import ru.yandex.market.helpers.OrderAcceptHelper;
import ru.yandex.market.helpers.OrderAcceptParameters;
import ru.yandex.market.providers.CartRequestProvider;
import ru.yandex.market.providers.ItemProvider;
import ru.yandex.market.providers.OrderAcceptRequestProvider;
import ru.yandex.market.shopadminstub.application.AbstractTestBase;
import ru.yandex.market.shopadminstub.model.CartRequest;
import ru.yandex.market.shopadminstub.model.Item;
import ru.yandex.market.shopadminstub.model.OrderAcceptRequest;
import ru.yandex.market.util.report.CurrencyConvertConfigurer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

public class StubPushApiTest extends AbstractTestBase {

    @Autowired
    private CartHelper cartHelper;
    @Autowired
    private OrderAcceptHelper orderAcceptHelper;
    @Autowired
    private CurrencyConvertConfigurer currencyConvertConfigurer;

    @Test
    @Disabled("Тест работал потому что поле price не сериализовалось." +
            "В коде count не обнуляется, если оффер не найден в репорте")
    public void testReturnsCount0IfNotFound() throws Exception {
        CartParameters cartParameters = new CartParameters(CartRequestProvider.buildCartRequest(
                ItemProvider.buildDefaultItem(),
                ItemProvider.buildItem(ItemProvider.DEFAULT_FEED_ID, ItemProvider.ANOTHER_OFFER_ID)
        ));
        cartParameters.getReportParameters().setHiddenItems(Collections.singleton(ItemProvider.DEFAULT_FEED_OFFER_ID));

        cartHelper.cart(cartParameters)
                .andDo(log())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(xpath("/cart/@delivery-currency").string("RUR"))
                .andExpect(xpath("/cart/items/item[@offer-id='%s']", ItemProvider.DEFAULT_OFFER_ID).exists())
                .andExpect(xpath("/cart/items/item[@offer-id='%s']/@feed-id", ItemProvider.DEFAULT_OFFER_ID).string
                        (String.valueOf(ItemProvider.DEFAULT_FEED_ID)))
                .andExpect(xpath("/cart/items/item[@offer-id='%s']/@delivery", ItemProvider.DEFAULT_OFFER_ID)
                        .booleanValue(false))
                .andExpect(xpath("/cart/items/item[@offer-id='%s']/@count", ItemProvider.DEFAULT_OFFER_ID).number(0d));
    }


    @Test
    @Disabled("Тест работал потому что поле price не сериализовалось." +
            "В коде по условию, что в ответе репорта cpa = no, нет обнуления count")
    public void shouldReturnCount0IfNotCpa() throws Exception {
        Item item = ItemProvider.buildDefaultItem();
        item.setCpa("no");

        CartRequest cartRequest = CartRequestProvider.buildCartRequest(item);

        CartParameters cartParameters = new CartParameters(cartRequest);

        cartHelper.cart(cartParameters)
                .andDo(log())
                .andExpect(status().isOk())
                .andExpect(xpath("/cart/items/item/@count").number(0d));
    }

    @Test
    public void testSandboxCart() throws Exception {
        Item item = ItemProvider.buildDefaultItem();
        CartRequest cartRequest = CartRequestProvider.buildCartRequest(item);
        cartRequest.setContext(Context.SANDBOX);

        CartParameters cartParameters = new CartParameters(cartRequest);

        cartHelper.cart(cartParameters)
                .andDo(log())
                .andExpect(status().isOk());
    }

    @Test
    public void testSandboxCartShouldNotCallStockStorage() throws Exception {
        Item item = ItemProvider.buildDefaultItem();
        item.setFulfilmentShopId(123L);
        item.setShopSku("ssku");

        CartRequest cartRequest = CartRequestProvider.buildCartRequest(item);
        cartRequest.setContext(Context.SANDBOX);
        cartRequest.setFulfilment(true);

        CartParameters cartParameters = new CartParameters(cartRequest);

        cartHelper.cart(cartParameters)
                .andDo(log())
                .andExpect(status().isOk());
    }

    @Test
    public void testSandboxCartShouldCallReportWithHasGone() throws Exception {
        Item item = ItemProvider.buildDefaultItem();
        item.setFulfilmentShopId(123L);
        item.setShopSku("ssku");

        CartRequest cartRequest = CartRequestProvider.buildCartRequest(item);
        cartRequest.setContext(Context.SANDBOX);
        cartRequest.setFulfilment(true);

        CartParameters cartParameters = new CartParameters(cartRequest);

        cartHelper.cart(cartParameters)
                .andDo(log())
                .andExpect(status().isOk());

        assertReportCalledWithHasGone();
    }

    private void assertReportCalledWithHasGone() {
        List<ServeEvent> serveEvents = cartHelper.getReportEvents();
        assertThat(serveEvents, CoreMatchers.not(empty()));
        assertThat(serveEvents.size(), CoreMatchers.is(3));

        QueryParameter queryParameter = serveEvents.stream()
                .map(event -> event.getRequest().getQueryParams().get("ignore-has-gone"))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("report not called!"));

        assertThat(queryParameter, CoreMatchers.not(nullValue()));
        assertThat(queryParameter.values().iterator().next(), CoreMatchers.is("1"));
    }

    @Test
    public void testNormalCartShouldNotCallReportWithHasGone() throws Exception {
        Item item = ItemProvider.buildDefaultItem();
        item.setFulfilmentShopId(123L);
        item.setShopSku("ssku");

        CartRequest cartRequest = CartRequestProvider.buildCartRequest(item);
        cartRequest.setContext(Context.MARKET);
        cartRequest.setFulfilment(true);

        CartParameters cartParameters = new CartParameters(cartRequest);

        cartHelper.cart(cartParameters)
                .andDo(log())
                .andExpect(status().isOk());

        List<ServeEvent> serveEvents = cartHelper.getReportEvents();
        assertThat(serveEvents, CoreMatchers.not(empty()));
        assertThat(serveEvents.size(), CoreMatchers.is(3));

        QueryParameter queryParameter = serveEvents.stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("report not called!"))
                .getRequest().getQueryParams().get("ignore-has-gone");

        assertThat(queryParameter, CoreMatchers.is(nullValue()));
    }


    @Test
    public void testNormalCartShouldCallStockStorage() throws Exception {
        Item item = ItemProvider.buildDefaultItem();
        item.setFulfilmentShopId(123L);
        item.setShopSku("ssku");

        CartRequest cartRequest = CartRequestProvider.buildCartRequest(item);
        cartRequest.setContext(Context.MARKET);
        cartRequest.setFulfilment(true);

        CartParameters cartParameters = new CartParameters(cartRequest);

        cartHelper.cart(cartParameters)
                .andDo(log())
                .andExpect(status().isOk());
    }


    @Test
    public void testCartWithCertificateShouldNotCallStockStorage() throws Exception {
        Item item = ItemProvider.buildDefaultItem();
        item.setFulfilmentShopId(123L);
        item.setShopSku("ssku");
        item.setPrice(BigDecimal.ZERO);

        CartRequest cartRequest = CartRequestProvider.buildCartRequest(item);
        cartRequest.setContext(Context.MARKET);
        cartRequest.setFulfilment(true);
        cartRequest.setHasCertificate(true);

        CartParameters cartParameters = new CartParameters(cartRequest);

        cartParameters.getReportParameters().setOfferModifier(foundOffer -> {
            foundOffer.setPrice(new BigDecimal(100.));
            foundOffer.setShopPrice(new BigDecimal(100.));
        });
        cartHelper.cart(cartParameters)
                .andDo(log())
                .andExpect(status().isOk())
                .andExpect(xpath("/cart/items/item/@count").number(1d));
        assertReportCalledWithHasGone();
    }

    @Test
    public void testCartWithCertificateShouldNotSetCountToZero() throws Exception {
        Item item = ItemProvider.buildDefaultItem();
        item.setFulfilmentShopId(123L);
        item.setShopSku("ssku");
        item.setPrice(BigDecimal.ZERO);

        CartRequest cartRequest = CartRequestProvider.buildCartRequest(item);
        cartRequest.setContext(Context.MARKET);
        cartRequest.setFulfilment(true);
        cartRequest.setHasCertificate(true);

        CartParameters cartParameters = new CartParameters(cartRequest);
        cartParameters.getReportParameters().setHiddenItems(Collections.singleton(ItemProvider.DEFAULT_FEED_OFFER_ID));

        cartHelper.cart(cartParameters)
                .andDo(log())
                .andExpect(status().isOk())
                .andExpect(xpath("/cart/items/item/@count").number(1d));

        assertReportCalledWithHasGone();
    }

    @Test
    public void testEmptyContextCartShouldCallStockStorage() throws Exception {
        Item item = ItemProvider.buildDefaultItem();
        item.setFulfilmentShopId(123L);
        item.setShopSku("ssku");

        CartRequest cartRequest = CartRequestProvider.buildCartRequest(item);
        cartRequest.setFulfilment(true);

        CartParameters cartParameters = new CartParameters(cartRequest);

        cartHelper.cart(cartParameters)
                .andDo(log())
                .andExpect(status().isOk());
    }

    @Test
    public void testShouldNotFailOnGlobal() throws Exception {
        CartRequest cartRequest = CartRequestProvider.buildCartRequest(
                ItemProvider.buildGlobalItem()
        );
        CartParameters cartParameters = new CartParameters(10217455L, cartRequest);
        cartParameters.getReportGeoParameters().setResourceUrl(ItemProvider.DEFAULT_GLOBAL_WARE_MD5, Collections
                .emptyList());

        cartHelper.cart(cartParameters)
                .andDo(log())
                .andExpect(status().isOk());
    }

    @Test
    public void testShouldReturnBadRequestIfRegionIdInvalid() throws Exception {
        CartRequest cartRequest = CartRequestProvider.buildCartRequest();
        //set invalid region id
        cartRequest.setRegionId(9999999);
        CartParameters cartParameters = new CartParameters(cartRequest);
        cartHelper.cart(cartParameters)
                .andDo(log())
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void shouldReturnOkIfFreezed() throws Exception {
        OrderAcceptParameters orderAcceptParameters = new OrderAcceptParameters(
                OrderAcceptRequestProvider.buildOrderAcceptRequest(
                        ItemProvider.buildFulfilmentItem()
                )
        );
        orderAcceptParameters.getOrderAcceptRequest().setFulfilment(true);

        orderAcceptHelper.orderAccept(orderAcceptParameters)
                .andExpect(status().isOk())
                .andExpect(xpath("/order/@accepted").booleanValue(true));
    }

    @ParameterizedTest
    @MethodSource("testShipmentDateCalculation")
    public void testShipmentDateCorrectCalculation(String requestSource, String expectedDate) throws Exception {
        String requestBody = IOUtils.readInputStream(StubPushApiTest.class.getResourceAsStream(requestSource));

        mockMvc.perform(
                post("/{shopId}/order/accept", 123456)
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_XML))
                .andExpect(status().isOk())
                .andExpect(xpath("/order/@accepted").booleanValue(true))
                .andExpect(xpath("/order/@shipment-date").string(expectedDate));
    }

    @Test
    public void shouldCallFreezeWhenPreorder() throws Exception {
        OrderAcceptRequest orderAcceptRequest = OrderAcceptRequestProvider.buildOrderAcceptRequest(
                ItemProvider.buildFulfilmentItem()
        );
        orderAcceptRequest.setFulfilment(true);
        orderAcceptRequest.setPreorder(true);
        OrderAcceptParameters orderAcceptParameters = new OrderAcceptParameters(orderAcceptRequest);

        orderAcceptHelper.orderAccept(orderAcceptParameters)
                .andExpect(status().isOk())
                .andExpect(xpath("/order/@accepted").booleanValue(true));
    }

    @Test
    public void shouldReturnNotAcceptedIfEmptyItems() throws Exception {
        OrderAcceptRequest request = OrderAcceptRequestProvider.buildOrderAcceptRequest(new Item[0]);
        request.setFulfilment(true);
        OrderAcceptParameters orderAcceptParameters = new OrderAcceptParameters(request);

        orderAcceptHelper.orderAccept(orderAcceptParameters)
                .andExpect(status().isOk())
                .andExpect(xpath("/order/@accepted").booleanValue(false));
    }

    private static Stream<Arguments> testShipmentDateCalculation() {
        return Stream.of(
                Arguments.of("/data/testShipment_toLocal_withoutHour.xml", "27-03-2021"),
                Arguments.of("/data/testShipment_toLocal_withHour.xml", "31-03-2021"),
                Arguments.of("/data/testShipment_toNonLocal_withHour.xml", "04-04-2021"),
                Arguments.of("/data/testShipment_toNonLocal_withoutHour.xml", "05-04-2021"),
                Arguments.of("/data/testShipment_toNonLocal_afterDelivery.xml", "06-04-2021"),
                Arguments.of("/data/testShipment_toExactLocal_withoutHour.xml", "06-04-2021"),
                Arguments.of("/data/testShipment_noHolidaysInRequestToStub.xml", "05-04-2021")
        );
    }

}
