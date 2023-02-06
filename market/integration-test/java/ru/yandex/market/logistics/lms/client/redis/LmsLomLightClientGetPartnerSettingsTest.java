package ru.yandex.market.logistics.lms.client.redis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lms.client.utils.LmsLomClientLogUtils;
import ru.yandex.market.logistics.lms.client.utils.LmsLomClientsChainTestCases;
import ru.yandex.market.logistics.lom.entity.InternalVariable;
import ru.yandex.market.logistics.lom.entity.enums.InternalVariableType;
import ru.yandex.market.logistics.lom.lms.model.logging.enums.LmsLomLoggingCode;
import ru.yandex.market.logistics.lom.repository.InternalVariableRepository;
import ru.yandex.market.logistics.lom.service.redis.util.RedisKeys;
import ru.yandex.market.logistics.lom.service.yt.dto.YtPartnerApiSettings;
import ru.yandex.market.logistics.lom.utils.YtLmsVersionsUtils;
import ru.yandex.market.logistics.lom.utils.YtUtils;
import ru.yandex.market.logistics.management.entity.request.settings.SettingsMethodFilter;
import ru.yandex.market.logistics.management.entity.response.settings.methods.SettingsMethodDto;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ParametersAreNonnullByDefault
@DisplayName("Получение методов партнеров")
@DatabaseSetup("/lms/client/redis/get_partner_settings_methods_enabled.xml")
class LmsLomLightClientGetPartnerSettingsTest extends LmsLomLightClientAbstractTest {

    private static final String GET_PARTNER_API_SETTINGS_QUERY =
        "* FROM [//home/2022-03-02T08:05:24Z/partner_api_settings_dyn] " +
            "WHERE partner_id = 1 AND method = 'method'";

    private static final String PARTNER_SETTINGS_TABLE_NAME = RedisKeys.getHashTableFromYtName(
        YtPartnerApiSettings.class,
        REDIS_ACTUAL_VERSION
    );
    private static final long PARTNER_ID = 1L;
    private static final String METHOD = "method";

    @Autowired
    private InternalVariableRepository internalVariableRepository;

    @Test
    @SneakyThrows
    @DisplayName("Все флаги включены, идем в redis")
    void searchPartnerSettingsMethods() {
        doReturn(REDIS_ACTUAL_VERSION).when(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);

        String settingsMethodsHash = "1:method";
        List<SettingsMethodDto> expectedSettingsMethods = buildSettingsMethodDto();

        doReturn(redisObjectConverter.serializeToString(buildYtPartnerApiSettingsDto()))
            .when(clientJedis).hget(eq(PARTNER_SETTINGS_TABLE_NAME), eq(settingsMethodsHash));

        softly.assertThat(lmsLomLightClient.searchPartnerApiSettingsMethods(buildFilter()))
            .usingRecursiveComparison()
            .isEqualTo(expectedSettingsMethods);

        verify(clientJedis).get(eq(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY));
        verify(clientJedis).hget(eq(PARTNER_SETTINGS_TABLE_NAME), eq(settingsMethodsHash));
    }

    @ParameterizedTest
    @MethodSource
    @DisplayName("Идем в redis за методами партнеров, данных в редисе нет")
    void noMethodsFoundInRedis(
        @SuppressWarnings("unused") String displayName,
        boolean noResultsAcceptable,
        boolean methodExistsInLms
    ) {
        setCheckingMethodsInLms(noResultsAcceptable);
        if (methodExistsInLms) {
            doReturn(buildSettingsMethodDto()).when(lmsClient).searchPartnerApiSettingsMethods(buildFilter());
        }
        lmsLomLightClient.searchPartnerApiSettingsMethods(buildFilter());

        String settingsMethodsHash = "1:method";
        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        verify(clientJedis).hget(eq(PARTNER_SETTINGS_TABLE_NAME), eq(settingsMethodsHash));

        if (!noResultsAcceptable) {
            verify(lmsClient).searchPartnerApiSettingsMethods(buildFilter());
        }

        softly.assertThat(backLogCaptor.getResults().toString().contains(LmsLomClientLogUtils.getEntityNotFoundLog(
                LmsLomLoggingCode.LMS_LOM_REDIS,
                "searchPartnerApiSettingsMethods partnerIds = [1], methodTypes = [method]"
            )))
            .isEqualTo(!noResultsAcceptable);
    }

