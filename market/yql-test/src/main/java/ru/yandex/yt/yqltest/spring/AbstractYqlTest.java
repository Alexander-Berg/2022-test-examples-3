package ru.yandex.yt.yqltest.spring;

import org.json.JSONException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.yt.yqltest.YqlTestRunner;
import ru.yandex.yt.yqltest.YqlTestScript;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 01.09.2021
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource({
    "classpath:/yql-test-application.properties",
    "classpath:/yql-test-application-custom.properties",
})
@ContextConfiguration(classes = {YqlTestYtConfig.class})
public class AbstractYqlTest {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    protected YqlTestRunner yqlTestRunner;

    @Value("${USE_YT_RECIPE:false}")
    private boolean useRecipe;

    public void runTest(YqlTestScript script,
                        String expectedPath,
                        String... mocks) {
        yqlTestRunner.runTest(script, expectedPath, this::checkJson, mocks);
    }

    public void checkJson(String expected, String actual) {
        try {
            JSONAssert.assertEquals(expected, actual, false);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (AssertionError e) {
            log.error("Json differs from expected: " + actual, e);
            throw e;
        }
    }

    public boolean isUseRecipe() {
        return useRecipe;
    }
}
