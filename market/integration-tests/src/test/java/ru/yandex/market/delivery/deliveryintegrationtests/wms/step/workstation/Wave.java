package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.workstation;

import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Order;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.WaveId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.WaveProgress;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.WaveStatus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.common.PopupAlert;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.leftmenu.LeftMenu;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.topcontextmenu.TopContextMenu;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.topmenu.TopMenu;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wave.execution.OrderWithoutWavePage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wave.execution.wavemanagement.WaveDetailPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wave.execution.wavemanagement.WaveManagementPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wave.execution.wavemanagement.WavePage;

@Resource.Classpath({"wms/infor.properties"})
public class Wave {
    private static final Logger log = LoggerFactory.getLogger(Wave.class);

    private WebDriver driver;
    private TopMenu topMenu;
    private LeftMenu leftMenu;
    private OrderWithoutWavePage orderWithoutWavePage;
    private WavePage wavePage;
    private TopContextMenu topContextMenu;
    private WaveManagementPage waveManagementPage;
    private PopupAlert popupAlert;
    private WaveDetailPage waveDetailPage;

    public Wave(WebDriver drvr) {

        PropertyLoader.newInstance().populate(this);

        this.driver = drvr;
        topMenu = new TopMenu(driver);
        leftMenu = new LeftMenu(driver);
        orderWithoutWavePage = new OrderWithoutWavePage(driver);
        wavePage = new WavePage(driver);
        topContextMenu = new TopContextMenu(driver);
        waveManagementPage = new WaveManagementPage(driver);
        popupAlert = new PopupAlert(driver);
        waveDetailPage = new WaveDetailPage(driver);
    }

    @Step("Создаем волну с заказом {order.fulfillmentId}")
    public WaveId createWaveWithOrder(Order order) {
        topMenu.whSelectorClick().openWarehouse();
        leftMenu.Wave().Execution().orderWithoutWave();

        orderWithoutWavePage
                .inputOrderId(order.getFulfillmentId())
                .filterButtonClick()
                .selectFirstResult()
                .createWaveClick();

        return new WaveId(wavePage.getWaveId());
    }

    @Step("Создаем пакетный заказ для волны")
    public void createBatchOrder(WaveId waveId) {
        topMenu.whSelectorClick().openWarehouse();
        leftMenu.Wave().Execution().waveManagement();

        waveManagementPage
                .inputWaveId(waveId.getId())
                .filterButtonClick()
                .openFirstWave();

        topContextMenu.Function().createBatchOrder();

        Assertions.assertTrue(wavePage.getBatchOrderId().matches("B[0-9]*"),
                "Не найден номер пакетного заказа волны");
    }

    @Step("Резервируем товары для волны")
    public void reserveGoods(WaveId waveId) {
        topMenu.whSelectorClick().openWarehouse();
        leftMenu.Wave().Execution().waveManagement();

        waveManagementPage
                .inputWaveId(waveId.getId())
                .filterButtonClick()
                .openFirstWave();

        topContextMenu.Actions().reserve();

        wavePage.OrderList().refreshOrderList();

        Assertions.assertTrue(wavePage.OrderList().getTotalGoods() == wavePage.OrderList().getReservedGoods(),
                "Количество зарезервированых товаров не равно общему количеству"
        );
    }

    @Step("Запуск волны")
    public void startWave(WaveId waveId) {
        topMenu.whSelectorClick().openWarehouse();
        leftMenu.Wave().Execution().waveManagement();

        waveManagementPage
                .inputWaveId(waveId.getId())
                .filterButtonClick()
                .openFirstWave();

        topContextMenu.Actions().start();

        topMenu.whSelectorClick().openWarehouse();
        leftMenu.Wave().Execution().waveManagement();

        waveManagementPage
                .inputWaveId(waveId.getId())
                .filterButtonClick();

        Assertions.assertTrue(waveManagementPage.getWaveProgressState().equals(WaveProgress.STARTED),
                "Волна не запущена"
        );

        waveManagementPage.openFirstWave();

        Assertions.assertTrue(wavePage.getWaveStatus().equals(WaveStatus.STARTED),
                "Волна не запущена"
        );
    }

    @Step("Получаем номер последней созданной волны")
    public WaveId getLastWave(){
        topMenu.whSelectorClick().openWarehouse();
        leftMenu.Wave().Execution().waveManagement();

        waveManagementPage
                .filterButtonClick();
        waveManagementPage.openFirstWave();
        return new WaveId(wavePage.getWaveId());
    }

    @Step("Получаем номер волны для заказа {order.fulfillmentId}")
    public WaveId getOrderWave(Order order){
        topMenu.whSelectorClick().openWarehouse();
        leftMenu.Wave().Execution().waveDetail();

        waveDetailPage.inputOrderId(order.getFulfillmentId());
        waveDetailPage.filterButtonClick();
        waveDetailPage.resultsShown();
        return new WaveId(waveDetailPage.getWaveNumber());
    }

    @Step("Отменить резервирование последней волны, если есть запущенные волны")
    public void stopWavesIfStartedByWaveId(WaveId waveId) {
        topMenu.whSelectorClick().openWarehouse();
        leftMenu.Wave().Execution().waveManagement();

        waveManagementPage
                .inputWaveId(waveId.getId())
                .inputReserved(">0")
                .filterButtonClick();

        if (waveManagementPage.resultsShown()) {
            waveManagementPage.openFirstWave();
            topContextMenu.Actions().cancelReserve();

            popupAlert.yesButtonClick();
        }
    }

}
