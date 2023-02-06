package ru.yandex.market.logistics.lms.client.redis;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lms.client.utils.LmsLomClientsChainTestCases;
import ru.yandex.market.logistics.lom.entity.InternalVariable;
import ru.yandex.market.logistics.lom.entity.enums.InternalVariableType;
import ru.yandex.market.logistics.lom.lms.model.PartnerRelationLightModel;
import ru.yandex.market.logistics.lom.service.redis.util.RedisKeys;
import ru.yandex.market.logistics.lom.service.yt.dto.YtPartnerRelation;
import ru.yandex.market.logistics.lom.service.yt.dto.YtPartnerRelationTo;
import ru.yandex.market.logistics.lom.utils.YtLmsVersionsUtils;
import ru.yandex.market.logistics.lom.utils.YtUtils;
import ru.yandex.market.logistics.management.entity.page.PageRequest;
import ru.yandex.market.logistics.management.entity.page.PageResult;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.response.CutoffResponse;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.logistics.lms.client.redis.LmsLomLightClientGetPartnerRelationTest.PARTNER_RELATION_TABLE_NAME;
import static ru.yandex.market.logistics.lms.client.redis.LmsLomLightClientGetPartnerRelationTest.PARTNER_RELATION_TO_TABLE_NAME;

@DisplayName("Получение связок партнеров по цепочке Redis, YT, LMS")
class LmsLomLightClientGetPartnerRelatioRedisYtLmsChainTest extends LmsLomLightClientAbstractTest {

    private static final String CUTOFFS_QUERY = "* FROM [//home/2022-03-02T08:05:24Z/partner_relation_dyn] "
        + "WHERE partner_from = 1 AND partner_to = 2";

    private static final String PARTNER_RETURNS_QUERY = "* FROM [//home/2022-03-02T08:05:24Z/partner_relation_to_dyn] "
        + "WHERE partner_from IN (";

