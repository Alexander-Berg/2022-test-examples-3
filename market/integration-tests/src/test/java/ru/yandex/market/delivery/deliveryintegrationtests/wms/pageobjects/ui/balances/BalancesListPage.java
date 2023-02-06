package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.balances;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.TablePreloader;
import com.codeborne.selenide.SelenideElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class BalancesListPage extends AbstractPage {

    @FindBy(xpath = "//div[@data-e2e='manufacturerSku_filter']//input")
    private SelenideElement skuFilterInput;
    @FindBy(xpath = "//div[@data-e2e='loc_filter']//input")
    private SelenideElement locFilterInput;
    @FindBy(xpath = "//td[@data-e2e='qty_cell_row_0']")
    private SelenideElement qtyRow0;

    private final By emptyTableText = By.xpath("//div[@data-e2e='disabled_table_preloader']//span");
    private final TablePreloader tablePreloader;

    public BalancesListPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("balancesListPage"));
        tablePreloader = new TablePreloader(driver);
    }

    @Step("Поиск по SKU и ячейке")
    public BalancesListPage searchBySku(String sku) {
        skuFilterInput.click();
        skuFilterInput.sendKeys(sku);
        skuFilterInput.pressEnter();
        tablePreloader.waitUntilHidden();
        return this;
    }

    public BalancesListPage searchByCell(String cell) {
        locFilterInput.click();
        locFilterInput.sendKeys(cell);
        locFilterInput.pressEnter();
        tablePreloader.waitUntilHidden();
        return this;
    }

    @Step("Получаем количество из результатов фильтра")
    public int getItemCountFromFilterResults() {
        if (isElementPresent(emptyTableText)){
            return 0;
        } else {
            return Integer.parseInt(qtyRow0.getText());
        }
    }
}
