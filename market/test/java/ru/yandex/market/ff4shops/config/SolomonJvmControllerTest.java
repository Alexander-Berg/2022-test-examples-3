package ru.yandex.market.ff4shops.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.ff4shops.util.FF4ShopsUrlBuilder;
import ru.yandex.misc.lang.StringUtils;
import ru.yandex.misc.test.Assert;


public class SolomonJvmControllerTest extends FunctionalTest {
    private static final Pattern JETTY_PATTERN = Pattern.compile("org\\.eclipse\\.jetty\\.util\\.thread:.*");
    private static final Pattern HIKARI_POOL_PATTERN = Pattern.compile("com\\.zaxxer\\.hikari:type-Pool .*");
    @Test
    public void testGetSensors() throws IOException {
        var response = FunctionalTestHelper.get(FF4ShopsUrlBuilder.getSolomonJvmUrl(randomServerPort), String.class);
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

        var sensorsMap = new ObjectMapper().readValue(response.getBody(), HashMap.class);
        Assert.assertTrue(sensorsMap.containsKey("sensors"));


        var sensors = (List<Map<String, Object>>) sensorsMap.get("sensors");
        Assert.assertTrue(sensors.size() > 0);
        int jettyCount = 0;
        int hikariPoolCount = 0;

        var sensorNames = new TreeSet<String>();
        for (var sensor: sensors) {
            Assert.assertTrue(sensor.containsKey("kind"));
            Assert.assertTrue(sensor.containsKey("labels"));
            Assert.assertTrue(sensor.containsKey("ts"));
            Assert.assertTrue(sensor.containsKey("value"));

            var labels = (Map<String, String>) sensor.get("labels");
            String sensorName = labels.get("sensor");
            sensorNames.add(sensorName);
            Assert.assertTrue(StringUtils.isNotBlank(sensorName));
            Assert.assertTrue(StringUtils.isNotBlank((String) sensor.get("kind")));

            Assert.assertTrue(sensor.get("ts") instanceof Integer);
            Assert.assertTrue(sensor.get("value") instanceof Number);

            Integer ts = (Integer) sensor.get("ts");
            Assert.assertTrue(ts.compareTo(1500000000) > 0); // a valid timestamp

            switch (sensorName) {
                case "jvm.threads.daemons":
                case "jvm.threads.total":
                    Assert.assertTrue((int) sensor.get("value") > 0);
            }

            if (JETTY_PATTERN.matcher(sensorName).matches()) {
                jettyCount++;
            } else if (HIKARI_POOL_PATTERN.matcher(sensorName).matches()) {
                hikariPoolCount++;
            }
        }
        sensorNames.forEach(System.out::println);
        Assert.assertTrue(jettyCount > 0);
        Assert.assertTrue(hikariPoolCount > 0);
    }
}
