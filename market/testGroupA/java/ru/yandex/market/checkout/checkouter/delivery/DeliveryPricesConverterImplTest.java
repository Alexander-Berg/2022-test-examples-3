package ru.yandex.market.checkout.checkouter.delivery;

import java.math.BigDecimal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.delivery.converter.DeliveryPrices;
import ru.yandex.market.checkout.checkouter.delivery.converter.DeliveryPricesConverterImpl;
import ru.yandex.market.checkout.checkouter.order.PriceConverter;
import ru.yandex.market.checkout.checkouter.order.global.CurrencyConvertService;
import ru.yandex.market.common.report.model.CurrencyConvertResult;

import static ru.yandex.common.util.currency.Currency.EUR;
import static ru.yandex.common.util.currency.Currency.RUR;
import static ru.yandex.common.util.currency.Currency.USD;

@ExtendWith(MockitoExtension.class)
public class DeliveryPricesConverterImplTest {

    private static final BigDecimal HUNDRED_USD_IN_RUR = new BigDecimal(600);
    private static final BigDecimal HUNDRED_USD_IN_EUR = new BigDecimal(75);
    private static final BigDecimal DELIVERY_PRICE = new BigDecimal("100");

    @Mock
    private CurrencyConvertService currencyConvertService;

    @Mock
    private CartOperations cartOperations;

    @InjectMocks
    private DeliveryPricesConverterImpl deliveryPricesConverter;

    private static CurrencyConvertResult buildResult(BigDecimal convertedValue) {
        CurrencyConvertResult currencyConvertResult = new CurrencyConvertResult();
        currencyConvertResult.setConvertedValue(convertedValue);
        return currencyConvertResult;
    }

    @Test
    public void shouldUsePriceConverterIfDeliveryCurrencyEqualToShopCurrency() {
        Mockito.when(cartOperations.convertPriceToBuyer(DELIVERY_PRICE))
                // Эра водолея
                .thenReturn(HUNDRED_USD_IN_RUR);

        DeliveryPrices deliveryPrices = deliveryPricesConverter.convert(DELIVERY_PRICE, USD, USD, RUR, cartOperations);

        Assertions.assertEquals(HUNDRED_USD_IN_RUR, deliveryPrices.getBuyerPrice());
        Assertions.assertEquals(DELIVERY_PRICE, deliveryPrices.getShopPrice());

        Mockito.verifyZeroInteractions(currencyConvertService);
    }

    @Test
    public void shouldUseCurrencyConvertIfDeliveryCurrencyNotEqualToShopCurrency() {
        Currency deliveryCurrency = USD;
        Currency shopCurrency = RUR;
        Currency buyerCurrency = EUR;

        Mockito.when(currencyConvertService.convert(deliveryCurrency, shopCurrency, DELIVERY_PRICE))
                .thenReturn(buildResult(HUNDRED_USD_IN_RUR));
        Mockito.when(currencyConvertService.convert(deliveryCurrency, buyerCurrency, DELIVERY_PRICE))
                .thenReturn(buildResult(HUNDRED_USD_IN_EUR));

        DeliveryPrices deliveryPrices = deliveryPricesConverter.convert(DELIVERY_PRICE, deliveryCurrency,
                shopCurrency, buyerCurrency, cartOperations);
        Assertions.assertEquals(HUNDRED_USD_IN_RUR, deliveryPrices.getShopPrice());
        Assertions.assertEquals(HUNDRED_USD_IN_EUR, deliveryPrices.getBuyerPrice());

        Mockito.verify(cartOperations, Mockito.never()).convertPriceToBuyer(Mockito.any(BigDecimal.class));
    }


    private interface CartOperations extends PriceConverter {

    }


}
