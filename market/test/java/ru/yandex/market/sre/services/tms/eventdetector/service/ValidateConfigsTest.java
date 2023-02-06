package ru.yandex.market.sre.services.tms.eventdetector.service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.market.sre.services.tms.eventdetector.model.settings.Sensor;
import ru.yandex.market.sre.services.tms.eventdetector.service.startrek.ComponentsService;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.StartrekClientBuilder;
import ru.yandex.startrek.client.model.Field;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ValidateConfigsTest {

    private final String STARTREK_TOKEN = ""; //Type your token here
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

    @Test
    public void validate() {
        try {
            SettingsValidator validator = new SettingsValidator(null, null);
            SettingsConverter converter = new SettingsConverter();
            ConfigImportService importService = new ConfigImportService(new ObjectMapper(), validator,
                    null, null, converter);
            if (System.getenv().get("ARCADIA_SOURCE_ROOT") != null) {
                String configsPath = System.getenv().get("ARCADIA_SOURCE_ROOT") + "/market/reliability/sre_tms/src" +
                        "/main/properties.d";
                System.setProperty("configs.path", configsPath);
            } else {
                System.setProperty("configs.path", "./src/main/properties.d");
            }

            List<Sensor> sensors = importService.loadAndValidateSensors("sensors");
            assertTrue("No sensors found", sensors.size() > 0);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Ignore
    @Test
    public void validateFull() {
        try {
            SettingsValidator validator = new SettingsValidator(startrekSession,
                    new ComponentsService(startrekSession));
            SettingsConverter converter = new SettingsConverter();
            ConfigImportService importService = new ConfigImportService(new ObjectMapper(), validator,
                    null, null, converter);
            if (System.getenv().get("ARCADIA_SOURCE_ROOT") != null) {
                String configsPath = System.getenv().get("ARCADIA_SOURCE_ROOT") + "/market/reliability/sre_tms/src" +
                        "/main/properties.d";
                System.setProperty("configs.path", configsPath);
            } else {
                System.setProperty("configs.path", "./src/main/properties.d");
            }

            List<Sensor> sensors = importService.loadAndValidateSensors("sensors");
            assertTrue("No sensors found", sensors.size() > 0);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
