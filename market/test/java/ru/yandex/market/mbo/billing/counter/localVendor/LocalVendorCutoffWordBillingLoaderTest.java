package ru.yandex.market.mbo.billing.counter.localVendor;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.counter.AbstractFIllCustomWithRollbackBillingLoaderTest;
import ru.yandex.market.mbo.billing.counter.base.AbstractBillingLoader;
import ru.yandex.market.mbo.catalogue.CategoryMatcherParamService;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;

/**
 * @author danfertev
 * @since 23.10.2018
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:magicNumber")
public class LocalVendorCutoffWordBillingLoaderTest extends AbstractFIllCustomWithRollbackBillingLoaderTest {
    protected AbstractBillingLoader loader;

    @Before
    public void setUp() {
        super.setUp();

        loader = new LocalVendorCutoffBillingLoader();
        loader.setAuditService(auditService);
        loader.setPaidEntryDao(paidEntryDao);
        loader.setBillingStartDateStr("20-10-2018");
    }

    @Override
    protected AuditAction.EntityType getEntityType() {
        return AuditAction.EntityType.OPTION;
    }

    @Override
    public Long getParameterId() {
        return (long) KnownIds.VENDOR_PARAM_ID;
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
    public AbstractBillingLoader getLoader() {
        return loader;
    }

    @Override
    public String getAuditPropertyName() {
        return CategoryMatcherParamService.CUT_OFF_WORD;
    }
}
