package ru.yandex.market.pers.qa.controller.api.system;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import ru.yandex.market.pers.qa.controller.ControllerTest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 12.10.2018
 */
public class PageMatcherTest extends ControllerTest {

    @Test
    void testPageMatcher() throws Exception {
        final String doc = getDoc();

        assertTrue(doc.contains("get_model_questions_uid_using_get\tGET:/question/model/<modelId>/UID/<userId>\thttp"));
        assertTrue(doc.contains("delete_question_using_delete\tDELETE:/question/<questionId>\thttp"));
    }

    private String getDoc() throws Exception {
        return invokeAndRetrieveResponse(
            get("/pagematch")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
    }
}
