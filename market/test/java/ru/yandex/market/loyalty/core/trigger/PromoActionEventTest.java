package ru.yandex.market.loyalty.core.trigger;

import java.math.BigDecimal;
import java.time.Duration;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.dao.OrderActionExecutionDao;
import ru.yandex.market.loyalty.core.dao.trigger.TriggerEventDao;
import ru.yandex.market.loyalty.core.model.action.PromoActionContainer;
import ru.yandex.market.loyalty.core.model.multistage.ResolvingState;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.trigger.TriggerEventQueueService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.iterableWithSize;
import static ru.yandex.market.loyalty.core.model.order.OrderStage.ORDER_CREATION;
import static ru.yandex.market.loyalty.core.model.trigger.EventParamName.UID;
import static ru.yandex.market.loyalty.core.model.trigger.EventParamName.UNIQUE_KEY;
import static ru.yandex.market.loyalty.core.utils.CheckouterUtils.DEFAULT_ORDER_ID;
import static ru.yandex.market.loyalty.core.utils.EventFactory.ANOTHER_EVENT_UNIQUE_KEY;
import static ru.yandex.market.loyalty.core.utils.EventFactory.DEFAULT_EVENT_UNIQUE_KEY;
import static ru.yandex.market.loyalty.core.utils.EventFactory.createActionEvent;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.Cashback.getActionsMapWithStaticPerkAddition;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

