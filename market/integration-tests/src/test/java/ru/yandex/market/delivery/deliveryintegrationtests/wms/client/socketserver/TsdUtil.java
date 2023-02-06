package ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.tsdscreens.InboundByPuoAndRecordDataTsdScreen;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.tsdscreens.InboundS1RCC01TsdScreen;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.tsdscreens.InboundS1TsdScreen;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.tsdscreens.InboundS2TsdScreen;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.tsdscreens.InputExpirationsTsdScreen;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.tsdscreens.InputLocationTsdScreen;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.tsdscreens.InputNewIdAndLocTsdScreen;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.tsdscreens.InputTemplateTsdScreen;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.tsdscreens.MainMenuWmsTsdScreen;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.tsdscreens.MenuOfInboundTsdScreen;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.tsdscreens.SelectPrinterTsdScreen;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.tsdscreens.login.InforSceLoginTsdScreen;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.tsdscreens.login.InputIdOfDeviceTsdScreen;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.tsdscreens.login.SelectWarehouseTsdScreen;

/**
 * Методы получения экрана нужного типа из ТСД. Если извлечь не тот экран, который ожидается, то будет
 * ClassCastException.
 */
public class TsdUtil {

    public static InboundByPuoAndRecordDataTsdScreen getInboundByPuoAndRecordDataTsdScreen(TsdEmulator tsd) {
        return (InboundByPuoAndRecordDataTsdScreen) tsd.getCurrentScreen();
    }

    public static InboundS1RCC01TsdScreen getInboundS1RCC01TsdScreen(TsdEmulator tsd) {
        return (InboundS1RCC01TsdScreen) tsd.getCurrentScreen();
    }

    public static InboundS1TsdScreen getInboundS1TsdScreen(TsdEmulator tsd) {
        return (InboundS1TsdScreen) tsd.getCurrentScreen();
    }

    public static InboundS2TsdScreen getInboundS2TsdScreen(TsdEmulator tsd) {
        return (InboundS2TsdScreen) tsd.getCurrentScreen();
    }

    public static InforSceLoginTsdScreen getInforSceLoginTsdScreen(TsdEmulator tsd) {
        return (InforSceLoginTsdScreen) tsd.getCurrentScreen();
    }

    public static InputExpirationsTsdScreen getInputExpirationsTsdScreen(TsdEmulator tsd) {
        return (InputExpirationsTsdScreen) tsd.getCurrentScreen();
    }

    public static InputIdOfDeviceTsdScreen getInputIdOfDeviceTsdScreen(TsdEmulator tsd) {
        return (InputIdOfDeviceTsdScreen) tsd.getCurrentScreen();
    }

    public static InputLocationTsdScreen getInputLocationTsdScreen(TsdEmulator tsd) {
        return (InputLocationTsdScreen) tsd.getCurrentScreen();
    }

    public static InputNewIdAndLocTsdScreen getInputNewIdAndLocTsdScreen(TsdEmulator tsd) {
        return (InputNewIdAndLocTsdScreen) tsd.getCurrentScreen();
    }

    public static InputTemplateTsdScreen getInputTemplateTsdScreen(TsdEmulator tsd) {
        return (InputTemplateTsdScreen) tsd.getCurrentScreen();
    }

    public static MainMenuWmsTsdScreen getMainMenuWmsTsdScreen(TsdEmulator tsd) {
        return (MainMenuWmsTsdScreen) tsd.getCurrentScreen();
    }

    public static MenuOfInboundTsdScreen getMenuOfInboundTsdScreen(TsdEmulator tsd) {
        return (MenuOfInboundTsdScreen) tsd.getCurrentScreen();
    }

    public static SelectPrinterTsdScreen getSelectPrinterTsdScreen(TsdEmulator tsd) {
        return (SelectPrinterTsdScreen) tsd.getCurrentScreen();
    }

    public static SelectWarehouseTsdScreen getSelectWarehouseTsdScreen(TsdEmulator tsd) {
        return (SelectWarehouseTsdScreen) tsd.getCurrentScreen();
    }
}
