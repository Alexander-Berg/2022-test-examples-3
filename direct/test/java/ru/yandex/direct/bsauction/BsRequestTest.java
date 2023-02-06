package ru.yandex.direct.bsauction;

import org.junit.Test;

import ru.yandex.direct.currency.CurrencyCode;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class BsRequestTest {
    @Test
    public void YndFixedCurrencyIsHandledCorrectly() {
        BsRequest<BsRequestPhrase> request = new BsRequest<>();
        request.withCurrency(CurrencyCode.YND_FIXED.getCurrency());
        String url = request.getUrlBuilder("http://ya.ru").build();
        assertThat(url, not(containsString("currency")));
    }
}
