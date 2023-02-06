package ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.tsdscreens.login;

import io.qameta.allure.Step;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.AbstractTsdScreen;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.TsdEmulator;

import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.NSPRFLL1;
import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.NSPRFM1;
import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.NSPRFSND00;
import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.nspdrfallmsg;

/**
 * Экран ТСД: Выбор склада
 */
public class SelectWarehouseTsdScreen extends AbstractTsdScreen {

    public SelectWarehouseTsdScreen(TsdEmulator tsdEmulator) {
        super(tsdEmulator);
    }

    @Step("Открыт экран ТСД: Выбор склада - Выбираем склад")
    public void select(String database) {
        TsdEmulator tsd = getTsdEmulator();

        tsd.setSessionValue("database", database);

        tsd.exec(nspdrfallmsg, "1,$user,918534,$database,INFOR,nspdrfallm,sg,$host,$user,");
        tsd.exec(NSPRFLL1, "1,$user,133049,$database,INFOR,LL1,01,$host,,");
        tsd.exec(NSPRFSND00, "1,$user,778828,$database,INFOR,SND,00,$host,,");
        tsd.exec(NSPRFM1, "1,$user,437653,$database,INFOR,M1,01,$host,");

        tsd.setCurrentScreen(new InputIdOfDeviceTsdScreen(tsd));
    }
}
