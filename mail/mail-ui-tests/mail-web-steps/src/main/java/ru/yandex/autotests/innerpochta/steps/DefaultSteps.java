package ru.yandex.autotests.innerpochta.steps;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.hash.Hashing;
import com.google.gson.JsonObject;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.LoggerFactory;
import ru.lanwen.diff.uri.core.filters.UriDiffFilter;
import ru.yandex.autotests.innerpochta.annotations.SkipIfFailed;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.data.Select;
import ru.yandex.autotests.innerpochta.objstruct.base.misc.Account;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;
import ru.yandex.autotests.innerpochta.steps.beans.Sign;
import ru.yandex.autotests.innerpochta.util.MultipleWindowsHandler;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.autotests.passport.api.common.data.YandexDomain;
import ru.yandex.autotests.passport.api.core.objects.UserWithProps;
import ru.yandex.junitextensions.rules.passportrule.PassportRule;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.htmlelements.element.TextInput;

import javax.ws.rs.core.UriBuilder;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static ch.lambdaj.Lambda.filter;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static javax.ws.rs.core.UriBuilder.fromUri;
import static org.apache.commons.lang.StringUtils.substringAfter;
import static org.apache.commons.lang.StringUtils.substringBefore;
import static org.apache.commons.lang.StringUtils.substringBeforeLast;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.openqa.selenium.support.ui.ExpectedConditions.attributeContains;
import static org.openqa.selenium.support.ui.ExpectedConditions.attributeToBe;
import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;
import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.textToBePresentInElement;
import static org.openqa.selenium.support.ui.ExpectedConditions.textToBePresentInElementValue;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOf;
import static ru.lanwen.diff.uri.core.filters.AnyParamValueFilter.param;
import static ru.lanwen.diff.uri.core.filters.SchemeFilter.scheme;
import static ru.yandex.autotests.innerpochta.matchers.CommonMatchers.disabledButton;
import static ru.yandex.autotests.innerpochta.matchers.CommonMatchers.enabledButton;
import static ru.yandex.autotests.innerpochta.matchers.CurrentUrlMatcher.containsInCurrentUrl;
import static ru.yandex.autotests.innerpochta.matchers.CurrentUrlMatcher.currentUrl;
import static ru.yandex.autotests.innerpochta.matchers.CustomExpectedConditions.elementIsEnabled;
import static ru.yandex.autotests.innerpochta.matchers.CustomExpectedConditions.elementIsInViewport;
import static ru.yandex.autotests.innerpochta.matchers.CustomExpectedConditions.elementIsNotDisplayed;
import static ru.yandex.autotests.innerpochta.matchers.CustomExpectedConditions.jsScriptRunsAsExpected;
import static ru.yandex.autotests.innerpochta.matchers.CustomExpectedConditions.numberOfElementsInListToBe;
import static ru.yandex.autotests.innerpochta.matchers.CustomExpectedConditions.textEquals;
import static ru.yandex.autotests.innerpochta.matchers.CustomExpectedConditions.textMatchesPattern;
import static ru.yandex.autotests.innerpochta.matchers.CustomExpectedConditions.visibilityOfElementsInList;
import static ru.yandex.autotests.innerpochta.matchers.UriDiffMatcher.currentUriSameAs;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.CHECK_DOWNLOAD_STATE_SCRIPT;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.GET_DOWNLOADED_FILEPATH_SCRIPT;
import static ru.yandex.autotests.innerpochta.util.SkipStep.SkipStepMethods.assumeStepCanContinue;
import static ru.yandex.autotests.innerpochta.util.Utils.getUserUid;
import static ru.yandex.autotests.innerpochta.util.Utils.isPresent;
import static ru.yandex.autotests.innerpochta.util.Utils.standardProgressiveWait;
import static ru.yandex.autotests.innerpochta.util.Utils.standardWait;
import static ru.yandex.autotests.innerpochta.util.Utils.withWaitFor;
import static ru.yandex.autotests.innerpochta.util.props.UrlProps.urlProps;
import static ru.yandex.qatools.htmlelements.matchers.common.HasTextMatcher.hasText;

/**
 * <p> Created by IntelliJ IDEA.
 * <p> User: lanwen
 * <p> Date: 22.05.12
 * <p> Time: 19:33
 */
@SuppressWarnings({"unchecked", "UnusedReturnValue", "UnstableApiUsage"})
public class DefaultSteps {

    private AllureStepStorage user;
    private WebDriverRule webDriverRule;

    public DefaultSteps(WebDriverRule webDriverRule, AllureStepStorage user) {
        this.webDriverRule = webDriverRule;
        this.user = user;
    }

    @Step("Log(what:{0})")
    public DefaultSteps log(String what) {
        LoggerFactory.getLogger(this.getClass()).info(what);
        return this;
    }


    @Step("Клик по: {0}")
    public DefaultSteps clicksOn(WebElement... elements) {
        for (WebElement element : elements) {
            clicksOnElementWithWaiting(element);
        }
        return this;
    }

    @Step("Клик правой кнопкой по: «{0}»")
    public DefaultSteps rightClick(WebElement element) {
        shouldSee(element);
        new Actions(webDriverRule.getDriver()).contextClick(element).perform();
        return this;
    }

    @Step("Клик в координаты: «{0}» «{1}»")
    public DefaultSteps offsetClick(int x, int y) {
        new Actions(webDriverRule.getDriver()).moveByOffset(x, y).click().build().perform();
        return this;
    }

    @Step("Клик в «{0}» со смещением от левого верхнего края элемента «{0}» на («{1}»,«{2}») ")
    public DefaultSteps offsetClick(WebElement element, int x, int y) {
        shouldSee(element);
        new Actions(webDriverRule.getDriver()).moveToElement(element, x, y).click().build().perform();
        return this;
    }

    @Step("Клик в «{0}» со смещением от правого нижнего края элемента «{0}» на («{1}»,«{2}») ")
    public DefaultSteps offsetFromRightCornerClick(WebElement element, int x, int y) {
        shouldSee(element);
        new Actions(webDriverRule.getDriver()).moveToElement(
            element,
            element.getSize().width - x,
            element.getSize().height - y
        ).click().build().perform();
        return this;
    }

    @Step("Двойной клик по: «{0}»")
    public DefaultSteps doubleClick(WebElement element) {
        shouldSee(element);
        new Actions(webDriverRule.getDriver()).doubleClick(element).perform();
        return this;
    }

    @SkipIfFailed
    @Step("Клик по: «{0}», если присутствует на странице")
    public DefaultSteps clicksIfCanOn(WebElement element) {
        assumeStepCanContinue(element, isPresent());
        clicksOn(element);
        return this;
    }

    @SkipIfFailed
    @Step("Клик со смещенияем по: «{0}», если присутствует на странице")
    public DefaultSteps offsetClicksIfCanOn(WebElement element, int x, int y) {
        assumeStepCanContinue(element, isPresent());
        new Actions(webDriverRule.getDriver()).moveToElement(element, x, y).click().build().perform();
        return this;
    }

