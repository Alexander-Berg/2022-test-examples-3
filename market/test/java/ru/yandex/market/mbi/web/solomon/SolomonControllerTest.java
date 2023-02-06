package ru.yandex.market.mbi.web.solomon;

import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.market.mbi.web.solomon.pull.SolomonController;
import ru.yandex.market.mbi.web.solomon.pull.SolomonUtils;
import ru.yandex.monlib.metrics.MetricType;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SolomonControllerTest {
    private static final Gson GSON = new Gson();
    private static final String URL = "/solomon";

    private SolomonController solomonController = new SolomonController();
    private MockMvc mockMvc = MockMvcBuilders.standaloneSetup(solomonController).build();

    @BeforeEach
    public void before() {
        SolomonUtils.getMetricRegistry().lazyGaugeInt64("test_sensor", () -> 100500);
    }

    @Test
    public void solomonTest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        solomonController.getSensors(request, response);
        checkResponse(response, new SensorLongValue(
                MetricType.IGAUGE,
                ImmutableMap.of("sensor", "test_sensor"),
                100500)
        );
    }

    @Test
    public void solomonControllerTest() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get(URL)).andReturn();
        checkResponse(result.getResponse(), new SensorLongValue(
                MetricType.IGAUGE,
                ImmutableMap.of("sensor", "test_sensor"),
                100500)
        );
    }

    private void checkResponse(MockHttpServletResponse response, SensorLongValue expected) throws Exception {
        JsonObject result = GSON.fromJson(response.getContentAsString(), JsonObject.class);
        JsonArray sensors = result.getAsJsonArray("sensors");
        Assertions.assertEquals(1, sensors.size());

        SensorLongValue sensor = GSON.fromJson(sensors.get(0).getAsJsonObject(), SensorLongValue.class);
        assertEquals(expected, sensor);
    }

    private class SensorLongValue {
        @SerializedName("kind")
        private MetricType type;
        private Map<String, String> labels;
        private long value;

        SensorLongValue(MetricType type, Map<String, String> labels, long value) {
            this.type = type;
            this.labels = labels;
            this.value = value;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            if (obj == null || this.getClass() != obj.getClass()) {
                return false;
            }

            SensorLongValue other = (SensorLongValue) obj;
            return this.type == other.type && this.labels.equals(other.labels) && this.value == other.value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, labels, value);
        }

        @Override
        public String toString() {
            return "{type:" + type + ", labels: " + labels + ", value: " + value + '}';
        }
    }
}
