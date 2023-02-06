package ru.yandex.market.logistics.management.controller;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.util.CleanDatabase;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@CleanDatabase
class PartnerRouteControllerTest extends AbstractContextualTest {

    private static final long PARTNER_ID = 3000;
    private static final String CREATE_ROUTE_URL = String.format("/externalApi/partners/%s/route", PARTNER_ID);

    @Test
    @DatabaseSetup("/data/controller/partnerRoute/partner_wo_route.xml")
    @ExpectedDatabase(
        value = "/data/controller/partnerRoute/partner_with_route.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void testCreateRouteSuccessfully() throws Exception {
        execute(CREATE_ROUTE_URL, HttpMethod.POST, "data/controller/partnerRoute/create_route_request.json")
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/partnerRoute/create_route_response.json", true));
    }

    @Test
    @DatabaseSetup("/data/controller/partnerRoute/partner_with_route.xml")
    @ExpectedDatabase(
        value = "/data/controller/partnerRoute/partner_with_route.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void testCreateDuplicateRouteFail() throws Exception {
        execute(CREATE_ROUTE_URL, HttpMethod.POST, "data/controller/partnerRoute/create_route_request.json")
            .andExpect(status().isConflict())
            .andExpect(status().reason(
                "Partner route already exists for partner 3000 from location 100 to location 500"
            ));
    }

    @Test
    @DatabaseSetup("/data/controller/partnerRoute/partner_wo_route_nonexisting_region.xml")
    @ExpectedDatabase(
        value = "/data/controller/partnerRoute/partner_wo_route_nonexisting_region.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void testCreateRouteForNonExistingRegionFail() throws Exception {
        execute(CREATE_ROUTE_URL, HttpMethod.POST, "data/controller/partnerRoute/create_route_request.json")
            .andExpect(status().isNotFound())
            .andExpect(status().reason("No regions with ids [500] found"));
    }

    @Test
    @DatabaseSetup("/data/controller/partnerRoute/regions_wo_partner.xml")
    @ExpectedDatabase(
        value = "/data/controller/partnerRoute/regions_wo_partner.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void testCreateRouteForNonExistingPartnerFail() throws Exception {
        execute(CREATE_ROUTE_URL, HttpMethod.POST, "data/controller/partnerRoute/create_route_request.json")
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Partners with ids [3000] not found"));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @DisplayName("Невалидный запрос создания магистрали")
    @MethodSource("getInvalidCreateRouteRequests")
    void testCreateRouteWithInvalidRequest(
        @SuppressWarnings("unused") String caseName,
        String requestBodyPath,
        String responsePath
    ) throws Exception {
        execute(CREATE_ROUTE_URL, HttpMethod.POST, requestBodyPath)
            .andExpect(status().isBadRequest())
            .andExpect(testJson(responsePath, Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER));
    }

    private static Stream<Arguments> getInvalidCreateRouteRequests() {
        return Stream.of(
            Arguments.of(
                "Запрос без locationFrom",
                "data/controller/partnerRoute/invalid_create_request_wo_locationFrom.json",
                "data/controller/partnerRoute/invalid_create_response_wo_locationFrom.json"
            ),
            Arguments.of(
                "Запрос без locationTo",
                "data/controller/partnerRoute/invalid_create_request_wo_locationTo.json",
                "data/controller/partnerRoute/invalid_create_response_wo_locationTo.json"
            ),
            Arguments.of(
                "Запрос без weekDays",
                "data/controller/partnerRoute/invalid_create_request_wo_weekDays.json",
                "data/controller/partnerRoute/invalid_create_response_wo_weekDays.json"
            ),
            Arguments.of(
                "Запрос с пустым списком weekDays",
                "data/controller/partnerRoute/create_route_request_empty_days.json",
                "data/controller/partnerRoute/invalid_create_response_empty_days.json"
            ),
            Arguments.of(
                "Запрос со списком weekDays, содержащим null",
                "data/controller/partnerRoute/create_route_request_days_contain_null.json",
                "data/controller/partnerRoute/invalid_create_response_days_contain_null.json"
            )
        );
    }

    private ResultActions execute(
        @Nonnull String urlTemplate,
        @Nonnull HttpMethod httpMethod,
        @Nullable String requestBodyPath
    ) throws Exception {
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
            .request(httpMethod, urlTemplate)
            .contentType(MediaType.APPLICATION_JSON);
        if (requestBodyPath != null) {
            builder.content(pathToJson(requestBodyPath));
        }
        return mockMvc.perform(builder);
    }
}
