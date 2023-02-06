package ru.yandex.market.mbo.billing.counter.model;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.action.BillingAction;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author anmalysh
 */
@RunWith(MockitoJUnitRunner.class)
public class ModelPickerBillingLoaderTest extends ModelPictureBillingLoaderTestBase {

    @Before
    public void setUp() {
        super.setUp();

        loader = new ModelPickerBillingLoader();
        loader.setAuditService(auditService);
        loader.setBillingStartDateStr("09-03-2017");
    }

    @Override
    protected AuditAction.EntityType getEntityType() {
        return AuditAction.EntityType.MODEL_PICKER;
    }

    @Override
    protected PaidAction getAddPaidAction() {
        return PaidAction.ADD_MODEL_PICKER;
    }

    @Override
    protected PaidAction getDeletePaidAction() {
        return PaidAction.DELETE_MODEL_PICKER;
    }

    @Override
    protected PaidAction getCopyPaidAction() {
        return PaidAction.COPY_MODEL_PICKER;
    }

    @Test
    @Override
    public void testBulkCopy() {
        auditService.writeActions(auditActionsBulkCopy);

        List<BillingAction> billingActions = loader.loadBillingActions(provider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(auditActionsBulkCopy.get(0), getCopyPaidAction()),
            createBillingAction(auditActionsBulkCopy.get(1), getCopyPaidAction())
        );
    }
}
