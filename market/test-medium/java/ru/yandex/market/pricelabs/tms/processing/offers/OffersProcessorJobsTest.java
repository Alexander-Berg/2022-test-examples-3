package ru.yandex.market.pricelabs.tms.processing.offers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.pricelabs.CoreTestUtils;
import ru.yandex.market.pricelabs.apis.ApiConst;
import ru.yandex.market.pricelabs.apis.LogOutput;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyFilter;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyFilterSimple;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySave;
import ru.yandex.market.pricelabs.misc.TimingUtils;
import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.pricelabs.model.NewShopsDat;
import ru.yandex.market.pricelabs.model.Shop;
import ru.yandex.market.pricelabs.model.VendorDatasource;
import ru.yandex.market.pricelabs.model.types.ShopStatus;
import ru.yandex.market.pricelabs.model.types.ShopType;
import ru.yandex.market.pricelabs.services.database.model.JobType;
import ru.yandex.market.pricelabs.services.database.model.Task;
import ru.yandex.market.pricelabs.services.database.model.TaskStatus;
import ru.yandex.market.pricelabs.tms.processing.TmsTestUtils;
import ru.yandex.market.pricelabs.tms.processing.autostrategies.AbstractAutostrategiesMetaProcessorTest;
import ru.yandex.market.pricelabs.tms.processing.imports.VendorDatasourcesProcessor;
import ru.yandex.misc.lang.number.UnsignedLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.autostrategyOfferSource;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.shopCategory;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.update;
import static ru.yandex.market.pricelabs.tms.processing.offers.OffersCheckers.vendorAutostrategy;

class OffersProcessorJobsTest extends AbstractOffersProcessorTest {

    static Object[] fullShopImported() {
        return new Object[][]{
                {"Обновлялся в течение 3 дней", (Consumer<Shop>) shop1 -> {
                    // Попадает в диапазон, за который мы грузим магазина
                    shop1.setUpdated_at(getMbiUpdatedAt().plusMillis(1));
                }
                },
        };
    }

    @Test
    void testFullJobWithSyncAutostrategiesDsbs() throws InvalidProtocolBufferException {
        testControls.resetShops();
        shopsDatExecutor.insertSource(List.of(
                new NewShopsDat(SHOP_ID, FEED_ID, BUSINESS_ID, "shop1", "shop1", "RUR", 225, 213, true, false, true)
        ));
        long autostrategyBid = 200L;
        var settings = autostrategySettings(autostrategyBid);
        processor.setBatchSize(1000);

        final long minPrice = 1000000L;

        var auto1 = new AutostrategySave()
                .name("Test1")
                .enabled(true)
                .filter(new AutostrategyFilter()
                        .type(AutostrategyFilter.TypeEnum.SIMPLE)
                        .simple(new AutostrategyFilterSimple().priceFrom(minPrice)))
                .settings(settings);

        int id1 = metaProcessor.create(UID, SHOP_ID, auto1);

        Instant now = getInstant();
        var expectRows = update(executor.asCreated(readActiveOffers()), offer -> {
            offer.setVendor_strategy_updated_at(now);
        });

        doFullJob();
        var task = startScheduledTask(JobType.SHOP_LOOP_FULL);
        checkNoScheduledTasks();

        var expect = expectBuilder()
                .action(() -> {
                    controller.processTask(task);
                })
                .newRows(readSourceList())
                .expectRows(expectRows)
                .response(LogOutput.FeedStateConst.OK);

        this.test(expect.build());

        mockWebServerAmore.checkNoMessages();
    }
    //CHECKSTYLE:ON

    @Test
    void testJobNothingToSchedule() {
        executor.removeSourceTables();

        this.doFullJob();
        checkNoScheduledTasks();

        // Нечего делать
        this.doFullJob();
        checkNoScheduledTasks();

        CoreTestUtils.compare(List.of(), testControls.getShopTasks(SHOP_ID));
    }

