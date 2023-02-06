package ru.yandex.market.jmf.telephony.voximplant.test;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.jmf.telephony.voximplant.VoximplantError;
import ru.yandex.market.jmf.telephony.voximplant.impl.VoximplantErrorsParser;
import ru.yandex.market.jmf.utils.SerializationUtils;

public class VoximplantErrorsParserTest {

    private static final VoximplantErrorsParser parser =
            new VoximplantErrorsParser(SerializationUtils.defaultObjectSerializeService());

    @Test
    public void testParser() {
        String response = "[{\"id\":239,\"type\":\"JS error\",\"env\":\"production\"," +
                "\"log\":\"https://vox/239\",\"createdAt\":\"2020-03-24T10:13:41.037Z\"}," +
                "{\"id\":240,\"type\":\"JS error\",\"env\":\"testing\"," +
                "\"log\":\"https://vox/240\",\"createdAt\":\"2020-03-24T11:13:42.037Z\"}]";

        List<VoximplantError> result = parser.parse(response.getBytes());
        Assertions.assertEquals(2, result.size());
        checkVoximplantError(result.get(0), 239L, "JS error", "production",
                "https://vox/239", 1585044821037L);
        checkVoximplantError(result.get(1), 240L, "JS error", "testing",
                "https://vox/240", 1585048422037L);
    }

    private void checkVoximplantError(VoximplantError actual,
                                      Long expectedId,
                                      String expectedType,
                                      String expectedEnv,
                                      String expectedLog,
                                      long expectedCreatedAt) {
        Assertions.assertEquals(expectedId, actual.getId());
        Assertions.assertEquals(expectedType, actual.getType());
        Assertions.assertEquals(expectedEnv, actual.getEnv());
        Assertions.assertEquals(expectedLog, actual.getLog());
        Assertions.assertEquals(expectedCreatedAt, actual.getCreatedAt().toInstant().toEpochMilli());
    }
}
