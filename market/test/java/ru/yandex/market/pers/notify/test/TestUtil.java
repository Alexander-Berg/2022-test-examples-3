package ru.yandex.market.pers.notify.test;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.test.web.client.response.DefaultResponseCreator;

import ru.yandex.common.util.IOUtils;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class TestUtil {
    private static final Logger logger = Logger.getLogger(TestUtil.class);

    private TestUtil() {
    }

    public static RequestMatcher contentJsonFromFile(String fileName) throws IOException {
        return content().string(new JsonMatcher(stringFromFile(fileName)));
    }

    public static String stringFromFile(String filename) throws IOException {
        return IOUtils.readInputStream(TestUtil.class.getResourceAsStream(filename));
    }

    public static DefaultResponseCreator withSuccessJson(String responseFileName) throws IOException {
        return withSuccess(stringFromFile(responseFileName), MediaType.APPLICATION_JSON);
    }


    private static class JsonMatcher extends BaseMatcher<String> {
        private final String expected;

        JsonMatcher(String expected) {
            this.expected = expected;
        }

        @Override
        public boolean matches(Object item) {
            try {
                System.out.println(item.toString());
                JSONCompareResult result = JSONCompare.compareJSON(expected, (String) item, JSONCompareMode.STRICT);
                boolean passed = result.passed();
                if (!passed) {
                    logger.error("JSONCompareResult: " + result);
                }
                return passed;
            } catch (JSONException e) {
                logger.error(e);
                return false;
            }
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("Json: " + expected);
        }
    }
}
