package ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.tsdscreens;

import io.qameta.allure.Step;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.AbstractTsdScreen;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.TsdEmulator;

import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.NSPRFASNGSI01;
import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.NSPRFBOM0;
import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.NSPRFMETA01;
import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.NSPRFRC10QTYENABLED;
import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.NSPRFRC10XXX;

/**
 * Экран ТСД: Приемка-S2
 */
public class InboundS2TsdScreen extends AbstractTsdScreen {

    public InboundS2TsdScreen(TsdEmulator tsdEmulator) {
        super(tsdEmulator);
    }

    /**
     * @param sku Товар (баркод)
     */
    @Step("Открыт экран ТСД: Приемка-S2 - Вводим Товар (баркод)")
    public void enter(String sku) {
        TsdEmulator tsd = getTsdEmulator();

        tsd.setSessionValue("sku", sku);

        // storerKey=4652394, не понятно, нужно не нужно
        tsd.exec(NSPRFBOM0, "1,$user,405901,$database,INFOR,BO,M0,$host,$storerKey,$sku,$receiptKey,");

        String[] response = tsd.exec(NSPRFASNGSI01, "1,$user,258434,$database,INFOR,ASNGSI,01,$host,,$sku," +
                "$receiptKey,1,RC00,");
        tsd.setSessionValue("palletSku", response[10]); // ROV000000000000000xxxx

        response = tsd.exec(NSPRFRC10XXX, "1,$user,817132,$database,INFOR,RC10X,XX,$host,$sku,$storerKey,");
        if (response[7].startsWith("08:01729:")) {
            // Новый товар! Требуется заполнить следующие параметры контроля срока годности в карточке товара:
            // Шаблон проверки атрибутов, Срок годности, Тип индикатора ShelfLife, Значение ShelfLife для
            // входящего и исходящего контролей.
            tsd.setCurrentScreen(new InputTemplateTsdScreen(tsd));
            return;
        }

        tsd.exec(NSPRFMETA01, "1,$user,858185,$database,INFOR,META,01,$host,RC10QTYENABLED,REC_OTGR,16x20,ru-RU,");
        tsd.exec(NSPRFRC10QTYENABLED, "1,$user,559606,$database,INFOR,RC10QTYENABL,ED,$host,");

        tsd.setCurrentScreen(new InboundByPuoAndRecordDataTsdScreen(tsd));
    }
}
