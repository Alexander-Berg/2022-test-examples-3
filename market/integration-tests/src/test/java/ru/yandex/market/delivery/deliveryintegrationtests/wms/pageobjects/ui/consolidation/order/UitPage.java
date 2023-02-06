package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.consolidation.order;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

public class UitPage extends AbstractInputPage {
    @FindBy(xpath = "//div[@data-e2e='additionalMenu']")
    private SelenideElement additionalMenuButton;
    @FindBy(xpath = "//button[@data-e2e='Context_consolidation_close_container']")
    private SelenideElement ranOutOfItemsInContainerButton;

    public UitPage(WebDriver driver) {
        super(driver);
    }

    @Override
    protected String getUrl() {
        return "uitInput";
    }

    @Step("Сканируем УИТ")
    public CellPage enterUIT(String uit) {
        super.performInput(uit);
        return new CellPage(driver);
    }

    @Step("Жмём пункт меню, что в контейнере кончились товары")
    public UitPage doShortageOfItems() {
        additionalMenuButton.click();
        ranOutOfItemsInContainerButton.click();
        return this;
    }

}
