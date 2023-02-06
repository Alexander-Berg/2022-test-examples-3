package ru.yandex.market.mbo.billing.counter.model;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.tarif.TarifMultiplicatorService;
import ru.yandex.market.mbo.gwt.utils.XslNames;

/**
 * @author yuramalinov
 * @created 03.12.18
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:magicNumber")
public class ModelVendorCodesBillingLoaderTest extends CustomModelParameterBillingLoaderTest {

    private TarifMultiplicatorService tarifMultiplicatorService;

    @Before
    public void setUp() {
        super.setUp();

        tarifMultiplicatorService = new TarifMultiplicatorService(null, null);
        loader = new ModelVendorCodesBillingLoader(tarifMultiplicatorService);
        loader.setAuditService(auditService);
        loader.setPaidEntryDao(paidEntryDao);
        loader.setBillingStartDateStr("01-09-2018");
    }

    @Override
    public PaidAction getAddAction() {
        return PaidAction.ADD_VENDORCODE;
    }

    @Override
    public PaidAction getDeleteAction() {
        return PaidAction.REMOVE_VENDORCODE;
    }

    @Override
    public PaidAction getRollbackAction() {
        return PaidAction.ROLLBACK_VENDORCODE;
    }

    @Override
    public String getAuditPropertyName() {
        return XslNames.VENDOR_CODE;
    }
}
