//CHECKSTYLE:OFF
package ru.yandex.market.pricelabs.tms.processing.offers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import NMarket.NAmore.NAutostrategy.MarketAmoreService.TAutostrategies.AutostrategyParams;
import NMarket.NAmore.NAutostrategy.MarketAmoreService.TAutostrategies.CpaParams;
import NMarket.NAmore.NAutostrategy.MarketAmoreService.TAutostrategies.PositionalParams;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.pricelabs.apis.ApiConst;
import ru.yandex.market.pricelabs.apis.LogOutput;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyFilter;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyFilterSimple;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySave;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettings;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettingsCPA;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettingsCPO;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettingsDRR;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettingsPOS;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettingsVPOS;
import ru.yandex.market.pricelabs.misc.TimingUtils;
import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.pricelabs.model.Offer;
import ru.yandex.market.pricelabs.model.ShopFeed;
import ru.yandex.market.pricelabs.model.VendorDatasource;
import ru.yandex.market.pricelabs.model.VendorModelBid;
import ru.yandex.market.pricelabs.model.types.ShopType;
import ru.yandex.market.pricelabs.model.types.Status;
import ru.yandex.market.pricelabs.model.types.StrategyType;
import ru.yandex.market.pricelabs.processing.ShopArg;
import ru.yandex.market.pricelabs.tms.processing.TaskInfo;
import ru.yandex.market.pricelabs.tms.processing.TmsTestUtils;
import ru.yandex.market.pricelabs.tms.processing.autostrategies.AbstractAutostrategiesMetaProcessorTest;
import ru.yandex.market.pricelabs.tms.processing.imports.VendorDatasourcesProcessor;
import ru.yandex.market.pricelabs.tms.processing.offers.OffersHolderGen.PricelabsParams;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettings.TypeEnum.CPA;
import static ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettings.TypeEnum.CPO;
import static ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettings.TypeEnum.DRR;
import static ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettings.TypeEnum.POS;
import static ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettings.TypeEnum.VPOS;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.autostrategyOfferSource;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.autostrategyOfferTarget;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.autostrategyShopState;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.feeds;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.offer;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.offerPush;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.offerVendor;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.shop;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.update;
import static ru.yandex.market.pricelabs.tms.processing.offers.OffersCheckers.autostrategyState;
import static ru.yandex.market.pricelabs.tms.processing.offers.OffersCheckers.autostrategyStateHistory;
import static ru.yandex.market.pricelabs.tms.processing.offers.OffersCheckers.vendorAutostrategy;

@Slf4j
public class OffersProcessorBasicTest extends AbstractOffersProcessorTest {


    static void applyOfferUpdates(Offer offer, Instant instant) {
        offer.setUpdated_at(instant);
        offer.setStrategy_modified_at(instant);
        offer.setStrategy_applied_at(instant);
        offer.setStrategy_updated_at(instant);
    }

    public static AutostrategySave autostrategyToOffer(String name, String offerId,
                                                       AutostrategySettings.TypeEnum settingsType, long value) {
        return new AutostrategySave()
                .name(name)
                .enabled(true)
                .filter(new AutostrategyFilter()
                        .type(AutostrategyFilter.TypeEnum.SIMPLE)
                        .simple(new AutostrategyFilterSimple()
                                .addOfferIdsItem(offerId)))
                .settings(new AutostrategySettings()
                        .type(settingsType)
                        .cpo(settingsType == CPO ?
                                new AutostrategySettingsCPO().cpo(value) : null)
                        .drr(settingsType == DRR ?
                                new AutostrategySettingsDRR().takeRate(value) : null)
                        .pos(settingsType == POS ?
                                new AutostrategySettingsPOS().position(1).maxBid(value) : null)
                        .vpos(settingsType == VPOS ?
                                new AutostrategySettingsVPOS().position(1).maxBid(value) : null)
                        .cpa(settingsType == CPA ?
                                new AutostrategySettingsCPA().drrBid(value) : null));
    }

    @Test
    void testNoData() {
        var expect = expectBuilder();
        this.test(expect.build());
    }

    @Test
    void testFilteredData() {
        var expect = expectBuilder()
                .newRows(readSourceList(getSourceCsvPrefix() + "2.csv"))
                .expectRows(
                        update(
                                executor.asCreated(readTargetList("tms/processing/offers/offers_target2.csv")),
                                o -> o.setVendor_strategy_updated_at(getInstant())
                        )
                );
        this.test(expect.build());
    }

    @ParameterizedTest
    @MethodSource("ru.yandex.market.pricelabs.tms.processing.offers.OffersProcessorBasicScenarios#sizes")
    void testOnlyNewOffers(int batchSize) {
        processor.setBatchSize(batchSize);

        var expect = expectBuilder()
                .newRows(readSourceList())
                .expectRows(
                        update(
                                executor.asCreated(readActiveOffers()),
                                o -> o.setVendor_strategy_updated_at(getInstant())
                        )
                );

        this.test(expect.build());
    }
    //CHECKSTYLE:ON

