package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.workstation;

import java.util.concurrent.TimeUnit;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.Retrier;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wms.ingoing.AproveClosePuoPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wms.ingoing.PuoInboundDetailsPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wms.ingoing.PuoInboundPage;

import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.Resource;

@Resource.Classpath({"wms/infor.properties"})
public class Inbound extends AbstractWSSteps {
    private static final Logger log = LoggerFactory.getLogger(Inbound.class);

    private final PuoInboundPage puoInboundPage;
    private final AproveClosePuoPage aproveClosePuoPage;
    private final PuoInboundDetailsPage puoInboundDetailsPage;

    public Inbound(WebDriver drvr) {
        super(drvr);

        puoInboundPage = new PuoInboundPage(driver);
        aproveClosePuoPage = new AproveClosePuoPage(driver);
        puoInboundDetailsPage = new PuoInboundDetailsPage(driver);
    }

    @Step("Закрыть ПУО-приемку {inboundId}")
    public void closePuo(String inboundId) {
        log.info("Closing PUO {}", inboundId);

        topMenu.whSelectorClick().openWarehouse();
        leftMenu.WMS().ingoing().puoInbound();
        puoInboundPage.inputInboundId(inboundId);
        puoInboundPage.filterButtonClick();
        puoInboundPage.selectFirstResult();
        topContextMenu.Actions().closePuo();
        popupAlert.yesButtonClick();
    }

    @Step("Проверка входящих с закрытием: Закрыть ПУО {inboundId}")
    public void approveClosePuo(String inboundId) {
        log.info("Confirming PUO {} closing", inboundId);

        topMenu.whSelectorClick().openWarehouse();
        leftMenu.WMS().ingoing().approveClosePuo();
        aproveClosePuoPage.inputInboundId(inboundId);
        aproveClosePuoPage.filterButtonClick();
        aproveClosePuoPage.selectFirstResult();
        topContextMenu.Actions().aproveClosePuo();
        popupAlert.yesButtonClick();
    }

    @Step("Находим паллету ПУО-приёмки {inboundId}")
    public String findPalletOfInbound(String inboundId) {
        log.info("Finding pallet of inbound {}", inboundId);
        topMenu.whSelectorClick().openWarehouse();
        leftMenu.WMS().ingoing().puoInboundDetails();
        puoInboundDetailsPage.inputInboundId(inboundId);
        puoInboundDetailsPage.inputSku("PL");

        //Тесты иногда приходят на страницу раньше, чем товар PL успел создаться
        //Более честного костыля не придумал

        Retrier.retry(() -> {
            puoInboundDetailsPage.filterButtonClick();

            Assertions.assertTrue(
            puoInboundDetailsPage.firstResultIsPresent(),
                    "На странице должен появиться результат фильтрации");
        },
                Retrier.RETRIES_SMALL,
                1,
                TimeUnit.SECONDS
        );

        return puoInboundDetailsPage.selectPallet();
    }
}
