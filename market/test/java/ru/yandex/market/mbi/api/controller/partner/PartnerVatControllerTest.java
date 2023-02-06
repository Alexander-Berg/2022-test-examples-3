package ru.yandex.market.mbi.api.controller.partner;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.core.tax.model.TaxSystem;
import ru.yandex.market.core.tax.model.VatRate;
import ru.yandex.market.core.tax.model.VatSource;
import ru.yandex.market.mbi.api.client.entity.vat.VatInfo;
import ru.yandex.market.mbi.api.config.FunctionalTest;

/**
 * Функциональные тесты для {@link PartnerVatController}.
 */
@DbUnitDataSet(before = "PartnerVatControllerTest.before.csv")
public class PartnerVatControllerTest extends FunctionalTest {

    @Test
    @DisplayName("информация о налогообложении партнера находится по идентификатору")
    void getPartnerVatInfoTest() {
        VatInfo expectedPartnerVat = new VatInfo(
                100,
                TaxSystem.USN,
                VatRate.VAT_10,
                VatSource.WEB_AND_FEED,
                VatRate.NO_VAT);

        VatInfo partnerVatInfo = mbiApiClient.getPartnerVat(100L);

        ReflectionAssert.assertReflectionEquals(expectedPartnerVat, partnerVatInfo);
    }

    @Test
    @DisplayName("возвращается ошибка если в теле и в path partnerId  при обновлении расходятся")
    void updatePartnerVatInfoNotMatchTest() {
        VatInfo shopVat = new VatInfo();
        shopVat.setPartnerId(102L);
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.updatePartnerVatInfo(101L, shopVat, 12345L)
        );
        MatcherAssert.assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)
        );
    }

    @Test
    @DisplayName("возвращается ошибка при попытке найти информацию о налогообложении несуществующего партнера")
    void updatePartnerVatInfoNotFoundTest() {
        VatInfo expectedPartnerVat = new VatInfo(
                101L,
                TaxSystem.USN_MINUS_COST,
                VatRate.NO_VAT,
                VatSource.WEB,
                VatRate.VAT_10);
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.updatePartnerVatInfo(101L, expectedPartnerVat, 12345L)
        );
        MatcherAssert.assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.NOT_FOUND)
        );

    }

    @Test
    @DisplayName("обновляется информация о налогообложении партнера")
    @DbUnitDataSet(after = "PartnerVatControllerTest.after.csv")
    void updatePartnerVatInfoTest() {
        VatInfo expectedPartnerVat = new VatInfo(
                100,
                TaxSystem.USN_MINUS_COST,
                VatRate.NO_VAT,
                VatSource.WEB,
                VatRate.VAT_10);

        mbiApiClient.updatePartnerVatInfo(100L, expectedPartnerVat, 12345L);
    }
}
