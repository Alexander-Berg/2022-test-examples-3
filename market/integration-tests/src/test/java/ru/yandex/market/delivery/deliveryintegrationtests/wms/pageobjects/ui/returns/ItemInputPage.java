package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.returns;

import io.qameta.allure.Step;
import lombok.NonNull;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.ReturnItemProps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.TypeOfDamaged;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.SkuShelfLifePage;
import com.codeborne.selenide.SelenideElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class ItemInputPage extends AbstractPage {

    @FindBy(xpath = "//div[@data-e2e='text-field']//input")
    private SelenideElement input;

    @FindBy(xpath = "//button[@data-e2e='button_no-more-items']")
    private SelenideElement noMoreItemsButton;

    ItemInputPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("itemInputPage$"));
    }

    @Step("Вводим штрихкод (EAN / УИТ)")
    public void enterBarcode(Item item) {
        if (item.getSerialNumber() != null) {
            input.sendKeys(item.getSerialNumber());
        } else {
            input.sendKeys(item.getArticle());
        }
        input.pressEnter();
    }

    @Step("Принимаем единицу товара")
    public ItemInputPage receiveInstance(Item item, String cart, @NonNull ReturnItemProps returnItemProps) {
        enterBarcode(item);
        if (item.getCheckCis() > -1) {
            new ReturnsIdentitiesPage(driver).enterIdentity(item.getInstances().getOrDefault("CIS", null),
                            returnItemProps.isClickNoCisButtonAfterCisEntering());
        }
        return receiveInstanceGeneralSteps(item, cart, returnItemProps.getTypeOfDamaged());
    }

    private ItemInputPage receiveInstanceGeneralSteps(Item item, String cart, TypeOfDamaged typeOfDamaged) {
        SkuInputPage skuInputPage = item.isShelfLife() ?
                new SkuShelfLifePage(driver)
                        .enterCreationDate(item)
                        .enterExpirationDate(item) : new SkuInputPage(driver, item).checkInfo();

        if (typeOfDamaged != null) {
            SkuCheckPage skuCheckPage = skuInputPage.confirmDamaged();
            typeOfDamaged.selectDamage(skuCheckPage);
            skuCheckPage.confirm();
        } else {
            skuInputPage.confirm();
        }

        setupDimensionsIfNeeded();

        CartPage cartPage = new CartInputPage(driver).enterCart(cart);
        cartPage.confirm();
        return this;
    }

    private void setupDimensionsIfNeeded() {
        wait.until(ExpectedConditions.or(urlMatches("cartInput"),
                urlMatches("setupDimensions")));
        String currentUrl = driver.getCurrentUrl();

        if (currentUrl.contains("setupDimensions")) {
            new SetupDimensionsPage(driver).enterVgh();
        }
    }
}
