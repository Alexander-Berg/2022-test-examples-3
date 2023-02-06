package ru.yandex.mail.common.rules;

import java.lang.annotation.Annotation;

import org.junit.runner.Description;
import ru.yandex.mail.common.properties.IgnoreSshTest;

import static org.junit.Assume.assumeFalse;
import static ru.yandex.mail.common.properties.CoreProperties.props;

public class IgnoreSshTestRule extends TestWatcherWithExceptions {
    public static IgnoreSshTestRule newIgnoreSshTestRule() {
        return new IgnoreSshTestRule();
    }

    @Override
    protected void starting(Description description) {
        if (hasAnnotation(description, IgnoreSshTest.class)) {
            assumeFalse("Ssh тест проигнорирован", props().ignoreSshTests());
        }
    }

    private boolean hasAnnotation(Description description, Class<? extends Annotation> annotation) {
        return description.getAnnotation(annotation) != null;
    }
}
