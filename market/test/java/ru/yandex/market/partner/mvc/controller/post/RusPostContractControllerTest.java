package ru.yandex.market.partner.mvc.controller.post;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import feign.FeignException;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.post.RusPostAuthClient;
import ru.yandex.market.core.post.RusPostContractClient;
import ru.yandex.market.core.post.model.dto.AccessTokenDto;
import ru.yandex.market.core.post.model.dto.ContractOfferExistence;
import ru.yandex.market.core.post.model.dto.ContractOfferSubmitErrorDto;
import ru.yandex.market.core.post.model.dto.ContractOfferSubmitResultDto;
import ru.yandex.market.core.post.model.dto.PostContractOfferDto;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.partner.test.context.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.test.util.JsonTestUtil.getJsonHttpEntity;
import static ru.yandex.market.common.test.util.StringTestUtil.getString;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;
import static ru.yandex.market.partner.util.FunctionalTestHelper.delete;
import static ru.yandex.market.partner.util.FunctionalTestHelper.get;
import static ru.yandex.market.partner.util.FunctionalTestHelper.post;

/**
 * Тесты на {@link RusPostContractController}.
 */
@DbUnitDataSet(before = "/mvc/post/database/RusPostControllerTest.before.csv")
class RusPostContractControllerTest extends FunctionalTest {

    private static final String ACCESS_TOKEN = "THIS_IS_ACCESS_TOKEN";
    private static final String CLIENT_ID = "IG9sVCeSFNagBG0nVmd2cpexIssa";
    private static final String CLIENT_SECRET = "secret";
    private static final String REDIRECT_URL =
            "https%3A%2F%2Fpartner-front--marketpartner-14304-rupost.demofslb.market.yandex.ru%2Fredirect%2Frupost";

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private RusPostAuthClient rusPostAuthClient;

    @Autowired
    private RusPostContractClient rusPostContractClient;

    @Autowired
    private Clock clock;

    private static ContractOfferSubmitResultDto createSuccessfulSubmitResult() {
        return new ContractOfferSubmitResultDto(null);
    }

    private static ContractOfferSubmitResultDto createErroneousSubmitResult() {
        var error1 = new ContractOfferSubmitErrorDto(
                "orgOgrn",
                "APPLICATION",
                null,
                "Длина ОГРН не должна быть более 13 символов",
                0,
                ContractOfferSubmitErrorDto.ErrorType.MAX_LENGTH
        );
        var error2 = new ContractOfferSubmitErrorDto(
                "signerRightProxyNumber",
                "APPLICATION",
                null,
                "Номер и дата выдачи доверенности не заданы",
                1,
                ContractOfferSubmitErrorDto.ErrorType.REQUIRED
        );

        return new ContractOfferSubmitResultDto(List.of(error1, error2));
    }

    private static ContractOfferSubmitResultDto createSubmittedTwiceOfferResponse() {
        var error1 = new ContractOfferSubmitErrorDto(
                "general",
                "APPLICATION",
                null,
                "Заявление уже сохранено",
                0,
                ContractOfferSubmitErrorDto.ErrorType.EXISTED
        );

        return new ContractOfferSubmitResultDto(Collections.singletonList(error1));
    }

    static Stream<Arguments> validationTestArgumentsValid() {
        return Stream.of(
                Arguments.of("/mvc/post/RusPostControllerTest.testValidationContractOffers.forIP.valid.request.json", null),
                Arguments.of("/mvc/post/RusPostControllerTest.testValidationContractOffers.forOOO.valid.request.json", null));
    }

    static Stream<Arguments> validationTestArgumentsInvalid() {
        return Stream.of(
                Arguments.of("/mvc/post/RusPostControllerTest.testValidationContractOffers.forType.invalid.request.json", "/mvc/post/RusPostControllerTest.testValidationContractOffers.forType.invalid.response.json"),
                Arguments.of("/mvc/post/RusPostControllerTest.testValidationContractOffers.forIP.invalid.request.json", "/mvc/post/RusPostControllerTest.testValidationContractOffers.forIP.invalid.response.json"),
                Arguments.of("/mvc/post/RusPostControllerTest.testValidationContractOffers.forOOO.invalid.request.json", "/mvc/post/RusPostControllerTest.testValidationContractOffers.forOOO.invalid.response.json"),
                Arguments.of("/mvc/post/RusPostControllerTest.testValidationContractOffers.emptyContractForm.request.json", "/mvc/post/RusPostControllerTest.testValidationContractOffers.emptyContractForm.response.json"),
                Arguments.of("/mvc/post/RusPostControllerTest.testValidationContractOffers.forOOO.invalid.withSignerProxy.request.json", "/mvc/post/RusPostControllerTest.testValidationContractOffers.forOOO.invalid.withSignerProxy.response.json"),
                Arguments.of("/mvc/post/RusPostControllerTest.testValidationContractOffers.forIP.invalid.postCode.request.json", "/mvc/post/RusPostControllerTest.testValidationContractOffers.forIP.invalid.postCode.response.json")
        );
    }

