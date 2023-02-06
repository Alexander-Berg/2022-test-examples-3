package ru.yandex.market.crm.triggers.services.bpm;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.builder.EventBasedGatewayBuilder;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.core.services.trigger.MessageTypes;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.triggers.services.active.ActiveSubscription;
import ru.yandex.market.crm.triggers.services.active.ActiveSubscription.MessageInfo;
import ru.yandex.market.crm.triggers.test.AbstractTriggerTest;
import ru.yandex.market.crm.triggers.test.helpers.EventSubscriptionsTestHelper;
import ru.yandex.market.crm.triggers.test.helpers.TriggersHelper;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author apershukov
 */
public class ActiveSubscriptionCreationTest extends AbstractTriggerTest {

    private static void assertTime(LocalDateTime expectedTime, LocalDateTime time) {
        assertNotNull("Time must not be null", time);

        LocalDateTime leftBorder = expectedTime.minusMinutes(5);
        LocalDateTime rightBorder = expectedTime.plusMinutes(5);
        assertTrue(
                "Time " + time + " is not in expected interval [" + leftBorder + ", " + rightBorder + "]",
                leftBorder.isBefore(time) && rightBorder.isAfter(time)
        );
    }

    private static void assertTimeEquals(LocalDateTime expected, LocalDateTime actual) {
        assertEquals(expected.truncatedTo(ChronoUnit.SECONDS), actual.truncatedTo(ChronoUnit.SECONDS));
    }

    private static final String WAIT_ACTIVITY_ID = "wait_activity_id";

    private static final UidBpmMessage CART_ITEM_ADDED_MESSAGE = new UidBpmMessage(
            MessageTypes.CART_ITEM_ADDED,
            Uid.asPuid(111L),
            Map.of(ProcessVariablesNames.PRODUCT_ITEMS, List.of(121212))
    );

    @Inject
    private TriggerService triggerService;

    @Inject
    private TriggersHelper triggersHelper;

    @Inject
    private EventSubscriptionsTestHelper eventSubscriptionsTestHelper;

    /**
     * Для пассивного события активная подписка не создается
     */
    @Test
    public void testDoNotAddActiveSubscriptionForPassiveMessage() {
        prepareAndStartTrigger(MessageTypes.CART_ITEM_ADDED);

        List<ActiveSubscription> subscriptions = eventSubscriptionsTestHelper.getActiveSubscriptions();
        assertTrue(subscriptions.isEmpty());
    }

    /**
     * Для подписке на активное событие создается активная подписка
     */
    @Test
    public void testAddActiveSubscriptionForActiveMessage() {
        ProcessInstance processInstance = prepareAndStartTrigger(MessageTypes.MODEL_NEW_MIN_PRICE);

        List<ActiveSubscription> subscriptions = eventSubscriptionsTestHelper.getActiveSubscriptions();
        assertThat(subscriptions, hasSize(1));

        ActiveSubscription subscription = subscriptions.get(0);

        Map<String, MessageInfo> messages = subscription.getMessages();
        assertNotNull(messages);
        assertEquals(1, messages.size());

        MessageInfo message = messages.get(MessageTypes.MODEL_NEW_MIN_PRICE);
        assertNotNull(message);

        LocalDateTime visitTime = message.getVisitTime();
        assertTime(LocalDateTime.now().plusHours(6), visitTime);

        assertTimeEquals(visitTime, subscription.getVisitTime());
        assertEquals(processInstance.getId(), subscription.getProcessInstanceId());
    }

    /**
     * При удалении подписки на активное собылие удаляется активная подписка
     */
    @Test
    public void testDeleteSubscriptionAfterOnActivityEnd() {
        EventBasedGatewayBuilder builder = TriggersHelper.triggerBuilder()
                .startEvent().message(MessageTypes.CART_ITEM_ADDED)
                .eventBasedGateway().id(WAIT_ACTIVITY_ID);

        builder.intermediateCatchEvent().message(MessageTypes.CART_ITEM_ADDED)
                .endEvent();

        builder.intermediateCatchEvent().message(MessageTypes.MODEL_NEW_MIN_PRICE)
                .endEvent();

        BpmnModelInstance model = builder.done();

        ProcessInstance process = saveAndStartTrigger(model);

        List<ActiveSubscription> subscriptions = eventSubscriptionsTestHelper.getActiveSubscriptions();
        assertThat(subscriptions, hasSize(1));

        ActiveSubscription subscription = subscriptions.get(0);
        assertEquals(process.getId(), subscription.getProcessInstanceId());
        assertEquals(1, subscription.getMessages().size());

        triggerService.sendBpmMessage(CART_ITEM_ADDED_MESSAGE);

        processSpy.waitProcessesEnd(Duration.ofSeconds(30));

        subscriptions = eventSubscriptionsTestHelper.getActiveSubscriptions();
        assertTrue("Active subscription has not been removed", subscriptions.isEmpty());
    }

    /**
     * В случае если процесс одновременно подписывается на несколько активных событий
     * для него создается одна активная подписка в которую включены все активные события
     */
    @Test
    public void testActiveSubscriptionForSeveralMessages() {
        EventBasedGatewayBuilder builder = TriggersHelper.triggerBuilder()
                .startEvent().message(MessageTypes.CART_ITEM_ADDED)
                .eventBasedGateway().id(WAIT_ACTIVITY_ID);

        builder.intermediateCatchEvent().message(MessageTypes.MODEL_NEW_PROMO)
                .endEvent();

        builder.intermediateCatchEvent().message(MessageTypes.MODEL_NEW_MIN_PRICE)
                .endEvent();

        BpmnModelInstance model = builder.done();

        ProcessInstance process = saveAndStartTrigger(model);

        List<ActiveSubscription> subscriptions = eventSubscriptionsTestHelper.getActiveSubscriptions();
        assertThat(subscriptions, hasSize(1));

        ActiveSubscription subscription = subscriptions.get(0);

        assertEquals(process.getId(), subscription.getProcessInstanceId());

        LocalDateTime visitTime = subscription.getVisitTime();
        assertTime(LocalDateTime.now().plusHours(6), visitTime);

        Map<String, MessageInfo> messages = subscription.getMessages();
        assertNotNull(messages);
        assertEquals(2, messages.size());

        assertTimeEquals(visitTime, messages.get(MessageTypes.MODEL_NEW_PROMO).getVisitTime());
        assertTimeEquals(visitTime, messages.get(MessageTypes.MODEL_NEW_MIN_PRICE).getVisitTime());
    }

    private ProcessInstance prepareAndStartTrigger(String waitMessageType) {
        BpmnModelInstance model = TriggersHelper.triggerBuilder()
               .startEvent().message(MessageTypes.CART_ITEM_ADDED)
               .intermediateCatchEvent(WAIT_ACTIVITY_ID).message(waitMessageType)
               .endEvent()
               .done();

        return saveAndStartTrigger(model);
    }

    @NotNull
    private ProcessInstance saveAndStartTrigger(BpmnModelInstance model) {
        ProcessDefinition processDefinition = triggerService.addTrigger(model, null);
        triggerService.changeStateById(processDefinition.getId(), false);

        ProcessInstance processInstance = triggersHelper.startProcessWithMessage(CART_ITEM_ADDED_MESSAGE);
        processSpy.waitActivityStart(processInstance.getId(), WAIT_ACTIVITY_ID);
        return processInstance;
    }
}