    @SkipIfFailed
    @Step("Перезагрузить страницу, если элемент «{0}» присутствует на странице")
    public DefaultSteps refreshIfSee(WebElement element) {
        assumeStepCanContinue(element, isPresent());
        refreshPage();
        return this;
    }

    @Step("Клик по элементу который содержит текст «{1}» из списка «{0}»")
    public DefaultSteps clicksOnElementWithText(List<? extends WebElement> elements, String text) {
        List<? extends WebElement> list = filter(hasText(containsString(text)), elements);
        standardProgressiveWait(webDriverRule, 10)
            .withMessage(
                String.format(
                    "Нет элемента с нужным текстом %s",
                    text
                )
            )
            .until(visibilityOfElementsInList(list));
        onMouseHoverAndClick(list.get(0));
        return this;
    }

    @Step("Клик по ссылке на странице")
    public DefaultSteps clicksOnLink(String text) {
        standardProgressiveWait(webDriverRule, 10)
            .withMessage(
                String.format(
                    "Нет элемента с нужным текстом: %s",
                    text
                )
            )
            .until(presenceOfElementLocated(By.linkText(text)));
        clicksOn(webDriverRule.getDriver().findElement(By.linkText(text)));
        return this;
    }

    @Step("Должны видеть элемент с текстом «{1}» в списке «{0}»")
    public MailElement shouldSeeElementInList(ElementsCollection<? extends MailElement> elements, String text) {
        elements.waitUntil(not(empty()));
        List<? extends MailElement> list = filter(hasText(Matchers.containsString(text)), elements);
        standardProgressiveWait(webDriverRule, 10)
            .withMessage(
                String.format(
                    "Нет элемента с нужным текстом %s",
                    text
                )
            )
            .until(visibilityOfElementsInList(list));
        return list.get(0);
    }

    @Step("Должны видеть элемент с текстом «{1}» в списке «{0}» c ожиданием «{2}»")
    public MailElement shouldSeeElementInListWithWaiting(List<? extends MailElement> elements, String text, int timeout) {
        List<? extends MailElement> list = filter(hasText(Matchers.containsString(text)), elements);
        standardWait(webDriverRule, timeout)
            .withMessage(
                String.format(
                    "Нет элемента с нужным текстом %s",
                    text
                )
            )
            .until(visibilityOfElementsInList(list));
        return list.get(0);
    }

    @Step("Должны видеть элементы с текстом «{1}» в списке «{0}»")
    public void shouldSeeAllElementsInList(List<? extends MailElement> elements, String... array) {
        shouldSeeElementsCount(elements, array.length);
        for (String text : array) {
            List<? extends MailElement> list = filter(hasText(Matchers.containsString(text)), elements);
            standardProgressiveWait(webDriverRule, 10)
                .withMessage(
                    String.format(
                        "Нет элемента с нужным текстом %s",
                        text
                    )
                )
                .until(visibilityOfElementsInList(list));
        }
    }

    @Step("Должны видеть хотя бы один элемент из списка во viewport")
    public MailElement shouldSeeElementFromListInViewport(ElementsCollection<? extends MailElement> elements) {
        ElementsCollection<? extends MailElement> list = elements
            .filter(element -> {
                Boolean result = elementIsInViewport(element).apply(webDriverRule.getDriver());
                return (result != null) && result;
            });
        assertFalse("Элемента нет в viewport", list.isEmpty());
        return list.get(0);

    }

    @Step("Не должны видеть элемент «{1}» в списке «{0}»")
    public DefaultSteps shouldNotSeeElementInList(List<? extends MailElement> elements, String text) {
        List<? extends MailElement> list = filter(hasText(Matchers.containsString(text)), elements);
        standardProgressiveWait(webDriverRule, 10)
            .withMessage(
                String.format(
                    "Элемент %s не должен присутствовать в списке",
                    text
                )
            )
            .until(ExpectedConditions.not(visibilityOfElementsInList(list)));
        return this;
    }

    @Step("Выделяем чекбоксы: «{0}»")
    public DefaultSteps turnTrue(MailElement... checkboxes) {
        turnTrue(Arrays.asList(checkboxes));
        return this;
    }

    @Step("Выделяем чекбоксы: «{0}»")
    public DefaultSteps turnTrue(List<MailElement> checkboxes) {
        for (MailElement checkbox : checkboxes) {
            if (!checkbox.isSelected()) {
                onMouseHoverAndClick(checkbox);
            }
        }
        return this;
    }

    @Step("Убираем выделение с чекбоксов: «{0}»")
    public DefaultSteps deselects(MailElement... checkboxes) {
        deselects(Arrays.asList(checkboxes));
        return this;
    }

    @Step("Убираем выделение с чекбоксов: «{0}»")
    public DefaultSteps deselects(List<MailElement> checkboxes) {
        for (MailElement checkbox : checkboxes) {
            if (checkbox.isSelected()) {
                onMouseHoverAndClick(checkbox);
            }
        }
        return this;
    }

    @Step("Вводим текст «{1}» в поле «{0}» без очищения от старого текста")
    public DefaultSteps appendTextInElement(WebElement element, String text) {
        element.sendKeys(text);
        return this;
    }

    @Step("Очищаем поле: «{0}» от старого текста и вводим «{1}»")
    public DefaultSteps inputsTextInElement(WebElement element, String text) {
        shouldSee(element);
        element.clear();
        element.sendKeys(text);
        return this;
    }

    @Step("Очищаем поле через backspace: «{0}» от старого текста и вводим «{1}»")
    public DefaultSteps inputsTextInElementClearingThroughHotKeys(WebElement element, String text) {
        shouldSee(element);
        element.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        element.sendKeys(Keys.DELETE);
        element.sendKeys(text);
        return this;
    }

    @Step("Очищаем поле: «{0}» от старого текста и вводим «{1}»")
    public DefaultSteps clearTextInput(WebElement input) {
        input.clear();
        return this;
    }

    @Step("Из списка «{0}» выбираем  значение:«{1}»")
    public DefaultSteps selectsOption(Select select, String option) {
        assertThat("Элемент «" + select + "» не виден", select, withWaitFor(isPresent()));
        select.click();
        select.getOptionByText(option).click();
        return this;
    }

    @Step("Чекаем «{0}»")
    public void selectsRadio(ElementsCollection<MailElement> radioButtons, int index) {
        assertThat("Радио «" + radioButtons + "» не видны", radioButtons, withWaitFor(hasSize(greaterThan(0))));
        assertThat("Не все радио «" + radioButtons + "» видны", radioButtons, withWaitFor(hasSize(greaterThan(index))));
        turnTrue(radioButtons.get(index));
    }

    @Step("Текст элемента: «{0}» должен совпадать с «{1}»")
    public DefaultSteps shouldSeeThatElementTextEquals(MailElement element, String text) {
        shouldSee(element);
        standardProgressiveWait(webDriverRule, 10)
            .withMessage(
                String.format(
                    "Текст элемента не соотвутствует тексту: «%s». Текущий текст элемента: «%s»",
                    text,
                    element.getText()
                )
            )
            .until(textEquals(element, text));
        return this;
    }

