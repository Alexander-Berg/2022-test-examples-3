package ru.yandex.market.logistics.lms.client.redis;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lms.client.utils.LmsLomClientLogUtils;
import ru.yandex.market.logistics.lom.entity.InternalVariable;
import ru.yandex.market.logistics.lom.entity.enums.InternalVariableType;
import ru.yandex.market.logistics.lom.lms.converter.PartnerRelationYtToLmsConverter;
import ru.yandex.market.logistics.lom.lms.model.logging.enums.LmsLomLoggingCode;
import ru.yandex.market.logistics.lom.repository.InternalVariableRepository;
import ru.yandex.market.logistics.lom.service.redis.util.RedisKeys;
import ru.yandex.market.logistics.lom.service.yt.dto.YtPartnerRelation;
import ru.yandex.market.logistics.lom.service.yt.dto.YtPartnerRelationTo;
import ru.yandex.market.logistics.management.entity.page.PageRequest;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.logistics.lom.utils.jobs.executor.RedisFromYtMigrationTestUtils.buildPartnerRelation;
import static ru.yandex.market.logistics.lom.utils.jobs.executor.RedisFromYtMigrationTestUtils.buildPartnerRelationTo;

@ParametersAreNonnullByDefault
@DisplayName("Получение связок партнеров")
class LmsLomLightClientGetPartnerRelationTest extends LmsLomLightClientAbstractTest {

    public static final String PARTNER_RELATION_TABLE_NAME = RedisKeys.getHashTableFromYtName(
        YtPartnerRelation.class,
        REDIS_ACTUAL_VERSION
    );
    public static final String PARTNER_RELATION_TO_TABLE_NAME = RedisKeys.getHashTableFromYtName(
        YtPartnerRelationTo.class,
        REDIS_ACTUAL_VERSION
    );

    @Autowired
    private PartnerRelationYtToLmsConverter partnerRelationYtToLmsConverter;

    @Autowired
    private InternalVariableRepository internalVariableRepository;

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("noRelationsFoundInRedis")
    @SneakyThrows
    @DisplayName("Идем в redis за моделью с катоффами, данных в редисе нет")
    void withCutoffsNoDataInRedis(String dispayName, boolean noDataAcceptable, boolean relationExistsInLms) {
        setCheckingRelationsInLms(noDataAcceptable);
        if (relationExistsInLms) {
            mockLmsClient();
        }
        lmsLomLightClient.searchPartnerRelationWithCutoffs(buildFilterForOneRelation());

        verify(clientJedis).hget(eq(PARTNER_RELATION_TABLE_NAME), eq("1:2"));
        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        if (!noDataAcceptable) {
            verify(lmsClient).searchPartnerRelation(buildFilterForOneRelation(), new PageRequest(0, 1));
        }
        softly.assertThat(backLogCaptor.getResults().toString().contains(
                LmsLomClientLogUtils.getEntityNotFoundLog(
                    LmsLomLoggingCode.LMS_LOM_REDIS,
                    "searchPartnerRelationWithCutoffs partnerFromIds = [1], partnerToIds = [2]"
                )
            ))
            .isEqualTo(!noDataAcceptable);
    }

    @ParameterizedTest
    @MethodSource("noRelationsFoundInRedis")
    @DisplayName("Идем в redis за моделями с возвратными партнерами, данных в редисе нет")
    void withReturnPartnersDataInRedisNotFound(
        @SuppressWarnings("unused") String displayName,
        boolean noResultsAcceptable,
        boolean relationExistsInLms
    ) {
        setCheckingRelationsInLms(noResultsAcceptable);
        if (relationExistsInLms) {
            mockLmsClient();
        }
        lmsLomLightClient.searchPartnerRelationsWithReturnPartners(buildFilterForMultipleRelations());

        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        verify(clientJedis).hmget(eq(PARTNER_RELATION_TO_TABLE_NAME), any());

        if (!noResultsAcceptable) {
            verify(lmsClient).searchPartnerRelation(buildFilterForMultipleRelations());
        }
        softly.assertThat(backLogCaptor.getResults().toString().contains(LmsLomClientLogUtils.getEntityNotFoundLog(
                LmsLomLoggingCode.LMS_LOM_REDIS,
                "searchPartnerRelationsWithReturnPartners partnerFromIds = [1, 2]"
            )))
            .isEqualTo(!noResultsAcceptable);
    }

    @Nonnull
    private static Stream<Arguments> noRelationsFoundInRedis() {
        return Stream.of(
            Arguments.of(
                "Не идём в лмс, флаг проверки данных выключен, данные в лмс есть",
                false,
                true
            ),
            Arguments.of(
                "Идём в лмс, флаг проверки данных включен, данные в лмс есть",
                true,
                true
            ),
            Arguments.of(
                "Не идём в лмс, флаг проверки данных выключен, данных в лмс нет",
                false,
                false
            ),
            Arguments.of(
                "Идём в лмс, флаг проверки данных включен, данных в лмс нет",
                true,
                false
            )
        );
    }

