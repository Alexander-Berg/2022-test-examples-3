package ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.tsdscreens;

import io.qameta.allure.Step;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.AbstractTsdScreen;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.TsdEmulator;

import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.NSPRFMETA01;

/**
 * Экран ТСД: Главное меню WMS
 */
public class MainMenuWmsTsdScreen extends AbstractTsdScreen {

    public MainMenuWmsTsdScreen(TsdEmulator tsdEmulator) {
        super(tsdEmulator);
    }

    @Step("Открыт экран ТСД: Главное меню WMS - Выбираем пункт 0.Приемка")
    public void select0Inbound() {
        TsdEmulator tsd = getTsdEmulator();

        tsd.exec(NSPRFMETA01, "1,$user,639790,$database,INFOR,META,01,$host,RC,DEFAULT,16x20,ru-RU,");

        tsd.setCurrentScreen(new SelectPrinterTsdScreen(tsd)); // нажимаем Приемка, но переходим на ввод ИД принтера
    }
}