    public static final PartnerRelationFilter CUTOFFS_FILTER = PartnerRelationFilter.newBuilder()
        .fromPartnersIds(Set.of(1L))
        .toPartnersIds(Set.of(2L))
        .build();
    private static final PartnerRelationFilter RETURN_PARTNERS_FILTER = PartnerRelationFilter.newBuilder()
        .fromPartnersIds(Set.of(1L, 2L))
        .build();

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Достаем данные из разных источников (Redis, YT, LMS) по катоффам")
    void getDataFromDifferentSourcesCutoffs(
        @SuppressWarnings("unused") String displayName,
        boolean dataExistsInRedis,
        boolean dataExistsInYt,
        boolean fetchingFromYtEnabled,
        boolean dataExistsInLms
    ) {
        setCheckingDataInYt(fetchingFromYtEnabled);

        List<PartnerRelationEntityDto> expectedDtos = getSinglePartnerRelationDtoWithCutoffs();
        List<PartnerRelationLightModel> expectedRelations =
            expectedDtos.stream().map(PartnerRelationLightModel::build).collect(Collectors.toList());

        if (dataExistsInRedis) {
            doReturn(redisObjectConverter.serializeToString(buildSingleRelationYtModel())).when(clientJedis)
                .hget(eq(PARTNER_RELATION_TABLE_NAME), eq("1:2"));
        }

        if (dataExistsInYt) {
            YtUtils.mockSelectRowsFromYt(
                ytTables,
                expectedRelations,
                CUTOFFS_QUERY
            );
        }

        if (dataExistsInLms) {
            doReturn(new PageResult<PartnerRelationEntityDto>().setData(expectedDtos))
                .when(lmsClient)
                .searchPartnerRelation(eq(CUTOFFS_FILTER), eq(new PageRequest(0, 1)));
        }

        List<PartnerRelationLightModel> actualRelations =
            lmsLomLightClient.searchPartnerRelationWithCutoffs(CUTOFFS_FILTER);
        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        verify(clientJedis).hget(eq(PARTNER_RELATION_TABLE_NAME), eq("1:2"));

        if (!dataExistsInRedis) {
            if (fetchingFromYtEnabled) {
                YtLmsVersionsUtils.verifyYtVersionTableInteractions(ytTables, lmsYtProperties);
                YtUtils.verifySelectRowsInteractions(
                    ytTables,
                    CUTOFFS_QUERY
                );
            }

            if (!dataExistsInYt || !fetchingFromYtEnabled) {
                verify(lmsClient).searchPartnerRelation(eq(CUTOFFS_FILTER), eq(new PageRequest(0, 1)));
            }
        }

        if (dataExistsInRedis || (fetchingFromYtEnabled && dataExistsInYt) || dataExistsInLms) {
            softly.assertThat(actualRelations)
                .containsExactlyInAnyOrderElementsOf(expectedRelations);
        } else {
            softly.assertThat(actualRelations)
                .isEmpty();
        }
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Достаем данные из разных источников (Redis, YT, LMS) по возвратным партнерам")
    void getDataFromDifferentSourcesPartnerReturns(
        @SuppressWarnings("unused") String displayName,
        boolean dataExistsInRedis,
        boolean dataExistsInYt,
        boolean fetchingFromYtEnabled,
        boolean dataExistsInLms
    ) {
        setCheckingDataInYt(fetchingFromYtEnabled);

        List<PartnerRelationEntityDto> expectedDtos = getPartnerRelationsDtoWithPartnerReturns();
        List<PartnerRelationLightModel> expectedRelations =
            expectedDtos.stream().map(PartnerRelationLightModel::build).collect(Collectors.toList());

        if (dataExistsInRedis) {
            doReturn(List.of(
                redisObjectConverter.serializeToString(buildRelationToYtModel(1L)),
                redisObjectConverter.serializeToString(buildRelationToYtModel(2L))
            ))
                .when(clientJedis)
                .hmget(eq(PARTNER_RELATION_TO_TABLE_NAME), any());
        }

        if (dataExistsInYt) {
            YtUtils.mockSelectRowsFromYtQueryStartsWith(
                ytTables,
                expectedRelations,
                PARTNER_RETURNS_QUERY
            );
        }

        if (dataExistsInLms) {
            doReturn(expectedDtos)
                .when(lmsClient)
                .searchPartnerRelation(eq(RETURN_PARTNERS_FILTER));
        }

        List<PartnerRelationLightModel> actualRelations =
            lmsLomLightClient.searchPartnerRelationsWithReturnPartners(RETURN_PARTNERS_FILTER);
        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        verify(clientJedis).hmget(eq(PARTNER_RELATION_TO_TABLE_NAME), any());

        if (!dataExistsInRedis) {
            if (fetchingFromYtEnabled) {
                YtLmsVersionsUtils.verifyYtVersionTableInteractions(ytTables, lmsYtProperties);
                YtUtils.verifySelectRowsInteractionsQueryStartsWith(
                    ytTables,
                    PARTNER_RETURNS_QUERY
                );
            }

            if (!dataExistsInYt || !fetchingFromYtEnabled) {
                verify(lmsClient).searchPartnerRelation(eq(RETURN_PARTNERS_FILTER));
            }
        }

        if (dataExistsInRedis || (fetchingFromYtEnabled && dataExistsInYt) || dataExistsInLms) {
            softly.assertThat(actualRelations)
                .containsExactlyInAnyOrderElementsOf(expectedRelations);
        } else {
            softly.assertThat(actualRelations)
                .isEmpty();
        }

    }

    @Nonnull
    @SuppressWarnings("unused")
    private static Stream<Arguments> getDataFromDifferentSourcesCutoffs() {
        return LmsLomClientsChainTestCases.TEST_CASES.stream()
            .map(it -> it.getArguments("Получение связок партнеров с катоффами"));
    }

    @Nonnull
    @SuppressWarnings("unused")
    private static Stream<Arguments> getDataFromDifferentSourcesPartnerReturns() {
        return LmsLomClientsChainTestCases.TEST_CASES.stream()
            .map(it -> it.getArguments("Получение связок партнеров с возвратными партнерами"));
    }

    private void setCheckingDataInYt(boolean fetchingFromYtEnabled) {
        internalVariableRepository.save(
            new InternalVariable()
                .setType(InternalVariableType.GET_PARTNER_RELATION_ENABLED)
                .setValue(Boolean.toString(fetchingFromYtEnabled))
        );
    }

    @Nonnull
    private List<PartnerRelationEntityDto> getSinglePartnerRelationDtoWithCutoffs() {
        return List.of(
            PartnerRelationLightModel.newBuilder()
                .fromPartnerId(1L)
                .toPartnerId(2L)
                .cutoffs(getCutoffs())
                .build()
        );
    }

    @Nonnull
    private List<PartnerRelationEntityDto> getPartnerRelationsDtoWithPartnerReturns() {
        return List.of(
            PartnerRelationLightModel.newBuilder()
                .fromPartnerId(1L)
                .toPartnerId(2L)
                .returnPartnerId(4L)
                .build(),
            PartnerRelationLightModel.newBuilder()
                .fromPartnerId(1L)
                .toPartnerId(3L)
                .returnPartnerId(5L)
                .build(),
            PartnerRelationLightModel.newBuilder()
                .fromPartnerId(2L)
                .toPartnerId(3L)
                .returnPartnerId(5L)
                .build(),
            PartnerRelationLightModel.newBuilder()
                .fromPartnerId(2L)
                .toPartnerId(4L)
                .returnPartnerId(6L)
                .build()
        );
    }

    @Nonnull
    private Set<CutoffResponse> getCutoffs() {
        return Set.of(
            CutoffResponse.newBuilder()
                .cutoffTime(LocalTime.of(12, 30))
                .locationId(30)
                .build(),
            CutoffResponse.newBuilder()
                .cutoffTime(LocalTime.of(12, 40))
                .locationId(31)
                .build()
        );
    }

    @Nonnull
    private YtPartnerRelation buildSingleRelationYtModel() {
        return new YtPartnerRelation()
            .setPartnerFrom(1L)
            .setPartnerTo(2L)
            .setCutoffs(Optional.of(buildJsonCutoffs()));
    }

    @Nonnull
    private YtPartnerRelationTo buildRelationToYtModel(Long fromPartnerId) {
        return new YtPartnerRelationTo()
            .setPartnerFrom(fromPartnerId)
            .setPartnersTo(buildPartnersToJson(fromPartnerId));
    }

    private String buildPartnersToJson(Long partnerId) {
        return "{"
            + "\"relations\":"
            + "["
            + "{\"partner_to\":" + (partnerId + 1) + ",\"return_partner\":" + (partnerId + 3) + "},"
            + "{\"partner_to\":" + (partnerId + 2) + ",\"return_partner\":" + (partnerId + 4) + "}"
            + "]"
            + "}";
    }

    @Nonnull
    private String buildJsonCutoffs() {
        return "{"
            + "\"cutoffs\":"
            + "["
            + "{\"cutoff_time\":\"12:30:00.000000\",\"location_id\":30}, "
            + "{\"cutoff_time\":\"12:40:00.000000\",\"location_id\":31}"
            + "]"
            + "}";
    }
}
