package ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.tsdscreens;

import io.qameta.allure.Step;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.AbstractTsdScreen;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.TsdEmulator;

import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.NSPRFASNGSI01;
import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.NSPRFRCSHLF02;

/**
 * Экран ТСД: Ввод сроков
 */
public class InputExpirationsTsdScreen extends AbstractTsdScreen {

    public InputExpirationsTsdScreen(TsdEmulator tsdEmulator) {
        super(tsdEmulator);
    }

    @Step("Открыт экран ТСД: Ввод сроков - Вводим параметр dayExp или monthExp")
    public void enter(String dayExp, String monthExp) {
        TsdEmulator tsd = getTsdEmulator();

        //todo check (moved from entertemplate
        tsd.exec(NSPRFRCSHLF02,
                "1,$user,293161,$database,INFOR,RCSHLF,02,$host,$storerKey,$sku,$template," + dayExp + "," + monthExp + ",,,");
        tsd.exec(NSPRFRCSHLF02,
                "1,$user,293161,$database,INFOR,RCSHLF,02,$host,$storerKey,$palletSku,$template," + dayExp + "," + monthExp + ",,,");
        tsd.exec(NSPRFASNGSI01, "1,$user,556427,$database,INFOR,ASNGSI,01,$host,$storerKey,$sku,$receiptKey,24," +
                "ASNGSI01M,");

        tsd.setCurrentScreen(new InputNewIdAndLocTsdScreen(tsd));
    }

}
