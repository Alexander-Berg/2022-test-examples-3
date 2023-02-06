package ru.yandex.market.logistics.lms.client.controller.redis;

import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.util.NestedServletException;

import ru.yandex.market.logistics.lom.service.redis.AbstractRedisTest;
import ru.yandex.market.logistics.lom.service.redis.util.RedisKeys;
import ru.yandex.market.logistics.lom.service.yt.dto.YtPartnerRelation;
import ru.yandex.market.logistics.lom.service.yt.dto.YtPartnerRelationTo;
import ru.yandex.market.logistics.lom.utils.jobs.executor.RedisFromYtMigrationTestUtils;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.lom.utils.jobs.executor.RedisFromYtMigrationTestUtils.buildPartnerRelation;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

@ParametersAreNonnullByDefault
@DisplayName("Ручки для тестирования работы редиса с методами по получению связок партнеров")
class LmsLomRedisControllerPartnerRelationTest extends AbstractRedisTest {

    private static final String GET_PARTNER_RELATION_WITH_CUTOFFS_BY_FILTER_PATH =
        "/lms/test-redis/partner-relation/cutoffs/get-by-filter";

    private static final String GET_PARTNER_RELATION_WITH_RETURN_PARTNERS_BY_FILTER_PATH =
        "/lms/test-redis/partner-relation/return-partners/get-by-filter";

    private static final long PARTNER_FROM_ID = 1L;
    private static final long PARTNER_TO_ID = 2L;

    @Test
    @SneakyThrows
    @DisplayName("Получение связки партнеров с катоффами по фильтру")
    void getRelationWithCutoffsByFilter() {
        PartnerRelationFilter filter = PartnerRelationFilter.newBuilder()
            .fromPartnersIds(Set.of(PARTNER_FROM_ID))
            .toPartnersIds(Set.of(PARTNER_TO_ID))
            .build();
        doReturn(
            redisObjectConverter.serializeToString(buildPartnerRelation(PARTNER_FROM_ID))
        )
            .when(clientJedis).hget(eq(getRelationHashTableName()), any());

        sendPartnerRelationWithCutoffsSearchRequest(filter)
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "lms/client/controller/partner_relation_with_cutoffs_by_filter_response.json",
                false
            ));

        verify(clientJedis).hget(getRelationHashTableName(), String.format("%s:%s", PARTNER_FROM_ID, PARTNER_TO_ID));
        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
    }

    @Test
    @SneakyThrows
    @DisplayName("Получение связки партнеров с катоффами по фильтру: связка не найдена")
    void getRelationWithCutoffsByFilterNoRelationFound() {
        long partnerFrom = 1L;
        PartnerRelationFilter filter = PartnerRelationFilter.newBuilder()
            .fromPartnersIds(Set.of(partnerFrom))
            .toPartnersIds(Set.of(partnerFrom + 1))
            .build();

        sendPartnerRelationWithCutoffsSearchRequest(filter)
            .andExpect(status().isOk())
            .andExpect(content().string("[]"));

        verify(clientJedis).hget(getRelationHashTableName(), String.format("%s:%s", PARTNER_FROM_ID, PARTNER_TO_ID));
        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
    }

    @Test
    @SneakyThrows
    @DisplayName("Получение связки партнеров с катоффами по пустому фильтру")
    void getRelationWithCutoffsByEmptyFilter() {
        PartnerRelationFilter filter = PartnerRelationFilter.newBuilder().build();

        softly.assertThatCode(
                () -> sendPartnerRelationWithCutoffsSearchRequest(filter)
            )
            .isInstanceOf(NestedServletException.class)
            .hasMessage("Request processing failed; nested exception is java.lang.IllegalArgumentException: "
                + "Filter must contain exactly one 'fromPartnerId' and 'toPartnerId'");
    }

    @Test
    @SneakyThrows
    @DisplayName("Получение связок с возвратными партнерами по фильтру")
    void getRelationsWithReturnPartnersByFilter() {
        Set<Long> partnerFromIds = Set.of(1L, 2L, 3L);

        PartnerRelationFilter filter = PartnerRelationFilter.newBuilder()
            .fromPartnersIds(partnerFromIds)
            .build();
        doReturn(
            partnerFromIds.stream()
                .map(RedisFromYtMigrationTestUtils::buildPartnerRelationTo)
                .map(redisObjectConverter::serializeToString)
                .collect(Collectors.toList())
        )
            .when(clientJedis).hmget(eq(getRelationToHashTableName()), any());

        sendPartnerRelationWithReturnPartnerSearchRequest(filter)
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "lms/client/controller/partner_relation_with_return_partners_by_filter_response.json",
                false
            ));

        verifyJedisMultiGetRelationTo(partnerFromIds);
        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
    }

    @Test
    @SneakyThrows
    @DisplayName("Получение связок с возвратными партнерами по фильтру: связки не найдены")
    void getRelationsWithReturnPartnersByFilterNoRelationFound() {
        Set<Long> partnerFromIds = Set.of(1L, 2L, 3L);

        PartnerRelationFilter filter = PartnerRelationFilter.newBuilder()
            .fromPartnersIds(partnerFromIds)
            .build();

        sendPartnerRelationWithReturnPartnerSearchRequest(filter)
            .andExpect(status().isOk())
            .andExpect(content().string("[]"));

        verifyJedisMultiGetRelationTo(partnerFromIds);
        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
    }

    @Test
    @SneakyThrows
    @DisplayName("Получение связок с возвратными партнерами по пустому фильтру")
    void getRelationsWithReturnPartnersByEmptyFilter() {
        PartnerRelationFilter filter = PartnerRelationFilter.newBuilder().build();

        softly.assertThatCode(() -> sendPartnerRelationWithReturnPartnerSearchRequest(filter))
            .isInstanceOf(NestedServletException.class)
            .hasMessage("Request processing failed; nested exception is java.lang.IllegalArgumentException: "
                + "Field 'fromPartnerIds' in filter must not be null");
    }

    @Nonnull
    @SneakyThrows
    private ResultActions sendPartnerRelationWithCutoffsSearchRequest(PartnerRelationFilter filter) {
        return performPostRequest(GET_PARTNER_RELATION_WITH_CUTOFFS_BY_FILTER_PATH, filter);
    }

    @Nonnull
    @SneakyThrows
    private ResultActions sendPartnerRelationWithReturnPartnerSearchRequest(PartnerRelationFilter filter) {
        return performPostRequest(GET_PARTNER_RELATION_WITH_RETURN_PARTNERS_BY_FILTER_PATH, filter);
    }

    @Nonnull
    @SneakyThrows
    private ResultActions performPostRequest(String url, PartnerRelationFilter filter) {
        return mockMvc.perform(
            post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(redisObjectConverter.serializeToString(filter))
        );
    }

    private void verifyJedisMultiGetRelationTo(Set<Long> partnerFromIds) {
        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);

        verify(clientJedis).hmget(eq(getRelationToHashTableName()), argumentCaptor.capture());

        softly.assertThat(argumentCaptor.getAllValues())
            .containsExactlyInAnyOrderElementsOf(
                partnerFromIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList())
            );
    }

    @Nonnull
    private String getRelationHashTableName() {
        return RedisKeys.getHashTableFromYtName(YtPartnerRelation.class, RedisKeys.REDIS_DEFAULT_VERSION);
    }

    @Nonnull
    private String getRelationToHashTableName() {
        return RedisKeys.getHashTableFromYtName(YtPartnerRelationTo.class, RedisKeys.REDIS_DEFAULT_VERSION);
    }
}
