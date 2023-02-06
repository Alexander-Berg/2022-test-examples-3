package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.api.radiator;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.restassured.response.ValidatableResponse;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.RadiatorClient;


import static org.hamcrest.Matchers.matchesPattern;

@Resource.Classpath("wms/test.properties")
@DisplayName("API: Stocks and Expiration")
@Epic("API Tests")
public class StocksTest {
    private static final Logger log = LoggerFactory.getLogger(StocksTest.class);

    @Property("test.vendorId")
    private long vendorId;

    private final RadiatorClient radiatorClient = new RadiatorClient();

    @BeforeEach
    public void setUp() throws Exception {
        PropertyLoader.newInstance().populate(this);
    }

    /**
     * Текущие приоритеты:
     *
     * 1. Карантин (LOST) - 40
     * 2. Излишек - 70
     * 3. Брак - 50
     * 4. Просроченный - 30
     * 5. Годный - 10
     *
     * Если товар отвечает сразу нескольким условиям,
     * то его состояние будет соответствовать условию с наивысшим приоритетом
     */
    //TODO При включении тестов добавить проверку на Updated
    @Disabled("Отключено до починки в MARKETWMS-16561")
    @Test
    @DisplayName("getStocks")
    @Description("Тестовые данные: https://wiki.yandex-team.ru/users/ivalekseev/Avtotesty-WMS/getRefItems202002/")
    public void getStocksTest() {
        log.info("Testing getStocks");

        String article = "AUTO_GET_EXP_ITEMS_TEST3";

        ValidatableResponse response = radiatorClient.getStocks(
                vendorId,
                article
        );

        response
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '10'}.count", Matchers.is("2"))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '40'}.count", Matchers.is("2"))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '30'}.count", Matchers.is("2"))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '50'}.count", Matchers.is("2"))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '70'}.count", Matchers.is("2"));

        String respText = response.extract().asString();

        log.info(respText);

    }

    @Disabled("Отключено до починки в MARKETWMS-16561")
    @Test
    @DisplayName("getStocks: Товар на тележах приемки считается карантином")
    @Description("Принятый в ячейку STAGEn, но ещё не размещенный товар должен считаться карантином")
    public void getStocksQuarantineOnCartTest() {
        log.info("Testing getStocks: items on STAGEn carts should be quarantine");

        String article = "QARANTINE_ON_CART_TEST";

        ValidatableResponse response = radiatorClient.getStocks(
                vendorId,
                article
        );

        response
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '10'}.count", Matchers.is("0"))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '40'}.count", Matchers.is("2"))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '30'}.count", Matchers.is("0"))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '50'}.count", Matchers.is("0"))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '70'}.count", Matchers.is("0"));

        String respText = response.extract().asString();

        log.info(respText);

    }

    /**
     * Текущие приоритеты:
     *
     * 1. Карантин (LOST) - 40
     * 2. Излишек - 70
     * 3. Брак - 50
     * 4. Просроченный - 30
     * 5. Годный - 10
     *
     * Если товар отвечает сразу нескольким условиям,
     * то его состояние будет соответствовать условию с наивысшим приоритетом
     */
    @Test
    @DisplayName("getExpirationItems")
    @Description("Тестовые данные: https://wiki.yandex-team.ru/users/ivalekseev/Avtotesty-WMS/getRefItems202002/")
    public void getExpirationItemsTest() {
        log.info("Testing getReferenceItems");

        String article = "AUTO_GET_EXP_ITEMS_TEST3";

        ValidatableResponse response = radiatorClient.getExpirationItems(
                vendorId,
                article
        );

        response
                .body("root.response.itemExpirationList.itemExpiration.expirations.expiration.find " +
                                "{it.manufacturedDate == '2010-01-03T14:00:00+03:00'}.stocks.stock.find " +
                                "{it.type == '10'}.count",
                        Matchers.is("0"))
                .body("root.response.itemExpirationList.itemExpiration.expirations.expiration.find " +
                                "{it.manufacturedDate == '2010-01-03T14:00:00+03:00'}.stocks.stock.find " +
                                "{it.type == '40'}.count",
                        Matchers.is("1"))
                .body("root.response.itemExpirationList.itemExpiration.expirations.expiration.find " +
                                "{it.manufacturedDate == '2010-01-03T14:00:00+03:00'}.stocks.stock.find " +
                                "{it.type == '30'}.count",
                        Matchers.is("2"))
                .body("root.response.itemExpirationList.itemExpiration.expirations.expiration.find " +
                                "{it.manufacturedDate == '2010-01-03T14:00:00+03:00'}.stocks.stock.find " +
                                "{it.type == '50'}.count",
                        Matchers.is("1"))
                .body("root.response.itemExpirationList.itemExpiration.expirations.expiration.find " +
                                "{it.manufacturedDate == '2010-01-03T14:00:00+03:00'}.stocks.stock.find " +
                                "{it.type == '70'}.count",
                        Matchers.is("1"))
                .body("root.response.itemExpirationList.itemExpiration.expirations.expiration.find " +
                                "{it.manufacturedDate == '2110-12-15T14:00:00+03:00'}.stocks.stock.find " +
                                "{it.type == '10'}.count",
                        Matchers.is("2"))
                .body("root.response.itemExpirationList.itemExpiration.expirations.expiration.find " +
                                "{it.manufacturedDate == '2110-12-15T14:00:00+03:00'}.stocks.stock.find " +
                                "{it.type == '40'}.count",
                        Matchers.is("1"))
                .body("root.response.itemExpirationList.itemExpiration.expirations.expiration.find " +
                                "{it.manufacturedDate == '2110-12-15T14:00:00+03:00'}.stocks.stock.find " +
                                "{it.type == '30'}.count",
                        Matchers.is("0"))
                .body("root.response.itemExpirationList.itemExpiration.expirations.expiration.find " +
                                "{it.manufacturedDate == '2110-12-15T14:00:00+03:00'}.stocks.stock.find " +
                                "{it.type == '50'}.count",
                        Matchers.is("1"))
                .body("root.response.itemExpirationList.itemExpiration.expirations.expiration.find " +
                                "{it.manufacturedDate == '2110-12-15T14:00:00+03:00'}.stocks.stock.find " +
                                "{it.type == '70'}.count",
                        Matchers.is("1"));
    }

    @Test
    @DisplayName("getExpirationItems: со сроком годности и без")
    @Description("У товара есть айтемы как со сроком годности, так и без него. " +
            "Должны вернуться данные по тем товарам, у которых сроки годности заполнены.")
    public void getExpirationItemsWithAndWithoutLifetimeTest() {
        log.info("Testing getExpirationItemsWithAndWithoutLifetimeTest");

        String article = "AUTOLIFETIMECHANGED";

        ValidatableResponse response = radiatorClient.getExpirationItems(
                vendorId,
                article
        );

        response
                .body("root.response.itemExpirationList.itemExpiration.expirations.expiration.find " +
                                "{it.manufacturedDate == '2018-12-12T14:00:00+03:00'}.stocks.stock.find " +
                                "{it.type == '10'}.count",
                        Matchers.is("1"))
                .body("root.response.itemExpirationList.itemExpiration.expirations.expiration.find " +
                                "{it.manufacturedDate == '2018-12-12T14:00:00+03:00'}.stocks.stock.find " +
                                "{it.type == '40'}.count",
                        Matchers.is("0"))
                .body("root.response.itemExpirationList.itemExpiration.expirations.expiration.find " +
                                "{it.manufacturedDate == '2018-12-12T14:00:00+03:00'}.stocks.stock.find " +
                                "{it.type == '30'}.count",
                        Matchers.is("0"))
                .body("root.response.itemExpirationList.itemExpiration.expirations.expiration.find " +
                                "{it.manufacturedDate == '2018-12-12T14:00:00+03:00'}.stocks.stock.find " +
                                "{it.type == '50'}.count",
                        Matchers.is("0"))
                .body("root.response.itemExpirationList.itemExpiration.expirations.expiration.find " +
                                "{it.manufacturedDate == '2018-12-12T14:00:00+03:00'}.stocks.stock.find " +
                                "{it.type == '70'}.count",
                        Matchers.is("0"));
    }

    @Test
    @DisplayName("2-местный товар с одним заведенным BOM")
    @Description("У 2х местного товара принят и заведен только 1 BOM. " +
            "Все принятые единицы должны считаться дефектом.")
    public void justFirstBomCreatedAndAcceptedCountsAsDefectGetStocksTest() {

        String article = "2BOM1";
        String regexpUTC = "\\d{4}-[0-1]\\d-[0-3]\\dT[0-2]\\d:[0-5]\\d:[0-6]\\d.+\\d\\d:\\d\\d";

        ValidatableResponse response = radiatorClient.getStocks(
                vendorId,
                article
        );

        response
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '10'}.count",
                        Matchers.is("0"))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '10'}.updated",
                        matchesPattern(regexpUTC))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '40'}.count",
                        Matchers.is("0"))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '40'}.updated",
                        matchesPattern(regexpUTC))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '30'}.count",
                        Matchers.is("0"))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '30'}.updated",
                        matchesPattern(regexpUTC))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '50'}.count",
                        Matchers.is("2"))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '50'}.updated",
                        matchesPattern(regexpUTC))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '70'}.count",
                        Matchers.is("0"))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '70'}.updated",
                        matchesPattern(regexpUTC));
    }

    @Disabled("Отключено до починки в MARKETWMS-16561")
    @Test
    @DisplayName("2-местный товар, принят только один BOM")
    @Description("У 2х местного товара заведено 2 BOM, но принят один. " +
            "Все принятые единицы должны считаться дефектом.")
    public void twoBomsCreatedOnlyFirstAcceptedGetStocksTest() {

        String article = "2BOM2";

        ValidatableResponse response = radiatorClient.getStocks(
                vendorId,
                article
        );

        response
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '10'}.count",
                        Matchers.is("0"))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '40'}.count",
                        Matchers.is("2"))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '30'}.count",
                        Matchers.is("0"))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '50'}.count",
                        Matchers.is("0"))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '70'}.count",
                        Matchers.is("0"));
    }

    @Test
    @DisplayName("2-местный товар, одного из BOM недосдача")
    @Description("В поставке 2х местного товара один BOM недопоставлен.")
    public void twoBomsFirstInsufficientGetStocksTest() {

        String article = "2BOM3";
        String regexpUTC = "\\d{4}-[0-1]\\d-[0-3]\\dT[0-2]\\d:[0-5]\\d:[0-6]\\d.+\\d\\d:\\d\\d";

        ValidatableResponse response = radiatorClient.getStocks(
                vendorId,
                article
        );

        response
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '10'}.count",
                        Matchers.is("2"))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '10'}.updated",
                        matchesPattern(regexpUTC))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '40'}.count",
                        Matchers.is("0"))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '40'}.updated",
                        matchesPattern(regexpUTC))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '30'}.count",
                        Matchers.is("0"))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '30'}.updated",
                        matchesPattern(regexpUTC))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '50'}.count",
                        Matchers.is("3"))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '50'}.updated",
                        matchesPattern(regexpUTC))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '70'}.count",
                        Matchers.is("0"))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '70'}.updated",
                        matchesPattern(regexpUTC));
    }

    @Test
    @DisplayName("2-местный товар, один BOM дефект")
    @Description("В поставке 2х местного товара один БОМ дефектный")
    public void twoBomsOneDefectGetStocksTest() {

        String article = "2BOM4";
        String regexpUTC = "\\d{4}-[0-1]\\d-[0-3]\\dT[0-2]\\d:[0-5]\\d:[0-6]\\d.+\\d\\d:\\d\\d";

        ValidatableResponse response = radiatorClient.getStocks(
                vendorId,
                article
        );

        response
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '10'}.count",
                        Matchers.is("1"))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '10'}.updated",
                        matchesPattern(regexpUTC))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '40'}.count",
                        Matchers.is("0"))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '40'}.updated",
                        matchesPattern(regexpUTC))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '30'}.count",
                        Matchers.is("0"))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '30'}.updated",
                        matchesPattern(regexpUTC))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '50'}.count",
                        Matchers.is("1"))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '50'}.updated",
                        matchesPattern(regexpUTC))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '70'}.count",
                        Matchers.is("0"))
                .body("root.response.itemStocksList.itemStocks.stocks.stock.find {it.type == '70'}.updated",
                        matchesPattern(regexpUTC));
    }
}
