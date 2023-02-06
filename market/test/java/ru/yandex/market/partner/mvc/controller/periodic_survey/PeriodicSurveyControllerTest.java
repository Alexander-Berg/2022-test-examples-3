package ru.yandex.market.partner.mvc.controller.periodic_survey;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.core.periodic_survey.model.SurveyId;
import ru.yandex.market.core.periodic_survey.model.SurveyRecord;
import ru.yandex.market.core.periodic_survey.model.SurveyStatus;
import ru.yandex.market.core.periodic_survey.model.SurveyType;
import ru.yandex.market.core.periodic_survey.service.PeriodicSurveyExperiment;
import ru.yandex.market.core.periodic_survey.service.PeriodicSurveyService;
import ru.yandex.market.core.periodic_survey.yt.PeriodicSurveyYtDao;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Функциональный тест для {@link PeriodicSurveyController}.
 */
@DbUnitDataSet(before = "PeriodicSurveyControllerTest.before.csv")
class PeriodicSurveyControllerTest extends FunctionalTest {

    private static final long CAMPAIGN_ID = 10101L;
    private static final long PARTNER_ID = 101L;
    private static final long USER_ID = 1010L;
    public static final String INCORRECT_SURVEY_ID = "65-1-16-5efc8968";
    public static final String PARSING_ERROR_MESSAGE = "[{\"code\":\"BAD_PARAM\",\"message\":\"Incorrect survey type " +
            "22\",\"details\":{}}]";
    public static final String SURVEY_ID = "65-3f2-0-5efc8968";
    private static final String SURVEY_ID_INCORRECT_PARTNER = "777-3f2-0-5efc8968";
    private static final String SURVEY_ID_INCORRECT_USER = "65-777-0-5efc8968";

    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private PeriodicSurveyYtDao periodicSurveyYtDao;
    @Autowired
    private PeriodicSurveyService periodicSurveyService;

    @Autowired
    private PeriodicSurveyExperiment periodicSurveyExperiment;
    @Autowired
    @Qualifier("jacksonMapper")
    private ObjectMapper jsonMapper;
    @Autowired
    private TestableClock clock;

    private PeriodicSurveyController controller;

    @BeforeEach
    void setUp() {
        clock.setFixed(LocalDateTime.of(2020, 7, 1, 13, 2, 32)
                .toInstant(ZoneOffset.UTC), ZoneId.ofOffset("", ZoneOffset.UTC));
        controller = new PeriodicSurveyController(jsonMapper, periodicSurveyService, environmentService,
                periodicSurveyExperiment, clock);
        environmentService.setValue(PeriodicSurveyExperiment.EXP_VAR, "false");
    }

