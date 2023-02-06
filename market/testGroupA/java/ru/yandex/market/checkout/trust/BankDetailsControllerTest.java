package ru.yandex.market.checkout.trust;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.pay.BankDetails;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;

public class BankDetailsControllerTest extends AbstractWebTestBase {

    @Autowired
    private WireMockServer refsMock;

    @Epic(Epics.BANK_DETAILS)
    @Story(Stories.GET_BANK_DETAILS_BY_BIK)
    @DisplayName("Получаем реквизиты банка по БИКу")
    @Test
    public void getBankDetailsByBikPositiveCase() {
        mockRefsResponse("044525728", "ООО \\\"СПЕЦСТРОЙБАНК\\\"", "30101810045250000728", "МОСКВА");

        BankDetails bd = client.loadBankDetailsByBik("044525728");
        assertThat(bd, notNullValue());
        assertThat(bd.getBik(), equalTo("044525728"));
        assertThat(bd.getBankCity(), equalTo("МОСКВА"));
        assertThat(bd.getCorraccount(), equalTo("30101810045250000728"));
        assertThat(bd.getBank(), equalTo("ООО \"СПЕЦСТРОЙБАНК\""));
    }

    @Epic(Epics.BANK_DETAILS)
    @Story(Stories.GET_BANK_DETAILS_BY_BIK)
    @DisplayName("Ошибка при получении реквизитов банка из-за невалидного ответа от траста")
    @Test
    public void getBankDetailsByBikWrongResponse() {
        Assertions.assertThrows(ErrorCodeException.class, () -> {
            client.loadBankDetailsByBik("123456789");
        });
    }

    @Epic(Epics.BANK_DETAILS)
    @Story(Stories.GET_BANK_DETAILS_BY_BIK)
    @DisplayName("Ошибка при получении реквизитов банка из-за невалидного БИК")
    @Test
    public void getBankDetailsByBikWithLessDigits() {
        Assertions.assertThrows(ErrorCodeException.class, () -> {
            client.loadBankDetailsByBik("12345678");
        });
    }

    @Epic(Epics.BANK_DETAILS)
    @Story(Stories.GET_BANK_DETAILS_BY_BIK)
    @DisplayName("Ошибка при получении реквизитов банка из-за неполного ответа от траста (не все поля)")
    @Test
    public void getBankDetailsByBikNotAllFieldsInResponse() {
        Assertions.assertThrows(ErrorCodeException.class, () -> {
            MappingBuilder builder = post(urlPathEqualTo("/cbrf"))
                    .withRequestBody(containing("bic:\\\"044525728\\\""))
                    .willReturn(aResponse().withBody("{\"data\":{\"banks\":[{\"nameFull\":\"ООО " +
                            "\\\"СПЕЦСТРОЙБАНК\\\"\"," +
                            "\"regionCode\":\"45\",\"corr\":\"30101810045250000728\",\"bic\":\"044525728\"}]}}"));
            refsMock.stubFor(builder);

            client.loadBankDetailsByBik("044525728");
        });
    }


    @Epic(Epics.BANK_DETAILS)
    @Story(Stories.GET_BANK_DETAILS_BY_BIK)
    @DisplayName("Проверка БИКа и рассчетного счета")
    @Test
    public void getBankDetailsAndValidateAccount() {
        mockRefsResponse("044525225", "Сбербанк России", "30101810400000000225", "МОСКВА");

        BankDetails bd = client.loadBankDetails("044525225", "40817810238091513888");
        assertThat(bd, notNullValue());
        assertThat(bd.getBik(), equalTo("044525225"));
        assertThat(bd.getAccount(), equalTo("40817810238091513888"));
    }

    @Epic(Epics.BANK_DETAILS)
    @Story(Stories.GET_BANK_DETAILS_BY_BIK)
    @DisplayName("Неправильный рассчетный счет (не принадлежит банку)")
    @Test
    public void getBankDetailsWithInvalidAccount() {
        Assertions.assertThrows(ErrorCodeException.class, () -> {
            mockRefsResponse("044525225", "Сбербанк России", "30101810400000000225", "МОСКВА");

            client.loadBankDetails("044525225", "40817810238091513889");
        });
    }

    @Epic(Epics.BANK_DETAILS)
    @Story(Stories.GET_BANK_DETAILS_BY_BIK)
    @DisplayName("Неправильный рассчетный счет (неверная длина)")
    @Test
    public void getBankDetailsWithShortAccount() {
        Assertions.assertThrows(ErrorCodeException.class, () -> {
            mockRefsResponse("044525225", "Сбербанк России", "30101810400000000225", "МОСКВА");

            client.loadBankDetails("044525225", "4081781023809151388");
        });
    }

    @Epic(Epics.BANK_DETAILS)
    @Story(Stories.GET_BANK_DETAILS_BY_BIK)
    @DisplayName("Неправильный рассчетный счет (содержит буквы)")
    @Test
    public void getBankDetailsWithNonDigitAccount() {
        Assertions.assertThrows(ErrorCodeException.class, () -> {
            mockRefsResponse("044525225", "Сбербанк России", "30101810400000000225", "МОСКВА");

            client.loadBankDetails("044525225", "s081781023809151388a");
        });
    }

    @Epic(Epics.BANK_DETAILS)
    @Story(Stories.GET_BANK_DETAILS_BY_BIK)
    @DisplayName("Неправильный БИК (содержит буквы)")
    @Test
    public void getBankDetailsWithNonDigitBik() {
        Assertions.assertThrows(ErrorCodeException.class, () -> {
            mockRefsResponse("044525225", "Сбербанк России", "30101810400000000225", "МОСКВА");

            client.loadBankDetails("ff4525225", "4081781023809151388");
        });
    }

    private void mockRefsResponse(String bic, String bankName, String corrAccount, String city) {
        MappingBuilder builder = post(urlPathEqualTo("/cbrf"))
                .withRequestBody(containing("bic:\"" + bic + "\""))
                .willReturn(aResponse().withBody(
                        "{\"data\":{\"banks\":[{\"name\":\"" + bankName + "\"," +
                                "\"nameFull\":\"" + bankName + "\"," +
                                "\"regionCode\":\"45\"," +
                                "\"corr\":\"" + corrAccount + "\"," +
                                "\"bic\":\"" + bic + "\"," +
                                "\"place\":\"" + city + "\"," +
                                "\"term\":\"1\"," +
                                "\"controlCode\":\"\"," +
                                "\"swift\":\"\"," +
                                "\"checksum\":\"0eda283da2528cbef786444c9010f668\"}]}}"));
        refsMock.stubFor(builder);
    }
}
