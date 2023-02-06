package ru.yandex.market.logshatter.parser.front.errorBooster.universal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ErrorsProtoConverterTest {
    @Test
    public void fullProtoConverterCheck() throws Exception {
        ErrorsProtoConverter converter = new ErrorsProtoConverter();
        byte[] data = Base64.getDecoder().decode(
            ("CgVzdGFjaxICZGMaBWxldmVsIgVzbG90cyoFcmVxaWQyBm1ldGhvZDoGc291cmNlQgxzb3VyY2VNZXRob2RKCnNvdXJjZVR5cGVQAFg" +
                "AYgRob3N0agRmaWxlcgdtZXNzYWdlegdwcm9qZWN0ggEHc2VydmljZYoBAmlwkgELZXhwZXJpbWVudHOaAQhwbGF0Zm9ybaIBBWJ" +
                "sb2NrqgEIbGFuZ3VhZ2WyAQZyZWdpb266AQd2ZXJzaW9uwgEJeWFuZGV4dWlkygEDZW520gEJdXNlcmFnZW502AHo8reb3y3iAQN" +
                "1cmzyAQRwYWdl")
                .getBytes(StandardCharsets.US_ASCII)
        );
        InputStream input = new ByteArrayInputStream(data);
        String output = IOUtils.toString(converter.convert(input), StandardCharsets.UTF_8.toString());
        String expected = "{\"stack\":\"stack\",\"dc\":\"dc\",\"level\":\"level\",\"slots\":\"slots\"," +
            "\"reqid\":\"reqid\",\"method\":\"method\",\"source\":\"source\",\"sourceMethod\":\"sourceMethod\"," +
            "\"sourceType\":\"sourceType\",\"isInternal\":false,\"isRobot\":false,\"host\":\"host\"," +
            "\"file\":\"file\",\"message\":\"message\",\"project\":\"project\",\"service\":\"service\",\"ip\":\"ip\"," +
            "\"experiments\":\"experiments\",\"platform\":\"platform\",\"block\":\"block\",\"language\":\"language\"," +
            "\"region\":\"region\",\"version\":\"version\",\"yandexuid\":\"yandexuid\",\"env\":\"env\"," +
            "\"useragent\":\"useragent\",\"timestamp\":1571747133800,\"url\":\"url\",\"page\":\"page\"}";

        Assertions.assertEquals(
            JsonParser.parseString(expected).getAsJsonObject(),
            JsonParser.parseString(output).getAsJsonObject(),
            "Expected and resulted should be equal"
        );
    }
}
