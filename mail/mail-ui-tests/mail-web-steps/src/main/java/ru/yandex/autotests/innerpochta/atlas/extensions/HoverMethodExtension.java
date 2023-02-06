package ru.yandex.autotests.innerpochta.atlas.extensions;

import io.qameta.atlas.core.AtlasException;
import io.qameta.atlas.core.api.MethodExtension;
import io.qameta.atlas.core.internal.Configuration;
import io.qameta.atlas.core.util.MethodInfo;
import io.qameta.atlas.webdriver.context.WebDriverContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.lang.reflect.Method;

public class HoverMethodExtension implements MethodExtension {

    private static final String HOVER = "hover";

    public HoverMethodExtension() {
    }

    public boolean test(Method method) {
        return method.getName().equals(HOVER);
    }

    public Object invoke(Object proxy, MethodInfo methodInfo, Configuration configuration) {
        WebDriver driver = configuration.getContext(WebDriverContext.class)
            .orElseThrow(() -> new AtlasException("WebDriver is missing"))
            .getValue();
        Actions actions = new Actions(driver);
        actions.moveToElement((WebElement) proxy).perform();
        return proxy;
    }

}
