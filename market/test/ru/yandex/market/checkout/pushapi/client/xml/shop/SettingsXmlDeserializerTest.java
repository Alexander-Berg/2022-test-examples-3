package ru.yandex.market.checkout.pushapi.client.xml.shop;

import java.sql.Timestamp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.pushapi.client.util.CheckoutDateFormat;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;
import ru.yandex.market.checkout.pushapi.settings.AuthType;
import ru.yandex.market.checkout.pushapi.settings.DataType;
import ru.yandex.market.checkout.pushapi.settings.Settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SettingsXmlDeserializerTest {

    private CheckoutDateFormat checkoutDateFormat = new CheckoutDateFormat();

    private SettingsXmlDeserializer deserializer = new SettingsXmlDeserializer(new FeaturesXmlDeserializer());

    @BeforeEach
    public void setUp() {
        deserializer.setCheckoutDateFormat(checkoutDateFormat);
    }

    @Test
    public void testDeserialize() throws Exception {
        final Settings actual = XmlTestUtil.deserialize(
                deserializer,
                "<settings url='prefix'" +
                        "   token='auth'" +
                        "   format='XML'" +
                        "   auth-type='HEADER'" +
                        "   forceLogResponseUntil='20-09-2019 13:30:00'>" +
                        "<features enabledGenericBundleSupport='true' />" +
                        "</settings>"
        );

        assertEquals("prefix", actual.getUrlPrefix());
        assertEquals("auth", actual.getAuthToken());
        assertTrue(actual.getFeatures().isEnabledGenericBundleSupport());
        assertEquals(DataType.XML, actual.getDataType());
        assertEquals(AuthType.HEADER, actual.getAuthType());
        assertEquals(Timestamp.valueOf("2019-09-20 13:30:00"), actual.getForceLogResponseUntil());
    }

    @Test
    public void testDeserializeEmpty() throws Exception {
        final Settings actual = XmlTestUtil.deserialize(
                deserializer,
                "<settings />"
        );

        assertNotNull(actual);
        assertNotNull(actual.getFeatures());
    }
}