    @ParameterizedTest
    @MethodSource("ru.yandex.market.pricelabs.tms.processing.offers.OffersProcessorBasicScenarios#sizes")
    void testOnlyOldOffers(int batchSize) {
        processor.setBatchSize(batchSize);

        var expect = expectBuilder()
                .existingRows(readTargetList())
                .expectRows(update(readTargetList(), offer -> {
                    if (offer.getStatus() == Status.ACTIVE) {
                        executor.asDeleted(offer);
                    }
                }));

        this.test(expect.build());
    }

    @Test
    void testUpdateWithSameOffersButNullifyField() {
        var expect = expectBuilder()
                .newRows(readSourceList())
                .existingRows(update(readTargetList(), offer -> {
                    if (offer.getStatus() == Status.ACTIVE) {
                        // strategy_id теперь вычисляется на основании текущего набора фильтров

                        // А это поле должно быть обнулено
                        offer.setVendor_code("Vendor-400");
                    }
                }))
                .expectRows(update(readTargetList(), offer -> {
                    if (offer.getStatus() == Status.ACTIVE) {
                        executor.asUpdated(offer);
                        offer.setVendor_strategy_updated_at(getInstant());
                    }
                }));

        this.test(expect.build());
    }

    //CHECKSTYLE:ON

    @ParameterizedTest
    @MethodSource("ru.yandex.market.pricelabs.tms.processing.offers.OffersProcessorBasicScenarios#sizes")
    void testSomeUpdateSomeDelete(int batchSize) {
        processor.setBatchSize(batchSize);

        // Добавляем первые 4 строчки
        // Существуют все записи кроме первых 2
        // Проверяем, что все записи кроме первых 4 удалены

        List<Offer> activeOffers = readActiveOffers();

        List<Offer> expectRows = new ArrayList<>();
        expectRows.addAll(executor.asCreated(activeOffers.subList(0, SMALL_BATCH)));
        expectRows.addAll(executor.asUpdated(activeOffers.subList(SMALL_BATCH, 4)));
        expectRows.addAll(executor.asDeleted(activeOffers.subList(4, activeOffers.size())));

        var expect = expectBuilder()
                .newRows(readSourceList().subList(0, 4))
                .existingRows(
                        readActiveOffers().subList(SMALL_BATCH, activeOffers.size())
                )
                .expectRows(
                        update(
                                expectRows,
                                o -> {
                                    if (o.getStatus() == Status.ACTIVE) {
                                        o.setVendor_strategy_updated_at(getInstant());
                                    }
                                }
                        )
                );

        this.test(expect.build());
    }

    @Test
    void testSyncAutostrategiesBlue() throws InvalidProtocolBufferException {
        var supplierShopId = SHOP_ID;
        var supplierFeedId = FEED_ID;
        args.setType(ShopType.SUPPLIER);

        testControls.executeInParallel(
                () -> executorBlue.clearTargetTable(),
                () -> {
                    testControls.resetShops();
                    testControls.saveShop(shop(SHOP_ID, s -> {
                        s.setBusiness_id(BUSINESS_ID);
                        s.setType(ShopType.SUPPLIER);
                        s.setFeeds(Set.of((long) FEED_ID));
                    }));
                },
                () -> AbstractAutostrategiesMetaProcessorTest.cleanupTables(
                        metaProcessorBlue, stateProcessorBlue, testControls),
                () -> autostrategyOffersExecutorBlue.clearTargetTable(),
                () -> autostrategiesShopStateExecutorBlue.clearTargetTable()
        );

        long minPrice = 1000000L;
        long autostrategyBid = 300;
        AutostrategySave blueAutostrategySave = new AutostrategySave()
                .name("Test1")
                .enabled(true)
                .filter(new AutostrategyFilter()
                        .type(AutostrategyFilter.TypeEnum.SIMPLE)
                        .simple(new AutostrategyFilterSimple()
                                .priceFrom(minPrice)))
                .settings(new AutostrategySettings()
                        .type(AutostrategySettings.TypeEnum.CPA)
                        .cpa(new AutostrategySettingsCPA()
                                .drrBid(autostrategyBid)));

        var id1 = metaProcessorBlue.create(UID, SHOP_ID, blueAutostrategySave);
        var now = getInstant();

        List<Offer> existingBlueOffers = update(readTargetList(), offer -> {
            offer.setShop_id(supplierShopId);
            offer.setFeed_id(supplierFeedId);
            offer.normalizeFields();
        });

        executorBlue.insert(existingBlueOffers);

        var expectBlueOffers = update(existingBlueOffers, offer -> {
            if (offer.getStatus() == Status.ACTIVE && offer.getPrice() >= minPrice) {
                offer.setApp_autostrategy_id(id1);
                offer.setUpdated_at(now);
                offer.setStrategy_updated_at(now);
                offer.setStrategy_modified_at(now);
                offer.setBid(autostrategyBid);
                offer.setStrategy_type(StrategyType.STRATEGY);
            }

            offer.normalizeFields();
        });

        processorRouter.syncAutostrategiesBlue(new ShopArg(supplierShopId, ShopType.SUPPLIER),
                new TaskInfo(getInstant(), taskWriter));

        testControls.executeInParallel(
                () -> executorBlue.verify(expectBlueOffers, OffersCheckers::resetWareAndCurrency),
                () -> autostrategiesStateExecutorBlue.verify(List.of(
                        autostrategyState(id1, true, 2, 0, now)
                )),
                () -> autostrategiesStateHistoryExecutorBlue.verify(List.of(
                        autostrategyStateHistory(id1, true, 2, 0, now)
                )),
                () -> autostrategiesShopStateExecutorBlue.verify(List.of(
                        autostrategyShopState(supplierShopId, auto -> {
                            auto.setProcess_start(now);
                            auto.setProcess_complete(now);
                            auto.setUpdated_at(now);
                        })
                )),
                () -> autostrategyOffersExecutorBlue.verify(List.of(
                        autostrategyOfferTarget(ApiConst.VIRTUAL_SHOP_BLUE, ApiConst.VIRTUAL_FEED_BLUE,
                                FEED_ID + ".4370", SHOP_ID, id1, 0, now, BUSINESS_ID, 0, (int) autostrategyBid),
                        autostrategyOfferTarget(ApiConst.VIRTUAL_SHOP_BLUE, ApiConst.VIRTUAL_FEED_BLUE,
                                FEED_ID + ".4371", SHOP_ID, id1, 0, now, BUSINESS_ID, 0, (int) autostrategyBid)
                ))
        );

        checkers.checkAmore("add_supplier", expectAmore ->
                expectAmore.addShopsBuilder()
                        .setShopId(SHOP_ID)
                        .setNOffers(2)
                        .setTsCreate(timeSource().getMillis())
                        .addAsParams(AutostrategyParams.newBuilder()
                                .setUid(id1)
                                .setCpa(CpaParams.newBuilder().setCpa(300))));
        mockWebServerAmore.checkNoMessages();
    }

