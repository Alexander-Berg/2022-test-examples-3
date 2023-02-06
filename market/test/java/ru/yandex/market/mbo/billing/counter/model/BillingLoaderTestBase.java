package ru.yandex.market.mbo.billing.counter.model;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.mockito.Mock;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.mbo.billing.BillingProvider;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.action.BillingAction;
import ru.yandex.market.mbo.core.audit.AuditServiceMock;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import static org.mockito.Mockito.when;

/**
 * @author anmalysh
 */
@SuppressWarnings({"checkstyle:magicNumber", "checkstyle:parameternumber"})
public abstract class BillingLoaderTestBase {
    private static long actionIdSeq = 1L;

    protected static final long PARAM1 = 1L;
    protected static final long PARAM2 = 2L;
    protected static final long PARAM3 = 3L;
    protected static final long PARAM4 = 4L;
    protected static final String PARAM1_NAME = "Param1";
    protected static final String PARAM2_NAME = "Param2";
    protected static final String PARAM3_NAME = "Param3";
    protected static final String PARAM4_NAME = "Param4";
    protected static final String VALUE1 = "black";
    protected static final String VALUE2 = "white";
    protected static final String VALUE3 = "orange";
    protected static final String VALUE4 = "blue";

    protected static final String URL1 = "url1";
    protected static final String URL2 = "url2";

    protected static final Long DEFAULT_USER_ID = 2L;
    protected static final Long ANOTHER_USER_ID = 3L;

    protected static final Calendar CALENDAR = Calendar.getInstance();
    protected static final Calendar ROLLBACK_CALENDAR = Calendar.getInstance();
    protected static final Date INTERVAL_END;
    protected static final Date INTERVAL_START;

    static {
        INTERVAL_END = CALENDAR.getTime();
        CALENDAR.add(Calendar.DAY_OF_MONTH, -1);
        INTERVAL_START = CALENDAR.getTime();
        ROLLBACK_CALENDAR.add(Calendar.DAY_OF_MONTH, -2);
    }

    protected AuditServiceMock auditService;

    @Mock
    protected BillingProvider provider;

    @Before
    public void setUp() {
        auditService = new AuditServiceMock();
        when(provider.getInterval()).thenReturn(new Pair<>(INTERVAL_START, INTERVAL_END));
    }

    protected AuditAction createAction(Long entityId, AuditAction.ActionType type,
                                            AuditAction.BillingMode billingMode) {
        return createAction(entityId, type, billingMode, DEFAULT_USER_ID);
    }

    protected AuditAction createPreviousAction(Long entityId, AuditAction.ActionType type,
                                               AuditAction.BillingMode billingMode) {
        ROLLBACK_CALENDAR.add(Calendar.SECOND, 1);
        return createAction(entityId, type, billingMode, DEFAULT_USER_ID, ROLLBACK_CALENDAR.getTime());
    }

    protected AuditAction createAction(Long entityId, AuditAction.ActionType type,
                                            AuditAction.BillingMode billingMode, Long userId) {
        CALENDAR.add(Calendar.SECOND, 1);
        return createAction(entityId, type, billingMode, userId, CALENDAR.getTime());
    }

    protected AuditAction createBulkAction(Long entityId, AuditAction.ActionType type,
                                           AuditAction.BillingMode billingMode, Long userId) {
        return  createAction(entityId, type, billingMode, userId, CALENDAR.getTime());
    }

    protected AuditAction createAction(Long entityId, AuditAction.ActionType type,
                                       AuditAction.BillingMode billingMode,
                                       String oldValue, String newValue) {
        return createAction(entityId, type, billingMode, DEFAULT_USER_ID, oldValue, newValue);
    }

    protected AuditAction createPreviousAction(Long entityId, AuditAction.ActionType type,
                                               AuditAction.BillingMode billingMode,
                                               Long paramId, String propertyName,
                                               String oldValue, String newValue) {
        AuditAction result = createPreviousAction(entityId, type, billingMode);
        result.setParameterId(paramId);
        result.setPropertyName(propertyName);
        result.setOldValue(oldValue);
        result.setNewValue(newValue);
        return result;
    }

    protected AuditAction createPreviousAction(Long entityId, AuditAction.ActionType type,
                                               AuditAction.BillingMode billingMode,
                                               String oldValue, String newValue) {
       return createPreviousAction(entityId, type, billingMode, PARAM1, PARAM1_NAME, oldValue, newValue);
    }

    protected AuditAction createAction(Long entityId, AuditAction.ActionType type,
                                       AuditAction.BillingMode billingMode, Long userId,
                                       String oldValue, String newValue) {
        return createAction(entityId, type, billingMode, userId, PARAM1, PARAM1_NAME, oldValue, newValue);
    }

    protected AuditAction createAction(Long entityId, AuditAction.ActionType type,
                                       AuditAction.BillingMode billingMode, Long userId,
                                       Long paramId, String propertyName,
                                       String oldValue, String newValue) {
        AuditAction result = createAction(entityId, type, billingMode, userId);
        result.setParameterId(paramId);
        result.setPropertyName(propertyName);
        result.setOldValue(oldValue);
        result.setNewValue(newValue);
        return result;
    }

