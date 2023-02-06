package ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.tsdscreens;

import io.qameta.allure.Step;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.AbstractTsdScreen;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.TsdEmulator;

import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.NSPRFASNGSI01;
import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.NSPRFPGL01C;
import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.NSPRFPGL02C;
import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.NSPRFRC01PL;
import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.NSPRFRCC01PL;
import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.NSPRFRCC06PL;
import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.NSPRFVDMSG;

/**
 * Экран ТСД: Приемка-S1 RCC01 (Потарная)
 */
public class InboundS1RCC01TsdScreen extends AbstractTsdScreen {

    public InboundS1RCC01TsdScreen(TsdEmulator tsdEmulator) {
        super(tsdEmulator);
    }

    /**
     * @param receiptKey  Номер ПУО
     * @param palletCount Общие этикетки (кол-во паллет)
     */
    @Step("Открыт экран ТСД: Приемка-S1 RCC01 - Вводим Номер ПУО и Общие этикетки (кол-во паллет)")
    public void enter(String receiptKey, int palletCount) {
        TsdEmulator tsd = getTsdEmulator();

        tsd.setSessionValue("receiptKey", receiptKey);
        tsd.setSessionValue("palletCount", palletCount);

        tsd.exec(NSPRFRCC01PL, "1,$user,908268,$database,INFOR,RCC01,PL,$host,A,$receiptKey,,$palletCount,");
        tsd.exec(NSPRFPGL01C, "1,$user,925022,$database,INFOR,PGL0,1C,$host,$receiptKey,$printerId,");

        String[] response = tsd.exec(NSPRFPGL02C, "1,$user,727840,$database,INFOR,PGL0,2C,$host,$receiptKey," +
                "$printerId,$palletCount,");
        String lpnLabels = response[9];
        tsd.setSessionValue("lpnLabels", lpnLabels);
        tsd.setSessionValue("lpn", lpnLabels.split("%")[0]); // эксперимент

        tsd.exec(NSPRFRCC06PL, "1,$user,732971,$database,INFOR,RCC06,PL,$host,A,$receiptKey,$receiptKey,," +
                "STAGE,$lpnLabels,");

        tsd.exec(NSPRFASNGSI01, "1,$user,282528,$database,INFOR,ASNGSI,01,$host,,PL,$receiptKey,,RC00PL,");


        tsd.exec(NSPRFRC01PL, "1,$user,080185,$database,INFOR,RC01,PL,$host,,PL,,$receiptKey,PL,NOPO,1,EA,STD,STAGE,," +
                ",N,,,,,,,,,1,ALL,,1,,,$printerId,0,0,,0,,,,,,,,,,,,$lpnLabels,");

        tsd.exec(NSPRFVDMSG, "1,$user,431410,$database,INFOR,VDM,SG,$host,$receiptKey,");
    }

    public void backToMenuOfInboundTsdScreen() {
        TsdEmulator tsd = getTsdEmulator();
        tsd.setCurrentScreen(new MenuOfInboundTsdScreen(tsd));
    }
}
