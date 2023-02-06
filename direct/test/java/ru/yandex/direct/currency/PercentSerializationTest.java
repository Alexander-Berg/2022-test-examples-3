package ru.yandex.direct.currency;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.Test;

import ru.yandex.direct.utils.JsonUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class PercentSerializationTest {

    @Test
    public void percentSerialization_success() {
        Percent percent = Percent.fromPercent(BigDecimal.valueOf(123456789.0123456789));
        // Значение округляется вверх до двух знаков после запятой
        assertThat(JsonUtils.toJson(new TestClass(percent)))
                .isEqualTo("{\"percent\":123456789.02}");
    }

    @Test
    public void percentDeserialization_success() {
        String serialized = "{\"percent\":123456789.02}";
        TestClass deserialized = JsonUtils.fromJson(serialized, TestClass.class);

        Percent expected = Percent.fromPercent(BigDecimal.valueOf(123456789.02));
        assertThat(deserialized.getPercent()).isEqualTo(expected);
    }

    @Test
    public void percentDeserialization_success_onString() {
        String serialized = "{\"percent\":\"123456789.02\"}";
        TestClass deserialized = JsonUtils.fromJson(serialized, TestClass.class);

        Percent expected = Percent.fromPercent(BigDecimal.valueOf(123456789.02));
        assertThat(deserialized.getPercent()).isEqualTo(expected);
    }

    private static class TestClass {
        private Percent percent;

        public TestClass() {
        }

        private TestClass(Percent percent) {
            this.percent = percent;
        }

        @JsonProperty
        public Percent getPercent() {
            return percent;
        }
    }

}
