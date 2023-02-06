package ru.yandex.market.mbo.billing.counter.task;

import org.junit.Before;
import ru.yandex.market.mbo.billing.tarif.TarifMultiplicatorService;

public class FillSkuYangTaskBillingLoaderTest extends AbstractYangTaskBillingLoaderTest {

    private FillSkuYangTaskBillingLoader loader;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        TarifMultiplicatorService tarifMultiplicatorService =
            new TarifMultiplicatorService(guruCategoryService, parameterLoaderService);
        loader = new FillSkuYangTaskBillingLoader(statisticsService, tarifMultiplicatorService);
        loader.setEnabled(true);
    }

    @Override
    protected FillSkuYangTaskBillingLoader getLoader() {
        return loader;
    }
}
