package ru.yandex.market.logshatter.parser.front.errorBooster.stackParser;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class MordaViewsStackParserTest {
    private static final TestData[] ERRORS = {
        new TestData(
            "Object.home.error (js_libs/core.js:183:17) -> execView (js_libs/core.js:1453:38) -> body__layout " +
                "(stream/pages/views/commonStreamDesktopViews.view.js:38:121) -> execView (js_libs/core.js:1442:47) " +
                "->  body (common/pages/views/commonViews.view.js:2238:88)",
            new StackFrame("Object.home.error", "js_libs/core.js", 183, 17),
            new StackFrame("execView", "js_libs/core.js", 1453, 38),
            new StackFrame("body__layout", "stream/pages/views/commonStreamDesktopViews.view.js", 38, 121),
            new StackFrame("execView", "js_libs/core.js", 1442, 47),
            new StackFrame("body", "common/pages/views/commonViews.view.js", 2238, 88)
        ),
        new TestData(
            "TypeError.home.error (js_libs/core.js:177:21) -> execView (js_libs/core.js:1433:44) -> " +
                "common/pages/views/commonViews.view.js:4349:20 -> execView (js_libs/core.js:1422:47) -> " +
                "yabro/pages/views/yabroBannerViews.view.js:432:18",
            new StackFrame("TypeError.home.error", "js_libs/core.js", 177, 21),
            new StackFrame("execView", "js_libs/core.js", 1433, 44),
            new StackFrame("(anonymous)", "common/pages/views/commonViews.view.js", 4349, 20),
            new StackFrame("execView", "js_libs/core.js", 1422, 47),
            new StackFrame("(anonymous)", "yabro/pages/views/yabroBannerViews.view.js", 432, 18)
        ),

        new TestData(
            "Object.home.error (/place/db/iss3/instances/testing-morda-yp-1_testing_morda_yp_rEehjhaOvFP/www/morda" +
                "/tmpl/js_libs/home/home.js:183:17) -> execView " +
                "(/place/db/iss3/instances/testing-morda-yp-1_testing_morda_yp_rEehjhaOvFP/www/morda/tmpl/js_libs" +
                "/home/home.views.js:103:30) -> " +
                "/place/db/iss3/instances/testing-morda-yp-1_testing_morda_yp_rEehjhaOvFP/www/morda/tmpl/common/pages" +
                "/views/blocks/b-banner/b-banner.view.js:607:13",
            new StackFrame("Object.home.error", "/place/db/iss3/instances/testing-morda-yp" +
                "-1_testing_morda_yp_rEehjhaOvFP/www/morda/tmpl/js_libs/home/home.js", 183, 17),
            new StackFrame("execView", "/place/db/iss3/instances/testing-morda-yp-1_testing_morda_yp_rEehjhaOvFP/www" +
                "/morda/tmpl/js_libs/home/home.views.js", 103, 30),
            new StackFrame("(anonymous)", "/place/db/iss3/instances/testing-morda-yp-1_testing_morda_yp_rEehjhaOvFP" +
                "/www/morda/tmpl/common/pages/views/blocks/b-banner/b-banner.view.js", 607, 13)
        ),

        // Empty
        new TestData(
            ""
        ),
    };

    @Test
    public void parseStack() {
        for (TestData test : ERRORS) {
            test.assertStack();
        }
    }

    static class TestData {
        private final String stack;
        private final StackFrame[] frames;

        TestData(String stack, StackFrame... frames) {
            this.stack = stack;
            this.frames = frames;
        }

        void assertStack() {
            StackFrame[] actualFrames = new MordaViewsStackParser(stack).getStackFrames();
            assertArrayEquals(
                frames,
                actualFrames,

                "Error parsing stack. Original stack:\n" +
                    "-----\n" +
                    stack + "\n" +
                    "-----\n" +
                    "Parsed stack:\n" +
                    "-----\n" +
                    StringUtils.join(actualFrames, '\n') + "\n" +
                    "-----\n" +
                    "Assertion message"
            );
        }
    }
}
