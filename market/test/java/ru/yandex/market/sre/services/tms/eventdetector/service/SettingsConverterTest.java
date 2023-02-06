package ru.yandex.market.sre.services.tms.eventdetector.service;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.fest.util.Collections;
import org.junit.Test;

import ru.yandex.market.sre.services.tms.eventdetector.dao.entity.ServiceIndicator;
import ru.yandex.market.sre.services.tms.eventdetector.dao.repository.ServiceIndicatorRepository;
import ru.yandex.market.sre.services.tms.eventdetector.enums.IndicatorSource;
import ru.yandex.market.sre.services.tms.eventdetector.model.settings.Sensor;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.sre.services.tms.eventdetector.service.SettingsConverter.escape;

public class SettingsConverterTest {
    public String asString(ServiceIndicator indicator) throws JsonProcessingException {
        return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(indicator);
    }

    public String asString(Sensor indicator) throws JsonProcessingException {
        return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(indicator);
    }

    public void convert(SettingsConverter converter, ServiceIndicator src) throws JsonProcessingException {
        String beforeAsString = asString(src);
        Sensor sensor = converter.convert(src);
        String sensorAsString = asString(sensor);
        ServiceIndicator after = converter.convert(sensor);
        String afterAsString = asString(after);
//        System.out.println(beforeAsString);
        System.out.println(sensorAsString);
//        System.out.println(afterAsString);)
        if (src.getIndicatorSource() == IndicatorSource.GRAPHITE) {
            List<String> targets = src.getTargets();
            src.setTargets(Collections.list(escape(targets.get(0)), escape(targets.get(1))));
            beforeAsString = asString(src);
        }
        assertEquals(beforeAsString, afterAsString);
    }

    @Test
    public void convert() {
        SettingsConverter converter = new SettingsConverter();
        ServiceIndicatorRepository.getAll().forEach(i -> {
            try {
                convert(converter, i);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });
    }
}
