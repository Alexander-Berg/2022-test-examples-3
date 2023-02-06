package ru.yandex.market.pricelabs.tms.services.market_report.model.models;

import java.util.List;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import ru.yandex.market.pricelabs.misc.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class ModelsTest {

    @Test
    void testParseXmlLargeSample() {
        var models = readModels("tms/services/market_report/model/models/models-empty-sample.xml");
        assertNotNull(models);
        log.info("Models: {}", models);

        Long2ObjectMap<Model> modelMap = new Long2ObjectOpenHashMap<>();
        for (Model model : models.getModels()) {
            modelMap.put(model.getModelId(), model);
        }

        assertTrue(modelMap.containsKey(1002926));
    }

    @Test
    void testParseXmlLarge() {
        var models = readModels("tms/services/market_report/model/models/models-iphone.xml");
        assertNotNull(models);
        log.info("Models: {}", models);
    }

    @Test
    void testParseXmlSmall() {
        var models = readModels("tms/services/market_report/model/models/models-iphone-2items.xml");
        assertNotNull(models);
        log.info("Models: {}", models);

        var expectModels = new Models();
        expectModels.setCurrency("RUR");
        expectModels.setIndexGeneration("20191022_1109");

        var model = new Model();
        model.setModelId(558168089);
        model.setMin(56280);
        model.setMax(68000);
        model.setAvg(61990);
        model.setName("Смартфон Apple iPhone 11 128GB");

        var offers = new Offers();
        offers.setCount(279);

        var offer1 = new Offer();
        offer1.setDiscountPercent(14);
        offer1.setOldPrice(65750);
        offer1.setOnstock(1);
        offer1.setPrice(56280);

        var shop1 = new Shop();
        shop1.setName("WISHMASTER");
        shop1.setRegion(213);
        shop1.setRating(5);
        offer1.setShop(shop1);

        var delivery1 = new Delivery();
        delivery1.setPrice(390);
        delivery1.setRegion(213);
        offer1.setDelivery(delivery1);

        var offer2 = new Offer();
        offer2.setOnstock(1);
        offer2.setPrice(56290);

        var shop2 = new Shop();
        shop2.setName("Mobi-Lera.ru");
        shop2.setRegion(213);
        shop2.setRating(5);
        offer2.setShop(shop2);

        var delivery2 = new Delivery();
        delivery2.setPrice(350);
        delivery2.setRegion(213);
        offer2.setDelivery(delivery2);

        offers.setOffers(List.of(offer1, offer2));
        model.setOffers(offers);

        expectModels.setModels(List.of(model));

        assertEquals(expectModels, models);
    }

    @Test
    void testParseXmlWithShopId() {
        var models = readModels("tms/services/market_report/model/models/models-with-shop.xml");
        assertNotNull(models);
        log.info("Models: {}", models);

        var expectModels = new Models();
        expectModels.setCurrency("RUR");
        expectModels.setIndexGeneration("20191113_1154");

        var model = new Model();
        model.setModelId(216557650);
        model.setMin(4790);
        model.setMax(4790);
        model.setAvg(4790);
        model.setName("Бра MAYTONI Indiana MOD544WL-01W");

        var offers = new Offers();
        offers.setCount(5);

        var offer1 = new Offer();
        offer1.setOnstock(0);
        offer1.setPrice(4790);
        offer1.setFeedId(621678);
        offer1.setOfferId("406924");

        var shop1 = new Shop();
        shop1.setName("АлоСвет");
        shop1.setRegion(213);
        shop1.setRating(3);
        shop1.setShopId(580843);
        offer1.setShop(shop1);

        var delivery1 = new Delivery();
        delivery1.setPrice(350);
        delivery1.setRegion(213);
        offer1.setDelivery(delivery1);

        var offer2 = new Offer();
        offer2.setOnstock(0);
        offer2.setPrice(4790);
        offer2.setFeedId(650482);
        offer2.setOfferId("59862");

        var shop2 = new Shop();
        shop2.setName("Sveto-Store");
        shop2.setRegion(213);
        shop2.setRating(3);
        shop2.setShopId(586400);
        offer2.setShop(shop2);

        var delivery2 = new Delivery();
        delivery2.setPrice(590);
        delivery2.setRegion(213);
        offer2.setDelivery(delivery2);

        offers.setOffers(List.of(offer1, offer2));
        model.setOffers(offers);

        expectModels.setModels(List.of(model));

        assertEquals(expectModels, models);
    }

    @Test
    void testParseXmlError() {
        var models = readModels("tms/services/market_report/model/models/models-error.xml");
        assertNotNull(models);
        log.info("Models: {}", models);

        var expectModels = new Models();
        expectModels.setErrorCode(3639);
        expectModels.setErrorText("market/report/library/cgi/params.cpp:960: Failed to parse hyperid and vclusterid " +
                "params: util/string/cast.cpp:338: Unexpected symbol \" \" at pos 0 in string \" 71336695\". ");
        assertEquals(expectModels, models);
    }

    static Models readModels(String resource) {
        return Utils.fromXmlResource(resource, Models.class);
    }
}