    //CHECKSTYLE:OFF
    @Test
    void mapVendorBlueAutostrategies() {
        var virtualShopId = ApiConst.VIRTUAL_SHOP_BLUE;
        var virtualFeedId = ApiConst.VIRTUAL_FEED_BLUE;
        var supplierShopId = SHOP_ID;
        var supplierFeedId = FEED_ID;

        this.args.setShopId(virtualShopId);
        this.shop.setFeeds(Set.of((long) virtualFeedId));

        testControls.executeInParallel(
                () -> executorBlue.clearTargetTable(),
                () -> {
                    //testControls.resetShops();
                    saveAsVirtualShop();
                },
                () -> {
                    marketCategoriesExecutor.insertSource(categoriesProcessorTest.readMarketCategories());
                },
                () -> blueCategoriesExecutor.clearTargetTable(),
                () -> AbstractAutostrategiesMetaProcessorTest.cleanupTables(
                        metaProcessorVendorBlue, stateProcessorVendorBlue, testControls),
                () -> vendorDatasourceExecutor.clearTargetTable(),
                () -> autostrategyOffersExecutorVendorSource.clearTargetTable()
        );

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

        var modelDisabled = 6;
        var vidDisabled = metaProcessorVendorBlue.create(UID, VENDOR_ID2,
                vendorAutostrategy(999, List.of(), List.of(modelDisabled)).enabled(false));

        vendorDatasourceExecutor.insert(List.of(
                new VendorDatasource(VendorDatasourcesProcessor.PRODUCT_ID, VENDOR_ID, DATASOURCE_ID,
                        getInstant()),
                new VendorDatasource(VendorDatasourcesProcessor.PRODUCT_ID, VENDOR_ID2, DATASOURCE_ID2,
                        getInstant())));

        var expectModelId = Map.of(
                "4179", model1,
                "4335", model2,
                "4370", model3,
                "4371", model4,
                "4584", model5,
                "4105", modelDisabled);
        var expectVendorAutostrategyMap = Map.of(
                "4179", vid1x,
                "4335", vid2xx,
                "4370", vid3,
                "4371", vid5,
                "4584", vid5,
                "4105", vidDisabled);

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

        List<Offer> existingBlueOffers = update(readTargetList(), offer -> {
            //if ("4105".equals(offer.getOffer_id())) {
            //    offer.setApp_vendor_autostrategy_id(vidDisabled);
            //}

            offer.setShop_id(supplierShopId);
            offer.setFeed_id(supplierFeedId);
            fixOffer.accept(offer);
            offer.normalizeFields();
        });

        testControls.executeInParallel(
                () -> executorBlue.insert(existingBlueOffers)
        );

        var expectBlueOffers = update(existingBlueOffers, offer -> {
            offer.setApp_vendor_autostrategy_id(expectVendorAutostrategyMap.getOrDefault(offer.getOffer_id(), 0));

            if (offer.getApp_vendor_autostrategy_id_int() > 0) {
                offer.setVendor_strategy_updated_at(now);
                offer.setVendor_strategy_modified_at(now);
                offer.setUpdated_at(now);
            }

            offer.normalizeFields();
        });

        processorRouter.mapVendorBlueAutostrategies(new TaskInfo(getInstant(), taskWriter));

        testControls.executeInParallel(
                () -> executorBlue.verify(expectBlueOffers, OffersCheckers::resetWareAndCurrency),
                () -> autostrategyOffersExecutorVendorSource.verify(List.of(
                        autostrategyOfferSource(virtualShopId, virtualFeedId, supplierFeedId + ".4105", VENDOR_ID2,
                                vidDisabled, DATASOURCE_ID2, now, BUSINESS_ID, modelDisabled, 0, o -> {
                                    o.setShop_id(supplierShopId);
                                    o.setFeed_id(supplierFeedId);
                                    o.setOffer_id("4105");
                                }),
                        autostrategyOfferSource(virtualShopId, virtualFeedId, supplierFeedId + ".4179", VENDOR_ID2,
                                vid1x, DATASOURCE_ID2, now, BUSINESS_ID, model1, 110, o -> {
                                    o.setShop_id(supplierShopId);
                                    o.setFeed_id(supplierFeedId);
                                    o.setOffer_id("4179");
                                }),
                        autostrategyOfferSource(virtualShopId, virtualFeedId, supplierFeedId + ".4335", VENDOR_ID,
                                vid2xx, DATASOURCE_ID, now, BUSINESS_ID, model2, 106, o -> {
                                    o.setShop_id(supplierShopId);
                                    o.setFeed_id(supplierFeedId);
                                    o.setOffer_id("4335");
                                }),
                        autostrategyOfferSource(virtualShopId, virtualFeedId, supplierFeedId + ".4370", VENDOR_ID2,
                                vid3, DATASOURCE_ID2, now, BUSINESS_ID, model3, 105, o -> {
                                    o.setShop_id(supplierShopId);
                                    o.setFeed_id(supplierFeedId);
                                    o.setOffer_id("4370");
                                }),
                        autostrategyOfferSource(virtualShopId, virtualFeedId, supplierFeedId + ".4371", VENDOR_ID,
                                vid5, DATASOURCE_ID, now, BUSINESS_ID, model4, 104, o -> {
                                    o.setShop_id(supplierShopId);
                                    o.setFeed_id(supplierFeedId);
                                    o.setOffer_id("4371");
                                }),
                        autostrategyOfferSource(virtualShopId, virtualFeedId, supplierFeedId + ".4584", VENDOR_ID,
                                vid5, DATASOURCE_ID, now, BUSINESS_ID, model5, 104, o -> {
                                    o.setShop_id(supplierShopId);
                                    o.setFeed_id(supplierFeedId);
                                    o.setOffer_id("4584");
                                })))
        );
    }

