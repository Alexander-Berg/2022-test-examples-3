package ru.yandex.market.marketId.controller;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.marketId.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DbUnitDataSet(before = "OrganizationNameCleanControllerTest.before.csv")
public class OrganizationNameCleanControllerTest extends FunctionalTest {

    @Test
    @DisplayName("Тест очистки имен организации")
    @DbUnitDataSet(after = "OrganizationNameCleanControllerTest.after.csv")
    void testCleanOrganizationNames() {
        String url = UriComponentsBuilder.fromUriString(baseUrl()).path("/organizations/fixNames").toUriString();
        ResponseEntity<String> response = FunctionalTestHelper.post(url, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
