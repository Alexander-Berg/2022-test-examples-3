package ru.yandex.market.logistics.lms.client.controller.yt;

import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.util.NestedServletException;

import ru.yandex.market.logistics.lms.client.utils.PartnerExternalParamsDataUtils;
import ru.yandex.market.logistics.lom.utils.YtLmsVersionsUtils;
import ru.yandex.market.logistics.lom.utils.YtUtils;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
@DisplayName("Получение информации о параметрах партнёров по списку типов параметров")
class LmsLomYtControllerGetPartnerExternalParamsTest extends LmsLomYtControllerAbstractTest {

    private static final String GET_PARAMS_PATH = "/lms/test-yt/partner/params";

    @Test
    @SneakyThrows
    @DisplayName("Успешное получение параметров партнёра")
    void successGetPartnerExternalParams() {
        mockYtParamsQueryResponse();

        getPartnerParams(PartnerExternalParamsDataUtils.PARTNER_EXTERNAL_PARAM_TYPES_SET)
            .andExpect(status().isOk())
            .andExpect(jsonContent("lms/client/controller/yt/partner_external_params.json"));

        verifyYtCalling();
    }

    @Test
    @SneakyThrows
    @DisplayName("Параметры не найдены")
    void paramsNotFound() {
        getPartnerParams(PartnerExternalParamsDataUtils.PARTNER_EXTERNAL_PARAM_TYPES_SET)
            .andExpect(status().isOk())
            .andExpect(noContent());

        verifyYtCalling();
    }

    @Test
    @SneakyThrows
    @DisplayName("Ошибка в yt")
    void ytErrorWhileGettingParams() {
        YtUtils.mockExceptionCallingYt(
            ytTables,
            PartnerExternalParamsDataUtils.GET_PARTNER_EXTERNAL_PARAMS_QUERY,
            new RuntimeException("Some yt exception")
        );

        softly.assertThatCode(
                () -> getPartnerParams(PartnerExternalParamsDataUtils.PARTNER_EXTERNAL_PARAM_TYPES_SET)
            )
            .isInstanceOf(NestedServletException.class)
            .hasMessage("Request processing failed; nested exception is java.lang.RuntimeException: Some yt exception");

        verifyYtCalling();
    }

    @Test
    @SneakyThrows
    @DisplayName("Вызов получения параметров с пустым типом параметров")
    void callingMethodWithEmptyTypesSet() {
        getPartnerParams(Set.of())
            .andExpect(status().isOk())
            .andExpect(jsonContent("lms/client/controller/yt/empty_array.json"));

        //чтобы verify на ytTables корректно отработал
        successGetPartnerExternalParams();
    }

    private void mockYtParamsQueryResponse() {
        YtUtils.mockSelectRowsFromYt(
            ytTables,
            PartnerExternalParamsDataUtils.partnerExternalParams(),
            PartnerExternalParamsDataUtils.GET_PARTNER_EXTERNAL_PARAMS_QUERY
        );
    }

    private void verifyYtCalling() {
        YtLmsVersionsUtils.verifyYtVersionTableInteractions(ytTables, lmsYtProperties);
        YtUtils.verifySelectRowsInteractions(
            ytTables,
            PartnerExternalParamsDataUtils.GET_PARTNER_EXTERNAL_PARAMS_QUERY
        );
    }

    @Nonnull
    @SneakyThrows
    private ResultActions getPartnerParams(Set<PartnerExternalParamType> types) {
        return mockMvc.perform(
            post(GET_PARAMS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(types))
        );
    }
}
