package ru.yandex.market.mbo.billing.counter.model;

import ru.yandex.market.mbo.billing.counter.AbstractFIllCustomWithRollbackBillingLoaderTest;
import ru.yandex.market.mbo.billing.counter.base.AbstractBillingLoader;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;

/**
 * @author danfertev
 * @since 07.09.2018
 */
@SuppressWarnings("checkstyle:magicNumber")
public abstract class CustomModelParameterBillingLoaderTest extends AbstractFIllCustomWithRollbackBillingLoaderTest {
    private static final Long PARAM_ID = 123L;

    protected AbstractModelParametersBillingLoader loader;

    @Override
    public AbstractBillingLoader getLoader() {
        return loader;
    }

    @Override
    protected AuditAction.EntityType getEntityType() {
        return AuditAction.EntityType.MODEL_PARAM;
    }

    @Override
    public Long getParameterId() {
        return PARAM_ID;
    }
}
