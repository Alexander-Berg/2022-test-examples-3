package ru.yandex.market.deliverycalculator.indexer.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.indexer.FunctionalTest;

class HealthCheckControllerFunctionalTest extends FunctionalTest {

    @Test
    void checkNotTariffTest() {
        ResponseEntity<String> response = monitorTariffChangeRequest();
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals("0;Ok", response.getBody());
    }

    @Test
    @DbUnitDataSet(before = "data/db/checkUpdateTariffs.before.csv")
    void checkUpdateTariffTest() {
        ResponseEntity<String> response = monitorTariffChangeRequest();
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals("0;Ok", response.getBody());
    }

    @Test
    @DbUnitDataSet(before = "data/db/checkUpdateTariffsCrit.before.csv")
    void checkUpdateTariffCritTest() {
        ResponseEntity<String> response = monitorTariffChangeRequest();
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(
                "2;CRIT DELETE program:BERU_CROSSDOCK tariffs:100001, 100002, 100003, 100004, 100005, 100006, 100007 carriers:1003375",
                response.getBody());
    }

    @Test
    @DbUnitDataSet(before = "data/db/checkUpdateTariffsWarn.before.csv")
    void checkUpdateTariffWarnTest() {
        ResponseEntity<String> response = monitorTariffChangeRequest();
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(
                "1;WARN DELETE program:BERU_CROSSDOCK tariffs:100002, 100003, 100004, 100005, 100007 carriers:1003375",
                response.getBody());
    }

    private ResponseEntity<String> monitorTariffChangeRequest() {
        return REST_TEMPLATE.getForEntity(baseUrl + "/monitorTariffChanges", String.class);
    }
}
