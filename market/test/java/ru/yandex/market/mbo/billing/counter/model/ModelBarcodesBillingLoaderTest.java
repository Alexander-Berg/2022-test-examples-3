package ru.yandex.market.mbo.billing.counter.model;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.tarif.TarifMultiplicatorService;
import ru.yandex.market.mbo.gwt.utils.XslNames;

/**
 * @author dmserebr
 * @date 21.06.18
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:magicNumber")
public class ModelBarcodesBillingLoaderTest extends CustomModelParameterBillingLoaderTest {

    private TarifMultiplicatorService tarifMultiplicatorService;

    @Before
    public void setUp() {
        super.setUp();

        tarifMultiplicatorService = new TarifMultiplicatorService(null, null);
        loader = new ModelBarcodesBillingLoader(tarifMultiplicatorService);
        loader.setAuditService(auditService);
        loader.setPaidEntryDao(paidEntryDao);
        loader.setBillingStartDateStr("09-03-2017");
    }

    @Override
    public PaidAction getAddAction() {
        return PaidAction.ADD_BARCODE;
    }

    @Override
    public PaidAction getDeleteAction() {
        return PaidAction.REMOVE_BARCODE;
    }

    @Override
    public PaidAction getRollbackAction() {
        return PaidAction.ROLLBACK_BARCODE;
    }

    @Override
    public String getAuditPropertyName() {
        return XslNames.BAR_CODE;
    }
}
