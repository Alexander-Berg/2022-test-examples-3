package ru.yandex.market.delivery.transport_manager.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.PropertyKey;
import ru.yandex.market.delivery.transport_manager.repository.mappers.DynamicPropertyMapper;

class PropertyServiceTest extends AbstractContextualTest {

    @Autowired
    private DynamicPropertyMapper mapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void cantCreateServiceWithInvalidDefaults() {
        softly.assertThatThrownBy(() ->
                new PropertyService<>(
                    mapper,
                    Set.of(InvalidSampleKey.values()),
                    objectMapper
                )
            )
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testEmptyDatabase() {
        var service = new PropertyService<>(mapper, Set.of(SampleKey.values()), objectMapper);
        service.refreshAll();

        softly.assertThat(service.getBoolean(SampleKey.KEY1))
            .isTrue();
        softly.assertThat(service.getBoolean(SampleKey.KEY2))
            .isFalse();
    }

    @Test
    void testGetPrimitive() {
        insertProperty("KEY1", "fsfsefs");
        insertProperty("KEY2", "true");
        insertProperty("KEY3", "false");
        insertProperty("KEY100500", "true");
        var service = new PropertyService<>(mapper, Set.of(SampleKey.values()), objectMapper);
        service.refreshAll();

        softly.assertThatThrownBy(() -> service.getBoolean(SampleKey.KEY1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Can't parse BOOLEAN property KEY1 = fsfsefs");
        softly.assertThat(service.getBoolean(SampleKey.KEY2))
            .isTrue();
        softly.assertThat(service.getBoolean(SampleKey.KEY3))
            .isFalse();
    }

    @Test
    void testGetLongMap() {
        insertProperty("KEY4", "{\"10001787683,10001890880\":8,\"10001787683,10001801990\":7}");
        var service = new PropertyService<>(mapper, Set.of(SampleKey.values()), objectMapper);
        service.refreshAll();

        softly.assertThat(service.<Long>getMap(SampleKey.KEY4))
            .isEqualTo(Map.of(
                "10001787683,10001890880", 8L,
                "10001787683,10001801990", 7L
            ));
    }

    @Test
    void testGetIntMap() {
        insertProperty("KEY5", "{\"10001787683,10001890880\":8,\"10001787683,10001801990\":7}");
        var service = new PropertyService<>(mapper, Set.of(SampleKey.values()), objectMapper);
        service.refreshAll();

        softly.assertThat(service.getMap(SampleKey.KEY5))
            .isEqualTo(Map.of(
                "10001787683,10001890880", 8,
                "10001787683,10001801990", 7
            ));
    }

    @Test
    void testGetStringMap() {
        insertProperty("KEY6", "{\"10001787683,10001890880\":\"A\",\"10001787683,10001801990\":\"B\"}");
        var service = new PropertyService<>(mapper, Set.of(SampleKey.values()), objectMapper);
        service.refreshAll();

        softly.assertThat(service.<String>getMap(SampleKey.KEY6))
            .isEqualTo(Map.of(
                "10001787683,10001890880", "A",
                "10001787683,10001801990", "B"
            ));
    }

    @Test
    void testGetLongList() {
        insertProperty("KEY7", "[8,2147483648]");
        var service = new PropertyService<>(mapper, Set.of(SampleKey.values()), objectMapper);
        service.refreshAll();

        softly.assertThat(service.getList(SampleKey.KEY7))
            .isEqualTo(List.of(8L, 2147483648L));
    }

    @Test
    void testGetIntList() {
        insertProperty("KEY8", "[8,7]");
        var service = new PropertyService<>(mapper, Set.of(SampleKey.values()), objectMapper);
        service.refreshAll();

        softly.assertThat(service.getList(SampleKey.KEY8))
            .isEqualTo(List.of(8, 7));
    }

    @Test
    void testGetIntBadValueList() {
        insertProperty("KEY8", "[\"A\", \"B\"]");
        var service = new PropertyService<>(mapper, Set.of(SampleKey.values()), objectMapper);
        service.refreshAll();

        softly.assertThatThrownBy(() -> service.getList(SampleKey.KEY8))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Can't parse LIST property KEY8 = [\"A\", \"B\"]");
    }

    @Test
    void testGetStringList() {
        insertProperty("KEY9", "[\"A\",\"B\"]");
        var service = new PropertyService<>(mapper, Set.of(SampleKey.values()), objectMapper);
        service.refreshAll();

        softly.assertThat(service.getList(SampleKey.KEY9))
            .isEqualTo(List.of("A", "B"));
    }

    @Test
    void testRefresh() {
        var service = new PropertyService<>(mapper, Set.of(SampleKey.values()), objectMapper);
        softly.assertThat(service.getBoolean(SampleKey.KEY2))
            .isFalse(); // Default value

        insertProperty("KEY2", "1");
        softly.assertThat(service.getBoolean(SampleKey.KEY2))
            .isFalse(); // Still false because it hasn't been refreshed

        service.refreshAll();
        softly.assertThat(service.getBoolean(SampleKey.KEY2))
            .isTrue();

        updateProperty("KEY2", "0");
        softly.assertThat(service.getBoolean(SampleKey.KEY2))
            .isTrue(); // Still true because it hasn't been refreshed

        service.refreshAll();
        softly.assertThat(service.getBoolean(SampleKey.KEY2))
            .isFalse();

        updateProperty("KEY2", "1");
        service.refreshAll();
        softly.assertThat(service.getBoolean(SampleKey.KEY2))
            .isTrue();

        deleteProperty("KEY2");
        softly.assertThat(service.getBoolean(SampleKey.KEY2))
            .isTrue(); // Still true because it hasn't been refreshed

        service.refreshAll();
        softly.assertThat(service.getBoolean(SampleKey.KEY2))
            .isFalse(); // Default value again
    }

    @Test
    void cantInsertDuplicateProperties() {
        insertProperty("KEY", "1");
        softly.assertThatThrownBy(() -> insertProperty("KEY", "2"));
    }

    private void insertProperty(String key, String value) {
        jdbcTemplate.update("INSERT INTO dynamic_property (key, value) VALUES ('" + key + "', '" + value + "')");
    }

    private void updateProperty(String key, String value) {
        jdbcTemplate.update("UPDATE dynamic_property SET value = '" + value + "' WHERE key = '" + key + "'");
    }

    private void deleteProperty(String key) {
        jdbcTemplate.update("DELETE FROM dynamic_property WHERE key = '" + key + "'");
    }

    @Getter
    private enum SampleKey implements PropertyKey {
        KEY1(Type.BOOLEAN, "true"),
        KEY2(Type.BOOLEAN, "false"),
        KEY3(Type.BOOLEAN, "true"),
        KEY4(Type.MAP, "{}", Long.class),
        KEY5(Type.MAP, "{}", Integer.class),
        KEY6(Type.MAP, "{}", String.class),
        KEY7(Type.LIST, "[]", Long.class),
        KEY8(Type.LIST, "[]", Integer.class),
        KEY9(Type.LIST, "[]", String.class);

        private final Type type;
        private final String defaultValue;
        private final java.lang.reflect.Type[] genericTypes;

        SampleKey(
            Type type,
            String defaultValue,
            java.lang.reflect.Type... genericTypes
        ) {
            this.type = type;
            this.defaultValue = defaultValue;
            this.genericTypes = genericTypes;
        }

        @Nonnull
        @Override
        public String getName() {
            return name();
        }
    }

    @Getter
    @RequiredArgsConstructor
    private enum InvalidSampleKey implements PropertyKey {
        INVALID_KEY(Type.BOOLEAN, "sfsdsd");

        private final Type type;
        private final String defaultValue;

        @Nonnull
        @Override
        public String getName() {
            return name();
        }

        @Nonnull
        @Override
        public java.lang.reflect.Type[] getGenericTypes() {
            return new java.lang.reflect.Type[0];
        }
    }
}
