package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.balances;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.LocationKey;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selectors.byXpath;
import static com.codeborne.selenide.Selenide.$;

@Slf4j
public class BalancesPage extends AbstractPage {

    @FindBy(xpath = "//select[@data-e2e='duration-selector']")
    private SelenideElement typeOfSearchSelect;

    @FindBy(xpath = "//option[@value='loc']")
    private SelenideElement searchByLoc;

    @FindBy(xpath = "//option[@value='sku']")
    private SelenideElement searchBySku;

    @FindBy(xpath = "//option[@value='id']")
    private SelenideElement searchById;

    @FindBy(xpath = "//option[@value='serialNumber']")
    private SelenideElement searchBySerialNumber;

    @FindBy(xpath = "//option[@value='pallet']")
    private SelenideElement searchByPallet;

    @FindBy(xpath = "//div[@data-e2e='balances_search_input']//input")
    private SelenideElement requestInput;

    @FindBy(xpath = "//button[@data-e2e='button_back']")
    private SelenideElement backButton;

    @FindBy(xpath = "//span[@data-e2e='value_of_Партия']")
    private SelenideElement lotNumber;

    @FindBy(xpath = "//span[@data-e2e='value_of_НЗН']")
    private SelenideElement id;

    @FindBy(xpath = "//span[@data-e2e='value_of_УИТы']")
    private SelenideElement uits;

    @FindBy(xpath = "//span[@data-e2e='value_of_Ячейка']")
    private SelenideElement cell;

    String notFoundText = "Ничего не найдено";

    public BalancesPage(WebDriver driver) {
        super(driver);
    }

    @Step("Поиск в балансах по вводу НЗН - {nzn}")
    public BalancesPage searchById(String nzn) {
        searchById.click();
        requestInput.click();
        requestInput.sendKeys(nzn);
        requestInput.pressEnter();
        return new BalancesPage(driver);
    }

    @Step("Поиск в балансах по вводу УИТ - {uit}")
    public BalancesPage searchByUit(String uit) {
        searchBySerialNumber.click();
        requestInput.click();
        requestInput.sendKeys(uit);
        requestInput.pressEnter();
        return new BalancesPage(driver);
    }

    @Step("Ищем УИТы по ячейке {loc}")
    public BalancesPage searchByLoc(String loc) {
        searchByLoc.click();
        requestInput.click();
        requestInput.sendKeys(loc);
        requestInput.pressEnter();
        return new BalancesPage(driver);
    }

    public void clickBack() {
        backButton.click();
    }

    @Step("Проверяем, что появилось сообщение о том, что уит не найден")
    public boolean checkUitNotFound(){
        return notificationDialog.IsPresentWithMessage("Данный товар не существует");
    }

    @Step("Получаем номер партии из результатов")
    public String getLotFromFilterResults() {
        return lotNumber.getText();
    }

    @Step("Получаем УИТы из результатов")
    public String getUITFromFilterResults() {
        return uits.getText();
    }

    @Step("Получаем ячейку и НЗН из результатов фильтра")
    public LocationKey getLocationKeyFromFilterResults() {
        if (cell.isDisplayed()) {
            String loc = cell.getText();
            String nzn = id.getText();
            return new LocationKey(loc, nzn); // когда УИТ где-то числится
        } else {
            Assertions.assertTrue(checkUitNotFound(),
                    "Нет ни ячейки по УИТу, ни сообщения, что УИТа не существует");
            return null; // когда УИТ не найден
        }
    }

    @Step("Проверяем что результат поиска пустой")
    public boolean checkNoResultsFound(){
        return isElementPresent(byText(notFoundText));
    }
}
