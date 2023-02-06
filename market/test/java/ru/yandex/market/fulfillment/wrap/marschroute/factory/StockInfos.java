package ru.yandex.market.fulfillment.wrap.marschroute.factory;

import ru.yandex.market.fulfillment.wrap.marschroute.model.response.stock.StockInfo;

/**
 * Класс по созданию экземпляров класса StockInfo для тестов.
 */
public class StockInfos {

    public static StockInfo stockInfo(int available, int fit, int quarantine, int damaged, int expired) {
        StockInfo stockInfo = new StockInfo();
        stockInfo.setAvailable(available);
        stockInfo.setFit(fit);
        stockInfo.setQuarantine(quarantine);
        stockInfo.setDamaged(damaged);
        stockInfo.setExpired(expired);

        return stockInfo;
    }
}
