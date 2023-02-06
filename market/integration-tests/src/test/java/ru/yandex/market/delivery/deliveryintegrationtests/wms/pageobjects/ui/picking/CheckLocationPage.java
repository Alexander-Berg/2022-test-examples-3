package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.picking;

import java.util.Arrays;

import io.qameta.allure.Step;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.NotificationDialog;
import ru.yandex.qatools.htmlelements.annotations.Name;
import com.codeborne.selenide.SelenideElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class CheckLocationPage extends AbstractPage {

    @FindBy(xpath = "//div[@data-e2e='check-location']//input")
    private SelenideElement cellInput;

    @FindBy(xpath = "//label[@data-e2e='remaining_items']/span")
    private SelenideElement itemsLeftLabel;

    @Name("Ячейка")
    @FindBy(xpath = "//span[@data-e2e='loc-text']")
    private SelenideElement cell;

    public CheckLocationPage(WebDriver driver) {
        super(driver);
        wait.until(ExpectedConditions
                .and(NotificationDialog.getRemoteErrorBreakerCondition(), urlMatches("checkLocationPage$")));
    }

    @Step("Вводим ячейку отбора")
    public CheckItemCardPage enterCellId() {
        String cellId = StringUtils.substringAfter(cell.getText(), "Ячейка:").trim();
        cellInput.sendKeys(cellId);
        cellInput.pressEnter();
        return new CheckItemCardPage(driver);
    }

    @Step("Получаем количество товаров, оставшихся в назначении на отбор")
    public int getLeftItemsCount() {
        return Integer.parseInt(Arrays.asList(itemsLeftLabel.getText().split(" ")).get(1).trim());
    }
}
