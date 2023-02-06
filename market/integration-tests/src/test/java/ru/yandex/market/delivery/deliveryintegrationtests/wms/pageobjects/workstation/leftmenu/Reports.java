package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.leftmenu;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class Reports extends AbstractWsPage {

    @Name("Отчеты")
    @FindBy(xpath = "//div[@id = '$ozejcc']")
    private HtmlElement reports;

    public Reports(WebDriver driver) {
        super(driver);
    }

    @Step("Открываем Отчеты")
    public void reports() {
        safeClick(reports);
    }

}