    //CHECKSTYLE:OFF
    @Test
    void testSyncVendorAutostrategies() throws InvalidProtocolBufferException {
        var virtualShopId = ApiConst.VIRTUAL_SHOP_BLUE;
        var virtualFeedId = ApiConst.VIRTUAL_FEED_BLUE;
        var supplierShopId = SHOP_ID;
        var supplierFeedId = FEED_ID;

        this.args.setShopId(virtualShopId);
        this.shop.setFeeds(Set.of((long) virtualFeedId));

        testControls.executeInParallel(
                () -> executorBlue.clearTargetTable(),
                () -> saveAsVirtualShop(),
                () -> {
                    marketCategoriesExecutor.insertSource(categoriesProcessorTest.readMarketCategories());
                },
                () -> blueCategoriesExecutor.clearTargetTable(),
                () -> AbstractAutostrategiesMetaProcessorTest.cleanupTables(
                        metaProcessorVendorBlue, stateProcessorVendorBlue, testControls),
                () -> autostrategiesShopStateExecutorVendorBlue.clearTargetTable(),
                () -> vendorModelBid.clearTargetTable(),
                () -> vendorDatasourceExecutor.clearTargetTable(),
                () -> autostrategyOffersExecutorVendorSource.clearTargetTable(),
                () -> autostrategyOffersExecutorVendorTarget.clearTargetTable()
        );

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

        var modelDisabled = 6;
        var vidDisabled = metaProcessorVendorBlue.create(UID, VENDOR_ID2,
                vendorAutostrategy(999, List.of(), List.of(modelDisabled)).enabled(false));

        vendorDatasourceExecutor.insert(List.of(
                new VendorDatasource(VendorDatasourcesProcessor.PRODUCT_ID, VENDOR_ID, DATASOURCE_ID,
                        getInstant()),
                new VendorDatasource(VendorDatasourcesProcessor.PRODUCT_ID, VENDOR_ID2, DATASOURCE_ID2,
                        getInstant())));

        var now = getInstant();

        List<Offer> existingWhiteOffers = update(readTargetList(), offer -> {
            if ("4105".equals(offer.getOffer_id())) {
                offer.setApp_vendor_autostrategy_id(vidDisabled);
            } else {
                offer.setShop_id(virtualShopId);
                offer.setFeed_id(virtualFeedId);
                offer.setOffer_id(supplierFeedId + "." + offer.getOffer_id());
            }

            offer.normalizeFields();
        });

        testControls.executeInParallel(
                () -> executor.insert(existingWhiteOffers)
        );

        // Добавим белый оффер. Он тоже должен попасть в поколение для Амура вместе с синими
        var whiteOfferSourse = autostrategyOfferSource(SHOP_ID, FEED_ID, "9999", VENDOR_ID,
                vid5, DATASOURCE_ID2, now, BUSINESS_ID, model5, 555);
        var whiteOfferTarget = autostrategyOfferTarget(SHOP_ID, FEED_ID, "9999", VENDOR_ID,
                vid5, DATASOURCE_ID2, now, BUSINESS_ID, model5, 555);

        // Добавим оффер с неактивной стратегией. Он не должен попасть в рекомендации VendorModelBid
        var whiteDisabledOfferSourse = autostrategyOfferSource(SHOP_ID, FEED_ID, "4105",
                VENDOR_ID2, vidDisabled, DATASOURCE_ID2, now, BUSINESS_ID, model5, 999);
        autostrategyOffersExecutorVendorSource.insert(List.of(
                whiteDisabledOfferSourse,
                whiteOfferSourse,
                autostrategyOfferSource(virtualShopId, virtualFeedId, supplierFeedId + ".4179", VENDOR_ID2,
                        vid1x, DATASOURCE_ID2, now, BUSINESS_ID, model1, 110, o -> {
                            o.setShop_id(1);
                            o.setFeed_id(1);
                            o.setOffer_id("1");
                        }),
                autostrategyOfferSource(virtualShopId, virtualFeedId, supplierFeedId + ".4335", VENDOR_ID,
                        vid2xx, DATASOURCE_ID, now, BUSINESS_ID, model2, 106),
                autostrategyOfferSource(virtualShopId, virtualFeedId, supplierFeedId + ".4370", VENDOR_ID2,
                        vid3, DATASOURCE_ID2, now, BUSINESS_ID, model3, 105),
                autostrategyOfferSource(virtualShopId, virtualFeedId, supplierFeedId + ".4371", VENDOR_ID,
                        vid5, DATASOURCE_ID, now, BUSINESS_ID, model5, 104),
                autostrategyOfferSource(virtualShopId, virtualFeedId, supplierFeedId + ".4584", VENDOR_ID,
                        vid5, DATASOURCE_ID, now, BUSINESS_ID, model5, 104))
        );

        var whiteDisabledOfferTarget = autostrategyOfferTarget(SHOP_ID, FEED_ID, "4105",
                VENDOR_ID2, vidDisabled, DATASOURCE_ID2, now, BUSINESS_ID, model5, 999);

        processorRouter.syncVendorAutostrategies(new TaskInfo(getInstant(), taskWriter));

        testControls.executeInParallel(
                () -> autostrategyOffersExecutorVendorTarget.verify(List.of(
                        whiteOfferTarget,
                        autostrategyOfferTarget(virtualShopId, virtualFeedId, supplierFeedId + ".4371", VENDOR_ID,
                                vid5, DATASOURCE_ID, now, BUSINESS_ID, model5, 104),
                        autostrategyOfferTarget(virtualShopId, virtualFeedId, supplierFeedId + ".4584", VENDOR_ID,
                                vid5, DATASOURCE_ID, now, BUSINESS_ID, model5, 104),
                        autostrategyOfferTarget(virtualShopId, virtualFeedId, supplierFeedId + ".4335", VENDOR_ID,
                                vid2xx, DATASOURCE_ID, now, BUSINESS_ID, model2, 106),
                        autostrategyOfferTarget(virtualShopId, virtualFeedId, supplierFeedId + ".4179", VENDOR_ID2,
                                vid1x, DATASOURCE_ID2, now, BUSINESS_ID, model1, 110),
                        autostrategyOfferTarget(virtualShopId, virtualFeedId, supplierFeedId + ".4370", VENDOR_ID2,
                                vid3, DATASOURCE_ID2, now, BUSINESS_ID, model3, 105),
                        whiteDisabledOfferTarget)),
                () -> autostrategiesStateExecutorVendorBlue.verify(List.of(
                        autostrategyState(VENDOR_ID, vid1, true, 0, 0, now),
                        autostrategyState(VENDOR_ID, vid4, true, 0, 0, now),
                        autostrategyState(VENDOR_ID, vid5, true, 3, 0, now),
                        autostrategyState(VENDOR_ID, vid2xx, true, 1, 0, now),
                        autostrategyState(VENDOR_ID2, vid1x, true, 1, 0, now),
                        autostrategyState(VENDOR_ID2, vid2, true, 0, 0, now),
                        autostrategyState(VENDOR_ID2, vid3, true, 1, 0, now),
                        autostrategyState(VENDOR_ID2, vid3x, true, 0, 0, now),
                        autostrategyState(VENDOR_ID2, vidDisabled, false, 1, 0, now))),
                () -> autostrategiesStateHistoryExecutorVendorBlue.verify(List.of(
                        autostrategyStateHistory(VENDOR_ID, vid1, true, 0, 0, now),
                        autostrategyStateHistory(VENDOR_ID, vid4, true, 0, 0, now),
                        autostrategyStateHistory(VENDOR_ID, vid5, true, 3, 0, now),
                        autostrategyStateHistory(VENDOR_ID, vid2xx, true, 1, 0, now),
                        autostrategyStateHistory(VENDOR_ID2, vid1x, true, 1, 0, now),
                        autostrategyStateHistory(VENDOR_ID2, vid2, true, 0, 0, now),
                        autostrategyStateHistory(VENDOR_ID2, vid3, true, 1, 0, now),
                        autostrategyStateHistory(VENDOR_ID2, vid3x, true, 0, 0, now),
                        autostrategyStateHistory(VENDOR_ID2, vidDisabled, false, 1, 0, now))),
                () -> autostrategiesShopStateExecutorVendorBlue.verify(List.of(
                        autostrategyShopState(VENDOR_ID, auto -> {
                            auto.setProcess_start(now);
                            auto.setProcess_complete(now);
                            auto.setUpdated_at(now);
                        }),
                        autostrategyShopState(VENDOR_ID2, auto -> {
                            auto.setProcess_start(now);
                            auto.setProcess_complete(now);
                            auto.setUpdated_at(now);
                        }))),
                () -> vendorModelBid.verify(List.of(
                        new VendorModelBid(model1, 110, now),
                        new VendorModelBid(model2, 106, now),
                        new VendorModelBid(model3, 105, now),
                        new VendorModelBid(model5, 555, now))
                )
        );

        // Проверим, что у нас уходит в АМУР
        checkers.checkAmore("add_vendor", expectAmore -> {
            expectAmore.addShopsBuilder()
                    .setShopId(VENDOR_ID)
                    .setNOffers(4)
                    .setTsCreate(timeSource().getMillis())
                    .addAsParams(AutostrategyParams.newBuilder()
                            .setUid(vid5)
                            .setPositional(PositionalParams.newBuilder()
                                    .setPosition(1)
                                    .setMaxBid(104)))
                    .addAsParams(AutostrategyParams.newBuilder()
                            .setUid(vid2xx)
                            .setPositional(PositionalParams.newBuilder()
                                    .setPosition(1)
                                    .setMaxBid(106)));
            expectAmore.addShopsBuilder()
                    .setShopId(VENDOR_ID2)
                    .setNOffers(2)
                    .setTsCreate(timeSource().getMillis())
                    .addAsParams(AutostrategyParams.newBuilder()
                            .setUid(vid1x)
                            .setPositional(PositionalParams.newBuilder()
                                    .setPosition(1)
                                    .setMaxBid(110)))
                    .addAsParams(AutostrategyParams.newBuilder()
                            .setUid(vid3)
                            .setPositional(PositionalParams.newBuilder()
                                    .setPosition(1)
                                    .setMaxBid(105)));
        });

        mockWebServerAmore.checkNoMessages();
    }

