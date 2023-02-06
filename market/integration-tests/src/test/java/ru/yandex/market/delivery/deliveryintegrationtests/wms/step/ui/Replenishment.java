package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.ui;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.Retrier;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.MenuPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.NotificationDialog;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.replenishment.OrderReplenishmentPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.transport.ReplenishmentPickingSerialNumberApieceInputPage;
import ru.yandex.market.wms.common.spring.enums.replenishment.ProblemStatus;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Replenishment {
    private final WebDriver driver;
    private final MenuPage menuPage;
    private final NotificationDialog notificationDialog;

    public Replenishment(WebDriver driver) {
        this.driver = driver;
        this.menuPage = new MenuPage(driver);
        this.notificationDialog = new NotificationDialog(driver);
    }

    public void waitOrderStatusIs(String orderId, ProblemStatus status) {
        waitOrderStatusIs(List.of(orderId), status);
    }

    @Step("Ждём, пока статус проблемного заказа {orderId} станет {status}")
    public void waitOrderStatusIs(Collection<String> orderIds, ProblemStatus status) {
        log.info("Verifying problem order {} status is {}", orderIds, status);
        OrderReplenishmentPage orderReplenishmentPage = menuPage.inputOrdersReplenishmentPath();
        orderReplenishmentPage.clickProblemOrderTab();
        for (var orderId : orderIds) {
            Retrier.retry(() -> {
                ProblemStatus actualOrderStatus = orderReplenishmentPage
                        .resetFiltersClick()
                        .inputOrderId(orderId)
                        .getOrderStatus();
                checkProblemOrderStatus(status, actualOrderStatus);
            }, Retrier.RETRIES_BIGGEST, Retrier.TIMEOUT_TINY, TimeUnit.SECONDS);
        }
    }

    @Step("Проверяем, что ожидаемый статус проблемного заказа {status} равен фактическому {actualOrderStatus}")
    private void checkProblemOrderStatus(ProblemStatus status, ProblemStatus actualOrderStatus) {
        Assertions.assertEquals(status, actualOrderStatus, "Проблемный заказ в неверном статусе");
    }

    @Step("[Пополнения] Спускаем паллету {pallet} [{storage} -> {buffer}]")
    public void moveDown(String area, String storage, String pallet, String buffer) {
        menuPage.inputTransportationManualInput()
                .getReplenishmentMoveTask(area)
                .inputLocation(storage)
                .inputContainerIdForMove(pallet)
                .inputTargetLocation(buffer)
                .verifyProceedConfirmShown()
                .refuseToFetchNextTask();
    }

    @Step("[Пополнения] Поднимаем паллету {pallet} [{buffer} -> {storage}]")
    public void moveUp(String area, String buffer, String pallet, String storage) {
        menuPage.inputTransportationManualInput()
                .getReplenishmentMoveTask(area)
                .inputLocation(buffer)
                .inputContainerIdForMove(pallet)
                .inputTargetLocation(storage)
                .verifyProceedConfirmShown()
                .refuseToFetchNextTask();
    }

    @Step("[Пополнения] Начинаем отбирать [{buffer} -> {cart}]")
    public ReplenishmentPickingSerialNumberApieceInputPage startPicking(
            String area, String buffer, String pallet, String cart
    ) {
        return menuPage.inputTransportationManualInput()
                .getReplenishmentPickTask(area)
                .inputLocation(buffer)
                .inputContainerIdForPick(pallet)
                .inputTargetContainerId(cart);
    }

    @Step("[Пополнения] Спускаем паллету под изъятие {pallet} [{storage} -> {buffer}]")
    public void moveDownWithdrawal(String area, String storage, String pallet, String buffer) {
        menuPage.inputTransportationManualInput()
                .getReplenishmentWithdrawalMoveTask(area)
                .inputLocation(storage)
                .inputContainerIdForMove(pallet)
                .inputTargetLocation(buffer)
                .verifyProceedConfirmShown()
                .refuseToFetchNextTask();
    }

    @Step("[Пополнения] Поднимаем паллету под изъятие {pallet} [{buffer} -> {storage}]")
    public void moveUpWithdrawal(String area, String buffer, String pallet, String storage) {
        menuPage.inputTransportationManualInput()
                .getReplenishmentWithdrawalMoveTask(area)
                .inputLocation(buffer)
                .inputContainerIdForMove(pallet)
                .inputTargetLocation(storage)
                .verifyProceedConfirmShown()
                .refuseToFetchNextTask();
    }

    @Step("[Пополнения] Проверяем что нет заданий на перемещение под изъятия")
    public void validateNoMoveTasksWithdrawal(String area) {
        menuPage.inputTransportationManualInput()
                .validateNoMoveWithdrawalTask(area);
    }

    @Step("[Пополнения] Проверяем что нет заданий на перемещение под оборачиваемость")
    public void validateNoMoveTasksTurnover(String area) {
        menuPage.inputTransportationManualInput()
                .validateNoMoveTurnoverTask(area);
    }

    @Step("[Пополнения] Начинаем отбирать под изъятие [{buffer} -> {cart}]")
    public ReplenishmentPickingSerialNumberApieceInputPage startPickingWithdrawal(
            String area, String buffer, String pallet, String cart
    ) {
        return menuPage.inputTransportationManualInput()
                .getReplenishmentWithdrawalPickTask(area)
                .inputLocation(buffer)
                .inputContainerIdForPick(pallet)
                .inputTargetContainerId(cart);
    }
}
