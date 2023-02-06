package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving;

import java.util.concurrent.TimeUnit;

import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.Retrier;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.RegistryItemWrapper;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.NotificationDialog;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.dropping.BbxdConfirmPage;

import com.codeborne.selenide.SelenideElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;
import java.util.List;

public class BarcodeInputPage extends AbstractPage {

    @FindBy(xpath = "//div[@data-e2e='text-field']//input")
    private SelenideElement input;

    @FindBy(xpath = "//button[@data-e2e='close-container']")
    private SelenideElement closeCartButton;

    @FindBy(xpath = "//button[@data-e2e='close-source-container']")
    private SelenideElement closePalletButton;

    @FindBy(xpath = "//button[@data-e2e='anomaly']")
    private SelenideElement anomalyButton;

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement forward;

    @FindBy(xpath = "//span[contains(text(), 'Ок')]")
    private SelenideElement okButton;

    private final NotificationDialog notificationDialog = new NotificationDialog(driver);
    private final List<String> itemNotFoundErrorTitles = List.of(
            "Товар не найден в системе",
            "Такой товар не заявлен в этой поставке",
            "Не удалось отобразить детали"
    );


    public BarcodeInputPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("barcodeInput$"));
    }

    @Step("Вводим штрихкод (EAN)")
    public SkuConfirmationPage enterBarcode(Item item) {
        input.sendKeys(item.getArticle());
        submitWithRetryOnError(itemNotFoundErrorTitles);
        if (item.isHasDuplicates()) {
            new SkuSelectionPage(driver).select(item.getName());
        }
        return new SkuConfirmationPage(driver);
    }

    @Step("Вводим штрихкод (EAN)")
    public SkuConfirmationPage enterBarcode(RegistryItemWrapper item) {
        input.sendKeys(item.getBarcode());
        input.pressEnter();
        if (item.isHasDuplicates()) {
            new SkuSelectionPage(driver).select(item.getName());
        }
        return new SkuConfirmationPage(driver);
    }

    @Step("Вводим штрихкод (EAN) многоместного товара")
    public ChooseBomPage enterMnogoboxBarcode(Item item) {
        input.sendKeys(item.getArticle());
        submitWithRetryOnError(itemNotFoundErrorTitles);
        return new ChooseBomPage(driver, item);
    }

    @Step("Вводим неправильный штрихкод (EAN)")
    public BarcodeInputPage enterWrongBarcode(String barcode) {
        input.sendKeys(barcode);
        input.pressEnter();
        notificationDialog.IsPresentWithMessage("Товар с этим ШК не найден. Попробуйте отсканировать заново");

        return this;
    }

    @Step("Вводим штрихкод из другой поставки {barcode}")
    public BarcodeInputPage enterNotDeclaredBarcode(String barcode) {
        input.sendKeys(barcode);
        input.pressEnter();
        Assertions.assertTrue(
                notificationDialog.IsPresentWithMessage("Введите ШК, заявленный поставщиком на этот товар"),
                "Не появилось ошибки о незаявленном ШК");
        return this;
    }

    @Step("Принимаем единицу товара")
    public BarcodeInputPage receiveInstance(Item item, String cart, boolean allowQty) {
        final SkuConfirmationPage skuConfirmationPage = enterBarcode(item)
                .checkInfo(item);
        if (allowQty) {
            skuConfirmationPage.confirm(item);
        } else {
            skuConfirmationPage.confirmNoQty(item);
        }
        final CartInputPage cartInputPage = new CartInputPage(driver);
        final String instance = cartInputPage.getInstance();
        cartInputPage.enterCart(cart);
        checkInstancePlaced(instance);
        return this;
    }

    @Step("Принимаем единицу товара")
    public BarcodeInputPage receiveInstance(RegistryItemWrapper item, String cart, boolean allowQty) {
        final SkuConfirmationPage skuConfirmationPage = enterBarcode(item)
                .checkInfo(item);
        if (allowQty) {
            skuConfirmationPage.confirm(item);
        } else {
            skuConfirmationPage.confirmNoQty(item);
        }
        final CartInputPage cartInputPage = new CartInputPage(driver);
        final String instance = cartInputPage.getInstance();
        cartInputPage.enterCart(cart);
        checkInstancePlaced(instance);
        return this;
    }
    @Step("Принимаем единицу клиентского возврата")
    public BarcodeInputPage receiveReturnInstance(Item item, String cart, boolean isDamaged) {
       return receiveReturnInstance(item, cart, isDamaged,false);
    }

    @Step("Принимаем единицу клиентского возврата")
    public BarcodeInputPage receiveReturnInstance(Item item, String cart, boolean isDamaged, boolean notDeclaredId) {
        enterBarcode(item)
                .checkInfo(item)
                .confirmWithoutChecks();

        final boolean needToEnterIdentities = item.getCheckImei() > 0 ||
                item.getCheckSn() > 0 ||
                item.getCheckCis() > 0;
        if (needToEnterIdentities) {
            new IdentityInputPage(driver).enterIdentities(item);
        }

        if (notDeclaredId) {
            final CartInputPage cartInputPage = new CartInputPage(driver);
            cartInputPage.enterCart(cart);
            return this;
        }

        final SkuWarehouseDamagePage skuWarehouseDamagePage = new SkuWarehouseDamagePage(driver);
        if (isDamaged) {
            skuWarehouseDamagePage.selectDamaged();
            new SkuWarehouseCheckPage(driver).selectDamageAttributes();
        } else {
            skuWarehouseDamagePage.selectNotDamaged();
        }

        final CartInputPage cartInputPage = new CartInputPage(driver);
        final String instance = cartInputPage.getInstance();
        cartInputPage.enterCart(cart);
        checkInstancePlaced(instance);
        return this;
    }

    @Step("Принимаем единицу клиентского возврата c некорректным описанием")
    public CartInputPage receiveReturnInstanceWithIncorrectDescription(Item item) {

        return enterBarcode(item)
                .clickAnomalyButton()
                .clickIncorrectDescription();
    }

    @Step("Принимаем единицу товара с использованием вложенности тар")
    public BarcodeInputPage receiveInstanceWithNesting(Item item, String flipboxId, String cart, boolean allowQty) {
        final SkuConfirmationPage skuConfirmationPage = enterBarcode(item)
                .checkInfo(item);
        if (allowQty) {
            skuConfirmationPage.confirm(item);
        } else {
            skuConfirmationPage.confirmNoQty(item);
        }
        final CartInputPage cartInputPage = new CartInputPage(driver);
        final String instance = cartInputPage.getInstance();
        cartInputPage.enterFlipBox(flipboxId);
        final CartParentInputPage cartParentInputPage = new CartParentInputPage(driver);
        cartParentInputPage.enterParentCart(cart);
        checkInstancePlaced(instance);
        return this;
    }

    @Step("Принимаем безУИТный товар")
    public BarcodeInputPage receiveVirtualUitInstance(Item item, String palletId,
                                                      Integer numOfBoxes, Integer quantityPerBox) {
        enterBarcode(item)
                .checkInfo(item)
                .enterNumOfBoxes(numOfBoxes)
                .enterQuantityPerBox(quantityPerBox);
        final CartInputPage cartInputPage = new CartInputPage(driver);
        cartInputPage.enterCart(palletId);
        return this;
    }

    @Step("Принимаем безУИТный товар BBXD")
    public BbxdConfirmPage receiveVirtualUitAndStartSortingBbxd(Item item, Integer numOfBoxes, Integer quantityPerBox) {
        receiveVirtualUitBbxd(item, numOfBoxes, quantityPerBox);
        return new BbxdConfirmPage(driver);
    }

    @Step("Принимаем безУИТный товар BBXD")
    public void receiveVirtualUitBbxd(Item item, Integer numOfBoxes, Integer quantityPerBox) {
        enterBarcode(item)
                .checkInfo(item)
                .enterNumOfBoxes(numOfBoxes)
                .enterQuantityPerBox(quantityPerBox);
    }

    @Step("Принимаем единицу многоместного товара")
    public BarcodeInputPage receiveMnogoboxInstance(Item item, String name, String cart) {
        enterMnogoboxBarcode(item)
                .checkInfo()
                .createNewBom();
        final NewBomPage newBomPage = new NewBomPage(driver);
        newBomPage.enterBomName(item, name)
                .confirmWithoutChecks();
        new CartInputPage(driver)
                .enterCart(cart);
        return this;
    }

    @Step("Принимаем единицу товара в аномалии")
    public BarcodeInputPage receiveInstanceAsAnomaly(Item item, String cart) {
        enterBarcode(item)
                .checkInfo(item)
                .confirm(item);
        new AnomalyQtyConfirmationPage(driver)
                .confirm()
                .verifyAnomaly()
                .enterCart(cart);
        return this;
    }

    @Step("Принимаем единицу товара в аномалии")
    public BarcodeInputPage receiveInstanceAsAnomaly(RegistryItemWrapper item, String cart) {
        enterBarcode(item)
                .clickAnomaly()
                .clickOnDefect()
                .enterCart(cart)
                .closePallet();
        return this;
    }

    @Step("Закрываем тару")
    public void closeCart() {
        closeCartButton.click();
    }

    @Step("Сбросить тару на конвейер")
    public void putItemOnConveyor() {
        closeCartButton.click();
    }

    @Step("Проверяем размещение УИТ")
    private void checkInstancePlaced(String instance) {
        //TODO: use special UI after https://st.yandex-team.ru/MARKETWMSFRONT-99
    }

    @Step("Закрываем палету")
    public ReceiptInputPage closePallet() {
        closePalletButton.click();
        okButton.click();
        return new ReceiptInputPage(driver);
    }

    @Step("Закрываем палету с расхождениями")
    public ReceiptInputPage closePalletWithDiscrepancies() {
        closePalletButton.click();
        okButton.click();
        new NonDoneSkuPage(driver).approveClosingPallet();
        return new ReceiptInputPage(driver);
    }

    @Step("Нажимаем на кнопку Аномалия")
    public CartInputPage clickAnomaly() {
        anomalyButton.click();
        forward.click();
        return new CartInputPage(driver);
    }

    //TODO отказаться от использования метода после фикса https://st.yandex-team.ru/MARKETWMS-14835
    private void submitWithRetryOnError(List<String> errorTitles) {
        Retrier.retry(() -> {
            input.pressEnter();
            errorTitles.forEach(errorTitle -> Assertions.assertFalse(
                    notificationDialog.isPresentWithTitleCustomTimeout(errorTitle, 1),
                    String.format("Появилось сообщение об ошибке с заголовком: %s", errorTitle)));
        }, Retrier.RETRIES_SMALL, Retrier.TIMEOUT_TINY, TimeUnit.SECONDS);

    }
}
