package ru.yandex.market.mbo.billing.counter.localVendor;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.billing.BillingProvider;
import ru.yandex.market.mbo.billing.BillingProviderMock;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.action.BillingAction;
import ru.yandex.market.mbo.billing.counter.AbstractFIllCustomWithRollbackBillingLoaderTest;
import ru.yandex.market.mbo.billing.counter.base.AbstractBillingLoader;
import ru.yandex.market.mbo.billing.counter.info.BillingOperationInfoBase;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.db.GuruVendorsReaderStub;
import ru.yandex.market.mbo.db.KDAuditService;
import ru.yandex.market.mbo.db.VisualServiceAudit;
import ru.yandex.market.mbo.db.params.ParameterSaveContext;
import ru.yandex.market.mbo.db.params.audit.ParameterAuditService;
import ru.yandex.market.mbo.db.params.audit.ParameterAuditServiceImpl;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.security.MboRoles;
import ru.yandex.market.mbo.user.UserManagerMock;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author s-ermakov
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"checkstyle:magicNumber", "checkstyle:lineLength"})
public class LocalVendorAliasesBillingLoaderTest extends AbstractFIllCustomWithRollbackBillingLoaderTest {
    private static final long VISUAL_OPERATOR_UID = 7L;

    protected AbstractBillingLoader loader;
    protected UserManagerMock userManagerMock;

    private CategoryParam categoryParam;

    private ParameterAuditService parameterAuditService;
    private ParameterSaveContext context;

    public LocalVendorAliasesBillingLoaderTest() {
        categoryParam = new Parameter();
        categoryParam.setId(getParameterId());
        categoryParam.setXslName(XslNames.VENDOR);
        categoryParam.setCategoryHid(10);
    }

