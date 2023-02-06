package ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver;

abstract public class AbstractTsdScreen {

    /**
     * Обратная ссылка на ТСД. ТСД первичен.
     */
    private TsdEmulator tsdEmulator;

    public AbstractTsdScreen(TsdEmulator tsdEmulator) {
        this.tsdEmulator = tsdEmulator;
    }

    public TsdEmulator getTsdEmulator() {
        return tsdEmulator;
    }
}
