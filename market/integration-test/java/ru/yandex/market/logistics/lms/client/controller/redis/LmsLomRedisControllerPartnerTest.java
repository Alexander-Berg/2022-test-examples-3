package ru.yandex.market.logistics.lms.client.controller.redis;

import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.util.NestedServletException;

import ru.yandex.market.logistics.lom.service.redis.AbstractRedisTest;
import ru.yandex.market.logistics.lom.service.redis.util.RedisKeys;
import ru.yandex.market.logistics.lom.service.yt.dto.YtPartner;
import ru.yandex.market.logistics.lom.service.yt.dto.YtPartnerExternalParam;
import ru.yandex.market.logistics.lom.utils.jobs.executor.RedisFromYtMigrationTestUtils;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.lom.utils.jobs.executor.RedisFromYtMigrationTestUtils.buildExternalParams;
import static ru.yandex.market.logistics.lom.utils.jobs.executor.RedisFromYtMigrationTestUtils.buildPartner;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

@DisplayName("Ручки для тестирования работы редиса с методами партнёров")
class LmsLomRedisControllerPartnerTest extends AbstractRedisTest {

    private static final String PARTNER_ID = "1";
    protected static final String REDIS_ACTUAL_VERSION = "0";
    private static final Set<Long> PARTNER_IDS = Set.of(1L, 2L, 3L);
    private static final String GET_PARTNER_BY_ID_PATH = "/lms/test-redis/partner/get/" + PARTNER_ID;
    private static final String GET_PARTNERS_BY_IDS_PATH = "/lms/test-redis/partner/get-by-ids";
    private static final String GET_PARTNER_PARAMS_BY_TYPES_PATH = "/lms/test-redis/partner/params";