    @Before
    public void setUp() {
        super.setUp();

        ParameterAuditServiceImpl parameterAuditServiceImpl = new ParameterAuditServiceImpl();
        parameterAuditServiceImpl.setAuditService(auditService);
        parameterAuditServiceImpl.setVisualServiceAudit(Mockito.mock(VisualServiceAudit.class));
        parameterAuditServiceImpl.setGuruVendorsReader(new GuruVendorsReaderStub());
        parameterAuditServiceImpl.setGuruAuditService(Mockito.mock(KDAuditService.class));
        this.parameterAuditService = parameterAuditServiceImpl;


        userManagerMock = new UserManagerMock();
        userManagerMock.addRole(VISUAL_OPERATOR_UID, MboRoles.VISUAL_OPERATOR);

        loader = new LocalVendorAliasesBillingLoader(userManagerMock);
        loader.setAuditService(auditService);
        loader.setPaidEntryDao(paidEntryDao);
        loader.setBillingStartDateStr("01-09-2018");

        context = new ParameterSaveContext(DEFAULT_USER_ID).setBilledOperation(true);
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
    public String getAuditPropertyName() {
        return Option.ALIASES_PROPERTY_NAME;
    }

    @Override
    public PaidAction getAddAction() {
        return PaidAction.ADD_VENDOR_ALIAS;
    }

    @Override
    public PaidAction getDeleteAction() {
        return PaidAction.DELETE_VENDOR_ALIAS;
    }

    @Override
    public PaidAction getRollbackAction() {
        return PaidAction.VENDOR_ALIAS_ROLLBACK;
    }

    @Override
    public AbstractBillingLoader getLoader() {
        return loader;
    }

    @Test
    public void testCreateOption() {
        Option option = createOption("test option", "alias");

        parameterAuditService.optionCreate(context, categoryParam, option);

        List<BillingAction> billingActions = getLoader().loadBillingActions(toNowBillingProvider());

        assertThat(billingActions)
                .usingElementComparatorIgnoringFields("actionDate", "auditActionId")
                .containsExactlyInAnyOrder(
                        new BillingAction(DEFAULT_USER_ID, getAddAction(), null,
                                categoryParam.getCategoryHid(), option.getId(),
                                AuditAction.EntityType.OPTION, 0,
                                BillingOperationInfoBase.DEFAULT_EXTERNAL_SOURCE,
                                BillingOperationInfoBase.DEFAULT_EXTERNAL_SOURCE_ID)
                );
    }

    @Test
    public void testUpdateOption() {
        Option before = createOption("test option", "alias1");
        Option after = createOption("test option", "alias2", "alias3");
        parameterAuditService.optionUpdate(context, categoryParam, before, after);

        List<BillingAction> billingActions = getLoader().loadBillingActions(toNowBillingProvider());

        assertThat(billingActions)
                .usingElementComparatorIgnoringFields("actionDate", "auditActionId")
                .containsExactlyInAnyOrder(
                        new BillingAction(DEFAULT_USER_ID, getDeleteAction(), null,
                                categoryParam.getCategoryHid(), after.getId(), AuditAction.EntityType.OPTION, 0,
                                BillingOperationInfoBase.DEFAULT_EXTERNAL_SOURCE,
                                BillingOperationInfoBase.DEFAULT_EXTERNAL_SOURCE_ID),
                        new BillingAction(DEFAULT_USER_ID, getAddAction(), null,
                                categoryParam.getCategoryHid(), after.getId(), AuditAction.EntityType.OPTION, 0,
                                BillingOperationInfoBase.DEFAULT_EXTERNAL_SOURCE,
                                BillingOperationInfoBase.DEFAULT_EXTERNAL_SOURCE_ID),
                        new BillingAction(DEFAULT_USER_ID, getAddAction(), null,
                                categoryParam.getCategoryHid(), after.getId(), AuditAction.EntityType.OPTION, 0,
                                BillingOperationInfoBase.DEFAULT_EXTERNAL_SOURCE,
                                BillingOperationInfoBase.DEFAULT_EXTERNAL_SOURCE_ID)
                );
    }

    @Test
    public void testDeleteOption() {
        Option after = createOption("test option", "alias2", "alias3");
        parameterAuditService.optionDelete(context, categoryParam, after);

        List<BillingAction> billingActions = getLoader().loadBillingActions(toNowBillingProvider());

        assertThat(billingActions)
            .usingElementComparatorIgnoringFields("actionDate", "auditActionId")
            .isEmpty();
    }

    @Test
    public void testDeleteActionsByVisualOperatorNotProcessedInBilling() {
        AuditAction createPreviousAction = previousAction(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, null, VALUE1);
        AuditAction deleteAction = action(1L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, VALUE1, null);
        deleteAction.setUserId(VISUAL_OPERATOR_UID);

        auditService.writeActions(Arrays.asList(createPreviousAction, deleteAction));

        List<BillingAction> billingActions = getLoader().loadBillingActions(provider);
        assertThat(billingActions).isEmpty();
    }

    @Test
    public void testCreateActionsByVisualOperatorWontRollbackIfSomebodyDeletedThem() {
        AuditAction createAction = action(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, null, VALUE1);
        createAction.setUserId(VISUAL_OPERATOR_UID);
        AuditAction deleteAction = actionOfAnotherUser(1L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, VALUE1, null);

        auditService.writeActions(Arrays.asList(createAction, deleteAction));

        List<BillingAction> billingActions = getLoader().loadBillingActions(provider);
        assertThat(billingActions).isEmpty();
    }

    @Test
    public void testCreateActionsByVisualOperatorOfThePreviousDayWontRollbackIfSomebodyDeletedThem() {
        AuditAction createAction = previousAction(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, null, VALUE1);
        createAction.setUserId(VISUAL_OPERATOR_UID);
        AuditAction deleteAction = actionOfAnotherUser(1L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, VALUE1, null);

        auditService.writeActions(Arrays.asList(createAction, deleteAction));

        List<BillingAction> billingActions = getLoader().loadBillingActions(provider);
        assertThat(billingActions).containsExactlyInAnyOrder(
            createBillingAction(deleteAction, getDeleteAction())
        );
    }

    @Test
    public void testCreateDeleteActionOfVisualOperator() {
        AuditAction createAction = action(1L, AuditAction.ActionType.CREATE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, null, VALUE1);
        createAction.setUserId(VISUAL_OPERATOR_UID);
        AuditAction deleteAction = action(1L, AuditAction.ActionType.DELETE, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, VALUE1, null);
        deleteAction.setUserId(VISUAL_OPERATOR_UID);

        auditService.writeActions(Arrays.asList(createAction, deleteAction));

        List<BillingAction> billingActions = getLoader().loadBillingActions(provider);
        assertThat(billingActions).isEmpty();
    }

    private Option createOption(String name, String... aliases) {
        Option option = new OptionImpl();
        option.setId(7L);
        option.setNames(WordUtil.defaultWords(name));
        option.setLocalizedAliases(Arrays.stream(aliases).map(WordUtil::defaultEnumAlias).collect(Collectors.toList()));
        return option;
    }

    private BillingProvider toNowBillingProvider() {
        return new BillingProviderMock(DateUtils.addSeconds(new Date(), 1));
    }
}
