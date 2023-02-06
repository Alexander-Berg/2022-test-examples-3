package ru.yandex.market.tpl.internal.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.core.domain.company_draft.CompanyDraftManager;
import ru.yandex.market.tpl.core.domain.yaforms.dbqueue.survey.ProcessYaFormsSurveyAnswersProducer;
import ru.yandex.market.tpl.internal.BaseShallowTest;
import ru.yandex.market.tpl.internal.WebLayerTest;
import ru.yandex.market.tpl.internal.controller.yaforms.YandexFormsController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebLayerTest(YandexFormsController.class)
public class YaFormsControllerTest extends BaseShallowTest {

    @MockBean
    private ProcessYaFormsSurveyAnswersProducer surveyAnswersProducer;
    @MockBean
    private CompanyDraftManager companyDraftManager;

    @Test
    void surveyMappingIsSuccessful() throws Exception {
        mockMvc.perform(multipart("/external/yaforms/survey")
                .param("field_1", getFileContent("yaforms/firstreq/field_1.json"))
                .param("field_2", getFileContent("yaforms/firstreq/field_2.json"))
                .param("field_3", getFileContent("yaforms/firstreq/field_3.json"))
                .param("field_4", getFileContent("yaforms/firstreq/field_4.json"))
                .param("field_5", getFileContent("yaforms/firstreq/field_5.json"))
        ).andExpect(status().is2xxSuccessful());

    }
}