    static Stream<Arguments> submitContractOfferArguments() {
        return Stream.of(
                Arguments.of("SuccessfulSubmitResult", createSuccessfulSubmitResult()),
                Arguments.of("SubmittedTwiceOfferResponse", createSubmittedTwiceOfferResponse())
        );
    }

    static Stream<Arguments> contractDataTestArguments() {
        return Stream.of(
                Arguments.of("1", "/mvc/post/RusPostControllerTest.getContractData.signedViaMarket.json"),
                Arguments.of("2", "/mvc/post/RusPostControllerTest.getContractData.noContract.json"),
                Arguments.of("3", "/mvc/post/RusPostControllerTest.getContractData.signedViaPost.json"),
                Arguments.of("4", "/mvc/post/RusPostControllerTest.getContractData.noContract.json"),
                Arguments.of("5", "/mvc/post/RusPostControllerTest.getContractData.noContract.json"),
                Arguments.of("6", "/mvc/post/RusPostControllerTest.getContractData.contractWithErrors.json")
        );
    }

    @Nonnull
    private static AccessTokenDto createRefreshedToken(AccessTokenDto token) {
        var refreshedToken = new AccessTokenDto();
        refreshedToken.setAccessToken("itsanewaccesstoken");
        refreshedToken.setRefreshToken("itsanewrefreshedtoken");
        refreshedToken.setExpiresIn(Instant.now().getEpochSecond());
        refreshedToken.setIdToken(token.getIdToken());
        refreshedToken.setScope(token.getScope());
        refreshedToken.setTokenType(token.getTokenType());
        return refreshedToken;
    }

    @BeforeEach
    void setUpMocks() {
        environmentService.setValues("auto.calculated.post.carriers", List.of("138", "139"));
    }

    /**
     * Проверка работы ручки /campaigns/{campaignId}/ruspost/contract-data.
     *
     * @param campaignId идентификатор камппании, информацию о сотрудничестве которой с почтой мы извлекаем
     * @param responseFile файл, содержащий результат вызова ручки
     */
    @DbUnitDataSet(before = "/mvc/post/database/RusPostControllerTest.getContractData.before.csv")
    @MethodSource("contractDataTestArguments")
    @ParameterizedTest
    void testGetContractData(String campaignId, String responseFile) {
        var url = String.format("%s/campaigns/%s/ruspost/contract-data", baseUrl, campaignId);

        JsonTestUtil.assertEquals(get(url), getClass(), responseFile);
    }

    /**
     * Проверка валидации в ручке /ruspost/contractoffer/form: валидные результаты.
     *
     * @param requestPath - файл, содержащий запрос, содержащий информацию о заполненной пользователем форме договора
     * офферты
     */
    @MethodSource("validationTestArgumentsValid")
    @ParameterizedTest
    void testValidationOfContractOfferFormDataValid(String requestPath) {
        var result = post(contractOfferFormUrl(), getJsonHttpEntity(getClass(), requestPath));
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    /**
     * Проверка валидации в ручке /ruspost/contractoffer/form: невалидные результаты.
     *
     * @param requestPath файл, содержащий запрос, содержащий информацию о заполненной пользователем форме договора
     * офферты
     * @param responsePath файл, содержащий ответ, содержащий информацию о ошибках в форме договора офферты
     */
    @MethodSource("validationTestArgumentsInvalid")
    @ParameterizedTest
    void testValidationOfContractOfferFormDataInvalid(String requestPath, String responsePath) {
        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> post(contractOfferFormUrl(), getJsonHttpEntity(getClass(), requestPath)))
                .satisfies(ex -> JsonTestUtil.assertResponseErrorMessage(ex, getClass(), responsePath));
    }

