package ru.yandex.autotests.direct.utils.matchers;

import org.junit.Test;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.direct.utils.matchers.TextResourceMatcher.textResourceMatcher;
import static ru.yandex.autotests.direct.utils.textresource.TextResources.getKey;

/**
 * Created with IntelliJ IDEA.
 * User: alexey-n
 */
public class TextResourceMatcherTest {
    public enum TestTextResource implements ITextResource {
        TEST_STRING1, //test
        TEST_STRING2, //Test1
        TEST_STRING3; //test%s

        private static String BUNDLE = "TestTextResource";

        @Override
        public String getBundle() {
            return BUNDLE;
        }

        @Override
        public String toString() {
            return getKey(this);
        }

    }

    @Test
    public void containsStringTest() {
        TextResourceFormatter resourceFormatter = TextResourceFormatter.resource(TestTextResource.TEST_STRING1)
                .locale("en");
        assertThat("Error contains string", "test1 string",
                textResourceMatcher(resourceFormatter, containsString(resourceFormatter.toString())));
    }
    @Test
    public void containsStringLocaleTest() {
        TextResourceFormatter resourceFormatter = TextResourceFormatter.resource(TestTextResource.TEST_STRING1)
                .locale("ru");
        assertThat("Error contains string", "тест",
                textResourceMatcher(resourceFormatter, containsString(resourceFormatter.toString())));
    }
    @Test
    public void equalToTest() {
        TextResourceFormatter resourceFormatter = TextResourceFormatter.resource(TestTextResource.TEST_STRING2);
        assertThat("Error equal string", "Test1",
                textResourceMatcher(resourceFormatter, equalTo(resourceFormatter.toString())));
    }

    @Test
    public void equalToFormattedTest() {
        TextResourceFormatter resourceFormatter = TextResourceFormatter.resource(TestTextResource.TEST_STRING3)
                .args("ing");
        assertThat("Error equal formatted string", "testing",
                textResourceMatcher(resourceFormatter, equalTo(resourceFormatter.toString())));
    }

    @Test(expected = AssertionError.class)
    public void containsStringTestShouldFailed() {
        TextResourceFormatter resourceFormatter = TextResourceFormatter.resource(TestTextResource.TEST_STRING1);
        assertThat("Error contains string", "tes string",
                textResourceMatcher(resourceFormatter, containsString(resourceFormatter.toString())));
    }

    @Test(expected = AssertionError.class)
    public void equalToTestShouldFailed() {
        TextResourceFormatter resourceFormatter = TextResourceFormatter.resource(TestTextResource.TEST_STRING2);
        assertThat("Error equal string", "Test1 string",
                textResourceMatcher(resourceFormatter, equalTo(resourceFormatter.toString())));
    }
}
