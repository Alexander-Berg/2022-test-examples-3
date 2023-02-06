package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.outgoing.sorting;

import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;

public class SortingFinishedDialog extends AbstractTsdPage {

    @FindBy(xpath = "//button[text() = 'Да']")
    private HtmlElement yesBtn;

    @FindBy(xpath = "//button[text() = 'Нет']")
    private HtmlElement noBtn;

    public SortingFinishedDialog(WebDriver driver) {
        super(driver);
    }

    private Boolean isPresent() {

        String confirmationXpath = "//div[@class = 'inforDialog']//*[text()[contains(.,'Подтверждение')]]";

        String confirmationMessageXpath = "//div[@class = 'inforDialog']//*[text()[contains(.,'Сортировка тележки')] " +
                "and text()[contains(.,'Подтверждаете, что тележка пуста')]]";

        return isElementPresent(By.xpath(confirmationXpath))
                && isElementPresent(By.xpath(confirmationMessageXpath));
    }

    public SortingFinishedDialog shouldBePresent () {
        Assertions.assertTrue(isPresent(), "Должен был появиться диалог о завершении сортировки");
        return this;
    }

    @Step("Нажимаем Да в диалоге подтверждения")
    public void clickYes() {
        waitOverlayHidden();
        wait.until(elementToBeClickable(yesBtn));
        yesBtn.click();
        waitOverlayHidden();
    }

    @Step("Нажимаем Нет в диалоге подтверждения")
    public void clickNo() {
        waitOverlayHidden();
        wait.until(elementToBeClickable(noBtn));
        noBtn.click();
        waitOverlayHidden();
    }
}
