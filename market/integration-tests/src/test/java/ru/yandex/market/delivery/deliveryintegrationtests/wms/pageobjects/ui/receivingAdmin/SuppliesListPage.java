package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receivingAdmin;

import io.qameta.allure.Step;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import com.codeborne.selenide.SelenideElement;

import java.util.ArrayList;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class SuppliesListPage extends AbstractPage {
    public SuppliesListPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("suppliesList"));
    }

    @Name("Поле фильтрации по номеру поставки")
    @FindBy(xpath = "//div[@data-e2e='receiptkey_filter']/div/input")
    private SelenideElement receiptKeyFilterInput;

    @Name("Чекбокс первого в результатах поиска")
    @FindBy(xpath = "//td[@data-e2e='checkbox_cell_row_0']/div/input")
    private SelenideElement firstCheckbox;

    @Name("Кнопка открытия меню действий")
    @FindBy(xpath = "//button[@data-e2e='more_actions_button_row_0']")
    private SelenideElement moreActionsButton;

    @Name("Кнопка \"Закрыть ПУО приёмку\"")
    @FindBy(xpath = "//button[@data-e2e='close_asn_receiving_row_0']")
    private SelenideElement closeReceiptButton;

    @Name("Кнопка \"Закрыть с проверкой\"")
    @FindBy(xpath = "//button[@data-e2e='close_with_check_row_0']")
    private SelenideElement approveClosePuoButton;

    @Name("Первая поставка из результатов фильтрации")
    @FindBy(xpath = "//td[@data-e2e='receiptkey_cell_row_0']/div/a")
    private SelenideElement firstReceiptInResults;

    @Name("Меню дополнительных действий")
    @FindBy(xpath = "//button[@data-e2e='open_asn_receiving_row_0']/..")
    private SelenideElement moreActionsMenu;

    @Step("Вводим номер поставки {receiptKey} в поле фильтрации")
    public SuppliesListPage filterByReceiptKey(String receiptKey) {
        receiptKeyFilterInput.sendKeys(receiptKey);
        receiptKeyFilterInput.pressEnter();
        return this;
    }

    @Step("Открываем первую поставку из результатов фильтрации")
    public SuppliesDetailsPage openFirstReceipt() {
        firstReceiptInResults.shouldBe(visible);
        ArrayList<String> tabs = new ArrayList<String>(getWebDriver().getWindowHandles());
        firstReceiptInResults.click();
        switchToSubWindow(tabs.size());

        return new SuppliesDetailsPage(driver);
    }

    @Step("Выбираем первый элемент в таблице")
    public SuppliesListPage selectFirstResult() {
        firstCheckbox.click();

        return this;
    }

    @Step("Открываем меню действий")
    public SuppliesListPage clickMoreActionsButton() {
        String moreActionsMenuXpath = "//button[@data-e2e='open_asn_receiving_row_0']/..";
        moreActionsButton.click();
        moreActionsMenu.shouldBe(visible);

        return this;
    }

    @Step("Нажимаем кнопку \"Закрыть ПУО приёмку\"")
    public SuppliesListPage clickCloseReceiptButton() {
        closeReceiptButton.click();

        return this;
    }

    @Step("Нажимаем кнопку \"Закрыть с проверкой\"")
    public SuppliesListPage clickApproveClosePuoButton() {
        approveClosePuoButton.click();

        return this;
    }
}
