package ru.yandex.market.checkout.pushapi.client.xml;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXParseException;
import ru.yandex.common.util.xml.parser.*;
import ru.yandex.market.checkout.common.xml.XmlDeserializer;
import ru.yandex.market.checkout.pushapi.client.entity.shop.Settings;
import ru.yandex.market.checkout.pushapi.client.xml.shop.SettingsXmlDeserializer;
import ru.yandex.market.checkout.pushapi.shop.entity.AllSettings;

import java.util.HashMap;
import java.util.Map;

@Component
public class BatchShopSettingsXmlDeserializer extends StackableElementOrientedSAXHandler<AllSettings>
    implements XmlDeserializer<Map<Long, Settings>>
{
    
    private AllSettings result;
    private Long shopId;
    private SettingsXmlDeserializer settingsXmlDeserializer;

    @Autowired
    public void setSettingsXmlDeserializer(SettingsXmlDeserializer settingsXmlDeserializer) {
        this.settingsXmlDeserializer = settingsXmlDeserializer;
    }

    public BatchShopSettingsXmlDeserializer() {
        addElementListener("/shops", new AttributeHelpingElementListener() {
            @Override
            public void onOpen(ElementOrientedSAXHandler elementOrientedSAXHandler, AttributesHelper attributesHelper) throws SAXParseException {
                result = new AllSettings();
            }
        });
        addElementListener("/shops/shop", new AttributeHelpingElementListener() {
            @Override
            public void onOpen(ElementOrientedSAXHandler elementOrientedSAXHandler, AttributesHelper attributesHelper) throws SAXParseException {
                shopId = attributesHelper.getLong("id");
            }
        });
        addInnerParser("/shops/shop/settings", new InnerParser<Settings>() {
            @Override
            public StackableElementOrientedSAXHandler<Settings> getParser() {
                return settingsXmlDeserializer;
            }

            @Override
            public void parsed(Settings o) {
                result.put(shopId, o);
            }
        });
    }

    @Override
    public AllSettings getParsed() {
        return result;
    }

    @Override
    public Map<Long, Settings> getContent() {
        return result;
    }
}
