package ru.yandex.market.pers.grade.api;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.common.util.StringUtils;
import ru.yandex.market.pers.grade.MockedPersGradeTest;
import ru.yandex.market.pers.grade.core.model.experiment.ExperimentalGrade;
import ru.yandex.market.pers.grade.core.model.experiment.ExperimentalUserType;
import ru.yandex.market.pers.grade.ugc.ExperimentalGradeService;
import ru.yandex.market.pers.grade.ugc.api.dto.ExperimentalGradeResponseDto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.grade.core.model.experiment.ExperimentalUserType.PASSPORT_ID;
import static ru.yandex.market.pers.grade.core.model.experiment.ExperimentalUserType.YANDEXUID;

/**
 * Тесты экспериментального API для отзывов.
 *
 * @author vvolokh
 * 09.07.2018
 */
public class ExperimentalGradeControllerTest extends MockedPersGradeTest {
    private static final String OFFER_GRADE_TYPE_STRING = "offer";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String PASSPORT_ID_PATH = "UID";
    private static final String YANDEXUID_PATH = "YANDEXUID";

    private static final String TEST_USER_ID = "123456789";

    public static final String OFFER_GRADE_JSON =
            "{" +
                "\"comment\":\"Test grade\", " +
                "\"averageGrade\":\"3\", " +
                "\"offerName\":\"some name\", " +
                "\"offerId\":\"test_entity_id\"" +
                "}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExperimentalGradeService experimentalGradeService;

    private String invoke(MockHttpServletRequestBuilder requestBuilder, ResultMatcher... expected) throws Exception {
        ResultActions mvc = mockMvc.perform(requestBuilder
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print());
        for (ResultMatcher matcher : expected) {
            mvc.andExpect(matcher);
        }
        return mvc.andReturn().getResponse().getContentAsString();
    }

    private String addGrade(String gradeType,
                            ExperimentalUserType userType,
                            String userId,
                            String body,
                            ResultMatcher... expected) throws Exception {
        return invoke(
            post("/api/exp_grade/user/" + getType(userType) + "/" + userId + "/" + gradeType + "/publish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .accept(MediaType.APPLICATION_JSON),
            expected);
    }

    private void addGradeOk(String gradeType,
                            ExperimentalUserType userType,
                            String userId,
                            String body) throws Exception {
        addGrade(gradeType, userType, userId, body, status().is2xxSuccessful());

        List<ExperimentalGrade> grades = experimentalGradeService.getExperimentalGrades(userType, userId);
        assertEquals(1, grades.size());

        ExperimentalGrade grade = grades.get(0);
        assertEquals("test_entity_id", grade.getEntityId());
        assertEquals("some name", grade.getEntityName());
        assertEquals(3, grade.getGradeValue());
        assertEquals("Test grade", grade.getText());
    }

    /**
     * Тест отправки отзыва с использованием passport id.
     */
    @Test
    public void testSendingExpOfferGradePassport() throws Exception {
        addGradeOk(OFFER_GRADE_TYPE_STRING, PASSPORT_ID, TEST_USER_ID, OFFER_GRADE_JSON);
    }

    /**
     * Тест отправки отзыва с использованием YandexUID.
     */
    @Test
    public void testSendingExpOfferGradeYandexUid() throws Exception {
        addGradeOk(OFFER_GRADE_TYPE_STRING, YANDEXUID, TEST_USER_ID, OFFER_GRADE_JSON);
    }

    /**
     * Тест валидации минимального значения оценки.
     */
    @Test
    public void testSendingExpOfferInvalidGradeL() throws Exception {
        addGrade(OFFER_GRADE_TYPE_STRING, YANDEXUID,
                TEST_USER_ID, OFFER_GRADE_JSON.replace("\"averageGrade\":\"3\"", "\"averageGrade\":\"0\""),
                status().is4xxClientError());
    }

    /**
     * Тест валидации максимального значения оценки.
     */
    @Test
    public void testSendingExpOfferInvalidGradeM() throws Exception {
        addGrade(OFFER_GRADE_TYPE_STRING, YANDEXUID,
                TEST_USER_ID, OFFER_GRADE_JSON.replace("\"averageGrade\":\"3\"", "\"averageGrade\":\"6\""),
                status().is4xxClientError());
    }

    /**
     * Тест валидации максимальной длины отзыва.
     */
    @Test
    public void testSendingExpOfferGradeWithLongText() throws Exception {
        addGrade(OFFER_GRADE_TYPE_STRING, YANDEXUID,
                TEST_USER_ID, OFFER_GRADE_JSON.replace("\"comment\":\"Test grade\"", "\"comment\":\"" +
                        StringUtils.multiply("1234567890", 200, "") + "1" + "\""), status().is4xxClientError());
    }

    /**
     * Тест отзыва с пустым текстом.
     */
    @Test
    public void testSendingExpOfferGradeWithoutText() throws Exception {
        addGrade(OFFER_GRADE_TYPE_STRING, YANDEXUID,
                TEST_USER_ID, OFFER_GRADE_JSON.replace("\"comment\":\"Test grade\"", "\"comment\":\"\""),
                status().is2xxSuccessful());
    }

    /**
     * Тест повторной отправки отзыва.
     */
    @Test
    public void testSendingExpOfferGradeTwice() throws Exception {
        String result = addGrade(OFFER_GRADE_TYPE_STRING, YANDEXUID, TEST_USER_ID,
                OFFER_GRADE_JSON,
                status().is2xxSuccessful());
        ExperimentalGradeResponseDto responseDto = objectMapper.readValue(result, ExperimentalGradeResponseDto.class);
        result = addGrade(OFFER_GRADE_TYPE_STRING, YANDEXUID, TEST_USER_ID,
                OFFER_GRADE_JSON,
                status().is4xxClientError());
        ExperimentalGradeResponseDto failedResponseDto =
                objectMapper.readValue(result, ExperimentalGradeResponseDto.class);
        assertTrue(responseDto.getSuccess());
        assertFalse(failedResponseDto.getSuccess());
    }

    private String getType(ExperimentalUserType type) {
        switch (type) {
            case PASSPORT_ID:
                return PASSPORT_ID_PATH;
            case YANDEXUID:
                return YANDEXUID_PATH;
        }
        return "unknown";
    }
}