    @Override
    @AfterEach
    public void tearDown() {
        verify(clientJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        super.tearDown();
    }

    @Test
    @SneakyThrows
    @DisplayName("Успешное получение партнёра")
    void getPartnerSuccess() {
        long partnerId = Long.parseLong(PARTNER_ID);
        doReturn(redisObjectConverter.serializeToString(buildPartner(partnerId)))
            .when(clientJedis).hget(getPartnerHashTableName(), PARTNER_ID);

        mockMvc.perform(get(GET_PARTNER_BY_ID_PATH))
            .andExpect(status().isOk())
            .andExpect(jsonContent("lms/client/controller/partner_by_id_response.json"));

        verifyGetPartner();
    }

    @Test
    @SneakyThrows
    @DisplayName("Ошибка в редисе при получении партнёра")
    void getPartnerError() {
        doThrow(new RuntimeException())
            .when(clientJedis).hget(getPartnerHashTableName(), PARTNER_ID);

        softly.assertThatCode(() -> mockMvc.perform(get(GET_PARTNER_BY_ID_PATH)))
            .isInstanceOf(NestedServletException.class)
            .hasMessage(
                "Request processing failed; nested exception is java.lang.RuntimeException: "
                    + "java.lang.RuntimeException: Connection retries to redis limit exceeded"
            );

        verifyGetPartner();
    }

    @Test
    @SneakyThrows
    @DisplayName("Получение несуществующего партнёра")
    void getNotExistingPartner() {
        mockMvc.perform(get(GET_PARTNER_BY_ID_PATH))
            .andExpect(status().isOk())
            .andExpect(content().string(""));

        verifyGetPartner();
    }

    @Test
    @SneakyThrows
    @DisplayName("Успешное получение нескольких партнёров")
    void getPartnersSuccess() {
        doReturn(
            PARTNER_IDS.stream()
                .map(RedisFromYtMigrationTestUtils::buildPartner)
                .map(redisObjectConverter::serializeToString)
                .collect(Collectors.toList())
        )
            .when(clientJedis).hmget(eq(getPartnerHashTableName()), any());

        mockMvc.perform(
                post(GET_PARTNERS_BY_IDS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(redisObjectConverter.serializeToString(PARTNER_IDS))
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent("lms/client/controller/partners_by_ids_response.json", false));

        verifyGetPartners();
    }

    @Test
    @SneakyThrows
    @DisplayName("Ошибка в редисе при получении нескольких партнёров")
    void getPartnersError() {
        doThrow(new RuntimeException())
            .when(clientJedis).hmget(eq(getPartnerHashTableName()), any());

        softly.assertThatCode(() -> mockMvc.perform(
                post(GET_PARTNERS_BY_IDS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(redisObjectConverter.serializeToString(PARTNER_IDS))
            ))
            .isInstanceOf(NestedServletException.class)
            .hasMessage(
                "Request processing failed; nested exception is java.lang.RuntimeException: "
                    + "java.lang.RuntimeException: Connection retries to redis limit exceeded"
            );

        verifyGetPartners();
    }

    @Test
    @SneakyThrows
    @DisplayName("Получение несуществующих партнёров")
    void getNotExistingPartners() {
        mockMvc.perform(
                post(GET_PARTNERS_BY_IDS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(redisObjectConverter.serializeToString(PARTNER_IDS))
            )
            .andExpect(status().isOk())
            .andExpect(content().string("[]"));

        verifyGetPartners();
    }

    @Test
    @SneakyThrows
    @DisplayName("Получение параметров партнёра по типам")
    void getPartnerParamsByTypes() {
        Set<PartnerExternalParamType> types = Set.of(
            PartnerExternalParamType.DISABLE_AUTO_CANCEL_AFTER_SLA,
            PartnerExternalParamType.CAN_UPDATE_SHIPMENT_DATE,
            PartnerExternalParamType.VIRTUAL_PARTNER
        );

        doReturn(redisObjectConverter.serializeToString(
            buildExternalParams(PartnerExternalParamType.DISABLE_AUTO_CANCEL_AFTER_SLA.name()))
        )
            .when(clientJedis).hget(
                getPartnerExternalParamHashTableName(),
                PartnerExternalParamType.DISABLE_AUTO_CANCEL_AFTER_SLA.name()
            );
        doReturn(redisObjectConverter.serializeToString(
            buildExternalParams(PartnerExternalParamType.CAN_UPDATE_SHIPMENT_DATE.name()))
        )
            .when(clientJedis).hget(
                getPartnerExternalParamHashTableName(),
                PartnerExternalParamType.CAN_UPDATE_SHIPMENT_DATE.name()
            );

        mockMvc.perform(
                post(GET_PARTNER_PARAMS_BY_TYPES_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(redisObjectConverter.serializeToString(types))
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent("lms/client/controller/params_response.json", false));

        verify(clientJedis).hget(
            getPartnerExternalParamHashTableName(),
            PartnerExternalParamType.DISABLE_AUTO_CANCEL_AFTER_SLA.name()
        );
        verify(clientJedis).hget(
            getPartnerExternalParamHashTableName(),
            PartnerExternalParamType.CAN_UPDATE_SHIPMENT_DATE.name()
        );
        verify(clientJedis).hget(
            getPartnerExternalParamHashTableName(),
            PartnerExternalParamType.VIRTUAL_PARTNER.name()
        );
    }

    @Test
    @SneakyThrows
    @DisplayName("Получение параметров партнёра: параметров с заданными типами нет")
    void getPartnerParamsNoParamsFound() {
        Set<PartnerExternalParamType> types = Set.of(PartnerExternalParamType.DISABLE_AUTO_CANCEL_AFTER_SLA);

        mockMvc.perform(
                post(GET_PARTNER_PARAMS_BY_TYPES_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(redisObjectConverter.serializeToString(types))
            )
            .andExpect(status().isOk())
            .andExpect(content().string("[]"));

        verify(clientJedis).hget(
            getPartnerExternalParamHashTableName(),
            PartnerExternalParamType.DISABLE_AUTO_CANCEL_AFTER_SLA.name()
        );
    }

    @Nonnull
    private String getPartnerHashTableName() {
        return RedisKeys.getHashTableFromYtName(YtPartner.class, RedisKeys.REDIS_DEFAULT_VERSION);
    }

    @Nonnull
    private String getPartnerExternalParamHashTableName() {
        return RedisKeys.getHashTableFromYtName(
            YtPartnerExternalParam.class,
            RedisKeys.REDIS_DEFAULT_VERSION
        );
    }

    private void verifyGetPartner() {
        verify(clientJedis).hget(getPartnerHashTableName(), PARTNER_ID);
    }

    private void verifyGetPartners() {
        verifyMultiGetArguments(getPartnerHashTableName(), Set.of("1", "2", "3"));
    }
}