    /**
     * Проверяет сохранение формы договора оферты с Почтой Росии в базе данных.
     */
    @Test
    @DbUnitDataSet(after = "/mvc/post/database/RusPostControllerTest.postContractOfferForm.after.csv")
    void postContractOfferForm() {
        var request = getJsonHttpEntity(
                getClass(),
                "/mvc/post/RusPostControllerTest.postContractOfferForm.request.json"
        );
        var response = post(contractOfferFormUrl(), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    /**
     * Проверяет получение статуса 404 Not Found, если индекс ОПС не найден в БД.
     */
    @Test
    void postContractOfferForm_NotFound() {
        var request = getJsonHttpEntity(
                getClass(),
                "/mvc/post/RusPostControllerTest.postContractOfferForm.notFound.request.json"
        );
        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> post(contractOfferFormUrl(), request))
                .satisfies(ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    JsonTestUtil.assertResponseErrorMessage(ex, "[{\"code\":\"BAD_PARAM\"," +
                            "\"details\":{\"entity_name\":\"shippingPoint\",\"subcode\":\"ENTITY_NOT_FOUND\"," +
                            "\"entity_id\":\"555555\"}}]");
                });
    }

    @DbUnitDataSet(before = "/mvc/post/database/RusPostControllerTest.submitRusPostContractOffer.before.csv",
            after = "/mvc/post/database/RusPostControllerTest.submitRusPostContractOffer.successful.after.csv")
    @MethodSource("submitContractOfferArguments")
    @ParameterizedTest(name = "{0}")
    void submitRusPostContractOffer_successfulSubmit(String desc, ContractOfferSubmitResultDto submitResult) {
        // Given
        mockPostClientsResponses(createTestAccessTokenResponse(), submitResult);

        // When
        post(contractOfferUrl(100, "741425ef-3c84-41f3-b968-127835b56cb8"));

        // Then
        verify(rusPostAuthClient).getAccessToken(
                eq(CLIENT_ID),
                eq(CLIENT_SECRET),
                eq(REDIRECT_URL),
                eq("741425ef-3c84-41f3-b968-127835b56cb8")
        );
    }

    @DbUnitDataSet(before = "/mvc/post/database/RusPostControllerTest.submitRusPostContractOffer.reused.accessToken.before.csv",
            after = "/mvc/post/database/RusPostControllerTest.submitRusPostContractOffer.reused.accessToken.after.csv")
    @MethodSource("submitContractOfferArguments")
    @ParameterizedTest(name = "{0}")
    void submitRusPostContractOffer_successfulSubmit_reuseStoredAccessToken(
            String desc,
            ContractOfferSubmitResultDto submitResult
    ) {
        // Given
        mockPostClientsResponses(createTestAccessTokenResponse(), submitResult);

        // When
        post(contractOfferUrl(100));

        // Then
        verify(rusPostAuthClient, never()).getAccessToken(anyString(), anyString(), anyString(), anyString());
        verify(rusPostAuthClient, never()).refreshToken(anyString(), anyString(), anyString());
    }

    @DbUnitDataSet(before = "/mvc/post/database/RusPostControllerTest.submitRusPostContractOffer.refresh.accessToken.before.csv",
            after = "/mvc/post/database/RusPostControllerTest.submitRusPostContractOffer.refresh.accessToken.after.csv")
    @Test
    void submitRusPostContractOffer_successfulSubmit_refreshStoredAccessToken() {
        // Given
        var accessToken = createTestAccessTokenResponse();
        mockPostClientsResponses(accessToken, createSuccessfulSubmitResult());

        // When
        post(contractOfferUrl(100, "741425ef-3c84-41f3-b968-127835b56cb8"));

        // Then
        verify(rusPostAuthClient, only()).refreshToken(anyString(), anyString(), eq(accessToken.getRefreshToken()));
    }