    //CHECKSTYLE:OFF
    @Test
    void testFullJobWithSyncOffersAndVendorsBlue() {
        Instant now = getInstant();

        var model1 = 1;
        var model2 = 2;
        var model3 = 3;
        var model4 = 4;
        var model5 = 5;
        var modelDisabled = 6;

        var expectModelId = Map.of(
                "4179", model1,
                "4335", model2,
                "4370", model3,
                "4371", model4,
                "4584", model5,
                "4105", modelDisabled
        );

        var categoryMap = Map.of(
                "4179", 53,
                "4335", 53,
                "4370", 53,
                "4371", 68,
                "4584", 68,
                "4105", 88
        );

        var sourceRows = readSourceList()
                .stream()
                .map(o -> {
                    o.setShop_id(UnsignedLong.valueOf(ApiConst.VIRTUAL_SHOP_BLUE));
                    o.setSupplier_id(UnsignedLong.valueOf(SHOP_ID));
                    o.setModel_id(expectModelId.getOrDefault(o.getOffer_id(), 0));
                    o.setCategory_id(categoryMap.getOrDefault(o.getOffer_id(), 0));
                    o.setModel_published_on_market(true);
                    return o;
                })
                .collect(Collectors.toList());

        testControls.executeInParallel(
                () -> {
                    executor.clearSourceTable();
                    executor.insertSource(sourceRows);
                },
                () -> shopsDatExecutor.insertSource(List.of(
                        new NewShopsDat(SHOP_ID, FEED_ID, BUSINESS_ID, "shop1", "shop1", "RUR", 225, 213, false, true,
                                true)
                )),
                () -> executorBlue.clearTargetTable(),
                () -> {
                    testControls.resetShops();
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
        var vidDisabled = metaProcessorVendorBlue.create(UID, VENDOR_ID2,
                vendorAutostrategy(999, List.of(), List.of(modelDisabled)).enabled(false));

        vendorDatasourceExecutor.insert(List.of(
                new VendorDatasource(VendorDatasourcesProcessor.PRODUCT_ID, VENDOR_ID, DATASOURCE_ID,
                        getInstant()),
                new VendorDatasource(VendorDatasourcesProcessor.PRODUCT_ID, VENDOR_ID2, DATASOURCE_ID2,
                        getInstant())));

        var expectVendorAutostrategyMap = Map.of(
                "4179", vid1x,
                "4335", vid2xx,
                "4370", vid3,
                "4371", vid5,
                "4584", vid5,
                "4105", vidDisabled);

        var expectRows = update(executorBlue.asCreated(readActiveOffers()), offer -> {
            offer.setMarket_category_id(categoryMap.getOrDefault(offer.getOffer_id(), 0));
            offer.setCategory_id(offer.getMarket_category_id());
            offer.setModel_id(expectModelId.getOrDefault(offer.getOffer_id(), 0));
            offer.setApp_vendor_autostrategy_id(expectVendorAutostrategyMap.getOrDefault(offer.getOffer_id(), 0));
            offer.setVendor_strategy_updated_at(now);
            offer.setStrategy_updated_at(Instant.ofEpochMilli(0));
            offer.setSsku_offer(true);

            if (offer.getApp_vendor_autostrategy_id() > 0) {
                offer.setVendor_strategy_modified_at(now);
            }

            offer.normalizeFields();
        });

        args.setType(ShopType.SUPPLIER);

        doFullJob();
        var task = startScheduledTask(JobType.SHOP_LOOP_FULL_LARGE);
        checkNoScheduledTasks();
        controller.processTask(task);

        var virtualShopId = ApiConst.VIRTUAL_SHOP_BLUE;
        var virtualFeedId = ApiConst.VIRTUAL_FEED_BLUE;
        var supplierShopId = SHOP_ID;
        var supplierFeedId = FEED_ID;

        testControls.executeInParallel(
                () -> executorBlue.verify(expectRows, OffersCheckers::resetWareAndCurrency),
                () -> blueCategoriesExecutor.verify(List.of(
                        shopCategory(SHOP_ID, virtualFeedId, 21, -1, "LCD ТВ", 1),
                        shopCategory(SHOP_ID, virtualFeedId, 27, -1, "Портативная техника", 5),
                        shopCategory(SHOP_ID, virtualFeedId, 36, 27, "DVD проигрыватели", 5),
                        shopCategory(SHOP_ID, virtualFeedId, 53, 36, "BBK", 3),
                        shopCategory(SHOP_ID, virtualFeedId, 68, 36, "Panasonic", 2),
                        shopCategory(SHOP_ID, virtualFeedId, 88, 21, "SONY", 1)
                ), c -> {
                    c.setTree_left(0);
                    c.setTree_right(0);
                    c.setTree_lvl(0);
                    c.setChildren_count(0);
                    c.setCurrent_offer_count(0);
                }),
                () -> autostrategyOffersExecutorVendorSource.verify(List.of(
                        autostrategyOfferSource(virtualShopId, virtualFeedId, supplierFeedId + ".4105", VENDOR_ID2,
                                vidDisabled, DATASOURCE_ID2, now, BUSINESS_ID, modelDisabled, 0, o -> {
                                    o.setShop_id(supplierShopId);
                                    o.setFeed_id(supplierFeedId);
                                    o.setOffer_id("4105");
                                    o.setJob_id(task.getJob_id());
                                    o.setTask_id(task.getTask_id());
                                }),
                        autostrategyOfferSource(virtualShopId, virtualFeedId, supplierFeedId + ".4179", VENDOR_ID2,
                                vid1x, DATASOURCE_ID2, now, BUSINESS_ID, model1, 110, o -> {
                                    o.setShop_id(supplierShopId);
                                    o.setFeed_id(supplierFeedId);
                                    o.setOffer_id("4179");
                                    o.setJob_id(task.getJob_id());
                                    o.setTask_id(task.getTask_id());
                                }),
                        autostrategyOfferSource(virtualShopId, virtualFeedId, supplierFeedId + ".4335", VENDOR_ID,
                                vid2xx, DATASOURCE_ID, now, BUSINESS_ID, model2, 106, o -> {
                                    o.setShop_id(supplierShopId);
                                    o.setFeed_id(supplierFeedId);
                                    o.setOffer_id("4335");
                                    o.setJob_id(task.getJob_id());
                                    o.setTask_id(task.getTask_id());
                                }),
                        autostrategyOfferSource(virtualShopId, virtualFeedId, supplierFeedId + ".4370", VENDOR_ID2,
                                vid3, DATASOURCE_ID2, now, BUSINESS_ID, model3, 105, o -> {
                                    o.setShop_id(supplierShopId);
                                    o.setFeed_id(supplierFeedId);
                                    o.setOffer_id("4370");
                                    o.setJob_id(task.getJob_id());
                                    o.setTask_id(task.getTask_id());
                                }),
                        autostrategyOfferSource(virtualShopId, virtualFeedId, supplierFeedId + ".4371", VENDOR_ID,
                                vid5, DATASOURCE_ID, now, BUSINESS_ID, model4, 104, o -> {
                                    o.setShop_id(supplierShopId);
                                    o.setFeed_id(supplierFeedId);
                                    o.setOffer_id("4371");
                                    o.setJob_id(task.getJob_id());
                                    o.setTask_id(task.getTask_id());
                                }),
                        autostrategyOfferSource(virtualShopId, virtualFeedId, supplierFeedId + ".4584", VENDOR_ID,
                                vid5, DATASOURCE_ID, now, BUSINESS_ID, model5, 104, o -> {
                                    o.setShop_id(supplierShopId);
                                    o.setFeed_id(supplierFeedId);
                                    o.setOffer_id("4584");
                                    o.setJob_id(task.getJob_id());
                                    o.setTask_id(task.getTask_id());
                                })))
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("fullShopImported")
    void testFullJobScheduled(String description, Consumer<Shop> shopInit) {
        executor.insert(readSourceList(), List.of());

        shopInit.accept(shop);
        testControls.saveShop(shop);
        executor.insert(List.of(), List.of());

        this.doFullJob();
        startScheduledTask(JobType.SHOP_LOOP_FULL);
        checkNoScheduledTasks();
    }

    @Test
    void testFullJobShopsWithStrategies() {
        var newShop = initShop(9999, Utils.emptyConsumer());
        testControls.saveShop(newShop);

        executor.insert(List.of(), List.of());

        this.doFullJob();
        var taskShops = testControls.startScheduledTasks(2, JobType.SHOP_LOOP_FULL).stream()
                .map(Task::getShop_id)
                .collect(Collectors.toSet());
        assertEquals(Set.of((int) newShop.getShop_id(), SHOP_ID), taskShops);

        checkNoScheduledTasks();
    }

    @Test
    void testFullJobNotScheduled() {
        executor.insert(readSourceList(), List.of());

        shop.setUpdated_at(getInstant().minus(ApiConst.ALIVE_SHOP_DAYS, ChronoUnit.DAYS));
        shop.setStatus(ShopStatus.INACTIVE);
        testControls.saveShop(shop);
        executor.insert(List.of(), List.of());

        this.doFullJob();
        checkNoScheduledTasks();
    }

    @Test
    void testJobSimpleSchedule() {
        shop.setFeeds(Set.of((long) 100, (long) 500, (long) FEED_ID));
        testControls.saveShop(shop);
        executor.insert(List.of(), List.of());

        this.doFullJob();
        var item = startScheduledTask(JobType.SHOP_LOOP_FULL);
        checkNoScheduledTasks();

        OffersArg arg = TmsTestUtils.defaultOffersArg(shop.getShop_id()).setCluster(testControls.getCurrentCluster());
        assertEquals(arg.toJsonString(), item.getArgs());
        controller.processTask(item);

        checkTaskResult(TaskStatus.SUCCESS, LogOutput.OK);

        testControls.taskMonitoringJob();

        // Повторно задача не будет запланирована (т.е. таблица с офферами не изменилась)
        this.doFullJob();
        checkNoScheduledTasks();
    }

    @Test
    void testJobSimpleScheduleNoSecondTryAnyway() {
        executor.insert(List.of(), List.of());

        this.doFullJob();

        Task item1 = startScheduledTask(JobType.SHOP_LOOP_FULL);
        assertEquals(args.toJsonString(), item1.getArgs());
        testControls.completeTaskSuccess(item1);

        checkTaskResult(TaskStatus.SUCCESS, "Force close");

        testControls.taskMonitoringJob();

        // Не обработали запрос, но второй попытки запланировать задачу не будет
        this.doFullJob();
        checkNoScheduledTasks();
    }

    @Test
    void testJobNoScheduleAfterTableChange() {
        executor.insert(List.of(), List.of());
        this.doFullJob();

        controller.processTask(startScheduledTask(JobType.SHOP_LOOP_FULL));
        checkNoScheduledTasks();

        testControls.taskMonitoringJob();

        executor.removeSourceTables();
        executor.insert(List.of(), List.of());
        this.doFullJob();

        // Задача не будет запланирована, т.к. мы игнорируем любые изменения в поколениях
        checkNoScheduledTasks();
    }

    @Test
    void testJobFullLarge() {
        testControls.resetShops();
        saveAsVirtualShop();

        executor.insertSource(
                readSourceList()
                        .stream()
                        .map(o -> {
                            o.setShop_id(UnsignedLong.valueOf(ApiConst.VIRTUAL_SHOP_BLUE));
                            o.setSupplier_id(UnsignedLong.valueOf(SHOP_ID));
                            return o;
                        })
                        .collect(Collectors.toList())
        );
        executorBlue.insert(
                readActiveOffers()
                        .stream()
                        .map(o -> {
                            o.setApp_strategy_id(999L);
                            o.setApp_autostrategy_id(999);
                            o.setApp_vendor_autostrategy_id(999);
                            o.setBid(999L);
                            return o;
                        })
                        .collect(Collectors.toList())
        );

        this.doFullJob();
        var t = startScheduledTask(JobType.SHOP_LOOP_FULL_LARGE);
        controller.processTask(t);

        testControls.taskMonitoringJob();
        checkTaskResult(t.getTask_id(), TaskStatus.SUCCESS, LogOutput.OK);
        testControls.checkNoScheduledTasks();

        testControls.taskMonitoringJob();

        testControls.taskMonitoringJob();
        testControls.checkNoScheduledTasks();
    }

    private static Instant getMbiUpdatedAt() {
        return TimingUtils.getInstant().minus(ApiConst.ALIVE_SHOP_DAYS, ChronoUnit.DAYS);
    }

    @Test
    void testJobSimpleLargeSchedule() {
        testControls.executeInParallel(
                () -> {
                    testControls.resetShops();
                    saveAsVirtualShop();
                },
                () -> {
                    marketCategoriesExecutor.insertSource(categoriesProcessorTest.readMarketCategories());
                });

        executor.insert(List.of(), List.of());

        this.doFullJob();
        var item = startScheduledTask(JobType.SHOP_LOOP_FULL_LARGE);
        checkNoScheduledTasks();

        OffersArg arg = args
                .setShopId(ApiConst.VIRTUAL_SHOP_BLUE)
                .setType(ShopType.SUPPLIER);
        assertEquals(arg.toJsonString(), item.getArgs());
        controller.processTask(item);

        checkTaskResult(ApiConst.VIRTUAL_SHOP_BLUE, TaskStatus.SUCCESS, LogOutput.OK);

        // Повторно задача не будет запланирована
        this.doFullJob();
        checkNoScheduledTasks();
    }
}
