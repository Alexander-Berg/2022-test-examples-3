package ru.yandex.market.crm.triggers.test;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

/**
 * @author apershukov
 */
public class BpmnErrorMatcher extends BaseMatcher<Exception> {

    private final String expectedCode;

    private BpmnErrorMatcher(String expectedCode) {
        this.expectedCode = expectedCode;
    }

    public static BpmnErrorMatcher expectCode(String expectedCode) {
        return new BpmnErrorMatcher(expectedCode);
    }

    @Override
    public boolean matches(Object item) {
        if (!(item instanceof BpmnError)) {
            return false;
        }

        BpmnError error = (BpmnError) item;

        return expectedCode.equals(error.getErrorCode());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("BpmnError with code " + expectedCode);
    }
}
