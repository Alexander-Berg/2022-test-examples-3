package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.leftmenu.wms;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.leftmenu.wms.execution.CyclicInventorization;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.leftmenu.wms.execution.Productivity;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.leftmenu.wms.execution.Stocks;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

public class Execution extends AbstractWsPage {

    @Name("Меню Запасы")
    @FindBy(xpath = "//div[@id = '$d7zr5g']")
    private HtmlElement stocks;

    @Name("Меню Производительность/Трудозатраты")
    @FindBy(xpath = "//div[@id = '$5zxkdm']")
    private HtmlElement productivity;

    @Name("Меню Циклическая инвентаризация")
    @FindBy(xpath = "//div[@id = '$s13wgf']")
    private HtmlElement cyclicInventorization;

    public Execution(WebDriver driver) {
        super(driver);
    }

    @Step("Выбираем меню: Запасы")
    public Stocks stocks() {
        safeClick(stocks);
        return new Stocks(driver);
    }

    @Step("Выбираем меню: Производительность/Трудозатраты")
    public Productivity productivity() {
        safeClick(productivity);
        return new Productivity(driver);
    }

    @Step("Выбираем меню: Циклическая инвентаризация")
    public CyclicInventorization cyclicInventorization() {
        safeClick(cyclicInventorization);
        return new CyclicInventorization(driver);
    }
}
