package ru.yandex.market.api.partner.controllers.outlet;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.api.partner.controllers.outlet.model.OutletsPagerWrapperDTO;
import ru.yandex.market.api.partner.controllers.util.PagerDTO;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.mbi.jaxb.jackson.ApiObjectMapperFactory;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.common.test.util.StringTestUtil.getString;

/**
 * Тестирование взаимодействия с HTTP интерфейсом контроллера поставок {@link OutletController}
 */
class OutletControllerFunctionalTest extends FunctionalTest {
    @Test
    @DbUnitDataSet(before = "outletDropship.before.csv")
    void testOkSupplierDropshipJson() {
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(outletsUrl(1000571241L), HttpMethod.POST, Format.JSON,
                getString(getClass(), "outletForDropshipCreation.json"));

        JsonTestUtil.assertEquals("{\"status\":\"OK\",\"result\":{\"id\":1}}", response.getBody());
    }

    @Test
    @DbUnitDataSet(before = "outletUpdate.before.csv", after = "outletUpdate.after.csv")
    void testCreateAndUpdateOutlet() {
        ResponseEntity<String> response =
                FunctionalTestHelper.makeRequest(outletsUrl(1000571241L), HttpMethod.POST, Format.JSON,
                        getString(getClass(),"outletForDropshipCreationMultiDR.json"));
        JsonTestUtil.assertEquals("{\"status\":\"OK\",\"result\":{\"id\":1}}", response.getBody());
        response = FunctionalTestHelper.makeRequest(specifiedOutletUrl(1L), HttpMethod.PUT, Format.JSON,
                        getString(getClass(),"outletForDropshipCreationMultiDR.json"));

        JsonTestUtil.assertEquals("{\"status\":\"OK\"}", response.getBody());
    }

    @Test
    @DbUnitDataSet(before = "outletUpdate.before.csv", after = "outletUpdate.after.csv")
    void testCreateAndUpdateSelfDeliveryOutlet() {
        ResponseEntity<String> response =
                FunctionalTestHelper.makeRequest(outletsUrl(1000571241L), HttpMethod.POST, Format.JSON,
                        getString(getClass(), "selfDeliveryOutlet.json"));
        JsonTestUtil.assertEquals("{\"status\":\"OK\",\"result\":{\"id\":1}}", response.getBody());
        response =
                FunctionalTestHelper.makeRequest(specifiedOutletUrl(1L), HttpMethod.PUT, Format.JSON,
                        getString(getClass(), "selfDeliveryOutlet.update.json"));

        JsonTestUtil.assertEquals("{\"status\":\"OK\"}", response.getBody());
    }

    @Test
    @DbUnitDataSet(before = "outletUpdate.before.csv", after = "createDbsOutletWithDefaultStoragePeriod.after.csv")
    void testCreateDbsOutletWithDefaultStoragePeriod() {
        FunctionalTestHelper.makeRequest(outletsUrl(10774L),
                HttpMethod.POST, Format.JSON, getString(getClass(), "selfDeliveryOutlet.json"));
    }

    @Test
    @DbUnitDataSet(before = "outletUpdate.before.csv", after = "createDbsOutletWithStoragePeriod.after.csv")
    void testCreateDbsOutletWithStoragePeriod() {
        FunctionalTestHelper.makeRequest(outletsUrl(10774L),
                HttpMethod.POST, Format.JSON, getString(getClass(), "selfDeliveryOutletWithStoragePeriod.json"));
    }

    @Test
    @DbUnitDataSet(before = "outletUpdate.before.csv", after = "testUpdateDbsOutletWithStoragePeriod.after.csv")
    void testUpdateDbsOutletWithStoragePeriod() {
        FunctionalTestHelper.makeRequest(specifiedOutletUrl(555778L, 10774L), HttpMethod.PUT, Format.JSON,
                getString(getClass(), "updateDbsOutletWithStoragePeriod.json"));
    }

