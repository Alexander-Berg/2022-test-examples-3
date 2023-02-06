package ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.tsdscreens.login;

import io.qameta.allure.Step;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.AbstractTsdScreen;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.TsdEmulator;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.tsdscreens.MainMenuWmsTsdScreen;

import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.NSPRFEQID01;

/**
 * Экран ТСД: Ввод ID оборудования
 */
public class InputIdOfDeviceTsdScreen extends AbstractTsdScreen {

    public InputIdOfDeviceTsdScreen(TsdEmulator tsdEmulator) {
        super(tsdEmulator);
    }

    @Step("Открыт экран ТСД: ВведИДобор-я - Вводим ИД оборудования")
    public void enter(String equipmentId) {
        TsdEmulator tsd = getTsdEmulator();

        tsd.setSessionValue("equipmentId", equipmentId);

        tsd.exec(NSPRFEQID01, "1,$user,611431,$database,INFOR,EQID,01,$host,$equipmentId,N,,");

        tsd.setCurrentScreen(new MainMenuWmsTsdScreen(tsd));
    }
}
