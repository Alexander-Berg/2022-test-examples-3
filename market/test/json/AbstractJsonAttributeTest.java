package ru.yandex.market.jmf.attributes.test.json;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.attributes.AbstractAttributeTest;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.script.ScriptService;
import ru.yandex.market.jmf.utils.Maps;

public abstract class AbstractJsonAttributeTest extends AbstractAttributeTest {
    protected final ObjectMapper mapper = new ObjectMapper();

    @Inject
    ScriptService scriptService;

    @Nonnull
    protected abstract Object randomValue();

    @Override
    protected Object randomAttributeValue() {
        return mapper.convertValue(randomValue(), JsonNode.class);
    }

    @Test
    public void persist_map() {
        Object value = randomValue();
        Entity entity = getEntity(value);

        persist(entity);

        Entity result = get(entity);

        Object attributeValue = result.getAttribute(attributeCode);
        Assertions.assertEquals(
                value, mapper.convertValue(attributeValue, Map.class),
                "Из базы данных должны получить ранее сохраненное значение");
    }

    @Test
    public void persist_local_date_time() {
        String fieldName = "date_time";
        String dateStr = "2019-01-10T15:02:23";
        Map<String, Object> value = Map.of(fieldName, LocalDateTime.parse(dateStr,
                DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        Entity entity = getEntity(value);
        persist(entity);
        Entity result = get(entity);
        JsonNode attributeValue = result.getAttribute(attributeCode);
        Assertions.assertEquals(dateStr, attributeValue.get(fieldName).textValue());
    }

    @Test
    public void simple_integer() {
        long value = Randoms.longValue();
        Entity entity = createPersistedEntity(value);

        JsonNode result = entity.getAttribute(attributeCode);

        Assertions.assertTrue(result instanceof LongNode);
        Assertions.assertEquals(value, result.longValue());
    }

    @Test
    public void simple_integer_script() {
        long value = Randoms.longValue();
        Entity entity = createPersistedEntity(value);

        Object result = scriptService.execute("o.attr", Maps.of("o", entity));

        Assertions.assertEquals(value, result);
    }

    @Test
    public void simple_string() {
        String value = Randoms.string();
        Entity entity = createPersistedEntity(value);

        JsonNode result = entity.getAttribute(attributeCode);

        Assertions.assertTrue(result instanceof TextNode);
        Assertions.assertEquals(value, result.asText());
    }

    @Test
    public void simple_string_script() {
        String value = Randoms.string();
        Entity entity = createPersistedEntity(value);

        Object result = scriptService.execute("o.attr", Maps.of("o", entity));

        Assertions.assertEquals(value, result);
    }

    @Test
    public void simple_boolean() {
        boolean value = Randoms.booleanValue();
        Entity entity = createPersistedEntity(value);

        JsonNode result = entity.getAttribute(attributeCode);

        Assertions.assertTrue(result instanceof BooleanNode);
        Assertions.assertEquals(value, result.asBoolean());
    }

    @Test
    public void simple_boolean_script() {
        boolean value = Randoms.booleanValue();
        Entity entity = createPersistedEntity(value);

        Object result = scriptService.execute("o.attr", Maps.of("o", entity));

        Assertions.assertEquals(value, result);
    }

    @Test
    public void script_map_null() {
        Object result = persistAndScript(null);
        Assertions.assertNull(result);
    }

    @Test
    public void script_map_noKey() {
        Map<String, Object> map = Maps.of();
        Entity entity = createPersistedEntity(map);

        Object result = scriptService.execute("o.attr.k", Maps.of("o", entity));

        Assertions.assertNull(result);
    }

    @Test
    public void script_map_boolean() {
        Object value = Randoms.booleanValue();
        Object result = persistAndScript(value);
        Assertions.assertEquals(value, result);
    }

    @Test
    public void script_map_integer() {
        Object value = Randoms.longValue();
        Object result = persistAndScript(value);
        Assertions.assertEquals(value, result);
    }

    @Test
    public void script_map_string() {
        Object value = Randoms.string();
        Object result = persistAndScript(value);
        Assertions.assertEquals(value, result);
    }

    /**
     * Тестируем {@link ru.yandex.market.jmf.attributes.json.JsonAttributeEqFilterHandler}.
     */
    @Test
    public void filterByJsonValue() {
        String value = Randoms.string();

        // Сохраняем объект, у которого JSON поле содержит ключ jsonKey
        Entity entity = createPersistedEntity(Maps.of("jsonKey", value));
        createPersistedEntity(Maps.of("jsonKey", Randoms.string()));

        // Фильтруем по значению jsonKey
        Query q = Query.of(fqn).withFilters(Filters.eq("attr.jsonKey", value));
        List<Entity> result = dbService.list(q);

        Assertions.assertEquals(1, result.size());
        Assertions.assertTrue(result.contains(entity));
    }

    /**
     * Тестируем {@link ru.yandex.market.jmf.attributes.json.JsonAttributeInFilterHandler}.
     */
    @ParameterizedTest
    @MethodSource("countOfElements")
    public void filterInByJsonValue(int countOfElements) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < countOfElements; i++) {
            values.add(Randoms.string());
        }
        for (var value : values) {
            // Сохраняем объект, у которого JSON поле содержит ключ jsonKey
            createPersistedEntity(Maps.of("jsonKey", value));
            createPersistedEntity(Maps.of("jsonKey", Randoms.string()));
        }

        // Фильтруем по значению jsonKey
        Query q = Query.of(fqn).withFilters(Filters.in("attr.jsonKey", values));
        List<Entity> result = dbService.list(q);

        Assertions.assertEquals(countOfElements, result.size());
    }

    /**
     * Тестируем {@link ru.yandex.market.jmf.attributes.json.JsonAttributeEqFilterHandler}.
     */
    @Test
    public void filterByJsonPathValue() {
        String value = Randoms.string();

        // Сохраняем объект, у которого JSON поле содержит ключ jsonKey
        Entity entity = createPersistedEntity(Maps.of("jsonKey", Maps.of("jsonKey2", value)));
        createPersistedEntity(Maps.of("jsonKey", Maps.of("jsonKey2", Randoms.string())));

        // Фильтруем по значению jsonKey
        Query q = Query.of(fqn).withFilters(Filters.eq("attr.jsonKey.jsonKey2", value));
        List<Entity> result = dbService.list(q);

        Assertions.assertEquals(1, result.size());
        Assertions.assertTrue(result.contains(entity));
    }

    private Object persistAndScript(Object value) {
        Map<String, Object> map = Maps.of("k", value);
        Entity entity = createPersistedEntity(map);

        return scriptService.execute("o.attr.k", Maps.of("o", entity));
    }
}
