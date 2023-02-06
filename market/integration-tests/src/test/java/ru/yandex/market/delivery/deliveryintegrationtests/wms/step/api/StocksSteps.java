package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api;

import io.qameta.allure.Step;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.Assertions;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.Retrier;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.RadiatorClient;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.ServiceBus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Stock;

import java.util.concurrent.TimeUnit;

/**
 * 1. Брак - 50
 * 2. Просроченный - 30
 * 3. Излишек - 70
 * 4. Карантин (LOST) - 40
 * 5. Годный - 10
 */
public class StocksSteps {

    private final RadiatorClient radiatorClient = new RadiatorClient();
    ServiceBus serviceBusClient = new ServiceBus();

    protected StocksSteps() {}

    public Stock getStocks(Item item) {
        return getStocks(item.getVendorId(), item.getArticle());
    }

    @Step("Получаем данные стоков")
    public Stock getStocks(long vendorId, String article) {
        ValidatableResponse responce = radiatorClient.getStocks(vendorId, article);

        int fit = responce.extract().xmlPath().getInt(
                "root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '10'}.count"
        );

        int expired = responce.extract().xmlPath().getInt(
                "root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '30'}.count"
        );

        int lost = responce.extract().xmlPath().getInt(
                "root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '40'}.count"
        );

        int damage = responce.extract().xmlPath().getInt(
                "root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '50'}.count"
        );

        int surplus = responce.extract().xmlPath().getInt(
                "root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '70'}.count"
        );

        return new Stock(fit, expired, lost, damage, surplus);
    }

    @Step("Получаем обновленные стоки")
    public Stock getUpdatedStocks(long vendorId, String article, Stock stockBefore){

        Stock stocksAfter = Retrier.retry(() -> {
                    radiatorClient.dropCache();
                    Stock stockAfter =  getStocks(vendorId, article);
                    Assertions.assertNotEquals(stockBefore, stockAfter, "Старые и новые стоки совпадают");
                    return stockAfter;
        }               ,
                Retrier.RETRIES_SMALL,
                1,
                TimeUnit.MINUTES
        );
       return stocksAfter;
    }

    @Step("Проверяем, что излишек переместился на годный сток")
    public void verifyItemTransferedFromSurplusToFit(Stock stockBefore, Stock stockAfter) {
        Assertions.assertEquals(stockBefore.getFit() + 1, stockAfter.getFit(), "Годный сток должен увеличиться на 1");
        Assertions.assertEquals(stockBefore.getDamage(), stockAfter.getDamage(), "Брак не должен измениться");
        Assertions.assertEquals(stockBefore.getExpired(), stockAfter.getExpired(), "Просроченный сток не должен измениться");
        Assertions.assertEquals(stockBefore.getLost(), stockAfter.getLost(), "Потерянный сток не должен измениться");
        Assertions.assertEquals(stockBefore.getSurplus() - 1, stockAfter.getSurplus(), "Излишек должен уменьшиться на 1");
    }

    @Step("Проверяем, что годный сток переместился обратно на излишек")
    public void verifyItemTransferedFromFitToSurplus(Stock stockBefore, Stock stockAfter) {
        Assertions.assertEquals(stockBefore.getFit() - 1, stockAfter.getFit(), "Годный сток должен уменьшиться на 1");
        Assertions.assertEquals(stockBefore.getDamage(), stockAfter.getDamage(), "Брак не должен измениться");
        Assertions.assertEquals(stockBefore.getExpired(), stockAfter.getExpired(), "Просроченный сток не должен измениться");
        Assertions.assertEquals(stockBefore.getLost(), stockAfter.getLost(), "Потерянный сток не должен измениться");
        Assertions.assertEquals(stockBefore.getSurplus() + 1, stockAfter.getSurplus(), "Излишек должен увеличиться на 1");
    }
    @Step("Проверяем, что товар действительно переместился на сток брака")
    public void verifyItemDamageCountIncreasedBy(Stock stockBefore, Stock stockAfter, int number) {
        Assertions.assertEquals(stockBefore.getDamage() + number, stockAfter.getDamage(), "Брак должен увеличиться");
        Assertions.assertEquals(stockBefore.getSurplus(), stockAfter.getSurplus(), "Излишек не должен измениться");
        Assertions.assertEquals(stockBefore.getExpired(), stockAfter.getExpired(), "Просроченный сток не должен измениться");
        Assertions.assertEquals(stockBefore.getLost(), stockAfter.getLost() + number, "Карантин должен уменьшиться");
        Assertions.assertEquals(stockBefore.getFit(), stockAfter.getFit(), "Годный сток не должен измениться");
    }

    @Step("Узнаём наш внутренний SKU для SKU поставщика")
    public String getRovByStorerKeyAndManufaturerSku(long storerKey, String manufacrturerSku) {
        return serviceBusClient.mapArticleToNativeSku(storerKey, manufacrturerSku);
    }
}
