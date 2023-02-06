package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wms.execution.cyclicinventorization.initinventorization;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.Retrier;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.SeleniumUtil;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InitCyclicInventorizationPage extends AbstractWsPage {
    @Name("Кнопка Создать")
    @FindBy(xpath = "//div[@id = 'A1mxk2v']")
    private HtmlElement createButton;

    @Name("Кнопка Сохранить")
    @FindBy(xpath = "//div[@id = 'At0ujbx']")
    private HtmlElement saveButton;

    @Name("Кнопка Действия")
    @FindBy(xpath = "//div[@id = '$lvaodw']")
    private HtmlElement actionsButton;

    @Name("Кнопка Запустить")
    @FindBy(xpath = "//div[@id = '$j7win7']")
    private HtmlElement startButton;

    @Name("Ввод ячейка, начало")
    @FindBy(xpath = "//input[@id = 'Ilbdoyg']")
    private HtmlElement inputCellFrom;

    @Name("Ввод ячейка, конец")
    @FindBy(xpath = "//input[@id = 'Iicp5nt']")
    private HtmlElement inputCellTo;

    @Name("Выбираем действия для первой строчки")
    @FindBy(xpath = "//input[@id = '$wxvwyh_cell_0_0_Img']")
    private HtmlElement firstRowAction;

    public InitCyclicInventorizationPage(WebDriver driver) {
        super(driver);
    }

    @Step("Открываем меню создания заданий")
    public InitCyclicInventorizationPage createNewInventorizationTask() {
        createButton.click();
        overlayBusy.waitUntilHidden();
        return this;
    }

    @Step("Вводим ячейку, начало")
    public InitCyclicInventorizationPage inputCellFrom(String cellId) {
        Retrier.retry(() -> {
            inputCellFrom.click();
            SeleniumUtil.clearInput(inputCellFrom, driver);
            inputCellFrom.sendKeys(cellId);
            assertEquals(cellId, inputCellFrom.getAttribute("value"));
        }, 3, 10, TimeUnit.SECONDS);
        return this;
    }

    @Step("Вводим ячейку, конец")
    public InitCyclicInventorizationPage inputCellTo(String cellId) {
        Retrier.retry(() -> {
            inputCellTo.click();
            SeleniumUtil.clearInput(inputCellTo, driver);
            inputCellTo.sendKeys(cellId);
            assertEquals(cellId, inputCellTo.getAttribute("value"));
        }, 3, 10, TimeUnit.SECONDS);
        return this;
    }

    @Step("Сохраняем задание")
    public InitCyclicInventorizationPage save() {
        saveButton.click();
        overlayBusy.waitUntilHidden();
        return this;
    }

    @Step("Начинаем задание")
    public InitCyclicInventorizationPage selectFirstTask() {
        firstRowAction.click();
        overlayBusy.waitUntilHidden();
        return this;
    }

    public WebElement getActiveElement() {
        return driver.switchTo().activeElement();
    }
}
