package ru.yandex.market.sre.services.tms.eventdetector.service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.market.sre.services.tms.eventdetector.dao.entity.ServiceIndicator;
import ru.yandex.market.sre.services.tms.eventdetector.dao.repository.ServiceIndicatorRepository;
import ru.yandex.market.sre.services.tms.eventdetector.model.core.FlapSettings;
import ru.yandex.market.sre.services.tms.eventdetector.model.core.StartrekIssueSettings;
import ru.yandex.market.sre.services.tms.eventdetector.model.settings.Sensor;
import ru.yandex.market.sre.services.tms.eventdetector.service.startrek.ComponentsService;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.StartrekClientBuilder;
import ru.yandex.startrek.client.model.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Ignore
public class SettingsValidatorTest {
    final ObjectMapper mapper = new ObjectMapper();
    private final String STARTREK_TOKEN = "";
    final Session startrekSession = new StartrekClientBuilder()
            .uri("https://st-api.yandex-team.ru")
            .socketTimeout(30, TimeUnit.SECONDS)
            .customFields(
                    Cf.map(
                            "sreBeginTime", Field.Schema.scalar(Field.Schema.Type.DATETIME, false),
                            "sreEndTime", Field.Schema.scalar(Field.Schema.Type.DATETIME, false),
                            "vdt", Field.Schema.scalar(Field.Schema.Type.INTEGER, false),
                            "crashId", Field.Schema.scalar(Field.Schema.Type.STRING, false)
                    )
            )
            .build(STARTREK_TOKEN);
    private final String sensorText = "{\n" +
            "  \"id\" : \"WHITE_TOUCH_NGINX\",\n" +
            "  \"service\" : \"1\",\n" +
            "  \"name\" : \"White Touch Nginx\",\n" +
            "  \"periodType\" : \"ONE_MIN\",\n" +
            "  \"group\" : \"FrontSpeed\",\n" +
            "  \"monitoringInterval\" : \"1d\",\n" +
            "  \"signalDelay\" : \"10m\",\n" +
            "  \"batch\" : {\n" +
            "    \"min\" : \"0s\",\n" +
            "    \"max\" : \"4h\"\n" +
            "  },\n" +
            "  \"targets\" : {\n" +
            "    \"threshold\" : \"color(constantLine(1.0)%2C%22red%22)\",\n" +
            "    \"query\" : \"divideSeries(movingAverage(one_min.market-front-unified.nginx2.v2.white_touch" +
            ".total.any-code.metrics.ttlb.quantiles.0_99,%2730minute%27),sumSeries(minSeries(timeStack" +
            "(movingAverage(one_min.market-front-unified.nginx2.v2.white_touch.total.any-code.metrics.ttlb" +
            ".quantiles.0_99,%2730minute%27),%27-1week%27,1,4)),stddevSeries(timeStack(movingAverage(one_min" +
            ".market-front-unified.nginx2.v2.white_touch.total.any-code.metrics.ttlb.quantiles.0_99," +
            "%2730minute%27),%27-1week%27,1,4)),stdev(one_min.market-front-unified.nginx2.v2.white_touch.total" +
            ".any-code.metrics.ttlb.quantiles.0_99,30,0)))\"\n" +
            "  },\n" +
            "  \"type\" : \"TTLB\",\n" +
            "  \"source\" : \"GRAPHITE\",\n" +
            "  \"flap\" : {\n" +
            "    \"stableTime\" : \"90m\",\n" +
            "    \"critTime\" : \"1h\",\n" +
            "    \"warnWindow\" : \"2h\",\n" +
            "    \"critWindow\" : \"2h\"\n" +
            "  },\n" +
            "  \"alerting\" : {\n" +
            "    \"components\" : [ \"@Market\", \"@Touch\" ],\n" +
            "    \"tags\" : [ \"type:frontend\", \"type:overall\", \"type:unified\", \"frontech_speed_alert\", " +
            "\"Контур#Скорость\", \"front:whiteTouch\", \"белый\", \"market:eventdetector\", \"type:ttlb\" ],\n" +
            "    \"alarm\" : {\n" +
            "      \"queue\" : \"MARKETFRONT\",\n" +
            "      \"ticketType\" : \"BUG\",\n" +
            "      \"grafana\" : {\n" +
            "        \"iframe\" : \"https://grafana.yandex-team" +
            ".ru/d-solo/aTpYl6kGk/market-front-speed-lab?panelId=10003&var-mov_avg=30m&var-service=white_touch" +
            "&var-serviceLegacy=market_front_touch&var-retroFactor=4&var-retroScale=1w\",\n" +
            "        \"fullscreen\" : \"https://grafana.yandex-team" +
            ".ru/d/aTpYl6kGk/market-front-speed-lab?fullscreen&panelId=10003&var-mov_avg=30m&var-service" +
            "=white_touch&var-serviceLegacy=market_front_touch&var-retroFactor=4&var-retroScale=1w\"\n" +
            "      }\n" +
            "    },\n" +
            "    \"incident\" : {\n" +
            "      \"ticketType\" : \"INCIDENT\"\n" +
            "    },\n" +
            "    \"registerIncident\" : false,\n" +
            "    \"eventLowTime\" : \"30m\",\n" +
            "    \"eventWarnTime\" : \"3m\",\n" +
            "    \"downtimeWarnTime\" : \"30m\",\n" +
            "    \"eventCritTime\" : \"15m\",\n" +
            "    \"downtimeCritTime\" : \"1h\",\n" +
            "    \"noDataTime\" : \"1h\"\n" +
            "  },\n" +
            "  \"preInterval\" : \"30m\",\n" +
            "  \"rawSignalName\" : \"one_min.market-front-unified.nginx2.v2.white_touch.total.any-code.metrics" +
            ".ttlb.quantiles.0_99\"\n" +
            "}";

