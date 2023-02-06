package ui_tests.src.test.java.pages;

import entity.Entity;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import tools.Tools;
import unit.Config;

public class Navigate {
    private WebDriver webDriver;

    public Navigate(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Открыть страницу со списком всех тикетов
     *
     * @return
     */
    public void openLVAllTickets() {
        openPageByMetaClassAndID("root@1?tabBar=1");
    }

    /**
     * Открыть страницу со списком всех тикетов очереди "Логистическая поддержка Т Я.До > Общие"
     *
     * @return
     */
    public void openLVDeliveryLogisticSupportQuestion() {
        openPageByMetaClassAndID("service@77989904");
    }

    /**
     * Открыть страницу очереди Беру > Общие вопросы
     *
     * @return
     */
    public void openServicesBeruQuestion() {
        openPageByMetaClassAndID("service@30013907");
    }

    /**
     * Открыть страницу очереди Беру > Исходящий звонок
     *
     * @return
     */
    public void openServicesOutgoingCall() {
        openPageByMetaClassAndID("service@75776002");

    }

    /**
     * Открыть страницу очереди Беру > Входящий звонок
     *
     * @return
     */
    public void openServicesIncomingCall() {
        openPageByMetaClassAndID("service@73902202");
    }

    /**
     * Открыть страницу со списком всех очередей
     *
     * @return
     */
    public void openLVAllServices() {
        openPageByMetaClassAndID("root@1?tabBar=2");
    }

    /**
     * Открыть страницу очереди Беру > Недозвон СД
     *
     * @return
     */
    public void openBeruDeliveryServiceFailedToDeliver() {
        openPageByMetaClassAndID("service@88526602");

    }

    /**
     * Открыть страницу очереди Беру > Отмена Dropshipping
     *
     * @return
     */
    public void openCrossdocCancellation() {
        openPageByMetaClassAndID("service@78531002");
    }

    /**
     * Открыть страницу очереди Беру > Подтверждение фрод-заказов
     *
     * @return
     */
    public void openFraudConfirmation() {
        openPageByMetaClassAndID("service@72263102");

    }

    /**
     * Открыть страницу очереди Беру > Подтверждение предзаказов
     *
     * @return
     */
    public void openPreorderConfirmation() {
        openPageByMetaClassAndID("service@72263102");
    }

    /**
     * Открыть страницу очереди Маркет > DSBS"
     */
    public void openYandexMarketDSBS() {
        openPageByMetaClassAndID("service@118491884");
    }

    /**
     * Открыть страницу заказа по номеру заказа
     *
     * @param orderNumber
     */
    public void openOrderPageByOrderNumber(String orderNumber) {
        webDriver.get(Config.getProjectURL() + "/order/" + orderNumber);
        Tools.waitElement(webDriver).waitTime(3000);
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();
        Tools.waitElement(webDriver).waitVisibilityElement(By.xpath("//*[text()='Заказ']"));

    }

    /**
     * Открыть страницу справочника по его id
     *
     * @param idCatalog id справочника
     */
    public void openCatalogPage(String idCatalog) {
        webDriver.get(Config.getProjectURL() + "/entity/catalog@" + idCatalog);
        Tools.waitElement(webDriver).waitVisibilityElement(By.xpath("//*[text()='Элементы']"));
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();
    }

    public void openPageByMetaClassAndID(String metaClassAndID) {
        if (metaClassAndID == null) {
            throw new Error("gid, по которому пытается перейти автотест, равен null");
        }
        webDriver.get(Config.getProjectURL() + "/entity/" + metaClassAndID);
        Tools.waitElement(webDriver).waitTime(2000);
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();
        Entity.toast(webDriver).hideNotificationError();
    }

}