    @Step("Текст элемента: «{0}» должен подходить под паттерн «{1}»")
    public DefaultSteps shouldSeeThatElementTextMatchesPattern(WebElement element, String pattern) {
        standardProgressiveWait(webDriverRule, 10)
            .withMessage(
                String.format(
                    "Элемент не подходит под паттерн : %s. Текущий текст элемента: %s",
                    pattern,
                    element.getText()
                )
            )
            .until(textMatchesPattern(element, pattern));
        return this;
    }

    @Step("Элемент «{0}» Должен содержать текст: «{1}»")
    public DefaultSteps shouldSeeThatElementHasText(WebElement element, String text) {
        standardProgressiveWait(webDriverRule, 10)
            .withMessage(
                String.format(
                    "Элемент не содержит текст: %s. Текущий текст элемента: %s",
                    text,
                    element.getText()
                )
            )
            .until(textToBePresentInElement(element, text));
        return this;
    }

    @Step("Текст элемента: «{0}» должен содержать текст «{1}»")
    public DefaultSteps shouldSeeThatElementHasTextWithCustomWait(MailElement element, String text, int waitTime) {
        standardWait(webDriverRule, waitTime)
            .withMessage(
                String.format(
                    "Элемент не содержит текст: %s. Текущий текст элемента: %s",
                    text,
                    element.getText()
                )
            )
            .until(textToBePresentInElement(element, text));
        return this;
    }

    @Step("Для каждого элемента «{0}» должно выполнятся условие «{1}»")
    public DefaultSteps shouldSeeThatEvery(List<MailElement> checkboxes, boolean selected) {
        shouldSeeCheckBoxesInState(selected, checkboxes.toArray(new MailElement[0]));
        return this;
    }

    @Step("Для каждого чекбокса «{1}» должно выполнятся условие «{0}»")
    public DefaultSteps shouldSeeCheckBoxesInState(boolean selected, MailElement... checkboxes) {
        for (MailElement checkbox : checkboxes) {
            assertEquals("Чекбокс не в нужном состоянии", selected, checkbox.isSelected());
        }
        return this;
    }

    @Step("Для каждого тумблера {1}» должно выполнятся условие «{0}»")
    public DefaultSteps shouldSeeTumblersInState(String selected, MailElement... checkboxes) {
        for (MailElement checkbox : checkboxes) {
            standardProgressiveWait(webDriverRule, 10)
                .withMessage(
                    String.format(
                        "Тумблер находится в состоянии %s, а должен в %s!",
                        checkbox.getAttribute("value"),
                        selected
                    )
                )
                .until(attributeToBe(checkbox, "value", selected));
        }
        return this;
    }

    @Step("Чекбокс «{0}» должен быть выбран")
    public DefaultSteps shouldBeSelected(MailElement checkbox) {
        assertTrue("Чекбокс должен быть выбран!", checkbox.isSelected());
        return this;
    }

    @Step("Чекбокс «{0}» не должен быть выбран")
    public DefaultSteps shouldBeDeselected(MailElement checkbox) {
        assertFalse("Чекбокс не должен быть выбран!", checkbox.isSelected());
        return this;
    }

    @Step("Тумблер «{0}» должен быть выбран")
    public DefaultSteps shouldBeSelectedTumbler(MailElement checkbox) {
        standardProgressiveWait(webDriverRule, 10)
            .withMessage("Тумблер должен быть выбран!")
            .until(attributeToBe(checkbox, "value", "true"));
        return this;
    }

    @Step("Тумблер «{0}» не должен быть выбран")
    public DefaultSteps shouldBeDeselectedTumbler(MailElement checkbox) {
        standardProgressiveWait(webDriverRule, 10)
            .withMessage("Тумблер не должен быть выбран!")
            .until(attributeToBe(checkbox, "value", "false"));
        return this;
    }

    @Step("Поле ввода «{0}» должно содержать текст «{1}»")
    public DefaultSteps shouldContainText(TextInput element, String text) {
        standardProgressiveWait(webDriverRule, 10)
            .withMessage(
                String.format(
                    "Текст «%s» элемента «%s» не совпадает с «%s»",
                    element.getText(),
                    element,
                    text
                )
            )
            .until(textEquals(element, text));
        return this;
    }

    @Step("Свайпнуть элемент «{0}»")
    public DefaultSteps swipe(WebElement element, int x, int y) {
        shouldSee(element);
        new Actions(webDriverRule.getDriver()).dragAndDropBy(element, x, y).perform();
        waitInSeconds(1);
        return this;
    }

    @Step("Свайпнуть элемент «{0}» влево")
    public DefaultSteps swipeLeft(WebElement element) {
        return swipe(element, -element.getSize().getWidth() / 2, 0);
    }

    @Step("Свайпнуть элемент «{0}» вправо")
    public DefaultSteps swipeRight(WebElement element) {
        return swipe(element, element.getSize().getWidth() / 3, 0);
    }

    //---------------------------------------------------------
    // HOME PAGE STEPS
    //---------------------------------------------------------

    @Step("Должны видеть элемент: {0}")
    public DefaultSteps shouldSee(WebElement element) {
        standardProgressiveWait(webDriverRule, 8)
            .withMessage("Элемент «" + element + "» не найден!")
            .until(visibilityOf(element));
        return this;
    }

    @Step("Должны видеть элемент: {0} c ожиданием {1} секунд")
    public DefaultSteps shouldSeeWithWaiting(WebElement element, int timeout) {
        standardWait(webDriverRule, timeout)
            .withMessage("Элемент «" + element + "» не найден!")
            .until(visibilityOf(element));
        return this;
    }

    @Step("Должны видеть элементы: {0}")
    public DefaultSteps shouldSee(WebElement... elements) {
        for (WebElement element : elements) {
            shouldSee(element);
        }
        return this;
    }

    @Step("Должны видеть элементы: {0}")
    public DefaultSteps shouldSee(ElementsCollection<? extends WebElement> elements) {
        shouldSee(elements.toArray(new WebElement[0]));
        return this;
    }

    @Step("Не должны видеть элемент: {0}")
    public DefaultSteps shouldNotSee(WebElement element) {
        standardProgressiveWait(webDriverRule, 10)
            .withMessage(String.format("Элемент «%s» всё ещё виден!", element))
            .until(elementIsNotDisplayed(element));
        return this;
    }

    @Step("Не должны видеть элементы: {0}")
    public DefaultSteps shouldNotSee(WebElement... elements) {
        for (WebElement element : elements) {
            shouldNotSee(element);
        }
        return this;
    }

    @Step("Не должны видеть элемент: {0}")
    public DefaultSteps shouldNotSee(ElementsCollection<? extends WebElement> elements) {
        shouldNotSee(elements.toArray(new WebElement[0]));
        return this;
    }

    @Step("Элемент: «{0}» должен содержать текст: «{1}»")
    public DefaultSteps shouldContainText(WebElement element, String text) {
        standardProgressiveWait(webDriverRule, 10)
            .withMessage(
                String.format(
                    "Текст «%s» элемента «%s» не содержит текст «%s»",
                    element.getText(),
                    element,
                    text
                )
            )
            .until(textToBePresentInElement(element, text));
        return this;
    }