    @Test
    void testWithVendorsAndParameters() {

        testControls.executeInParallel(
                () -> vendorsExecutor.clearTargetTable()
        );

        Set<String> params = new TreeSet<>();
        Set<String> vendors = new TreeSet<>();

        var cnt = new Object() {
            int e;
            int a;
        };
        var insert = TmsTestUtils.map(readSourceList(), executor::copySource, offer -> {
            var i = ++cnt.e;
            var si = String.valueOf(i);
            offer.setVendor("vendor_" + i);

            var plParams = List.of(
                    new PricelabsParams("p1_" + i, si),
                    new PricelabsParams("p2_" + i, si));
            offer.setPricelabs_params(Utils.toJsonString(plParams));
        });

        var expectRows = update(readActiveOffers(), offer -> {
            var i = ++cnt.a;
            offer.setVendor_name("vendor_" + i);
            offer.setParams_map(Map.of("p1_" + i, i, "p2_" + i, i));
            offer.setVendor_name_lower(offer.getVendor_name().toLowerCase());

            vendors.add(offer.getVendor_name());
            params.addAll(offer.getParams_map().keySet());
        });

         //assertEquals(cnt.e, cnt.a);

        var expect = expectBuilder()
                .newRows(insert)
                .expectRows(
                        update(
                                executor.asCreated(expectRows),
                                o -> o.setVendor_strategy_updated_at(getInstant())
                        )
                );

        this.test(expect.build());

        testControls.executeInParallel(
                () -> vendorsExecutor.verify(
                        vendors.stream()
                                .map(name -> offerVendor(SHOP_ID, name, getInstant()))
                                .collect(Collectors.toList()))
        );
    }

