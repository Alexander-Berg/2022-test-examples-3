package ru.yandex.market.mbo.db.modelstorage.compatibility.audit_billing;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.mbo.billing.BillingProvider;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.action.BillingAction;
import ru.yandex.market.mbo.core.audit.AuditServiceMock;
import ru.yandex.market.mbo.db.modelstorage.compatibility.audit_billing.actions.CompatibilityAction;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.models.audit.AuditActionBuilder;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Тесты на биллинг сервис интерфейса совместимость.
 * ТЗ на момент составления тестов: https://wiki.yandex-team.ru/users/ippirina/Audit-i-billing-dlja-sovmestimosti/
 * @author s-ermakov
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
@SuppressWarnings("checkstyle:magicNumber")
public class ModelCompatibilitiesBillingLoaderTest {

    private static final long UID_1 = 100L;
    private static final long UID_2 = 200L;

    private static final long MODEL_ID_1 = 1L;
    private static final long MODEL_ID_2 = 2L;

    private static final BigDecimal PRICE = new BigDecimal("0.03333");
    private static final BigDecimal NEGATE_PRICE = PRICE.negate();

    private AuditServiceMock auditService;

    private ModelCompatibilitiesBillingLoader billingLoader;

    @Mock
    private BillingProvider billingProvider;

    @Before
    public void setUp() throws Exception {
        when(billingProvider.getPrice(any(), any())).thenReturn(PRICE);
        when(billingProvider.getInterval()).thenReturn(new Pair<>(getCurrentDate(), getNextDate()));

        auditService = new AuditServiceMock();
        billingLoader = new ModelCompatibilitiesBillingLoader(auditService);
    }

    // create

    @Test
    public void create() throws Exception {
        // config
        List<AuditAction> actions = createPairActions(UID_1, MODEL_ID_1, MODEL_ID_2, getMorningDate(),
                AuditAction.ActionType.CREATE);
        auditService.writeActions(actions);

        // run
        List<BillingAction> billingActions = billingLoader.loadBillingActions(billingProvider);

        // verify
        assertEquals(1, billingActions.size());
        assertAction(billingActions.get(0), PaidAction.CREATE_COMPATIBILITY, UID_1, getMorningDate());
    }

    @Test
    public void deleteCreate() throws Exception {
        // config
        List<AuditAction> deleteActions = createPairActions(UID_1, MODEL_ID_1, MODEL_ID_2, getMorningDate(),
                AuditAction.ActionType.DELETE);
        List<AuditAction> createActions = createPairActions(UID_1, MODEL_ID_1, MODEL_ID_2, getMiddayDate(),
                AuditAction.ActionType.CREATE);
        auditService.writeActions(deleteActions);
        auditService.writeActions(createActions);

        // run
        List<BillingAction> billingActions = billingLoader.loadBillingActions(billingProvider);

        // verify
        assertEquals(2, billingActions.size());
        assertAction(billingActions.get(0),
            PaidAction.CREATE_COMPATIBILITY_ROLLBACK, UID_1, getMiddayDate(), NEGATE_PRICE);
        assertAction(billingActions.get(1), PaidAction.DELETE_COMPATIBILITY, UID_1, getMorningDate());
    }

    @Test
    public void otherDeleteCreate() throws Exception {
        // config
        List<AuditAction> deleteActions = createPairActions(UID_2, MODEL_ID_1, MODEL_ID_2, getMorningDate(),
                AuditAction.ActionType.DELETE);
        List<AuditAction> createActions = createPairActions(UID_1, MODEL_ID_1, MODEL_ID_2, getMiddayDate(),
                AuditAction.ActionType.CREATE);
        auditService.writeActions(deleteActions);
        auditService.writeActions(createActions);

        // run
        List<BillingAction> billingActions = billingLoader.loadBillingActions(billingProvider);

        // verify
        assertEquals(3, billingActions.size());
        assertAction(billingActions.get(0), PaidAction.CREATE_COMPATIBILITY, UID_1, getMiddayDate());
        assertAction(billingActions.get(1),
            PaidAction.CREATE_COMPATIBILITY_ROLLBACK, UID_2, getMiddayDate(), NEGATE_PRICE);
        assertAction(billingActions.get(2), PaidAction.DELETE_COMPATIBILITY, UID_2, getMorningDate());
    }

    // update

