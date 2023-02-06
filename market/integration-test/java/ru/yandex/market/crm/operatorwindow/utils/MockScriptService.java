package ru.yandex.market.crm.operatorwindow.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import ru.yandex.market.jmf.script.Script;
import ru.yandex.market.jmf.script.ScriptService;
import ru.yandex.market.jmf.script.impl.ScriptServiceImpl;

@Primary
@Component
public class MockScriptService implements ScriptService, MockService {
    private final ScriptServiceImpl scriptService;
    private final List<Expectation> expectations = new ArrayList<>();

    public MockScriptService(ScriptServiceImpl scriptService) {
        this.scriptService = scriptService;
    }

    @Override
    public <T> T execute(String script, Map<String, ?> variables) {
        return scriptService.execute(script, variables);
    }

    @Override
    public <T> T execute(Script script, Map<String, ?> variables) {
        tryGetPredefinedError(script, variables)
                .ifPresent(e -> {
                    throw e;
                });
        return scriptService.execute(script, variables);
    }

    @Override
    public Script getCurrentScript() {
        return null;
    }

    @NotNull
    private Optional<RuntimeException> tryGetPredefinedError(Script script,
                                                             Map<String, ?> variables) {
        for (int i = 0; i < expectations.size(); ++i) {
            Expectation item = expectations.get(i);
            if (item.predicate.shouldThrowError(script, variables)) {
                expectations.remove(i);
                return Optional.of(item.getException());
            }
        }
        return Optional.empty();
    }

    public void registerError(ErrorPredicate p, String errorText) {
        expectations.add(new Expectation(p, new TestException(errorText)));
    }

    @Override
    public void clear() {
        this.expectations.clear();
    }

    public interface ErrorPredicate {
        boolean shouldThrowError(Script script,
                                 Map<String, ?> variables);
    }

    private static class Expectation {
        private final ErrorPredicate predicate;
        private final RuntimeException exception;

        public Expectation(ErrorPredicate predicate,
                           RuntimeException exception) {
            this.predicate = predicate;
            this.exception = exception;
        }

        public ErrorPredicate getPredicate() {
            return predicate;
        }

        public RuntimeException getException() {
            return exception;
        }
    }

    public static class TestException extends RuntimeException {
        public TestException(String message) {
            super(message);
        }
    }
}
