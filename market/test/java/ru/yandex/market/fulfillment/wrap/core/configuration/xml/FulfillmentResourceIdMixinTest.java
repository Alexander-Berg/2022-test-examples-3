package ru.yandex.market.fulfillment.wrap.core.configuration.xml;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.fulfillment.wrap.core.ParsingTest;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
    classes = {XmlMappingConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class FulfillmentResourceIdMixinTest extends ParsingTest<ResourceId> {

    @Autowired
    @Qualifier("fulfillmentMapper")
    private XmlMapper xmlMapper;

    FulfillmentResourceIdMixinTest() {
        super(null, ResourceId.class, "resource_id.xml");
    }

    @Override
    protected ObjectMapper getMapper() {
        return xmlMapper;
    }

    @Override
    protected Map<String, Object> fieldValues() {
        return ImmutableMap.of(
            "yandexId", "15",
            "fulfillmentId", "45");
    }
}
