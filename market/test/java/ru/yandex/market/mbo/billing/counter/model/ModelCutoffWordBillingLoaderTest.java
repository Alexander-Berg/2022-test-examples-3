package ru.yandex.market.mbo.billing.counter.model;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.tarif.TarifMultiplicatorService;
import ru.yandex.market.mbo.gwt.utils.XslNames;

/**
 * @author danfertev
 * @since 07.09.2018
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:magicNumber")
public class ModelCutoffWordBillingLoaderTest extends CustomModelParameterBillingLoaderTest {

    private TarifMultiplicatorService tarifMultiplicatorService;

    @Before
    public void setUp() {
        super.setUp();

        tarifMultiplicatorService = new TarifMultiplicatorService(null, null);
        loader = new ModelCutoffWordBillingLoader(tarifMultiplicatorService);
        loader.setAuditService(auditService);
        loader.setPaidEntryDao(paidEntryDao);
        loader.setBillingStartDateStr("01-09-2018");
    }

    @Override
    public PaidAction getAddAction() {
        return PaidAction.CUT_OFF_WORD;
    }

    @Override
    public PaidAction getDeleteAction() {
        return PaidAction.DELETE_CUT_OFF_WORD;
    }

    @Override
    public PaidAction getRollbackAction() {
        return PaidAction.CUT_OFF_WORD_ROLLBACK;
    }

    @Override
    public String getAuditPropertyName() {
        return XslNames.CUT_OFF_WORD;
    }
}
