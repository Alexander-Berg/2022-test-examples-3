package ru.yandex.market.jmf.attributes.test.hyperlink;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import ru.yandex.market.jmf.attributes.conf.metaclass.HyperlinkTypeConf;
import ru.yandex.market.jmf.attributes.hyperlink.HyperlinkDescriptor;
import ru.yandex.market.jmf.attributes.hyperlink.HyperlinkType;
import ru.yandex.market.jmf.metadata.metaclass.Attribute;
import ru.yandex.market.jmf.utils.SerializationUtils;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractHyperlinkDescriptorTest {

    static ObjectMapper objectMapper = SerializationUtils.defaultObjectMapper();
    HyperlinkDescriptor descriptor;
    Attribute attribute;
    HyperlinkType type;

    @BeforeAll
    public void setUp() {
        descriptor = new HyperlinkDescriptor(objectMapper);
        attribute = new Attribute();
        type = new HyperlinkType(false, HyperlinkTypeConf.builder().withOpenInNewTab(false).build());
    }
}
