package ru.yandex.market.markup2.tasks.card_duplicates;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import org.junit.Test;
import ru.yandex.common.util.collections.CollectionFactory;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * @author sergtru
 * @since 22.06.2017
 */
public class CardDuplicatesDataItemsProcessorTest {
    @Test
    public void requestFieldsTest() throws Exception {
        JsonSerializer<? super TaskDataItem<CardDuplicatesPayload, CardDuplicatesResponse>> serializer =
                new CardDuplicatesDataItemsProcessor(null).getRequestSerializer();
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(
                        new SimpleModule()
                                .setSerializerModifier(new BeanSerializerModifier() {
                                    @Override
                                    public JsonSerializer<?> modifySerializer(SerializationConfig config,
                                                                              BeanDescription beanDesc,
                                                                              JsonSerializer<?> defaultSerializer) {
                                        if (beanDesc.getBeanClass() == TaskDataItem.class) {
                                            return serializer;
                                        }
                                        return super.modifySerializer(config, beanDesc, defaultSerializer);
                                    }
                                })
                );
        CardDuplicatesPayloadTest.CardGenerator cards = new CardDuplicatesPayloadTest.CardGenerator();
        CardDuplicatesPayload obj = cards.generatePayload(1);
        Set<String> actualFields = new HashSet<>();
        JsonNode node = mapper.valueToTree(new TaskDataItem<>(1, obj));
        node.fieldNames().forEachRemaining(actualFields::add);

        Set<String> expected = CollectionFactory.set(
            "id", "query", "image1", "image2", "category", "category_id", "model_descr1", "model_descr2", "instruction",
            "card_id1", "card_id2", "title1", "title2", "serp_id", "query_type"
        );
        assertEquals(expected, actualFields);

        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            assertEquals("Field " + field, JsonNodeType.STRING, field.getValue().getNodeType());
        }
    }

}
