package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.leftmenu;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.leftmenu.wave.Execution;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.leftmenu.wave.Planning;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class Wave extends AbstractWsPage {

    @Name("Волна: Выполнение")
    @FindBy(xpath = "//div[@id = '$488p3m']")
    private HtmlElement execution;

    @Name("Волна: Планирование")
    @FindBy(xpath = "//div[@id = '$6lb4ta']")
    private HtmlElement planning;

    public Wave(WebDriver driver) {
        super(driver);
    }

    @Step("WMS: Выполнение")
    public Execution Execution() {
        safeClick(execution);
        return new Execution(driver);
    }

    @Step("WMS: Планирование")
    public Planning Planning() {
        safeClick(planning);
        return new Planning(driver);
    }
}
