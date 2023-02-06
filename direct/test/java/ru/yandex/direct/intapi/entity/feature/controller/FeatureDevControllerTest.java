package ru.yandex.direct.intapi.entity.feature.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.util.NestedServletException;

import ru.yandex.direct.core.entity.feature.container.ChiefRepresentativeWithClientFeature;
import ru.yandex.direct.core.entity.feature.model.ClientFeature;
import ru.yandex.direct.core.entity.feature.model.FeatureState;
import ru.yandex.direct.core.entity.feature.service.FeatureManagingService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.intapi.IntApiException;
import ru.yandex.direct.intapi.entity.feature.model.GetClientsWithExplicitFeatureResponse;
import ru.yandex.direct.intapi.entity.feature.service.GetAccessToFeatureService;
import ru.yandex.direct.intapi.validation.kernel.ValidationResultConversionService;
import ru.yandex.direct.intapi.validation.model.IntapiError;
import ru.yandex.direct.intapi.validation.model.IntapiValidationResponse;
import ru.yandex.direct.intapi.validation.model.IntapiValidationResult;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.direct.validation.defect.CollectionDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FeatureDevControllerTest {

    private static final String CONTROLLER_URL_PATH = "/feature_dev/get_clients_with_explicit_feature";
    private static final String TEST_FEATURE_NAME = "test_feature";
    private static final String TYPO_FEATURE_NAME = "test_feature_with_typo";
    private static final String VALIDATION_EXCEPTION_MESSAGE = "some feature validation exception message";

    private FeatureManagingService mockFeatureManagingService;
    private ValidationResultConversionService validationResultConversionService;
    private FeatureDevController controller;

    @Before
    public void setUp() {
        mockFeatureManagingService = mock(FeatureManagingService.class);
        validationResultConversionService = mock(ValidationResultConversionService.class);
        GetAccessToFeatureService mockService = new GetAccessToFeatureService(
                null,
                validationResultConversionService,
                mockFeatureManagingService
        );
        FeatureService mockedFeatureService = mock(FeatureService.class);
        controller = new FeatureDevController(mockService, mockedFeatureService);
    }

    @Test
    public void getClientsWithExplicitFeature_Success() throws Exception {
        mockSuccessfulServiceResponse(List.of(432L, 765L, 432L));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        RequestBuilder requestBuilder = MockMvcRequestBuilders.get(CONTROLLER_URL_PATH)
                .param("feature_name", TEST_FEATURE_NAME);

        var expectedResponse = new GetClientsWithExplicitFeatureResponse().withResult(
                List.of(432L, 765L)
        );
        mvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().json(JsonUtils.toJson(expectedResponse)));
    }

    @Test
    public void getClientsWithExplicitFeature_TypoInFeatureName_Throws() {
        mockInvalidServiceResponse(List.of(TYPO_FEATURE_NAME));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        RequestBuilder requestBuilder = MockMvcRequestBuilders.get(CONTROLLER_URL_PATH)
                .param("feature_name", TYPO_FEATURE_NAME);

        Throwable thrown = catchThrowable(() -> mvc.perform(requestBuilder));
        assertThat(thrown).isInstanceOf(NestedServletException.class);
        assertThat(thrown.getCause()).isInstanceOf(IntApiException.class);
        assertThat(thrown.getCause().getMessage()).contains(VALIDATION_EXCEPTION_MESSAGE);
    }

    /**
     * @param clientIds Список clientId клиентов, которым доступна фича
     */
    private void mockSuccessfulServiceResponse(List<Long> clientIds) {
        when(mockFeatureManagingService.getFeaturesClients(
                ArgumentMatchers.eq(List.of(TEST_FEATURE_NAME)),
                ArgumentMatchers.eq(FeatureState.ENABLED))
        ).thenReturn(successfulGetFeaturesServiceResponse(clientIds));
    }

    private Result<Map<Long, List<ChiefRepresentativeWithClientFeature>>> successfulGetFeaturesServiceResponse(
            List<Long> clientIds
    ) {
        return Result.successful(Map.of(123L,
                clientIds.stream().map(
                        clientId -> new ChiefRepresentativeWithClientFeature()
                                .withClientFeature(new ClientFeature().withClientId(ClientId.fromLong(clientId)))
                ).collect(Collectors.toList())
        ));
    }

    private void mockInvalidServiceResponse(List<String> invalidFeatures) {
        when(mockFeatureManagingService.getFeaturesClients(
                ArgumentMatchers.any(),
                ArgumentMatchers.eq(FeatureState.ENABLED))
        ).thenReturn(invalidGetFeaturesServiceResponse(invalidFeatures));

        when(validationResultConversionService.buildValidationResponse(ArgumentMatchers.any(Result.class)))
                .thenReturn(new IntapiValidationResponse(new IntapiValidationResult()
                        .addErrors(new IntapiError().withText(VALIDATION_EXCEPTION_MESSAGE)))
                );
    }

    private Result<Map<Long, List<ChiefRepresentativeWithClientFeature>>> invalidGetFeaturesServiceResponse(
            List<String> invalidFeatures
    ) {
        ValidationResult<List<String>, Defect> vr = new ValidationResult<>(
                invalidFeatures, List.of(CollectionDefects.inCollection()), emptyList()
        );
        return Result.broken(vr);
    }
}
