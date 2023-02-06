package ru.yandex.market.partner.mvc.controller.util;

import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.util.JsonTestUtil;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class ResponseJsonUtil {

    private ResponseJsonUtil() {
    }

    public static String getResult(ResponseEntity<String> entity) {
        String body = entity.getBody();
        assertNotNull(body);
        return JsonTestUtil.parseJson(body)
                .getAsJsonObject()
                .get("result")
                .toString();
    }
}
