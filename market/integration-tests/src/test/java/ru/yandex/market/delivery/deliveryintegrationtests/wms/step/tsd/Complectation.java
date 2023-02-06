package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.tsd;

import java.util.ArrayList;

import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.ParcelId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.complectation.AreaSelectPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.complectation.AssignmentPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.complectation.ComplectationCompletePage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.complectation.ComplectationPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.menu.ComplectationMenu;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.menu.MoveMenu;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.menu.TsdMainMenu;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.move.putsn.PutSnCartSelectPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.move.putsn.PutSnCellSelectPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.move.putsn.SerialScanAndMovePage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;

@Resource.Classpath({"wms/infor.properties"})
public class Complectation {
    private final Logger log = LoggerFactory.getLogger(Complectation.class);

    private WebDriver driver;
    private TsdMainMenu tsdMainMenu;
    private MoveMenu moveMenu;
    private PutSnCartSelectPage putSnCartSelectPage;
    private PutSnCellSelectPage putSnCellSelectPage;
    private SerialScanAndMovePage serialScanAndMovePage;
    private ComplectationMenu complectationMenu;
    private AreaSelectPage areaSelectPage;
    private AssignmentPage assignmentPage;
    private ComplectationPage complectationPage;
    private ComplectationCompletePage complectationCompletePage;

    public Complectation(WebDriver drvr) {

        PropertyLoader.newInstance().populate(this);

        this.driver = drvr;

        tsdMainMenu = new TsdMainMenu(driver);
        moveMenu = new MoveMenu(driver);
        putSnCartSelectPage = new PutSnCartSelectPage(driver);
        putSnCellSelectPage = new PutSnCellSelectPage(driver);
        serialScanAndMovePage = new SerialScanAndMovePage(driver);
        complectationMenu = new ComplectationMenu(driver);
        areaSelectPage = new AreaSelectPage(driver);
        assignmentPage = new AssignmentPage(driver);
        complectationPage = new ComplectationPage(driver);
        complectationCompletePage = new ComplectationCompletePage(driver);
    }

    public void selectGoods(String area, ParcelId parcelId) {
        selectGoods(area, parcelId.getId());
    }

    @Step("Отбор товаров")
    public ArrayList<String> selectGoods(String area, String cartId) {
        log.info("Selecting goods for order");

        ArrayList<String> itemList = new ArrayList<>();

        tsdMainMenu.Complectation();
        complectationMenu.complectationTaskButtonClick();
        areaSelectPage.enterArea(area);

        int tasksNum = assignmentPage.getTasksNumber();

        assignmentPage.enterCart(cartId);
        assignmentPage.acceptSelectedCart();

        for (int i = 0; i < tasksNum; i++) {
            if (complectationPage.needConfirmCell()) complectationPage.confirmCell();

            String item = DatacreatorSteps.Items().getItemSerialByLocLot(
                    complectationPage.getCell(),
                    complectationPage.getBatch()
            );

            complectationPage.inputItemSerial(item);

            log.info("Selected item serial: {}", item);

            itemList.add(item);
        }

        Assertions.assertTrue(complectationCompletePage.isDisplayed(),
                "Ошибка: Не открылась страница завершенного отбора");

        return itemList;
    }

    @Step("Проверяем, что нет начатых отборов")
    public void verifyNoComplectationStarted(String area) {
        log.info("Validating that there is no started complectation");

        tsdMainMenu.Complectation();
        complectationMenu.complectationTaskButtonClick();
        areaSelectPage.enterArea(area);

        Assertions.assertTrue(areaSelectPage.noComplectationStartedWarningPresent(),
                "Ошибка: Не должно быть начатых отборов");
    }
}