    @Test
    @DbUnitDataSet(before = "outletUpdate.before.csv", after = "createDbsOutletWithInvalidStoragePeriods.after.csv")
    void testCreateDbsOutletWithInvalidMinStoragePeriods() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(outletsUrl(10774L), HttpMethod.POST, Format.JSON,
                        getString(getClass(), "selfDeliveryOutletWithMinStoragePeriod.json"))
        );

        assertThat(exception.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
        assertThat(exception.getResponseBodyAsString(), containsString("[Storage period should be between 3 and 14]"));
    }

    @Test
    @DbUnitDataSet(before = "outletUpdate.before.csv", after = "createDbsOutletWithInvalidStoragePeriods.after.csv")
    void testCreateDbsOutletWithInvalidMaxStoragePeriods() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(outletsUrl(10774L), HttpMethod.POST, Format.JSON,
                        getString(getClass(), "selfDeliveryOutletWithMaxStoragePeriod.json"))
        );

        assertThat(exception.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
        assertThat(exception.getResponseBodyAsString(), containsString("[Storage period should be between 3 and 14]"));
    }

    @Test
    @DbUnitDataSet(before = "outletUpdate.before.csv", after = "outletUpdateWithUpdate.after.csv")
    void testCreateAndUpdateModifiedOutlet() {
        ResponseEntity<String> response =
                FunctionalTestHelper.makeRequest(outletsUrl(1000571241L), HttpMethod.POST, Format.JSON,
                        getString(getClass(),"outletForDropshipCreationMultiDR.json"));
        JsonTestUtil.assertEquals("{\"status\":\"OK\",\"result\":{\"id\":1}}", response.getBody());
        response =
                FunctionalTestHelper.makeRequest(specifiedOutletUrl(1L), HttpMethod.PUT, Format.JSON,
                        getString(getClass(),"outletForDropshipCreationMultiDRModified.json"));

        JsonTestUtil.assertEquals("{\"status\":\"OK\"}", response.getBody());
    }

    @Test
    @DbUnitDataSet(before = "outletDropship.before.csv")
    void testOutletWithMoreThanOneEmailJson() {
        testInvalidInfoException(outletsUrl(1000571241L), HttpMethod.POST, Format.JSON, "outletWithMoreThanOneEmail.json");
    }

    @Test
    @DbUnitDataSet(before = "outletDropship.before.csv")
    void testInvalidInfoException() {
        testInvalidInfoException(outletsUrl(1000571241L), HttpMethod.POST, Format.JSON, "outletWithIncorrectName.json");
        testInvalidInfoException(outletsUrl(1000571241L), HttpMethod.POST, Format.JSON, "outletWithIncorrectName2.json");
        testInvalidInfoException(outletsUrl(1000571241L), HttpMethod.POST, Format.XML, "outletWithIncorrectName.xml");
        testInvalidInfoException(outletsUrl(1000571241L), HttpMethod.POST, Format.XML, "outletWithIncorrectName2.xml");
        testInvalidInfoException(specifiedOutletUrl(720469L), HttpMethod.PUT, Format.JSON, "outletWithIncorrectName.json");
        testInvalidInfoException(specifiedOutletUrl(720469L), HttpMethod.PUT, Format.XML, "outletWithIncorrectName.xml");
    }

    @Test
    @DisplayName("Точка без расписание не может быть передана")
    void testEmptySchedule() {
        testInvalidInfoException(specifiedOutletUrl(1L), HttpMethod.PUT, Format.JSON, "outletWithoutSchedule.json");
        testInvalidInfoException(specifiedOutletUrl(1L), HttpMethod.PUT, Format.XML, "outletWithoutSchedule.xml");
    }

    @Test
    @DisplayName("Точка продаж с не разрешенным типом региона не может быть создана")
    @DbUnitDataSet(before = "outletDropship.before.csv")
    void testOutletWithWrongRegionType() {
        testInvalidInfoException(outletsUrl(1000571241L), HttpMethod.POST, Format.JSON, "outletWithWrongRegion.json");
    }

    private void testInvalidInfoException(String url, HttpMethod httpMethod, Format format, String filename) {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(url, httpMethod, format,
                        getString(getClass(),filename))
        );
        assertThat(exception.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DbUnitDataSet(before = "getOutletsInfo.before.csv")
    void testGetOutletsPaging() throws IOException {
        //Дефолтная пейджинация
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(outletsUrl(1000571241L), HttpMethod.GET, Format.JSON);
        ObjectMapper jsonMapper = new ApiObjectMapperFactory().createJsonMapper();
        OutletsPagerWrapperDTO pagerWrapperDTO = jsonMapper.readValue(response.getBody(), OutletsPagerWrapperDTO.class);
        PagerDTO pager = pagerWrapperDTO.getPager();
        assertEquals(1, pager.getCurrentPage().intValue());
        assertEquals(6, pager.getPageSize().intValue());
        assertEquals(1, pager.getFrom().intValue());
        assertEquals(6, pager.getTo().intValue());

        //Недефолтный pageSize
        response = FunctionalTestHelper.makeRequest(outletsUrl(1000571241L) + "?pageSize=3", HttpMethod.GET, Format.JSON);
        pagerWrapperDTO = jsonMapper.readValue(response.getBody(), OutletsPagerWrapperDTO.class);
        pager = pagerWrapperDTO.getPager();
        assertEquals(1, pager.getCurrentPage().intValue());
        assertEquals(3, pager.getPageSize().intValue());
        assertEquals(1, pager.getFrom().intValue());
        assertEquals(3, pager.getTo().intValue());

        //Недефолтный pageSize и pageNum
        response = FunctionalTestHelper.makeRequest(outletsUrl(1000571241L) + "?pageSize=3&page=2", HttpMethod.GET, Format.JSON);
        pagerWrapperDTO = jsonMapper.readValue(response.getBody(), OutletsPagerWrapperDTO.class);
        pager = pagerWrapperDTO.getPager();
        assertEquals(2, pager.getCurrentPage().intValue());
        assertEquals(3, pager.getPageSize().intValue());
        assertEquals(4, pager.getFrom().intValue());
        assertEquals(6, pager.getTo().intValue());

        //Несуществующая страница
        response = FunctionalTestHelper.makeRequest(outletsUrl(1000571241L) + "?pageSize=3&page=3", HttpMethod.GET, Format.JSON);
        pagerWrapperDTO = jsonMapper.readValue(response.getBody(), OutletsPagerWrapperDTO.class);
        pager = pagerWrapperDTO.getPager();
        assertEquals(3, pager.getCurrentPage().intValue());
        assertEquals(0, pager.getPageSize().intValue());
        assertEquals(0, pager.getFrom().intValue());
        assertEquals(0, pager.getTo().intValue());
    }

    @Test
    @DbUnitDataSet(before = "getOutletsInfo.before.csv")
    void testGetOutletsSerialization() {
        String jsonResponse1 = FunctionalTestHelper.makeRequest(outletsUrl(1000571241L) +
                "?pageSize=3&page=1", HttpMethod.GET, Format.JSON).getBody();
        String expectedJsonResponse1 = getString(getClass(),"getOutletsResponse1.json");
        MbiAsserts.assertJsonEquals(expectedJsonResponse1, jsonResponse1);


        String jsonResponse = FunctionalTestHelper.makeRequest(outletsUrl(1000571241L) +
                "?pageSize=3&page=2", HttpMethod.GET, Format.JSON).getBody();
        String expectedJsonResponse = getString(getClass(),"getOutletsResponse.json");
        MbiAsserts.assertJsonEquals(expectedJsonResponse, jsonResponse);

        String xmlResponse = FunctionalTestHelper.makeRequest(outletsUrl(1000571241L) +
                "?pageSize=3&page=2", HttpMethod.GET, Format.XML).getBody();
        String expectedXmlResponse = getString(getClass(),"getOutletsResponse.xml");
        MbiAsserts.assertXmlEquals(expectedXmlResponse, xmlResponse);
    }

    private String specifiedOutletUrl(long outletId) {
        return specifiedOutletUrl(outletId, 1000571241L);
    }

    private String specifiedOutletUrl(long outletId, long campaignId) {
        return String.format("%s/%d", outletsUrl(campaignId), outletId);
    }

    private String outletsUrl(long campaignId) {
        return String.format("%s/campaigns/%d/outlets", urlBasePrefix, campaignId);
    }

    @Test
    @DbUnitDataSet(before = "deleteOutlet.before.csv", after = "deleteOutlet.after.csv")
    void testDeleteOutlet() {
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(specifiedOutletUrl(101L), HttpMethod.DELETE, Format.JSON);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonTestUtil.assertEquals("{\"status\":\"OK\"}", response.getBody());
    }

}