    @Step("Элемент: «{0}» не должен содержать: «{1}»")
    public DefaultSteps shouldNotContainText(MailElement element, String text) {
        standardProgressiveWait(webDriverRule, 10)
            .withMessage(String.format("Текст элемента «%s» содержит «%s»", element, text))
            .until(ExpectedConditions.not(textToBePresentInElement(element, text)));
        return this;
    }

    @Step("Текст элемента: «{0}»  должен совпадать с «{1}»")
    public DefaultSteps shouldHasText(MailElement element, String text) {
        standardProgressiveWait(webDriverRule, 10)
            .withMessage(
                String.format(
                    "Текст «%s» элемента «%s» не совпадает с «%s»",
                    element.getText(),
                    element,
                    text
                )
            )
            .until(textEquals(element, text));
        return this;
    }

    @Step("Текст элемента «{0}» не должен совпадать с «{1}»")
    public DefaultSteps shouldNotHasText(MailElement element, String text) {
        standardProgressiveWait(webDriverRule, 10)
            .withMessage(
                String.format(
                    "Текст «%s» элемента «%s» совпадает с «%s»",
                    element.getText(),
                    element,
                    text
                )
            )
            .until(ExpectedConditions.not(textEquals(element, text)));
        return this;
    }

    @Step("Текст в value элемента «{0}» должен содержать «{1}»")
    public DefaultSteps shouldContainValue(WebElement element, String value) {
        standardProgressiveWait(webDriverRule, 10)
            .withMessage(
                String.format(
                    "Текст в value «%s» элемента «%s» не содержит «%s»",
                    element.getText(),
                    element,
                    value
                )
            )
            .until(textToBePresentInElementValue(element, value));
        return this;
    }

    @Step("Значение элемента: «{0}» должено совпадать с «{1}»")
    public DefaultSteps shouldHasValue(MailElement element, String value) {
        standardProgressiveWait(webDriverRule, 10)
            .withMessage(
                String.format(
                    "Текст в value «%s» элемента «%s» не совпадает с «%s»",
                    element.getText(),
                    element,
                    value
                )
            )
            .until(attributeToBe(element, "value", value));
        return this;
    }

    @Step("Значение title элемента: «{0}» должено совпадать с «{1}»")
    public DefaultSteps shouldHasTitle(MailElement element, String title) {
        standardProgressiveWait(webDriverRule, 10)
            .withMessage(
                String.format(
                    "Текст в title «%s» элемента «%s» не совпадает с «%s»",
                    element.getAttribute("title"),
                    element,
                    title
                )
            )
            .until(attributeToBe(element, "title", title));
        return this;
    }

    @Step("Значение аттрибута «{1}» элемента «{0}» должено совпадать с «{2}»")
    public DefaultSteps shouldContainsAttribute(WebElement element, String attribute, String value) {
        standardProgressiveWait(webDriverRule, 10)
            .until(attributeContains(element, attribute, value));
        return this;
    }

    @Step("Количество элементов в списке «{0}» должно быть «{1}»")
    public DefaultSteps shouldSeeElementsCount(List<? extends MailElement> elements, int size) {
        standardProgressiveWait(webDriverRule, 10)
            .withMessage(
                String.format(
                    "Количество элементов в списке %s вместо %s",
                    elements.size(),
                    size
                )
            )
            .until(numberOfElementsInListToBe(elements, size));
        return this;
    }

    @SkipIfFailed
    @Step("Вылогиниваемся")
    public void logsOut() {
        assumeStepCanContinue(webDriverRule.getDriver().manage().getCookieNamed("yandexuid"), is(not(nullValue())));
        PassportRule passportRule = new PassportRule(webDriverRule.getDriver());
        Cookie edaIdCookie = webDriverRule.getDriver().manage().getCookieNamed("Eda_id");
        UriBuilder urlBuilder = UriBuilder.fromPath("/").scheme("https").host("passport.yandex.ru");
        if (edaIdCookie != null) {
            urlBuilder = urlBuilder.path(edaIdCookie.getPath() + "/");
        }
        if (webDriverRule.getDriver().getCurrentUrl().contains(".com.tr")) {
            urlBuilder = urlBuilder.host("passport.yandex.com.tr");
        } else if (webDriverRule.getDriver().getCurrentUrl().contains(".com/")) {
            urlBuilder = urlBuilder.host("passport.yandex.com");
        }
        try {
            passportRule.onHost(urlBuilder.build().toURL()).logoff();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Mailformed url", e);
        }
    }

    @Step("Открываем новую вкладку и переключаемся в нее")
    public MultipleWindowsHandler opensNewWindowAndSwitchesOnIt() {
        return new MultipleWindowsHandler(webDriverRule.getDriver(), webDriverRule.getBaseUrl());
    }

    @Step("Переходим в главное окно")
    public DefaultSteps switchesOnMainWindow(MultipleWindowsHandler windowsHandler) {
        windowsHandler.switchToParent();
        return this;
    }

    //---------------------------------------------------------
    // PASSPORT PAGE STEPS
    //---------------------------------------------------------

    @Step("Создаём нового юзера через passport.api")
    public UserWithProps createsNewUser() {
        return Utils.createNewUser();
    }

    @Step("Открываем URL по умолчанию с доменом «{0}»")
    public DefaultSteps opensDefaultUrlWithDomain(String domain) {
        String url = substringBeforeLast(webDriverRule.getBaseUrl(), ".") + "." + domain;
        opensUrl(url);
        return this;
    }

    @Step("Переходим на URL по умолчанию")
    public DefaultSteps opensDefaultUrl() {
        webDriverRule.getDriver().get(webDriverRule.getBaseUrl());
        return this;
    }

    /**
     * Метод не рефрешит страницу, в большинстве случаев переход осуществляется без него, поэтому не стоит рассчитывать
     * что какие-то данные подтянутся после него
     */
    @Step("Переходим в «{0}»")
    public DefaultSteps opensFragment(QuickFragments fragment) {
        opensFragment(fragment.fragment());
        return this;
    }

    @Step("Переходим в «{0}» и рефрешим страницу")
    public DefaultSteps opensFragmentWithRefresh(QuickFragments fragment) {
        opensFragment(fragment.fragment());
        refreshPage();
        return this;
    }

    @Step("Переходим в «{0}»")
    public DefaultSteps opensFragment(String fragment) {
        String newUrl = fromUri(webDriverRule.getDriver().getCurrentUrl()).fragment(fragment).build().toString();
        opensUrl(newUrl);
        return this;
    }

    @Step("Переходим по урлу «{0}»")
    public DefaultSteps opensUrl(String url) {
        webDriverRule.getDriver().get(url);
        return this;
    }

    @Step("Окно с инексом: «{1}» должно содержать URL: «{0}»")
    public void shouldSeeNewWindowOpenedWithUrl(String url, int windowIndex) {
        switchOnWindow(windowIndex);
        shouldBeOnUrl(url);
    }

