package ru.yandex.market.pricelabs.tms.processing.offers;


import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.impl.ytree.serialization.YTreeDeepCopier;
import ru.yandex.market.pricelabs.CoreTestUtils;
import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.pricelabs.model.NewOfferGen;
import ru.yandex.market.pricelabs.model.Offer;
import ru.yandex.market.pricelabs.model.ShopCategory;
import ru.yandex.market.pricelabs.model.ShopParameters.Marginality;
import ru.yandex.market.pricelabs.model.types.OfferType;
import ru.yandex.market.pricelabs.model.types.Status;
import ru.yandex.market.pricelabs.tms.processing.TaskInfo;
import ru.yandex.market.pricelabs.tms.processing.TmsTestUtils;
import ru.yandex.market.pricelabs.tms.processing.offers.OffersHolderGen.DeliveryOptions;
import ru.yandex.market.pricelabs.tms.processing.offers.OffersHolderGen.DeliveryParams;
import ru.yandex.market.pricelabs.tms.processing.offers.OffersHolderGen.PricelabsParams;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.pricelabs.apis.ApiConst.VIRTUAL_FEED_BLUE;
import static ru.yandex.market.pricelabs.apis.ApiConst.VIRTUAL_SHOP_BLUE;
import static ru.yandex.market.pricelabs.misc.TimingUtils.getInstant;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.shop;
import static ru.yandex.market.pricelabs.tms.processing.offers.AbstractOffersHolder.FLAG_AVAILABLE;
import static ru.yandex.market.pricelabs.tms.processing.offers.AbstractOffersHolder.FLAG_CUT_PRICE;

class OffersHoldersTest {

    private ProcessingContext context;
    private OffersHolderGen holderGen;
    private Marginality marginality;

    @BeforeEach
    void init() {
        this.prepareCtx(1, 2);
    }

    @Test
    void onEmptyOffer() {
        init(Utils.emptyConsumer());

        Offer offer = createOffer();
        check(offer);
    }

    @Test
    void onSimpleVsApi() {
        init(newOfferGen -> {
            newOfferGen.setOldprice(amountGen(1.1));
            newOfferGen.setPrice(amountGen(1.2));
        });

        Offer offer = createOffer();
        offer.setOldprice(110);
        offer.setPrice(120);
        offer.setCurrency_id("RUR");
        check(offer);
    }

    @Test
    void onSimpleVsApi2() {
        init(newOfferGen -> {
            newOfferGen.setOldprice(amountGen(1.1));
            newOfferGen.setApi_oldprice(amountGen(1.11));
            newOfferGen.setPrice(amountGen(1.2));
            newOfferGen.setApi_price(amountGen(1.21));
        });

        Offer offer = createOffer();
        offer.setOldprice(111);
        offer.setPrice(121);
        offer.setCurrency_id("RUR");
        check(offer);
    }

    @Test
    void onModelId() {
        init(newOfferGen -> {
            newOfferGen.setModel_id(900); // Не передается, т.к. не установлено published_on_market
        });

        Offer offer = createOffer();
        check(offer);
    }

    @Test
    void onModelIdAccept() {
        init(newOfferGen -> {
            newOfferGen.setModel_id(900);
            newOfferGen.setModel_published_on_market(true);
        });

        Offer offer = createOffer();
        offer.setModel_id(900);
        check(offer);
    }

    @Test
    void onClusterId() {
        init(newOfferGen -> {
            newOfferGen.setCluster_id(900); // Не передается, т.к. не установлено
        });

        Offer offer = createOffer();
        check(offer);
    }

    @Test
    void onClusterIdAccept() {
        init(newOfferGen -> {
            newOfferGen.setCluster_id(900);
            newOfferGen.setModel_published_on_market(true);
        });

        Offer offer = createOffer();
        offer.setModel_id(900);
        check(offer);
    }

    @Test
    void onClusterIdAcceptNotOverwrite() {
        init(newOfferGen -> {
            newOfferGen.setModel_id(900);
            newOfferGen.setCluster_id(901);
            newOfferGen.setModel_published_on_market(true);
        });

        Offer offer = createOffer();
        offer.setModel_id(900);
        check(offer);
    }

