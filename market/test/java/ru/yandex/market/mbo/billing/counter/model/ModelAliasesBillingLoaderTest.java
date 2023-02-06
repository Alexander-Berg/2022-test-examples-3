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
public class ModelAliasesBillingLoaderTest extends CustomModelParameterBillingLoaderTest {

    private TarifMultiplicatorService tarifMultiplicatorService;

    @Before
    public void setUp() {
        super.setUp();

        tarifMultiplicatorService = new TarifMultiplicatorService(null, null);
        loader = new ModelAliasesBillingLoader(tarifMultiplicatorService);
        loader.setAuditService(auditService);
        loader.setPaidEntryDao(paidEntryDao);
        loader.setBillingStartDateStr("01-09-2018");
    }

    @Override
    public PaidAction getAddAction() {
        return PaidAction.ADD_MODEL_MODIFICATION_ALIAS;
    }

    @Override
    public PaidAction getDeleteAction() {
        return PaidAction.DELETE_MODEL_MODIFICATION_ALIAS;
    }

    @Override
    public PaidAction getRollbackAction() {
        return PaidAction.MODEL_MODIFICATION_ALIAS_ROLLBACK;
    }

    @Override
    public String getAuditPropertyName() {
        return XslNames.ALIASES;
    }
}
