package ru.yandex.autotests.innerpochta.atlas;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.util.ResultsUtils;
import io.qameta.atlas.core.api.Listener;
import io.qameta.atlas.core.api.Target;
import io.qameta.atlas.core.context.TargetContext;
import io.qameta.atlas.core.internal.Configuration;
import io.qameta.atlas.core.util.MethodInfo;
import org.hamcrest.Matcher;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * eroshenkoam
 * 23.03.17
 */
public class AllureListener implements Listener {

    private final Map<String, MethodFormatter> loggableMethods;
    private final AllureLifecycle lifecycle = Allure.getLifecycle();

    public AllureListener() {
        loggableMethods = new HashMap<>();
        loggableMethods.put("click", (description, args) -> String.format("Кликаем на элемент \'%s\'", description));
        loggableMethods.put("submit", (description, args) -> String.format("Нажимаем на элемент \'%s\'", description));
        loggableMethods.put("clear", (description, args) -> String.format("Очищаем элемент \'%s\'", description));
        loggableMethods.put("sendKeys", (description, args) -> {
            String arguments = Arrays.toString(((CharSequence[]) args[0]));
            return String.format("Вводим в элемент \'%s\' значение [%s]", description, arguments);
        });
        loggableMethods.put("waitUntil", (description, args) -> {
            Matcher matcher = (Matcher) (args[0] instanceof Matcher ? args[0] : args[1]);
            return String.format("Ждем пока элемент \'%s\' будет в состоянии [%s]", description, matcher);
        });
        loggableMethods.put("should", (description, args) -> {
            Matcher matcher = (Matcher) (args[0] instanceof Matcher ? args[0] : args[1]);
            return String.format("Проверяем что элемент \'%s\' в состоянии [%s]", description, matcher);
        });
        loggableMethods.put("open", (name, args) -> String.format("Открываем страницу \'%s\'", args[0]));
        loggableMethods.put("hover", (description, args) -> String.format("Наводим курсор на элемент \'%s\'", description));
    }

    @Override
    public void beforeMethodCall(final MethodInfo methodInfo,
                                 final Configuration configuration) {
        getMethodFormatter(methodInfo.getMethod()).ifPresent(formatter -> {
            final String name = configuration.getContext(TargetContext.class)
                .map(TargetContext::getValue)
                .map(Target::name)
                .orElse(methodInfo.getMethod().getName());
            final Object[] args = methodInfo.getArgs();
            lifecycle.startStep(Objects.toString(methodInfo.hashCode()),
                new StepResult().withName(formatter.format(name, args)).withStatus(Status.PASSED));
        });
    }

    @Override
    public void afterMethodCall(final MethodInfo methodInfo,
                                final Configuration configuration) {
        getMethodFormatter(methodInfo.getMethod())
            .ifPresent(title -> lifecycle.stopStep(Objects.toString(methodInfo.hashCode())));
    }

    @Override
    public void onMethodReturn(final MethodInfo methodInfo,
                               final Configuration configuration,
                               final Object returned) {
    }

    @Override
    public void onMethodFailure(final MethodInfo methodInfo,
                                final Configuration configuration,
                                final Throwable throwable) {
        getMethodFormatter(methodInfo.getMethod()).ifPresent(title ->
            lifecycle.updateStep(stepResult -> {
                stepResult.setStatus(ResultsUtils.getStatus(throwable).orElse(Status.BROKEN));
                stepResult.setStatusDetails(ResultsUtils.getStatusDetails(throwable).orElse(null));
            })
        );
    }

    private Optional<MethodFormatter> getMethodFormatter(Method method) {
        return Optional.ofNullable(loggableMethods.get(method.getName()));
    }


    @FunctionalInterface
    private interface MethodFormatter {
        String format(String name, Object[] args);
    }
}