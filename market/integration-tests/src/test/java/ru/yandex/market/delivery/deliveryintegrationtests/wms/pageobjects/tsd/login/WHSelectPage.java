package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.login;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class WHSelectPage extends AbstractTsdPage {

    @FindBy(xpath = "//button[text() = '0 - INFOR_SCPRD_wmwhse1']")
    private HtmlElement whbutton;

    private String captionXPath = "//div[text()='Выбор склада']";

    public WHSelectPage(WebDriver driver) {
        super(driver);
    }

    public boolean isDisplayed() { return driver.findElements(By.xpath(captionXPath)).size() != 0; }

    @Step("Выбираем склад INFOR_SCPRD_wmwhse1")
    public void SelectWarehouse () {
        whbutton.click();
    }
}