    public String asString(ServiceIndicator indicator) throws JsonProcessingException {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(indicator);
    }

    @Test
    public void serialize() throws IOException {
        ServiceIndicator src = ServiceIndicatorRepository.getAll().get(0);
        String text = asString(src);
        ServiceIndicator converted = mapper.readValue(text, ServiceIndicator.class);
        assertEquals(text, asString(converted));
    }

    @Test
    public void defaults() throws IOException {
        ServiceIndicator src = new ServiceIndicator();
        src.setAlerting(new StartrekIssueSettings());
        src.setFlap(new FlapSettings());
        System.out.println(asString(src));
    }

    @Test(expected = RuntimeException.class)
    public void minimalConfig() throws IOException {
        String asString = "{\n" +
                " \"type\": \"TIMINGS\" \n" +
                " ,\"preInterval\": \"1hh\" \n" +
                "}";
        Sensor afterConvert = mapper.readValue(asString, Sensor.class);
        SettingsValidator validator = new SettingsValidator(null, null);
        validator.validate(afterConvert);
    }

    private String asString(Sensor sensor) throws JsonProcessingException {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(sensor);
    }

    @Test
    public void equality() throws IOException {
        Sensor sensor1 = mapper.readValue(sensorText, Sensor.class);
        Sensor sensor2 = mapper.readValue(sensorText, Sensor.class);
        assertEquals(sensor1, sensor2);
        sensor2.getAlerting().getTags().add("another:tag");
        assertNotEquals(sensor1, sensor2);
    }

    @Test
    @Ignore
    public void testComponentCheck() throws IOException {
        Sensor sensor1 = mapper.readValue(sensorText, Sensor.class);
        String testComponent = "not-existing-component";
        sensor1.getAlerting().setComponents(Collections.singletonList(testComponent));
        SettingsValidator validator = new SettingsValidator(startrekSession, new ComponentsService(startrekSession));
        try {
            validator.validate(sensor1);
        } catch (NullPointerException e) {
            assertTrue(e.getMessage().contains(testComponent));
            return;
        }
        fail(testComponent + " not exists, but not validated");
    }

    @Ignore
    @Test
    public void validateCurrentConfigs() {
        SettingsConverter converter = new SettingsConverter();
        SettingsValidator validator = new SettingsValidator(startrekSession, new ComponentsService(startrekSession));
        Map<String, List> grouped = new HashMap<>();
        ServiceIndicatorRepository.getAll().forEach(indicator -> {
            Sensor sensor = converter.convert(indicator);
            try {
                if (sensor.getAlerting().getRegisterIncident() && sensor.getAlerting().getIncident().getQueue() == null) {
                    sensor.getAlerting().getIncident().setQueue("MARKETINCIDENTS");
                }
                if (sensor.getAlerting().getAlarm().getQueue() == null) {
                    sensor.getAlerting().getAlarm().setQueue("MARKETALARMS");
                }
                validator.validate(sensor);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(sensor.getId());
                fail(e.getMessage());
            }
            if (!grouped.containsKey(indicator.getGroup())) {
                grouped.put(indicator.getGroup(), new ArrayList<Sensor>());
            }
            grouped.get(indicator.getGroup()).add(sensor);
        });
        grouped.forEach((k, v) -> {
            try {
                save(k, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(v));
            } catch (IOException e) {
                fail(e.getMessage());
            }
        });
    }

    public void save(String group, String text)
            throws IOException {
        String path = "/Users/asivolapov/Documents";
        BufferedWriter writer = new BufferedWriter(new FileWriter(path + "/" + group + ".json"));
        writer.write(text);
        writer.close();
    }
}
