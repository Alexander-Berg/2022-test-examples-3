package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.move;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class MoveMenu extends AbstractTsdPage {
    private WebDriver driver;

    @Name("1 Перемещ. по S/N")
    @FindBy(xpath = "//button[text() = 'Перемещ. по S/N']")
    private HtmlElement extractSnButton;

    @Name("2 Размещ. по S/N")
    @FindBy(xpath = "//button[text() = 'Размещ. по S/N']")
    private HtmlElement putSnButton;

    public MoveMenu(WebDriver driver) {
        super(driver);
    }

    @Step("Меню: Приёмка")
    public void extractSnButtonCLick() {
        extractSnButton.click();
    }

    @Step("Меню: Приёмка брака")
    public void putSnButtonClick() {
        putSnButton.click();
    }
}
