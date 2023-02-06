package ru.yandex.market.pricelabs.integration.api.events;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.pricelabs.MockMvcProxy;
import ru.yandex.market.pricelabs.api.api.PublicAutostrategiesApi;
import ru.yandex.market.pricelabs.api.api.PublicAutostrategiesApiInterfaces;
import ru.yandex.market.pricelabs.api.search.AutostrategiesSearch;
import ru.yandex.market.pricelabs.integration.api.AbstractAutostrategiesTestsOffersInitialisation;
import ru.yandex.market.pricelabs.model.AutostrategyStateHistory;
import ru.yandex.market.pricelabs.model.types.AutostrategyTarget;
import ru.yandex.market.pricelabs.processing.CoreTables;
import ru.yandex.market.pricelabs.tms.processing.YtScenarioExecutor;
import ru.yandex.market.pricelabs.tms.processing.autostrategies.AbstractAutostrategiesMetaProcessorTest;
import ru.yandex.market.pricelabs.tms.processing.autostrategies.events.EventsGenerator;
import ru.yandex.market.pricelabs.yt.YtConfiguration;
import ru.yandex.market.yt.binding.BindingTable;
import ru.yandex.market.yt.client.YtClientProxy;

@Slf4j
public class AbstractAutostrategiesEventsTests extends AbstractAutostrategiesTestsOffersInitialisation {

    static final int SHOP_WHITE1 = 1;
    static final int SHOP_WHITE2 = 2;

    static final int SHOP_BLUE1 = 11;
    static final int SHOP_BLUE2 = 12;

    static final String WHITE_TARGET = AutostrategyTarget.white.name();

    protected PublicAutostrategiesApiInterfaces publicApi;
    @Autowired
    protected YtConfiguration ytCfg;
    @Autowired
    protected CoreTables coreTables;
    @Qualifier("autostrategeiesEventsGeneratorWhite")
    @Autowired
    protected EventsGenerator eventsGeneratorWhite;
    @Autowired
    protected YtClientProxy ytClient;
    @Autowired
    private PublicAutostrategiesApi publicApiBean;
    @Autowired
    @Qualifier("whiteSearch")
    private AutostrategiesSearch whiteSearch;
    @Autowired
    @Qualifier("blueSearch")
    private AutostrategiesSearch blueSearch;

    @BeforeEach
    void beforeEach() {
        publicApi = MockMvcProxy.buildProxy(PublicAutostrategiesApiInterfaces.class, publicApiBean);

        //Cleanup all tables
        cleanUpTables(coreTables.getAutostrategiesStateHistoryTable(), whiteSearch);
        cleanUpTables(coreTables.getBlueAutostrategiesStateHistoryTable(), blueSearch);
        YtScenarioExecutor.clearTable(ytCfg.getProcessorCfg(coreTables.getAutostrategiesEventTable()));
        YtScenarioExecutor.clearTable(ytCfg.getProcessorCfg(coreTables.getAutostrategiesEventsInstancesTable()));
        YtScenarioExecutor.clearTable(ytCfg.getProcessorCfg(coreTables.getShopWithNoAutostrategiesInstanceTable()));
        //

        var saveOffersWhite = getSaveOffersRunnable(false, SHOP_WHITE1, SHOP_WHITE2, executors.offers());
        var saveOffersBlue = getSaveOffersRunnable(true, SHOP_BLUE1, SHOP_BLUE2, executors.offersBlue());

        testControls.initOnce(getClass(), () ->
                testControls.executeInParallel(
                        saveOffersWhite,
                        saveOffersBlue));
    }

    @AfterEach
    void afterEach() {
        executors.offers().clearTargetTable();
        executors.offersBlue().clearTargetTable();
    }

    private void cleanUpTables(BindingTable<AutostrategyStateHistory> table, AutostrategiesSearch search) {
        AbstractAutostrategiesMetaProcessorTest.cleanupTables(search.getCfg(), search.getHistoryCfg(),
                search.getStateCfg(),
                ytCfg.getProcessorCfg(table),
                search.getFilterCfg(), search.getFilterHistoryCfg(),
                testControls);
    }
}

