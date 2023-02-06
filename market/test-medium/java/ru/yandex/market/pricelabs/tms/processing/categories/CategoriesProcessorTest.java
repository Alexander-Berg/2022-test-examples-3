package ru.yandex.market.pricelabs.tms.processing.categories;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.apis.ApiConst;
import ru.yandex.market.pricelabs.bindings.csv.CSVMapper;
import ru.yandex.market.pricelabs.model.MarketCategory;
import ru.yandex.market.pricelabs.model.NewShopCategory;
import ru.yandex.market.pricelabs.model.ShopCategory;
import ru.yandex.market.pricelabs.model.types.ShopType;
import ru.yandex.market.pricelabs.tms.processing.AbstractSourceTargetProcessorConfiguration;
import ru.yandex.market.pricelabs.tms.processing.TaskInfo;
import ru.yandex.market.pricelabs.tms.processing.TmsTestUtils;
import ru.yandex.market.pricelabs.tms.processing.YtSourceTargetScenarioExecutor;
import ru.yandex.market.pricelabs.tms.processing.offers.ProcessingContext;
import ru.yandex.market.pricelabs.tms.processing.offers.ShopLoopShopState;
import ru.yandex.market.yt.binding.YTBinder;

import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.offer;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.shop;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.update;

public class CategoriesProcessorTest extends
        AbstractSourceTargetProcessorConfiguration<NewShopCategory, ShopCategory> {

    private final CSVMapper<MarketCategory> marketCategoryMapper =
            CSVMapper.mapper(YTBinder.getBinder(MarketCategory.class));

    private ProcessingContext ctx;

    @Autowired
    private CategoriesProcessor processor;

    private YtSourceTargetScenarioExecutor<NewShopCategory, ShopCategory> executor;
    private YtSourceTargetScenarioExecutor<MarketCategory, ShopCategory> marketExecutor;

    @BeforeEach
    void init() {
        this.initCtx(2289);
        this.executor = executors.categories();
        this.marketExecutor = executors.marketCategories();

        marketExecutor.clearTargetTable();
    }

    @Test
    void addCategories_Empty() {
        this.test(List.of(), List.of(), List.of());
    }

    @Test
    void addCategories6_NoExistingData() {
        this.test(readSourceList(), List.of(), executor.asCreated(readTargetList()));
    }

    @Test
    void addCategories6_UpdateAll() {
        this.test(readSourceList(), readTargetList(), executor.asUpdated(readTargetList()));
    }

    @Test
    void addCategories6_NoNewData() {
        this.test(List.of(), readTargetList(), executor.asDeleted(readTargetList()));
    }

    @Test
    void addCategories6_NoNewDataNoExistingData() {
        this.test(List.of(), executor.asDeleted(readTargetList()), executor.asDeleted(readTargetList()));
    }

    @Test
    void addCategories6_MixedNewAndExistingData() {

        // Добавляем первые 4 строчки
        // Существуют все записи кроме первых 2
        // Проверяем, что все записи кроме первых 4 удалены

        var target = readTargetList();

        List<ShopCategory> expect = new ArrayList<>();
        expect.addAll(executor.asCreated(target.subList(0, 2)));
        expect.addAll(executor.asUpdated(target.subList(2, 4)));
        changeTree(expect.get(0), 1, 2);
        changeTree(expect.get(1), 3, 8);
        changeTree(expect.get(2), 4, 5);
        changeTree(expect.get(3), 6, 7);
        expect.addAll(executor.asDeleted(target.subList(4, target.size())));

        this.test(readSourceList().subList(0, 4), readTargetList().subList(2, target.size()), expect);
    }


    @Test
    void addMarketCategories_Empty() {
        initCtx(ApiConst.VIRTUAL_SHOP_BLUE, ShopType.SUPPLIER);
        this.testMarket(List.of(), List.of(), List.of());
    }

    @Test
    void addMarketCategories6_NoExistingData() {
        initCtx(ApiConst.VIRTUAL_SHOP_BLUE, ShopType.SUPPLIER);
        this.testMarket(readMarketCategories(), List.of(), executor.asCreated(readTargetList()));
    }

    @Test
    void addMarketCategories6_UpdateAll() {
        initCtx(ApiConst.VIRTUAL_SHOP_BLUE, ShopType.SUPPLIER);
        this.testMarket(readMarketCategories(), readTargetList(), executor.asUpdated(readTargetList()));
    }

    @Test
    void addMarketCategories6_NoNewData() {
        initCtx(ApiConst.VIRTUAL_SHOP_BLUE, ShopType.SUPPLIER);
        this.testMarket(List.of(), readTargetList(), executor.asDeleted(readTargetList()));
    }

    @Test
    void addMarketCategories6_NoNewDataNoExistingData() {
        initCtx(ApiConst.VIRTUAL_SHOP_BLUE, ShopType.SUPPLIER);
        this.testMarket(List.of(), executor.asDeleted(readTargetList()), executor.asDeleted(readTargetList()));
    }

    @Test
    void addMarketCategories6_MixedNewAndExistingData() {
        initCtx(ApiConst.VIRTUAL_SHOP_BLUE, ShopType.SUPPLIER);

        // Добавляем первые 4 строчки
        // Существуют все записи кроме первых 2
        // Проверяем, что все записи кроме первых 4 удалены

        var target = readTargetList();

        List<ShopCategory> expect = new ArrayList<>();
        expect.addAll(executor.asCreated(target.subList(0, 2)));
        expect.addAll(executor.asUpdated(target.subList(2, 4)));
        changeTree(expect.get(0), 1, 2);
        changeTree(expect.get(1), 3, 8);
        changeTree(expect.get(2), 4, 5);
        changeTree(expect.get(3), 6, 7);
        expect.addAll(executor.asDeleted(target.subList(4, target.size())));

        this.testMarket(readMarketCategories().subList(0, 4), readTargetList().subList(2, target.size()), expect);
    }


    @Override
    protected String getSourceCsv() {
        return "tms/processing/categories/categories_source.csv";
    }

    @Override
    protected String getTargetCsv() {
        return "tms/processing/categories/categories_target.csv";
    }

    @Override
    protected Class<NewShopCategory> getSourceClass() {
        return NewShopCategory.class;
    }

    @Override
    protected Class<ShopCategory> getTargetClass() {
        return ShopCategory.class;
    }

    @Override
    protected Consumer<ShopCategory> getTargetUpdate() {
        var catCleanup = cleanup();
        return cat -> {
            catCleanup.accept(cat);
            cat.normalizeIndexFields();
        };
    }

    private Consumer<ShopCategory> cleanup() {
        return cat -> {
            // Не можем посчитать без перерасчета всех офферов - так что просто игнорируем
            cat.setOffer_count(0);
            cat.setCurrent_offer_count(0);
            cat.setChildren_count(0);
        };
    }

    public List<MarketCategory> readMarketCategories() {
        return readCsv(marketCategoryMapper, "tms/processing/categories/market_categories_source.csv");
    }

    public List<ShopCategory> readTargetListRaw() {
        return readTargetListRaw(getTargetCsv());
    }

    public List<ShopCategory> readTargetListRaw(String resource) {
        return update(readCsv(getTargetMapper(), resource), ShopCategory::normalizeIndexFields);
    }

    private void testMarket(List<MarketCategory> newRows, List<ShopCategory> existingRows,
                            List<ShopCategory> expectRows) {
        marketExecutor.test(() -> {
                    CategoriesTreeHolder holder = processor.getCategoriesSync(ctx);
                    newRows.forEach(c -> {
                        holder.add(offer("1", o -> o.setCategory_id(c.getId())));
                    });
                    holder.flush();
                },
                newRows, update(existingRows, this::mapMarket), update(expectRows, this::mapMarket), cleanup());
    }

    private void map(ShopCategory cat) {
        cat.setShop_id((int) ctx.getShopId());
    }

    private void mapMarket(ShopCategory cat) {
        cat.setShop_id((int) ctx.getShopId());
        cat.setFeed_id(ApiConst.VIRTUAL_FEED_BLUE);
    }

    private void test(List<NewShopCategory> newRows, List<ShopCategory> existingRows, List<ShopCategory> expectRows) {
        executor.test(() -> processor.getCategoriesSync(ctx).flush(),
                newRows, existingRows, expectRows, cleanup());
    }

    private void initCtx(int shopId) {
        initCtx(shopId, ShopType.DSBS);
    }

    private void initCtx(int shopId, ShopType shopType) {
        this.ctx = new ProcessingContext(
                TmsTestUtils.defaultOffersArg(shopId).setCluster(testControls.getCurrentCluster()).setType(shopType),
                shop(shopId, s -> s.setType(shopType)),
                getInstant(),
                3393,
                new ShopLoopShopState(),
                TaskInfo.UNKNOWN
        );
    }

    //

    private static void changeTree(ShopCategory category, int treeLeft, int treeRight) {
        category.setTree_left(treeLeft);
        category.setTree_right(treeRight);
    }


}
