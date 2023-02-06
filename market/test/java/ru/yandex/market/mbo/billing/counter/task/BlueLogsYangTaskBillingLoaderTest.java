package ru.yandex.market.mbo.billing.counter.task;

import org.junit.Before;
import ru.yandex.market.mbo.billing.tarif.TarifMultiplicatorService;

public class BlueLogsYangTaskBillingLoaderTest extends AbstractYangTaskBillingLoaderTest {

    private BlueLogsYangTaskBillingLoader loader;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        TarifMultiplicatorService tarifMultiplicatorService =
            new TarifMultiplicatorService(guruCategoryService, parameterLoaderService);
        loader = new BlueLogsYangTaskBillingLoader(statisticsService, tarifMultiplicatorService);
        loader.setEnabled(true);
    }

    @Override
    protected AbstractYangTaskBillingLoader getLoader() {
        return loader;
    }
}
