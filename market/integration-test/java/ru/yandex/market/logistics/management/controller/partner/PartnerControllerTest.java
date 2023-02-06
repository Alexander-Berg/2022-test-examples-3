package ru.yandex.market.logistics.management.controller.partner;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.controller.PartnerController;
import ru.yandex.market.logistics.management.domain.entity.Partner;
import ru.yandex.market.logistics.management.domain.entity.type.StockSyncSwitchReason;
import ru.yandex.market.logistics.management.queue.producer.LogbrokerEventTaskProducer;
import ru.yandex.market.logistics.management.repository.PartnerRepository;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.TestableClock;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.emptyJsonList;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

/**
 * Интеграционный тест {@link PartnerController}.
 */
@DatabaseSetup("/data/controller/partner/prepare_data.xml")
@SuppressWarnings("unchecked")
class PartnerControllerTest extends AbstractContextualTest {

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private LogbrokerEventTaskProducer logbrokerEventTaskProducer;
    @Autowired
    private TestableClock clock;

    @BeforeEach
    void setup() {
        Mockito.doNothing().when(logbrokerEventTaskProducer).produceTask(Mockito.any());
        clock.setFixed(Instant.parse("2022-03-05T13:45:00Z"), ZoneId.systemDefault());
    }

    @Test
    void getPartnerById() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/externalApi/partners/3"))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/partner/get_partner_response.json"));
    }

    @Test
    void getPartnerByIdNotFoundError() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/externalApi/partners/10"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void searchPartners() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .put("/externalApi/partners/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/partner/partners_response.json"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    void searchPartnersWithPaging(
        @SuppressWarnings("unused") String displayName,
        Pageable pageable,
        String resultPath
    ) throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .put("/externalApi/partners/search-paged")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .param("size", String.valueOf(pageable.getPageSize()))
                .param("page", String.valueOf(pageable.getPageNumber()))
        )
            .andExpect(status().isOk())
            .andExpect(testJson(resultPath));
    }

    @Nonnull
    private static Stream<Arguments> searchPartnersWithPaging() {
        return Stream.of(
            Arguments.of(
                "Первая страница",
                PageRequest.of(0, 2),
                "data/controller/partner/partners_response_first_page.json"
            ),
            Arguments.of(
                "Последняя страница",
                PageRequest.of(3, 2),
                "data/controller/partner/partners_response_last_page.json"
            ),
            Arguments.of(
                "Слишком большой размер страницы",
                PageRequest.of(0, 50),
                "data/controller/partner/partners_response_large_page_size.json"
            ),
            Arguments.of(
                "Слишком большой номер страницы",
                PageRequest.of(10, 10),
                "data/controller/partner/partners_response_large_page_number.json"
            )
        );
    }

    @Test
    void changePartnerStatus() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders
                    .post("/externalApi/partners/3/changeStatus")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(pathToJson("data/controller/partner/createPartner/activate_partner_request.json")))
            .andExpect(status().isOk())
            .andExpect(content().json(
                pathToJson("data/controller/partner/createPartner/activate_partner_response.json")));
    }

    @Test
    void changePartnerStatusNotFound() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/externalApi/partners/10/changeStatus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/partner/createPartner/activate_partner_request.json")))
            .andExpect(status().is(404));
    }

    @Test
    void updatePartner() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders
                    .put("/externalApi/partners/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(pathToJson("data/controller/partner/update_partner_request.json")))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/partner/update_partner_response.json"));
        checkBuildWarehouseSegmentTask(2L);
    }

    @Test
    void updatePartnerWithoutBillingClientId() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders
                    .put("/externalApi/partners/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(pathToJson("data/controller/partner/update_partner_request_without_billing_id.json")))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/partner/update_partner_response_without_billing_id.json"));
        checkBuildWarehouseSegmentTask(2L);
    }

    @Test
    void updatePartnerWithoutRequiredFields() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .put("/externalApi/partners/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.errors[*].field", Matchers.containsInAnyOrder(
                    "name",
                    "readableName",
                    "status"
                ))
            );
    }

    @Test
    void getPartnerIntakeSchedule() throws Exception {
        LocalDate localDate = LocalDate.of(2018, 1, 1);

        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/externalApi/partners/1/intakeSchedule")
                .param("date", localDate.toString())
        )
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/partner/partner_intake_schedule.json", false));
    }

    @Test
    void getPartnerIntakeScheduleEmpty() throws Exception {
        LocalDate localDate = LocalDate.of(2018, 1, 1);

        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/externalApi/partners/2/intakeSchedule")
                .param("date", localDate.toString())
        )
            .andExpect(status().isOk())
            .andExpect(content().json(emptyJsonList()));
    }

    @Test
    void getPartnerCapacities() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/externalApi/partners/4/capacity"))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/partner/partner_capacity_response.json"));
    }

    @Test
    void getPartnerCapacitiesEmpty() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/externalApi/partners/3/capacity"))
            .andExpect(status().isOk())
            .andExpect(content().json(emptyJsonList()));
    }

    @Test
    void getPartnerIntakeScheduleDay() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/externalApi/schedule/11"))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/partner/partner_intake_schedule_day.json"));
    }

    @Test
    void getPartnerIntakeScheduleDayEmpty() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/externalApi/schedule/42"))
            .andExpect(status().isOk())
            .andExpect(content().string("null"));
    }

    @Test
    void getPartnerCargoTypesTest() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/externalApi/partners/cargoTypes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/partner/partner_cargo_types_request.json"))
        )
            .andExpect(status().isOk())
            .andExpect(content().json(pathToJson("data/controller/partner/partner_cargo_types_response.json")));
    }

    @Test
    void getPartnerForbiddenCargoTypesTest() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/externalApi/partners/forbiddenCargoTypes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/partner/partner_cargo_types_request.json"))
        )
            .andExpect(status().isOk())
            .andExpect(content().json(pathToJson(
                "data/controller/partner/partner_forbidden_cargo_types_response.json"
            )));
    }

    @Test
    void getPartnerCargoTypesEmptyPartnerIdsListTest() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/externalApi/partners/cargoTypes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"partnerMarketIds\":[]"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdatePartnerSettingsWithoutReason() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .put("/externalApi/partners/1/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/partner_setting.json"))
        ).andExpect(status().isOk());
        Optional<Partner> optionalPartner = partnerRepository.findById(1L);
        softly.assertThat(optionalPartner).as("Partner should be found").isNotEmpty();

        Partner partner = optionalPartner.get();
        softly.assertThat(partner)
            .extracting(
                Partner::getTrackingType,
                Partner::getLocationId,
                Partner::getStockSyncEnabled,
                Partner::getAutoSwitchStockSyncEnabled,
                Partner::getStockSyncSwitchReason
            ).as("Partner has all fields updated")
            .containsExactly("post_reg", 100500, true, true, StockSyncSwitchReason.UNKNOWN);
    }

    @Test
    void testUpdatePartnerSettingsWithReason() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .put("/externalApi/partners/1/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/partner_setting_with_reason.json"))
        ).andExpect(status().isOk());

        Partner partner = partnerRepository.findByIdOrThrow(1L);
        softly.assertThat(partner)
            .extracting(
                Partner::getTrackingType,
                Partner::getLocationId,
                Partner::getStockSyncEnabled,
                Partner::getAutoSwitchStockSyncEnabled,
                Partner::getStockSyncSwitchReason
            ).as("Partner has all fields updated")
            .containsExactly("post_reg", 100500, true, true, StockSyncSwitchReason.AUTO_CHANGED_AFTER_FAIL);
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/partner/after/update_partner_settings.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdatePartnerSettingsWithExternalParamValues() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .put("/externalApi/partners/1/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/partner/partner_settings_with_external_param_values.json")))
            .andExpect(status().isOk());
    }

    @Test
    void testUpdatePartnerSettingsPartnerNotExist() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .put("/externalApi/partners/100500/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/partner_setting.json"))
        ).andExpect(status().isNotFound());
    }

    @Test
    void addPlatformClientWithoutStatusOk() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/externalApi/partners/1/platform-clients")
                .param("platformClientId", "3902")
        ).andExpect(status().isOk())
            .andExpect(content()
                .json(pathToJson("data/controller/partner/add_platform_client_no_status.json")));
        checkBuildWarehouseSegmentTask(2L);
    }

    @Test
    void addPlatformClientWithStatusOk() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/externalApi/partners/1/platform-clients")
                .param("platformClientId", "3902")
                .param("status", "ACTIVE")
        ).andExpect(status().isOk())
            .andExpect(content()
                .json(pathToJson("data/controller/partner/add_platform_client_status_active.json")));
        checkBuildWarehouseSegmentTask(2L);
    }

    @Test
    void addPlatformClientWithStatusAndShipmentOk() throws Exception {
        mockMvc.perform(
        MockMvcRequestBuilders
            .post("/externalApi/partners/platform-clients")
            .contentType(MediaType.APPLICATION_JSON)
            .content(pathToJson("data/controller/platform_clients_with_shipment.json"))
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent(
                "data/controller/partner/add_platform_client_status_active_own_delivery.json"
            ));
    }

    @Test
    void addOrUpdatePlatformClientAlreadyExists() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/externalApi/partners/platform-clients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/platform_clients_already_exists.json"))
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent(
                "data/controller/partner/add_platform_client_already_exists.json"
            ));
        checkBuildWarehouseSegmentTask(2L);
    }

    @Test
    void addPlatformClientAlreadyExists() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/externalApi/partners/1/platform-clients")
                .param("platformClientId", "3901")
                .param("status", "ACTIVE")
        ).andExpect(status().isConflict());
    }

    @Test
    void addPlatformClientPartnerNotFound() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/externalApi/partners/1100500/platform-clients")
                .param("platformClientId", "3901")
                .param("status", "ACTIVE")
        ).andExpect(status().isNotFound());
    }

    @Test
    void addPlatformClientPlatformNotExists() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/externalApi/partners/1/platform-clients")
                .param("platformClientId", "100500")
                .param("status", "ACTIVE")
        ).andExpect(status().isNotFound());
    }

    @Test
    void addPlatformClientWrongStatus() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/externalApi/partners/1/platform-clients")
                .param("platformClientId", "3902")
                .param("status", "WRONG")
        ).andExpect(status().isBadRequest());
    }

    @Test
    void getPartnerTypeOptions() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/externalApi/partners/partnerTypeOptions")
        )
            .andExpect(status().isOk())
            .andExpect(content().json(pathToJson("data/controller/partner/partner_type_options.json")));
    }

    @Test
    void getPartnerSubtypeOptions() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/externalApi/partners/partnerSubtypeOptions")
                .param("partnerType", "DELIVERY")
        )
            .andExpect(status().isOk())
            .andExpect(content().json(pathToJson("data/controller/partner/partner_subtype_options.json")));
    }

    @Test
    void testAllUpdateMarketId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/externalApi/partners/updateMarketId"))
            .andExpect(status().isOk())
            .andExpect(content().json(pathToJson("data/controller/partner/update_market_id_all.json"), false));
    }

    @Test
    void testOneUpdateMarketIdNotExists() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/externalApi/partners/100500/updateMarketId"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testOneUpdateMarketId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/externalApi/partners/1/updateMarketId"))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/partner/update_market_id_one.json"));
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/partner/after/partner_country_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Добавление партнёру страны, в которую он доставляет")
    void testCreatePartnerCountry() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/externalApi/partners/2/country/159"))
            .andExpect(status().isOk());
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/partner/after/partner_country_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Повторное добавление партнёру страны, в которую он доставляет, идемпотентно")
    void testCreatePartnerCountryTwice() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/externalApi/partners/2/country/159"))
            .andExpect(status().isOk());

        // Повторное создание идемпотентно.
        mockMvc.perform(MockMvcRequestBuilders.post("/externalApi/partners/2/country/159"))
            .andExpect(status().isOk());
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/partner/after/partner_country_deleted.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Удаление у партнёра страны, в которую он доставляет")
    void testDeletePartnerCountry() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/externalApi/partners/2/country/149"))
            .andExpect(status().isOk());
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/partner/after/partner_country_deleted.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Повторное удаление у партнёра страны, в которую он доставляет, идемпотентно")
    void testDeletePartnerCountryTwice() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/externalApi/partners/2/country/149"))
            .andExpect(status().isOk());

        // Повторное удаление идемпотентно.
        mockMvc.perform(MockMvcRequestBuilders.delete("/externalApi/partners/2/country/149"))
            .andExpect(status().isOk());
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/partner/after/partner_forbidden_cargo_types_added.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Добавление карготипов в чёрный список партнёра")
    void testAddPartnerForbiddenCargoTypes() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/externalApi/partners/3/forbiddenCargoTypes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/partner/add_partner_forbidden_cargo_types_request.json"))
        )
            .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(
        value = "/data/controller/partner/after/partner_forbidden_cargo_types_added.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/controller/partner/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Удаление карготипов из чёрного списка партнёра")
    void testRemovePartnerForbiddenCargoTypes() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/externalApi/partners/3/removeForbiddenCargoTypes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/partner/remove_partner_forbidden_cargo_types_request.json"))
        )
            .andExpect(status().isOk());
    }
}