    @Step("Переходим в окно с индексом: «{0}»")
    public DefaultSteps switchOnWindow(int windowIndex) {
        String[] handles = webDriverRule.getDriver().getWindowHandles().toArray(new String[windowIndex + 1]);
        webDriverRule.getDriver().switchTo().window(handles[windowIndex]);
        return this;
    }

    @Step("Переходим в открывшееся окно")
    public DefaultSteps switchOnJustOpenedWindow() {
        waitInSeconds(1);
        String[] handles = webDriverRule.getDriver().getWindowHandles().toArray(
            new String[webDriverRule.getDriver().getWindowHandles().size() - 1]
        );
        webDriverRule.getDriver().switchTo().window(handles[webDriverRule.getDriver().getWindowHandles().size() - 1]);
        return this;
    }

    @Step("Закрываем текущее окно")
    public DefaultSteps closeOpenedWindow() {
        webDriverRule.getDriver().close();
        switchOnJustOpenedWindow();
        return this;
    }

    @Step("Должны быть на URL который содержит: {0}")
    public DefaultSteps shouldBeOnUrlWith(QuickFragments fragment) {
        shouldBeOnUrl(allOf(containsString(fragment.fragment())));
        return this;
    }

    @Step("Открываем “http://yandex.{0}“")
    public DefaultSteps opensMordaUrlWithDomain(String domain) {
        opensUrl("http://yandex." + domain);
        return this;
    }

    @Step("Проверяем, что находимся на URL: «{0}»")
    public DefaultSteps shouldBeOnUrl(Matcher<? super String> urlMatcher) {
        assertThat("Текущий урл не подходит матчеру", webDriverRule.getDriver(), currentUrl(urlMatcher));
        return this;
    }

    @Step("Проверяем, что находимся на URL: «{1}»")
    public DefaultSteps shouldBeOnUrl(Account acc, QuickFragments quickFragments) {
        shouldBeOnUrl(acc, quickFragments.makeUrlPart());
        return this;
    }

    @Step("Проверяем, что находимся на URL: «{2}»")
    public DefaultSteps shouldBeOnUrl(Account acc, String prefix, QuickFragments quickFragments) {
        String url = format(
            "%s/%s/?uid=%s%s",
            webDriverRule.getBaseUrl(),
            prefix,
            getUserUid(acc.getLogin()),
            quickFragments.makeUrlPart()
        );
        shouldBeOnUrlNotDiffWith(url, param("ncrnd").ignore(), scheme("http", "https"));
        return this;
    }

    @Step("Проверяем, что находимся на URL: «{1}»")
    public DefaultSteps shouldBeOnUrl(Account acc, String quickFragments) {
        String url = format(
            "%s/?uid=%s%s",
            webDriverRule.getBaseUrl(),
            getUserUid(acc.getLogin()),
            quickFragments
        );
        shouldBeOnUrlNotDiffWith(url, param("ncrnd").ignore(), scheme("http", "https"));
        return this;
    }

    @Step("Проверяем, что находимся на URL: “{1}?{2}“")
    public DefaultSteps shouldBeOnUrl(Account acc, QuickFragments quickFragments, String additionalParam) {
        String url = format(
            "%s/?uid=%s%s?%s",
            webDriverRule.getBaseUrl(),
            getUserUid(acc.getLogin()),
            quickFragments.makeUrlPart(),
            additionalParam
        );
        shouldBeOnUrlNotDiffWith(url, param("ncrnd").ignore(), scheme("http", "https"));
        return this;
    }

    @Step("Должны быть на URL: «{0}»")
    public DefaultSteps shouldBeOnUrl(String url) {
        assertEquals(
            "Переход на неверный урл",
            substringAfter(url, "://"),
            substringAfter(webDriverRule.getDriver().getCurrentUrl(), "://")
        );
        return this;
    }

    @Step("Должны быть на URL: «{0}» без «{1}»")
    public DefaultSteps shouldBeOnUrlNotDiffWith(String url, UriDiffFilter... filters) {
        assertThat("Переход на неверный урл", webDriverRule.getDriver(), currentUriSameAs(url, filters));
        return this;
    }

    @Step("Текущий url должен содержать текст «{0}»")
    public DefaultSteps shouldContainTextInUrl(String text) {
        assertThat(
            String.format(
                "Текущий url «%s» не содержит текст «%s»",
                webDriverRule.getDriver().getCurrentUrl(),
                text
            ),
            webDriverRule.getDriver(),
            withWaitFor(containsInCurrentUrl(text))
        );
        return this;
    }

    @Step("Переходим на дефолтный URL + {0}")
    public DefaultSteps opensDefaultUrlWithPostFix(String url) {
        opensUrl(webDriverRule.getBaseUrl() + url);
        return this;
    }

    @Step("Переходим на текущий URL + {0}")
    public DefaultSteps opensCurrentUrlWithPostFix(String url) {
        String currentUrl = fromUri(webDriverRule.getDriver().getCurrentUrl()).replacePath(url).build().toString();
        opensUrl(currentUrl);
        return this;
    }

    @Step("Сохраняем текущий URL")
    public String getsCurrentUrl() {
        return webDriverRule.getDriver().getCurrentUrl();
    }

    @Step("Обновляем страницу")
    public DefaultSteps refreshPage() {
        webDriverRule.getDriver().navigate().refresh();
        return this;
    }

    @Step("Выполняем JavaScript в консоли")
    public DefaultSteps executesJavaScript(String script) {
        JavascriptExecutor jsExecutor = (JavascriptExecutor) webDriverRule.getDriver();
        jsExecutor.executeScript(script);
        return this;
    }

    @Step("Выполняем JavaScript в консоли")
    public DefaultSteps executesJavaScript(String script, Object... arg) {
        JavascriptExecutor jsExecutor = (JavascriptExecutor) webDriverRule.getDriver();
        jsExecutor.executeScript(script, arg);
        return this;
    }

    @Step("Выполняем JavaScript в консоли")
    public String executesJavaScriptWithResult(String script) {
        JavascriptExecutor jsExecutor = (JavascriptExecutor) webDriverRule.getDriver();
        return (String) jsExecutor.executeScript(script);
    }

