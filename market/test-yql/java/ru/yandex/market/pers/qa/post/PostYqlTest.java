package ru.yandex.market.pers.qa.post;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pers.yt.yqlgen.YqlLoader;
import ru.yandex.yt.yqltest.YqlTestScript;
import ru.yandex.yt.yqltest.spring.AbstractYqlTest;

/**
 * @author grigor-vlad
 * 20.07.2022
 */
public class PostYqlTest extends AbstractYqlTest {

    @Test
    public void testPostExport() {
        runTest(
            loadScript("/yql/post/pub_post_for_recommendation.sql"),
            "/post/expected_export_post.json",

            "/post/post.mock",
            "/post/content.mock",
            "/post/link.mock"
        );
    }

    private YqlTestScript loadScript(String path) {
        return YqlTestScript.simple(YqlLoader.readYqlWithLib(path));
    }
}
