package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.ui;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.Retrier;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Order;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.WaveId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.WaveStatus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.MenuPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.wave.OrdersListPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.wave.WavesListPage;

public class Wave {
    private WebDriver driver;
    private MenuPage menuPage;

    public Wave(WebDriver driver) {
        this.driver = driver;
        this.menuPage = new MenuPage(driver);
    }

    @Step("Создаем волну с заказом {order.fulfillmentId}")
    public WaveId createWaveWithOrder(Order order) {
        OrdersListPage ordersListPage = menuPage
                .inputOrdersListPath();

        WavesListPage wavesListPage = Retrier.retryInteractionWithElement(() ->
                ordersListPage
                        .resetFiltersClick()
                        .inputOrderId(order.getFulfillmentId())
                        .selectAllResults()
                        .createWaveClick()
                        .clickCreateButton()
        );

        return new WaveId(wavesListPage.getWaveId());
    }

    @Step("Создаем волну с заказами {orderIds}")
    public WaveId createWaveWithOrders(Collection<String> orderIds) {
        OrdersListPage ordersListPage = menuPage
                .inputOrdersListPath();

        String searchOrderIdString = String.join(", ", orderIds);

        WavesListPage wavesListPage = Retrier.retryInteractionWithElement(() ->
                ordersListPage
                        .resetFiltersClick()
                        .inputOrderId(searchOrderIdString)
                        .selectAllResults()
                        .createWaveClick()
                        .clickCreateButton()
        );

        return new WaveId(wavesListPage.getWaveId());
    }

    @Step("Резервируем волну перед запуском")
    public void reserveWave(WaveId waveId) {
        menuPage
                .inputWavesListPath()
                .inputWaveId(waveId.getId())
                .firstResultMoreActionsClick()
                .firstResultReserveClick()
                .clickReserveButton();
    }

    @Step("Пробуем зарезервировать волну")
    public void tryReserveWaveAndReplenish(WaveId waveId) {
        menuPage
                .inputWavesListPath()
                .inputWaveId(waveId.getId())
                .firstResultMoreActionsClick()
                .firstResultReserveClick()
                .clickReserveButtonFail()
                .proceedReplenish();
    }

    @Step("Запускаем волну")
    public void startWave(WaveId waveId) {
        menuPage
                .inputWavesListPath()
                .inputWaveId(waveId.getId())
                .firstResultMoreActionsClick()
                .firstResultStartClick();
    }

    @Step("Принудительно запускаем волну на заданную сортировочную станцию {1}")
    public void forceStartWaveOntoSortingStation(WaveId waveId, String sortingStation) {
        menuPage
                .inputWavesListPath()
                .inputWaveId(waveId.getId())
                .firstResultMoreActionsClick()
                .firstResultForceStartClick()
                .inputSortingStationAndClickStartButton(sortingStation);
    }

    @Step("Принудительно запускаем волну на заданную линию консолидации {1}")
    public void forceStartWaveOntoConsolidationLine(WaveId waveId, String consolidationLine) {
        menuPage
                .inputWavesListPath()
                .inputWaveId(waveId.getId())
                .firstResultMoreActionsClick()
                .firstResultForceStartClick()
                .inputConsolidationLineAndClickStartButton(consolidationLine);
    }

    @Step("Проверям, что заказу проставилось здание")
    public void checkBuildingForOrder(String orderKey) {
        menuPage
                .inputOrdersListPath()
                .resetFiltersClick()
                .inputOrderId(orderKey)
                .checkBuilding();
    }

    @Step("Находим волну заказа {orderKey}")
    public WaveId getOrderWave(String orderKey) {
        menuPage
                .inputOrdersListPath();

        //без создания нового объекта OrdersListPage ретраер почему-то пытается сначала ввести путь до этой страницы
        String waveId = Retrier.retry(() -> new OrdersListPage(driver).resetFiltersClick()
                .inputOrderId(orderKey)
                .getWaveId(), Retrier.RETRIES_SMALL, Retrier.TIMEOUT_TINY, TimeUnit.SECONDS);
        return new WaveId(waveId);
    }

    @Step("Разрезервируем волну")
    public void unreserveWave(WaveId waveId) {
        WavesListPage wavesListPage = menuPage
                .inputWavesListPath()
                .resetTableFilters()
                .inputWaveId(waveId.getId());
        String waveStatus = wavesListPage.checkWaveStatus();
        if (waveStatus.equals(WaveStatus.RESERVED)) {
            wavesListPage
                    .firstResultMoreActionsClick()
                    .firstResultUnreserveClick()
                    .clickUneserveButton();
        }
    }


    public void waitWaveStatusIs(String waveId, WaveStatus status) {
        waitWaveStatusIs(List.of(waveId), status);
    }

    @Step("Ждём, пока статус волн {waveIds} станет {status}")
    public void waitWaveStatusIs(Collection<String> waveIds, WaveStatus status) {
        for (var waveId : waveIds) {
            Retrier.retry(() -> {
                WavesListPage wavesListPage = menuPage
                        .inputWavesListPath()
                        .resetTableFilters()
                        .inputWaveId(waveId);
                WaveStatus waveStatus = wavesListPage.getWaveStatus();
                checkWaveStatus(status, waveStatus);
            }, Retrier.RETRIES_BIGGEST, Retrier.TIMEOUT_TINY, TimeUnit.SECONDS);
        }
    }

    @Step("Проверяем, что ожидаемый статус волны {status} равен фактическому {actualOrderStatus}")
    private void checkWaveStatus(WaveStatus status, WaveStatus actualWaveStatus) {
        Assertions.assertEquals(status, actualWaveStatus, "Волна в неверном статусе");
    }
}
