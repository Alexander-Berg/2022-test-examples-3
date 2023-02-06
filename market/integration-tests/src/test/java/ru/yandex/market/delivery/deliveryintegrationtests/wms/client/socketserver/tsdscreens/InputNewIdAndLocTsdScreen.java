package ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.tsdscreens;

import io.qameta.allure.Step;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.AbstractTsdScreen;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.TsdEmulator;

/**
 * Экран ТСД: Input new ID and LOC-
 */
public class InputNewIdAndLocTsdScreen extends AbstractTsdScreen {

    public InputNewIdAndLocTsdScreen(TsdEmulator tsdEmulator) {
        super(tsdEmulator);
    }

    /**
     * @param idNzn ИД(НЗН)
     */
    @Step("Открыт экран ТСД: Input new ID and LOC- - Вводим параметр ИД(НЗН)")
    public void enter(String idNzn) {
        TsdEmulator tsd = getTsdEmulator();

        tsd.setSessionValue("lpn", idNzn);

        tsd.setCurrentScreen(new InboundByPuoAndRecordDataTsdScreen(tsd));
    }
}
