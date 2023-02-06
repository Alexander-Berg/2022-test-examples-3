package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui;

import java.util.concurrent.TimeUnit;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ex.ElementNotFound;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.Android.picking.AreaInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.balances.BalancesListPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.balances.BalancesOfUitPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.balances.BalancesPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.PrinterInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.TableInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.TablePreloader;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.consolidation.order.SortStationPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.deliverySorting.ContainerInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.dimensionControl.StationPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.initialReceiving.BoxIdInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.inventorization.CellInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.offSystemActivity.FinishOffSystemActivityPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.offSystemActivity.StartOffSystemActivityPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.outbound.recreateTask.ChooseTask;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.packing.TablePackingPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.picking.ContainerLabelInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.picking.WorkingAreaInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.placement.EmptyIdScanPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.placement.OrderCreationPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.precons.ContainerConsInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving.BbxdTableInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving.PalletInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving.ReceivingPalletMovePage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receivingAdmin.SuppliesListPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.replenishment.OrderReplenishmentPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.report.LabelsMenuPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.report.ReportLeftMenuPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.shipping.DoorToDeliveryPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.shipping.GatePage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.shipping.PlaceDropIdPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.shipping.ShipmentControlPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.shipping.ShippingOrderControlPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.shippingsorter.ShippingSorterSettingsPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.shippingsorter.SorterOrderManagementPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.supervisorActivity.EmployeeStatsPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.transport.TasksWithLocationPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.uitAdmin.UitInputDeletePage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.wave.OrdersListPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.wave.WavesListPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.WebDriverTimeout;

import static com.codeborne.selenide.Selectors.byXpath;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;

@Slf4j
public class MenuPage extends AbstractPage {

    //Из-за того что страница динамическая и перерисовывается не получается
    //добиться стабильной работы если не искать элементы непосредственно перед обращением к ним

    @FindBy(xpath = "//div[@data-e2e='menu-tree']//input")
    private SelenideElement input;

    private final String inboundPath = "1";
    private final String transportPath = "2";
    private final String outboundPath = "3";
    private final String adminPath = "4";

    private final String pickingButtonPath = "31";

    private final String manualWavesStartButtonPath = "41212";

    private final String returnsManualInput = "4. Приёмка невыкупов";
    private final String receivingManualInput = "1. Приёмка";
    private final String pickingManualInput = "1. Отбор заказов";
    private final String packingManualInput = "2. Упаковка";
    private final String palletInventorizationManualInput = "1. Инвентаризация";
    private final String initialReceivingManualInput = "2. Первичная приёмка";

    private final String receivingPath = "11";
    private final String initialReceivingPath = "12";
    private final String receivingVirtualUitPath = "14";
    private final String tasksPath = "21";
    private final String receivingPalletMovePath = "23";
    private final String palletInventorizationPath = "24";
    private final String placingPath = "26";
    private final String nokPath = "27";
    private final String movingPath = "28";
    private final String measurePath = "213";
    private final String pickingPath = "311";
    private final String pickingWithdrawalPath = "312";
    private final String multiPickingPath = "313";
    private final String orderSortingPath = "334";
    private final String packingPath = "32";
    private final String deliverySortingPath =  "331";
    private final String preconsPath = "361";
    private final String consolidationOrderPath = "362";
    private final String shippingPlacementPath =  "372";
    private final String newShippingPath =  "373";
    private final String reportsAndLabelsPath = "451";
    private final String labelsPath = "452";
    private final String balancesPath = "51";
    private final String balanceAdministrationPath = "521";
    private final String balancesOfUitPath = "522";
    private final String receivingAdminPath = "711";
    private final String startOfSystemActivityPath = "251";
    private final String endOfSystemActivityPath = "252";
    private final String employeeStatsPath = "911";

    private final String sorterOrderManagementPath = "41222";
    private final String shippingSorterSettingsPath = "41223";
    private final String shippingDoorToDeliveryPath = "41231";
    private final String shippingControlPath = "41232";
    private final String shippingOrderControlPath = "41233";
    private final String ordersListPath = "412121";
    private final String wavesListPath = "412122";
    private final String cancelUitReceivingPath = "413323";
    private final String orderReplenishmentPath = "47";

