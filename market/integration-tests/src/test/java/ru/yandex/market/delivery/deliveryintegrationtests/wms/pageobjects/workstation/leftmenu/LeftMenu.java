package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.leftmenu;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class LeftMenu extends AbstractWsPage {

    private static final Logger log = LoggerFactory.getLogger(LeftMenu.class);

    @Name("Меню WMS")
    @FindBy(xpath = "//span[@id = 'event_$jniyz2']")
    private HtmlElement wms;

    @Name("Меню Отчеты")
    @FindBy(xpath = "//span[@id = 'event_$9al8ic']")
    private HtmlElement reports;

    @Name("Меню WMS")
    @FindBy(xpath = "//span[@id = 'event_$s3os0o']")
    private HtmlElement wave;

    public LeftMenu(WebDriver driver) {
        super(driver);
    }

    @Step("Открываем меню WMS")
    public WMS WMS() {
        wms.click();
        return new WMS(driver);
    }

    @Step("Открываем меню Волна")
    public Wave Wave() {
        wave.click();
        return new Wave(driver);
    }

    @Step("Открываем меню Отчеты")
    public Reports Reports() {
        reports.click();
        return new Reports(driver);
    }
}
