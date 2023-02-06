package ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.tsdscreens;

import io.qameta.allure.Step;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.AbstractTsdScreen;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.TsdEmulator;

import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.NSPRFRCC01;

/**
 * Экран ТСД: Приемка-S1 (Поэкземплярная)
 */
public class InboundS1TsdScreen extends AbstractTsdScreen {

    public InboundS1TsdScreen(TsdEmulator tsdEmulator) {
        super(tsdEmulator);
    }

    /**
     * @param receiptKey Тарное место (или ПУО, или номер поставки)
     */
    @Step("Открыт экран ТСД: Приемка-S1 - Вводим Тарное место")
    public void enter(String receiptKey) {
        TsdEmulator tsd = getTsdEmulator();

        tsd.setSessionValue("receiptKey", receiptKey);
        String[] response = tsd.exec(NSPRFRCC01, "1,$user,088251,$database,INFOR,RCC,01,$host,A,$receiptKey,");
        String storerKey = response[9];
        tsd.setSessionValue("storerKey", storerKey);

        boolean found = true;
        if (found) { // TODO check error
            tsd.setCurrentScreen(new InputLocationTsdScreen(tsd));
        }
    }
}
