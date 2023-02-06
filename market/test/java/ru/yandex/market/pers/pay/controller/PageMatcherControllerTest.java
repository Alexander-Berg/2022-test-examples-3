package ru.yandex.market.pers.pay.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.pay.PersPayTest;
import ru.yandex.market.pers.pay.mvc.SystemMvcMocks;

import static java.lang.String.join;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 11.03.2021
 */
public class PageMatcherControllerTest extends PersPayTest {

    private static final String SEPARATOR = "\t";
    private static final String PAGE_TYPE = "http";

    @Autowired
    private SystemMvcMocks systemMvcMocks;

    @Test
    public void testPageMatcher200Ok() throws Exception {
        systemMvcMocks.getPageMatcher();
    }

    @Test
    public void testPageMatcherResultContains() throws Exception {
        String response = systemMvcMocks.getPageMatcher();
        assertTrue(response.contains(
            join(SEPARATOR, "show_model_grade_price_using_get", "GET:/pay/grade/model/user/UID/<userId>/show", PAGE_TYPE)));
        assertTrue(response.contains(
            join(SEPARATOR, "find_grade_payments_using_get", "GET:/pay/grade/check", PAGE_TYPE)));
    }
}
