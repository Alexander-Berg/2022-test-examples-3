package ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.tsdscreens;

import io.qameta.allure.Step;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.AbstractTsdScreen;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.TsdEmulator;

import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.NSPRFMETA01;

/**
 * Экран ТСД: Выбор принтера
 */
public class SelectPrinterTsdScreen extends AbstractTsdScreen {

    public SelectPrinterTsdScreen(TsdEmulator tsdEmulator) {
        super(tsdEmulator);
    }

    @Step("Открыт экран ТСД: Выбор принтера - Вводим ИД принтера")
    public void enter(String printerId) {
        TsdEmulator tsd = getTsdEmulator();

        tsd.setSessionValue("printerId", printerId);
        tsd.exec(NSPRFMETA01, "1,$user,247497,$database,INFOR,META,01,$host,RCM,REC_OTGR,16x20,ru-RU,");

        tsd.setCurrentScreen(new MenuOfInboundTsdScreen(tsd));
    }
}
