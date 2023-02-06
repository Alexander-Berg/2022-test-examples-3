package ru.yandex.mail.common.rules;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.runner.Description;

import ru.yandex.mail.common.properties.Scope;
import ru.yandex.mail.common.properties.Scopes;


import static java.util.Arrays.asList;
import static org.junit.Assume.assumeTrue;
import static ru.yandex.mail.common.properties.CoreProperties.props;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 08.04.16
 * Time: 14:18
 */
public class IgnoreRule extends TestWatcherWithExceptions {
    public static IgnoreRule newIgnoreRule() {
        return new IgnoreRule();
    }

    @Override
    protected void starting(Description description) {
        if (hasAnnotation(description, Scope.class)) {
            List<Scopes> scopes = asList(description.getAnnotation(Scope.class).value());
            String scopesDescription = String.join(",", scopes.stream()
                    .map(Scopes::getName)
                    .collect(Collectors.toList()));
            assumeTrue("Тест(ы) для scope in {" + scopesDescription + "}. Текущий scope: " + props().scope(),
                    scopes.contains(props().scope()));
        }
    }

    private boolean hasAnnotation(Description description, Class<? extends Annotation> annotation) {
        return description.getAnnotation(annotation) != null;
    }
}
