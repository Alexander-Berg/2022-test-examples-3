package ru.yandex.market.logistics.management.controller;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.entity.response.settings.SettingsApiDto;
import ru.yandex.market.logistics.management.util.CleanDatabase;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_SETTINGS;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_SETTINGS_EDIT;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_SETTINGS_TOKEN_VIEWER;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.SLUG_METHODS_API;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.SLUG_SETTINGS_API;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;

@SuppressWarnings({"checkstyle:MagicNumber"})
@CleanDatabase
@DatabaseSetup(
    value = "/data/controller/settings/before/setting_controller_prepare_data.xml",
    connection = "dbUnitQualifiedDatabaseConnection"
)
class ApiSettingControllerTest extends AbstractContextualTest {

    @Test
    void getPartnerSettings() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/partners/1/settings")
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/settings/partner_settings_api.json",
                Option.IGNORING_ARRAY_ORDER,
                Option.IGNORING_EXTRA_FIELDS
            ));
    }

    @Test
    void getPartnerSettingsNotFound() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/partners/4/settings")
        )
            .andExpect(status().is4xxClientError());
    }

    @Test
    @DatabaseSetup(
        value = "/data/controller/settings/before/setting_controller_prepare_data_api_type_addon.xml",
        connection = "dbUnitQualifiedDatabaseConnection",
        type = DatabaseOperation.INSERT
    )
    void getPartnerSettingsWithApiType() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/partners/4/settings")
                .param("apiType", "DELIVERY")
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/settings/partner_settings_api_with_type.json",
                Option.IGNORING_ARRAY_ORDER,
                Option.IGNORING_EXTRA_FIELDS
            ));
    }

    @Test
    @DatabaseSetup(
        value = "/data/controller/settings/before/setting_controller_prepare_data_api_type_addon.xml",
        connection = "dbUnitQualifiedDatabaseConnection",
        type = DatabaseOperation.INSERT
    )
    void getPartnerSettingsWithoutSpecifyingApiType() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/partners/4/settings")
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/settings/partner_settings_api_without_specifying_type.json",
                Option.IGNORING_ARRAY_ORDER,
                Option.IGNORING_EXTRA_FIELDS
            ));
    }

    @Test
    @DatabaseSetup(
        value = "/data/controller/settings/before/setting_controller_prepare_data_api_type_addon.xml",
        connection = "dbUnitQualifiedDatabaseConnection",
        type = DatabaseOperation.INSERT
    )
    void getPartnerSettingsWithApiTypeNotFound() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/partners/3/settings")
                .param("apiType", "FULFILLMENT")
        )
            .andExpect(status().is4xxClientError());
    }

    @Test
    void searchAllApiSettings() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .put("/partners/settings/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/settings/search_settings_apis_all_response.json",
                Option.IGNORING_ARRAY_ORDER,
                Option.IGNORING_EXTRA_FIELDS
            ));
    }

    @Test
    void searchApiSettingsByPartnerIds() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .put("/partners/settings/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.pathToJson(
                    "data/controller/settings/search_settings_apis_by_partner_ids_filter.json"
                ))
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/settings/search_settings_apis_by_partner_ids_response.json",
                Option.IGNORING_ARRAY_ORDER,
                Option.IGNORING_EXTRA_FIELDS
            ));
    }

    @Test
    @DatabaseSetup(
        value = "/data/controller/settings/before/setting_controller_prepare_data_api_type_addon.xml",
        connection = "dbUnitQualifiedDatabaseConnection",
        type = DatabaseOperation.INSERT
    )
    void searchApiSettingsByApiType() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .put("/partners/settings/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"apiType\": \"DELIVERY\"}")
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/settings/search_settings_apis_by_api_type_response.json",
                Option.IGNORING_ARRAY_ORDER,
                Option.IGNORING_EXTRA_FIELDS
            ));
    }

    @Test
    void getPartnerSettingsMethods() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/partners/2/settings/methods")
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson("data/controller/settings/partner_methods.json"));
    }

    @Test
    void searchAllSettingsMethods() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .put("/partners/settings/methods/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/settings/search_settings_methods_all_response.json",
                Option.IGNORING_ARRAY_ORDER,
                Option.IGNORING_EXTRA_FIELDS
            ));
    }

    @Test
    void searchSettingsMethodsByPartnerIds() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .put("/partners/settings/methods/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.pathToJson(
                    "data/controller/settings/search_settings_methods_by_partner_ids_filter.json"
                ))
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/settings/search_settings_methods_by_partner_ids_response.json"
            ));
    }

    @Test
    void searchSettingsMethodsByTypes() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .put("/partners/settings/methods/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.pathToJson(
                    "data/controller/settings/search_settings_methods_by_types_filter.json"
                ))
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson("data/controller/settings/search_settings_methods_by_types_response.json"));
    }

    @Test
    @DatabaseSetup(
        value = "/data/controller/settings/before/setting_controller_prepare_data_api_type_addon.xml",
        connection = "dbUnitQualifiedDatabaseConnection",
        type = DatabaseOperation.INSERT
    )
    void searchSettingsMethodsByApiType() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .put("/partners/settings/methods/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"apiType\": \"DELIVERY\"}")
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/settings/partner_methods_with_typed_settings_api.json",
                Option.IGNORING_ARRAY_ORDER
            ));
    }

    @Test
    void getPartnersBySettingsApiToken() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/partners")
                .param("token", "token")
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson("data/controller/settings/partners_with_same_token.json"));
    }

    @Test
    void getPartnerSettingsByMethodName() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/partners/3/settings/methods/updateOrder")
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/settings/partner_settings_create_order.json",
                Option.IGNORING_ARRAY_ORDER,
                Option.IGNORING_EXTRA_FIELDS
            ));
    }

    @Test
    @DatabaseSetup(
        value = "/data/controller/settings/before/setting_controller_prepare_data_api_type_addon.xml",
        connection = "dbUnitQualifiedDatabaseConnection",
        type = DatabaseOperation.INSERT
    )
    void getPartnerSettingsByMethodNameAndApiType() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/partners/4/settings/methods/updateOrder")
                .param("apiType", "DELIVERY")
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/settings/partner_settings_api_with_type.json",
                Option.IGNORING_ARRAY_ORDER,
                Option.IGNORING_EXTRA_FIELDS
            ));
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/settings/after/update_settings.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void updatePartnerSettings() throws Exception {
        mockMvc.perform(
            put("/partners/2/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.pathToJson("data/controller/settings/update_partner_settings.json"))
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/update_settings_successful.json",
                Option.IGNORING_ARRAY_ORDER,
                Option.IGNORING_EXTRA_FIELDS
        ));
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/settings/after/update_settings.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void updatePartnerSettingsByMethodName() throws Exception {
        mockMvc.perform(
            put("/partners/2/settings/methods/createOrder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.pathToJson("data/controller/settings/update_partner_settings.json"))
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/update_settings_by_method_name_successful.json",
                Option.IGNORING_ARRAY_ORDER,
                Option.IGNORING_EXTRA_FIELDS
            ));
    }

    @Test
    @DatabaseSetup(
        value = "/data/controller/settings/before/setting_controller_prepare_data_api_type_addon.xml",
        connection = "dbUnitQualifiedDatabaseConnection",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/settings/after/added_method_for_partner.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createPartnerMethod() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/partners/4/settings/methods")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.pathToJson("data/controller/settings/create_partner_method.json"))
        )
            .andExpect(status().isCreated())
            .andExpect(TestUtil.testJson("data/controller/settings/create_method_successful.json"));
    }

    @Test
    @DatabaseSetup(
        value = "/data/controller/settings/before/setting_controller_prepare_data_api_type_addon.xml",
        connection = "dbUnitQualifiedDatabaseConnection",
        type = DatabaseOperation.INSERT
    )
    void createPartnerMethodNoApiType() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/partners/4/settings/methods")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.pathToJson("data/controller/settings/create_partner_method_no_api_type.json"))
        )
            .andExpect(status().isBadRequest());
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/settings/after/added_methods_for_partner.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createPartnerMethods() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/partners/3/settings/methods/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.pathToJson("data/controller/create_partner_methods.json"))
        )
            .andExpect(status().isCreated())
            .andExpect(TestUtil.testJson(
                "data/controller/create_partner_methods_successful.json",
                Option.IGNORING_ARRAY_ORDER,
                Option.IGNORING_EXTRA_FIELDS
            ));
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/settings/before/setting_controller_prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void createPartnerMethodNotValid() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/partners/2/settings/methods")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    TestUtil.pathToJson("data/controller/settings/create_partner_method_not_valid.json"))
        )
            .andExpect(status().isBadRequest());
    }

    @Test
    @ExpectedDatabase(
            value = "/data/controller/settings/before/setting_controller_prepare_data.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            connection = "dbUnitQualifiedDatabaseConnection"
    )
    void createPartnerMethodNotValidCron() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/partners/2/settings/methods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                TestUtil.pathToJson("data/controller/settings/create_partner_method_invalid_cron.json"))
        )
                .andExpect(status().isBadRequest());
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/settings/before/setting_controller_prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void createPartnerMethodsNotValidMethod() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/partners/2/settings/methods/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    TestUtil.pathToJson("data/controller/create_partner_methods_not_valid_method.json")
                )
        )
            .andExpect(status().isBadRequest())
            .andExpect(TestUtil.testJson(
                "data/controller/validation_methods_failure_method.json",
                Option.IGNORING_EXTRA_FIELDS
            ));
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/settings/before/setting_controller_prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void createPartnerMethodsNotValidUrl() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/partners/2/settings/methods/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    TestUtil.pathToJson("data/controller/create_partner_methods_not_valid_url.json")
                )
        )
            .andExpect(status().isBadRequest())
            .andExpect(TestUtil.testJson(
                "data/controller/validation_methods_failure_url.json",
                Option.IGNORING_EXTRA_FIELDS
            ));
    }

    @Test
    void createNewApiSettingsWithoutApiType() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/partners/4/settings/api")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.pathToJson("data/controller/create_api_settings_without_api_type.json"))
        )
            .andExpect(status().isBadRequest());
    }

    @Test
    void createNewApiSettingsWithApiType() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/partners/4/settings/api")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.pathToJson("data/controller/create_api_settings_with_token.json"))
        )
            .andExpect(status().isCreated())
            .andExpect(TestUtil.testJson(
                "data/controller/create_settings_api_successful.json",
                Option.IGNORING_ARRAY_ORDER,
                Option.IGNORING_EXTRA_FIELDS
        ));
    }

    @Test
    void createApiSettingsWithToken() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/partners/3/settings/api")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.pathToJson("data/controller/create_api_settings_with_token.json"))
        )
            .andExpect(status().isCreated())
            .andExpect(TestUtil.testJson(
                "data/controller/update_settings_method_successful.json",
                Option.IGNORING_ARRAY_ORDER,
                Option.IGNORING_EXTRA_FIELDS
            ));
    }

    @Test
    void createApiSettingsWithoutToken() throws Exception {
        String contentAsString = mockMvc.perform(
            post("/partners/1/settings/api")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    pathToJson("data/controller/settings/create_api_settings_without_token.json"))
        )
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        SettingsApiDto settingsApiDto = TestUtil.getObjectMapper().readValue(contentAsString, SettingsApiDto.class);
        softly.assertThat(settingsApiDto.getToken())
            .as("Token not generated").isNotNull()
            .as("Token doesn't have proper length").hasSize(64);
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/settings/before/setting_controller_prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void createApiSettingsPartnerNotFound() throws Exception {
        mockMvc.perform(
            post("/partners/100500/settings/api")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    pathToJson("data/controller/settings/create_api_settings_without_token.json"))
        )
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Can't find Partner with id=100500"));
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/settings/before/setting_controller_prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void createApiSettingsWrongFormat() throws Exception {
        mockMvc.perform(
            post("/partners/1/settings/api")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    pathToJson("data/controller/settings/create_api_settings_wrong_format.json"))
        )
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Cannot validate api setting format: TXT"));
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/settings/before/setting_controller_prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void createApiSettingsNotValidDto() throws Exception {
        mockMvc.perform(
            post("/partners/1/settings/api")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    pathToJson("data/controller/settings/create_api_settings_validation_fail.json"))
        )
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {
        AUTHORITY_ROLE_SETTINGS
    })
    void partnerSettingsGridIsOk() throws Exception {
        getPartnerSettingsApiGrid()
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/settings/settings_api_grid.json",
                false
            ));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_SETTINGS_TOKEN_VIEWER)
    void settingsTokenIsOk() throws Exception {
        getSettingsApiToken(1)
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson("data/controller/settings/token.json"));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_SETTINGS)
    void settingsTokenIsNotOk() throws Exception {
        getSettingsApiToken(1).andExpect(status().isForbidden());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {
        AUTHORITY_ROLE_SETTINGS
    })
    void newPartnerSettingsForbiddenWithoutEdit() throws Exception {
        getPartnerSettingsApiNew()
            .andExpect(status().isForbidden());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {
        AUTHORITY_ROLE_SETTINGS,
        AUTHORITY_ROLE_SETTINGS_EDIT
    })
    void newPartnerSettingsIsOk() throws Exception {
        getPartnerSettingsApiNew()
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/settings/settings_api_new.json",
                false
            ));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_SETTINGS_EDIT)
    void detailCreateFailsWithoutApiType() throws Exception {
        createPartnerSettingsApi("partner_settings_api")
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_SETTINGS_EDIT)
    void detailCreate() throws Exception {
        createPartnerSettingsApi("partner_settings_api_with_type")
            .andExpect(status().isCreated())
            .andExpect(header().string("location", "http://localhost/admin/lms/settings-api/4"));
    }

    @Test
    @DatabaseSetup(
        value = "/data/controller/settings/before/setting_controller_prepare_data_api_type_addon.xml",
        connection = "dbUnitQualifiedDatabaseConnection",
        type = DatabaseOperation.INSERT
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_SETTINGS_EDIT)
    @DisplayName("Обновляет объект совпадающий по partnerId и apiType")
    void detailCreateUpdatesMatchingEntity() throws Exception {
        createPartnerSettingsApi("partner_settings_api_with_type")
            .andExpect(status().isCreated())
            .andExpect(header().string("location", "http://localhost/admin/lms/settings-api/5"));
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/settings/after/update_settings.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_SETTINGS_EDIT)
    void detailUpdateExistingSettings() throws Exception {
        updatePartnerSettingsApi("update_partner_settings_by_id", 2)
            .andExpect(status().isOk());
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/settings/after/update_settings_with_empty_token.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_SETTINGS_EDIT)
    void detailUpdateWithEmptyToken() throws Exception {
        updatePartnerSettingsApi("update_partner_settings_with_empty_token", 2)
            .andExpect(status().isOk());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_SETTINGS_EDIT)
    void detailCreateForNotExistsPartner() throws Exception {
        createPartnerSettingsApi("detail_partner_settings_api")
            .andExpect(status().isNotFound());
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/settings/before/setting_controller_prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_SETTINGS_EDIT)
    void detailCreateForNotValidFormat() throws Exception {
        createPartnerSettingsApi("partner_settings_api_not_valid_format")
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {
        AUTHORITY_ROLE_SETTINGS
    })
    void partnerMethodsGridIsOk() throws Exception {
        getPartnerMethodsApiGrid()
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/settings/methods_api_grid.json",
                false
            ));
    }

    @Test
    @DatabaseSetup(
        value = "/data/controller/settings/before/setting_controller_prepare_data_api_type_addon.xml",
        connection = "dbUnitQualifiedDatabaseConnection",
        type = DatabaseOperation.INSERT
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_SETTINGS_EDIT)
    void adminCreateMethod() throws Exception {
        createPartnerMethodsApi("admin_create_partner_method")
            .andExpect(status().isCreated());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_SETTINGS_EDIT)
    void adminCreateMethodInvalidCron() throws Exception {
        createPartnerMethodsApi("admin_create_partner_method_invalid_cron")
            .andExpect(status().isBadRequest());
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/settings/after/updated_method.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_SETTINGS_EDIT)
    void detailUpdateExistingMethod() throws Exception {
        updatePartnerMethodsApi("update_partner_method_by_id", 3)
            .andExpect(status().isOk());
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/settings/before/setting_controller_prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_SETTINGS_EDIT)
    void updateExistingMethodWithInvalidCron() throws Exception {
        updatePartnerMethodsApi("update_partner_method_by_id_invalid_cron", 3)
            .andExpect(status().isBadRequest());
    }

    private ResultActions getPartnerSettingsApiGrid() throws Exception {
        return mockMvc.perform(
            get("/admin/lms/settings-api")
        );
    }

    private ResultActions getSettingsApiToken(int id) throws Exception {
        return mockMvc.perform(
            get("/admin/lms/settings-api/" + id + "/token")
        );
    }

    private ResultActions getPartnerMethodsApiGrid() throws Exception {
        return mockMvc.perform(
            get("/admin/lms/methods-api")
        );
    }

    private ResultActions getPartnerSettingsApiNew() throws Exception {
        return mockMvc.perform(
            get("/admin/lms/settings-api/new")
        );
    }

    private ResultActions updatePartnerSettingsApi(String file, int id) throws Exception {
        return executePut(SLUG_SETTINGS_API, id, file);
    }

    private ResultActions createPartnerMethodsApi(String file) throws Exception {
        return executePost(SLUG_METHODS_API, file);
    }

    private ResultActions updatePartnerMethodsApi(String file, int id) throws Exception {
        return executePut(SLUG_METHODS_API, id, file);
    }

    private ResultActions createPartnerSettingsApi(String file) throws Exception {
        return mockMvc.perform(
            post("/admin/lms/settings-api")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/settings/" + file + ".json"))
        );
    }

    private ResultActions executePost(String slug, String file) throws Exception {
        return mockMvc.perform(
            post("/admin/lms/{slug}", slug)
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/settings/" + file + ".json"))
        );
    }

    private ResultActions executePut(String slug, long id, String file) throws Exception {
        return mockMvc.perform(
            put("/admin/lms/{slug}/{id}", slug, id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/settings/" + file + ".json"))
        );
    }
}
