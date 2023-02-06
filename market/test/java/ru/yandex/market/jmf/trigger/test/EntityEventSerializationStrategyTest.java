package ru.yandex.market.jmf.trigger.test;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.EntityAdapterService;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metainfo.MetaInfoService;
import ru.yandex.market.jmf.trigger.EntityEvent;
import ru.yandex.market.jmf.trigger.TriggerConstants;
import ru.yandex.market.jmf.trigger.conf.TriggerConf;
import ru.yandex.market.jmf.trigger.impl.EntityEventSerializationStrategy;
import ru.yandex.market.jmf.trigger.impl.TriggerData;

@SpringJUnitConfig(InternalTriggerTestConfiguration.class)
public class EntityEventSerializationStrategyTest {
    private static final Fqn SIMPLE_ENTITY_FQN = Fqn.of("simpleEntity");
    private static final Fqn TRANSIENT_ENTITY_FQN = Fqn.of("transientEntity");
    private static final String SIMPLE_TRIGGER = "simpleTrigger";
    private final TransientEntityImpl transientEntity = new TransientEntityImpl(1L, "test");
    @Inject
    private EntityEventSerializationStrategy entityEventSerializationStrategy;
    @Inject
    private BcpService bcpService;
    @Inject
    private EntityAdapterService entityAdapterService;
    @Inject
    private MetaInfoService metaInfoService;
    @Inject
    private ObjectMapper objectMapper;
    private TriggerConf simpleTrigger;
    private Entity entity;
    private byte[] serialized;

    @BeforeEach
    void setUp() {
        simpleTrigger = metaInfoService.get(TriggerData.class).getTrigger(SIMPLE_TRIGGER);
        entity = bcpService.create(SIMPLE_ENTITY_FQN, Map.of());
    }

    @Transactional
    @Test
    void serialize_simple() throws IOException {
        EntityEvent event = new EntityEvent(entity.getMetaclass(), TriggerConstants.CREATE, entity, null);
        byte[] serialize = entityEventSerializationStrategy.serialize(event, simpleTrigger);
        Assertions.assertNotNull(serialize);

        JsonNode serializeJson = objectMapper.readTree(serialize);
        assertSerialize(event, serializeJson);
        Assertions.assertTrue(serializeJson.has("variables"));
        Assertions.assertTrue(serializeJson.has("variablesSerializationType"));
        Assertions.assertEquals(0, serializeJson.get("variables").size());
        Assertions.assertEquals(0, serializeJson.get("variablesSerializationType").size());
    }

    @Transactional
    @Test
    void serialize_withVariables() throws IOException {
        EntityEvent event = new EntityEvent(entity.getMetaclass(), TriggerConstants.CREATE, entity, null);
        event.addVariable("testInt", 1);
        event.addVariable("testStr", "test");

        byte[] serialize = entityEventSerializationStrategy.serialize(event, simpleTrigger);
        Assertions.assertNotNull(serialize);

        JsonNode serializeJson = objectMapper.readTree(serialize);
        assertSerialize(event, serializeJson);
        Assertions.assertTrue(serializeJson.has("variables"));
        Assertions.assertTrue(serializeJson.has("variablesSerializationType"));
        Assertions.assertEquals(2, serializeJson.get("variables").size());
        Assertions.assertEquals(0, serializeJson.get("variablesSerializationType").size());

        Assertions.assertTrue(serializeJson.get("variables").get("testInt").isInt());
        Assertions.assertEquals(1, serializeJson.get("variables").get("testInt").asInt());

        Assertions.assertTrue(serializeJson.get("variables").get("testStr").isTextual());
        Assertions.assertEquals("test", serializeJson.get("variables").get("testStr").asText());
    }

    @Transactional
    @Test
    void serialize_withVariablesSnapshotable() throws IOException {
        Entity varible = entityAdapterService.wrap(transientEntity);

        EntityEvent event = new EntityEvent(entity.getMetaclass(), TriggerConstants.CREATE, entity, null);
        event.addVariable("testInt", 1);
        event.addVariable("testStr", "test");
        event.addVariable("entity", varible);

        serialized = entityEventSerializationStrategy.serialize(event, simpleTrigger);
        Assertions.assertNotNull(serialized);

        JsonNode serializeJson = objectMapper.readTree(serialized);
        assertSerialize(event, serializeJson);
        Assertions.assertTrue(serializeJson.has("variables"));
        Assertions.assertTrue(serializeJson.has("variablesSerializationType"));
        Assertions.assertEquals(3, serializeJson.get("variables").size());
        Assertions.assertEquals(1, serializeJson.get("variablesSerializationType").size());

        Assertions.assertTrue(serializeJson.get("variablesSerializationType").get("entity").isTextual());
        Assertions.assertEquals(
                "SNAPSHOTABLE",
                serializeJson.get("variablesSerializationType").get("entity").asText());

        Assertions.assertTrue(serializeJson.get("variables").get("testInt").isInt());
        Assertions.assertEquals(1, serializeJson.get("variables").get("testInt").asInt());

        Assertions.assertTrue(serializeJson.get("variables").get("testStr").isTextual());
        Assertions.assertEquals("test", serializeJson.get("variables").get("testStr").asText());

        Assertions.assertTrue(serializeJson.get("variables").get("entity").isTextual());
        Assertions.assertEquals(
                "{\"fqn\":\"transientEntity\",\"attrStr\":\"test\",\"attrInt\":1}",
                serializeJson.get("variables").get("entity").asText());
    }

    @Transactional
    @Test
    void deserialize_withVariablesSnapshotable() throws IOException {
        serialize_withVariablesSnapshotable();

        EntityEvent deserialize = entityEventSerializationStrategy.deserialize(serialized);
        Assertions.assertNotNull(deserialize);
        Assertions.assertEquals(entity.getMetaclass(), deserialize.getMetaclass());
        Assertions.assertEquals(TriggerConstants.CREATE, deserialize.getAction());
        Assertions.assertEquals(entity, deserialize.getEntity());

        Assertions.assertNotNull(deserialize.getVariables());
        Assertions.assertEquals(1, deserialize.getVariables().get("testInt"));
        Assertions.assertEquals("test", deserialize.getVariables().get("testStr"));

        TransientEntityImpl expectedEntity = transientEntity;
        TransientEntity actualEntity = (TransientEntity) deserialize.getVariables().get("entity");
        Assertions.assertNotNull(actualEntity);
        Assertions.assertEquals(TRANSIENT_ENTITY_FQN, actualEntity.getFqn());
        Assertions.assertEquals(expectedEntity.getAttrInt(), actualEntity.getAttrInt());
        Assertions.assertEquals(expectedEntity.getAttrStr(), actualEntity.getAttrStr());
    }

    private void assertSerialize(EntityEvent expectedEvent, JsonNode actualJson) {
        Assertions.assertEquals(expectedEvent.getMetaclass().getFqn().toString(), actualJson.get("fqn").asText());
        Assertions.assertEquals(expectedEvent.getAction(), actualJson.get("action").asText());
        Assertions.assertEquals(expectedEvent.getEntity().getGid(), actualJson.get("objGid").asText());
    }

}