    @Test
    @DbUnitDataSet(before = "/mvc/post/database/RusPostControllerTest.submitRusPostContractOffer.before.csv",
            after = "/mvc/post/database/RusPostControllerTest.submitRusPostContractOffer.failed.validation.after.csv")
    void submitRusPostContractOffer_submitFailed() {
        // Given
        mockPostClientsResponses(createTestAccessTokenResponse(), createErroneousSubmitResult());

        // When
        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> post(contractOfferUrl(100, "1233")))
                .satisfies(ex -> JsonTestUtil.assertResponseErrorMessage(
                        getString(
                                getClass(),
                                "/mvc/post/RusPostControllerTest.submitOffer.failedValidation.json"
                        ),
                        new String(ex.getResponseBodyAsByteArray(), StandardCharsets.UTF_8)
                ));
    }

    @Test
    @DbUnitDataSet(before = "/mvc/post/database/RusPostControllerTest.submitRusPostContractOffer.before.csv",
            after = "/mvc/post/database/RusPostControllerTest.submitRusPostContractOffer.failed.submit.after.csv")
    void submitRusPostContractOffer_submitFailedWithError() {
        // Given
        mockAuthClientResponses(createTestAccessTokenResponse());
        when(rusPostContractClient.submitOffer(any(PostContractOfferDto.class), anyString(), anyString()))
                .thenThrow(new FeignException.GatewayTimeout("timeout", ArrayUtils.EMPTY_BYTE_ARRAY));

        // When
        assertThatExceptionOfType(HttpServerErrorException.class)
                .isThrownBy(() -> post(contractOfferUrl(100, "1233")))
                .satisfies(ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    @DbUnitDataSet(before = "/mvc/post/database/RusPostControllerTest.submitRusPostContractOffer.withOldUserId.before.csv",
            after = "/mvc/post/database/RusPostControllerTest.submitRusPostContractOffer.successful.after.csv")
    @DisplayName("Проверка, что успешный сабмит формы перетирает user_id от неуспешного сабмита")
    void submitRusPostContractOfferRefreshUserId() {
        mockPostClientsResponses(createTestAccessTokenResponse(), createSuccessfulSubmitResult());
        post(contractOfferUrl(100, "741425ef-3c84-41f3-b968-127835b56cb8"));
    }

    @Test
    @DisplayName("Неудачная проверка существования заполненной формы, т.к. клиент не был аутентифицирован в почте")
    void checkContractOfferExistenceAuthenticationErrorNoToken() {
        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> get(contractOfferFormExistenceUrl(100, 100)))
                .satisfies(ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    JsonTestUtil.assertResponseErrorMessage(ex, "[{\"code\":\"UNAUTHORIZED\"," +
                            "\"details\":{\"reason\":\"RusPost token for (shopId: 1000, uid: 100) not found\"," +
                            "\"subcode\":\"ENTITY_NOT_FOUND\"}}]");
                });

    }

    @Test
    @DisplayName("Неудачная проверка существования заполненной формы, т.к. access token заэкспайрился")
    @DbUnitDataSet(before = "/mvc/post/database/RusPostControllerTest.checkContractOfferExistence.before.csv")
    void checkContractOfferExistenceAuthenticationError() {
        when(rusPostContractClient.checkOfferExistence(eq(ACCESS_TOKEN), anyString()))
                .thenThrow(new RuntimeException());
        when(rusPostAuthClient.getAccessToken(eq(ACCESS_TOKEN), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException());
        when(rusPostAuthClient.refreshToken(anyString(), anyString(), anyString()))
                .thenThrow(new FeignException.BadRequest("Bad Request",
                        "{\"error\":\"invalid_grant\",\"error_description\":\"1405 Refresh token expired\"}".getBytes(StandardCharsets.UTF_8)));
        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> get(contractOfferFormExistenceUrl(100, 51)))
                .satisfies(ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    JsonTestUtil.assertResponseErrorMessage(ex, "[{\"code\":\"UNAUTHORIZED\"," +
                            "\"details\":{\"reason\":\"Cannot get access token: refresh token is expired or invalid\"}}]");
                });

    }

    @Test
    @DisplayName("Успешная попытка проверки существования заполненной формы")
    @DbUnitDataSet(before = "/mvc/post/database/RusPostControllerTest.checkContractOfferExistence.before.csv")
    void checkContractOfferExistence() {
        doReturn(Instant.parse("2020-06-01T01:01:01Z")).when(clock).instant();
        when(rusPostContractClient.checkOfferExistence(eq(ACCESS_TOKEN), anyString())).thenReturn(
                new ContractOfferExistence(true, "additional message", ContractOfferExistence.SystemType.DOGOVOR));
        JsonTestUtil.assertEquals(get(contractOfferFormExistenceUrl(100, 50)), getClass(),
                "/mvc/post/RusPostControllerTest.checkContractOfferExistence.response.json");
    }

    @Test
    @DisplayName("Успешная попытка проверки существования контракта: контракта в почте нет")
    @DbUnitDataSet(before = "/mvc/post/database/RusPostControllerTest.checkContractOfferExistence.before.csv")
    void checkContractOfferExistenceNoContract() {
        doReturn(Instant.parse("2020-06-01T01:01:01Z")).when(clock).instant();
        when(rusPostContractClient.checkOfferExistence(eq(ACCESS_TOKEN), anyString())).thenReturn(
                new ContractOfferExistence(false, null, null));
        JsonTestUtil.assertEquals(get(contractOfferFormExistenceUrl(100, 50)), "{\"exists\": false}");
    }

    @Test
    @DisplayName("Успешно пометили магазин как имеющий контракт через Почту напрямую")
    @DbUnitDataSet(before = "/mvc/post/database/RusPostControllerTest.markContractExistence.before.csv",
            after = "/mvc/post/database/RusPostControllerTest.markContractExistence.markSuccessful.after.csv")
    void markContractOfferExistence() {
        post(contractExistsUrl(100));

        verifySentNotificationType(partnerNotificationClient, 1, 1596450039L);
    }

    @Test
    @DisplayName("Неуспешно пометили магазин как имеющий контракт через Почту напрямую: уже есть маркетный контракт")
    @DbUnitDataSet(before = "/mvc/post/database/RusPostControllerTest.markContractExistence.before.csv",
            after = "/mvc/post/database/RusPostControllerTest.markContractExistence.after.csv")
    void markContractOfferExistenceFailed() {
        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> post(contractExistsUrl(101)))
                .satisfies(ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("Успешно удалили контракт магазина")
    @DbUnitDataSet(before = "/mvc/post/database/RusPostControllerTest.deleteContract.before.csv",
            after = "/mvc/post/database/RusPostControllerTest.deleteContractSuccessful.after.csv")
    void deleteContract() {
        delete(contractOfferUrl(102, null));
        delete(contractOfferUrl(103, null));
        delete(contractOfferUrl(104, null));
    }

    @Test
    @DisplayName("Неуспешно удалили контракт магазина: у магазина нет контрактов")
    @DbUnitDataSet(before = "/mvc/post/database/RusPostControllerTest.deleteContract.before.csv",
            after = "/mvc/post/database/RusPostControllerTest.deleteContract.after.csv")
    void deleteContractFailedNoContract() {
        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> delete(contractOfferUrl(100, null)))
                .satisfies(ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("Неуспешно удалили контракт магазина: у магазина есть контракт в запрещенном статусе")
    @DbUnitDataSet(before = "/mvc/post/database/RusPostControllerTest.deleteContract.before.csv",
            after = "/mvc/post/database/RusPostControllerTest.deleteContract.after.csv")
    void deleteContractFailedNotAllowedStatus() {
        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> delete(contractOfferUrl(101, null)))
                .satisfies(ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private String contractOfferUrl(long campaignId, String code) {
        return partnersUrl(String.format("contractoffer?id=%d&_user_id=123&redirect_url=%s&code=%s", campaignId, REDIRECT_URL, code));
    }

    private String contractOfferUrl(long campaignId) {
        return partnersUrl(String.format("contractoffer?id=%d&_user_id=123", campaignId));
    }

    private String contractOfferFormExistenceUrl(long campaignId, long userId) {
        return partnersUrl(String.format("contractoffer/check-existence?id=%d&_user_id=%d", campaignId, userId));
    }

    private String contractExistsUrl(long campaignId) {
        return partnersUrl(String.format("contractoffer/contract-exists?id=%d&_user_id=4321", campaignId));
    }

    private String contractOfferFormUrl() {
        return partnersUrl("contractoffer/form?id=100&_user_id=100100");
    }

    private void mockPostClientsResponses(AccessTokenDto token, ContractOfferSubmitResultDto contractSubmitResult) {
        var refreshedToken = mockAuthClientResponses(token);

        doReturn(contractSubmitResult).when(rusPostContractClient)
                .submitOffer(any(PostContractOfferDto.class), eq(ACCESS_TOKEN), anyString());

        doReturn(contractSubmitResult).when(rusPostContractClient)
                .submitOffer(any(PostContractOfferDto.class), eq(refreshedToken.getAccessToken()), anyString());
    }

    @Nonnull
    private AccessTokenDto mockAuthClientResponses(AccessTokenDto token) {
        doReturn(token).when(rusPostAuthClient)
                .getAccessToken(eq(CLIENT_ID), eq(CLIENT_SECRET), any(String.class), any(String.class));

        var refreshedToken = createRefreshedToken(token);
        doReturn(refreshedToken).when(rusPostAuthClient)
                .refreshToken(eq(CLIENT_ID), eq(CLIENT_SECRET), eq(token.getRefreshToken()));

        return refreshedToken;
    }

    @Nonnull
    private static AccessTokenDto createTestAccessTokenResponse() {
        var accessToken = new AccessTokenDto();
        accessToken.setAccessToken(ACCESS_TOKEN);
        accessToken.setRefreshToken("some_brilliant_refresh_token");
        accessToken.setScope("openid");
        accessToken.setIdToken("very_excellent_id_token");
        accessToken.setTokenType("Bearer");
        accessToken.setExpiresIn(1800);
        return accessToken;
    }

    private String partnersUrl(String path) {
        return String.format("%s/ruspost/%s", baseUrl, path);
    }
}
