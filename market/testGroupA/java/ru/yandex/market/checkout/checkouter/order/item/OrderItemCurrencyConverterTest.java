package ru.yandex.market.checkout.checkouter.order.item;

import java.math.BigDecimal;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.feature.type.permanent.PermanentCollectionFeatureType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.CurrencyRateInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.viewmodel.OrderViewModel;
import ru.yandex.market.checkout.checkouter.viewmodel.containers.PagedOrderViewModel;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.common.report.model.MarketReportPlace;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.report.Experiments.FORCE_REGION_OPTIONS;

public class OrderItemCurrencyConverterTest extends AbstractWebTestBase {

    @Autowired
    protected WireMockServer reportMockWhite;

    @Autowired
    private ObjectMapper checkouterAnnotationObjectMapper;

    @Test
    public void testNullForeignCurrencyPriceForRur() {
        Order order = orderCreateHelper.createOrder(new Parameters());
        assertNotNull(order.getCurrencyRate());
        assertEquals(Currency.RUR, order.getCurrencyRate().getCurrency());
        assertEquals(BigDecimal.ONE, order.getCurrencyRate().getRate());
    }

    @Test
    public void testEmptyCisListForKz() {
        long ksRegion = 163L;

        Parameters parameters = new Parameters();
        parameters.getOrder().getDelivery().setRegionId(ksRegion);
        parameters.getReportParameters().setRegionId(ksRegion);
        parameters.getBuiltMultiCart().setBuyerRegionId(ksRegion);

        Order order = orderCreateHelper.createOrder(parameters);
        assertNotNull(order.getCurrencyRate());
        assertEquals(Currency.RUR, order.getCurrencyRate().getCurrency());
        assertEquals(BigDecimal.ONE, order.getCurrencyRate().getRate());

        CurrencyRateInfo currencyRateInfo = orderService.getOrder(order.getId())
                .getProperty(OrderPropertyType.CURRENCY_RATE);
        assertNull(currencyRateInfo);
    }

    @Test
    public void testAddedCurrenciesPrices() {
        checkouterFeatureWriter.writeValue(PermanentCollectionFeatureType.CIS_COUNTRIES, Set.of(149L, 159L));
        mockReportCurrencyResponse();

        long ksRegion = 163L;

        Parameters parameters = new Parameters();
        parameters.getOrder().getDelivery().setRegionId(ksRegion);
        parameters.getReportParameters().setRegionId(ksRegion);
        parameters.getBuiltMultiCart().setBuyerRegionId(ksRegion);

        Order order = orderCreateHelper.createOrder(parameters);
        assertNotNull(order.getCurrencyRate());
        assertEquals(Currency.KZT, order.getCurrencyRate().getCurrency());
        assertEquals(BigDecimal.valueOf(100), order.getCurrencyRate().getRate());

        CurrencyRateInfo currencyRateInfo = orderService.getOrder(order.getId())
                .getProperty(OrderPropertyType.CURRENCY_RATE);
        assertNotNull(currencyRateInfo);
        assertEquals(Currency.KZT, currencyRateInfo.getCurrency());
        assertEquals(BigDecimal.valueOf(100), currencyRateInfo.getRate());
    }

    @Test
    public void testCartWithCurrencyRate() {
        checkouterFeatureWriter.writeValue(PermanentCollectionFeatureType.CIS_COUNTRIES, Set.of(149L, 159L));
        mockReportCurrencyResponse();

        long ksRegion = 163L;

        Parameters parameters = new Parameters();

        parameters.getOrder().getDelivery().setRegionId(ksRegion);
        parameters.getReportParameters().setRegionId(ksRegion);
        parameters.getBuiltMultiCart().setBuyerRegionId(ksRegion);

        MultiCart cart = orderCreateHelper.cart(parameters);
        CurrencyRateInfo currencyRate = cart.getCarts().get(0).getCurrencyRate();
        assertNotNull(currencyRate);
        assertEquals(Currency.KZT, currencyRate.getCurrency());
        assertEquals(BigDecimal.valueOf(100), currencyRate.getRate());
    }

