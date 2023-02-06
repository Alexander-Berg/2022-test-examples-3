package ru.yandex.market.crm.triggers.services.active;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.camunda.bpm.model.bpmn.builder.EventBasedGatewayBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.crm.core.domain.Color;
import ru.yandex.market.crm.core.domain.trigger.ProductItem;
import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.core.test.utils.report.Offer;
import ru.yandex.market.crm.core.test.utils.report.ReportTestHelper;
import ru.yandex.market.crm.core.triggers.ExecutionListeners;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.ModelInfo;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.triggers.services.active.ActiveSubscription.MessageInfo;
import ru.yandex.market.crm.triggers.services.bpm.TriggerService;
import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;
import ru.yandex.market.crm.triggers.services.bpm.variables.ModelOffersInfo;
import ru.yandex.market.crm.triggers.services.bpm.variables.ProductItemChange;
import ru.yandex.market.crm.triggers.test.AbstractTriggerTest;
import ru.yandex.market.crm.triggers.test.helpers.EventSubscriptionsTestHelper;
import ru.yandex.market.crm.triggers.test.helpers.TriggersHelper;
import ru.yandex.misc.thread.ThreadUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames.Event.PRODUCT_ITEM_CHANGE;
import static ru.yandex.market.crm.core.services.trigger.MessageTypes.MODEL_BACK_IN_STOCK;
import static ru.yandex.market.crm.core.services.trigger.MessageTypes.MODEL_OUT_OF_STOCK;
import static ru.yandex.market.crm.core.services.trigger.MessageTypes.WISHLIST_ITEM_ADDED;
import static ru.yandex.market.crm.core.test.utils.report.ReportTestHelper.model;
import static ru.yandex.market.crm.core.test.utils.report.ReportTestHelper.offer;

/**
 * @author apershukov
 */
public class ActiveEventsEmitterTest extends AbstractTriggerTest {

    private static final String ACTIVITY_1 = "activity_1";
    private static final String ACTIVITY_2 = "activity_2";

    private static final long MODEL_ID = 121212;
    private static final long REGION_ID = 213;

    private static final ProductItemChange WISHLIST_ADDED_MODEL = new ProductItemChange(
            WISHLIST_ITEM_ADDED,
            Color.GREEN,
            ProductItem.model(String.valueOf(MODEL_ID), String.valueOf(MODEL_ID)),
            REGION_ID,
            false
    );

    @Inject
    private TriggerService triggerService;

    @Inject
    private TriggersHelper triggersHelper;

    @Inject
    private RuntimeService runtimeService;

    @Inject
    private JsonDeserializer jsonDeserializer;

    @Inject
    private ReportTestHelper reportTestHelper;

    @Inject
    private EventSubscriptionsTestHelper subscriptionsTestHelper;

    @Inject
    private ActiveSubscriptionsDAO activeSubscriptionsDAO;

    @Before
    public void setUp() {
        reportTestHelper.prepareModel(Color.GREEN, REGION_ID, model(MODEL_ID));
        reportTestHelper.prepareModelOffers(Color.GREEN, REGION_ID, MODEL_ID);
    }

    /**
     * В случае если на модель в переменной процесса, ожидающего событие MODEL_BACK_IN_STOCK
     * появились офферы, процесс получает сообщение
     */
    @Test
    public void testModelIsBackInStock() {
        String pid = prepareProcess();

        prepareModelOffers();

        moveVisitDate(pid);

        processSpy.waitActivityStartDB(pid, ACTIVITY_2);

        Map<String, Object> vars = runtimeService.getVariables(pid);

        ModelInfo modelInfo = (ModelInfo) vars.get(ProcessVariablesNames.MODEL);
        assertNotNull("Actual model info variable value is absent", modelInfo);
        assertTrue(modelInfo.isOnstock());

        String rawInitialInfo = (String) vars.get(ProcessVariablesNames.MODEL_REGION_OFFERS_INFO);
        assertNotNull("Initial info must be specified", rawInitialInfo);

        ModelOffersInfo initialInfo = jsonDeserializer.readObject(ModelOffersInfo.class, rawInitialInfo);
        assertTrue(initialInfo.getModelInfo().isOnstock());
    }

    /**
     * В случае если на модель в переменной процесса, ожидающего событие MODEL_BACK_IN_STOCK
     * ко времени проверки так и не появились офферы сообщение не отправляется. При этом
     * время следующей проверки сдвигается на два часа.
     */
    @Test
    public void testModelIsStillNotInStock() {
        String pid = prepareProcess();

        assertProcessIsSkippedByEmmiter(pid);
    }

    /**
     * В случае если процесс приостановлен сообщение не отправляется и время следующей
     * проверки сдвигается
     */
    @Test
    public void testSuspendedProcessesAreSkipped() {
        String pid = prepareProcess();
        runtimeService.suspendProcessInstanceById(pid);

        prepareModelOffers();

        assertProcessIsSkippedByEmmiter(pid);
    }

    /**
     * Если процесс ждет нескольких активных событий в случае наступления одного из
     * них процесс продвигается по диаграмме при этом все активные подписки удаляютсяюб.д
     */
    @Test
    public void testSubscriptionToMultipleActiveMessages() {
        String pid = prepareProcessWithEventGateway();

        prepareModelOffers();

        moveVisitDate(pid);

        processSpy.waitActivityStartDB(pid, ACTIVITY_2);

        List<ActiveSubscription> subscriptions = subscriptionsTestHelper.getActiveSubscriptions();
        assertTrue("Process still has active subscriptions", subscriptions.isEmpty());
    }