    private final String linkOrderIdToBoxPath = "15";

    private final String receivingXpath =
            String.format("//span[@data-e2e='%s']", receivingPath);
    private final String receivingVirtualUitXpath = String.format("//span[@data-e2e='%s']",
            receivingVirtualUitPath);
    private final String pickingXpath =
            String.format("//span[@data-e2e='%s']", pickingPath);
    private final String packingXpath =
            String.format("//span[@data-e2e='%s']", packingPath);
    private final String palletInventorizationFlowXpath =
            String.format("//span[@data-e2e='%s']", palletInventorizationPath);
    private final String pickingFlowXpath =
            String.format("//span[@data-e2e='%s']", pickingButtonPath);
    private final String initialReceivingXpath =
            String.format("//span[@data-e2e='%s']", initialReceivingPath);
    private final String manualWavesStartFlowXpath =
            String.format("//span[@data-e2e='%s']", manualWavesStartButtonPath);
    private final String ordersListXpath =
            String.format("//span[@data-e2e='%s']", ordersListPath);
    private final String wavesListXpath =
            String.format("//span[@data-e2e='%s']", wavesListPath);
    private final String tasksButtonXpath = String.format("//span[@data-e2e='%s']", tasksPath);
    private final String startOfSystemActivityXpath =
            String.format("//span[@data-e2e='%s']", startOfSystemActivityPath);
    private final String endOfSystemActivityXpath =
            String.format("//span[@data-e2e='%s']", endOfSystemActivityPath);
    private final String employeeStatsXpath =
            String.format("//span[@data-e2e='%s']", employeeStatsPath);

    private final String inboundFlowXpath = String.format("//span[@data-e2e='%s']", inboundPath);
    private final String outboundFlowXpath = String.format("//span[@data-e2e='%s']", outboundPath);
    private final String tasksAndStoringButtonXpath = String.format("//span[@data-e2e='%s']", transportPath);
    private final String adminFlowXpath = String.format("//span[@data-e2e='%s']", adminPath);
    private final String measureXpath = String.format("//span[@data-e2e='%s']", measurePath);
    private TablePreloader tablePreloader;


    public MenuPage(WebDriver driver) {
        super(driver);
        this.tablePreloader = new TablePreloader(driver);
    }

    @Step("Вводим путь до приёмки")
    public void inputReceivingPath() {
        inputMenuPath(receivingPath);
    }

    @Step("Вводим путь до БезУИТной приёмки")
    public TableInputPage inputReceivingVirtualUitPath() {
        inputMenuPath(receivingVirtualUitPath);
        return new TableInputPage(driver);
    }

    @Step("Вводим путь до первичной приемки")
    public PrinterInputPage inputInitialReceivingPath() {
        inputMenuPath(initialReceivingPath);
        return new PrinterInputPage(driver);
    }

    @Step("Вводим путь до инвентаризации")
    public CellInputPage inputInventorizationPath() {
        inputMenuPath(palletInventorizationPath);
        return new CellInputPage(driver);
    }

    @Step("Вводим путь до сортировки по СД")
    public ContainerInputPage inputDeliverySortingPath() {
        inputMenuPath(deliverySortingPath);
        return new ContainerInputPage(driver);
    }

    @Step("Вводим путь до привязки заказа к коробке")
    public BoxIdInputPage linkOrderIdToBoxPath() {
        inputMenuPath(linkOrderIdToBoxPath);
        return new BoxIdInputPage(driver);
    }

    @Step("Вводим путь до отбора")
    public WorkingAreaInputPage inputPickingPath() {
        inputMenuPath(pickingPath);
        return new WorkingAreaInputPage(driver);
    }

    @Step("Вводим путь до отбора при активном отборе")
    public ContainerLabelInputPage inputPickingPathWithActiveAssignment() {
        inputMenuPath(pickingPath);
        return new ContainerLabelInputPage(driver);
    }

    @Step("Вводим путь до отбора изъятий")
    public WorkingAreaInputPage inputPickingWithdrawalPath() {
        inputMenuPath(pickingWithdrawalPath);
        return new WorkingAreaInputPage(driver);
    }

