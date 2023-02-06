package ru.yandex.autotests.innerpochta.atlas.extensions;

import io.qameta.atlas.core.Atlas;
import io.qameta.atlas.core.api.MethodExtension;
import io.qameta.atlas.core.api.Retry;
import io.qameta.atlas.core.api.Target;
import io.qameta.atlas.core.context.RetryerContext;
import io.qameta.atlas.core.internal.Configuration;
import io.qameta.atlas.core.internal.DefaultRetryer;
import io.qameta.atlas.core.internal.Retryer;
import io.qameta.atlas.core.target.HardcodedTarget;
import io.qameta.atlas.core.target.LazyTarget;
import io.qameta.atlas.core.util.MethodInfo;
import io.qameta.atlas.webdriver.extension.Name;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static io.qameta.atlas.webdriver.util.MethodInfoUtils.getParamValues;
import static io.qameta.atlas.webdriver.util.MethodInfoUtils.processParamTemplate;
import static java.util.stream.Collectors.toList;
import static ru.yandex.autotests.passport.TestAdditionalSettings.onMobileDevice;

/**
 * @author gladnik (Nikolai Gladkov)
 */
public class FindByCssCollectionExtension implements MethodExtension {

    public FindByCssCollectionExtension() {
    }

    @Override
    public boolean test(final Method method) {
        return method.isAnnotationPresent(FindByCss.class) && List.class.isAssignableFrom(method.getReturnType());
    }

    @Override
    public Object invoke(final Object proxy,
                         final MethodInfo methodInfo,
                         final Configuration configuration) {
        final Method method = methodInfo.getMethod();

        assert proxy instanceof SearchContext;
        assert method.isAnnotationPresent(FindByCss.class);

        final SearchContext context = (SearchContext) proxy;

        final Map<String, String> parameters = getParamValues(method, methodInfo.getArgs());
        final String selector = getSelector(method, parameters);
        final String name = Optional.ofNullable(method.getAnnotation(Name.class))
            .map(Name::value)
            .map(template -> processParamTemplate(template, parameters))
            .orElse(method.getName());

        final LazyTarget elementsTarget = new LazyTarget(name, () -> {
            final List<WebElement> originalElements = context.findElements(By.cssSelector(selector));
            final Type methodReturnType = ((ParameterizedType) method.getGenericReturnType())
                .getActualTypeArguments()[0];

            return IntStream.range(0, originalElements.size())
                .mapToObj(i -> {
                    final WebElement originalElement = originalElements.get(i);
                    final Configuration childConfiguration = configuration.child();
                    final Target target = new HardcodedTarget(listElementName(name, i), originalElement);
                    return new Atlas(childConfiguration)
                        .create(target, (Class<?>) methodReturnType);
                })
                .collect(toList());
        });

        final Configuration childConfiguration = configuration.child();
        Optional.ofNullable(methodInfo.getMethod().getAnnotation(Retry.class)).ifPresent(retry -> {
            final Retryer retryer = new DefaultRetryer(retry);
            childConfiguration.registerContext(new RetryerContext(retryer));
        });

        return new Atlas(childConfiguration)
            .create(elementsTarget, method.getReturnType());
    }

    private String getSelector(final Method method, final Map<String, String> parameters) {
        final FindByCss annotation = method.getAnnotation(FindByCss.class);
        String selector = annotation.value();
        if (!annotation.mobile().isEmpty() && onMobileDevice()) {
            selector = annotation.mobile();
        }
        for (String key : parameters.keySet()) {
            selector = selector.replaceAll("\\{\\{ " + key + " \\}\\}", parameters.get(key));
        }
        return selector;
    }

    private String listElementName(final String name, final int position) {
        return String.format("%s [%s]", name, position);
    }

}