    @Test
    public void createUpdate() throws Exception {
        // config
        List<AuditAction> createActions = createPairActions(UID_1, MODEL_ID_1, MODEL_ID_2, getMorningDate(),
                AuditAction.ActionType.CREATE);
        List<AuditAction> updateActions = createPairActions(UID_1, MODEL_ID_1, MODEL_ID_2, getMiddayDate(),
                AuditAction.ActionType.UPDATE);
        auditService.writeActions(createActions);
        auditService.writeActions(updateActions);

        // run
        List<BillingAction> billingActions = billingLoader.loadBillingActions(billingProvider);

        // verify
        assertEquals(2, billingActions.size());
        assertAction(billingActions.get(0),
            PaidAction.UPDATE_COMPATIBILITY_ROLLBACK, UID_1, getMiddayDate(), NEGATE_PRICE);
        assertAction(billingActions.get(1), PaidAction.CREATE_COMPATIBILITY, UID_1, getMorningDate());
    }

    @Test
    public void otherCreateUpdate() throws Exception {
        // config
        List<AuditAction> createActions = createPairActions(UID_2, MODEL_ID_1, MODEL_ID_2, getMorningDate(),
                AuditAction.ActionType.CREATE);
        List<AuditAction> updateActions = createPairActions(UID_1, MODEL_ID_1, MODEL_ID_2, getMiddayDate(),
                AuditAction.ActionType.UPDATE);
        auditService.writeActions(createActions);
        auditService.writeActions(updateActions);

        // run
        List<BillingAction> billingActions = billingLoader.loadBillingActions(billingProvider);

        // verify
        assertEquals(3, billingActions.size());
        assertAction(billingActions.get(0), PaidAction.UPDATE_COMPATIBILITY, UID_1, getMiddayDate());
        assertAction(billingActions.get(1),
            PaidAction.UPDATE_COMPATIBILITY_ROLLBACK, UID_2, getMiddayDate(), NEGATE_PRICE);
        assertAction(billingActions.get(2), PaidAction.CREATE_COMPATIBILITY, UID_2, getMorningDate());
    }

    @Test
    public void updateUpdate() throws Exception {
        // config
        List<AuditAction> updateActions1 = createPairActions(UID_1, MODEL_ID_1, MODEL_ID_2, getMorningDate(),
                AuditAction.ActionType.UPDATE);
        List<AuditAction> updateActions2 = createPairActions(UID_1, MODEL_ID_1, MODEL_ID_2, getMiddayDate(),
                AuditAction.ActionType.UPDATE);
        auditService.writeActions(updateActions1);
        auditService.writeActions(updateActions2);

        // run
        List<BillingAction> billingActions = billingLoader.loadBillingActions(billingProvider);

        // verify
        assertEquals(2, billingActions.size());
        assertAction(billingActions.get(0), PaidAction.UPDATE_COMPATIBILITY_ROLLBACK, UID_1, getMiddayDate(),
            NEGATE_PRICE);
        assertAction(billingActions.get(1), PaidAction.UPDATE_COMPATIBILITY, UID_1, getMorningDate());
    }

    @Test
    public void otherUpdateUpdate() throws Exception {
        // config
        List<AuditAction> updateActions1 = createPairActions(UID_2, MODEL_ID_1, MODEL_ID_2, getMorningDate(),
                AuditAction.ActionType.UPDATE);
        List<AuditAction> updateActions2 = createPairActions(UID_1, MODEL_ID_1, MODEL_ID_2, getMiddayDate(),
                AuditAction.ActionType.UPDATE);
        auditService.writeActions(updateActions1);
        auditService.writeActions(updateActions2);

        // run
        List<BillingAction> billingActions = billingLoader.loadBillingActions(billingProvider);

        // verify
        assertEquals(3, billingActions.size());
        assertAction(billingActions.get(0), PaidAction.UPDATE_COMPATIBILITY, UID_1, getMiddayDate());
        assertAction(billingActions.get(1), PaidAction.UPDATE_COMPATIBILITY_ROLLBACK, UID_2, getMiddayDate(),
            NEGATE_PRICE);
        assertAction(billingActions.get(2), PaidAction.UPDATE_COMPATIBILITY, UID_2, getMorningDate());
    }

    // delete

    @Test
    public void createDelete() throws Exception {
        // config
        List<AuditAction> createActions = createPairActions(UID_1, MODEL_ID_1, MODEL_ID_2, getMorningDate(),
                AuditAction.ActionType.CREATE);
        List<AuditAction> deleteActions = createPairActions(UID_1, MODEL_ID_1, MODEL_ID_2, getMiddayDate(),
                AuditAction.ActionType.DELETE);
        auditService.writeActions(createActions);
        auditService.writeActions(deleteActions);

        // run
        List<BillingAction> billingActions = billingLoader.loadBillingActions(billingProvider);

        // verify
        assertEquals(2, billingActions.size());
        assertAction(billingActions.get(0), PaidAction.DELETE_COMPATIBILITY_ROLLBACK, UID_1, getMiddayDate(),
            NEGATE_PRICE);
        assertAction(billingActions.get(1), PaidAction.CREATE_COMPATIBILITY, UID_1, getMorningDate());
    }