    @ParameterizedTest
    @MethodSource("ru.yandex.market.pricelabs.tms.processing.offers.OffersProcessorBasicScenarios#" +
            "autostrategiesScenarios")
    void testOnlyNewOffersWithAutostrategies(FullLoopParams params, AutostrategySettings settings,
                                             AutostrategyParams amoreParams) throws InvalidProtocolBufferException {
        processor.setBatchSize(params.getBatchSize());

        final long minPrice = 1000000L;

        var auto1 = new AutostrategySave()
                .name("Test1")
                .enabled(true)
                .filter(new AutostrategyFilter()
                        .type(AutostrategyFilter.TypeEnum.SIMPLE)
                        .simple(new AutostrategyFilterSimple()
                                .priceFrom(minPrice)))
                .settings(settings);

        int id1 = metaProcessor.create(UID, SHOP_ID, auto1);
        long autostrategyBid = settings.getType() == CPA ? settings.getCpa().getDrrBid() : 0;
        auto1.name("Test2")
                .enabled(false)
                .getFilter().getSimple().priceFrom(1L);
        int id2 = metaProcessor.create(UID, SHOP_ID, auto1); // Выключена

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

        // 2 оффера подпадут под автостратгии
        var expect = expectBuilder()
                .action(() -> processorRouter.syncAutostrategiesWhite(args, new TaskInfo(getInstant(), taskWriter)))
                .existingRows(existingRows)
                .expectRows(expectRows)
                .autostrategyOffers(List.of(
                        autostrategyOfferTarget(SHOP_ID, FEED_ID, "4370", SHOP_ID, id1, 0, now, BUSINESS_ID, 0,
                                (int) autostrategyBid),
                        autostrategyOfferTarget(SHOP_ID, FEED_ID, "4371", SHOP_ID, id1, 0, now, BUSINESS_ID, 0,
                                (int) autostrategyBid)
                ))
                .autostrategyStates(List.of(
                        autostrategyState(id1, true, 2, 0, now),
                        autostrategyState(id2, false, 0, 0, now)))
                .autostrategyStatesHistory(List.of(
                        autostrategyStateHistory(id1, true, 2, 0, now),
                        autostrategyStateHistory(id2, false, 0, 0, now)
                ))
                .autostrategyShopStates(List.of(
                        autostrategyShopState(SHOP_ID, auto -> {
                            auto.setProcess_start(now);
                            auto.setProcess_complete(now);
                            auto.setUpdated_at(now);
                        })
                ));

        expect.response(LogOutput.FeedStateConst.OK);

        this.test(expect.build());

        checkers.checkAmore("add_proto", expectAmore ->
                expectAmore.addShopsBuilder()
                        .setShopId(SHOP_ID)
                        .setNOffers(2)
                        .setTsCreate(timeSource().getMillis())
                        .addAsParams(amoreParams.toBuilder().setUid(id1)));
        mockWebServerAmore.checkNoMessages();
    }