    @Step("Вводим путь до отбора")
    public WorkingAreaInputPage inputMultiPickingPath() {
        inputMenuPath(multiPickingPath);
        return new WorkingAreaInputPage(driver);
    }

    @Step("Вводим путь до мультиотбора в Android")
    public AreaInputPage inputAndroidMultipickingPath() {
        inputMenuPath(multiPickingPath);
        return new AreaInputPage();
    }

    @Step("Вводим путь до размещения")
    public OrderCreationPage inputPlacementPath() {
        inputMenuPath(placingPath);
        return new OrderCreationPage(driver);
    }

    @Step("Вводим путь до перемещения")
    public EmptyIdScanPage inputMovementPath() {
        inputMenuPath(movingPath);
        return new EmptyIdScanPage(driver);
    }

    @Step("Вводим путь до упаковки")
    public TablePackingPage inputPackingPath() {
        inputMenuPath(packingPath);
        return new TablePackingPage(driver);
    }

    @Step("Вводим путь до предконсолидации")
    public ContainerConsInputPage inputPreconsPath() {
        inputMenuPath(preconsPath);
        return new ContainerConsInputPage(driver);
    }

    @Step("Вводим путь до консолидации по заказам")
    public SortStationPage inputConsolidationOrderPath() {
        inputMenuPath(consolidationOrderPath);
        return new SortStationPage(driver);
    }

    @Step("Вводим путь до админки приемки")
    public SuppliesListPage inputReceivingAdminPath() {
        inputMenuPath(receivingAdminPath);
        return new SuppliesListPage(driver);
    }

    @Step("Открываем меню перемещения паллеты приемки")
    public PalletInputPage receivingPalletMovePath() {
        inputMenuPath(receivingPalletMovePath);
        return new PalletInputPage(driver);
    }

    @Step("Вводим путь до работы с ТОТами конвейера")
    public ChooseTask inputNokPath() {
        inputMenuPath(nokPath);
        return new ChooseTask(driver);
    }

    @Step("Вводим путь до Просмотра балансов")
    public BalancesPage inputBalancesPath() {
        inputMenuPath(balancesPath);
        return new BalancesPage(driver);
    }

    @Step("Вводим путь до Администрирования балансов УИТов")
    public BalancesOfUitPage inputBalancesOfUitPath() {
        inputMenuPath(balancesOfUitPath);
        return new BalancesOfUitPage(driver);
    }

    @Step("Вводим путь до Начать активность")
    public StartOffSystemActivityPage inputSystemActivityPath() {
        inputMenuPath(startOfSystemActivityPath);
        return new StartOffSystemActivityPage(driver);
    }

    @Step("Вводим путь до Завершить активность")
    public FinishOffSystemActivityPage inputEndOfSystemActivityPath() {
        inputMenuPath(endOfSystemActivityPath);
        return new FinishOffSystemActivityPage(driver);
    }

    @Step("Вводим путь до Статусов сотрудников смены")
    public EmployeeStatsPage inputEmployeeStatsPath() {
        inputMenuPath(employeeStatsPath);
        return new EmployeeStatsPage(driver);
    }

    @Step("Нажимаем на кнопку Входящий поток")
    private void clickInnerFlowButton() {
        clickMenuButtonByXpath(inboundFlowXpath);
    }

    @Step("Нажимаем на кнопку Отборы")
    private void clickPickingFlowButton() {
        clickMenuButtonByXpath(pickingFlowXpath);
    }

    @Step("Нажимаем на кнопку Исходящий поток")
    private void clickOuterFlowButton() {
        clickMenuButtonByXpath(outboundFlowXpath);
    }

    @Step("Нажимаем на кнопку Задания и хранение")
    public void clickTasksAndStoringButton() {
        clickMenuButtonByXpath(tasksAndStoringButtonXpath);
    }

    @Step("Нажимаем на кнопку 1. Задания")
    public TasksWithLocationPage clickTasksButton() {
        clickTasksAndStoringButton();
        clickMenuButtonByXpath(tasksButtonXpath);
        return new TasksWithLocationPage(driver, "");
    }

