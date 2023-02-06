package ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.tsdscreens;

import io.qameta.allure.Step;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.AbstractTsdScreen;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.TsdEmulator;

import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.NSPRFCILVC;
import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.NSPRFRC01X;
import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.NSPRFSMGEN1;
import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.NSPRFSMX1;
import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.NSPRFVDMSG;

/**
 * Экран ТСД: ПриемкаПоПУО,запись данных
 */
public class InboundByPuoAndRecordDataTsdScreen extends AbstractTsdScreen {

    public InboundByPuoAndRecordDataTsdScreen(TsdEmulator tsdEmulator) {
        super(tsdEmulator);
    }

    @Step("Открыт экран ТСД: ПриемкаПоПУО,запись данных - Вводим кол-во и срок годности")
    public void enter(int count, String expirationDate) {
        TsdEmulator tsd = getTsdEmulator();

        tsd.exec(NSPRFCILVC, "1,$user,797082,$database,INFOR,CIL,VC,$host,$palletSku,$storerKey,1,");

        tsd.exec(NSPRFSMX1, "1,$user,626092,$database,INFOR,SM,X1,$host,$storerKey,$palletSku,1,qty,,,,inbound,STAGE,");

        String[] response = tsd.exec(NSPRFSMGEN1, "1,$user,953918,$database,INFOR,SMGE,N1,$host,$storerKey," +
                "$receiptKey,$palletSku,," + count + ",STAGE,$lpn,,,,," + expirationDate + ",,,1,ALL,,,,,,");
        String transactionKey = response[9];
        String lot = response[10];

        tsd.exec(NSPRFRC01X, "1,$user,532320,$database,INFOR,RC0,1X,$host,,$storerKey," + lot + ",$receiptKey," +
                "$palletSku,NOPO," + count + ",EA,STD,STAGE,$location,,N,,,,,," + expirationDate + ",,,1,ALL,,1,,," +
                "$printerId,0,0,,0,,,,," + transactionKey + ",0,,,30,30,1,");

        tsd.exec(NSPRFVDMSG, "1,$user,500818,$database,INFOR,VDM,SG,$host,$receiptKey,");

        tsd.setCurrentScreen(new InboundS2TsdScreen(tsd));
    }
}
