package ru.yandex.market.api.common.currency;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.domain.v2.ModelPriceV2;
import ru.yandex.market.api.domain.v2.OfferPriceV2;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.model.Prices;
import ru.yandex.market.api.offer.Price;

import java.math.BigDecimal;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class PriceConverterTest extends UnitTestBase {

    private static final Function<String, String> CONVERTER = s ->  s + "0";

    @Test
    public void shouldConvertPricesCorrect() throws Exception {
        BigDecimal result = PriceConverter.convertWithRate(BigDecimal.valueOf(12345, 2), BigDecimal.valueOf(25, 1));
        assertEquals(BigDecimal.valueOf(30863, 2), result);

        result = PriceConverter.convertWithRate(BigDecimal.valueOf(10), BigDecimal.valueOf(2));
        assertEquals(BigDecimal.valueOf(2000, 2), result);
    }

    @Test
    public void shouldFillPriceWithCents() throws Exception {
        String result = PriceConverter.convertWithRate("1", BigDecimal.valueOf(2));
        assertEquals("2", result);

        result = PriceConverter.convertWithRate("1.1", BigDecimal.valueOf(2));
        assertEquals("2.20", result);

        result = PriceConverter.convertWithRate("1.11", BigDecimal.valueOf(2));
        assertEquals("2.22", result);
    }

    @Test
    public void shouldConvertModelPriceV2() throws Exception {
        ModelPriceV2 price = new ModelPriceV2();
        price.setBase("100");
        price.setDiscount("50");
        price.setMax("300");
        price.setMin("45");
        price.setAvg("52");

        ModelPriceV2 result = PriceConverter.convert(price, CONVERTER);
        assertEquals("1000", result.getBase());
        assertEquals("50", result.getDiscount());
        assertEquals("3000", result.getMax());
        assertEquals("450", result.getMin());
        assertEquals("520", result.getAvg());
    }

    @Test
    public void shouldConvertOfferPriceV2() throws Exception {
        OfferPriceV2 price =  new OfferPriceV2();
        price.setValue("50");
        price.setBase("100");
        price.setDiscount("50");
        price.setShopMax("200");
        price.setShopMin("42");

        OfferPriceV2 result = PriceConverter.convert(price, CONVERTER);
        assertEquals("500", result.getValue());
        assertEquals("1000", result.getBase());
        assertEquals("50", result.getDiscount());
        assertEquals("2000", result.getShopMax());
        assertEquals("420", result.getShopMin());
    }


    @Test
    public void shouldConvertOldPrice() throws Exception {
        Price price = new Price();
        price.setValue("50");
        price.setBase("100");
        price.setDiscount("50");
        price.setMaxValue("200");
        price.setMinValue("42");
        price.setCurrencyCode("RUR");
        price.setCurrencyName("тест-руб.");

        Price result = PriceConverter.convert(price, CONVERTER, Currency.BYR);
        assertEquals("500", result.getValue());
        assertEquals("1000", result.getBase());
        assertEquals("50", result.getDiscount());
        assertEquals("2000", result.getMaxValue());
        assertEquals("420", result.getMinValue());
        assertEquals("BYR", result.getCurrencyCode());
        assertEquals("б.р.", result.getCurrencyName());
    }

    @Test
    public void shouldConvertOldPrices() throws Exception {
        Prices prices = new Prices();
        prices.setBase("100");
        prices.setDiscount("50");
        prices.setMax("200");
        prices.setMin("42");
        prices.setAvg("55");
        prices.setCurCode("RUR");
        prices.setCurName("Doesnt matter");

        Prices result = PriceConverter.convert(prices, CONVERTER, Currency.BYR);
        assertEquals("1000", result.getBase());
        assertEquals("50", result.getDiscount());
        assertEquals("2000", result.getMax());
        assertEquals("420", result.getMin());
        assertEquals("550", result.getAvg());
        assertEquals("BYR", result.getCurCode());
        assertEquals("б.р.", result.getCurName());
    }
}
