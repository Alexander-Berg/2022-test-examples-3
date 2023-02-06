package ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.tsdscreens;

import io.qameta.allure.Step;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.AbstractTsdScreen;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.TsdEmulator;

import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.NSPRFMETA01;

/**
 * Экран ТСД: Меню приемки
 */
public class MenuOfInboundTsdScreen extends AbstractTsdScreen {

    public MenuOfInboundTsdScreen(TsdEmulator tsdEmulator) {
        super(tsdEmulator);
    }

    @Step("Открыт экран ТСД: Меню приемки - Выбираем пункт 0.Приёмка по местам")
    public void select0InboundByPallet() {
        TsdEmulator tsd = getTsdEmulator();

        tsd.exec(NSPRFMETA01, "1,$user,464212,$database,INFOR,META,01,$host,RCC01PL,REC_OTGR,16x20,ru-RU,");

        tsd.setCurrentScreen(new InboundS1RCC01TsdScreen(tsd));
    }

    @Step("Открыт экран ТСД: Меню приемки - Выбираем пункт 1.Приёмка")
    public void select1InboundBySku() {
        TsdEmulator tsd = getTsdEmulator();
        tsd.setCurrentScreen(new InboundS1TsdScreen(tsd));
    }
}
