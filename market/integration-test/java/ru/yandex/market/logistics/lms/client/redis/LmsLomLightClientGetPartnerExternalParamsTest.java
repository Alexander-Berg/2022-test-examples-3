package ru.yandex.market.logistics.lms.client.redis;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lms.client.utils.LmsLomClientLogUtils;
import ru.yandex.market.logistics.lms.client.utils.LmsLomClientsChainTestCases;
import ru.yandex.market.logistics.lms.client.utils.PartnerExternalParamsDataUtils;
import ru.yandex.market.logistics.lom.entity.InternalVariable;
import ru.yandex.market.logistics.lom.entity.enums.InternalVariableType;
import ru.yandex.market.logistics.lom.lms.model.logging.enums.LmsLomLoggingCode;
import ru.yandex.market.logistics.lom.service.redis.util.RedisKeys;
import ru.yandex.market.logistics.lom.service.yt.dto.YtPartnerExternalParam;
import ru.yandex.market.logistics.lom.utils.YtLmsVersionsUtils;
import ru.yandex.market.logistics.lom.utils.YtUtils;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParamGroup;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.logistics.lom.utils.jobs.executor.RedisFromYtMigrationTestUtils.buildExternalParams;

@ParametersAreNonnullByDefault
@DisplayName("Получение параметров партнеров")
class LmsLomLightClientGetPartnerExternalParamsTest extends LmsLomLightClientAbstractTest {

    @Test
    @SneakyThrows
    @DisplayName("В redis данных нет, идем в lms")
    void noParamsFoundInRedis() {
        lmsLomLightClient.getPartnerExternalParams(Set.of(PartnerExternalParamType.DISABLE_AUTO_CANCEL_AFTER_SLA));

        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        verify(clientJedis).hget(
            RedisKeys.getHashTableFromYtName(
                YtPartnerExternalParam.class,
                RedisKeys.REDIS_DEFAULT_VERSION
            ),
            PartnerExternalParamType.DISABLE_AUTO_CANCEL_AFTER_SLA.toString()
        );
        verify(lmsClient).getPartnerExternalParams(Set.of(PartnerExternalParamType.DISABLE_AUTO_CANCEL_AFTER_SLA));

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(LmsLomClientLogUtils.getEntityNotFoundLog(
                    LmsLomLoggingCode.LMS_LOM_REDIS,
                    "getPartnerExternalParamsByTypes types = [DISABLE_AUTO_CANCEL_AFTER_SLA]"
                )
            );
    }

    @Test
    @SneakyThrows
    @DisplayName("В redis данных нет и флаг CHECK_FOR_LIST_DATA_IF_NO_DATA_FOUND_IN_CLIENT включен, не идем в yt и lms")
    void noParamsFoundInRedisAndFlagNoDataAcceptableEnabled() {
        setNoDataAcceptable(true);
        List<PartnerExternalParamGroup> response =
            lmsLomLightClient.getPartnerExternalParams(Set.of(PartnerExternalParamType.DISABLE_AUTO_CANCEL_AFTER_SLA));

        softly.assertThat(response).isEmpty();
        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        verify(clientJedis).hget(
            RedisKeys.getHashTableFromYtName(
                YtPartnerExternalParam.class,
                RedisKeys.REDIS_DEFAULT_VERSION
            ),
            PartnerExternalParamType.DISABLE_AUTO_CANCEL_AFTER_SLA.toString()
        );
    }

    @Test
    @SneakyThrows
    @DisplayName("Параметры в redis найдены")
    void hasParamsInRedis() {
        doReturn(REDIS_ACTUAL_VERSION).when(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);

        String externalParamKey = "DISABLE_AUTO_CANCEL_AFTER_SLA";

        String partnerExternalParamTableName = RedisKeys.getHashTableFromYtName(
            YtPartnerExternalParam.class,
            REDIS_ACTUAL_VERSION
        );

        doReturn(convertToString(buildExternalParams()))
            .when(clientJedis).hget(partnerExternalParamTableName, externalParamKey);

        List<PartnerExternalParamGroup> expectedValues = List.of(
            new PartnerExternalParamGroup(1L, List.of(new PartnerExternalParam(externalParamKey, null, "1"))),
            new PartnerExternalParamGroup(2L, List.of(new PartnerExternalParam(externalParamKey, null, "0")))
        );

        softly.assertThat(lmsLomLightClient.getPartnerExternalParams(
                Set.of(PartnerExternalParamType.DISABLE_AUTO_CANCEL_AFTER_SLA)
            ))
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(expectedValues);

        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        verify(clientJedis).hget(partnerExternalParamTableName, externalParamKey);
    }

