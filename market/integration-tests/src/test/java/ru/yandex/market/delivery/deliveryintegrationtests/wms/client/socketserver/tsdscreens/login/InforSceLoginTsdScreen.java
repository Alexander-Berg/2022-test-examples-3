package ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.tsdscreens.login;

import io.qameta.allure.Step;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.AbstractTsdScreen;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.TsdEmulator;

import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.NSPRFCG01;
import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.NSPRFMETAOT08;

/**
 * Экран ТСД: Ввод имени пользователя и пароля
 */
public class InforSceLoginTsdScreen extends AbstractTsdScreen {

    public InforSceLoginTsdScreen(TsdEmulator tsdEmulator) {
        super(tsdEmulator);
    }

    @Step("Открыт экран ТСД: Ввод имени пользователя и пароля - Вводим параметры User и Password")
    public void enter(String userName, String password) {
        TsdEmulator tsd = getTsdEmulator();

        tsd.setSessionValue("user", userName);

        String[] response = tsd.exec(NSPRFMETAOT08, "1,$user,365617,,INFOR,OT,08,$host,en," + password +
                ",WEBRF,INFOR,");
        String token = response[9];
        tsd.setSessionValue("token", token);

        tsd.exec(NSPRFCG01, "1,$user,220537,,INFOR,OT,08,$host,");

        tsd.setCurrentScreen(new SelectWarehouseTsdScreen(tsd));
    }
}
