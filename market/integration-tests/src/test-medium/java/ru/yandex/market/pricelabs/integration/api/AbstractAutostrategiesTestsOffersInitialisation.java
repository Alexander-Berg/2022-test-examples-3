package ru.yandex.market.pricelabs.integration.api;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import ru.yandex.market.pricelabs.integration.AbstractIntegrationSpringConfiguration;
import ru.yandex.market.pricelabs.model.Offer;
import ru.yandex.market.pricelabs.model.types.Status;
import ru.yandex.market.pricelabs.model.types.StrategyType;
import ru.yandex.market.pricelabs.tms.processing.YtScenarioExecutor;

import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.offer;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.update;

public class AbstractAutostrategiesTestsOffersInitialisation extends AbstractIntegrationSpringConfiguration {
    public static final int FEED1 = 2;
    public static final int FEED2 = 3;

    public static final int MODEL1 = 1001;
    public static final int MODEL2 = 1002;
    public static final int MODEL3 = 1003;
    public static final int CATEGORY1 = 2001;
    public static final int CATEGORY2 = 2002;
    public static final int CATEGORY3 = 2003;

    public static final int CATEGORY4 = 2004;
    public static final int CATEGORY5 = 2005;
    public static final int CATEGORY6 = 2006;
    public static final int CATEGORY7 = 2007;
    public static final int CATEGORY8 = 2008;

    protected Runnable getSaveOffersRunnable(boolean sskuOffer, int shopIdWithAutostrategies,
                                             int shopidWithoutAutostrategies,
                                             YtScenarioExecutor<Offer> executor) {
        return () -> {
            var offers = List.of(offersSampleShop(sskuOffer, shopIdWithAutostrategies),
                            offersSampleShopNoAutostrategy(sskuOffer, shopidWithoutAutostrategies))
                    .stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
            executor.insert(offers);
        };
    }

    protected List<Offer> offersSampleShopNoAutostrategy(boolean sskuOffer, int shopId) {
        return update(offersSampleShop(sskuOffer, shopId), offer -> {
            offer.setApp_autostrategy_id(0);
        });
    }

    protected List<Offer> offersSampleShop(boolean sskuOffer, int shopId) {
        return List.of(
                offer("0", o -> {
                    o.setShop_id(shopId);
                    o.setFeed_id(FEED1);
                    o.setStatus(Status.ACTIVE);
                    o.setName("name1");
                    o.setPrice(999);
                    o.setModel_id(MODEL1);
                    o.setApp_autostrategy_id(3);
                    o.setSsku_offer(sskuOffer);
                    o.setCategory_id(CATEGORY1);
                }),
                offer("1", o -> {
                    o.setShop_id(shopId);
                    o.setFeed_id(FEED1);
                    o.setStatus(Status.ACTIVE);
                    o.setName("name2");
                    o.setPrice(998);
                    o.setModel_id(0);
                    o.setApp_autostrategy_id(3);
                    o.setStrategy_type(StrategyType.STRATEGY);
                    o.setSsku_offer(sskuOffer);
                    o.setCategory_id(CATEGORY1);
                    o.setVendor_name("Samsung");
                    o.setVendor_name_lower(o.getVendor_name().toLowerCase());
                }),
                offer("2", o -> {
                    o.setShop_id(shopId);
                    o.setFeed_id(FEED1);
                    o.setStatus(Status.ACTIVE);
                    o.setName("name3");
                    o.setModel_id(MODEL3);
                    o.setApp_autostrategy_id(3);
                    o.setSsku_offer(sskuOffer);
                    o.setCategory_id(CATEGORY1);
                }),
                offer("3", o -> {
                    o.setShop_id(shopId);
                    o.setFeed_id(FEED2);
                    o.setStatus(Status.ACTIVE);
                    o.setName("name4");
                    o.setPrice(1000);
                    o.setModel_id(MODEL1);
                    o.setApp_autostrategy_id(3);
                    o.setSsku_offer(sskuOffer);
                }),
                offer("4", o -> {
                    o.setShop_id(shopId);
                    o.setFeed_id(FEED2);
                    o.setStatus(Status.ACTIVE);
                    o.setName("name5");
                    o.setPrice(1001);
                    o.setModel_id(MODEL2);
                    o.setApp_autostrategy_id(4);
                    o.setStrategy_type(StrategyType.STRATEGY);
                    o.setSsku_offer(sskuOffer);
                    o.setCategory_id(CATEGORY3);
                    o.setVendor_name("Google");
                    o.setVendor_name_lower(o.getVendor_name().toLowerCase());
                }),
                offer("5", o -> {
                    o.setShop_id(shopId);
                    o.setFeed_id(FEED2);
                    o.setStatus(Status.ACTIVE);
                    o.setName("name6");
                    o.setPrice(1006);
                    o.setModel_id(MODEL1);
                    o.setSsku_offer(sskuOffer);
                    o.setCategory_id(CATEGORY2);
                    o.setVendor_name("Bosch");
                    o.setVendor_name_lower(o.getVendor_name().toLowerCase());
                }),
                offer("6", o -> {
                    o.setShop_id(shopId);
                    o.setFeed_id(FEED2);
                    o.setStatus(Status.ACTIVE);
                    o.setName("name7");
                    o.setPrice(1006);
                    o.setModel_id(MODEL1);
                    o.setApp_strategy_id(5L);
                    o.setStrategy_type(StrategyType.CAMPAIGN);
                    o.setSsku_offer(sskuOffer);
                    o.setCategory_id(CATEGORY2);
                    o.setVendor_name("Bosch");
                    o.setVendor_name_lower(o.getVendor_name().toLowerCase());
                })
        );
    }
}
