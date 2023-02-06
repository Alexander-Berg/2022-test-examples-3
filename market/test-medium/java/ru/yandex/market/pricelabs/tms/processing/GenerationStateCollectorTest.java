package ru.yandex.market.pricelabs.tms.processing;

import javax.annotation.Nullable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.model.NewOfferGen;
import ru.yandex.market.pricelabs.model.NewShopCategory;
import ru.yandex.market.pricelabs.model.Offer;
import ru.yandex.market.pricelabs.model.ShopCategory;
import ru.yandex.market.pricelabs.tms.AbstractTmsSpringConfiguration;
import ru.yandex.market.pricelabs.tms.processing.GenerationStateCollector.NewGenerationState;
import ru.yandex.market.yt.YtClusters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class GenerationStateCollectorTest extends AbstractTmsSpringConfiguration {

    @Autowired
    private GenerationStateCollector details;

    @Autowired
    private YtClusters clusters;

    private YtSourceTargetScenarioExecutor<NewShopCategory, ShopCategory> categoriesExecutor;
    private YtSourceTargetScenarioExecutor<NewOfferGen, Offer> offersGenExecutor;

    @BeforeEach
    void init() {
        this.categoriesExecutor = executors.categories();
        this.offersGenExecutor = executors.offersGen();

        testControls.executeInParallel(
                () -> testControls.cleanupTableRevisions(),
                () -> offersGenExecutor.removeSourceTables(),
                () -> categoriesExecutor.removeSourceTables()
        );
    }

    @Test
    void testCollectStatesNoData() {
        checkNoState();
    }

    @Test
    void testCollectNoOffers() {
        createCategoriesTable("20190101_0000");
        checkNoState();
    }

    @Test
    void testCollectNoCategories() {
        checkNoState();
    }

    @Test
    void testNoGeneration() {
        testControls.executeInParallel(
                () -> createCategoriesTable("20190101_0000")
        );
        checkNoState();
    }

    @Test
    void testCollectData() {
        testControls.executeInParallel(
                () -> createOffersGenTable("20190101_0000"),
                () -> createCategoriesTable("20190101_0000")
        );

        checkState("20190101_0000", "20190101_0000", true);
    }

    @Test
    void testCollectDataUnprocessed() {
        testControls.executeInParallel(
                () -> createOffersGenTable("20190101_0000"),
                () -> createCategoriesTable("20190101_0000")
        );
        checkState("20190101_0000", "20190101_0000", true);

        // Не подтвердили обработку? Снова ловим те же самые таблицы
        checkState("20190101_0000", "20190101_0000", true);
    }

    @Test
    void testCollectDataFromMultipleOptions() {
        testControls.executeInParallel(
                () -> createOffersGenTable("20190101_0000"),
                () -> createOffersGenTable("20190102_0000"),
                () -> createCategoriesTable("20190101_0000"),
                () -> createCategoriesTable("20190102_0000")
        );
        checkState("20190102_0000", "20190102_0000", true);
    }

    @Test
    void testCollectDataFromUnmatchedOptions() {
        testControls.executeInParallel(
                () -> createOffersGenTable("20190101_0000"),
                () -> createCategoriesTable("20190101_0000"),
                () -> createCategoriesTable("20190102_0000"),
                () -> createCategoriesTable("20190103_0000")
        );
        checkState("20190101_0000", "20190101_0000", true);
    }

    @Test
    void testCollectDataFromUnmatchedOptions2() {
        testControls.executeInParallel(
                () -> createOffersGenTable("20190101_0000"),
                () -> createCategoriesTable("20190101_0000")
        );
        checkState("20190101_0000", "20190101_0000", true);
    }

    @Test
    void testCollectDataAfterConfirmed() {
        testControls.executeInParallel(
                () -> createOffersGenTable("20190101_0000"),
                () -> createOffersGenTable("20190102_0000"),
                () -> createCategoriesTable("20190101_0000"),
                () -> createCategoriesTable("20190102_0000")
        );
        checkState("20190102_0000", "20190102_0000", true).confirmProcessed();

        checkState("20190102_0000", "20190102_0000", false);

        checkState("20190102_0000", "20190102_0000", false).confirmProcessed();

        checkState("20190102_0000", "20190102_0000", false);

        testControls.executeInParallel(() -> createOffersGenTable("20190103_0000"));
        // категории отстали, берем последнюю существующую
        checkState("20190103_0000", "20190102_0000", true).confirmProcessed();

        checkState("20190103_0000", "20190102_0000", false);
    }

    @Test
    void testCollectDataWithReference() {
        testControls.executeInParallel(
                () -> {
                    createOffersGenTable("20190101_0000");
                    offersGenExecutor.makeLink("20190101_0000", "recent");
                },
                () -> createCategoriesTable("20190101_0000")
        );

        checkState("20190101_0000", "20190101_0000", true);
    }

    @Test
    void testCollectDataWithOtherTables() {
        testControls.executeInParallel(
                () -> createOffersGenTable("1sample"),
                () -> createOffersGenTable("20190101_0000"),
                () -> createOffersGenTable("3sample"),
                () -> createCategoriesTable("20190101_0000")
        );

        checkState("20190101_0000", "20190101_0000", true);
    }

    private void createCategoriesTable(String table) {
        categoriesExecutor.createSourceTable(table);
    }

    private void createOffersGenTable(String table) {
        offersGenExecutor.createSourceTable(table);
    }

    private void checkNoState() {
        @Nullable var state = details.getNewGenerationState();
        assertNull(state);
    }

    private NewGenerationState checkState(String generationTable, String categoriesTable,
                                          boolean needProcessingGenOffers) {
        @Nullable var state = details.getNewGenerationState();
        assertNotNull(state);
        assertEquals(needProcessingGenOffers, state.isNeedProcessingGenOffers());

        assertEquals(generationTable, state.getGenOffersState().getTableName());
        assertEquals(categoriesTable, state.getCategoriesState().getTableName());
        return state;
    }

}