    @Test
    public void otherCreateDelete() throws Exception {
        // config
        List<AuditAction> createActions = createPairActions(UID_2, MODEL_ID_1, MODEL_ID_2, getMorningDate(),
                AuditAction.ActionType.CREATE);
        List<AuditAction> deleteActions = createPairActions(UID_1, MODEL_ID_1, MODEL_ID_2, getMiddayDate(),
                AuditAction.ActionType.DELETE);
        auditService.writeActions(createActions);
        auditService.writeActions(deleteActions);

        // run
        List<BillingAction> billingActions = billingLoader.loadBillingActions(billingProvider);

        // verify
        assertEquals(3, billingActions.size());
        assertAction(billingActions.get(0), PaidAction.DELETE_COMPATIBILITY, UID_1, getMiddayDate());
        assertAction(billingActions.get(1), PaidAction.DELETE_COMPATIBILITY_ROLLBACK, UID_2, getMiddayDate(),
            NEGATE_PRICE);
        assertAction(billingActions.get(2), PaidAction.CREATE_COMPATIBILITY, UID_2, getMorningDate());
    }

    @Test
    public void updateDelete() throws Exception {
        // config
        List<AuditAction> updateActions = createPairActions(UID_1, MODEL_ID_1, MODEL_ID_2, getMorningDate(),
                AuditAction.ActionType.UPDATE);
        List<AuditAction> deleteActions = createPairActions(UID_1, MODEL_ID_1, MODEL_ID_2, getMiddayDate(),
                AuditAction.ActionType.DELETE);
        auditService.writeActions(updateActions);
        auditService.writeActions(deleteActions);

        // run
        List<BillingAction> billingActions = billingLoader.loadBillingActions(billingProvider);

        // verify
        assertEquals(2, billingActions.size());
        assertAction(billingActions.get(0), PaidAction.DELETE_COMPATIBILITY_ROLLBACK, UID_1, getMiddayDate(),
            NEGATE_PRICE);
        assertAction(billingActions.get(1), PaidAction.UPDATE_COMPATIBILITY, UID_1, getMorningDate());
    }

    @Test
    public void otherUpdateDelete() throws Exception {
        // config
        List<AuditAction> updateActions = createPairActions(UID_2, MODEL_ID_1, MODEL_ID_2, getMorningDate(),
                AuditAction.ActionType.UPDATE);
        List<AuditAction> deleteActions = createPairActions(UID_1, MODEL_ID_1, MODEL_ID_2, getMiddayDate(),
                AuditAction.ActionType.DELETE);
        auditService.writeActions(updateActions);
        auditService.writeActions(deleteActions);

        // run
        List<BillingAction> billingActions = billingLoader.loadBillingActions(billingProvider);

        // verify
        assertEquals(3, billingActions.size());
        assertAction(billingActions.get(0), PaidAction.DELETE_COMPATIBILITY, UID_1, getMiddayDate());
        assertAction(billingActions.get(1), PaidAction.DELETE_COMPATIBILITY_ROLLBACK, UID_2, getMiddayDate(),
            NEGATE_PRICE);
        assertAction(billingActions.get(2), PaidAction.UPDATE_COMPATIBILITY, UID_2, getMorningDate());
    }

    // other different cases

    @Test
    public void noPreviousAction() throws Exception {
        List<AuditAction> updateActions = createPairActions(UID_1, MODEL_ID_1, MODEL_ID_2, getMiddayDate(),
                AuditAction.ActionType.UPDATE);
        auditService.writeActions(updateActions);

        // run
        List<BillingAction> billingActions = billingLoader.loadBillingActions(billingProvider);

        // verify
        assertEquals(1, billingActions.size());
        assertAction(billingActions.get(0), PaidAction.UPDATE_COMPATIBILITY, UID_1, getMiddayDate());
    }

    @Test
    public void previousActionNotInInterval() throws Exception {
        List<AuditAction> createActions = createPairActions(UID_2, MODEL_ID_1, MODEL_ID_2, getLongTimeAgoDate(),
                AuditAction.ActionType.CREATE);
        List<AuditAction> updateActions = createPairActions(UID_1, MODEL_ID_1, MODEL_ID_2, getMiddayDate(),
                AuditAction.ActionType.UPDATE);
        auditService.writeActions(createActions);
        auditService.writeActions(updateActions);

        // run
        List<BillingAction> billingActions = billingLoader.loadBillingActions(billingProvider);

        // verify
        assertEquals(2, billingActions.size());
        assertAction(billingActions.get(0), PaidAction.UPDATE_COMPATIBILITY, UID_1, getMiddayDate());
        assertAction(billingActions.get(1), PaidAction.UPDATE_COMPATIBILITY_ROLLBACK, UID_2, getMiddayDate(),
            NEGATE_PRICE);
    }