    @Test
    @SneakyThrows
    @DisplayName("Идем в redis за моделью с катоффами")
    void setPartnerRelationWithCutoffs() {
        doReturn(REDIS_ACTUAL_VERSION).when(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);

        YtPartnerRelation expectedModel = buildPartnerRelation(1L);

        doReturn(objectMapper.writeValueAsString(expectedModel))
            .when(clientJedis).hget(eq(PARTNER_RELATION_TABLE_NAME), eq("1:2"));

        softly.assertThat(lmsLomLightClient.searchPartnerRelationWithCutoffs(buildFilterForOneRelation()))
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(List.of(partnerRelationYtToLmsConverter.convert(expectedModel)));

        verify(clientJedis).hget(eq(PARTNER_RELATION_TABLE_NAME), eq("1:2"));
        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
    }

    @Test
    @DisplayName("Идем в redis за моделями с возвратными партнерами")
    void setPartnerRelationWithReturnPartners() {
        doReturn(REDIS_ACTUAL_VERSION).when(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);

        List<YtPartnerRelationTo> expectedModels = List.of(
            buildPartnerRelationTo(1L),
            buildPartnerRelationTo(2L)
        );

        doReturn(expectedModels.stream().map(this::convertToString).collect(Collectors.toList()))
            .when(clientJedis).hmget(eq(PARTNER_RELATION_TO_TABLE_NAME), any());

        softly.assertThat(lmsLomLightClient.searchPartnerRelationsWithReturnPartners(buildFilterForMultipleRelations()))
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(expectedModels.stream()
                .map(partnerRelationYtToLmsConverter::convert)
                .flatMap(List::stream)
                .collect(Collectors.toList())
            );

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(clientJedis).hmget(eq(PARTNER_RELATION_TO_TABLE_NAME), captor.capture());
        softly.assertThat(captor.getAllValues()).containsExactlyInAnyOrderElementsOf(List.of("1", "2"));
        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("incorrectCutoffFiltersSource")
    @DisplayName("Идем в redis за моделю с катоффами, некорректный фильтр")
    void incorrectCutoffFilters(String name, PartnerRelationFilter filter) {
        lmsLomLightClient.searchPartnerRelationWithCutoffs(filter);
        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "Filter must contain exactly one 'fromPartnerId' and 'toPartnerId'"
        );
        verify(lmsClient).searchPartnerRelation(filter, new PageRequest(0, 1));
    }

    @Test
    @DisplayName("Идем в redis за моделью с возвратными партнерами, пустой фильтр")
    void setPartnerRelationWithReturnPartnersEmptyFilter() {
        PartnerRelationFilter filter = PartnerRelationFilter.newBuilder().build();
        lmsLomLightClient.searchPartnerRelationsWithReturnPartners(filter);
        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "Field 'fromPartnerIds' in filter must not be null"
        );
        verify(lmsClient).searchPartnerRelation(filter);
    }

    @Nonnull
    private static Stream<Arguments> incorrectCutoffFiltersSource() {
        return Stream.of(
            Arguments.of(
                "Пустой фильтр",
                PartnerRelationFilter.newBuilder().build()
            ),
            Arguments.of(
                "В полях фильтра не один партнер",
                PartnerRelationFilter.newBuilder()
                    .fromPartnersIds(Set.of(1L, 2L))
                    .toPartnersIds(Set.of(3L, 4L))
                    .build()
            )
        );
    }

    @Nonnull
    private PartnerRelationFilter buildFilterForOneRelation() {
        return PartnerRelationFilter.newBuilder()
            .fromPartnersIds(Set.of(1L))
            .toPartnersIds(Set.of(2L))
            .build();
    }

    @Nonnull
    private PartnerRelationFilter buildFilterForMultipleRelations() {
        return PartnerRelationFilter.newBuilder()
            .fromPartnersIds(Set.of(1L, 2L))
            .build();
    }

    private void mockLmsClient() {
        doReturn(
            new ru.yandex.market.logistics.management.entity.page.PageResult<PartnerRelationEntityDto>()
                .setData(List.of(PartnerRelationEntityDto.newBuilder().build()))
        )
            .when(lmsClient).searchPartnerRelation(buildFilterForOneRelation(), new PageRequest(0, 1));
        doReturn(List.of(PartnerRelationEntityDto.newBuilder().build()))
            .when(lmsClient).searchPartnerRelation(buildFilterForMultipleRelations());
    }

    private void setCheckingRelationsInLms(boolean isCheckEnabled) {
        internalVariableRepository.save(
            new InternalVariable()
                .setType(InternalVariableType.CHECK_FOR_LIST_DATA_IF_NO_DATA_FOUND_IN_CLIENT)
                .setValue(Boolean.toString(isCheckEnabled))
        );
    }
}
