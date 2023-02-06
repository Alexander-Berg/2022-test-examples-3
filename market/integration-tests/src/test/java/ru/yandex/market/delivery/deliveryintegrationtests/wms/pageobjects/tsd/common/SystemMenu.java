package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.common;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdElement;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;

public class SystemMenu extends AbstractTsdElement {

    @Name("0 ГлавнМеню")
    @FindBy(xpath = "//button[@id = 'homeButton']")
    private HtmlElement mainMenuButton;

    private static final Logger log = LoggerFactory.getLogger(SystemMenu.class);

    public SystemMenu(WebDriver driver) {
        super(driver);
    }

    @Step("Системное меню: Возврат в главное меню")
    public void backToMainMenu() {
        wait.until(elementToBeClickable(mainMenuButton));
        mainMenuButton.click();
    }

}
