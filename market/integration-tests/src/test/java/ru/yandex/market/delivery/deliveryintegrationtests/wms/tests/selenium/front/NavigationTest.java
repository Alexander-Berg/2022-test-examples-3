package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.front;

import io.qameta.allure.Epic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;

@DisplayName("Selenium: Новый интерфейс - Меню")
@Epic("Selenium Tests")
@Resource.Classpath("wms/test.properties")
public class NavigationTest extends AbstractUiTest {

    @RetryableTest
    @DisplayName("Тест меню: кликаем мышкой")
    @ResourceLock("Тест меню: кликаем мышкой")
    public void flowAcceptanceDefectiveInboundTest() {
        uiSteps.Login().PerformLogin();
        uiSteps.Navigation().menu().clickPickingButton();
        uiSteps.Navigation().menu().clickInboundButton();
        uiSteps.Navigation().menu().clickInitialReceivingButton();
        uiSteps.Navigation().menu().clickInboundVirtualUitButton();
        uiSteps.Navigation().menu().clickPackingButton();
    }

    @RetryableTest
    @DisplayName("Тест меню: вводим название процесса")
    @ResourceLock("Тест меню: вводим название процесса")
    public void inputTextNameOfMainFlowsTest() {
        uiSteps.Login().PerformLogin();
        uiSteps.Navigation().menu().inputReceivingManualPath();
        uiSteps.Navigation().menu().inputInitialReceivingManualPath();
        uiSteps.Navigation().menu().inputPickingManualPath();
        uiSteps.Navigation().menu().inputPackingManualPath();
        uiSteps.Navigation().menu().inputPalletInventorizationManualPath();
    }

    @RetryableTest
    @DisplayName("Тест меню: ввод пути до Входящего потока")
    @ResourceLock("Тест меню: ввод пути до Входящего потока")
    public void inputIncomingFlowPathsTest() {
        uiSteps.Login().PerformLogin();
        uiSteps.Navigation().menu().inputReceivingPath();
        uiSteps.Navigation().menu().inputInitialReceivingPath();
        uiSteps.Navigation().menu().inputReceivingVirtualUitPath();
    }

    @RetryableTest
    @DisplayName("Тест меню: ввод пути до раздела Задания и хранение")
    @ResourceLock("Тест меню: ввод пути до раздела Задания и хранение")
    public void inputTaskAndStoragePathsTest() {
        uiSteps.Login().PerformLogin();
        uiSteps.Navigation().menu().inputPlacementPath();
        uiSteps.Navigation().menu().inputMovementPath();
        uiSteps.Navigation().menu().inputNokPath();
        uiSteps.Navigation().menu().inputInventorizationPath();
    }

    @RetryableTest
    @DisplayName("Тест меню: ввод пути до раздела Исходящий поток")
    @ResourceLock("Тест меню: ввод пути до раздела Исходящий поток")
    public void inputOutgoingFlowPathsTest() {
        uiSteps.Login().PerformLogin();
        uiSteps.Navigation().menu().inputDeliverySortingPath();
        uiSteps.Navigation().menu().inputPickingPath();
        uiSteps.Navigation().menu().inputPickingWithdrawalPath();
        uiSteps.Navigation().menu().inputMultiPickingPath();
        uiSteps.Navigation().menu().inputPackingPath();
        uiSteps.Navigation().menu().inputPreconsPath();
        uiSteps.Navigation().menu().inputConsolidationOrderPath();
        uiSteps.Navigation().menu().inputShippingPath();
        uiSteps.Navigation().menu().inputNewShippingPath();
    }

    @RetryableTest
    @DisplayName("Тест меню: ввод пути до раздела Администрирование")
    @ResourceLock("Тест меню: ввод пути до раздела Администрирование")
    public void inputAdministrationPathsTest() {
        uiSteps.Login().PerformLogin();
        uiSteps.Navigation().menu().inputShippingDoorToDeliveryPath();
        uiSteps.Navigation().menu().inputShippingSorterSettingsPath();
        uiSteps.Navigation().menu().inputSorterOrderManagementPath();
        uiSteps.Navigation().menu().inputShipControlPath();
        uiSteps.Navigation().menu().inputShippingOrderPath();
        uiSteps.Navigation().menu().inputOrdersListPath();
        uiSteps.Navigation().menu().inputWavesListPath();
        uiSteps.Navigation().menu().inputCancelUitReceivingPath();
    }

    @RetryableTest
    @DisplayName("Тест меню: ввод пути до раздела Просмотр балансов")
    @ResourceLock("Тест меню: ввод пути до раздела Просмотр балансов")
    public void inputBalancesViewPathsTest() {
        uiSteps.Login().PerformLogin();
        uiSteps.Navigation().menu().inputBalancesPath();
        uiSteps.Navigation().menu().inputBalanceAdministrationPath();
        uiSteps.Navigation().menu().inputBalancesOfUitPath();
    }

    @RetryableTest
    @DisplayName("Тест меню: ввод пути до раздела Документы и справочники")
    @ResourceLock("Тест меню: ввод пути до раздела Документы и справочники")
    public void inputDocumentsAndDirectoriesPathsTest() {
        uiSteps.Login().PerformLogin();
        uiSteps.Navigation().menu().inputReceivingAdminPath();
    }
}
