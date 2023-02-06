package ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.tsdscreens;

import io.qameta.allure.Step;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.AbstractTsdScreen;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.TsdEmulator;

import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.NSPRFMETA01;

/**
 * Экран ТСД: Ввод тары
 */
public class InputLocationTsdScreen extends AbstractTsdScreen {

    public InputLocationTsdScreen(TsdEmulator tsdEmulator) {
        super(tsdEmulator);
    }

    @Step("Открыт экран ТСД: Ввод тары - Вводим тару")
    public void enter(String location) {
        TsdEmulator tsd = getTsdEmulator();

        tsd.setSessionValue("location", location); // TODO
        tsd.exec(NSPRFMETA01, "1,$user,747834,$database,INFOR,META,01,$host,RC00,REC_OTGR,16x20,ru-RU,");

        tsd.setCurrentScreen(new InboundS2TsdScreen(tsd));
    }

}