    @Test
    void onInStockCount() {
        init(newOfferGen -> {
            setFlag(newOfferGen, FLAG_AVAILABLE);
            newOfferGen.setIn_stock_count(2);
        });

        Offer offer = createOffer();
        offer.setIn_stock_count(2);
        check(offer);
    }

    @Test
    void onPurchasePrice() {
        init(newOfferGen -> {
            newOfferGen.setPurchase_price(4.9);
            newOfferGen.setPrice(amountGen(120));
        });

        Offer offer = createOffer();
        offer.setPurchase_price(490);
        offer.setPrice(12000);
        offer.setCurrency_id("RUR");
        check(offer);
    }

    @Test
    void onPurchasePriceOverwrite() {
        init(newOfferGen -> {
            newOfferGen.setPurchase_price(4.8);
            newOfferGen.setPrice(amountGen(120));
        });

        Offer offer = createOffer();
        offer.setPurchase_price(480);
        offer.setPrice(12000);
        offer.setCurrency_id("RUR");
        check(offer);
    }

    @Test
    void onMarketCategory() {

        init(newOfferGen -> {
            newOfferGen.setShop_category_id("");
            newOfferGen.setCategory_id(4);
        });

        Offer offer = createOffer();
        offer.setCategory_id(ShopCategory.NO_CATEGORY);
        offer.setMarket_category_id(4);
        check(offer);
    }

    @Test
    void onMarketCategoryForBlue() {
        prepareCtx(VIRTUAL_SHOP_BLUE, VIRTUAL_FEED_BLUE, true);

        init(newOfferGen -> {
            newOfferGen.setShop_category_id("");
            newOfferGen.setCategory_id(4);
        });

        Offer offer = createOffer();
        offer.setCategory_id(4);
        offer.setMarket_category_id(4);
        check(offer);
    }

    @Test
    void onShippingCost() {
        init(newOfferGen -> {
            var params = new DeliveryParams(List.of(
                    new DeliveryOptions(amountPl(1.3)),
                    new DeliveryOptions(amountPl(1.4)),
                    new DeliveryOptions(amountPl(1.2)),
                    new DeliveryOptions(amountPl(1.8))
            ));
            newOfferGen.setDelivery_options(Utils.toJsonString(params));
        });

        Offer offer = createOffer();
        offer.setShipping_cost(120); // Минимальная
        check(offer);
    }

    @Test
    void onShippingCostNew() {
        init(newOfferGen -> {
            var params = new DeliveryParams(List.of(
                    new DeliveryOptions(amountPl(1.3)),
                    new DeliveryOptions(amountPl(1.4)),
                    new DeliveryOptions(amountPl(1.2)),
                    new DeliveryOptions(amountPl(1.8))
            ));
            newOfferGen.setDelivery_options(Utils.toJsonString(params));
        });

        Offer offer = createOffer();
        offer.setShipping_cost(120); // Минимальная
        check(offer);
    }

    @Test
    void onVendorCodeDescription() {
        init(newOfferGen -> newOfferGen.setVendor_code("code1"));

        Offer offer = createOffer();
        offer.setVendor_code("code1");
        check(offer);
    }

    @Test
    void onVendorNameForBookNonBook() {
        init(newOfferGen -> {
            newOfferGen.setType(OfferType.VENDOR_MODEL.value());
            newOfferGen.setPublisher("unknown");
        });

        Offer offer = createOffer();
        offer.setType(OfferType.VENDOR_MODEL);
        check(offer);
    }

    @Test
    void onVendorNameForBookBook() {
        init(newOfferGen -> {
            newOfferGen.setType(OfferType.BOOK.value());
            newOfferGen.setPublisher("unknown");
        });

        Offer offer = createOffer();
        offer.setVendor_name("unknown");
        offer.setType(OfferType.BOOK);
        check(offer);
    }

