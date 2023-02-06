package config.classmapping.shop;

import config.classmapping.BaseClassMappingsTest;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.pushapi.settings.AuthType;
import ru.yandex.market.checkout.pushapi.settings.DataType;
import ru.yandex.market.checkout.pushapi.settings.Settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SettingsClassMappingsTest extends BaseClassMappingsTest {

    @Test
    public void testDeserialize() throws Exception {
        final Settings actual = deserialize(Settings.class,
                "<settings url='prefix'" +
                        "   token='auth'" +
                        "   format='XML'" +
                        "   auth-type='HEADER' />"
        );

        assertEquals("prefix", actual.getUrlPrefix());
        assertEquals("auth", actual.getAuthToken());
        assertEquals(DataType.XML, actual.getDataType());
        assertEquals(AuthType.HEADER, actual.getAuthType());
    }

    @Test
    public void testDeserializeEmpty() throws Exception {
        final Settings actual = deserialize(Settings.class,
                "<settings />"
        );

        assertNotNull(actual);
    }

    @Test
    public void testSerialize() throws Exception {
        serializeAndCompare(
                Settings.builder()
                        .authToken("auth")
                        .authType(AuthType.HEADER)
                        .dataType(DataType.XML)
                        .urlPrefix("prefix")
                        .fingerprint(new byte[]{0x01, 0x02, (byte) 0xFF})
                        .partnerInterface(Boolean.FALSE)
                        .build(),
                "<settings url='prefix'" +
                        "   token='auth'" +
                        "   format='XML'" +
                        "   auth-type='HEADER'" +
                        "   fingerprint='0102ff'" +
                        "   partner-interface='false' " +
                        "   changerId='changerId' />"
        );
    }

    @Test
    public void testSerializeEmpty() throws Exception {
        serializeAndCompare(
                Settings.builder()
                        .changerId("changerId")
                        .build(),
                "<settings changerId=\"changerId\"/>"
        );
    }
}
