package ru.yandex.market.sre.services.tms.eventdetector.dao.repository.indicator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class AxaptaAvailabilityTest {

    @Ignore
    @Test
    public void getAll() throws JsonProcessingException {
        AxaptaAvailability availability = new AxaptaAvailability();
        String text = new ObjectMapper().writeValueAsString(availability.getAll().get(0));
        System.out.println(text);
    }
}
