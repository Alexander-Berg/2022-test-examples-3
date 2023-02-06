package ru.yandex.market.logistics.lms.client.controller.redis;

import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.util.NestedServletException;

import ru.yandex.market.logistics.lom.service.redis.AbstractRedisTest;
import ru.yandex.market.logistics.lom.service.redis.util.RedisKeys;
import ru.yandex.market.logistics.lom.service.yt.dto.YtPartnerApiSettings;
import ru.yandex.market.logistics.management.entity.request.settings.SettingsMethodFilter;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.lms.client.controller.redis.LmsLomRedisControllerPartnerTest.REDIS_ACTUAL_VERSION;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

@ParametersAreNonnullByDefault
@DisplayName("Ручки для тестирования работы редиса с поиском методов партнеров")
class LmsLomRedisControllerPartnerSettingsTest extends AbstractRedisTest {

    private static final String PARTNER_SETTINGS_TABLE_NAME = RedisKeys.getHashTableFromYtName(
        YtPartnerApiSettings.class,
        REDIS_ACTUAL_VERSION
    );
    private static final long PARTNER_ID = 1L;
    private static final String METHOD = "method";

    private static final String GET_PARTNER_SETTINGS_METHODS_BY_FILTER_PATH =
        "/lms/test-redis/partner-settings-methods/get-by-filter";

    @Test
    @SneakyThrows
    @DisplayName("Получение методов партнеров по фильтру")
    void getPartnerSettingsMethodsByFilter() {
        SettingsMethodFilter filter = buildFilter();
        String settingsMethodsHash = "1:method";
        doReturn(redisObjectConverter.serializeToString(buildPartnerSettingMethodLmsDto()))
            .when(clientJedis).hget(eq(PARTNER_SETTINGS_TABLE_NAME), eq(settingsMethodsHash));

        performSettingsMethodsSearchByFilter(filter)
            .andExpect(status().isOk())
            .andExpect(jsonContent("lms/client/controller/partner_settings_methods_response.json"));

        verify(clientJedis).hget(eq(PARTNER_SETTINGS_TABLE_NAME), eq(settingsMethodsHash));
        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
    }

    @Test
    @SneakyThrows
    @DisplayName("Метод не найден")
    void partnerSettingsMethodsNotFound() {
        SettingsMethodFilter filter = buildFilter();

        performSettingsMethodsSearchByFilter(filter)
            .andExpect(status().isOk())
            .andExpect(content().string("[]"));

        verify(clientJedis).hget(eq(PARTNER_SETTINGS_TABLE_NAME), eq("1:method"));
        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
    }

    @Test
    @SneakyThrows
    @DisplayName("Получение методов партнеров по пустому фильтру")
    void getPartnerSettingsMethodsByEmptyFilter() {
        SettingsMethodFilter filter = SettingsMethodFilter.newBuilder().build();

        softly.assertThatCode(
                () -> performSettingsMethodsSearchByFilter(filter)
            )
            .isInstanceOf(NestedServletException.class)
            .hasMessage("Request processing failed; nested exception is java.lang.IllegalArgumentException: "
                + "Filter must contain exactly one 'partnerId' and one 'methodType'");
    }

    @Nonnull
    @SneakyThrows
    private ResultActions performSettingsMethodsSearchByFilter(SettingsMethodFilter filter) {
        return mockMvc.perform(
            post(GET_PARTNER_SETTINGS_METHODS_BY_FILTER_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filter))
        );
    }

    @Nonnull
    private YtPartnerApiSettings buildPartnerSettingMethodLmsDto() {
        return new YtPartnerApiSettings()
            .setPartnerId(PARTNER_ID)
            .setMethod(METHOD)
            .setActive(true);
    }

    @Nonnull
    private SettingsMethodFilter buildFilter() {
        return SettingsMethodFilter.newBuilder()
            .partnerIds(Set.of(PARTNER_ID))
            .methodTypes(Set.of(METHOD))
            .build();
    }
}
