package ru.yandex.market.antifraud.orders.storage.entity.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;

/**
 * @author dzvyagin
 */
public class ConfigEnumTest {

    @Test
    public void check() {
        JsonNode booleanNode = AntifraudJsonUtil.toJsonTree(Boolean.TRUE);
        ConfigEnum.ANTIFRAUD_OFFLINE_BAN_USER.check(booleanNode);
        ConfigEnum.ANTIFRAUD_OFFLINE_CANCEL_ORDER.check(booleanNode);
        JsonNode longArrNode = AntifraudJsonUtil.toJsonTree(new long[]{1, 2, 3});
        ConfigEnum.MOCK_RESPONSES_USERS.check(longArrNode);
    }

}
