package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.tsd;

import java.util.Collections;
import java.util.List;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.CarId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.DropId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.ParcelId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.complectation.AreaSelectPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.complectation.AssignmentPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.complectation.ComplectationCompletePage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.complectation.ComplectationPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.menu.ComplectationMenu;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.menu.MoveMenu;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.menu.OutgoingMenu;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.menu.TsdMainMenu;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.move.putsn.PutSnCartSelectPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.move.putsn.PutSnCellSelectPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.move.putsn.SerialScanAndMovePage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.outgoing.PackingCellPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.outgoing.SortingPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.outgoing.sorting.SortingInfoPage;

@Resource.Classpath({"wms/infor.properties"})
public class Outgoing {
    private final Logger log = LoggerFactory.getLogger(Outgoing.class);

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
    private OutgoingMenu outgoingMenu;
    private SortingInfoPage sortingInfoPage;

    public Outgoing(WebDriver drvr) {

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
        outgoingMenu = new OutgoingMenu(driver);
        sortingInfoPage = new SortingInfoPage(driver);
    }

    @Step("Консолидация волны")
    public void consolidateWave(String cart, String cell) {
        log.info("Consolidate wave");
        tsdMainMenu
                .Outgoing()
                .consolidateWave()
                .submitCart(cart)
                .submitCell(cell);
    }

    @Step("Сортировка товаров")
    public void sortGoods(String cartId, String sortingCell, List<String> items, String station) {
        log.info("Sorting goods");

        tsdMainMenu
                .Outgoing()
                .sortGoods()
                .submitStation(station)
                .submitCart(cartId);

        items.forEach(item -> {
            new SortingPage.SkuPage(driver).submitItems(item)
                    .submitCell(sortingCell);
        });
    }

    @Step("Упаковка товаров на дроп")
    public DropId packGoods(List<ParcelId> parcelIds) {
        log.info("Packing goods");
        String dropId = "DRP" + parcelIds.get(0).getId().substring(3);

        //ячейка PACK вводится только для первой посылки из всех, что кладутся на одну DRP
        //для определения первой посылки используется индекс i
        for (int i = 0; i < parcelIds.size(); i++) {
            tsdMainMenu.Outgoing()
                    .packGoods()
                    .inputParcel(parcelIds.get(i).getId())
                    .inputDropId(dropId);
            if (i == 0) {
                new PackingCellPage(driver)
                        .inputPackingCell("PACK");
            }
        }

        return new DropId(dropId);
    }

    public DropId packGoods(ParcelId parcelId) {
        return packGoods(Collections.singletonList(parcelId));
    }

    @Deprecated
    @Step("Загрузка товаров в машину")
    public CarId loadGoods(DropId dropId) {
        log.info("Loading goods");

        String carId = dropId
                .getId()
                .substring(dropId.getId().length() - 3);

        tsdMainMenu
                .Outgoing()
                .loadGoods()
                .inputDropId(dropId.getId())
                .inputCarId(carId)
                .inputCellId("")
                .accept();

        return new CarId(carId);
    }

    @Deprecated
    @Step("Отгрузка товаров")
    public void shipGoods(CarId carId) {
        log.info("Loading goods");

        tsdMainMenu
                .Outgoing()
                .shipGoods()
                .shipByNzn(carId.getId());
    }
}
