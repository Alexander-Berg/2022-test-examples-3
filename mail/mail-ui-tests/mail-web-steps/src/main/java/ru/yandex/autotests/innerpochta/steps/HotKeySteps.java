package ru.yandex.autotests.innerpochta.steps;

import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;
import ru.yandex.autotests.innerpochta.util.KeysOwn;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.List;

import static ch.lambdaj.Lambda.filter;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

public class HotKeySteps {

    private AllureStepStorage user;
    private WebDriverRule webDriverRule;

    HotKeySteps(WebDriverRule webDriverRule, AllureStepStorage user) {
        this.webDriverRule = webDriverRule;
        this.user = user;
    }

    private MailElement destination;

    @Step("Нажимаем клавишу: {0}")
    public HotKeySteps pressSimpleHotKey(KeysOwn key) {
        pressHotKeys(Keys.chord(key.key()));
        return this;
    }

    @Step("Нажимаем клавишу: {0}")
    public HotKeySteps pressSimpleHotKey(MailElement dest, KeysOwn key) {
        pressHotKeysWithDestination(dest, Keys.chord(key.key()));
        return this;
    }

    @Step("Нажимаем клавишу: {0}")
    public HotKeySteps pressSimpleHotKey2(KeysOwn key) {
        destination.sendKeys(Keys.chord(key.key()));
        return this;
    }

    @Step("Нажимаем клавишу «{0}»")
    public HotKeySteps pressSimpleHotKey(String key) {
        pressHotKeys(key);
        return this;
    }

    @Step("Нажимаем комбинацию клавиш «{0}» + «{1}»")
    public HotKeySteps pressCombinationOfHotKeys(KeysOwn firstKey, String secondKey) {
        pressHotKeys(Keys.chord(firstKey.key(), secondKey));
        return this;
    }

    @Step("Открываем новый таб")
    public HotKeySteps openBrowserTabWithHotkey() {
        pressCombinationOfHotKeys(key(Keys.CONTROL), "t");
        return this;
    }

    @Step("Закрываем таб браузера")
    public HotKeySteps closeBrowserTabWithHotkey() {
        pressCombinationOfHotKeys(key(Keys.CONTROL), "w");
        return this;
    }

    @Step("Нажимаем комбинацию «{0}» + «{1}»")
    public HotKeySteps pressCombinationOfHotKeys(KeysOwn firstKey, KeysOwn secondKey) {
        pressHotKeys(Keys.chord(firstKey.key(), secondKey.key()));
        return this;
    }

    @Step("Нажимаем комбинацию «{1}» + «{2}» (0)")
    public HotKeySteps pressCombinationOfHotKeys(MailElement dest, KeysOwn firstKey, KeysOwn secondKey) {
        pressHotKeysWithDestination(dest, Keys.chord(firstKey.key(), secondKey.key()));
        return this;
    }

    @Step("Нажимаем комбинацию «{1}» + «{2}» (0)")
    public HotKeySteps pressCombinationOfHotKeys(MailElement dest, KeysOwn firstKey, String secondKey) {
        pressHotKeysWithDestination(dest, Keys.chord(firstKey.key(), secondKey));
        return this;
    }

    @Step("Кликаем на элемент {0} удерживая Ctrl")
    public HotKeySteps clicksOnElementHoldingCtrlKey(MailElement element) {
        Actions act = new Actions(webDriverRule.getDriver());
        act.keyDown(Keys.CONTROL);
        act.click(element).build().perform();
        act.keyUp(Keys.CONTROL).build().perform();
        return this;
    }

    @Step("Кликаем на элемент {0} с текстом {1} удерживая Ctrl")
    public HotKeySteps clicksOnElementWithTextHoldingCtrlKey(List<? extends WebElement> elements, String text) {
        Actions act = new Actions(webDriverRule.getDriver());
        List<? extends WebElement> list = filter(hasText(containsString(text)), elements);
        assertThat("Нет элемента с нужным текстом", list, hasSize(greaterThan(0)));
        act.keyDown(Keys.CONTROL);
        act.click(list.get(0)).build().perform();
        act.keyUp(Keys.CONTROL).build().perform();
        return this;
    }

    @Step("Кликаем на сообщение {0} удерживая {1}")
    public HotKeySteps clicksOnMessageByNumberWhileHolding(KeysOwn key, int i) {
        Actions act = new Actions(webDriverRule.getDriver());
        act.keyDown(key.key());
        act.click(user.pages().MessagePage().displayedMessages().list().get(i).subject()).build().perform();
        act.keyUp(key.key()).build().perform();
        return this;
    }

    @Step("Кликаем на элемент {0} удерживая {1}")
    public HotKeySteps clicksOnElementWhileHolding(WebElement element, KeysOwn key) {
        Actions act = new Actions(webDriverRule.getDriver());
        act.keyDown(key.key());
        act.click(element).build().perform();
        act.keyUp(key.key()).build().perform();
        return this;
    }


    public HotKeySteps pressHotKeys(String keys) {
        setDefaultDestination();
        destination.sendKeys(keys);
        return this;
    }

    public HotKeySteps pressCalHotKeys(String keys) {
        setDefaultCalDestination();
        destination.sendKeys(keys);
        return this;
    }

    public HotKeySteps pressHotKeysWithDestination(MailElement dest, String keys) {
        this.destination = dest;
        destination.sendKeys(keys);
        return this;
    }

    public HotKeySteps pressHotKeys(MailElement element, Keys keys) {
        element.sendKeys(keys);
        return this;
    }

    public HotKeySteps pressHotKeys(MailElement element, String... keys) {
        element.sendKeys(keys);
        return this;
    }

    public HotKeySteps setSearchFieldForLabelsAsDestination() {
        setDefaultDestination();
        this.destination = user.pages().MessagePage().labelsDropdownMenu().searchField();
        return this;
    }

    private HotKeySteps setDefaultDestination() {
        this.destination = user.pages().HomePage().pageContent();
        return this;
    }

    private HotKeySteps setDefaultCalDestination() {
        this.destination = user.pages().HomePage().pageCalContent();
        return this;
    }

    public HotKeySteps setDestination(MailElement destination) {
        this.destination = destination;
        return this;
    }
}
