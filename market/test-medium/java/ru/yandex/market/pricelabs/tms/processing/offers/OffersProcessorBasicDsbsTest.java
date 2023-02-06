package ru.yandex.market.pricelabs.tms.processing.offers;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import NMarket.NAmore.NAutostrategy.MarketAmoreService;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.pricelabs.apis.LogOutput;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyFilter;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyFilterSimple;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySave;
import ru.yandex.market.pricelabs.model.Autostrategy;
import ru.yandex.market.pricelabs.model.AutostrategyOffer;
import ru.yandex.market.pricelabs.model.Offer;
import ru.yandex.market.pricelabs.model.VendorDatasource;
import ru.yandex.market.pricelabs.model.types.AutostrategyType;
import ru.yandex.market.pricelabs.model.types.ShopType;
import ru.yandex.market.pricelabs.model.types.Status;
import ru.yandex.market.pricelabs.model.types.StrategyType;
import ru.yandex.market.pricelabs.tms.processing.TaskInfo;
import ru.yandex.market.pricelabs.tms.processing.TmsTestUtils;
import ru.yandex.market.pricelabs.tms.processing.autostrategies.AbstractAutostrategiesMetaProcessorTest;
import ru.yandex.market.pricelabs.tms.processing.imports.VendorDatasourcesProcessor;

import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.autostrategyOfferTarget;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.autostrategyShopState;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.shop;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.update;
import static ru.yandex.market.pricelabs.tms.processing.offers.OffersCheckers.autostrategyState;
import static ru.yandex.market.pricelabs.tms.processing.offers.OffersCheckers.autostrategyStateHistory;
import static ru.yandex.market.pricelabs.tms.processing.offers.OffersCheckers.vendorAutostrategy;

public class OffersProcessorBasicDsbsTest extends AbstractOffersProcessorTest {

    private final AtomicInteger requestCounter = new AtomicInteger();

    @BeforeEach
    void init() {
        super.init();
        testControls.resetShops();
        testControls.saveShop(shop(SHOP_ID, FEED_ID, 213, s -> {
            s.setType(ShopType.DSBS);
            s.setBusiness_id(BUSINESS_ID);
        }));
        requestCounter.set(101);
    }

    @Test
    void testOnlyNewOffersWithCpaAutostrategiesForDsbsShop()
            throws InvalidProtocolBufferException {

        var settings = autostrategySettings(200);
        processor.setBatchSize(1000);

        final long minPrice = 1000000L;

        var auto1 = new AutostrategySave()
                .name("Test1")
                .enabled(true)
                .filter(new AutostrategyFilter()
                        .type(AutostrategyFilter.TypeEnum.SIMPLE)
                        .simple(new AutostrategyFilterSimple().priceFrom(minPrice)))
                .settings(settings);
        long autostrategyBid = settings.getCpa().getDrrBid();
        int id1 = metaProcessor.create(UID, SHOP_ID, auto1);

        Instant now = getInstant();
        var existingRows = executor.asCreated(readActiveOffers());
        var expectRows = update(existingRows, offer -> {
            if (offer.getPrice() >= minPrice) {
                offer.setApp_autostrategy_id(id1);
                offer.setStrategy_modified_at(now);
                offer.setBid(autostrategyBid);

                if (autostrategyBid > 0) {
                    offer.setStrategy_type(StrategyType.STRATEGY);
                }
            }

            offer.setVendor_strategy_updated_at(now);
        });

        var expect = expectBuilder()
                .action(() -> processorRouter.syncAutostrategiesWhite(args, new TaskInfo(getInstant(), taskWriter)))
                .existingRows(existingRows)
                .expectRows(expectRows)
                .autostrategyStates(List.of(
                        autostrategyState(id1, true, 2, 0, now))
                )
                .autostrategyOffers(List.of(
                        autostrategyOfferTarget(SHOP_ID, FEED_ID, "4370", SHOP_ID, id1, 0, now, BUSINESS_ID, 0,
                                (int) autostrategyBid),
                        autostrategyOfferTarget(SHOP_ID, FEED_ID, "4371", SHOP_ID, id1, 0, now, BUSINESS_ID, 0,
                                (int) autostrategyBid)
                ))
                .autostrategyStatesHistory(List.of(
                        autostrategyStateHistory(id1, true, 2, 0, now))
                )
                .autostrategyShopStates(List.of(
                        autostrategyShopState(SHOP_ID, auto -> {
                            auto.setProcess_start(now);
                            auto.setProcess_complete(now);
                            auto.setUpdated_at(now);
                        })
                ))
                .response(LogOutput.FeedStateConst.OK);

        this.test(expect.build());

        checkers.checkAmore("add_proto", expectAmore ->
                expectAmore.addShopsBuilder()
                        .setShopId(SHOP_ID)
                        .setNOffers(2)
                        .setTsCreate(timeSource().getMillis())
                        .addAsParams(MarketAmoreService.TAutostrategies.AutostrategyParams.newBuilder()
                                .setUid(id1)
                                .setCpa(MarketAmoreService.TAutostrategies.CpaParams.newBuilder().setCpa(200))));
        mockWebServerAmore.checkNoMessages();
    }