    @Step("Нажимаем на кнопку Инвентаризация")
    public CellInputPage clickInventorizationTasksButton() {
        clickTasksAndStoringButton();
        clickMenuButtonByXpath(palletInventorizationFlowXpath);
        return new CellInputPage(driver);
    }

    @Step("Вводим путь до Обмера ВГХ")
    public StationPage inputMeasurePath() {
        clickTasksAndStoringButton();
        clickMenuButtonByXpath(measureXpath);
        return new StationPage(driver);
    }

    @Step("Нажимаем кнопку  БезУИТная приёмка")
    public TableInputPage clickInboundVirtualUitButton() {
        clickInnerFlowButton();
        clickMenuButtonByXpath(receivingVirtualUitXpath);
        return new TableInputPage(driver);
    }

    @Step("Нажимаем кнопку Приёмка")
    public TableInputPage clickInboundButton() {
        clickInnerFlowButton();
        clickMenuButtonByXpath(receivingXpath);
        return new TableInputPage(driver);
    }

    @Step("Нажимаем кнопку Первичная приёмка")
    public PrinterInputPage clickInitialReceivingButton() {
        clickInnerFlowButton();
        clickMenuButtonByXpath(initialReceivingXpath);
        return new PrinterInputPage(driver);
    }

    @Step("Нажимаем кнопку Отбор")
    public WorkingAreaInputPage clickPickingButton() {
        clickOuterFlowButton();
        clickPickingFlowButton();
        clickMenuButtonByXpath(pickingXpath);
        return new WorkingAreaInputPage(driver);
    }

    @Step("Нажимаем кнопку Упаковка")
    public TablePackingPage clickPackingButton() {
        clickOuterFlowButton();
        clickMenuButtonByXpath(packingXpath);
        return new TablePackingPage(driver);
    }

    @Step("Нажимаем на кнопку Администрирование")
    public void clickAdminButton() {
        clickMenuButtonByXpath(adminFlowXpath);
    }

    @Step("Нажимаем на кнопку Ручной запуск волн")
    private void clickManualWavesStartFlowButton() {
        clickMenuButtonByXpath(manualWavesStartFlowXpath);
    }

    @Step("Нажимаем кнопку Список заказов")
    public OrdersListPage clickOrdersListButton() {
        clickAdminButton();
        clickManualWavesStartFlowButton();
        clickMenuButtonByXpath(ordersListXpath);
        return new OrdersListPage(driver);
    }

    @Step("Нажимаем кнопку Список волн")
    public WavesListPage clickWavesListButton() {
        clickAdminButton();
        clickManualWavesStartFlowButton();
        clickMenuButtonByXpath(wavesListXpath);
        return new WavesListPage(driver);
    }

    @Step("Переходим в меню настроек выходов конвейера для доставки и сортировки")
    public ShippingSorterSettingsPage inputShippingSorterSettingsPath() {
        inputMenuPath(shippingSorterSettingsPath);
        return new ShippingSorterSettingsPage(driver);
    }

    @Step("Переходим в таблицу заданий на сортировку")
    public SorterOrderManagementPage inputSorterOrderManagementPath() {
        inputMenuPath(sorterOrderManagementPath);
        return new SorterOrderManagementPage(driver);
    }

    @Step("Открываем Отчеты")
    public ReportLeftMenuPage openReports() {
        inputMenuPath(reportsAndLabelsPath);
        return new ReportLeftMenuPage(driver);
    }

    @Step("Открываем Этикетки")
    public LabelsMenuPage openLabels() {
        inputMenuPath(labelsPath);
        return new LabelsMenuPage(driver);
    }

    @Step("Вводим название процесса: Приёмка")
    public TableInputPage inputReceivingManualPath() {
        inputMenuPath(receivingManualInput);
        return new TableInputPage(driver);
    }

    @Step("Вводим название процесса: Перемещения")
    public TasksWithLocationPage inputTransportationManualInput() {
        inputMenuPath(tasksPath);
        return new TasksWithLocationPage(driver, "");
    }

    @Step("Вводим название процесса: Первичная приёмка")
    public PrinterInputPage inputInitialReceivingManualPath() {
        inputMenuPath(initialReceivingManualInput);
        return new PrinterInputPage(driver);
    }