    @Step("Ждём {0} секунд")
    public DefaultSteps waitInSeconds(double time) {
        try {
            Thread.sleep(Math.round(time * 1000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return this;
    }

    @Step("Размер окна браузера (Ширина: {0}, Высота: {1})")
    public DefaultSteps setsWindowSize(int x, int y) {
        webDriverRule.getDriver().manage().window().setSize(new Dimension(x, y));
        return this;
    }

    @Step("Элемент: {0} должен быть задизейблен.")
    public DefaultSteps shouldBeDisabled(WebElement webElement) {
        standardProgressiveWait(webDriverRule, 10)
            .withMessage("Элемент должен быть задизейблен")
            .until(ExpectedConditions.not(elementIsEnabled(webElement)));
        return this;
    }

    @Step("Элемент: {0} должен быть активным.")
    public DefaultSteps shouldBeEnabled(WebElement webElement) {
        standardProgressiveWait(webDriverRule, 10)
            .withMessage("Элемент должен быть активен")
            .until(elementIsEnabled(webElement));
        return this;
    }

    @Step("Элементы: {0} должны быть задизейблены.")
    public DefaultSteps shouldBeDisabled(WebElement... webElements) {
        for (WebElement webElement : webElements) {
            shouldBeDisabled(webElement);
        }
        return this;
    }

    @Step("Элементы: {0} должны быть активными.")
    public DefaultSteps shouldBeEnabled(WebElement... webElements) {
        for (WebElement webElement : webElements) {
            shouldBeEnabled(webElement);
        }
        return this;
    }

    @Step("Элемент: {0} должен быть задизейблен.")
    public DefaultSteps shouldBeDisabled(MailElement... mailElement) {
        assertThat(
            "Каждая из списка островная кнопка должна быть отключена",
            asList(mailElement),
            everyItem(disabledButton())
        );
        return this;
    }

    @Step("Элемент: {0} должен быть активным.")
    public DefaultSteps shouldBeEnabled(MailElement... mailElement) {
        assertThat(
            "Каждая из списка островная кнопка должна быть активна",
            asList(mailElement),
            everyItem(enabledButton())
        );
        return this;
    }

    @Step("Открываем lite-версию")
    public void openLightMail() {
        opensDefaultUrlWithPostFix("/lite/inbox");
    }

    @Step("Кликаем по элементу «{0}», вводим «{1}»")
    public DefaultSteps clicksAndInputsText(WebElement elemInput, String text) {
        clicksOn(elemInput);
        inputsTextInElement(elemInput, text);
        return this;
    }

    public static Sign sign(String signText) {
        return new Sign().withText(signText);
    }

    public static Account acc(String login, String pwd) {
        return new Account(login, pwd);
    }

    @Step("Запрос должен содержать следующие обязательные параметры {1}")
    public DefaultSteps shouldBeParamsInRequest(JsonObject encodedParameters, Map<String, ?> pairs) {
        for (Map.Entry<String, ?> entry : pairs.entrySet()) {
            String paramName = StringUtils.substringBefore(entry.getKey(), ".");
            String paramValue = entry.getValue().toString();
            String encodedParam = encodedParameters.get(paramName).toString();
            assertThat(
                "Значение параметра \"" + paramValue + "\" не совпадает с данными в запросе " + encodedParam,
                encodedParam.contains(paramValue)
            );
        }
        return this;
    }

    @Step("Наводим мышку на элемент «{0}»")
    public DefaultSteps onMouseHover(WebElement element) {
        shouldSee(element);
        new Actions(webDriverRule.getDriver()).moveToElement(element).perform();
        return this;
    }

    @Step("Ховер на «{0}» со смещением от левого верхнего края элемента на («{1}»,«{2}») ")
    public DefaultSteps offsetHover(WebElement element, int x, int y) {
        shouldSee(element);
        new Actions(webDriverRule.getDriver()).moveToElement(element, x, y).perform();
        return this;
    }

    @Step("Наводим мышку на элемент «{0}»")
    public DefaultSteps shouldSeeWithHover(WebElement element) {
        new Actions(webDriverRule.getDriver()).moveToElement(element).perform();
        shouldSee(element);
        return this;
    }

    @Step("Наводим мышку на элемент «{0}» и кликаем по нему")
    public DefaultSteps onMouseHoverAndClick(WebElement element) {
        new Actions(webDriverRule.getDriver()).moveToElement(element).click().perform();
        return this;
    }

    @Step("Драг-н-дроп «{0}» на «{1}»")
    public DefaultSteps dragAndDrop(WebElement draggable, WebElement target) {
        shouldSee(draggable);
        shouldSee(target);
        new Actions(webDriverRule.getDriver()).dragAndDrop(draggable, target).perform();
        return this;
    }

    @Step("Удаляем настройки «{0}»")
    public DefaultSteps deleteSettings(String... settings) {
        String settingsToDelete = Joiner.on(",").join(settings);
        Cookie c = new Cookie("debug-settings-delete", settingsToDelete, "mail.yandex.ru", null, null);
        webDriverRule.getDriver().manage().addCookie(c);
        refreshPage();
        return this;
    }

    @Step("Добавляем эксперименты «{0}»")
    public DefaultSteps addExperiments(String... expNumbers) {
        final String EMPTY_JSON_STRING = "W3siSEFORExFUiI6Ik1BSUwiLCJDT05URVhUIjp7Ik1BSUwiOnsiZmxhZ3MiOlsie30iXX19fV0=";
        Map<String, String> cookies = new HashMap<>();

        for (String expNumber : expNumbers) {
            cookies.put(expNumber, EMPTY_JSON_STRING);
        }
        addExperimentsWithJson(cookies);
        return this;
    }

    @Step("Добавляем эксперименты с их json-ами «{0}»")
    public DefaultSteps addExperimentsWithJson(Map<String, String> cookies) {
        final String EXPERIMENT_COOKIE_NAME = "debug-experiments";
        final String EXPERIMENT_JSON_COOKIE_NAME = "debug-experiments-json";
        final String EXPERIMENT_COOKIE_SEPARATOR = "%3B";
        final String EXPERIMENT_JSON_COOKIE_SEPARATOR = ",";
        final String EXPERIMENT_JSON_KEY_COOKIE_NAME = "debug-experiments-json-key";
        String cookieHost = getCookieHost();
        log(webDriverRule.getDriver().getCurrentUrl());
        log("Кука эксперимента на домен " + cookieHost);
        removeAllYexpExperiments();
        for (Map.Entry<String, String> cookie : cookies.entrySet()) {
            String experimentString = cookie.getKey();
            String experimentJsonString = cookie.getValue();
            if (webDriverRule.getDriver().manage().getCookieNamed(EXPERIMENT_COOKIE_NAME) != null) {
                experimentString =
                    webDriverRule.getDriver().manage().getCookieNamed(EXPERIMENT_COOKIE_NAME).getValue() +
                        EXPERIMENT_COOKIE_SEPARATOR +
                        experimentString;
                experimentJsonString =
                    webDriverRule.getDriver().manage().getCookieNamed(EXPERIMENT_JSON_COOKIE_NAME).getValue() +
                        EXPERIMENT_JSON_COOKIE_SEPARATOR +
                        experimentJsonString;
            }
            Cookie c = new Cookie(EXPERIMENT_COOKIE_NAME, experimentString, cookieHost, "/", null);
            webDriverRule.getDriver().manage().addCookie(c);
            c = new Cookie(EXPERIMENT_JSON_COOKIE_NAME, experimentJsonString, cookieHost, "/", null);
            webDriverRule.getDriver().manage().addCookie(c);
            webDriverRule.getDriver().manage().addCookie(user.loginSteps().getYandexuidCookie());
            Cookie yandexuid = webDriverRule.getDriver().manage().getCookieNamed("yandexuid");
            final String hashed = Hashing.sha256()
                .hashString(yandexuid.getValue() + ":" + experimentJsonString, Charsets.UTF_8).toString();
            c = new Cookie(EXPERIMENT_JSON_KEY_COOKIE_NAME, hashed, cookieHost, "/", null);
            webDriverRule.getDriver().manage().addCookie(c);
            refreshPage();
        }
        return this;
    }

    @Step("Добавляем эксперимент(ы) «{0}» при помощи куки «yexp»")
    public DefaultSteps addExperimentsWithYexp(String... expNums) {
        if (webDriverRule.getDriver().getCurrentUrl().contains("yandex-team")) {
            return this;
        }
        String cookieHost = getCookieHost();
        log(webDriverRule.getDriver().getCurrentUrl());
        log("Кука эксперимента на домен " + cookieHost);
        if (expNums == null || expNums.length == 0) {
            return this;
        }
        this.user.putExperiments(expNums);

        final int MAX_RETRIES_NUM = 2;
        final String EXPERIMENT_COOKIE_NAME = "yexp";
        String yexpValue = "";
        String testId = String.join("_", user.getExperiments());
        boolean experimentsAreUpToDate = false;
        for (int retriesCounter = 0; retriesCounter < MAX_RETRIES_NUM; retriesCounter++) {
            RequestSpecification request = RestAssured.given();
            log("Эксперименты, которые добавляем: " + mapExperiments(user.getExperiments()));
            Response response = request.post("https://yandex.ru/ecoo/sign?test-id=" + testId + "&ts=1800000000");
            Assert.assertEquals("Запрос за кукой «yexp» завершился с ошибкой", response.getStatusCode(), 200);
            yexpValue = response.getBody().asString();

            Cookie c = new Cookie(EXPERIMENT_COOKIE_NAME, yexpValue, cookieHost, "/", null);
            webDriverRule.getDriver().manage().addCookie(c);
            refreshPage();
            if ((experimentsAreUpToDate = experimentsAreUpToDate())) {
                break;
            }
        }
        if (!experimentsAreUpToDate) {
            log("WARNING: Эксперименты не были успешно проставлены");
        }
        return this;
    }

    @Step("Добавляем эксперимент(ы) «{1}»  для домена {0} при помощи куки «yexp»")
    public DefaultSteps addExperimentsWithYexpToDomain(YandexDomain domain, String... expNums) {
        if (expNums == null || expNums.length == 0) {
            return this;
        }
        this.user.putExperiments(expNums);

        final int MAX_RETRIES_NUM = 2;
        final String EXPERIMENT_COOKIE_NAME = "yexp";
        String yexpValue = "";
        String testId = String.join("_", user.getExperiments());
        boolean experimentsAreUpToDate = false;
        for (int retriesCounter = 0; retriesCounter < MAX_RETRIES_NUM; retriesCounter++) {
            RequestSpecification request = RestAssured.given();
            log("Эксперименты, которые добавляем: " + mapExperiments(user.getExperiments()));
            Response response = request.post("https://yandex.ru/ecoo/sign?test-id=" + testId + "&ts=1800000000");
            Assert.assertEquals("Запрос за кукой «yexp» завершился с ошибкой", response.getStatusCode(), 200);
            yexpValue = response.getBody().asString();

            Cookie c = new Cookie(EXPERIMENT_COOKIE_NAME, yexpValue, ".yandex." + domain, "/", null);
            webDriverRule.getDriver().manage().addCookie(c);
            refreshPage();
            if ((experimentsAreUpToDate = experimentsAreUpToDate())) {
                break;
            }
        }
        if (!experimentsAreUpToDate) {
            log("WARNING: Эксперименты не были успешно проставлены");
        }
        return this;
    }

    private String getCookieHost() {
        String cookieHost = null;
        try {
            String host = new URI(webDriverRule.getDriver().getCurrentUrl()).getHost();
            cookieHost = host.substring(host.lastIndexOf(".yandex."));
        } catch (URISyntaxException e) {
            log(e.getMessage());
            cookieHost = ".yandex.ru";
        }
        return cookieHost;
    }

    private Set<String> getCurrentExperiments() {
        JavascriptExecutor jsExecutor = (JavascriptExecutor) webDriverRule.getDriver();
        String jsResponse = (String) jsExecutor.executeScript(getExperimentsRequestString());
        String[] toDebug = Arrays.stream(jsResponse.split("\\s*;\\s*"))
            .map(exp -> substringBefore(exp, ","))
            .toArray(String[]::new);
        return new HashSet<>(Arrays.asList(toDebug));
    }

    @Step("Проверяем, нет ли лишних экспериментов")
    public boolean experimentsAreUpToDate() {
        Set<String> expsSetOnUser = getCurrentExperiments();
        for (String exp : expsSetOnUser) {
            if (!this.user.getExperiments().contains(exp)) {
                log("WARNING: Эксперименты, проставленные на юзере не совпадают с теми, которые должны быть " +
                    "установлены");
                log("Эксперименты из куки: " + mapExperiments(this.user.getExperiments()));
                log("Эксперименты, установленные на юзере: " + mapExperiments(expsSetOnUser));
                return false;
            }
        }
        log("SUCCESS: Все эксперименты соответсвуют юзеру");
        return true;
    }

    @Step("Копируем скриншот в системный буффер обмена")
    public DefaultSteps copyScreenInTransferBuffer() {
        BufferedImage screen = grabScreen();
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new ImageTransferable(screen), null);
        return this;
    }

    private String mapExperiments(Collection<String> exps) {
        return exps.stream().map(
            (String ex) -> (substringBefore(ex, ","))).collect(Collectors.joining(", ")
        );
    }

    private String getExperimentsRequestString() {
        switch (urlProps().getProject()) {
            case "liza":
                return "return Daria.Config[\"eexp-boxes\"]";
            case "touch":
                return "return __CONFIG[\"eexp-boxes\"]";
            case "cal":
                return "return Maya.config[\"experiments\"][\"eexp\"]";
            default:
                return "";
        }
    }

    @Step("Снимаем с пользователя все эксперименты из куки «yexp»")
    public DefaultSteps removeAllYexpExperiments() {
        webDriverRule.getDriver().manage().deleteCookieNamed("yexp");
        this.user.clearExperiments();
        refreshPage();
        return this;
    }

    @Step("Скроллим к элементу «{0}»")
    public DefaultSteps scrollTo(MailElement element) {
        shouldSee(element);
        element.executeScript("arguments[0].scrollIntoView(false);");
        return this;
    }

    @Step("Скроллим к элементу «{0}»")
    public DefaultSteps scrollToInvisibleElement(MailElement element) {
        element.executeScript("arguments[0].scrollIntoView(false);");
        return this;
    }

    @Step("Скроллим элемент «{0}» к верхушке вьюпорта")
    public DefaultSteps scrollElementToTopOfView(MailElement element) {
        shouldSee(element);
        element.executeScript("arguments[0].scrollIntoView(true);");
        return this;
    }

    @Step("Скроллим вниз элемент {0}")
    public DefaultSteps scrollDown(MailElement element) {
        shouldSee(element);
        element.executeScript("$(arguments[0]).scrollTop(5000)");
        return this;
    }

    @Step("Клик по элементу «{0}» без проверки на видимость")
    public DefaultSteps forceClickOn(WebElement element) {
        new Actions(webDriverRule.getDriver()).click(element).perform();
        return this;
    }

    public DefaultSteps clicksOnElementWithWaiting(final WebElement element) {
        standardProgressiveWait(webDriverRule, 10)
            .withMessage("Элемент «" + element + "» не найден!")
            .until(elementToBeClickable(element));
        element.click();
        return this;
    }

    @Step("Клик по: {0}")
    public DefaultSteps scrollAndClicksOn(MailElement... elements) {
        for (MailElement element : elements) {
            scrollTo(element);
            clicksOnElementWithWaiting(element);
        }
        return this;
    }

    @Step("Переключаемся в iframe: {0}")
    public DefaultSteps switchTo(String iframeCssSelector) {
        webDriverRule.getDriver().switchTo().frame(
            webDriverRule.getDriver().findElement(By.cssSelector(iframeCssSelector))
        );
        return this;
    }

    @Step("CSS-аттрибут «{1}» элемента «{0}» должен содержать «{2}»")
    public DefaultSteps shouldContainCSSAttributeWithValue(WebElement element, String name, String value) {
        standardProgressiveWait(webDriverRule, 7)
            .withMessage(String.format("Элемент должен иметь стиль «%s=%s»", name, value))
            .until(attributeToBe(element, name, value));
        return this;
    }

    @Step("Проверяем, что элемент «{0}» во вьюпорте")
    public DefaultSteps shouldSeeInViewport(MailElement element) {
        shouldSee(element);
        standardWait(webDriverRule, 5)
            .withMessage(String.format("Элемент «%s» находится вне вьюпорта", element))
            .until(elementIsInViewport(element));
        return this;
    }

    @Step("Проверяем, что элемента «{0}» нет во вьюпорте")
    public DefaultSteps shouldNotSeeInViewport(MailElement element) {
        standardWait(webDriverRule, 5)
            .withMessage(String.format("Элемент «%s» находится во вьюпорте", element))
            .until(ExpectedConditions.not(elementIsInViewport(element)));
        return this;
    }

    @Step("Выделяем текст в {0} с символа {1} до символа {2}")
    public DefaultSteps selectText(WebElement element, int from, int to) {
        Actions selectAction = setCaretToIndexAction(element, from);
        for (int i = from; i < to; i++) {
            selectAction.keyDown(Keys.SHIFT)
                .sendKeys(Keys.RIGHT)
                .keyUp(Keys.SHIFT);
        }
        selectAction.perform();
        return this;
    }

    @Step("Формируем экшн установки каретки в элемент {0} по индексу {1}")
    public Actions setCaretToIndexAction(WebElement element, int index) {
        Actions setAction = new Actions(webDriverRule.getDriver()).moveToElement(element, 0, 0).click();
        for (int i = 0; i < index; i++) {
            setAction.sendKeys(Keys.RIGHT);
        }
        return setAction;
    }

    @Step("Проверяем, что имя скачанного аттача - {0}, а его размер - {1}")
    public DefaultSteps checkDownloadedFileNameAndSize(String fileName, String fileSize) {
        opensUrl("chrome://downloads/");
        switchOnJustOpenedWindow();
        standardWait(webDriverRule, 20)
            .until(jsScriptRunsAsExpected(CHECK_DOWNLOAD_STATE_SCRIPT, "COMPLETE"));
        String pathToDownloadedFile = executesJavaScriptWithResult(GET_DOWNLOADED_FILEPATH_SCRIPT);
        System.out.println(pathToDownloadedFile);
        opensUrl("file://" + pathToDownloadedFile.substring(0, pathToDownloadedFile.lastIndexOf('/')))
            .switchOnJustOpenedWindow();
        shouldSeeElementInList(user.pages().chromeFileBrowserPage().downloadedFiles(), fileName);
        shouldSeeThatElementHasText(user.pages().chromeFileBrowserPage().fileSize(fileName), fileSize);
        return this;
    }

    @Step("Проверяем, что имя скачанного архива содержит - {0}, а его размер - {1}")
    public DefaultSteps checkDownloadedArchiveNameAndSize(String fileName, String fileSize) {
        opensUrl("chrome://downloads/");
        switchOnJustOpenedWindow();
        standardWait(webDriverRule, 20)
            .until(jsScriptRunsAsExpected(CHECK_DOWNLOAD_STATE_SCRIPT, "COMPLETE"));
        String pathToDownloadedFile = executesJavaScriptWithResult(GET_DOWNLOADED_FILEPATH_SCRIPT);
        System.out.println(pathToDownloadedFile);
        opensUrl("file://" + pathToDownloadedFile.substring(0, pathToDownloadedFile.lastIndexOf('/')))
            .switchOnJustOpenedWindow();
        shouldSeeThatElementHasText(user.pages().chromeFileBrowserPage().downloadedFiles().get(0), fileName);
        String name = user.pages().chromeFileBrowserPage().downloadedFiles().get(0).getText();
        shouldSeeThatElementHasText(user.pages().chromeFileBrowserPage().fileSize(name), fileSize);
        shouldSeeThatElementHasText(user.pages().chromeFileBrowserPage().downloadedFiles().get(0), ".zip");
        return this;
    }

    @Step("Получаем путь до аттача")
    public String getAttachPath(String attachName) {
        String path;
        webDriverRule.getRemoteDriver().setFileDetector(new LocalFileDetector());
        URL resource = getClass().getClassLoader().getResource("attach/" + attachName);
        try {
            path = Paths.get(Objects.requireNonNull(resource).toURI()).toAbsolutePath().toString();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException("Не удалось считать файл " + attachName);
        }
        return path;
    }

    @Step("Переключаем язык на {0}")
    public DefaultSteps switchLanguage(String lang) {
        String uid = getUserUid(webDriverRule.getDriver().manage().getCookieNamed("yandex_login").getValue());
        String days = String.valueOf((int) (System.currentTimeMillis() / (1000 * 24 * 60 * 60)));
        String secretKey = DigestUtils.md5Hex(uid + "::" + days);
        opensUrl(String.format(
            "https://yandex.ru/portal/set/lang/?intl=%s&retpath=%s&sk=u%s", //u добавляется в начале secretKey для авторизованных юзеров
            lang,
            URLEncoder.encode(webDriverRule.getDriver().getCurrentUrl()),
            secretKey
            )
        );
        return this;
    }

    @Step("Делаем скриншот")
    private static BufferedImage grabScreen() {
        try {
            return new Robot().createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
        } catch (SecurityException e) {
        } catch (AWTException e) {
        }
        return null;
    }

    public static final class ImageTransferable implements Transferable {
        final BufferedImage image;

        public ImageTransferable(final BufferedImage image) {
            this.image = image;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.imageFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(final DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(final DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (isDataFlavorSupported(flavor)) {
                return image;
            }

            throw new UnsupportedFlavorException(flavor);
        }
    }
}
