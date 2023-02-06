package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.leftmenu.wms.execution;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class Stocks extends AbstractWsPage {

    @Name("Последовательная инвентаризация")
    @FindBy(xpath = "//div[@id = '$amwgiw']")
    private HtmlElement sequentialInventory;

    @Name("Балансы")
    @FindBy(xpath = "//div[@id = '$kdev1k']")
    private HtmlElement balances;

    @Name("Перемещения")
    @FindBy(xpath = "//div[@id = '$i1h1ia']")
    private HtmlElement transfers;

    @Name("Обслуживание блокировок")
    @FindBy(xpath = "//div[@id = '$o91yao']")
    private HtmlElement blockings;

    public Stocks(WebDriver driver) {
        super(driver);
    }

    @Step("Выбираем меню: Последовательная инвентаризаци")
    public HtmlElement sequentialInventory() {
        safeClick(sequentialInventory);
        return sequentialInventory;
    }

    @Step("Выбираем меню: Балансы")
    public HtmlElement balances() {
        safeClick(balances);
        return balances;
    }

    @Step("Выбираем меню: Перемещения")
    public void transfers() {
        safeClick(transfers);
    }

    @Step("Выбираем меню: Обслуживание блокировок")
    public void blockings() {
        safeClick(blockings);
    }
}
