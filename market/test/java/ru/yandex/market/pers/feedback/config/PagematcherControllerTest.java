package ru.yandex.market.pers.feedback.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.feedback.mock.PagematcherMock;

import static java.lang.String.join;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Dionisii Iuzhakov / bahus@ / 27.04.2021
 */
public class PagematcherControllerTest extends AbstractPersFeedbackTest{
    private static final String SEPARATOR = "\t";
    private static final String PAGE_TYPE = "http";

    @Autowired
    private PagematcherMock pagematcherMock;

    @Test
    public void testPageMatcher200Ok() throws Exception {
        pagematcherMock.getPageMatcher(status().is2xxSuccessful());
    }

    @Test
    public void testPageMatcherResultContains() throws Exception {
        String response = pagematcherMock.getPageMatcher();
        System.out.println(response);
        assertTrue(response.contains(
            join(SEPARATOR, "get_order_feedback_using_get", "GET:/orders/<orderId>/feedback", PAGE_TYPE)));
        assertTrue(response.contains(
            join(SEPARATOR, "put_order_feedback_using_put", "PUT:/orders/<orderId>/feedback", PAGE_TYPE)));
        assertTrue(response.contains(
            join(SEPARATOR, "get_order_questions_using_get", "GET:/questions", PAGE_TYPE)));
        assertTrue(response.contains(
            join(SEPARATOR, "post_question_rule_using_post", "POST:/questions/rules", PAGE_TYPE)));
        assertTrue(response.contains(
            join(SEPARATOR, "delete_question_rule_using_delete", "DELETE:/questions/rules/<id>", PAGE_TYPE)));
    }

}