    /**
     * Условия наступления события проверяются только для сообщений для которых произошел срок проверки.
     * Если у процесса есть другое событие с подоспевшим сроком будет проверено только оно, при этом
     * время проверки событий, которые не были проверены не сдвигается.
     */
    @Test
    public void testDoNotFireEventIfItsVisitTimeInFuture() {
        String pid = prepareProcessWithEventGateway();

        prepareModelOffers();

        ActiveSubscription initialSubscription = updateActiveSubscription(pid, subscription ->
                subscription.getMessages().get(MODEL_OUT_OF_STOCK)
                        .setVisitTime(LocalDateTime.now().minusHours(1))
        );

        waitVisitDateMoved(initialSubscription);

        assertFalse(
                "Process received message",
                processSpy.getCurrentActivityStates().get(pid).containsKey(ACTIVITY_2)
        );

        ActiveSubscription actualSubscription = subscriptionsTestHelper.getSubscriptionForProcess(pid);
        assertEquals(
                initialSubscription.getMessages().get(MODEL_BACK_IN_STOCK).getVisitTime(),
                actualSubscription.getMessages().get(MODEL_BACK_IN_STOCK).getVisitTime()
        );
    }

    private String prepareProcessWithEventGateway() {
        return prepareProcess(builder -> {
            EventBasedGatewayBuilder gatewayBuilder = builder.eventBasedGateway();

            gatewayBuilder
                    .intermediateCatchEvent().message(MODEL_OUT_OF_STOCK)
                    .endEvent();

            gatewayBuilder
                    .intermediateCatchEvent().message(MODEL_BACK_IN_STOCK)
                    .intermediateCatchEvent(ACTIVITY_2).message(WISHLIST_ITEM_ADDED)
                    .endEvent();
        });
    }

    private String prepareProcess(Consumer<AbstractFlowNodeBuilder<?, ?>> customizer) {
        AbstractFlowNodeBuilder<?, ?> builder = TriggersHelper.triggerBuilder()
                .startEvent().message(WISHLIST_ITEM_ADDED)
                .camundaExecutionListenerDelegateExpression(
                        "start",
                        "${" + ExecutionListeners.PDO_PRODUCT_ITEM_CHANGE_LISTENER + "}"
                );

        customizer.accept(builder);

        BpmnModelInstance model = builder.done();

        ProcessDefinition processDefinition = triggerService.addTrigger(model, null);
        triggerService.changeStateById(processDefinition.getId(), false);

        UidBpmMessage message = new UidBpmMessage(
                WISHLIST_ITEM_ADDED,
                Uid.asPuid(111L),
                Map.of(PRODUCT_ITEM_CHANGE, WISHLIST_ADDED_MODEL)
        );

        return triggersHelper.startProcessWithMessage(message).getId();
    }

    private String prepareProcess() {
        return prepareProcess(builder -> builder
                .intermediateCatchEvent(ACTIVITY_1).message(MODEL_BACK_IN_STOCK)
                .intermediateCatchEvent(ACTIVITY_2).message(WISHLIST_ITEM_ADDED)
                .endEvent()
        );
    }

    private void moveVisitDate(String processInstanceId) {
        updateActiveSubscription(processInstanceId, subscription -> {
            LocalDateTime visitTime = LocalDateTime.now().minusHours(1);

            subscription.getMessages().values()
                    .forEach(message -> message.setVisitTime(visitTime));
        });
    }

    private ActiveSubscription updateActiveSubscription(String processInstanceId, Consumer<ActiveSubscription> customizer) {
        ActiveSubscription subscription = subscriptionsTestHelper.getSubscriptionForProcess(processInstanceId);

        customizer.accept(subscription);

        subscription.getMessages().values()
                .stream()
                .map(MessageInfo::getVisitTime)
                .min(Comparator.naturalOrder())
                .ifPresent(subscription::setVisitTime);

        activeSubscriptionsDAO.update(subscription);
        return subscription;
    }

    private void waitVisitDateMoved(ActiveSubscription initial) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 10_000) {
            ActiveSubscription actual = subscriptionsTestHelper.getActiveSubscription(initial.getId());

            if (initial.getVisitTime().isBefore(actual.getVisitTime())) {
                return;
            }

            ThreadUtils.sleep(500);
        }

        Assert.fail("Visit time of subscription has not been moved");
    }

    private void assertProcessIsSkippedByEmmiter(String pid) {
        ActiveSubscription subscription = subscriptionsTestHelper.getSubscriptionForProcess(pid);

        moveVisitDate(pid);

        waitVisitDateMoved(subscription);

        assertFalse(
                "Process received message",
                processSpy.getCurrentActivityStates().get(pid).containsKey(ACTIVITY_2)
        );
    }

    private void prepareModelOffers() {
        Offer[] offers = new Offer[]{offer(), offer()};

        reportTestHelper.prepareModel(Color.GREEN, REGION_ID,
                model(MODEL_ID)
                        .setOffers(offers)
        );

        reportTestHelper.prepareModelOffers(Color.GREEN, REGION_ID, MODEL_ID, offers);
    }
}
