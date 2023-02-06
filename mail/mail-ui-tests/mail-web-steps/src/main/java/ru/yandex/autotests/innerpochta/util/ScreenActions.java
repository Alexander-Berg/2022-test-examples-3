package ru.yandex.autotests.innerpochta.util;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.coordinates.Coords;
import ru.yandex.qatools.ashot.coordinates.WebDriverCoordsProvider;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.openqa.selenium.By.cssSelector;
import static ru.yandex.autotests.innerpochta.util.props.UrlProps.urlProps;

/**
 * @author cosmopanda
 */
public class ScreenActions {

    private Set<By> ignoredElements;
    private Set<Coords> ignoredAreas;
    private Consumer<InitStepsRule> actions;

    private Function<InitStepsRule, Screenshot> scr = stepsRule -> {
        AShot ashot = new AShot();
        ashot.ignoredElements(ignoredElements);
        ashot.ignoredAreas(ignoredAreas);
        ashot.coordsProvider(new WebDriverCoordsProvider());
        long xOffset = getOffset(stepsRule, "pageXOffset");
        long yOffset = getOffset(stepsRule, "pageYOffset");
        Screenshot screenshot = ashot.takeScreenshot(stepsRule.getDriver());
        for (Coords coords : screenshot.getIgnoredAreas()) {
            coords.translate((int) -xOffset, (int) -yOffset);
        }
        return screenshot;
    };

    private static long getOffset(InitStepsRule stepsRule, String parameter) {
        JavascriptExecutor executor = (JavascriptExecutor) stepsRule.getDriver();
        String offset = executor.executeScript("return window." + parameter + ";").toString();
        try {
            return Math.round(Double.parseDouble(offset));
        } catch (NumberFormatException e) {
            throw new AssertionError("Не удалось получить смещение окна: некорректный формат числа: " + offset);
        }
    }

    private ScreenActions(Consumer<InitStepsRule> actions) {
        this.actions = actions;
    }

    public static ScreenActions withScreenshot(Consumer<InitStepsRule> actions) {
        return new ScreenActions(actions);
    }

    ScreenActions withIgnoredElements(Set<By> ignoredElements) {
        if (urlProps().getIgnoreElement() != null) {
            String[] customElementsToIgnore = urlProps().getIgnoreElement().split(";");
            for (String customElementToIgnore : customElementsToIgnore) {
                ignoredElements.add(cssSelector(customElementToIgnore));
            }
        }
        this.ignoredElements = ignoredElements;
        return this;
    }

    ScreenActions withIgnoredAreas(Set<Coords> ignoredAreas) {
        this.ignoredAreas = ignoredAreas;
        return this;
    }

    @Step("Выполнение действий")
    private void accept(InitStepsRule stepsRule, Consumer<InitStepsRule> prepare) {
        prepare.andThen(actions).accept(stepsRule);
    }

    public Screenshot on(InitStepsRule stepsRule, Consumer<InitStepsRule> prepare) {
        accept(stepsRule, prepare);
        //ждем пока все элементы догрузятся
        try {
            Thread.sleep(SECONDS.toMillis(2));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return scr.apply(stepsRule);
    }
}
