package ru.yandex.market.logistic.gateway.controller;

import java.time.Instant;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.delivery.gruzin.client.GruzinClient;
import ru.yandex.market.delivery.gruzin.model.CargoUnitCreateDto;
import ru.yandex.market.delivery.gruzin.model.CargoUnitsCreateDto;
import ru.yandex.market.delivery.gruzin.model.UnitCargoType;
import ru.yandex.market.delivery.gruzin.model.UnitType;
import ru.yandex.market.delivery.gruzin.model.WarehouseId;
import ru.yandex.market.delivery.gruzin.model.WarehouseIdType;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistics.test.integration.matchers.XmlMatcher;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class PushCargoUnitsFulfillmentTest extends AbstractIntegrationTest {
    @Autowired
    private GruzinClient gruzinClient;

    @Test
    public void testFulfillmentPushCargoUnitsSuccessful() throws Exception {
        String xmlResponse = performRequest("fixtures/request/fulfillment/push_cargo_units/" +
            "fulfillment_push_cargo_units.xml");

        verify(gruzinClient).pushCargoUnits(Mockito.eq(
            createCargoUnitsSucessDto()
        ));

        String expectedXml = getFileContent("fixtures/response/fulfillment/push_cargo_units/" +
            "fulfillment_push_cargo_units.xml");

        softAssert.assertThat(new XmlMatcher(expectedXml).matches(xmlResponse))
            .as("Asserting that the XML response is correct")
            .isTrue();
    }


    @Test
    public void testFulfillmentPushCargoUnitsNullTargetWarehouse() throws Exception {
        String xmlResponse = performRequest("fixtures/request/fulfillment/push_cargo_units/" +
            "fulfillment_push_cargo_units_null_target_warehouse.xml");

        verify(gruzinClient).pushCargoUnits(Mockito.eq(
            createCargoUnitsSucessNullTargetWarehouseDto()
        ));

        String expectedXml = getFileContent("fixtures/response/fulfillment/push_cargo_units/" +
            "fulfillment_push_cargo_units.xml");

        softAssert.assertThat(new XmlMatcher(expectedXml).matches(xmlResponse))
            .as("Asserting that the XML response is correct")
            .isTrue();
    }

    @Test
    public void testSeveralPartnersPushCargoUnitsSuccessful() throws Exception {
        String xmlResponse = performRequest("fixtures/request/fulfillment/push_cargo_units/" +
            "universal_fulfillment_push_cargo_units.xml");

        // Пушим только для 1-го партнёра, если несколько
        verify(gruzinClient).pushCargoUnits(Mockito.eq(
            createCargoUnitsSucessDto()
                .setPartnerId(45L)
        ));

        String expectedXml = getFileContent("fixtures/response/fulfillment/push_cargo_units/" +
            "fulfillment_push_cargo_units.xml");

        softAssert.assertThat(new XmlMatcher(expectedXml).matches(xmlResponse))
            .as("Asserting that the XML response is correct")
            .isTrue();
    }

    @Test
    public void testFulfillmentPushPushOrdersStatusHistoryInvalidTokenError() throws Exception {
        String xmlResponse = performRequest("fixtures/request/fulfillment/push_cargo_units/" +
            "fulfillment_push_cargo_units_invalid_token.xml");

        verifyNoInteractions(gruzinClient);

        String expectedXml = getFileContent("fixtures/response/fulfillment/push_cargo_units/" +
            "fulfillment_push_cargo_units_invalid_token.xml");

        softAssert.assertThat(new XmlMatcher(expectedXml).matches(xmlResponse))
            .as("Asserting that the XML response is correct")
            .isTrue();
    }

    private String performRequest(String contentPath) throws Exception {
        return mockMvc.perform(post("/fulfillment/query-gateway")
                .contentType(MediaType.TEXT_XML_VALUE)
                .accept(MediaType.TEXT_XML_VALUE)
                .content(getFileContent(contentPath)))
            .andExpect(status().is2xxSuccessful())
            .andReturn()
            .getResponse()
            .getContentAsString();
    }

    private static CargoUnitsCreateDto createCargoUnitsSucessDto() {
        return new CargoUnitsCreateDto()
            .setPartnerId(145L)
            .setTargetWarehouse(new WarehouseId(WarehouseIdType.PARTNER, 172L))
            .setSnapshotDateTime(Instant.parse("2017-09-11T07:30:00Z"))
            .setUnits(List.of(
                new CargoUnitCreateDto()
                    .setId("DRP0001")
                    .setUnitType(UnitType.PALLET)
                    .setUnitCargoType(UnitCargoType.INTERWAREHOUSE_FIT)
                    .setCreationDate(Instant.parse("2017-09-10T17:00:00Z"))
                    .setPlannedOutboundDate(Instant.parse("2017-09-11T17:00:00Z")),
                new CargoUnitCreateDto()
                    .setId("BOX0001")
                    .setParentId("DRP0001")
                    .setUnitType(UnitType.BOX)
                    .setUnitCargoType(UnitCargoType.INTERWAREHOUSE_FIT)
                    .setCreationOutboundId("TMU12345")
                    .setCreationDate(Instant.parse("2017-09-11T07:16:01Z"))
                    .setPlannedOutboundDate(Instant.parse("2017-09-11T17:00:00Z"))
            ));
    }

    private static CargoUnitsCreateDto createCargoUnitsSucessNullTargetWarehouseDto() {
        return new CargoUnitsCreateDto()
            .setPartnerId(145L)
            .setSnapshotDateTime(Instant.parse("2017-09-11T07:30:00Z"))
            .setUnits(List.of(
                new CargoUnitCreateDto()
                    .setId("DRP0001")
                    .setUnitType(UnitType.PALLET)
                    .setUnitCargoType(UnitCargoType.INTERWAREHOUSE_FIT)
                    .setCreationDate(Instant.parse("2017-09-10T17:00:00Z"))
                    .setPlannedOutboundDate(Instant.parse("2017-09-11T17:00:00Z")),
                new CargoUnitCreateDto()
                    .setId("BOX0001")
                    .setParentId("DRP0001")
                    .setUnitType(UnitType.BOX)
                    .setUnitCargoType(UnitCargoType.INTERWAREHOUSE_FIT)
                    .setCreationOutboundId("TMU12345")
                    .setCreationDate(Instant.parse("2017-09-11T07:16:01Z"))
                    .setPlannedOutboundDate(Instant.parse("2017-09-11T17:00:00Z"))
            ));
    }
}
