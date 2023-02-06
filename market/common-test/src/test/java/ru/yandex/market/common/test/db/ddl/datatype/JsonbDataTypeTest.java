package ru.yandex.market.common.test.db.ddl.datatype;

import org.dbunit.dataset.datatype.TypeCastException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonbDataTypeTest {
    private final JsonbDataType tested = new JsonbDataType();

    /**
     * Тестируем проверку равенства для постгрес типа json.
     *
     * @throws TypeCastException - при ошибке кастования к стринге (не может возникнуть в нашем случае)
     */
    @Test
    void testCompare() throws TypeCastException {
        assertThat(tested.compare(
                "{\"field1\": \"value1\", \"field2\": \"value2\"}",
                "{\"field1\": \"value1\", \"field2\": \"value2\"}"
        )).isEqualTo(0);
        assertThat(tested.compare(
                "{\"field1\": \"value1\", \"field2\": \"value2\"}",
                "{\"field2\":\"value2\",\"field1\": \"value1\"}")
        ).isEqualTo(0);
        assertThat(tested.compare(null, null)).isEqualTo(0);
        assertThat(tested.compare("", null)).isEqualTo(1);
        assertThat(tested.compare("{\"field1\": \"value1\"}", "{\"field1\": \"value2\"}")).isEqualTo(-1);
        assertThat(tested.compare("{\"field1\": [1,2,3]}", "{\"field1\": [3,2,1]}")).isEqualTo(0);
        assertThat(tested.compare("{\"field1\": [1,2,3]}", "{\"field1\": [3,2,1], \"field2\": 12}")).isEqualTo(-1);
        assertThat(tested.compare(null, "")).isEqualTo(-1);
    }
}
