package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wms.outgoing.packing;

import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;

public class PackingPage extends AbstractWsPage {

    private TableSelectDlg tableSelectDlg = new TableSelectDlg(driver);

    @Name("Фрейм упаковки")
    @FindBy(xpath = "//iframe[@id = '$z1g2l0']")
    private HtmlElement packingFrame;

    @Name("Сканируйте УИТ")
    @FindBy(xpath = "//vaadin-text-field[@class = 'pack-input']")
    private HtmlElement uitInput;

    private final String packTypeInputXpath =
            "//div[@class = 'draggable']//vaadin-text-field[@class = 'pack-input']";

    @Name("Сканируйте коробку")
    @FindBy(xpath = packTypeInputXpath)
    private HtmlElement packTypeInput;

    @Name("Кнопка OK в диалоге ввода коробки")
    @FindBy(xpath = "//div[@class = 'draggable']//vaadin-button[text() = 'OK']")
    private HtmlElement packTypeOkButton;

    @Name("Предлагаемый тип посылки")
    @FindBy(xpath = "//div[starts-with(text(), 'Сканируйте коробку')]")
    private HtmlElement suggestedPackType;

    @Name("Заголовок Назначенные ячейки")
    @FindBy(xpath = "//*[text() = 'Назначенные ячейки:']")
    private HtmlElement assignedCellsSign;

    public PackingPage(WebDriver driver) {
        super(driver);
    }

    @Step("Открываем стол сортировки")
    public void openSortTable(String sortTable) {
        driver.switchTo().frame(packingFrame);
        tableSelectDlg.openSortTable(sortTable);
        Assertions.assertTrue(assignedCellsSign.isDisplayed());
        driver.switchTo().defaultContent();
    }

    @Step("Сканируем УИТ товара")
    public void enterSerial(String itemSerial) {
        driver.switchTo().frame(packingFrame);
        uitInput.sendKeys(itemSerial);
        uitInput.sendKeys(Keys.ENTER);
        Assertions.assertTrue(suggestedPackType.isDisplayed());
        driver.switchTo().defaultContent();
    }

    private String getSuggestedPackType() {
        final String text = suggestedPackType.getText();
        return text.substring(text.indexOf("[") + 1, text.indexOf("]"));
    }

    @Step("Вводим тип посылки")
    public void enterSuggestedPackType() {
        driver.switchTo().frame(packingFrame);
        packTypeInput.sendKeys(getSuggestedPackType());
        wait.until(elementToBeClickable(packTypeOkButton));
        packTypeOkButton.click();
        waitElementHidden(By.xpath(packTypeInputXpath), true);
        driver.switchTo().defaultContent();
    }
}
