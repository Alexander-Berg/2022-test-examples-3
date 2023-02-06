package ru.yandex.market.tpl.integration.tests.facade;

import io.qameta.allure.Step;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

/**
 * Фасад к АПИ Курьерской Платформы, который содержит логически связанные действия.
 * Является statefull, т.е. умеет сохранять состояние ответов ручек, чтобы использовать в будущем в других вызовах.
 */
@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class ApiFacade {
    private final PublicApiFacade publicApiFacade;
    private final ManualApiFacade manualApiFacade;

    @Step("Создание заказа и забор курьером из СЦ")
    public void createAndPickupOrder() {
        createOrder();
        pickupOrders();
        publicApiFacade.successCallToRecipient();
    }

    @Step("Создание заказа и забор курьером из СЦ")
    public void createAndPickupMultiOrder() {
        createMultiOrder();
        pickupOrders();
        publicApiFacade.successCallToRecipient();
    }


    @Step("Создание заказа")
    public void createOrder() {
        manualApiFacade.createShift();
        manualApiFacade.createUserShift();
        manualApiFacade.createEmptyRoutePoint();
        manualApiFacade.createDeliveryTask();
    }


    @Step("Создание мультиказа")
    public void createMultiOrder() {
        manualApiFacade.createShift();
        manualApiFacade.createUserShift();
        manualApiFacade.createEmptyRoutePoint();
        manualApiFacade.createDeliveryMultiTask();
    }


    @Step("Забор посылок")
    public void pickupOrders() {
        publicApiFacade.startUserShift();
        publicApiFacade.updateDataButton();
        publicApiFacade.updateCurrentRoutePoint();
        publicApiFacade.arriveToRoutePoint();
        publicApiFacade.scanItems();
        publicApiFacade.updateCurrentRoutePoint();
    }

    @Step("Доехать до клиента и передать посылку")
    public void arriveToRecipientAndGiveOrder() {
        publicApiFacade.arriveToRoutePoint();
        publicApiFacade.giveParcel();
        publicApiFacade.finishLastDeliveryTask();
    }

    @Step("Принять оплату и передать посылку")
    public void giveOrderWithPayment() {
        publicApiFacade.payAndGiveParcel();
        publicApiFacade.finishLastDeliveryTask();
    }

    @Step("Вернуться в СЦ и закончить смену")
    public void finishShiftInSc() {
        publicApiFacade.arriveToRoutePoint();
        publicApiFacade.finishUserShift();
    }

    @Step("Вернуться в СЦ передать деньги в кассу и закончить смену")
    public void finishShiftInScWithCash() {
        publicApiFacade.arriveToRoutePoint();
        publicApiFacade.returnCash();
        publicApiFacade.finishUserShift();
    }

    @Step("Вернуться в СЦ с посылками и закончить смену")
    public void finishShiftInScWithOrders() {
        publicApiFacade.arriveToRoutePoint();
        publicApiFacade.startReturnOrders();
        publicApiFacade.enterOrderCode();
        publicApiFacade.finishUserShift();
    }

    @Step("Создание заказа для постомата")
    public void createLockerOrder() {
        manualApiFacade.createShift();
        manualApiFacade.createUserShift();
        manualApiFacade.createLockerDeliveryRoutePoint();
    }
}
