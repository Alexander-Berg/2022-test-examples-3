package ru.yandex.market.logistics.management.controller.admin;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.TestableClock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DatabaseSetup("/data/controller/admin/partnerRelation/prepare_data.xml")
@ParametersAreNonnullByDefault
class SupportControllerPartnerRelationTest extends AbstractContextualTest {
    @Autowired
    private TestableClock clock;

    @BeforeEach
    void setup() {
        clock.setFixed(
            LocalDate.of(2018, 10, 29).atStartOfDay(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
        );
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerRelation/new_product_rating.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testCreateNewProductRatingSuccess() throws Exception {
        mockMvc.perform(
            post("/support/lms/partner-relation/2/ratings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.pathToJson("data/controller/admin/partnerRelation/new_product_rating.json"))
        )
            .andExpect(status().isCreated())
            .andExpect(header().string("location", "http://localhost/support/lms/partner-relation/2/ratings/4"))
            .andExpect(TestUtil.testJson("data/controller/admin/partnerRelation/created_product_rating.json"));
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerRelation/product_rating_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testPutProductRatingSuccessChange() throws Exception {
        mockMvc.perform(
            put("/support/lms/partner-relation/1/ratings/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.pathToJson("data/controller/admin/partnerRelation/update_product_rating.json"))
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson("data/controller/admin/partnerRelation/updated_product_rating.json"));
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerRelation/delete_rating.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testDeleteRatingSuccess() throws Exception {
        mockMvc.perform(delete("/support/lms/partner-relation/1/ratings/1")).andExpect(status().isNoContent());
    }

    //Cutoff tests

    @Test
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerRelation/create_cutoff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createCutOffSuccess() throws Exception {
        mockMvc.perform(
            post("/support/lms/partner-relation/2/cutoffs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    TestUtil.pathToJson("data/controller/admin/partnerRelation/new_cutoff.json"))
        )
            .andExpect(status().isCreated())
            .andExpect(TestUtil.testJson("data/controller/admin/partnerRelation/created_cutoff.json"));
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerRelation/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createCutOffIfAlreadyExists() throws Exception {
        mockMvc.perform(
            post("/support/lms/partner-relation/1/cutoffs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.pathToJson("data/controller/admin/partnerRelation/exists_cutoff.json"))
        )
            .andExpect(status().isConflict());
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerRelation/update_cutoff_different_values.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateCutoffWithDifferentValues() throws Exception {
        mockMvc.perform(
            put("/support/lms/partner-relation/1/cutoffs/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.pathToJson("data/controller/admin/partnerRelation/new_cutoff_for_update.json"))
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson("data/controller/admin/partnerRelation/updated_cutoff.json"));
        checkBuildWarehouseSegmentTask(1L, 2L, 3L, 4L);
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerRelation/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateCutoffWithSameValues() throws Exception {
        mockMvc.perform(
            put("/support/lms/partner-relation/1/cutoffs/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.pathToJson("data/controller/admin/partnerRelation/exists_cutoff.json"))
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson("data/controller/admin/partnerRelation/exists_updated_cutoff.json"));
        checkBuildWarehouseSegmentTask(1L, 2L, 3L, 4L);
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerRelation/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateNotExistCutoff() throws Exception {
        mockMvc.perform(
            put("/support/lms/partner-relation/2/cutoff/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.pathToJson("data/controller/admin/partnerRelation/new_cutoff.json"))
        )
            .andExpect(status().isNotFound());
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerRelation/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateCutoffForWrongPartnerRelation() throws Exception {
        mockMvc.perform(
            put("/support/lms/partner-relation/2/cutoff/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.pathToJson("data/controller/admin/partnerRelation/new_cutoff.json"))
        )
            .andExpect(status().isNotFound());
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerRelation/delete_cutoff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteExistsCutoff() throws Exception {
        mockMvc.perform(delete("/support/lms/partner-relation/1/cutoffs/1")).andExpect(status().isNoContent());
        checkBuildWarehouseSegmentTask(1L, 2L, 3L, 4L);
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerRelation/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteNotExistsCutoff() throws Exception {
        mockMvc.perform(delete("/support/lms/partner-relation/2/cutoffs/1")).andExpect(status().isNoContent());
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    void testErrorStatuses(
        @SuppressWarnings("unused") String name,
        HttpMethod method,
        String requestUri,
        String payload,
        ResultMatcher matcher
    ) throws Exception {
        mockMvc.perform(
            request(method, "/support/lms/partner-relation" + requestUri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
        )
            .andExpect(matcher);
    }

    @Nonnull
    private static Stream<Arguments> testErrorStatuses() {
        return Stream.of(
            Arguments.of(
                "POST existing product rating result in 409",
                HttpMethod.POST,
                "/1/ratings",
                TestUtil.pathToJson("data/controller/admin/partnerRelation/new_product_rating.json"),
                status().isConflict()
            ),
            Arguments.of(
                "GET not existing partner relation result in 404",
                HttpMethod.GET,
                "/24",
                TestUtil.emptyJson(),
                status().isNotFound()

            ),
            Arguments.of(
                "GET product rating for not existing partner relation result in 404",
                HttpMethod.GET,
                "/24/ratings",
                TestUtil.emptyJson(),
                status().isNotFound()

            ),
            Arguments.of(
                "PUT product rating for not existing partner relation result in 404",
                HttpMethod.PUT,
                "/24/ratings/1",
                TestUtil.pathToJson("data/controller/admin/partnerRelation/update_product_rating.json"),
                status().isNotFound()
            ),
            Arguments.of(
                "DELETE product rating for not existing partner relation result in 404",
                HttpMethod.DELETE,
                "/ratings/153",
                TestUtil.emptyJson(),
                status().isNotFound()
            ),
            Arguments.of(
                "GET day off for not existing partner relation result in 404",
                HttpMethod.GET,
                "/3/days-off/1",
                TestUtil.emptyJson(),
                status().isNotFound()
            ),
            Arguments.of(
                "GET non existing day off result in 404",
                HttpMethod.GET,
                "/1/days-off/3",
                TestUtil.emptyJson(),
                status().isNotFound()
            ),
            Arguments.of(
                "GET day off when it doesn't belong to partner relation result in 404",
                HttpMethod.GET,
                "/2/days-off/1",
                TestUtil.emptyJson(),
                status().isNotFound()
            ),
            Arguments.of(
                "GET registers for non existing partner relation result in 404",
                HttpMethod.GET,
                "/4/registers",
                TestUtil.emptyJson(),
                status().isNotFound()
            ),
            Arguments.of(
                "GET intakes for non existing partner relation result in 404",
                HttpMethod.GET,
                "/4/intakes",
                TestUtil.emptyJson(),
                status().isNotFound()
            )
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    void testGetMethodsNoErrors(@SuppressWarnings("unused") String name, String requestUri, String response)
        throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/support/lms/partner-relation" + requestUri)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.content().json(response, true));
    }

    @Nonnull
    private static Stream<Arguments> testGetMethodsNoErrors() {
        return Stream.of(
            Arguments.of("GET all product ratings return empty list", "/2/ratings", TestUtil.emptyJsonList()),
            Arguments.of(
                "GET all product ratings return 3",
                "/1/ratings",
                TestUtil.pathToJson("data/controller/admin/partnerRelation/all_product_ratings.json")
            ),
            Arguments.of(
                "GET one product ratings success",
                "/1/ratings/1",
                TestUtil.pathToJson("data/controller/admin/partnerRelation/one_product_rating.json")
            ),
            Arguments.of(
                "GET all cutoffs for partnerRelation with id = 1",
                "/1/cutoffs",
                TestUtil.pathToJson("data/controller/admin/partnerRelation/cutoffs_list.json")
            )
        );
    }
}
