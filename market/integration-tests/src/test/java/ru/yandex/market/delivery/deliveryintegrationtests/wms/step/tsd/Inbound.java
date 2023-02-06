package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.tsd;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.common.Header;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.inbound.InboundPrinterSelectPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.inbound.inbound.AcceptItemPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.inbound.inbound.CartSelectPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.inbound.inbound.EnterCartPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.inbound.inbound.InboundByPlacesPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.inbound.inbound.InboundIdPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.inbound.inbound.ItemCancelPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.inbound.inbound.ItemInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.inbound.inbound.MeasuringCartPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.inbound.inbound.MeasuringConfirmationPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.inbound.inbound.MeasuringDevicePage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.inbound.inbound.MeasuringPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.inbound.inbound.ScanItemPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.menu.InboundMenu;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.menu.TsdMainMenu;

@Resource.Classpath({"wms/infor.properties"})
public class Inbound {
    private final Logger log = LoggerFactory.getLogger(Inbound.class);

    private final TsdMainMenu tsdMainMenu;
    private final Header header;
    private final InboundPrinterSelectPage inboundPrinterSelectPage;
    private final InboundMenu inboundMenu;
    private final InboundIdPage inboundIdPage;
    private final CartSelectPage cartSelectPage;
    private final ScanItemPage scanItemPage;
    private final AcceptItemPage acceptItemPage;
    private final InboundByPlacesPage inboundByPlacesPage;
    private final ItemCancelPage itemCancelPage;
    private final MeasuringDevicePage measuringDevicePage;
    private final MeasuringCartPage measuringCartPage;
    private final EnterCartPage enterCartPage;
    private final ItemInputPage itemInputPage;
    private final MeasuringPage measuringPage;
    private final MeasuringConfirmationPage measuringConfirmationPage;

    @Property("infor.printerid")
    private String printerId;

    public Inbound(WebDriver driver) {

        PropertyLoader.newInstance().populate(this);

        tsdMainMenu = new TsdMainMenu(driver);
        inboundPrinterSelectPage = new InboundPrinterSelectPage(driver);
        inboundMenu = new InboundMenu(driver);
        header = new Header(driver);
        inboundIdPage = new InboundIdPage(driver);
        cartSelectPage = new CartSelectPage(driver);
        scanItemPage = new ScanItemPage(driver);
        acceptItemPage = new AcceptItemPage(driver);
        inboundByPlacesPage = new InboundByPlacesPage(driver);
        itemCancelPage = new ItemCancelPage(driver);
        measuringDevicePage = new MeasuringDevicePage(driver);
        measuringCartPage = new MeasuringCartPage(driver);
        enterCartPage = new EnterCartPage(driver);
        itemInputPage = new ItemInputPage(driver);
        measuringPage = new MeasuringPage(driver);
        measuringConfirmationPage = new MeasuringConfirmationPage(driver);
    }

    @Step("Начинаем приемку по местам")
    public void inboundByPlaces(String inboundId) {
        log.info("Opening Inbound {}", inboundId);
        tsdMainMenu.Inbound();
        inboundPrinterSelectPage.selectPrinter(printerId);
        inboundPrinterSelectPage.selectInboundCell("STAGE");
        inboundMenu.inboundByPlacesButtonClick();
        inboundByPlacesPage.enterInboundAndPlaces(inboundId, 1);
    }

    @Step("Начинаем приемку ПУО {inboundId}")
    public void openInbound(String inboundId, String cartId) {
        log.info("Opening Inbound {}", inboundId);

        tsdMainMenu.Inbound();

        inboundPrinterSelectPage.selectPrinter(printerId);
        inboundPrinterSelectPage.selectInboundCell("STAGE");
        inboundMenu.inboundButtonClick();

        inboundIdPage.enterInboundId(inboundId);
        cartSelectPage.enterCartId(cartId);

        cartSelectPage.acceptSelectedCart();
    }

    @Step("Переобмеряем старый товар {itemId}")
    public void reMeasureItem(String cartId, String itemId, String length, String width, String height, String weight) {
        log.info("measure item...");

        tsdMainMenu.Inbound();

        inboundPrinterSelectPage.selectPrinter(printerId);
        inboundPrinterSelectPage.selectInboundCell("STAGE");
        inboundMenu.goodsSetupButton();

        measuringDevicePage.enterEquipId();
        measuringCartPage.enterCartId(cartId);
        enterCartPage.enterCartId(cartId);
        itemInputPage.enterItemId(itemId);
        measuringConfirmationPage.MeasureConfirm();
        measuringPage.enterKorobyts(length, width, height, weight);
    }

    @Step("Начинаем приемку бракованного товара {inboundId}")
    public void defectInbound(String inboundId, String cartId) {
        log.info("Opening defect Inbound {}", inboundId);

        tsdMainMenu.Inbound();

        inboundPrinterSelectPage.selectPrinter(printerId);
        inboundPrinterSelectPage.selectInboundCell("STAGE");
        inboundMenu.defectButton();

        inboundIdPage.enterInboundId(inboundId);
        cartSelectPage.enterCartId(cartId);
    }

    @Step("Принимаем {number} единиц товара")
    public void acceptItem(String itemBarcode, int number) {
        for (int i = 0; i < number; i++) {
            acceptItem(itemBarcode);
        }
    }

    @Step("Принимаем единицу товара")
    public void acceptItem(String itemBarcode) {
        scanItemPage.enterBarcode(itemBarcode);
        acceptItemPage.verifyItem();
        acceptItemPage.acceptItem();
        scanItemPage.isDisplayed();
    }

    @Step("Отменяем единицу товара")
    public void cancelItem(String itemId) {

        tsdMainMenu.Inbound();

        inboundPrinterSelectPage.selectPrinter(printerId);
        inboundPrinterSelectPage.selectInboundCell("STAGE");

        inboundMenu.inboundCancelButton();
        itemCancelPage.enterItemId(itemId);
    }

}