    @MethodSource
    @DisplayName("Получение параметров партнёров redis -> yt -> lms")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    void getPartnerExternalParamsFromDifferentSources(
        @SuppressWarnings("unused") String displayName,
        boolean dataExistsInRedis,
        boolean dataExistsInYt,
        boolean fetchingFromYtEnabled,
        boolean dataExistsInLms
    ) {
        mockRedisPartnerExternalParamsData(dataExistsInRedis);
        mockYtForGetExternalParamsData(fetchingFromYtEnabled, dataExistsInYt);
        mockLmsPartnerExternalParamsData(dataExistsInLms);

        List<PartnerExternalParamGroup> paramsResponse = lmsLomLightClient.getPartnerExternalParams(
            PartnerExternalParamsDataUtils.PARTNER_EXTERNAL_PARAM_TYPES_SET
        );
        if (dataExistsInRedis || (dataExistsInYt && fetchingFromYtEnabled) || dataExistsInLms) {
            softly.assertThat(paramsResponse)
                .containsExactlyInAnyOrderElementsOf(PartnerExternalParamsDataUtils.partnerExternalParams());
        } else {
            softly.assertThat(paramsResponse).isEmpty();
        }

        verifyRedisInteractions();
        if (dataExistsInRedis) {
            return;
        }
        if (fetchingFromYtEnabled) {
            verify(hahnYt, times(2)).tables();
            YtLmsVersionsUtils.verifyYtVersionTableInteractions(ytTables, lmsYtProperties);
            YtUtils.verifySelectRowsInteractions(
                ytTables,
                PartnerExternalParamsDataUtils.GET_PARTNER_EXTERNAL_PARAMS_QUERY
            );
        }
        if (fetchingFromYtEnabled && dataExistsInYt) {
            return;
        }

        verify(lmsClient).getPartnerExternalParams(PartnerExternalParamsDataUtils.PARTNER_EXTERNAL_PARAM_TYPES_SET);
    }

    @Nonnull
    private static Stream<Arguments> getPartnerExternalParamsFromDifferentSources() {
        return LmsLomClientsChainTestCases.TEST_CASES.stream()
            .map(testCase -> testCase.getArguments("Получение параметров партнёров"));
    }

    private void verifyRedisInteractions() {
        String partnerExternalParamTableName = RedisKeys.getHashTableFromYtName(
            YtPartnerExternalParam.class,
            REDIS_ACTUAL_VERSION
        );
        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        PartnerExternalParamsDataUtils.PARTNER_EXTERNAL_PARAM_TYPES.forEach(
            paramType -> verify(clientJedis).hget(partnerExternalParamTableName, paramType.name())
        );
    }

    private void mockRedisPartnerExternalParamsData(boolean dataExistsInRedis) {
        if (!dataExistsInRedis) {
            return;
        }

        String partnerExternalParamTableName = RedisKeys.getHashTableFromYtName(
            YtPartnerExternalParam.class,
            REDIS_ACTUAL_VERSION
        );
        for (int i = 0; i < PartnerExternalParamsDataUtils.PARTNER_EXTERNAL_PARAM_TYPES.size(); i++) {
            String paramName = PartnerExternalParamsDataUtils.PARTNER_EXTERNAL_PARAM_TYPES.get(i).name();

            doReturn(convertToString(buildExternalParams(paramName, (long) (i + 1), "true")))
                .when(clientJedis).hget(partnerExternalParamTableName, paramName);
        }
    }

    private void mockYtForGetExternalParamsData(boolean fetchingFromYtEnabled, boolean dataExistsInYt) {
        internalVariableRepository.save(
            new InternalVariable()
                .setType(InternalVariableType.GET_PARTNER_EXTERNAL_PARAMS_ENABLED)
                .setValue(String.valueOf(fetchingFromYtEnabled))
        );

        List<PartnerExternalParamGroup> params =
            dataExistsInYt ? PartnerExternalParamsDataUtils.partnerExternalParams() : List.of();

        YtUtils.mockSelectRowsFromYt(
            ytTables,
            params,
            PartnerExternalParamsDataUtils.GET_PARTNER_EXTERNAL_PARAMS_QUERY
        );
    }

    private void mockLmsPartnerExternalParamsData(boolean dataExistsInLms) {
        if (dataExistsInLms) {
            doReturn(PartnerExternalParamsDataUtils.partnerExternalParams())
                .when(lmsClient).getPartnerExternalParams(
                    PartnerExternalParamsDataUtils.PARTNER_EXTERNAL_PARAM_TYPES_SET
                );
        }
    }

    private void setNoDataAcceptable(boolean noDataAcceptable) {
        internalVariableRepository.save(
            new InternalVariable()
                .setType(InternalVariableType.CHECK_FOR_LIST_DATA_IF_NO_DATA_FOUND_IN_CLIENT)
                .setValue(Boolean.toString(noDataAcceptable))
        );
    }
}
