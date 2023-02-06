package ru.yandex.market.checkout.pushapi.client.xml;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;
import ru.yandex.market.checkout.pushapi.client.entity.shop.AuthType;
import ru.yandex.market.checkout.pushapi.client.entity.shop.DataType;
import ru.yandex.market.checkout.pushapi.client.entity.shop.Settings;
import ru.yandex.market.checkout.pushapi.client.xml.shop.SettingsXmlDeserializer;
import ru.yandex.market.checkout.pushapi.shop.entity.AllSettings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class BatchShopSettingsXmlDeserializerTest {
    
    private SettingsXmlDeserializer settingsXmlDeserializer = new SettingsXmlDeserializer();
    private BatchShopSettingsXmlDeserializer deserializer = new BatchShopSettingsXmlDeserializer();

    @Before
    public void setUp() throws Exception {
        deserializer.setSettingsXmlDeserializer(settingsXmlDeserializer);
    }

    @Test
    public void testDeserialize() throws Exception {
        final AllSettings result = XmlTestUtil.deserialize(
            deserializer,
            "<shops>" +
                "    <shop id='155'>" +
                "        <settings url='https://ozon.ru/specially-for-market'" +
                "                  token='6asd75f713rhjkawhfg457621'" +
                "                  auth-type='HEADER'" +
                "                  format='JSON' />" +
                "    </shop>" +
                "    <shop id='211'>" +
                "        <settings url='https://mvideo.ru/specially-for-market'" +
                "                  token='78asd5f75asd4f731fasf12ef'" +
                "                  auth-type='URL'" +
                "                  format='XML' />" +
                "    </shop>" +
                "</shops>"
        );
        
        assertEquals(2, result.size());
        final Settings shop155 = result.get(155l);
        assertEquals("https://ozon.ru/specially-for-market", shop155.getUrlPrefix());
        assertEquals("6asd75f713rhjkawhfg457621", shop155.getAuthToken());
        assertEquals(AuthType.HEADER, shop155.getAuthType());
        assertEquals(DataType.JSON, shop155.getDataType());
        final Settings shop211 = result.get(211l);
        assertEquals("https://mvideo.ru/specially-for-market", shop211.getUrlPrefix());
        assertEquals("78asd5f75asd4f731fasf12ef", shop211.getAuthToken());
        assertEquals(AuthType.URL, shop211.getAuthType());
        assertEquals(DataType.XML, shop211.getDataType());
    }

    @Test
    public void testDeserializeEmpty() throws Exception {
        final AllSettings actual = XmlTestUtil.deserialize(
            deserializer,
            "<shops />"
        );

        assertNotNull(actual);
    }
}
