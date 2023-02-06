package ru.yandex.market.logistics.lms.client.redis;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
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
import ru.yandex.market.logistics.lom.utils.jobs.executor.RedisFromYtMigrationTestUtils;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.logistics.lom.utils.jobs.executor.RedisFromYtMigrationTestUtils.buildPartner;

@DisplayName("Получение партнеров по набору идентификаторов")
class LmsLomLightClientGetPartnersByIdsTest extends LmsLomLightClientAbstractTest {

    private static final Set<Long> PARTNER_IDS = Set.of(1L, 2L, 3L);
    private static final String GET_PARTNERS_BY_IDS_QUERY = "* FROM [//home/2022-03-02T08:05:24Z/partner_dyn] "
        + "WHERE id IN";

    @Autowired
    private InternalVariableRepository internalVariableRepository;

    @Autowired
    private PartnerYtToLmsConverter partnerYtToLmsConverter;

    @Test
    @DisplayName("Партнёры в redis не найдены, пишем в лог, идем в лмс")
    void partnerNotFoundInRedis() {
        lmsLomLightClient.getPartners(PARTNER_IDS);

        verify(lmsLomLightClient).getPartners(PARTNER_IDS);
        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        verifyMultiGetArguments(
            RedisKeys.getHashTableFromYtName(YtPartner.class, REDIS_ACTUAL_VERSION),
            Set.of("1", "2", "3")
        );
        verify(lmsClient).searchPartners(SearchPartnerFilter.builder().setIds(PARTNER_IDS).build());
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(LmsLomClientLogUtils.getEntityNotFoundLog(
                LmsLomLoggingCode.LMS_LOM_REDIS,
                "getPartnersByIds [1, 2, 3]"
            ));
    }

    @Test
    @DisplayName("Все нужные флаги включены, идем в redis")
    void allFlagsEnabled() {
        doReturn(REDIS_ACTUAL_VERSION).when(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);

        String partnerTableName = RedisKeys.getHashTableFromYtName(YtPartner.class, REDIS_ACTUAL_VERSION);

        doReturn(PARTNER_IDS.stream()
            .map(id -> convertToString(buildPartner(id)))
            .collect(Collectors.toList())
        ).when(clientJedis).hmget(eq(partnerTableName), any());

        Set<PartnerLightModel> expectedModels = PARTNER_IDS.stream()
            .map(RedisFromYtMigrationTestUtils::buildPartner)
            .map(partnerYtToLmsConverter::convert)
            .collect(Collectors.toSet());

        List<PartnerLightModel> actualModels = lmsLomLightClient.getPartners(PARTNER_IDS);

        softly.assertThat(actualModels)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(expectedModels);

        verify(lmsLomLightClient).getPartners(PARTNER_IDS);
        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        verifyMultiGetArguments(partnerTableName, Set.of("1", "2", "3"));
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

        Set<Long> partnerIds = Set.of(1L, 2L, 3L);
        List<YtPartner> expectedYtPartners = partnerIds.stream()
            .map(RedisFromYtMigrationTestUtils::buildPartner)
            .collect(Collectors.toList());

        if (dataExistsInRedis) {
            doReturn(expectedYtPartners.stream().map(this::convertToString).collect(Collectors.toList()))
                .when(clientJedis).hmget(
                    eq(RedisKeys.getHashTableFromYtName(YtPartner.class, REDIS_ACTUAL_VERSION)),
                    any()
                );
        }

        List<PartnerLightModel> partnerLightModels = partnerIds.stream()
            .map(this::partnerLightModel)
            .collect(Collectors.toList());

        if (dataExistsInYt) {
            mockYtGetPartnersQueryResponse(partnerLightModels);
        }

        if (dataExistsInLms) {
            doReturn(partnerLightModels).when(lmsClient)
                .searchPartners(SearchPartnerFilter.builder().setIds(partnerIds).build());
        }

        List<PartnerLightModel> result = lmsLomLightClient.getPartners(partnerIds);

        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(clientJedis).hmget(
            eq(RedisKeys.getHashTableFromYtName(YtPartner.class, REDIS_ACTUAL_VERSION)),
            argumentCaptor.capture()
        );

        softly.assertThat(argumentCaptor.getAllValues())
            .containsExactlyInAnyOrderElementsOf(
                partnerIds.stream().map(String::valueOf).collect(Collectors.toList())
            );

        if (!dataExistsInRedis) {
            if (fetchingFromYtEnabled) {
                verifyYtCalling();
            }

            if (!fetchingFromYtEnabled || !dataExistsInYt) {
                verify(lmsClient).searchPartners(any());
            }
        }

        if (dataExistsInRedis || (fetchingFromYtEnabled && dataExistsInYt) || dataExistsInLms) {
            softly.assertThat(result)
                .containsExactlyInAnyOrderElementsOf(partnerLightModels);
        } else {
            softly.assertThat(result)
                .isEmpty();
        }
    }

    @Nonnull
    private static Stream<Arguments> getDataFromDifferentSources() {
        return LmsLomClientsChainTestCases.TEST_CASES.stream()
            .map(it -> it.getArguments("Получение партнеров по идентификаторам"));
    }

    private void setCheckingDataInYt(boolean isCheckEnabled) {
        internalVariableRepository.save(
            new InternalVariable()
                .setType(InternalVariableType.GET_PARTNERS_BY_IDS_FROM_YT_ENABLED)
                .setValue(Boolean.toString(isCheckEnabled))
        );
    }

    private void verifyYtCalling() {
        YtLmsVersionsUtils.verifyYtVersionTableInteractions(ytTables, lmsYtProperties);
        YtUtils.verifySelectRowsInteractionsQueryStartsWith(
            ytTables,
            GET_PARTNERS_BY_IDS_QUERY
        );
    }

    private void mockYtGetPartnersQueryResponse(List<PartnerLightModel> response) {
        YtUtils.mockSelectRowsFromYtQueryStartsWith(
            ytTables,
            response,
            GET_PARTNERS_BY_IDS_QUERY
        );
    }

    @Nonnull
    private PartnerLightModel partnerLightModel(Long partnerId) {
        return partnerYtToLmsConverter.convert(buildPartner(partnerId));
    }
}