    @Nonnull
    private static Stream<Arguments> noMethodsFoundInRedis() {
        return Stream.of(
            Arguments.of(
                "Не идём в лмс, так как флаг проверки в лмс выключен, данные в лмс есть",
                false,
                true
            ),
            Arguments.of(
                "Идём в лмс, так как флаг проверки в лмс включен, данные в лмс есть",
                true,
                true
            ),
            Arguments.of(
                "Не идём в лмс, так как флаг проверки в лмс выключен, данных в лмс нет",
                false,
                false
            ),
            Arguments.of(
                "Идём в лмс, так как флаг проверки в лмс включен, данных в лмс нет",
                true,
                false
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    @DisplayName("Достаем данные из разных источников (Redis, YT, LMS)")
    void getDataFromDifferentSources(
        @SuppressWarnings("unused") String displayName,
        boolean dataExistsInRedis,
        boolean dataExistsInYt,
        boolean fetchingFromYtEnabled,
        boolean dataExistsInLms,
        boolean fetchingFromRedisEnabled
    ) {
        setCheckingDataInRedis(fetchingFromRedisEnabled);
        setCheckingDataInYt(fetchingFromYtEnabled);

        String settingsMethodsHash = "1:method";
        List<SettingsMethodDto> expectedSettingsMethods = buildSettingsMethodDto();

        if (dataExistsInRedis) {
            doReturn(redisObjectConverter.serializeToString(buildYtPartnerApiSettingsDto()))
                .when(clientJedis).hget(eq(PARTNER_SETTINGS_TABLE_NAME), eq(settingsMethodsHash));
        }

        if (dataExistsInYt) {
            mockYtPartnerApiSettingsQueryResponse(expectedSettingsMethods);
        }

        if (dataExistsInLms) {
            doReturn(expectedSettingsMethods).when(lmsClient).searchPartnerApiSettingsMethods(buildFilter());
        }

        List<SettingsMethodDto> result = lmsLomLightClient.searchPartnerApiSettingsMethods(buildFilter());

        if (fetchingFromRedisEnabled) {
            verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
            verify(clientJedis).hget(eq(PARTNER_SETTINGS_TABLE_NAME), eq(settingsMethodsHash));
        }

        if (!fetchingFromRedisEnabled || !dataExistsInRedis) {
            if (fetchingFromYtEnabled) {
                verifyYtCalling();
            }

            if (!fetchingFromYtEnabled || !dataExistsInYt) {
                verify(lmsClient).searchPartnerApiSettingsMethods(buildFilter());
            }
        }

        if (
            (fetchingFromRedisEnabled && dataExistsInRedis)
                || (fetchingFromYtEnabled && dataExistsInYt)
                || dataExistsInLms
        ) {
            softly.assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expectedSettingsMethods);
        } else {
            softly.assertThat(result)
                .isEmpty();
        }
    }

    @Nonnull
    private static Stream<Arguments> getDataFromDifferentSources() {
        List<Arguments> arguments = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            boolean fetchingFromRedisEnabled = i == 1;
            arguments.addAll(LmsLomClientsChainTestCases.TEST_CASES.stream()
                .map(it -> Stream.concat(
                    Arrays.stream(it.getArguments("Получение API методов партнеров").get()),
                    Stream.of(fetchingFromRedisEnabled)
                ))
                .map(it -> Arguments.of(it.toArray()))
                .collect(Collectors.toList()));
        }
        return arguments.stream();
    }

    @Test
    @DisplayName("Пустой фильтр, идем в лмс")
    void emptyFilter() {
        SettingsMethodFilter filter = SettingsMethodFilter.newBuilder().build();
        lmsLomLightClient.searchPartnerApiSettingsMethods(filter);

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains("Filter must contain exactly one 'partnerId' and one 'methodType'");
        verify(lmsClient).searchPartnerApiSettingsMethods(eq(filter));
    }

    @Nonnull
    private YtPartnerApiSettings buildYtPartnerApiSettingsDto() {
        return new YtPartnerApiSettings()
            .setPartnerId(PARTNER_ID)
            .setMethod(METHOD)
            .setActive(true);
    }

    @Nonnull
    private List<SettingsMethodDto> buildSettingsMethodDto() {
        return List.of(
            SettingsMethodDto.newBuilder()
                .partnerId(PARTNER_ID)
                .method(METHOD)
                .active(true)
                .build()
        );
    }

    @Nonnull
    private SettingsMethodFilter buildFilter() {
        return SettingsMethodFilter.newBuilder()
            .partnerIds(Set.of(PARTNER_ID))
            .methodTypes(Set.of(METHOD))
            .build();
    }

    private void setCheckingMethodsInLms(boolean isCheckEnabled) {
        internalVariableRepository.save(
            new InternalVariable()
                .setType(InternalVariableType.CHECK_FOR_LIST_DATA_IF_NO_DATA_FOUND_IN_CLIENT)
                .setValue(Boolean.toString(isCheckEnabled))
        );
    }

    private void setCheckingDataInRedis(boolean isCheckEnabled) {
        internalVariableRepository.save(
            new InternalVariable()
                .setType(InternalVariableType.GET_PARTNER_API_SETTINGS_ENABLED)
                .setValue(Boolean.toString(isCheckEnabled))
        );
    }

    private void setCheckingDataInYt(boolean isCheckEnabled) {
        internalVariableRepository.save(
            new InternalVariable()
                .setType(InternalVariableType.GET_PARTNER_API_SETTINGS_FROM_YT_ENABLED)
                .setValue(Boolean.toString(isCheckEnabled))
        );
    }

    private void mockYtPartnerApiSettingsQueryResponse(List<SettingsMethodDto> expectedResult) {
        YtUtils.mockSelectRowsFromYt(
            ytTables,
            expectedResult,
            GET_PARTNER_API_SETTINGS_QUERY
        );
    }

    private void verifyYtCalling() {
        YtLmsVersionsUtils.verifyYtVersionTableInteractions(ytTables, lmsYtProperties);
        YtUtils.verifySelectRowsInteractions(
            ytTables,
            GET_PARTNER_API_SETTINGS_QUERY
        );
    }
}