    @Test
    public void severalDifferentActions() throws Exception {
        List<AuditAction> firstPart = createPairActions(UID_2, MODEL_ID_1, MODEL_ID_2, getLongTimeAgoDate(),
                AuditAction.ActionType.UPDATE);
        firstPart.addAll(createPairActions(UID_2, MODEL_ID_1, 3L, getLongTimeAgoDate(),
                AuditAction.ActionType.CREATE));

        List<AuditAction> secondPart = createPairActions(UID_1, MODEL_ID_1, MODEL_ID_2, getMorningDate(),
                AuditAction.ActionType.UPDATE);
        secondPart.addAll(createPairActions(UID_1, MODEL_ID_2, 3L, getMorningDate(),
                AuditAction.ActionType.CREATE));

        List<AuditAction> thirdPart = createPairActions(UID_2, MODEL_ID_1, MODEL_ID_2, getMiddayDate(),
                AuditAction.ActionType.DELETE);
        thirdPart.addAll(createPairActions(UID_2, MODEL_ID_2, 3L, getMiddayDate(),
                AuditAction.ActionType.DELETE));

        auditService.writeActions(firstPart);
        auditService.writeActions(secondPart);
        auditService.writeActions(thirdPart);

        // run
        List<BillingAction> billingActions = billingLoader.loadBillingActions(billingProvider);

        // verify
        assertEquals(7, billingActions.size());
        assertAction(billingActions.get(0), PaidAction.DELETE_COMPATIBILITY, UID_2, getMiddayDate());
        assertAction(billingActions.get(1), PaidAction.DELETE_COMPATIBILITY_ROLLBACK, UID_1, getMiddayDate(),
            NEGATE_PRICE);
        assertAction(billingActions.get(2), PaidAction.DELETE_COMPATIBILITY, UID_2, getMiddayDate());
        assertAction(billingActions.get(3), PaidAction.DELETE_COMPATIBILITY_ROLLBACK, UID_1, getMiddayDate(),
            NEGATE_PRICE);

        assertAction(billingActions.get(4), PaidAction.UPDATE_COMPATIBILITY, UID_1, getMorningDate());
        assertAction(billingActions.get(5), PaidAction.UPDATE_COMPATIBILITY_ROLLBACK, UID_2, getMorningDate(),
            NEGATE_PRICE);
        assertAction(billingActions.get(6), PaidAction.CREATE_COMPATIBILITY, UID_1, getMorningDate());
    }

    private void assertAction(BillingAction actualAction, PaidAction paidAction, long uid, Date actionDate) {
        assertAction(actualAction, paidAction, uid, actionDate, null);
    }

    private void assertAction(BillingAction actualAction, PaidAction paidAction, long uid, Date actionDate,
                              BigDecimal specialPrice) {
        assertEquals(actualAction.getPaidAction(), paidAction);
        assertEquals(actualAction.getUid(), uid);
        assertEquals(actualAction.getActionDate(), actionDate);
        assertEquals(actualAction.getSpecialPrice(), specialPrice);
    }

    private List<AuditAction> createPairActions(long uid, long modelId1, long modelId2, Date date,
                                                AuditAction.ActionType actionType) {
        String id = CompatibilityAction.getCompatibilityId(modelId1, modelId2);

        AuditAction auditAction1 = AuditActionBuilder.newBuilder(modelId1, String.valueOf(modelId1), date)
                .setPropertyName(id)
                .setActionType(actionType)
                .setEntityType(AuditAction.EntityType.MODEL_COMPATIBILITY)
                .setUserId(uid)
                .create();
        AuditAction auditAction2 = AuditActionBuilder.newBuilder(modelId2, String.valueOf(modelId2), date)
                .setPropertyName(id)
                .setActionType(actionType)
                .setEntityType(AuditAction.EntityType.MODEL_COMPATIBILITY)
                .setUserId(uid)
                .create();
        List<AuditAction> result = new ArrayList<>();
        result.add(auditAction1);
        result.add(auditAction2);
        return result;
    }

    private Date getNextDate() {
        return parseDate("21.05.2017 00:00");
    }

    private Date getCurrentDate() {
        return parseDate("20.05.2017 00:00");
    }

    private Date getLongTimeAgoDate() {
        return parseDate("01.01.2001 14:48");
    }

    private Date getMorningDate() {
        return parseDate("20.05.2017 10:35");
    }

    private Date getMiddayDate() {
        return parseDate("20.05.2017 12:40");
    }

    private Date parseDate(String date) {
        try {
            DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
            return dateFormat.parse(date);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
