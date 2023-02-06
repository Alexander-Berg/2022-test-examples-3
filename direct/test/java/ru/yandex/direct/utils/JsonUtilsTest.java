package ru.yandex.direct.utils;

import java.util.LinkedHashMap;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonUtilsTest {
    @Test
    public void deserializeWorksWithUnknownProperties() throws Exception {
        assertThat(JsonUtils.fromJson("{\"id\":12}", Data.class))
                .isEqualToComparingFieldByField(new Data().withId(12L));
    }

    @Test
    public void serializeNullFields() {
        assertThat(JsonUtils.toJson(new Data())).isEqualTo("{\"id\":null}");
    }

    public static class Data {
        private Long id;

        public Data() {
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Data withId(Long id) {
            setId(id);
            return this;
        }
    }

    /**
     * Проверяем, что JsonUtils.toDeterministicJson сортирует ключи мап по алфавиту
     */
    @Test
    public void deterministicSerializerTest() {
        // используется LinkedHashMap, т.к. если бы сериализатор не сортировал ключи,
        // они бы точно шли не в алфавитном порядке
        LinkedHashMap<String, String> lhm = new LinkedHashMap<>();
        lhm.put("foo", "bar");
        lhm.put("abc", "def");
        String expectedJson = "{\"abc\":\"def\",\"foo\":\"bar\"}";

        String json = JsonUtils.toDeterministicJson(lhm);
        assertThat(json).isEqualTo(expectedJson);
    }
}