    @Test
    void testOnlyNewOffersWithAutostrategiesThenResetAutostrategies() throws InvalidProtocolBufferException {
        final long minPrice = 1000000L;

        testOnlyNewOffersWithAutostrategies(new FullLoopParams.FullLoopParamsBuilder().build(),
                new AutostrategySettings()
                        .type(CPA)
                        .cpa(new AutostrategySettingsCPA()
                                .drrBid(300L)),
                AutostrategyParams.newBuilder()
                        .setCpa(CpaParams.newBuilder().setCpa(300))
                        .build());

        var before = getInstant();
        TimingUtils.addTime(10000);

        var strategies = metaProcessor.selectAll();
        assertEquals(2, strategies.size());
        var id1 = strategies.get(0).getAutostrategy_id();
        var id2 = strategies.get(1).getAutostrategy_id();

        // Удалили автостратегию - запускаем процессинг
        metaProcessor.delete(1, SHOP_ID, id1);

        Instant now = getInstant();
        var existingRows = executor.asCreated(readActiveOffers());
        var expectRows = update(existingRows, offer -> {
            offer.setCreated_at(before);

            if (offer.getPrice() >= minPrice) {
                offer.setApp_autostrategy_id(0); // Привязки сброшены
                offer.setStrategy_modified_at(now);
            }

            offer.setVendor_strategy_updated_at(now);
        });

        // автостратегии будут сброшены у двух офферов
        var expect = expectBuilder()
                .action(() -> processorRouter.syncAutostrategiesWhite(args, new TaskInfo(getInstant(), taskWriter)))
                .existingRows(existingRows)
                .expectRows(expectRows)
                .autostrategyOffers(List.of(
                        autostrategyOfferTarget(SHOP_ID, FEED_ID, "4370", SHOP_ID, id1, 0, before, BUSINESS_ID, 0, 300),
                        autostrategyOfferTarget(SHOP_ID, FEED_ID, "4371", SHOP_ID, id1, 0, before, BUSINESS_ID, 0, 300),
                        autostrategyOfferTarget(SHOP_ID, FEED_ID, "4370", SHOP_ID, 0, 0, now, BUSINESS_ID, 0, 0,
                                o -> o.setType(null)),
                        autostrategyOfferTarget(SHOP_ID, FEED_ID, "4371", SHOP_ID, 0, 0, now, BUSINESS_ID, 0, 0,
                                o -> o.setType(null))
                ))
                .autostrategyStates(List.of(
                        // Состояние привязок автостратегий не поменялось
                        autostrategyState(id1, true, 2, 0, before),
                        autostrategyState(id2, false, 0, 0, now)
                ))
                .autostrategyStatesHistory(List.of(
                        autostrategyStateHistory(id1, true, 2, 0, before),
                        autostrategyStateHistory(id2, false, 0, 0, before),
                        autostrategyStateHistory(id2, false, 0, 0, now)
                ))
                .autostrategyShopStates(List.of(
                        autostrategyShopState(SHOP_ID, auto -> {
                            auto.setProcess_start(now);
                            auto.setProcess_complete(now);
                            auto.setUpdated_at(now);
                        })
                ));

        this.test(expect.build());

        checkers.checkAmore("add_proto", expectAmore ->
                expectAmore.addShopsBuilder()
                        .setShopId(SHOP_ID)
                        .setNOffers(0)
                        .setTsCreate(timeSource().getMillis()));
        mockWebServerAmore.checkNoMessages();
    }