public class PromoActionEventTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private OrderActionExecutionDao orderActionExecutionDao;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private TriggerEventQueueService triggerEventQueueService;
    @Autowired
    private TriggerEventDao triggerEventDao;

    private Promo promo;
    private PromoActionContainer<?> action;

    @Before
    public void init() {
        configurationService.set(ConfigurationService.PROMO_ACTION_EVENT_EXECUTION_ENABLED, true);
        this.promo = promoManager.createCashbackPromo(PromoUtils.Cashback
                .defaultFixed(BigDecimal.TEN)
                .setCashbackActionsMap(getActionsMapWithStaticPerkAddition(
                        "additionPerkName", ORDER_CREATION
                ))
        );
        this.action = cashbackCacheService.getCashbackPropsOrEmpty(promo.getPromoId()).get()
                .getPromoActionsMap().getAllContainers().get(0);
    }

    @Test
    public void shouldProcessPromoActionEvent() {
        var event = createActionEvent(DEFAULT_ORDER_ID, promo, action.getActionId(), false)
                .addParam(UID, DEFAULT_UID)
                .build();

        var executions = orderActionExecutionDao.findAll();
        assertThat(executions, hasSize(0));

        triggerEventQueueService.addEventToQueue(event);
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        executions = orderActionExecutionDao.findAll();
        assertThat(executions, allOf(
                iterableWithSize(1),
                hasItem(allOf(
                        hasProperty("orderId", equalTo(DEFAULT_ORDER_ID)),
                        hasProperty("promoId", equalTo(promo.getPromoId().getId())),
                        hasProperty("actionId", equalTo(action.getActionId())),
                        hasProperty("result", equalTo(ResolvingState.FINAL))
                ))
        ));
    }

    @Test
    public void shouldNotProcessOnePromoActionTwice() {
        var event = createActionEvent(DEFAULT_ORDER_ID, promo, action.getActionId(), false)
                .addParam(UID, DEFAULT_UID)
                .addParam(UNIQUE_KEY, DEFAULT_EVENT_UNIQUE_KEY)
                .build();

        var executions = orderActionExecutionDao.findAll();
        assertThat(executions, hasSize(0));

        triggerEventQueueService.addEventToQueue(event);
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        executions = orderActionExecutionDao.findAll();
        assertThat(executions, allOf(
                iterableWithSize(1),
                hasItem(allOf(
                        hasProperty("orderId", equalTo(DEFAULT_ORDER_ID)),
                        hasProperty("promoId", equalTo(promo.getPromoId().getId())),
                        hasProperty("actionId", equalTo(action.getActionId())),
                        hasProperty("result", equalTo(ResolvingState.FINAL))
                ))
        ));
        var executionId = executions.get(0).getId();
        var modificationTime = orderActionExecutionDao.selectModificationTimeById(executionId);

        var secondEvent = createActionEvent(DEFAULT_ORDER_ID, promo, action.getActionId(), false)
                .addParam(UID, DEFAULT_UID)
                .addParam(UNIQUE_KEY, ANOTHER_EVENT_UNIQUE_KEY)
                .build();

        triggerEventQueueService.addEventToQueue(secondEvent);
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(triggerEventDao.getAll(), hasSize(2));
        executions = orderActionExecutionDao.findAll();
        var secondModificationTime = orderActionExecutionDao.selectModificationTimeById(executionId);
        assertThat(executions, hasSize(1));
        assertThat(secondModificationTime, equalTo(modificationTime));
    }


    @Test
    public void shouldCancelNotProcessedPromoActionEvent() {
        var event = createActionEvent(DEFAULT_ORDER_ID, promo, action.getActionId(), true)
                .addParam(UID, DEFAULT_UID)
                .build();

        var executions = orderActionExecutionDao.findAll();
        assertThat(executions, hasSize(0));

        triggerEventQueueService.addEventToQueue(event);
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        executions = orderActionExecutionDao.findAll();
        assertThat(executions, allOf(
                iterableWithSize(1),
                hasItem(allOf(
                        hasProperty("orderId", equalTo(DEFAULT_ORDER_ID)),
                        hasProperty("promoId", equalTo(promo.getPromoId().getId())),
                        hasProperty("actionId", equalTo(action.getActionId())),
                        hasProperty("result", equalTo(ResolvingState.CANCELLED))
                ))
        ));
    }


    @Test
    public void shouldCancelProcessedPromoAction() {
        var event = createActionEvent(DEFAULT_ORDER_ID, promo, action.getActionId(), false)
                .addParam(UID, DEFAULT_UID)
                .addParam(UNIQUE_KEY, DEFAULT_EVENT_UNIQUE_KEY)
                .build();

        var executions = orderActionExecutionDao.findAll();
        assertThat(executions, hasSize(0));

        triggerEventQueueService.addEventToQueue(event);
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        executions = orderActionExecutionDao.findAll();
        assertThat(executions, allOf(
                iterableWithSize(1),
                hasItem(allOf(
                        hasProperty("orderId", equalTo(DEFAULT_ORDER_ID)),
                        hasProperty("promoId", equalTo(promo.getPromoId().getId())),
                        hasProperty("actionId", equalTo(action.getActionId())),
                        hasProperty("result", equalTo(ResolvingState.FINAL))
                ))
        ));

        var secondEvent = createActionEvent(DEFAULT_ORDER_ID, promo, action.getActionId(), true)
                .addParam(UID, DEFAULT_UID)
                .addParam(UNIQUE_KEY, ANOTHER_EVENT_UNIQUE_KEY)
                .build();

        triggerEventQueueService.addEventToQueue(secondEvent);
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(triggerEventDao.getAll(), hasSize(2));
        executions = orderActionExecutionDao.findAll();
        assertThat(executions, allOf(
                iterableWithSize(1),
                hasItem(allOf(
                        hasProperty("orderId", equalTo(DEFAULT_ORDER_ID)),
                        hasProperty("promoId", equalTo(promo.getPromoId().getId())),
                        hasProperty("actionId", equalTo(action.getActionId())),
                        hasProperty("result", equalTo(ResolvingState.CANCELLED))
                ))
        ));
    }

    @Test
    public void shouldNotCancelOnePromoActionTwice() {
        var event = createActionEvent(DEFAULT_ORDER_ID, promo, action.getActionId(), true)
                .addParam(UID, DEFAULT_UID)
                .addParam(UNIQUE_KEY, DEFAULT_EVENT_UNIQUE_KEY)
                .build();

        var executions = orderActionExecutionDao.findAll();
        assertThat(executions, hasSize(0));

        triggerEventQueueService.addEventToQueue(event);
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        executions = orderActionExecutionDao.findAll();
        assertThat(executions, allOf(
                iterableWithSize(1),
                hasItem(allOf(
                        hasProperty("orderId", equalTo(DEFAULT_ORDER_ID)),
                        hasProperty("promoId", equalTo(promo.getPromoId().getId())),
                        hasProperty("actionId", equalTo(action.getActionId())),
                        hasProperty("result", equalTo(ResolvingState.CANCELLED))
                ))
        ));
        var executionId = executions.get(0).getId();
        var modificationTime = orderActionExecutionDao.selectModificationTimeById(executionId);

        var secondEvent = createActionEvent(DEFAULT_ORDER_ID, promo, action.getActionId(), true)
                .addParam(UID, DEFAULT_UID)
                .addParam(UNIQUE_KEY, ANOTHER_EVENT_UNIQUE_KEY)
                .build();

        triggerEventQueueService.addEventToQueue(secondEvent);
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(triggerEventDao.getAll(), hasSize(2));
        executions = orderActionExecutionDao.findAll();
        var secondModificationTime = orderActionExecutionDao.selectModificationTimeById(executionId);
        assertThat(executions, hasSize(1));
        assertThat(secondModificationTime, equalTo(modificationTime));
    }

    @Test
    public void shouldNotProcessCancelledPromoAction() {
        var event = createActionEvent(DEFAULT_ORDER_ID, promo, action.getActionId(), true)
                .addParam(UID, DEFAULT_UID)
                .addParam(UNIQUE_KEY, DEFAULT_EVENT_UNIQUE_KEY)
                .build();

        var executions = orderActionExecutionDao.findAll();
        assertThat(executions, hasSize(0));

        triggerEventQueueService.addEventToQueue(event);
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        executions = orderActionExecutionDao.findAll();
        assertThat(executions, allOf(
                iterableWithSize(1),
                hasItem(allOf(
                        hasProperty("orderId", equalTo(DEFAULT_ORDER_ID)),
                        hasProperty("promoId", equalTo(promo.getPromoId().getId())),
                        hasProperty("actionId", equalTo(action.getActionId())),
                        hasProperty("result", equalTo(ResolvingState.CANCELLED))
                ))
        ));
        var executionId = executions.get(0).getId();
        var modificationTime = orderActionExecutionDao.selectModificationTimeById(executionId);

        var secondEvent = createActionEvent(DEFAULT_ORDER_ID, promo, action.getActionId(), false)
                .addParam(UID, DEFAULT_UID)
                .addParam(UNIQUE_KEY, ANOTHER_EVENT_UNIQUE_KEY)
                .build();

        triggerEventQueueService.addEventToQueue(secondEvent);
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(triggerEventDao.getAll(), hasSize(2));
        executions = orderActionExecutionDao.findAll();
        var secondModificationTime = orderActionExecutionDao.selectModificationTimeById(executionId);
        assertThat(executions, hasSize(1));
        assertThat(secondModificationTime, equalTo(modificationTime));
    }
}
