package ru.yandex.direct.api.v5.logging;

import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yandex.direct.api.v5.agencyclients.AddResponse;
import com.yandex.direct.api.v5.audiencetargets.DeleteResponse;
import com.yandex.direct.api.v5.audiencetargets.SetBidsResponse;
import com.yandex.direct.api.v5.general.ActionResult;
import com.yandex.direct.api.v5.general.ActionResultBase;
import com.yandex.direct.api.v5.general.ExceptionNotification;
import com.yandex.direct.api.v5.general.SetBidsActionResult;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import one.util.streamex.StreamEx;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.api.v5.testing.configuration.Api5Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnitParamsRunner.class)
@Api5Test
public class ApiLoggingInterceptorTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    ApiLoggingInterceptor interceptor;

    @Test
    public void secretPasswordIsMasked() throws JsonProcessingException {
        String data = interceptor.serializeResponse(new AddResponse().withLogin("Password").withPassword("se!#$cret"));

        assertThat(data)
                .isEqualTo("{\"Login\":\"Password\",\"Password\":\"*********\"}");
    }

    @Test
    @Parameters(method = "countObjectsWithProp")
    @TestCaseName("{0}")
    public void getObjectErrorCount_parameterized(CountObjectsTestCase testCase) {
        int actual = interceptor.countObjectsWithProp(testCase.response, testCase.propGetter, testCase.propName);
        assertThat(actual).isEqualTo(testCase.expectedCount);
    }

    Iterable<Object> countObjectsWithProp() {
        return asList(
                new CountObjectsTestCase()
                        .responseWith(
                                new ActionResult().withErrors(resultElements(5)),
                                new ActionResult().withErrors(resultElements(1)))
                        .invokeWith(ActionResult::getErrors, "errors")
                        .expect(2),
                new CountObjectsTestCase()
                        .responseWith(new ActionResult().withWarnings(resultElements(2)))
                        .invokeWith(ActionResult::getWarnings, "warnings")
                        .expect(1), // 1 resp obj holds warnings
                new CountObjectsTestCase()
                        .responseWith(new ActionResult().withWarnings(resultElements(2)))
                        .invokeWith(ActionResult::getErrors, "errors")
                        .expect(0),

                new CountObjectsTestCase()
                        .responseWith(
                                new SetBidsActionResult()
                                        .withErrors(resultElements(4)),
                                new SetBidsActionResult()
                                        .withWarnings(resultElements(1)))
                        .invokeWith(ActionResult::getErrors, "errors")
                        .expect(1), // 1 resp obj holds errors
                new CountObjectsTestCase()
                        .responseWith(
                                new SetBidsActionResult()
                                        .withWarnings(resultElements(5)),
                                new SetBidsActionResult()
                                        .withWarnings(resultElements(5))
                                        .withErrors(resultElements(1)),
                                new SetBidsActionResult()
                                        .withErrors(resultElements(1)))
                        .invokeWith(ActionResult::getWarnings, "warnings")
                        .expect(2),

                new CountObjectsTestCase()
                        .response(new Object()) // just should not fail
                        .invokeWith(ActionResultBase::getErrors, "errors")
                        .expect(0)
        );
    }

    private static class CountObjectsTestCase {
        private Object response;
        private Function<ActionResult, List> propGetter;
        private String propName;
        private int expectedCount;

        CountObjectsTestCase invokeWith(Function<ActionResult, List> propGetter, String propName) {
            this.propGetter = propGetter;
            this.propName = propName;
            return this;
        }

        CountObjectsTestCase responseWith(ActionResult... results) {
            this.response = new DeleteResponse().withDeleteResults(results);
            return this;
        }

        CountObjectsTestCase responseWith(SetBidsActionResult... results) {
            this.response = new SetBidsResponse().withSetBidsResults(results);
            return this;
        }

        CountObjectsTestCase response(Object response) {
            this.response = response;
            return this;
        }

        CountObjectsTestCase expect(int expectedCount) {
            this.expectedCount = expectedCount;
            return this;
        }

        @Override
        public String toString() {
            return String.join(" : ", response.getClass().getSimpleName(), propName, String.valueOf(expectedCount));
        }
    }

    private static List<ExceptionNotification> resultElements(int size) {
        return StreamEx.generate(ExceptionNotification::new).limit(size).toList();
    }
}