    @Test
    @Timeout(30)
    void testNewOffersWithManyFeeds() {
        var feedIds = List.of(FEED_ID + 1, FEED_ID + 2, FEED_ID + 3, FEED_ID + 4, FEED_ID + 5,
                FEED_ID + 6, FEED_ID + 7, FEED_ID + 8, FEED_ID + 9);

        List<ShopFeed> feeds = new ArrayList<>(feedIds.size() + 1);
        feeds.addAll(feeds(FEED_ID));
        feeds.addAll(feeds(feedIds));

        shop.setFeeds(Set.of(
                (long) FEED_ID, (long) FEED_ID + 1, (long) FEED_ID + 2, (long) FEED_ID + 3, (long) FEED_ID + 4,
                (long) FEED_ID + 5, (long) FEED_ID + 6, (long) FEED_ID + 7, (long) FEED_ID + 8, (long) FEED_ID + 9
        ));
        testControls.saveShop(shop);

        var expect = expectBuilder()
                .newRows(readSourceList())
                .expectRows(
                        executor.asCreated(
                                update(
                                        readActiveOffers(),
                                        o -> o.setVendor_strategy_updated_at(getInstant()))
                        )
                );

        this.test(expect.build());
    }

    //@Test
    void testPushWithStrategies() throws InvalidProtocolBufferException {
        var dbsShopId = 123;
        var dbsFeedId = 123;

        testControls.saveShop(shop(dbsShopId, s -> s.setFeeds(Set.of((long) dbsFeedId))));
        testControls.saveDsbsShopDat(dbsShopId);

        var autostrategyAmore1 = autostrategyToOffer("Amore white 1", "1", DRR, 100);
        var autostrategyAmore2 = autostrategyToOffer("Amore white 2", "5", DRR, 200);
        var amoreAutostrategyId1 = metaProcessor.create(UID, SHOP_ID, autostrategyAmore1);
        var amoreAutostrategyId2 = metaProcessor.create(UID, SHOP_ID, autostrategyAmore2);

        var autostrategyCpa = autostrategyToOffer("Dbs cpa white", "6", CPA, 100);
        metaProcessor.create(UID, dbsShopId, autostrategyCpa);

        var existingOffers = List.of(
                // Этого оффера нет в пуше, но по нему все равно должна отправиться стратегия в Амур
                offer("1", o -> {
                    o.setShop_id(SHOP_ID);
                    o.setFeed_id(FEED_ID);
                    o.setStatus(Status.ACTIVE);
                    o.setPrice(12300);
                    o.setBid(20L);
                }),
                offer("2", o -> {
                    o.setShop_id(SHOP_ID);
                    o.setFeed_id(FEED_ID);
                }),
                offer("3", o -> {
                    o.setShop_id(SHOP_ID);
                    o.setFeed_id(FEED_ID);
                })
        );
        executor.insert(existingOffers);

        var pushPrice = OffersHolderPush.convertPrice(12300);
        var pushOffers = List.of(
                offerPush(SHOP_ID, FEED_ID, "2", b -> b.setPrice(b.getPriceBuilder().setPrice(pushPrice))),
                offerPush(SHOP_ID, FEED_ID, "3", b -> b.setPrice(b.getPriceBuilder().setPrice(pushPrice))),
                offerPush(SHOP_ID, FEED_ID, "4", b -> b.setPrice(b.getPriceBuilder().setPrice(pushPrice))),
                offerPush(SHOP_ID, FEED_ID, "5")
        );
        var pushOffersDbs = List.of(
                offerPush(dbsShopId, dbsFeedId, "6")
        );
        shopOffersProcessor.pushOffers(pushOffers);
        shopOffersProcessor.pushOffers(pushOffersDbs);

        var targetOffers = executor.selectTargetRows();
        assertEquals(6, targetOffers.size());

        // Проверяем обычные стратегии
        targetOffers.forEach(o -> {
            if (Integer.parseInt(o.getOffer_id()) <= 4) {
                assertEquals(12300, o.getPrice());
                assertEquals(20, o.getBid_long());
            }
        });
    }

    //@Test
    void testPushWithEmptyStrategies() throws InvalidProtocolBufferException {
        var dbsShopId = 123;
        var dbsFeedId = 123;

        testControls.saveShop(shop(dbsShopId, s -> s.setFeeds(Set.of((long) dbsFeedId))));
        testControls.saveDsbsShopDat(dbsShopId);

        var autostrategy = autostrategyToOffer("Test", "3", DRR, 100);
        var id = metaProcessor.create(UID, SHOP_ID, autostrategy);

        // Удаляем стратегию, чтобы остались записи только в autostrategies_history
        metaProcessor.delete(UID, SHOP_ID, id);

        var existingOffers = List.of(
                offer("1", o -> {
                    o.setShop_id(SHOP_ID);
                    o.setFeed_id(FEED_ID);
                    o.setApp_autostrategy_id(123);
                    // У оффера была какая-то стратегия. Так как ее уже нет, то станет 0
                }),
                offer("2", o -> {
                    o.setShop_id(dbsShopId);
                    o.setFeed_id(dbsFeedId);
                    o.setApp_autostrategy_id(456);
                    // У оффера была какая-то стратегия. Так как ее уже нет, то станет 0
                })
        );
        executor.insert(existingOffers);

        var pushOffers = List.of(
                offerPush(SHOP_ID, FEED_ID, "1")
        );
        var pushOffersDbs = List.of(
                offerPush(dbsShopId, dbsFeedId, "2")
        );
        shopOffersProcessor.pushOffers(pushOffers);
        shopOffersProcessor.pushOffers(pushOffersDbs);
    }
}
//CHECKSTYLE:ON
