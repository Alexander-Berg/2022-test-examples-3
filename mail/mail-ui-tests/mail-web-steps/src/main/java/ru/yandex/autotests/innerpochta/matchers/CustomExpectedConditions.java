package ru.yandex.autotests.innerpochta.matchers;

import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

public class CustomExpectedConditions {

    private final static Logger log = Logger.getLogger(CustomExpectedConditions.class.getName());

    private CustomExpectedConditions() {
        // Utility class
    }

    /**
     * Ожидание, проверяющее, что текст элемента соответствует определенному тексту
     *
     * @param element это искомый элемент
     * @param text    это текст, который должен быть в элементе
     * @return Возвращает true, когда текст элемента равен тексту text
     */
    public static ExpectedCondition<Boolean> textEquals(final WebElement element, final String text) {
        return new ExpectedCondition<Boolean>() {
            private String currentValue = null;

            @Override
            public Boolean apply(WebDriver driver) {
                try {
                    currentValue = element.getText();
                    return currentValue.equals(text);
                } catch (Exception e) {
                    return false;
                }
            }

            @Override
            public String toString() {
                return String
                    .format("Текущий текст элемента: %s. Необходим текст: %s", currentValue, text);
            }
        };
    }

    /**
     * Ожидание, проверяющее, что элемента нет на странице
     *
     * @param element это искомый элемент
     * @return Возвращает true, когда элемент не виден или не существует на странице
     */
    public static ExpectedCondition<Boolean> elementIsNotDisplayed(final WebElement element) {
        return new ExpectedCondition<Boolean>() {

            @Override
            public Boolean apply(WebDriver driver) {
                try {
                    return !element.isDisplayed();
                } catch (NoSuchElementException | ElementNotVisibleException | StaleElementReferenceException e) {
                    return true;
                }
            }

            @Override
            public String toString() {
                return String
                    .format("Элемент %s всё ещё существует", element);
            }
        };
    }

    /**
     * Методы, проверяющие элементы в списке
     * Дублирует дефолтные условия, но принимают "<? extends WebElement>"
     *
     * @param elements это список элементов
     */

    public static ExpectedCondition<List<? extends WebElement>> visibilityOfElementsInList(final List<? extends WebElement> elements) {
        return new ExpectedCondition<List<? extends WebElement>>() {

            public List<? extends WebElement> apply(WebDriver driver) {
                Iterator var2 = elements.iterator();
                WebElement element;
                do {
                    if (!var2.hasNext()) {
                        return elements.size() > 0 ? elements : null;
                    }
                    element = (WebElement) var2.next();
                } while (element.isDisplayed());
                return null;
            }

            public String toString() {
                return "Отображаются элементы " + elements;
            }
        };
    }

    public static ExpectedCondition<List<? extends WebElement>> numberOfElementsInListToBe(List<? extends WebElement> elements, final Integer number) {
        return new ExpectedCondition<List<? extends WebElement>>() {

            private Integer currentNumber = 0;

            public List<? extends WebElement> apply(WebDriver webDriver) {
                this.currentNumber = elements.size();
                return this.currentNumber.equals(number) ? elements : null;
            }

            public String toString() {
                return String
                    .format(
                        "Ожидаемое количество элементов в списке \"%s\" не совпадает с текущим: \"%s\"",
                        number,
                        this.currentNumber
                    );
            }
        };
    }

    /**
     * Ожидание, проверяющее, что элемент в данный момент находится во вьюпорте. Т.е. элемент виден человеку, а не
     * находится где-то за скроллом
     *
     * @param element это искомый элемент
     * @return Возвращает true, когда элемент находится во вьюпорте
     */
    public static ExpectedCondition<Boolean> elementIsInViewport(final MailElement element) {
        return new ExpectedCondition<Boolean>() {

            @Override
            public Boolean apply(WebDriver webDriver) {
                return (Boolean) ((JavascriptExecutor) webDriver).executeScript("var element = arguments[0];" +
                    "box = element.getBoundingClientRect();" +
                    "if (box.x + box.width > 0 && box.x < window.innerWidth){" +
                    "  if (box.y + box.height > 0 && box.y < window.innerHeight){" +
                    "    return true" +
                    "  }" +
                    "}" +
                    "return false", element.getWrappedElement());
            }

            public String toString() {
                return String.format("Элемент «%s» не отображается на экране", element);
            }
        };
    }

    /**
     * Ожидание, проверяющее, что элемент активен
     *
     * @param element это искомый элемент
     * @return Возвращает true, когда элемент активен
     */
    public static ExpectedCondition<Boolean> elementIsEnabled(final WebElement element) {
        return new ExpectedCondition<Boolean>() {

            @Override
            public Boolean apply(WebDriver driver) {
                try {
                    return element.isEnabled();
                } catch (NoSuchElementException | ElementNotVisibleException | StaleElementReferenceException e) {
                    return false;
                }
            }

            @Override
            public String toString() {
                return String
                    .format("Элемент %s всё ещё задизеблен", element);
            }
        };
    }

    /**
     * Ожидание, проверяющее, что текст элемента подходит под паттерн
     *
     * @param element это искомый элемент
     * @param pattern это паттерн, с которым сравниваем
     * @return Возвращает true, когда текст элемента подходит под паттерн
     */
    public static ExpectedCondition<Boolean> textMatchesPattern(final WebElement element, final String pattern) {
        return new ExpectedCondition<Boolean>() {

            @Override
            public Boolean apply(WebDriver driver) {
                try {
                    return element.getText().matches(pattern);
                } catch (NoSuchElementException | ElementNotVisibleException | StaleElementReferenceException e) {
                    return false;
                }
            }

            @Override
            public String toString() {
                return String
                    .format("Элемент %s всё ещё задизеблен", element);
            }
        };
    }

    /**
     * Ожидание, проверяющее, что выполняемый js возвращает нужную строку
     *
     * @param jsscript это скрипт, который должен возвращать строку в результате
     * @param expected это строка, которую ожидаем получить в результате работы скрипта
     * @return Возвращает true, когда скрипт вернул ожидаемый результат
     */
    public static ExpectedCondition<Boolean> jsScriptRunsAsExpected(final String jsscript, final String expected) {
        return new ExpectedCondition<Boolean>() {

            private String scriptResult;

            @Override
            public Boolean apply(WebDriver driver) {
                try {
                    JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
                    scriptResult = ((String) jsExecutor.executeScript(jsscript));
                    return ((String) jsExecutor.executeScript(jsscript)).equals(expected);
                } catch (NoSuchElementException | ElementNotVisibleException | StaleElementReferenceException | NullPointerException e) {
                    return false;
                }
            }

            @Override
            public String toString() {
                return String
                    .format("Скрипт вернул %s, ожидалось %s", scriptResult, expected);
            }
        };
    }
}
