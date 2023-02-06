package ru.yandex.market.checkout.pushapi.client.xml.shop;

import java.sql.Timestamp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.pushapi.client.util.CheckoutDateFormat;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;
import ru.yandex.market.checkout.pushapi.settings.AuthType;
import ru.yandex.market.checkout.pushapi.settings.DataType;
import ru.yandex.market.checkout.pushapi.settings.Features;
import ru.yandex.market.checkout.pushapi.settings.Settings;

public class SettingsXmlSerializerTest {

    private CheckoutDateFormat checkoutDateFormat = new CheckoutDateFormat();

    private SettingsXmlSerializer serializer = new SettingsXmlSerializer();

    @BeforeEach
    public void setUp() {
        serializer.setCheckoutDateFormat(checkoutDateFormat);
        serializer.setFeaturesXmlSerializer(new FeaturesXmlSerializer());
    }

    @Test
    public void testSerialize() throws Exception {
        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                Settings.builder()
                        .authToken("auth")
                        .authType(AuthType.HEADER)
                        .dataType(DataType.XML)
                        .urlPrefix("prefix")
                        .changerId("changerId")
                        .fingerprint(new byte[]{0x01, 0x02, (byte) 0xFF})
                        .partnerInterface(Boolean.FALSE)
                        .forceLogResponseUntil(Timestamp.valueOf("2019-09-20 13:30:00"))
                        .features(Features.builder()
                                .enabledGenericBundleSupport(true)
                                .build())
                        .build(),
                "<settings url='prefix'" +
                        "   token='auth'" +
                        "   format='XML'" +
                        "   auth-type='HEADER'" +
                        "   fingerprint='0102ff'" +
                        "   partner-interface='false'" +
                        "   changerId='changerId'" +
                        "   forceLogResponseUntil='20-09-2019 13:30:00'>" +
                        "<features  enabledGenericBundleSupport='true' />" +
                        "</settings>"
        );
    }

    @Test
    public void testSerializeEmpty() throws Exception {
        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                Settings.builder()
                        .authToken(null)
                        .authType(null)
                        .dataType(null)
                        .urlPrefix(null)
                        .fingerprint(null)
                        .partnerInterface(null)
                        .changerId("changerId")
                        .build(),
                "<settings changerId=\"changerId\">" +
                        "<features enabledGenericBundleSupport='false' /><" +
                        "/settings>"
        );
    }
}