    @Test
    void onVendorNameForBookOverwrite() {
        init(newOfferGen -> {
            newOfferGen.setType(OfferType.BOOK.value());
            newOfferGen.setVendor("known");
            newOfferGen.setPublisher("unknown");
        });

        Offer offer = createOffer();
        offer.setVendor_name("known");
        offer.setType(OfferType.BOOK);
        check(offer);
    }

    @Test
    void onLastModelIdChange() {
        var newOfferGen = createNewOfferGen();
        newOfferGen.setModel_id(1);
        newOfferGen.setModel_published_on_market(true);
        holderGen.onNewOffer(newOfferGen);

        Offer offer = createOffer();
        offer.setModel_id(1);
        check(offer);

        prepareCtx(1, 2);

        newOfferGen.setModel_id(2);
        holderGen.onNewOffer(newOfferGen);
        holderGen.onExistingOffer(YTreeDeepCopier.deepCopyOf(offer));

        offer.setModel_id(2);
        offer.setLast_model_id(1);

        check(offer);


        prepareCtx(1, 2);

        newOfferGen.setModel_id(0);
        holderGen.onNewOffer(newOfferGen);
        holderGen.onExistingOffer(YTreeDeepCopier.deepCopyOf(offer));

        offer.setModel_id(0);
        offer.setLast_model_id(2);

        check(offer);

        prepareCtx(1, 2);

        holderGen.onNewOffer(newOfferGen);
        holderGen.onExistingOffer(YTreeDeepCopier.deepCopyOf(offer));

        check(offer);
    }

    @Test
    void onPartiallyFilledOffer() {
        init(newOfferGen -> {
            newOfferGen.setShop_id_int(100);
            newOfferGen.setFeed_id_int(200);
            newOfferGen.setOffer_id("3");
            // Сейчас эти значения не валидируются?
            newOfferGen.setCategory_id(400);
            newOfferGen.setShop_category_id("401");
            newOfferGen.setOldprice(amountGen(4.1));
            // По умолчанию берется из фида
            newOfferGen.setPurchase_price(5.1);
            newOfferGen.setModel_published_on_market(true);
            newOfferGen.setModel_id(500);
            newOfferGen.setPrice(amountGen(6.1));
            newOfferGen.setType(OfferType.VENDOR_MODEL.value());
            newOfferGen.setTitle("title");
            setFlag(newOfferGen, FLAG_AVAILABLE);
            newOfferGen.setWare_md5("md5"); // пе проверяется
            setFlag(newOfferGen, FLAG_CUT_PRICE);
            newOfferGen.setVendor("vendor");
        });


        Offer offer = createOffer();
        offer.setOffer_id("3");
        offer.setVendor_name("vendor");
        offer.setModel_id(500);
        offer.setType(OfferType.VENDOR_MODEL);
        offer.setCategory_id(401);
        offer.setMarket_category_id(400);
        offer.setCurrency_id("RUR");
        offer.setPrice(610);
        offer.setOldprice(410);
        offer.setPurchase_price(510);
        offer.setIn_stock_count(3);
        offer.setName("title");
        offer.setWare_md5("md5");
        offer.set_cutprice(true);
        offer.normalizeFields();
        check(offer);
    }

    @Test
    void onPricelabsParams() {
        init(newOfferGen -> {
            var params = List.of(new PricelabsParams("k1", "1"),
                    new PricelabsParams("k2", "2"),
                    new PricelabsParams("k3", "k3"),
                    new PricelabsParams("k4", "4"));
            newOfferGen.setPricelabs_params(Utils.toJsonString(params));
        });

        Offer offer = createOffer();
        Map<String, Integer> params = new Object2ObjectOpenHashMap<>();
        params.put("k1", 1);
        params.put("k2", 2);
        params.put("k4", 4);
        offer.setParams_map(params);
        check(offer);
    }

    @Test
    void testParseParams() {
        Object2IntMap<String> map = new Object2IntOpenHashMap<>();
        map.put("Marginality", 4);
        Assertions.assertEquals(map, Offer.parseParams("[Marginality]=[4]"));
    }

