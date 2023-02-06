package ru.yandex.market.mbi.logprocessor.metrics;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.mbi.logprocessor.FunctionalTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SolomonJvmControllerTest extends FunctionalTest {
    private static final Pattern JETTY_PATTERN = Pattern.compile("org\\.eclipse\\.jetty\\.util\\.thread:.*");
    private static final TypeReference<Map<String, List<Map<String, Object>>>> TYPE_REFERENCE = new TypeReference<>() {
    };

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    public void testGetSensors() throws IOException {
        ResponseEntity<String> response = testRestTemplate.getForEntity("/solomon-jvm", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map<String, List<Map<String, Object>>> sensorsMap = new ObjectMapper().readValue(response.getBody(),
                TYPE_REFERENCE);
        assertTrue(sensorsMap.containsKey("sensors"));

        var sensors = sensorsMap.get("sensors");
        Assert.assertTrue(sensors.size() > 0);
        int jettyCount = 0;

        var sensorNames = new TreeSet<String>();
        for (var sensor: sensors) {
            assertTrue(sensor.containsKey("kind"));
            assertTrue(sensor.containsKey("labels"));
            assertTrue(sensor.containsKey("ts"));
            assertTrue(sensor.containsKey("value"));

            var labels = (Map<?, ?>) sensor.get("labels");
            String sensorName = labels.get("sensor").toString();
            sensorNames.add(sensorName);
            assertTrue(StringUtils.isNotBlank(sensorName));
            assertTrue(StringUtils.isNotBlank((String) sensor.get("kind")));

            assertTrue(sensor.get("ts") instanceof Integer);
            assertTrue(sensor.get("value") instanceof Number);

            Integer ts = (Integer) sensor.get("ts");
            assertTrue(ts.compareTo(1500000000) > 0); // a valid timestamp

            switch (sensorName) {
                case "jvm.threads.daemons":
                case "jvm.threads.total":
                    assertTrue((int) sensor.get("value") > 0);
            }

            if (JETTY_PATTERN.matcher(sensorName).matches()) {
                jettyCount++;
            }
        }
        sensorNames.forEach(System.out::println);
        assertTrue(jettyCount > 0);
    }
}
