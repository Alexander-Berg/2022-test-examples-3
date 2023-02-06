package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.picking;

import java.util.ArrayList;
import java.util.List;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.springframework.data.util.Pair;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.qatools.htmlelements.annotations.Name;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openqa.selenium.support.ui.ExpectedConditions.not;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlToBe;

public class CheckItemCardPage extends AbstractPage {

    private static final String NUMBER_OF_ITEMS =
            "//span[text()[starts-with(., 'Отобрано')]]";

    @FindBy(xpath = "//div[@data-e2e='check-item']//input")
    private SelenideElement serialNumberInput;

    @Name("Ячейка")
    @FindBy(xpath = "//span[@data-e2e='from-loc']")
    private SelenideElement cell;

    @Name("Партия")
    @FindBy(xpath = "//span[@data-e2e='info-lot']")
    private SelenideElement batch;

    @FindBy(xpath = "//div[@data-e2e='additionalMenu']")
    private SelenideElement openAdditionalMenuButton;

    @Name("Количество товаров")
    @FindBy(xpath = NUMBER_OF_ITEMS)
    private SelenideElement numberOfItems;

    @FindBy(xpath = "//button[@data-e2e='Context_picking_lost_item']")
    private SelenideElement contextPickingLostItem;

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement confirmReplaceIdButton;

    public CheckItemCardPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("moveItemCardPage$"));
    }

    @Step("Вводим ШК товара")
    public List<String> enterSerialNumbers() {
        List<String> serialNumbers = new ArrayList<>();
        String checkItemCardPageUrl = driver.getCurrentUrl();

        int tasks = getAmountToPickFromThisCell();
        for (int i = 0; i < tasks; i++) {
            String cellName = cell.getText().trim();
            String lotNameEnding = batch.getText().trim();
            String serialNumber = DatacreatorSteps.Items().getItemSerialByLocLot(cellName, "%" + lotNameEnding);
            assertTrue(!serialNumber.isEmpty(),
                    String.format("По балансам в ячейке %s нет товаров из партии *%s", cellName, lotNameEnding));
            serialNumberInput.sendKeys(serialNumber);
            serialNumberInput.pressEnter();
            serialNumbers.add(serialNumber);
        }

        wait.until(not(urlToBe(checkItemCardPageUrl)));

        return serialNumbers;
    }

    @Step("Вводим НЗН")
    public void enterId(String id) {
        String checkItemCardPageUrl = driver.getCurrentUrl();

        serialNumberInput.sendKeys(id);
        serialNumberInput.pressEnter();

        confirmReplaceIdButton.click();

        wait.until(not(urlToBe(checkItemCardPageUrl)));
    }

    @Step("Жмём пункт меню, что не смогли найти товар")
    public Pair<String, String> doShortageOfItem(boolean expectToLeaveCell) {
        String cellName = cell.getText().trim();
        String lotNameEnding = batch.getText().trim();

        String initialUrl = driver.getCurrentUrl();
        openAdditionalMenuButton.click();
        contextPickingLostItem.click();
        Allure.step(String.format("Для шорта попалась партия *%s в ячейке %s", lotNameEnding, cellName));
        if (expectToLeaveCell) {
            wait.until(not(urlToBe(initialUrl)));
        }
        return Pair.of(cellName, lotNameEnding);
    }

    private int getAmountToPickFromThisCell() {
        return Integer.valueOf(StringUtils.substringAfter(numberOfItems.getText(), "из").trim());
    }
}
