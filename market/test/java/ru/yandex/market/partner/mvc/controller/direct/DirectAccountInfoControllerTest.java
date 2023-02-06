package ru.yandex.market.partner.mvc.controller.direct;

import java.io.InputStream;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

@ParametersAreNonnullByDefault
public class DirectAccountInfoControllerTest extends FunctionalTest {

    private static final long UID = 1172184166;

    @Test
    @DisplayName("Получение информации об общем счете Директа")
    void getAutoPaymentMethodsTest() {
        final var url = UriComponentsBuilder.fromUriString(baseUrl + "/direct/account")
                .queryParam("_user_id", UID)
                .queryParam("format", "json")
                .build().toString();
        final ResponseEntity<String> entity = FunctionalTestHelper.get(url);
        final InputStream expected = this.getClass()
                .getResourceAsStream("json/DirectInfoControllerTest.getAccountInfo.json");
        JsonTestUtil.assertEquals(entity, expected);
    }
}
