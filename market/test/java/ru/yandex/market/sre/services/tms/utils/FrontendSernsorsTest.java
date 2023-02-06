package ru.yandex.market.sre.services.tms.utils;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.sre.services.tms.eventdetector.enums.IndicatorSource;
import ru.yandex.market.sre.services.tms.eventdetector.enums.IndicatorType;
import ru.yandex.market.sre.services.tms.eventdetector.enums.PeriodType;
import ru.yandex.market.sre.services.tms.eventdetector.model.core.GrafanaTicketBlock;
import ru.yandex.market.sre.services.tms.eventdetector.model.settings.AlarmUserSettings;
import ru.yandex.market.sre.services.tms.eventdetector.model.settings.AlertingUserSettings;
import ru.yandex.market.sre.services.tms.eventdetector.model.settings.BatchUserSettings;
import ru.yandex.market.sre.services.tms.eventdetector.model.settings.FlapUserSettings;
import ru.yandex.market.sre.services.tms.eventdetector.model.settings.Sensor;
import ru.yandex.market.sre.services.tms.eventdetector.model.settings.TargetSettings;
import ru.yandex.market.sre.services.tms.utils.model.Alert;
import ru.yandex.market.sre.services.tms.utils.model.Platform;
import ru.yandex.market.sre.services.tms.utils.model.Sensors;

public class FrontendSernsorsTest {
    final String MARKET = "MARKET";
    final String BERU = "BERU";
    private final String SRT = "";
    private final String OBJECTS_FOLDER_PATH = "eventdetector/frontindicators/";

    protected String loadTextResource(String fileName) throws IOException {
        String name = OBJECTS_FOLDER_PATH + fileName;
        URL url = Resources.getResource(name);
        return Resources.toString(url, Charsets.UTF_8);
    }

    @Test
    @Ignore("For development checks only")
    public void processIndicators() throws IOException {
        processIndicators("sensors.json", "FrontSpeed");
    }

    @Test
    @Ignore("For development checks only")
    public void processPagesIndicators() throws IOException {
        processIndicators("sensorPages.json", "FrontPagesSpeed");
    }

    public void processIndicators(String fileName, String group) throws IOException {
        String json = loadTextResource(fileName);
        ObjectMapper mapper = new ObjectMapper();
        Sensors sensors = mapper.readValue(json, Sensors.class);
        List<Sensor> sensorList = new ArrayList<>();
        sensorList.addAll(processPlatform2(sensors.getWhiteTouch(), MARKET, group));
        sensorList.addAll(processPlatform2(sensors.getBlueTouch(), BERU, group));
        sensorList.addAll(processPlatform2(sensors.getWhiteDesktop(), MARKET, group));
        sensorList.addAll(processPlatform2(sensors.getBlueDesktop(), BERU, group));
        sensorList.addAll(processPlatform2(sensors.getBlueFAPI(), BERU, group));
        System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(sensorList));
    }


    private List<Sensor> processPlatform2(List<Platform> whiteTouch, String service, String group) throws JsonProcessingException {
        if (whiteTouch == null) {
            return Collections.emptyList();
        }
        List<Sensor> sensors = whiteTouch.stream().map(item -> {
            try {
                return convert(item, service, group);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }).collect(Collectors.toList());
        return sensors;
    }

    private Sensor convert(Platform sensor, String service, String group) throws Exception {
        if (!sensor.getHigher()) {
            throw new RuntimeException("Invalid sensor " + sensor.getTitle());
        }
        return Sensor.builder()
                .showInSloDash(null)
                .group(group)
                .flap(flap(sensor))
                .solomon(null)
                .rawSignalName(raw(sensor))
                .name(sensor.getTitle())
                .monitoringInterval("7d")
                .description(null)
                .service(service.equalsIgnoreCase("market") ? "1" : "2")
                .id(id(sensor.getTitle()))
                .alerting(alerting(service, sensor))
                .batch(batch(sensor))
                .periodType(PeriodType.ONE_MIN)
                .preInterval("30m")
                .signalDelay("10m")
                .source(IndicatorSource.GRAPHITE)
                .targets(targets(sensor))
                .type(IndicatorType.valueOf(sensor.getAlerting().getEventType().toUpperCase()))
                .build();
    }

    private AlertingUserSettings alerting(String service, Platform sensor) {
        Alert alert = sensor.getAlerting();
        return AlertingUserSettings.builder()
                .registerIncident(false)
                .incident(null)
                .tags(tagList(service, alert.getTicket().getTags()))
                .components(alert.getTicket().getComponents())
                .downtimeCritTime(alert.getCritTime())
                .downtimeWarnTime(alert.getWarnTime())
                .eventLowTime("30m")
                .eventCritTime("15m")
                .eventWarnTime("3m")
                .alarm(alarm(sensor))
                .noDataTime("3h")
                .build();
    }

    private AlarmUserSettings alarm(Platform sensor) {
        Alert alert = sensor.getAlerting();
        return AlarmUserSettings.builder()
                .ticketType(alert.getTicket().getType().toUpperCase())
                .queue(alert.getTicket().getQueue())
                .grafana(GrafanaTicketBlock.builder()
                        .iframe(sensor.getGrafana().getIframe())
                        .fullscreen(sensor.getGrafana().getFullscreen())
                        .build())
                .build();
    }

    private TargetSettings targets(Platform sensor) throws Exception {
        return TargetSettings.builder()
                .threshold("color(constantLine(" + sensor.getThreshold().toString() + ")%2C%22red%22)")
                .query(query(sensor.getQuery()))
                .build();
    }

    private BatchUserSettings batch(Platform sensor) {
        return BatchUserSettings.builder()
                .max("4h")
                .min("0s")
                .build();
    }

    private FlapUserSettings flap(Platform sensor) {
        return FlapUserSettings.builder()
                .stableTime(sensor.getAlerting().getGoodTime())
                .critTime(sensor.getAlerting().getCritTime())
                .critWindow(sensor.getAlerting().getWindowTime())
                .warnWindow(sensor.getAlerting().getWindowTime())
                .build();
    }

    private String raw(Platform sensor) throws Exception {
        if (sensor.getRawQuery() != null && !sensor.getRawQuery().isEmpty()) {
            return sensor.getRawQuery();
        }
        return query(sensor.getQuery());
    }

    private String id(String service) {
        return service.replaceAll(" ", "_").toUpperCase();
    }


    private List<String> tagList(String service, List<String> tags) {
        tags.add(service.equals(BERU) ? "синий" : "белый");
        return tags;
    }

    private String query(String query) throws Exception {
        query = query
                .replaceAll("w'", "week'")
                .replaceAll("m'", "minute'")
                .replaceAll(" ", "%20")
                .replaceAll("'", "%27");
        return query;
    }

}