    @Test
    void onMarketSku() {
        init(newOfferGen -> {
            newOfferGen.setVendor_id(1);
            newOfferGen.setMarket_sku(111222333444555L);
            newOfferGen.setSupplier_id_int(1003);
            newOfferGen.setFeed_id_int(1004);
            newOfferGen.setShop_sku("abc123");
        });

        Offer offer = createOffer();
        offer.setVendor_id(1);
        offer.setMarket_sku(111222333444555L);
        offer.setShop_sku("abc123");
        check(offer);
    }

    @Test
    void onSupplierIdDefaultShop() {
        prepareCtx(1001, 1002);

        init(newOfferGen -> {
            newOfferGen.setShop_id_int(1001);
            newOfferGen.setFeed_id_int(1002);
            newOfferGen.setOffer_id("abc123");
            newOfferGen.setSupplier_id_int(1003);
            newOfferGen.setShop_sku("abc123");
        });

        Offer offer = createOffer();
        offer.setShop_id(1001);
        offer.setFeed_id(1002);
        offer.setOffer_id("abc123");
        offer.setShop_sku("abc123");
        check(offer);
    }

    @Test
    void onIncorrectPrice() {
        init(newOfferGen -> {
            newOfferGen.setPrice("RUR 18446744073709551615"); // Ошибочная цена
        });

        Offer offer = createOffer();
        offer.setCurrency_id("RUR");
        check(offer);
    }

    private NewOfferGen createNewOfferGen() {
        return new NewOfferGen();
    }

    private Offer createOffer() {
        Offer expect = TmsTestUtils.offer("");
        expect.setOffer_id(null);
        expect.setShop_id(context.getShopId());
        expect.setFeed_id((int) context.getFeedId());
        expect.setVendor_name("");
        expect.setType(OfferType.UNSUPPORTED);
        expect.setName("");
        expect.setCreated_at(getInstant());
        expect.setMin_price_offer_id("");
        expect.setCurrency_id("");
        expect.setVendor_code("");
        expect.setVendor_name("");
        expect.setWare_md5("");
        expect.setStatus(Status.ACTIVE);
        expect.setParams_map(Map.of());
        return expect;
    }

    private long amountPl(double value) {
        return (long) (value * AbstractOffersHolder.YT_PRICE_DIVIDER * 100.0);
    }

    private String amountGen(double value) {
        return "RUR " + amountPl(value);
    }

    static void setFlag(NewOfferGen newOffer, int position) {
        var flags = newOffer.getFlags();
        flags = flags | (1 << position);
        newOffer.setFlags(flags);
    }

    private String xmlParams(String... keysAndValues) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        buffer.append("<offer_params>");
        assertEquals(0, keysAndValues.length % 2);
        for (int i = 0; i < keysAndValues.length / 2; i++) {
            buffer.append("<param name=").append('"').append(keysAndValues[i * 2]).append('"').append(">");
            buffer.append("<![CDATA[").append(keysAndValues[i * 2 + 1]).append("]]>");
            buffer.append("</param>");
        }
        buffer.append("</offer_params>");
        return buffer.toString();
    }

    private void check(Offer expect) {
        expect.normalizeFields();

        var matchGen = holderGen.matchOffers();
        CoreTestUtils.compare(List.of(expect), matchGen.getUpdateList());
        CoreTestUtils.compare(List.of(), matchGen.getDeleteList());
    }

    private void init(Consumer<NewOfferGen> initNewOfferGen) {
        NewOfferGen newOfferGen = createNewOfferGen();

        initNewOfferGen.accept(newOfferGen);

        holderGen.onNewOffer(newOfferGen);
    }

    private void prepareCtx(int shopId, int feedId) {
        prepareCtx(shopId, feedId, false);
    }

    private void prepareCtx(int shopId, int feedId, boolean blueOnBlue) {
        context = new ProcessingContext(
                TmsTestUtils.defaultOffersArg(shopId),
                shop(shopId),
                getInstant(),
                feedId,
                new ShopLoopShopState(),
                TaskInfo.UNKNOWN
        );

        holderGen = new OffersHolderGen(context, 16, blueOnBlue);
    }
}

