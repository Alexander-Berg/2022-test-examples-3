package ru.yandex.autotests.innerpochta.atlas.extensions;

import io.qameta.atlas.core.Atlas;
import io.qameta.atlas.core.api.MethodExtension;
import io.qameta.atlas.core.api.Retry;
import io.qameta.atlas.core.api.Target;
import io.qameta.atlas.core.context.RetryerContext;
import io.qameta.atlas.core.internal.Configuration;
import io.qameta.atlas.core.internal.DefaultRetryer;
import io.qameta.atlas.core.internal.Retryer;
import io.qameta.atlas.core.target.LazyTarget;
import io.qameta.atlas.core.util.MethodInfo;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.util.MethodInfoUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

import static ru.yandex.autotests.passport.TestAdditionalSettings.onMobileDevice;

/**
 * @author gladnik (Nikolai Gladkov)
 */
public class FindByCssExtension implements MethodExtension {

    public FindByCssExtension() {
    }

    @Override
    public boolean test(final Method method) {
        return method.isAnnotationPresent(FindByCss.class) && WebElement.class.isAssignableFrom(method.getReturnType());
    }

    @Override
    public Object invoke(final Object proxy,
                         final MethodInfo methodInfo,
                         final Configuration configuration) {
        final Method method = methodInfo.getMethod();

        assert proxy instanceof SearchContext;
        assert method.isAnnotationPresent(FindByCss.class);

        SearchContext searchContext = (SearchContext) proxy;

        final Map<String, String> parameters = MethodInfoUtils.getParamValues(method, methodInfo.getArgs());
        final String selector = getSelector(method, parameters);
        final String name = Optional.ofNullable(method.getAnnotation(Name.class))
            .map(Name::value)
            .map(template -> MethodInfoUtils.processParamTemplate(template, parameters))
            .orElse(method.getName());

        final Configuration childConfiguration = configuration.child();
        Optional.ofNullable(methodInfo.getMethod().getAnnotation(Retry.class)).ifPresent(retry -> {
            Retryer retryer = new DefaultRetryer(retry);
            childConfiguration.registerContext(new RetryerContext(retryer));
        });
        final Target elementTarget = new LazyTarget(name, () -> searchContext.findElement(By.cssSelector(selector)));
        return (new Atlas(childConfiguration)).create(elementTarget, method.getReturnType());
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

}
