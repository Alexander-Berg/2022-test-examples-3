package ru.yandex.market.logistics.lms.client.redis;

import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

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
import ru.yandex.market.logistics.lom.lms.converter.PartnerYtToLmsConverter;
import ru.yandex.market.logistics.lom.lms.model.PartnerLightModel;
import ru.yandex.market.logistics.lom.lms.model.logging.enums.LmsLomLoggingCode;
import ru.yandex.market.logistics.lom.repository.InternalVariableRepository;
import ru.yandex.market.logistics.lom.service.redis.util.RedisKeys;
import ru.yandex.market.logistics.lom.service.yt.dto.YtPartner;
import ru.yandex.market.logistics.lom.utils.YtLmsVersionsUtils;
import ru.yandex.market.logistics.lom.utils.YtUtils;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.logistics.lom.utils.jobs.executor.RedisFromYtMigrationTestUtils.buildPartner;

@DisplayName("Получение партнера по идентификатору")
class LmsLomLightClientGetPartnerByIdTest extends LmsLomLightClientAbstractTest {

    private static final String GET_PARTNER_BY_ID_QUERY = "* FROM [//home/2022-03-02T08:05:24Z/partner_dyn] "
        + "WHERE id = 1";

    @Autowired
    private PartnerYtToLmsConverter partnerYtToLmsConverter;

    @Autowired
    private InternalVariableRepository internalVariableRepository;

    @Test
    @DisplayName("Партнёр в redis не найден, пишем в лог, идем в лмс")
    void partnerNotFoundInRedis() {
        long partnerId = 1L;
        lmsLomLightClient.getPartner(partnerId);

        verify(lmsLomLightClient).getPartner(partnerId);
        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        verify(clientJedis).hget(
            RedisKeys.getHashTableFromYtName(YtPartner.class, REDIS_ACTUAL_VERSION),
            String.valueOf(partnerId)
        );
        verify(lmsClient).getPartner(partnerId);
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(LmsLomClientLogUtils.getEntityNotFoundLog(
                LmsLomLoggingCode.LMS_LOM_REDIS,
                "getPartnerById id = 1"
            ));
    }

    @Test
    @DisplayName("Партнёр есть в redis")
    void allFlagsEnabled() {
        doReturn(REDIS_ACTUAL_VERSION).when(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);

        String partnerTableName = RedisKeys.getHashTableFromYtName(YtPartner.class, REDIS_ACTUAL_VERSION);
        long partnerId = 1L;
        doReturn(convertToString(buildPartner(partnerId)))
            .when(clientJedis).hget(partnerTableName, String.valueOf(partnerId));

        YtPartner expectedYtPartner = buildPartner(partnerId);

        PartnerLightModel expectedModel = partnerYtToLmsConverter.convert(expectedYtPartner);

        PartnerLightModel actualModel = lmsLomLightClient.getPartner(1L).get();

        softly.assertThat(actualModel).usingRecursiveComparison().isEqualTo(expectedModel);

        verify(lmsLomLightClient).getPartner(1L);
        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        verify(clientJedis).hget(partnerTableName, String.valueOf(partnerId));
    }


    @ParameterizedTest
    @MethodSource
    @DisplayName("Достаем данные из разных источников (Redis, YT, LMS)")
    void getDataFromDifferentSources(
        @SuppressWarnings("unused") String displayName,
        boolean dataExistsInRedis,
        boolean dataExistsInYt,
        boolean fetchingFromYtEnabled,
        boolean dataExistsInLms
    ) {
        setCheckingDataInYt(fetchingFromYtEnabled);

        long partnerId = 1L;
        String partnerTableName = RedisKeys.getHashTableFromYtName(YtPartner.class, REDIS_ACTUAL_VERSION);

        if (dataExistsInRedis) {
            doReturn(convertToString(buildPartner(partnerId)))
                .when(clientJedis).hget(partnerTableName, String.valueOf(partnerId));
        }

        if (dataExistsInYt) {
            mockYtGetPartnerQueryResponse(partnerLightModel(partnerId));
        }

        if (dataExistsInLms) {
            doReturn(Optional.of(partnerLightModel(partnerId))).when(lmsClient).getPartner(1L);
        }

        Optional<PartnerLightModel> result = lmsLomLightClient.getPartner(1L);

        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        verify(clientJedis).hget(partnerTableName, String.valueOf(partnerId));

        if (!dataExistsInRedis) {
            if (fetchingFromYtEnabled) {
                verifyYtCalling();
            }

            if (!fetchingFromYtEnabled || !dataExistsInYt) {
                verify(lmsClient).getPartner(1L);
            }
        }

        if (dataExistsInRedis || (fetchingFromYtEnabled && dataExistsInYt) || dataExistsInLms) {
            softly.assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(Optional.of(partnerLightModel(partnerId)));
        } else {
            softly.assertThat(result)
                .isEmpty();
        }
    }

    @Nonnull
    private static Stream<Arguments> getDataFromDifferentSources() {
        return LmsLomClientsChainTestCases.TEST_CASES.stream()
            .map(it -> it.getArguments("Получение партнера"));
    }

    private void setCheckingDataInYt(boolean isCheckEnabled) {
        internalVariableRepository.save(
            new InternalVariable()
                .setType(InternalVariableType.GET_PARTNER_BY_ID_FROM_YT_ENABLED)
                .setValue(Boolean.toString(isCheckEnabled))
        );
    }

    private void verifyYtCalling() {
        YtLmsVersionsUtils.verifyYtVersionTableInteractions(ytTables, lmsYtProperties);
        YtUtils.verifySelectRowsInteractionsQueryStartsWith(
            ytTables,
            GET_PARTNER_BY_ID_QUERY
        );
    }

    private void mockYtGetPartnerQueryResponse(PartnerLightModel response) {
        YtUtils.mockSelectRowsFromYt(
            ytTables,
            Optional.of(response),
            GET_PARTNER_BY_ID_QUERY
        );
    }

    @Nonnull
    private PartnerLightModel partnerLightModel(Long partnerId) {
        return partnerYtToLmsConverter.convert(buildPartner(partnerId));
    }
}
