package ru.yandex.market.api.parsers;

import javax.servlet.http.HttpServletRequest;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.common.Result;
import ru.yandex.market.api.error.ValidationError;
import ru.yandex.market.api.integration.ContainerTestBase;
import ru.yandex.market.api.internal.common.GenericParams;
import ru.yandex.market.api.internal.common.GenericParamsParser;

/**
 * Created by fettsery on 18.02.19.
 */
public class GenericParamsParserTest extends ContainerTestBase {
    @Test
    public void testMarketDeliveryOffersOnly() {
        HttpServletRequest request = MockRequestBuilder.start()
            .param("market_delivery_offers_only", "0")
            .build();

        Result<GenericParams, ValidationError> result = new GenericParamsParser().get(request);

        Assert.assertTrue(result.isOk());

        Assert.assertFalse(result.getValue().getMarketDeliveryOffersOnly());
    }

    @Test
    public void testMarketDeliveryOffersOnlyDefault() {
        HttpServletRequest request = MockRequestBuilder.start()
            .build();

        Result<GenericParams, ValidationError> result = new GenericParamsParser().get(request);

        Assert.assertTrue(result.isOk());

        Assert.assertTrue(result.getValue().getMarketDeliveryOffersOnly());
    }

    @Test
    public void beruOrderParamsCustom() {
        HttpServletRequest request = MockRequestBuilder.start()
                .param("beru_order_params_custom", "purchase-referrer=zen_widgets")
                .build();

        Result<GenericParams, ValidationError> result = new GenericParamsParser().get(request);

        Assert.assertTrue(result.isOk());

        Assert.assertEquals("purchase-referrer=zen_widgets", result.getValue().getBeruOrderParamsCustom());
    }

    @Test
    public void bnplInfoSelectedParamsParserTest() {
        HttpServletRequest request = MockRequestBuilder.start()
                .param("bnpl_info_selected", "true")
                .build();

        Result<GenericParams, ValidationError> result = new GenericParamsParser().get(request);

        Assert.assertTrue(result.isOk());

        Assert.assertTrue(result.getValue().getBnplInfoSelected());
    }


    @Test
    public void asyncPaymentCardIdTest() {
        HttpServletRequest request = MockRequestBuilder.start()
                .param("asyncPaymentCardId", "1234")
                .build();

        Result<GenericParams, ValidationError> result = new GenericParamsParser().get(request);

        Assert.assertTrue(result.isOk());

        Assert.assertEquals(result.getValue().getAsyncPaymentCardId(), "1234");
    }
}
