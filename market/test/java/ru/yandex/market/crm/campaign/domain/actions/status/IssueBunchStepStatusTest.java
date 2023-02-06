package ru.yandex.market.crm.campaign.domain.actions.status;

import org.junit.Test;

import ru.yandex.market.crm.core.jackson.CustomObjectMapperFactory;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.json.serialization.JsonDeserializerImpl;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.crm.json.serialization.JsonSerializerImpl;

import static org.junit.Assert.assertEquals;

/**
 * @author zloddey
 */
public class IssueBunchStepStatusTest {

    private final JsonDeserializer deserializer = new JsonDeserializerImpl(CustomObjectMapperFactory.INSTANCE.getJsonObjectMapper());
    private final JsonSerializer serializer = new JsonSerializerImpl(CustomObjectMapperFactory.INSTANCE.getJsonObjectMapper());

    /**
     * Конфиги старых акций хранятся в БД в фиксированном виде. Надо уметь читать их без проблем
     */
    @Test
    public void deserializeOldFormat() {
        String json = "{\"issuedCount\":33,\"stepType\":\"ISSUE_COINS\"}";
        IssueBunchStepStatus issueBunchStepStatus = deserialize(json);
        assertEquals(Integer.valueOf(33), issueBunchStepStatus.getIssuedCount());
    }

    /**
     * Конфиги новых акцих будут записываться в БД в новом виде.
     */
    @Test
    public void deserializeModernFormat() {
        String json = "{\"issuedPerStageCount\":{\"AUTH\":12,\"NO_AUTH\":21}," +
                "\"issuedCount\":33,\"stepType\":\"ISSUE_COINS\"}";
        IssueBunchStepStatus issueBunchStepStatus = deserialize(json);
        assertEquals(Integer.valueOf(12), issueBunchStepStatus.getIssuedCount("AUTH"));
        assertEquals(Integer.valueOf(21), issueBunchStepStatus.getIssuedCount("NO_AUTH"));
        assertEquals(Integer.valueOf(33), issueBunchStepStatus.getIssuedCount());
    }

    /**
     * Проверяем, что после сериализации сохраняются корректные метрики: plannedCount
     */
    @Test
    public void deserializeModernSerializedStatusPlannedCount() {
        IssueBunchStepStatus before = new IssueBunchStepStatus()
                .setPlannedCount("AUTH", 55)
                .setPlannedCount("NO_AUTH", 10);
        String serialized = serializer.writeObjectAsString(before);
        IssueBunchStepStatus after = deserialize(serialized);
        assertEquals(Integer.valueOf(65), after.getPlannedCount());
        assertEquals(Integer.valueOf(55), after.getPlannedCount("AUTH"));
        assertEquals(Integer.valueOf(10), after.getPlannedCount("NO_AUTH"));
    }

    /**
     * Проверяем, что после сериализации сохраняются корректные метрики: issuedCount
     */
    @Test
    public void deserializeModernSerializedStatusIssuedCount() {
        IssueBunchStepStatus before = new IssueBunchStepStatus()
                .setIssuedCount("AUTH", 55)
                .setIssuedCount("NO_AUTH", 0);
        String serialized = serializer.writeObjectAsString(before);
        IssueBunchStepStatus after = deserialize(serialized);
        assertEquals(Integer.valueOf(55), after.getIssuedCount());
        assertEquals(Integer.valueOf(55), after.getIssuedCount("AUTH"));
        assertEquals(Integer.valueOf(0), after.getIssuedCount("NO_AUTH"));
    }

    /**
     * Проверяем, что после сериализации сохраняются корректные метрики: processedCount
     */
    @Test
    public void deserializeModernSerializedStatusProcessedCount() {
        IssueBunchStepStatus before = new IssueBunchStepStatus()
                .setProcessedCount("AUTH", 0)
                .setProcessedCount("NO_AUTH", 10);
        String serialized = serializer.writeObjectAsString(before);
        IssueBunchStepStatus after = deserialize(serialized);
        assertEquals(Integer.valueOf(10), after.getProcessedCount());
        assertEquals(Integer.valueOf(0), after.getProcessedCount("AUTH"));
        assertEquals(Integer.valueOf(10), after.getProcessedCount("NO_AUTH"));
    }

    private IssueBunchStepStatus deserialize(String json) {
        return deserializer.readObject(IssueBunchStepStatus.class, json);
    }
}
