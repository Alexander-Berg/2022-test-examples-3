package ru.yandex.market.delivery.mdbapp.integration.enricher.fulfillment.json.serialization;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ru.yandex.market.checkout.checkouter.jackson.CheckouterModule;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Delivery;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;

public class TestSerializerModule extends CheckouterModule {

    public TestSerializerModule() {
        super();
        addKeyDeserializer(FeedOfferId.class, new KeyDeserializer() {
            @Override
            public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
                return new ObjectMapper().readValue(key, FeedOfferId.class);
            }
        });
        addDeserializer(
            Delivery.class,
            new JsonDeserializer<>() {
                @Override
                public Delivery deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                    ObjectMapper mapper = (ObjectMapper) p.getCodec();
                    ObjectNode node = mapper.readTree(p);
                    JsonNode deliveryIdValue = node.get("deliveryId");
                    JsonNode nameValue = node.get("name");
                    JsonNode phonesValue = node.get("phones");
                    JsonNode contractValue = node.get("contract");
                    JsonNode docsValue = node.get("docs");
                    JsonNode priorityValue = node.get("priority");
                    JsonNode intakeTimeValue = node.get("intakeTime");
                    return new Delivery(
                        isNull(deliveryIdValue)
                            ? null
                            : mapper.readValue(deliveryIdValue.toString(), ResourceId.class),
                        isNull(nameValue) ? null : nameValue.textValue(),
                        isNull(phonesValue)
                            ? null
                            : mapper.readValue(phonesValue.toString(), new TypeReference<>() {
                            }),
                        isNull(contractValue)
                            ? null
                            : contractValue.textValue(),
                        isNull(docsValue)
                            ? null
                            : mapper.readValue(docsValue.toString(), new TypeReference<>() {
                            }),
                        isNull(priorityValue) ? null : priorityValue.intValue(),
                        isNull(intakeTimeValue)
                            ? null
                            : mapper.readValue(intakeTimeValue.toString(), new TypeReference<>() {
                            }),
                        null
                    );
                }
            }
        );
    }

    private static boolean isNull(JsonNode node) {
        return node == null || node.isNull();
    }
}
