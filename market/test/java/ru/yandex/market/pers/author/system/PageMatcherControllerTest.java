package ru.yandex.market.pers.author.system;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.author.PersAuthorTest;
import ru.yandex.market.pers.author.mock.mvc.SystemMvcMocks;

import static java.lang.String.join;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Damir Shagaev / damir-vvlpx@ / 04.03.2020
 */
public class PageMatcherControllerTest extends PersAuthorTest {

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
            join(SEPARATOR, "save_user_interests_by_ids_using_post", "POST:/interest/UID/<userId>/save", PAGE_TYPE)));
        assertTrue(response.contains(
            join(SEPARATOR, "get_all_interests_using_get", "GET:/interest/dictionary", PAGE_TYPE)));
        assertTrue(response.contains(
            join(SEPARATOR, "save_video_info_using_post", "POST:/video/UID/<userId>", PAGE_TYPE)));
        assertTrue(response.contains(
            join(SEPARATOR, "delete_video_using_delete", "DELETE:/video/<id>", PAGE_TYPE)));
        assertTrue(response.contains(
            join(SEPARATOR, "get_video_info_by_uid_using_get", "GET:/video/UID/<userId>/bulk", PAGE_TYPE)));
    }
}
