package ru.yandex.autotests.market.partner.backend.tests.payment;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.market.common.http.request.BackendRequest;
import ru.yandex.autotests.market.partner.backend.util.query.payment.AutoPaymentMethodsDto;
import ru.yandex.autotests.market.partner.backend.util.query.payment.AutoPaymentMethodsRequest;
import ru.yandex.autotests.market.partner.backend.util.query.payment.BoundPaymentMethodDto;
import ru.yandex.autotests.market.partner.backend.util.query.payment.PaymentPersonDTO;
import ru.yandex.autotests.market.partner.backend.util.query.payment.PersonBoundPaymentMethodDto;
import ru.yandex.qatools.allure.annotations.Features;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.market.partner.backend.tests.util.JsonTestUtil.getString;

@Aqua.Test(title = "Тесты на настройки авто пополнения баланса (ручки /auto-payment/*) ")
@Features("Тесты на настройки авто пополнения баланса (ручки /auto-payment/*) ")
public class AutoPaymentTest {
    private static final long AUTO_PAYMENT_CAMPAIGN_ID = 1000510155;
    private static final long AUTO_PAYMENT_UID = 404744950;

    @Ignore("Отключено здесь: https://st.yandex-team.ru/MBI-74989")
    @Test
    public void getAutoAutoPaymentMethods() {
        Gson g = new Gson();
        String expectedJson = getString(getClass(), "json/getAutoPaymentMethodsTest.json");
        AutoPaymentMethodsDto expected = g.fromJson(expectedJson, AutoPaymentMethodsDto.class);
        AutoPaymentMethodsDto actual = new AutoPaymentMethodsRequest()
                .method(BackendRequest.Method.GET)
                .withUserId(AUTO_PAYMENT_UID)
                .setQueryParameter("campaign_id", String.valueOf(AUTO_PAYMENT_CAMPAIGN_ID))
                .sendAndExtract();


        Map<Long, PaymentPersonDTO> expectedPersons = expected.getPersons().stream()
                .collect(Collectors.toMap(PaymentPersonDTO::getId, Function.identity()));

        Map<String, PaymentPersonDTO> actualPersons = actual.getPersons().stream()
                .collect(Collectors.toMap(PaymentPersonDTO::getName, Function.identity()));
        expected.getPersonMethods().forEach(pm -> setPersonId(expectedPersons, actualPersons, pm));
        expected.getPersons().forEach(p -> p.setId(actualPersons.get(p.getName()).getId()));
        fixCardIds(actual.getMethods(), expected.getMethods(), expected.getPersonMethods());
        assertThat(actual).isEqualTo(expected);
    }

    private void fixCardIds(List<BoundPaymentMethodDto> actual,
                            List<BoundPaymentMethodDto> expected,
                            List<PersonBoundPaymentMethodDto> actualLinks) {
        Map<String, BoundPaymentMethodDto> actualMethods = actual.stream()
                .collect(Collectors.toMap(BoundPaymentMethodDto::getAccount, Function.identity()));
        Map<String, BoundPaymentMethodDto> expectedMethods = expected.stream()
                .collect(Collectors.toMap(BoundPaymentMethodDto::getId, Function.identity()));
        actualLinks.forEach(link ->
                link.setMethodId(actualMethods.get(expectedMethods.get(link.getMethodId()).getAccount()).getId()));
    }


    private void setPersonId(Map<Long, PaymentPersonDTO> expectedPersons,
                             Map<String, PaymentPersonDTO> actualPersons,
                             PersonBoundPaymentMethodDto pm) {
        long expectedId = pm.getPersonId();
        String name = expectedPersons.get(expectedId).getName();
        long actualId = actualPersons.get(name).getId();
        pm.setPersonId(actualId);
    }

}