    @Step("Вводим название процесса: Отбор")
    public WorkingAreaInputPage inputPickingManualPath() {
        inputMenuPath(pickingManualInput);
        return new WorkingAreaInputPage(driver);
    }

    @Step("Вводим название процесса: Упаковка")
    public TablePackingPage inputPackingManualPath() {
        inputMenuPath(packingManualInput);
        return new TablePackingPage(driver);
    }

    @Step("Вводим название процесса: Инветаризация")
    public CellInputPage inputPalletInventorizationManualPath() {
        inputMenuPath(palletInventorizationManualInput);
        return new CellInputPage(driver);
    }

    @Step("Разлогиниваемся в UI")
    public LoginPage logout() {
        clickMenuButtonByXpath("//span[@data-e2e='13']");
        return new LoginPage(driver);
    }

    private void clickMenuButtonByXpath(String xpath) {
        $(byXpath(xpath)).click();
    }

    @Step("Вводим путь до Размещения для отгрузки")
    public PlaceDropIdPage inputShippingPath() {
        inputMenuPath(shippingPlacementPath);
        return new PlaceDropIdPage(driver);
    }

    @Step("Вводим путь до Список заказов")
    public OrdersListPage inputOrdersListPath() {
        inputMenuPath(ordersListPath);
        tablePreloader.waitUntilHidden();
        return new OrdersListPage(driver);
    }

    @Step("Вводим путь до Пополнения под заказ")
    public OrderReplenishmentPage inputOrdersReplenishmentPath() {
        inputMenuPath(orderReplenishmentPath);
        return new OrderReplenishmentPage(driver);
    }

    @Step("Вводим путь до Список волн")
    public WavesListPage inputWavesListPath() {
        inputMenuPath(wavesListPath);
        tablePreloader.waitUntilHidden();
        return new WavesListPage(driver);
    }

    @Step("Вводим путь до Новой Отгрузки")
    public GatePage inputNewShippingPath() {
        inputMenuPath(newShippingPath);
        return new GatePage(driver);
    }

    @Step("Вводим путь до Привязки ворот к СД")
    public DoorToDeliveryPage inputShippingDoorToDeliveryPath() {
        inputMenuPath(shippingDoorToDeliveryPath);
        return new DoorToDeliveryPage(driver);
    }

    @Step("Вводим путь до Диспетчера отгрузки")
    public ShipmentControlPage inputShipControlPath() {
        inputMenuPath(shippingControlPath);
        return new ShipmentControlPage(driver);
    }

    @Step("Вводим путь до Отмены приемки УИТ")
    public UitInputDeletePage inputCancelUitReceivingPath() {
        inputMenuPath(cancelUitReceivingPath);
        return new UitInputDeletePage(driver);
    }

    @Step("Вводим путь до Отгрузки заказов")
    public ShippingOrderControlPage inputShippingOrderPath() {
        inputMenuPath(shippingOrderControlPath);
        return new ShippingOrderControlPage(driver);
    }

    @Step("Вводим путь до Администрирования балансов")
    public BalancesListPage inputBalanceAdministrationPath(){
        inputMenuPath(balanceAdministrationPath);
        tablePreloader.waitUntilHidden();
        return new BalancesListPage(driver);
    }

    @Step("Вводим путь до сортировки BBXD")
    public BbxdTableInputPage sortingOrderPath() {
        inputMenuPath(orderSortingPath);
        return new BbxdTableInputPage(driver);
    }

    /**
     * Если не удается найти инпут на странице меню, то игнорируем исключение
     * потому что возможно мы уже перешли на нужную страницу
     */
    private void inputMenuPath(String processPath) {
        try {
            getWebDriver().manage().timeouts().implicitlyWait(WebDriverTimeout.MEDIUM_WAIT_TIMEOUT, TimeUnit.SECONDS);
            log.info("Inputting menu path: " + processPath);
            input.setValue(processPath);
            getWebDriver().manage().timeouts().implicitlyWait(WebDriverTimeout.LONG_WAIT_TIMEOUT, TimeUnit.SECONDS);
        } catch (ElementNotFound e) {
                log.info("""
                        Caught error while inputting menu path: %s
                        Ignoring error and continuing test execution
                        """.formatted(e.getMessage()));
        }
    }
}
