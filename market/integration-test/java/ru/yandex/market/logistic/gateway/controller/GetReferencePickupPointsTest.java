package ru.yandex.market.logistic.gateway.controller;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.GetReferencePickupPointsRequest;
import ru.yandex.market.logistic.gateway.utils.CommonDtoFactory;
import ru.yandex.market.logistic.gateway.utils.DeliveryDtoFactory;
import ru.yandex.market.logistic.gateway.utils.JsonMatcher;

import static ru.yandex.market.logistic.api.model.common.PartnerMethod.GET_REFERENCE_PICKUP_POINTS_DS;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class GetReferencePickupPointsTest extends AbstractIntegrationTest {

    private MockRestServiceServer mockServer;

    @Before
    public void setup() {
        mockServer = createMockServerByRequest(GET_REFERENCE_PICKUP_POINTS_DS);
    }

    @After
    public void tearDown() {
        mockServer.verify();
    }

    @Test
    public void executeSuccessSearchByLocations() throws Exception {
        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/delivery/get_reference_pickup_points/search_by_locations.xml",
            "fixtures/response/delivery/get_reference_pickup_points/response.xml"
        );

        GetReferencePickupPointsRequest request = new GetReferencePickupPointsRequest(
            List.of(CommonDtoFactory.createLocationFilterRussia()),
            null,
            null,
            CommonDtoFactory.createPartner()
        );

        executeScenario(request);
    }

    @Test
    public void executeSuccessSearchByCodes() throws Exception {
        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/delivery/get_reference_pickup_points/search_by_codes.xml",
            "fixtures/response/delivery/get_reference_pickup_points/response.xml"
        );

        GetReferencePickupPointsRequest request = new GetReferencePickupPointsRequest(
            null,
            List.of("test-code"),
            null,
            CommonDtoFactory.createPartner()
        );

        executeScenario(request);
    }

    @Test
    public void executeSuccessCalendarInterval() throws Exception {
        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/delivery/get_reference_pickup_points/calendar_interval.xml",
            "fixtures/response/delivery/get_reference_pickup_points/response.xml"
        );

        GetReferencePickupPointsRequest request = new GetReferencePickupPointsRequest(
            null,
            null,
            DeliveryDtoFactory.createDateTimeInterval(),
            CommonDtoFactory.createPartner()
        );

        executeScenario(request);
    }

    @Test
    public void executeSearchByLocationsAndGetInvalidPoint() throws Exception {
        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/delivery/get_reference_pickup_points/search_by_locations.xml",
            "fixtures/response/delivery/get_reference_pickup_points/response_with_invalid_point.xml"
        );

        GetReferencePickupPointsRequest request = new GetReferencePickupPointsRequest(
            List.of(CommonDtoFactory.createLocationFilterRussia()),
            null,
            null,
            CommonDtoFactory.createPartner()
        );

        executeScenario(request);
    }

    private void executeScenario(GetReferencePickupPointsRequest request) throws Exception {
        String jsonRequest = jsonMapper.writeValueAsString(request);

        RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/delivery/getReferencePickupPoints")
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonRequest);

        String expectedJson = getFileContent(
            "fixtures/response/delivery/get_reference_pickup_points/response.json");

        String actualJson = mockMvc.perform(requestBuilder)
            .andReturn()
            .getResponse()
            .getContentAsString();

        softAssert.assertThat(new JsonMatcher(expectedJson).matches(actualJson))
            .as("Asserting that JSON response is correct")
            .isTrue();
    }
}
