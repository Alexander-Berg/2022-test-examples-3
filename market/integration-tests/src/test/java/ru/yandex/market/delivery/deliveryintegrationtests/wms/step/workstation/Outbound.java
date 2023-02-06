package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.workstation;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.OrderStatus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wms.outgoing.outgoingorder.OutgoingOrderPage;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.Resource;

@Resource.Classpath({"wms/infor.properties"})
public class Outbound extends AbstractWSSteps {
    private static final Logger log = LoggerFactory.getLogger(Outbound.class);

    private final OutgoingOrderPage outgoingOrderPage;

    public Outbound(WebDriver drvr) {
        super(drvr);

        outgoingOrderPage = new OutgoingOrderPage(driver);
    }

    @Step("Запустить отбор, Изъятие {outboundId}")
    public void startOutboundComplectation(String outboundId) {
        log.info("Starting complectation for Outbound {}", outboundId);

        topMenu.whSelectorClick().openWarehouse();
        leftMenu.WMS().outgoing().outgoingOrder();

        outgoingOrderPage.inputOrderId(outboundId);
        outgoingOrderPage.filterButtonClick();
        outgoingOrderPage.openFirstOrder();

        topContextMenu.Actions().reserve();
        topContextMenu.Actions().start();
    }

    @Step("Отменить резервирование для последнего изъятия, если есть запущенные")
    public void cancelReservationforOutbound(
            ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Outbound outbound) {
        log.info("Canceling reservation for Outbound {}", outbound.getFulfillmentId());

        topMenu.whSelectorClick().openWarehouse();
        leftMenu.WMS().outgoing().outgoingOrder();

        outgoingOrderPage.inputOrderId(outbound.getFulfillmentId());
        outgoingOrderPage.filterButtonClick();

        if (outgoingOrderPage.getOrderStatus()
                .equals(OrderStatus.COMPLECTATION_STARTED)) {

            outgoingOrderPage.selectFirstResult();

            topContextMenu.Actions().cancelReserve();
            popupAlert.yesButtonClick();
        }
    }

}
