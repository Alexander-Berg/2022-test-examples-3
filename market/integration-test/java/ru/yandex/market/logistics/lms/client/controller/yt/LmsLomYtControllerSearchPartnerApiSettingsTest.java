package ru.yandex.market.logistics.lms.client.controller.yt;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.util.NestedServletException;

import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.market.logistics.lom.configuration.properties.LmsYtProperties;
import ru.yandex.market.logistics.lom.utils.YtLmsVersionsUtils;
import ru.yandex.market.logistics.lom.utils.YtUtils;
import ru.yandex.market.logistics.management.entity.request.settings.SettingsMethodFilter;
import ru.yandex.market.logistics.management.entity.response.settings.methods.SettingsMethodDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
@DisplayName("Получение API методов партнера по фильтру")
class LmsLomYtControllerSearchPartnerApiSettingsTest extends LmsLomYtControllerAbstractTest {

    private static final String SEARCH_PARTNER_API_SETTINGS_PATH =
        "/lms/test-yt/partner-settings-methods/get-by-filter";
    private static final String GET_PARTNER_API_SETTINGS_QUERY =
        "* FROM [//home/2022-03-02T08:05:24Z/partner_api_settings_dyn] " +
            "WHERE partner_id = 1 AND method = 'methodName'";
    private static final SettingsMethodFilter FILTER =
        SettingsMethodFilter.newBuilder().partnerIds(Set.of(1L)).methodTypes(Set.of("methodName")).build();

    @Autowired
    private LmsYtProperties lmsYtProperties;

    @Autowired
    private YtTables ytTables;

    @Test
    @SneakyThrows
    @DisplayName("Успешное получение методов партнёра")
    void successGetPartnerApiSettings() {
        mockYtPartnerApiSettingsQueryResponse();

        getPartnerApiSettings(FILTER)
            .andExpect(status().isOk())
            .andExpect(jsonContent("lms/client/controller/yt/partner_api_settings.json"));

        verifyYtCalling();
    }

    @Test
    @SneakyThrows
    @DisplayName("Методы не найдены")
    void methodsNotFound() {
        getPartnerApiSettings(FILTER)
            .andExpect(status().isOk())
            .andExpect(noContent());

        verifyYtCalling();
    }

    @Test
    @SneakyThrows
    @DisplayName("Ошибка в yt")
    void ytErrorWhileGettingMethods() {
        YtUtils.mockExceptionCallingYt(
            ytTables,
            GET_PARTNER_API_SETTINGS_QUERY,
            new RuntimeException("Some yt exception")
        );

        softly.assertThatCode(
                () -> getPartnerApiSettings(FILTER)
            )
            .isInstanceOf(NestedServletException.class)
            .hasMessage("Request processing failed; nested exception is java.lang.RuntimeException: Some yt exception");

        verifyYtCalling();
    }

    private void verifyYtCalling() {
        YtLmsVersionsUtils.verifyYtVersionTableInteractions(ytTables, lmsYtProperties);
        YtUtils.verifySelectRowsInteractions(
            ytTables,
            GET_PARTNER_API_SETTINGS_QUERY
        );
    }

    @Nonnull
    @SneakyThrows
    private ResultActions getPartnerApiSettings(SettingsMethodFilter filter) {
        return mockMvc.perform(
            post(SEARCH_PARTNER_API_SETTINGS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filter))
        );
    }

    private void mockYtPartnerApiSettingsQueryResponse() {
        YtUtils.mockSelectRowsFromYt(
            ytTables,
            List.of(partnerApiSettings()),
            GET_PARTNER_API_SETTINGS_QUERY
        );
    }

    @Nonnull
    private SettingsMethodDto partnerApiSettings() {
        return SettingsMethodDto.newBuilder()
            .partnerId(1L)
            .method("methodName")
            .active(true)
            .build();
    }
}
