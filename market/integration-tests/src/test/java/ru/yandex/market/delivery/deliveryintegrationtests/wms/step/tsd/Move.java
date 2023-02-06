package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.tsd;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.common.WarningDialog;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.menu.MoveMenu;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.menu.TsdMainMenu;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.move.putsn.PutSnCartSelectPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.move.putsn.PutSnCellSelectPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.move.putsn.SerialScanAndMovePage;

import java.util.List;

@Resource.Classpath({"wms/infor.properties"})
public class Move {
    private final Logger log = LoggerFactory.getLogger(Move.class);

    private WebDriver driver;
    private TsdMainMenu tsdMainMenu;
    private MoveMenu moveMenu;
    private PutSnCartSelectPage putSnCartSelectPage;
    private PutSnCellSelectPage putSnCellSelectPage;
    private SerialScanAndMovePage serialScanAndMovePage;
    private WarningDialog warningDialog;

    public Move(WebDriver drvr) {

        PropertyLoader.newInstance().populate(this);

        this.driver = drvr;

        tsdMainMenu = new TsdMainMenu(driver);
        moveMenu = new MoveMenu(driver);
        putSnCartSelectPage = new PutSnCartSelectPage(driver);
        putSnCellSelectPage = new PutSnCellSelectPage(driver);
        serialScanAndMovePage = new SerialScanAndMovePage(driver);
        warningDialog = new WarningDialog(driver);

    }

    @Step("Перемещение товара с тележки на полку")
    public void moveItems(List<String> itemSerialNumbers, String cartId, String cellId) {
        log.info("Moving items from cart {} to cell {}", cartId, cellId);

        tsdMainMenu.Move();
        moveMenu.putSnButtonClick();
        putSnCartSelectPage.enterCartId(cartId);
        putSnCartSelectPage.acceptSelectedCart();
        putSnCellSelectPage.enterCellId(cellId);
        putSnCellSelectPage.acceptSelectedCell();
        for (String sn:itemSerialNumbers
             ) {
            log.info("SN: {}", sn);
            serialScanAndMovePage.enterSN(sn);
        }

        if (warningDialog.IsPresentWithMessage("больше нет товара")) {
            warningDialog.clickOk();
            putSnCartSelectPage.assertCartInputFieldDisplayed();
        }

    }
}
