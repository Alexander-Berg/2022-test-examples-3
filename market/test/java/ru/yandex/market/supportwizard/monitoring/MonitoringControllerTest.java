package ru.yandex.market.supportwizard.monitoring;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.supportwizard.config.BaseFunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.supportwizard.monitoring.MonitoringController.BASE_URL;
import static ru.yandex.market.supportwizard.monitoring.MonitoringController.TICKET_CREATION_TODAY_URL;

public class MonitoringControllerTest extends BaseFunctionalTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    @DbUnitDataSet(before = "testMonitoringTicketCreationTodayOK.csv")
    public void testMonitoringTicketCreationTodayOK() {
        ResponseEntity<String> entity =
                testRestTemplate.getForEntity(BASE_URL + TICKET_CREATION_TODAY_URL, String.class);
        assertEquals(entity.getStatusCode(), HttpStatus.OK);
        assertEquals(entity.getBody(), MonitoringService.OK_RESULT.toString());
    }

    @Test
    @DbUnitDataSet(before = "testMonitoringTicketCreationTodayCRIT.csv")
    public void testMonitoringTicketCreationTodayCRIT() {
        ResponseEntity<String> entity =
                testRestTemplate.getForEntity(BASE_URL + TICKET_CREATION_TODAY_URL, String.class);
        assertEquals(entity.getStatusCode(), HttpStatus.OK);
        assertEquals(entity.getBody(), MonitoringService.CRITICAL_RESULT.toString());
    }
}