    //CHECKSTYLE:OFF
    @Test
    void testVendorAutostrategiesDsbs() throws InvalidProtocolBufferException {
        var settings = autostrategySettings(200);
        processor.setBatchSize(1000);

        testControls.executeInParallel(
                () -> vendorDatasourceExecutor.clearTargetTable(),
                () -> AbstractAutostrategiesMetaProcessorTest.cleanupTables(
                        metaProcessorVendorBlue, stateProcessorVendorBlue, testControls),
                () -> autostrategyOffersExecutorVendorSource.clearTargetTable(),
                () -> brandBusiness.clearTargetTable()
        );

        long minPrice = 1000000L;

        var auto1 = new AutostrategySave()
                .name("Test1")
                .enabled(true)
                .filter(new AutostrategyFilter()
                        .type(AutostrategyFilter.TypeEnum.SIMPLE)
                        .simple(new AutostrategyFilterSimple().priceFrom(minPrice)))
                .settings(settings);

        int id1 = metaProcessor.create(UID, SHOP_ID, auto1);
        long autostrategyBid = settings.getCpa().getDrrBid();
        var model1 = 1;
        var model2 = 2;
        var model3 = 3;
        var model4 = 4;
        var model5 = 5;

        var vid1 = metaProcessorVendorBlue.create(UID, VENDOR_ID,
                vendorAutostrategy(101, List.of(BUSINESS_ID), List.of(model1)));

        var vid1x = metaProcessorVendorBlue.create(UID, VENDOR_ID2,
                vendorAutostrategy(110, List.of(), List.of(model1)));
        var vid2 = metaProcessorVendorBlue.create(UID, VENDOR_ID2,
                vendorAutostrategy(105, List.of(BUSINESS_ID), List.of(model2)));
        var vid3 = metaProcessorVendorBlue.create(UID, VENDOR_ID2,
                vendorAutostrategy(105, List.of(BUSINESS_ID), List.of(model3)));
        var vid3x = metaProcessorVendorBlue.create(UID, VENDOR_ID2,
                vendorAutostrategy(103, List.of(), List.of(model3)));

        var vid4 = metaProcessorVendorBlue.create(UID, VENDOR_ID,
                vendorAutostrategy(104, List.of(), List.of(model4)));
        var vid5 = metaProcessorVendorBlue.create(UID, VENDOR_ID,
                vendorAutostrategy(104, List.of(), List.of(model5)));
        var vid2xx = metaProcessorVendorBlue.create(UID, VENDOR_ID,
                vendorAutostrategy(106, List.of(), List.of(model2)));

        vendorDatasourceExecutor.insert(List.of(
                new VendorDatasource(VendorDatasourcesProcessor.PRODUCT_ID, VENDOR_ID, DATASOURCE_ID,
                        getInstant()),
                new VendorDatasource(VendorDatasourcesProcessor.PRODUCT_ID, VENDOR_ID2, DATASOURCE_ID2,
                        getInstant())));

        // Настройки вендорских АС:
        // vid1 = never
        // vid1x = models(1)
        // vid2 = never
        // vid3 = models(3)
        // vid3x = never
        // vid4 = never
        // vid5 = models(4,5)
        // vid2xx = models(2)

        var expectModelId = Map.of(
                "4179", model1,
                "4335", model2,
                "4370", model3,
                "4371", model4,
                "4584", model5);
        var expectVendorAutostrategyMap = Map.of(
                "4179", vid1x,
                "4335", vid2xx,
                "4370", vid3,
                "4371", vid5,
                "4584", vid5);

        var offerIdSplit = "4370";
        var vendorA = "vendor-a";
        var vendorB = "vendor-b";
        var vendorIdA = 336;
        var vendorIdB = 337;

        Consumer<Offer> fixOffer = offer -> {
            if (offer.getOffer_id().compareTo(offerIdSplit) <= 0) {
                offer.setVendor_name(vendorA);
                offer.setVendor_id(vendorIdA);
            } else {
                offer.setVendor_name(vendorB);
                offer.setVendor_id(vendorIdB);
            }
            offer.setModel_id(expectModelId.getOrDefault(offer.getOffer_id(), 0));
        };

        var now = getInstant();

        var existingRows = update(executor.asCreated(readActiveOffers()), offer -> {
            if (offer.getStatus() == Status.ACTIVE) {
                fixOffer.accept(offer);
                offer.setMarket_category_id((int) offer.getCategory_id());
                offer.setShop_sku(offer.getOffer_id());
            }
        });
        var expectRows = update(existingRows, offer -> {
            if (offer.getPrice() >= minPrice) {
                offer.setApp_autostrategy_id(id1);
                offer.setStrategy_modified_at(now);
                offer.setBid(autostrategyBid);

                if (autostrategyBid > 0) {
                    offer.setStrategy_type(StrategyType.STRATEGY);
                }
            }

            offer.setApp_vendor_autostrategy_id(expectVendorAutostrategyMap.getOrDefault(offer.getOffer_id(), 0));

            if (offer.getApp_vendor_autostrategy_id_int() > 0) {
                offer.setVendor_strategy_modified_at(now);
            }

            offer.setVendor_strategy_updated_at(now);

            offer.normalizeFields();
        });

        Consumer<AutostrategyOffer> ds1 = a -> a.setDatasource_id(DATASOURCE_ID);
        Consumer<AutostrategyOffer> ds2 = a -> a.setDatasource_id(DATASOURCE_ID2);

        var expect = expectBuilder()
                .action(() -> processorRouter.syncAutostrategiesWhite(args, new TaskInfo(getInstant(), taskWriter)))
                .existingRows(existingRows)
                .expectRows(expectRows)
                .feedId(FEED_ID)
                .autostrategyStates(List.of(
                        autostrategyState(id1, true, 2, 0, now))
                )
                .autostrategyOffers(List.of(
                        autostrategyOfferTarget(SHOP_ID, FEED_ID, "4370", SHOP_ID, id1, 0, now, BUSINESS_ID, 3,
                                (int) autostrategyBid),
                        autostrategyOfferTarget(SHOP_ID, FEED_ID, "4371", SHOP_ID, id1, 0, now, BUSINESS_ID, 4,
                                (int) autostrategyBid)
                ))
                .autostrategyStatesHistory(List.of(
                        autostrategyStateHistory(id1, true, 2, 0, now))
                )
                .autostrategyShopStates(List.of(
                        autostrategyShopState(SHOP_ID, auto -> {
                            auto.setProcess_start(now);
                            auto.setProcess_complete(now);
                            auto.setUpdated_at(now);
                        })
                ))
                .response(LogOutput.FeedStateConst.OK);

        this.test(expect.build());

        checkers.checkAmore("add_proto", expectAmore ->
                expectAmore.addShopsBuilder()
                        .setShopId(SHOP_ID)
                        .setNOffers(2)
                        .setTsCreate(timeSource().getMillis())
                        .addAsParams(MarketAmoreService.TAutostrategies.AutostrategyParams.newBuilder()
                                .setUid(id1)
                                .setCpa(MarketAmoreService.TAutostrategies.CpaParams.newBuilder().setCpa(200))));
        mockWebServerAmore.checkNoMessages();
    }
    //CHECKSTYLE:ON

    private Offer offer(int feedId, String offerId, long bid) {
        return TmsTestUtils.offer(offerId, o -> {
            o.setShop_id(shop.getShop_id());
            o.setFeed_id(feedId);
            Autostrategy autostrategy = TmsTestUtils.autostrategy(1, (int) shop.getShop_id());
            Autostrategy.CpaStrategySettings cpa = new Autostrategy.CpaStrategySettings();
            cpa.setDrr_bid(bid);
            autostrategy.setType(AutostrategyType.CPA);
            autostrategy.setCpaSettings(cpa);
            o.getOfferState().applyAutostrategy(autostrategy, getInstant());
        });
    }
}
