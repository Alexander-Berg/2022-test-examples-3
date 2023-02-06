package ru.yandex.direct.jobs.bannersystem.export.util;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.currencies.CurrencyRub;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class BsExportUtilTest {
    @Test
    void testGetBsIsoCurrencyCodeReturnsCode() {
        assertThat("Для валюты ISO-код возвращаем верно",
                BsExportUtil.getBsIsoCurrencyCode(CurrencyCode.RUB),
                equalTo(CurrencyRub.getInstance().getIsoNumCode()));
    }

    @Test
    void testGetBsIsoCurrencyCodeReturnsNegativeCodeForYndFixed() {
        assertThat("Для фишек ISO-код валюты равен -1",
                BsExportUtil.getBsIsoCurrencyCode(CurrencyCode.YND_FIXED),
                equalTo(-1));
    }
}