    @Test
    @DisplayName("Получение информации о доступных опросах для партнера - есть активный опрос для юзера")
    void testGetSurveysHasActiveSurvey() {
        clock.setFixed(Instant.parse("2020-07-02T13:03:32Z"), ZoneId.of("UTC"));

        environmentService.setValue(PeriodicSurveyController.DISPLAY_SURVEYS, "true");
        when(periodicSurveyYtDao.getOpenedSurveysForPartnerUser(eq(PARTNER_ID), eq(USER_ID)))
                .thenReturn(List.of(getSurveyRecord()));
        //language=json
        String expected = "[\n" +
                "{\n" +
                "    \"surveyId\": \"65-1-0-5efc8968\",\n" +
                "    \"surveyType\": \"NPS_DROPSHIP\",\n" +
                "    \"surveyStatus\": \"ACTIVE\",\n" +
                "    \"createdAt\": \"2020-07-01T13:02:32Z\"\n" +
                "  }\n" +
                "]";

        ResponseEntity<String> response = getRequestOpenSurveys();

        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DisplayName("Получение информации о доступных опросах для партнера - есть активный опрос для юзера," +
            " но сутки ещё не прошли")
    void testDoNotShowSurveyIfDayNotPassed() {
        clock.setFixed(LocalDateTime.of(2020, 7, 1, 23, 2, 32)
                .toInstant(ZoneOffset.UTC), ZoneId.ofOffset("", ZoneOffset.UTC));

        environmentService.setValue(PeriodicSurveyController.DISPLAY_SURVEYS, "true");
        when(periodicSurveyYtDao.getOpenedSurveysForPartnerUser(eq(PARTNER_ID), eq(USER_ID)))
                .thenReturn(List.of(getSurveyRecord()));
        //language=json
        String expected = "[]";
        ResponseEntity<String> response = getRequestOpenSurveys();
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DisplayName("Получение информации о доступных опросах для партнера - есть отложенный опрос для юзера")
    void testGetSurveysHasPostponedSurvey() {
        clock.setFixed(LocalDateTime.of(2020, 8, 2, 13, 3, 32)
                .toInstant(ZoneOffset.UTC), ZoneId.ofOffset("", ZoneOffset.UTC));

        environmentService.setValue(PeriodicSurveyController.DISPLAY_SURVEYS, "true");
        when(periodicSurveyYtDao.getOpenedSurveysForPartnerUser(eq(PARTNER_ID), eq(USER_ID)))
                .thenReturn(List.of(getPostponedSurveyRecord()));
        //language=json
        String expected = "[{\"surveyId\":\"65-1-0-5efc8968\",\"surveyType\":\"NPS_DROPSHIP\"," +
                "\"surveyStatus\":\"POSTPONED\"," +
                "\"createdAt\":\"2020-07-01T13:02:32Z\"," +
                "\"lastAnsweredAt\":\"2020-08-01T13:02:32Z\"}]";
        ResponseEntity<String> response = getRequestOpenSurveys();
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DisplayName("Получение информации о доступных опросах для партнера - есть активный опрос для юзера," +
            " но сутки ещё не прошли")
    void testDoNotShowPostponedSurveyIfDayNotPassed() {
        clock.setFixed(LocalDateTime.of(2020, 8, 1, 23, 2, 32)
                .toInstant(ZoneOffset.UTC), ZoneId.ofOffset("", ZoneOffset.UTC));

        environmentService.setValue(PeriodicSurveyController.DISPLAY_SURVEYS, "true");
        when(periodicSurveyYtDao.getOpenedSurveysForPartnerUser(eq(PARTNER_ID), eq(USER_ID)))
                .thenReturn(List.of(getPostponedSurveyRecord()));
        //language=json
        String expected = "[]";
        ResponseEntity<String> response = getRequestOpenSurveys();
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DisplayName("Получение информации о доступных опросах для партнера - из ытя приходит null")
    void testGetSurveysHasNullSurvey() {
        environmentService.setValue(PeriodicSurveyController.DISPLAY_SURVEYS, "true");
        when(periodicSurveyYtDao.getOpenedSurveysForPartnerUser(eq(PARTNER_ID), eq(USER_ID)))
                .thenReturn(List.of());
        //language=json
        String expected = "[]";
        ResponseEntity<String> response = getRequestOpenSurveys();
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DisplayName("Получение информации о доступных опросах для партнера - отображение опросов выключено")
    void testGetSurveysDisplayDisabled() {
        environmentService.setValue(PeriodicSurveyController.DISPLAY_SURVEYS, "false");
        //language=json
        String expected = "[]";
        ResponseEntity<String> response = getRequestOpenSurveys();
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DisplayName("Получение информации об опросе по ид - корректный сценарий")
    void testGetSurveyByID() {
        environmentService.setValue(PeriodicSurveyController.DISPLAY_SURVEYS, "true");
        when(periodicSurveyYtDao.getSurvey(any())).thenReturn(Optional.of(getSurveyRecordWithAnswer()));
        //language=json
        String expected = "{\"surveyId\":\"65-3f2-0-5efc8968\",\"surveyType\":\"NPS_DROPSHIP\"," +
                "\"surveyStatus\":\"ACTIVE\",\"createdAt\":\"2020-07-01T13:02:32Z\"," +
                "\"lastAnsweredAt\":\"2020-07-02T09:14:30Z\",\"lastAnswer\":{\"input\":\"answer\"}}";

        ResponseEntity<String> response = getResponseSurveyById(SURVEY_ID);
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DisplayName("Получение информации об опросе по ид - не совпадает partnerId")
    void testGetSurveyIncorrectPartnerId() {
        environmentService.setValue(PeriodicSurveyController.DISPLAY_SURVEYS, "true");
        when(periodicSurveyYtDao.getSurvey(any())).thenReturn(Optional.of(getSurveyRecordWithAnswer()));

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class, () -> getResponseSurveyById(SURVEY_ID_INCORRECT_PARTNER));
        assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.NOT_FOUND)
        );
    }

    @Test
    @DisplayName("Получение информации об опросе по ид - не совпадает userId")
    void testGetSurveyIncorrectUserId() {
        environmentService.setValue(PeriodicSurveyController.DISPLAY_SURVEYS, "true");
        when(periodicSurveyYtDao.getSurvey(any())).thenReturn(Optional.of(getSurveyRecordWithAnswer()));

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class, () -> getResponseSurveyById(SURVEY_ID_INCORRECT_USER));
        assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.NOT_FOUND)
        );
    }

    @Test
    @DisplayName("Получение информации об опросе по ид - ошибка парсинга")
    void testGetSurveyByIdParsingError() {
        environmentService.setValue(PeriodicSurveyController.DISPLAY_SURVEYS, "true");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class, () -> getResponseSurveyById(INCORRECT_SURVEY_ID));
        assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)
        );
        JsonTestUtil.assertResponseErrorMessage("[{\"code\":\"BAD_PARAM\",\"message\":\"Incorrect survey type 22\"," +
                "\"details\":{}}]", exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Ответ на опрос - ошибка парсинга")
    void answerSurveyTestParsingError() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class, () -> completeSurvey(INCORRECT_SURVEY_ID));
        assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)
        );
        JsonTestUtil.assertResponseErrorMessage(PARSING_ERROR_MESSAGE, exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Ответ на опрос - ошибка при попытке отложить отложенный опрос")
    void postponeAlreadyPostponedSurvey() {
        when(periodicSurveyYtDao.getSurvey(any())).thenReturn(Optional.of(getPostponedSurveyRecord()));
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class, this::postponeSurvey);
        assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.CONFLICT)
        );
        JsonTestUtil.assertResponseErrorMessage("[{\"code\":\"CONFLICT\",\"message\":\"Survey 65-1-0-5efc8968 " +
                        "can't be postponed from status POSTPONED\",\"details\":{}}]",
                exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Ответ на опрос - завершить опрос корректно")
    void answerSurveyTestComplete() {
        SurveyRecord initialRecord = getSurveyRecordWithAnswer();
        when(periodicSurveyYtDao.getSurvey(any())).thenReturn(Optional.of(initialRecord));
        ResponseEntity<String> response = completeSurvey(SURVEY_ID);
        ArgumentCaptor<List<SurveyRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(periodicSurveyYtDao).upsertRecords(captor.capture());
        List<SurveyRecord> surveyRecords = captor.getAllValues().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        Assertions.assertEquals(1, surveyRecords.size());
        Assertions.assertEquals(initialRecord.getSurveyId(), surveyRecords.get(0).getSurveyId());
        Assertions.assertEquals(SurveyStatus.COMPLETED, surveyRecords.get(0).getStatus());
        Assertions.assertEquals(response.getStatusCode(), HttpStatus.OK);
    }

    @Test
    @DisplayName("Ответ на опрос - завершить c несовпадающим partnerId")
    void answerSurveyIncorrectPartner() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class, () -> completeSurvey(SURVEY_ID_INCORRECT_PARTNER));
        assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.NOT_FOUND)
        );
    }

    @Test
    @DisplayName("Ответ на опрос - завершить c несовпадающим userId")
    void answerSurveyIncorrectUser() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class, () -> completeSurvey(SURVEY_ID_INCORRECT_USER));
        assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.NOT_FOUND)
        );
    }

    @Test
    @DisplayName("Ответ на опрос - сохранить частичный ответ")
    void answerSurveyTestStorePartialAnswer() {
        SurveyRecord initialRecord = getSurveyRecordWithAnswer();
        when(periodicSurveyYtDao.getSurvey(any())).thenReturn(Optional.of(initialRecord));
        ResponseEntity<String> response = storePartialAnswerSurvey();

        ArgumentCaptor<List<SurveyRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(periodicSurveyYtDao).upsertRecords(captor.capture());
        List<SurveyRecord> surveyRecords = captor.getAllValues().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        assertThat(surveyRecords.size(), equalTo(1));
        SurveyRecord actualRecord = surveyRecords.get(0);
        assertThat(actualRecord.getSurveyId(), equalTo(initialRecord.getSurveyId()));
        assertThat(actualRecord.getStatus(), equalTo(SurveyStatus.ACTIVE));
        JsonTestUtil.assertEquals("{\"a\":1}", actualRecord.getAnswer());
        assertThat(initialRecord.getAnsweredAt(), equalTo(actualRecord.getAnsweredAt()));
        assertThat(HttpStatus.OK, equalTo(response.getStatusCode()));
    }

    @Test
    @DisplayName("Ответ на опрос - успешно отложить активный опрос")
    void postponeSurveyWithSuccess() {
        Instant currentTime = LocalDateTime.of(2020, 7, 1, 13, 2, 32)
                .toInstant(ZoneOffset.UTC);
        clock.setFixed(currentTime, ZoneId.ofOffset("", ZoneOffset.UTC));


        SurveyRecord initialRecord = getSurveyRecord();
        when(periodicSurveyYtDao.getSurvey(any())).thenReturn(Optional.of(initialRecord));
        ResponseEntity<String> response = postponeSurvey();

        ArgumentCaptor<List<SurveyRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(periodicSurveyYtDao).upsertRecords(captor.capture());
        List<SurveyRecord> surveyRecords = captor.getAllValues().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        assertThat(surveyRecords.size(), equalTo(1));
        SurveyRecord actualRecord = surveyRecords.get(0);
        assertThat(actualRecord.getSurveyId(), equalTo(initialRecord.getSurveyId()));
        assertThat(actualRecord.getStatus(), equalTo(SurveyStatus.POSTPONED));
        JsonTestUtil.assertEquals("{}", actualRecord.getAnswer());
        assertThat(actualRecord.getAnsweredAt(), equalTo(currentTime));
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    }

    @Test
    @DisplayName("Ответ на опрос - отклонить активный запрос, если эксперимент включен")
    void rejectSurveyWithSuccess() {
        Instant currentTime = LocalDateTime.of(2020, 7, 1, 13, 2, 32)
                .toInstant(ZoneOffset.UTC);
        clock.setFixed(currentTime, ZoneId.ofOffset("", ZoneOffset.UTC));
        environmentService.setValue(PeriodicSurveyExperiment.EXP_VAR, "true");


        SurveyRecord initialRecord = getSurveyRecord();
        when(periodicSurveyYtDao.getSurvey(any())).thenReturn(Optional.of(initialRecord));
        ResponseEntity<String> response = postponeSurvey();

        ArgumentCaptor<List<SurveyRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(periodicSurveyYtDao).upsertRecords(captor.capture());
        List<SurveyRecord> surveyRecords = captor.getAllValues().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        assertThat(surveyRecords.size(), equalTo(1));
        SurveyRecord actualRecord = surveyRecords.get(0);
        assertThat(actualRecord.getSurveyId(), equalTo(initialRecord.getSurveyId()));
        assertThat(actualRecord.getStatus(), equalTo(SurveyStatus.REJECTED));
        JsonTestUtil.assertEquals("{}", actualRecord.getAnswer());
        assertThat(actualRecord.getAnsweredAt(), equalTo(currentTime));
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    }

    private ResponseEntity<String> getRequestOpenSurveys() {
        return FunctionalTestHelper.get(
                baseUrl + "/campaigns/{campaignId}/periodic-survey?_user_id=1010&euid=100500", CAMPAIGN_ID);
    }

    private ResponseEntity<String> getResponseSurveyById(String surveyId) {
        return FunctionalTestHelper.get(
                baseUrl + "/campaigns/{campaignId}/periodic-survey/{surveyId}?_user_id=1010&euid=100500",
                CAMPAIGN_ID, surveyId);
    }

    private ResponseEntity<String> completeSurvey(String surveyId) {
        return FunctionalTestHelper.post(
                baseUrl + "/campaigns/{campaignId}/periodic-survey/{surveyId}/answer?_user_id=1010&euid=100500",
                JsonTestUtil.getJsonHttpEntity("{" +
                        "  \"action\": \"COMPLETE\"," +
                        "  \"answer\": {" +
                        "    \"a\": 1" +
                        "  }," +
                        "  \"viewDetails\": {" +
                        "    \"b\": \"w\"" +
                        "  }" +
                        "}"),
                CAMPAIGN_ID, surveyId);
    }

    private ResponseEntity<String> storePartialAnswerSurvey() {
        return FunctionalTestHelper.post(
                baseUrl + "/campaigns/{campaignId}/periodic-survey/{surveyId}/answer?_user_id=1010&euid=100500",
                JsonTestUtil.getJsonHttpEntity("{" +
                        "  \"action\": \"STORE_PARTIAL_ANSWER\"," +
                        "  \"answer\": {" +
                        "    \"a\": 1" +
                        "  }," +
                        "  \"viewDetails\": {" +
                        "    \"b\": \"w\"" +
                        "  }" +
                        "}"),
                CAMPAIGN_ID, SURVEY_ID);
    }

    private ResponseEntity<String> postponeSurvey() {
        return FunctionalTestHelper.post(
                baseUrl + "/campaigns/{campaignId}/periodic-survey/{surveyId}/answer?_user_id=1010&euid=100500",
                JsonTestUtil.getJsonHttpEntity("{" +
                        "  \"action\": \"POSTPONE\"," +
                        "  \"answer\": {}," +
                        "  \"viewDetails\": {}" +
                        "}"),
                CAMPAIGN_ID, SURVEY_ID);
    }

    private SurveyRecord getSurveyRecord() {
        return SurveyRecord.newBuilder()
                .withSurveyId(SurveyId.of(101L, 1L,
                        SurveyType.NPS_DROPSHIP,
                        Instant.parse("2020-07-01T13:02:32Z")))
                .withStatus(SurveyStatus.ACTIVE)
                .build();
    }

    private SurveyRecord getPostponedSurveyRecord() {
        return SurveyRecord.newBuilder()
                .withSurveyId(SurveyId.of(101L, 1L,
                        SurveyType.NPS_DROPSHIP,
                        Instant.parse("2020-07-01T13:02:32Z")))
                .withStatus(SurveyStatus.POSTPONED)
                .withAnsweredAt(Instant.parse("2020-08-01T13:02:32Z"))
                .build();
    }

    private SurveyRecord getSurveyRecordWithAnswer() {
        return SurveyRecord.newBuilder()
                .withSurveyId(SurveyId.of(PARTNER_ID, USER_ID,
                        SurveyType.NPS_DROPSHIP,
                        Instant.parse("2020-07-01T13:02:32Z")))
                .withStatus(SurveyStatus.ACTIVE)
                .withAnsweredAt(Instant.parse("2020-07-02T09:14:30Z"))
                .withAnswer("{\"input\":\"answer\"}")
                .build();
    }
}
