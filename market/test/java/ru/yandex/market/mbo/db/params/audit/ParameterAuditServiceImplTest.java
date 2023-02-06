package ru.yandex.market.mbo.db.params.audit;


import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.core.audit.AuditService;
import ru.yandex.market.mbo.db.VisualServiceAudit;
import ru.yandex.market.mbo.db.params.ParameterSaveContext;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Collection;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

/**
 * @author danfertev
 * @since 12.02.2019
 */
@SuppressWarnings("checkstyle:MagicNumber")
@RunWith(MockitoJUnitRunner.class)
public class ParameterAuditServiceImplTest {
    private static final long UID = 999L;

    @Mock
    private AuditService auditService;
    @Mock
    private VisualServiceAudit visualServiceAudit;
    @InjectMocks
    private ParameterAuditServiceImpl parameterAuditService = new ParameterAuditServiceImpl();

    @Captor
    private ArgumentCaptor<Collection<AuditAction>> actionCaptor;
    @Captor
    private ArgumentCaptor<Boolean> billedOperationCaptor;


    private CategoryParam param;
    private CategoryParam vendorParam;

    @Before
    public void setUp() throws Exception {
        param = CategoryParamBuilder.newBuilder(1L, "param").build();
        vendorParam = CategoryParamBuilder.newBuilder(KnownIds.VENDOR_PARAM_ID, XslNames.VENDOR).build();
    }

    @Test
    public void parameterCreateNoBilling() {
        parameterAuditService.parameterCreate(context(), param);
        Mockito.verify(auditService).writeActions(actionCaptor.capture());
        Assertions.assertThat(actionCaptor.getValue()).allSatisfy(action ->
            Assertions.assertThat(action.getBillingMode()).isEqualTo(AuditAction.BillingMode.BILLING_MODE_NONE));
    }

    @Test
    public void parameterCreateBilling() {
        parameterAuditService.parameterCreate(context().setBilledOperation(true), param);
        Mockito.verify(auditService).writeActions(actionCaptor.capture());
        Assertions.assertThat(actionCaptor.getValue()).allSatisfy(action ->
            Assertions.assertThat(action.getBillingMode()).isEqualTo(AuditAction.BillingMode.BILLING_MODE_NONE));
    }

    @Test
    public void vendorParameterCreateNoBilling() {
        parameterAuditService.parameterCreate(context(), vendorParam);
        Mockito.verify(auditService).writeActions(actionCaptor.capture());
        Assertions.assertThat(actionCaptor.getValue()).allSatisfy(action ->
            Assertions.assertThat(action.getBillingMode()).isEqualTo(AuditAction.BillingMode.BILLING_MODE_NONE));
    }

    @Test
    public void vendorParameterCreateBilling() {
        parameterAuditService.parameterCreate(context().setBilledOperation(true), vendorParam);
        Mockito.verify(auditService).writeActions(actionCaptor.capture());
        Assertions.assertThat(actionCaptor.getValue()).allSatisfy(action ->
            Assertions.assertThat(action.getBillingMode()).isEqualTo(AuditAction.BillingMode.BILLING_MODE_NONE));
    }

    @Test
    public void optionCreateNoBilling() {
        parameterAuditService.optionCreate(context(), param, new OptionImpl());
        Mockito.verify(auditService).writeActions(actionCaptor.capture());
        Assertions.assertThat(actionCaptor.getValue()).allSatisfy(action ->
            Assertions.assertThat(action.getBillingMode()).isEqualTo(AuditAction.BillingMode.BILLING_MODE_NONE));

        Mockito.verify(visualServiceAudit).valueAdded(anyLong(), any(), any(), billedOperationCaptor.capture());
        Assertions.assertThat(billedOperationCaptor.getValue()).satisfies(billedOperation ->
            Assertions.assertThat(billedOperation).isFalse());
    }

    @Test
    public void optionCreateBilling() {
        parameterAuditService.optionCreate(context().setBilledOperation(true), param, new OptionImpl());
        Mockito.verify(auditService).writeActions(actionCaptor.capture());
        Assertions.assertThat(actionCaptor.getValue()).allSatisfy(action ->
            Assertions.assertThat(action.getBillingMode()).isEqualTo(AuditAction.BillingMode.BILLING_MODE_NONE));

        Mockito.verify(visualServiceAudit).valueAdded(anyLong(), any(), any(), billedOperationCaptor.capture());
        Assertions.assertThat(billedOperationCaptor.getValue()).satisfies(billedOperation ->
            Assertions.assertThat(billedOperation).isTrue());
    }

    @Test
    public void vendorCreateNoBilling() {
        parameterAuditService.optionCreate(context(), vendorParam, new OptionImpl());
        Mockito.verify(auditService).writeActions(actionCaptor.capture());
        Assertions.assertThat(actionCaptor.getValue()).allSatisfy(action ->
            Assertions.assertThat(action.getBillingMode()).isEqualTo(AuditAction.BillingMode.BILLING_MODE_NONE));

        Mockito.verify(visualServiceAudit).valueAdded(anyLong(), any(), any(), billedOperationCaptor.capture());
        Assertions.assertThat(billedOperationCaptor.getValue()).satisfies(billedOperation ->
            Assertions.assertThat(billedOperation).isFalse());
    }

    @Test
    public void vendorCreateBilling() {
        parameterAuditService.optionCreate(context().setBilledOperation(true), vendorParam, new OptionImpl());
        Mockito.verify(auditService).writeActions(actionCaptor.capture());
        Assertions.assertThat(actionCaptor.getValue()).allSatisfy(action ->
            Assertions.assertThat(action.getBillingMode()).isEqualTo(AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM));

        Mockito.verify(visualServiceAudit).valueAdded(anyLong(), any(), any(), billedOperationCaptor.capture());
        Assertions.assertThat(billedOperationCaptor.getValue()).satisfies(billedOperation ->
            Assertions.assertThat(billedOperation).isTrue());
    }

    private ParameterSaveContext context() {
        return new ParameterSaveContext(UID);
    }
}
