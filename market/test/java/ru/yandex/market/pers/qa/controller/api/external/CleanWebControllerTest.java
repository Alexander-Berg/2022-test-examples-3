package ru.yandex.market.pers.qa.controller.api.external;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.qa.controller.QAControllerTest;
import ru.yandex.market.pers.qa.mock.mvc.CleanWebMvcMocks;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.service.QuestionService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CleanWebControllerTest extends QAControllerTest {

    @Autowired
    QuestionService questionService;

    @Autowired
    CleanWebMvcMocks cleanWebMvcMocks;

    @Test
    void testQuestionCallback() throws Exception {
        long id = createQuestion();
        String body =  String.format(fileToString("/data/clean_web_callback.json"),
            QaEntityType.QUESTION.getValue(), id, QaEntityType.QUESTION.getValue(), id);
        cleanWebMvcMocks.callback(body);

        Question question = questionService.getQuestionByIdInternal(id);
        assertEquals(ModState.AUTO_FILTER_REJECTED, question.getModState());
    }

    @Test
    void testRepeatedQuestionCallback() throws Exception {
        long id = createQuestion();
        String body =  String.format(fileToString("/data/clean_web_callback.json"),
            QaEntityType.QUESTION.getValue(), id, QaEntityType.QUESTION.getValue(), id);
        cleanWebMvcMocks.callback(body);
        Long rowsInLog = qaJdbcTemplate.queryForObject(
            "select count(*) from qa.auto_filter_result where entity_id = ? and entity_type = ?",
            Long.class, id, QaEntityType.QUESTION.getValue());
        assertEquals(1, rowsInLog);

        Question question = questionService.getQuestionByIdInternal(id);
        assertEquals(ModState.AUTO_FILTER_REJECTED, question.getModState());
        cleanWebMvcMocks.callback(body);
        rowsInLog = qaJdbcTemplate.queryForObject(
            "select count(*) from qa.auto_filter_result where entity_id = ? and entity_type = ?",
            Long.class, id, QaEntityType.QUESTION.getValue());
        assertEquals(1, rowsInLog);
    }

    @Test
    void testCallbackWithNewFilter() throws Exception {
        long id = createQuestion();
        String body =  String.format(fileToString("/data/clean_web_callback_with_unknown_filter.json"),
            QaEntityType.QUESTION.getValue(), id, QaEntityType.QUESTION.getValue(), id);

        cleanWebMvcMocks.callback(body, status().is2xxSuccessful());

        Question question = questionService.getQuestionByIdInternal(id);

        // забанили даже на непонятном новом фильтре, т.к. он вернул булев ответ
        assertEquals(ModState.AUTO_FILTER_REJECTED, question.getModState());
    }


    @Test
    void testCallbackInvalidKey() throws Exception {
        String body =  String.format(fileToString("/data/clean_web_callback_with_unknown_filter.json"),
            13, 11, 13, 11);

        cleanWebMvcMocks.callback(body, status().is4xxClientError());
    }

    @Test
    void testWrongCallbackWithNewFilter() throws Exception {
        String body = "{}";

        cleanWebMvcMocks.callback(body, status().is2xxSuccessful());
    }
}