    @Test
    public void testExperimentValueToGetKZNCurrencyRate() {
        checkouterFeatureWriter.writeValue(PermanentCollectionFeatureType.CIS_COUNTRIES, Set.of(149L, 159L));
        mockReportCurrencyResponse();

        Parameters parameters = new Parameters();
        parameters.configuration().cart().request()
                .setExperiments(String.format("%s=%d", FORCE_REGION_OPTIONS, 159)); // Указание страны Казахстан

        long ksRegion = 163L; // Город из Казахстана

        parameters.getOrder().getDelivery().setRegionId(216L); // Зеленоград
        parameters.getReportParameters().setRegionId(ksRegion);
        parameters.getBuiltMultiCart().setBuyerRegionId(ksRegion);

        MultiCart cart = orderCreateHelper.cart(parameters);
        CurrencyRateInfo currencyRate = cart.getCarts().get(0).getCurrencyRate();
        assertNotNull(currencyRate);
        assertEquals(Currency.KZT, currencyRate.getCurrency());
        assertEquals(BigDecimal.valueOf(100), currencyRate.getRate());
    }

    @Test
    public void testPagedOrder() throws Exception {
        checkouterFeatureWriter.writeValue(PermanentCollectionFeatureType.CIS_COUNTRIES, Set.of(149L, 159L));
        mockReportCurrencyResponse();

        long ksRegion = 163L;

        Parameters parameters = new Parameters();

        parameters.getOrder().getDelivery().setRegionId(ksRegion);
        parameters.getReportParameters().setRegionId(ksRegion);
        parameters.getBuiltMultiCart().setBuyerRegionId(ksRegion);

        orderCreateHelper.createMultiOrder(parameters);

        PagedOrderViewModel ordersByUID = getOrdersByUID();
        assertEquals(1, ordersByUID.getItems().size());
        OrderViewModel orderViewModel = ordersByUID.getItems().iterator().next();
        CurrencyRateInfo currencyRate = orderViewModel.getCurrencyRate();
        assertNotNull(currencyRate);
        assertEquals(Currency.KZT, currencyRate.getCurrency());
        assertEquals(BigDecimal.valueOf(100), currencyRate.getRate());
    }

    private void mockReportCurrencyResponse() {
        MappingBuilder builder = get(urlPathEqualTo("/yandsearch"))
                .withQueryParam("place", equalTo(MarketReportPlace.CURRENCY_CONVERT.getId()))
                .withQueryParam("currency-from", equalTo(Currency.RUR.name()))
                .withQueryParam("currency-to", equalTo(Currency.KZT.name()))
                .withQueryParam("currency-value", equalTo(BigDecimal.ONE.toString()));

        JSONObject result = new JSONObject();
        result.put("currencyFrom", Currency.RUR);
        result.put("currencyTo", Currency.KZT);
        result.put("value", BigDecimal.valueOf(100));
        result.put("convertedValue", BigDecimal.valueOf(100));
        result.put("renderedValue", BigDecimal.valueOf(100));
        result.put("renderedConvertedValue", BigDecimal.valueOf(100));

        reportMockWhite.stubFor(builder.willReturn(aResponse().withBody(result.toString())));
    }

    private PagedOrderViewModel getOrdersByUID() throws Exception {
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get("/orders/by-uid/{uid}", BuyerProvider.UID)
                .param(CheckouterClientParams.RGB, Color.BLUE.name(), Color.WHITE.name());

        ResultActions resultActions = mockMvc.perform(builder)
                .andExpect(jsonPath("$.orders")
                        .value(hasSize(1)))
                .andExpect(status().isOk());

        String contentAsString = resultActions.andReturn().getResponse().getContentAsString();

        return checkouterAnnotationObjectMapper.readValue(contentAsString, PagedOrderViewModel.class);
    }
}