    protected AuditAction createAction(Long entityId, AuditAction.ActionType type,
                                       Long paramId, String propertyName,
                                       String oldValue, String newValue) {
        AuditAction result = createAction(entityId, type, AuditAction.BillingMode.BILLING_MODE_FILL, DEFAULT_USER_ID);
        result.setParameterId(paramId);
        result.setPropertyName(propertyName);
        result.setOldValue(oldValue);
        result.setNewValue(newValue);
        return result;
    }

    protected AuditAction createAction(Long entityId, AuditAction.ActionType type,
                                       AuditAction.BillingMode billingMode,
                                       Long paramId, String propertyName,
                                       String oldValue, String newValue) {
        AuditAction result = createAction(entityId, type, billingMode, DEFAULT_USER_ID);
        result.setParameterId(paramId);
        result.setPropertyName(propertyName);
        result.setOldValue(oldValue);
        result.setNewValue(newValue);
        return result;
    }

    protected AuditAction createBulkAction(Long entityId, AuditAction.ActionType type,
                                           AuditAction.BillingMode billingMode,
                                           Long paramId, String propertyName,
                                           String oldValue, String newValue) {
        AuditAction result = createBulkAction(entityId, type, billingMode, DEFAULT_USER_ID);
        result.setParameterId(paramId);
        result.setPropertyName(propertyName);
        result.setOldValue(oldValue);
        result.setNewValue(newValue);
        return result;
    }

    protected AuditAction createAction(Long entityId, AuditAction.ActionType type,
                                       AuditAction.BillingMode billingMode, Long userId, Date date) {
        AuditAction result = new AuditAction();
        result.setActionId(actionIdSeq++);
        result.setEntityType(getEntityType());
        result.setCategoryId(1L);
        result.setEntityId(entityId);
        result.setDate(date);
        result.setEntityName("SKU1");
        result.setUserId(userId);
        result.setActionType(type);
        result.setBillingMode(billingMode);
        return result;
    }

    protected AuditAction actionWithSource(AuditAction action, AuditAction.Source source) {
        action.setSource(source);
        return action;
    }


    public BillingAction createBillingAction(AuditAction action, PaidAction paidAction) {
        return createBillingActionWithSpecialPrice(action, paidAction, null);
    }

    public BillingAction createRepeatedBillingAction(AuditAction action, PaidAction paidAction) {
        return createBillingActionWithSpecialPrice(action, paidAction, BigDecimal.ZERO);
    }

    public BillingAction createBillingActionWithSpecialPrice(AuditAction action, PaidAction paidAction,
                                                             @Nullable BigDecimal specialPrice) {
        BillingAction result = new BillingAction(action.getUserId(), paidAction, action.getDate(),
            action.getCategoryId(), action.getEntityId(), action.getEntityType(), action.getActionId());
        if (specialPrice != null) {
            result.setSpecialPrice(specialPrice);
        }
        return result;
    }

    protected abstract AuditAction.EntityType getEntityType();

    /**
     * Creates 'previous action' with date 3 weeks earlier than usually (according to ROLLBACK_CALENDAR).
     */
    protected AuditAction createOldAction(Long entityId, AuditAction.ActionType type,
                                        AuditAction.BillingMode billingMode,
                                        String oldValue, String newValue) {
        AuditAction action = createPreviousAction(entityId, type, billingMode, oldValue, newValue);
        action.setDate(DateUtils.addDays(action.getDate(), -21));
        return action;
    }

    protected AuditAction createOldAction(Long entityId, AuditAction.ActionType type,
                                          AuditAction.BillingMode billingMode,
                                          Long paramId, String propertyName,
                                          String oldValue, String newValue) {
        AuditAction action = createPreviousAction(entityId, type, billingMode, paramId, propertyName,
            oldValue, newValue);
        action.setDate(DateUtils.addDays(action.getDate(), -21));
        return action;
    }

    /**
     * Creates audit action for the user different from the default user.
     */
    protected AuditAction createActionOfAnotherUser(Long entityId, AuditAction.ActionType type,
                                                  AuditAction.BillingMode billingMode,
                                                  String oldValue, String newValue) {
        AuditAction action = createAction(entityId, type, billingMode, oldValue, newValue);
        action.setUserId(ANOTHER_USER_ID);
        return action;
    }

    protected AuditAction createActionOfAnotherUser(Long entityId, AuditAction.ActionType type,
                                                    AuditAction.BillingMode billingMode,
                                                    Long paramId, String propertyName,
                                                    String oldValue, String newValue) {
        AuditAction action = createAction(entityId, type, billingMode, paramId, propertyName, oldValue, newValue);
        action.setUserId(ANOTHER_USER_ID);
        return action;
    }

    /**
     * Creates billing action for the user different from the user in 'action'.
     */
    protected BillingAction createBillingActionOfAnotherUser(AuditAction action, PaidAction paidAction,
                                                           BigDecimal price, long uid) {
        BillingAction tmpAction = createBillingActionWithSpecialPrice(action, paidAction, price);
        BillingAction result = new BillingAction(uid, tmpAction.getPaidAction(), tmpAction.getActionDate(),
            tmpAction.getCategoryHid(), tmpAction.getEntityId(), tmpAction.getEntityType(), action.getActionId());
        if (price != null) {
            result.setSpecialPrice(price);
        }
        return result;
    }
}
