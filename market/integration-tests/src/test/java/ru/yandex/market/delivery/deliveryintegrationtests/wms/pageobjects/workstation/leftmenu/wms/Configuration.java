package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.leftmenu.wms;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class Configuration extends AbstractWsPage {

    @Name("Меню Товар")
    @FindBy(xpath = "//div[@id = '$izoiv6']")
    private HtmlElement product;

    public Configuration(WebDriver driver) {
        super(driver);
    }

    @Step("Выбираем меню: Товар")
    public void product() {
        safeClick(product);
    }
}
