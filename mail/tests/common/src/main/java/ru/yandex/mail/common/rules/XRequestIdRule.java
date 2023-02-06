package ru.yandex.mail.common.rules;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static ru.yandex.mail.common.properties.CoreProperties.props;

/**
 * Created by vicdev on 05.07.16.
 */
public class XRequestIdRule extends TestWatcher {
    public static final int MAX_LENGTH = 55;
    public static final int RANDOM_PATH_LENGTH = 5;

    public static XRequestIdRule xRequestIdRule() {
        return new XRequestIdRule();
    }

    /**
     * AUTOTESTPERS-196
     * 55 символов предел количества символов в заголовке X-Request-Id для sanitaizer-а
     *
     * @param description
     */
    @Override
    protected void starting(Description description) {
        String className = description.getTestClass().getSimpleName();
        String methodName = description.getMethodName();
        StringBuilder xRequestId = new StringBuilder(randomAlphanumeric(RANDOM_PATH_LENGTH));

        if (className.matches("[a-zA-Z0-9]+") && className.length() < (MAX_LENGTH - RANDOM_PATH_LENGTH - 1)) {
            xRequestId.insert(0, className + ".");
        }

        if (methodName.matches("[a-zA-Z0-9]+") && (methodName.length() + xRequestId.toString().length())
                < (MAX_LENGTH - RANDOM_PATH_LENGTH - 1)) {
            xRequestId.insert(className.length() + 1, methodName + ".");
        }

        props().setCurrentRequestId(xRequestId.toString());
    }
}
