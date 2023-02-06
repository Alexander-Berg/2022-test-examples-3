package ru.yandex.market.pers.comparison.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.comparison.PersComparisonTest;
import ru.yandex.market.pers.comparison.mvc.SystemMvcMocks;

import static java.lang.String.join;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PageMatcherControllerTest extends PersComparisonTest {

    private static final String SEPARATOR = "\t";
    private static final String PAGE_TYPE = "http";

    @Autowired
    private SystemMvcMocks systemMvcMocks;

    @Test
    public void testPageMatcher200Ok() throws Exception {
        systemMvcMocks.getPageMatcher(status().is2xxSuccessful());
    }

    @Test
    public void testPageMatcherResultContains() throws Exception {
        String response = systemMvcMocks.getPageMatcher();
        assertTrue(response.contains(
            join(SEPARATOR, "get_items_using_get_1", "GET:/comparison/<userType>/<userId>", PAGE_TYPE)));
        assertTrue(response.contains(
            join(SEPARATOR, "remove_user_items_using_delete_1", "DELETE:/comparison/<userType>/<userId>",
                PAGE_TYPE)));
        assertTrue(response.contains(
            join(SEPARATOR, "merge_using_patch_1", "PATCH:/comparison/<userType>/<userId>", PAGE_TYPE)));
        assertTrue(response.contains(
            join(SEPARATOR, "remove_items_with_product_id_using_delete_1", "DELETE:/comparison/<userType>/<userId>/product/<productId>", PAGE_TYPE)));
        assertTrue(response.contains(
            join(SEPARATOR, "save_item_using_post_1",
                "POST:/comparison/<userType>/<userId>/item", PAGE_TYPE)));
    }
}
