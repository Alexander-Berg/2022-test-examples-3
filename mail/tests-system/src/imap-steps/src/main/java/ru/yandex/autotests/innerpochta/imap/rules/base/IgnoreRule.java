package ru.yandex.autotests.innerpochta.imap.rules.base;

import java.lang.annotation.Annotation;

import org.junit.runner.Description;

import ru.yandex.autotests.innerpochta.imap.rules.TestWatcherWithExceptions;


/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 12.05.14
 * Time: 20:54
 */
public class IgnoreRule extends TestWatcherWithExceptions {
    public static IgnoreRule newIgnoreRule() {
        return new IgnoreRule();
    }

    @Override
    protected void starting(Description description) {

    }

    private boolean hasAnnotation(Description description, Class<? extends Annotation> annotation) {
        return description.getAnnotation(annotation) != null;
    }


}
