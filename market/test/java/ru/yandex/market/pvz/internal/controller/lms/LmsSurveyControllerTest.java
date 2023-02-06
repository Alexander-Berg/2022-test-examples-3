package ru.yandex.market.pvz.internal.controller.lms;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.pvz.core.test.factory.TestSurveyFactory;
import ru.yandex.market.pvz.internal.BaseShallowTest;
import ru.yandex.market.pvz.internal.WebLayerTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pvz.core.TestUtils.getFileContent;

@WebLayerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LmsSurveyControllerTest extends BaseShallowTest {

    private final TestSurveyFactory surveyFactory;

    @Test
    void getSurveys() throws Exception {

        var survey = surveyFactory.create();

        mockMvc.perform(get("/lms/survey"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("lms/survey/response_get_surveys.json"),
                        survey.getStartDate(), survey.getEndDate()), false));
    }

    @Test
    void getSurveyInformation() throws Exception {

        var survey = surveyFactory.create();

        mockMvc.perform(get("/lms/survey/" + survey.getId()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("lms/survey/response_get_survey_information.json"),
                        survey.getStartDate(), survey.getEndDate()), false));
    }

    @Test
    void getEmptySurveyInformation() throws Exception {

        mockMvc.perform(get("/lms/survey/new"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(
                        getFileContent("lms/survey/response_get_empty_survey_information.json"), false));
    }

    @Test
    void createSurveyThenSuccess() throws Exception {
        mockMvc.perform(post("/lms/survey")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("lms/survey/request_create_survey.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(
                        getFileContent("lms/survey/response_create_survey.json"), false));
    }

    @Test
    void createSurveyWithNoFrequency() throws Exception {
        mockMvc.perform(post("/lms/survey")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("lms/survey/request_create_survey_no_frequency.json")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createSurveyWithBadFrequency() throws Exception {
        mockMvc.perform(post("/lms/survey")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("lms/survey/request_create_survey_bad_frequency.json")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createSurveyWithFrequencyOnceMonth() throws Exception {
        mockMvc.perform(post("/lms/survey")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("lms/survey/request_create_survey_frequency_once_month.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(
                        getFileContent("lms/survey/response_create_survey_frequency_once_month.json"), false));
    }

    @Test
    void createSurveyWithBadDates() throws Exception {
        mockMvc.perform(post("/lms/survey")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("lms/survey/request_create_survey_bad_dates.json")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateSurvey() throws Exception {

        var survey = surveyFactory.create();

        mockMvc.perform(put("/lms/survey/" + survey.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(getFileContent("lms/survey/request_update_survey.json"),
                                survey.getId())))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(
                        getFileContent("lms/survey/response_update_survey.json"), false));
    }

}
