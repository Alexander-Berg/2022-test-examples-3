package ru.yandex.market.checkout.checkouter.antifraud;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderBuyerDeviceIdRequestDto;
import ru.yandex.market.checkout.checkouter.antifraud.entity.AntifraudItemLimitRule;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AntifraudUtilsTest {

    @Test
    public void testParseCsvLine() throws AntifraudUtils.RuleParseException {
        String ruleString = "a,100210870546,,7,,,1,,123,2";
        AntifraudItemLimitRule antifraudItemLimitRule = AntifraudUtils.deserializeItemRule(ruleString);
        assertEquals(
                new AntifraudItemLimitRule(
                        "a",
                        100210870546L,
                        null,
                        7,
                        null,
                        null,
                        1L,
                        null,
                        123L,
                        2),
                antifraudItemLimitRule);
        assertEquals(ruleString, AntifraudUtils.serializeItemRule(antifraudItemLimitRule));
    }

    @Test
    public void testParseDeviceId() throws IOException {
        String androidDeviceIdJson = "{" +
                "\"androidDeviceId\":\"5d07a0ae73137f86\"," +
                "\"googleServiceId\":\"29ae4b91-beca-4330-8a09-7c590ef42afc\"," +
                "\"androidHardwareSerial\":\"unknown\"," +
                "\"androidBuildModel\":\"ZB602KL\"," +
                "\"androidBuildManufacturer\":\"asus\"" +
                "}";
        String iosDeviceIdJson = "{\"ios_device_id\":\"device_id\"}";
        OrderBuyerDeviceIdRequestDto expectedAndroidDeviceId = OrderBuyerDeviceIdRequestDto.builder()
                .androidDeviceId("5d07a0ae73137f86")
                .googleServiceId("29ae4b91-beca-4330-8a09-7c590ef42afc")
                .androidHardwareSerial("unknown")
                .androidBuildModel("ZB602KL")
                .androidBuildManufacturer("asus")
                .build();
        OrderBuyerDeviceIdRequestDto expectedIosDeviceId = OrderBuyerDeviceIdRequestDto.builder()
                .iosDeviceId("device_id")
                .build();
        OrderBuyerDeviceIdRequestDto parsedAndroidDeviceId = AntifraudUtils.parseDeviceId(androidDeviceIdJson);
        OrderBuyerDeviceIdRequestDto parsedIosDeviceId = AntifraudUtils.parseDeviceId(iosDeviceIdJson);
        assertEquals(expectedAndroidDeviceId, parsedAndroidDeviceId);
        assertEquals(expectedIosDeviceId, parsedIosDeviceId);
    }
}
