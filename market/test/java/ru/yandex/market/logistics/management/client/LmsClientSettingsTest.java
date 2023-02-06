package ru.yandex.market.logistics.management.client;

import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.management.entity.request.settings.SettingsApiFilter;
import ru.yandex.market.logistics.management.entity.request.settings.SettingsMethodFilter;
import ru.yandex.market.logistics.management.entity.response.settings.SettingsApiDto;
import ru.yandex.market.logistics.management.entity.response.settings.SettingsApiUpdateDto;
import ru.yandex.market.logistics.management.entity.response.settings.methods.SettingsMethodCreateDto;
import ru.yandex.market.logistics.management.entity.response.settings.methods.SettingsMethodDto;
import ru.yandex.market.logistics.management.entity.type.ApiType;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonResource;

@SuppressWarnings("unchecked")
class LmsClientSettingsTest extends AbstractClientTest {

    @Test
    void getPartnerApiSettings() {
        mockServer.expect(requestTo(uri + "/partners/1/settings"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/clientSettings/partner_settings_api.json"))
            );

        softly.assertThat(client.getPartnerApiSettings(1L))
            .as("Settings have correct field values")
            .extracting(
                SettingsApiDto::getId,
                SettingsApiDto::getPartnerId,
                SettingsApiDto::getToken,
                SettingsApiDto::getFormat,
                SettingsApiDto::getVersion
            )
            .containsExactly(1L, 1L, "token", "JSON", "1.0");
    }

    @Test
    void getPartnerApiSettingsByMethodName() {
        mockServer.expect(requestTo(uri + "/partners/1/settings/methods/createOrder"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/clientSettings/partner_settings_api.json"))
            );

        softly.assertThat(client.getPartnerApiSettings(1L, "createOrder"))
            .as("There are settings for partner with this method name")
            .isNotNull()
            .as("Settings fields are correct")
            .extracting(
                SettingsApiDto::getId,
                SettingsApiDto::getPartnerId,
                SettingsApiDto::getToken,
                SettingsApiDto::getFormat,
                SettingsApiDto::getVersion
            )
            .containsExactly(1L, 1L, "token", "JSON", "1.0");
    }

    @Test
    void searchPartnerApiSettings() {
        mockServer.expect(requestTo(uri + "/partners/settings/search"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json("{}"))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/clientSettings/search_settings_apis_all_response.json"))
            );

        List<SettingsApiDto> settingsApis = client.searchPartnerApiSettings(SettingsApiFilter.newBuilder().build());

        softly.assertThat(settingsApis)
            .containsExactlyInAnyOrder(
                SettingsApiDto.newBuilder()
                    .id(1L)
                    .partnerId(1L)
                    .token("token")
                    .format("JSON")
                    .version("1.0")
                    .build(),
                SettingsApiDto.newBuilder()
                    .id(2L)
                    .partnerId(2L)
                    .token("token2")
                    .format("JSON")
                    .version("1.0")
                    .build()
            );
    }

    @Test
    void createApiSettings() {
        mockServer.expect(requestTo(uri + "/partners/1/settings/api"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json(jsonResource("data/controller/create_api_settings_with_token.json")))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/clientSettings/update_settings_successful.json")));

        SettingsApiDto apiSettings = client.createApiSettings(1L, SettingsApiUpdateDto.newBuilder()
            .apiType(ApiType.DELIVERY)
            .token("new_token")
            .version("3.0")
            .format("JSON")
            .build());

        softly.assertThat(apiSettings).as("Should properly parse response")
            .isEqualTo(getSettingApiResponse());
    }

    @Test
    void updatePartnerApiSettings() {
        mockServer.expect(requestTo(uri + "/partners/1/settings"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json(jsonResource("data/controller/update_partner_settings.json")))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/clientSettings/update_settings_successful.json"))
            );

        softly.assertThat(client.updatePartnerApiSettings(
            1L,
            SettingsApiUpdateDto.newBuilder()
                .apiType(ApiType.DELIVERY)
                .token("new_token")
                .format("JSON")
                .version("3.0")
                .build()
        ))
            .as("Settings updated")
            .isNotNull()
            .as("Settings fields are correct after update")
            .isEqualTo(getSettingApiResponse());
    }

    @Test
    void updatePartnerApiSettingsByMethodName() {
        mockServer.expect(requestTo(uri + "/partners/1/settings/methods/createOrder"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json(jsonResource("data/controller/update_partner_settings_method.json")))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/clientSettings/update_settings_method_successful.json"))
            );

        softly.assertThat(
            client.updatePartnerApiSettings(
                1L,
                "createOrder",
                SettingsApiUpdateDto.newBuilder()
                    .token("new_token_by_method")
                    .format("JSON")
                    .version("3.0")
                    .build()
            ))
            .as("Settings updated")
            .isNotNull()
            .as("Settings fields are correct after update")
            .extracting(
                SettingsApiDto::getId,
                SettingsApiDto::getPartnerId,
                SettingsApiDto::getToken,
                SettingsApiDto::getFormat,
                SettingsApiDto::getVersion
            )
            .containsExactly(1L, 1L, "new_token_by_method", "JSON", "3.0");
    }

    @Test
    void getPartnerApiSettingsMethods() {
        mockServer.expect(requestTo(uri + "/partners/1/settings/methods"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/partner_settings_methods.json"))
            );

        softly.assertThat(client.getPartnerApiSettingsMethods(1L))
            .as("There are 2 methods for this partner")
            .hasSize(2)
            .as("Two methods have correct field values")
            .containsExactlyInAnyOrder(
                SettingsMethodDto.newBuilder()
                    .id(1L)
                    .settingsApiId(1L)
                    .method("createOrder")
                    .active(true)
                    .url("testurl")
                    .build(),
                SettingsMethodDto.newBuilder()
                    .id(2L)
                    .settingsApiId(1L)
                    .method("updateOrder")
                    .active(true)
                    .url("testurl2")
                    .build()
            );
    }

    @Test
    void searchPartnerApiSettingsMethods() {
        mockServer.expect(requestTo(uri + "/partners/settings/methods/search"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json("{}"))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/clientSettings/search_settings_methods_all_response.json"))
            );

        List<SettingsMethodDto> settingsMethods = client.searchPartnerApiSettingsMethods(
            SettingsMethodFilter.newBuilder().build()
        );

        softly.assertThat(settingsMethods)
            .containsExactlyInAnyOrder(
                SettingsMethodDto.newBuilder()
                    .id(1L)
                    .settingsApiId(1L)
                    .partnerId(1L)
                    .method("createOrder")
                    .active(true)
                    .url("testurl")
                    .build(),
                SettingsMethodDto.newBuilder()
                    .id(2L)
                    .settingsApiId(1L)
                    .partnerId(1L)
                    .method("updateOrder")
                    .active(true)
                    .url("testurl2")
                    .build(),
                SettingsMethodDto.newBuilder()
                    .id(3L)
                    .settingsApiId(2L)
                    .partnerId(2L)
                    .method("updateOrder")
                    .active(true)
                    .url("testurl3")
                    .build()
            );
    }

    @Test
    void createPartnerApiMethod() {
        mockServer.expect(requestTo(uri + "/partners/2/settings/methods"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json(jsonResource("data/controller/create_partner_method.json")))
            .andRespond(withStatus(CREATED)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/create_partner_method_successful.json"))
            );

        softly.assertThat(client.createPartnerApiMethod(
            2L,
            SettingsMethodCreateDto.newBuilder()
                .method("createOrder")
                .active(true)
                .url("testurl")
                .build()
        ))
            .as("Method was created")
            .isNotNull()
            .as("Method fields are correct")
            .extracting(
                SettingsMethodDto::getId,
                SettingsMethodDto::getSettingsApiId,
                SettingsMethodDto::getMethod,
                SettingsMethodDto::getActive,
                SettingsMethodDto::getUrl,
                SettingsMethodDto::getCronExpression,
                SettingsMethodDto::getEntityPollingFrequencyInSecs,
                SettingsMethodDto::getBatchSize
            )
            .containsExactly(1L, 2L, "createOrder", true, "testurl", null, null, null);
    }

    @Test
    void createPartnerApiMethods() {
        mockServer.expect(requestTo(uri + "/partners/2/settings/methods/list"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json(jsonResource("data/controller/create_partner_methods.json")))
            .andRespond(withStatus(CREATED)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/clientSettings/create_partner_methods_successful.json"))
            );

        List<SettingsMethodCreateDto> settingsMethodCreateDtos = ImmutableList.of(
            SettingsMethodCreateDto.newBuilder()
                .apiType(ApiType.DELIVERY)
                .method("createOrder")
                .active(true)
                .url("testurl")
                .cronExpression(null)
                .entityPollingFrequencyInSecs(null)
                .batchSize(null)
                .build(),
            SettingsMethodCreateDto.newBuilder()
                .apiType(ApiType.DELIVERY)
                .method("getOrder")
                .active(true)
                .url("testurl2")
                .cronExpression(null)
                .entityPollingFrequencyInSecs(null)
                .batchSize(null)
                .build()
        );

        List<SettingsMethodDto> partnerApiMethods = client.createPartnerApiMethods(
            2L,
            settingsMethodCreateDtos
        );

        softly.assertThat(partnerApiMethods).usingRecursiveFieldByFieldElementComparator().isEqualTo(
            ImmutableList.of(
                SettingsMethodDto.newBuilder()
                    .id(1L)
                    .partnerId(2L)
                    .settingsApiId(2L)
                    .method("createOrder")
                    .active(true)
                    .url("testurl")
                    .build(),
                SettingsMethodDto.newBuilder()
                    .id(2L)
                    .partnerId(2L)
                    .settingsApiId(2L)
                    .method("getOrder")
                    .active(true)
                    .url("testurl2")
                    .build()
            )
        );
    }

    private SettingsApiDto getSettingApiResponse() {
        return SettingsApiDto.newBuilder()
            .id(1L)
            .partnerId(1L)
            .token("new_token")
            .format("JSON")
            .version("3.0")
            .build();
    }
}
