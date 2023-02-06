package ru.yandex.market.crm.triggers.services.bpm.delegates;

import javax.inject.Inject;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.Test;

import ru.yandex.market.crm.core.domain.Color;
import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.core.services.trigger.MessageTypes;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.triggers.services.bpm.TriggerService;
import ru.yandex.market.crm.triggers.test.AbstractServiceTest;
import ru.yandex.market.crm.triggers.test.helpers.PersBasketTestHelper;
import ru.yandex.market.crm.triggers.test.helpers.TriggersHelper;
import ru.yandex.market.crm.triggers.test.helpers.TriggersHelper.ProcessInstance;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.crm.triggers.test.helpers.PersBasketTestHelper.product;

/**
 * @author apershukov
 */
public class WishlistModelPresenceInjectorTest extends AbstractServiceTest {

    private static final long PUID = 111;
    private static final long MODEL_1 = 111;

    @Inject
    private TriggerService triggerService;

    @Inject
    private TriggersHelper triggersHelper;

    @Inject
    private PersBasketTestHelper persBasketTestHelper;

    @Inject
    private WishlistModelPresenceInjector injector;

    /**
     * Если модель из процесса присутствует в отложенных на стороне персов
     * в качестве значения переменной "hasModelInWishlist" устанавливается true
     */
    @Test
    public void testSetPresentFlagIfModelIsInList() throws Exception {
        persBasketTestHelper.prepareWishlist(Uid.asPuid(PUID), Color.GREEN,
                product(MODEL_1),
                product(222)
        );

        DelegateExecution execution = runTask();

        Object value = execution.getVariable(ProcessVariablesNames.HAS_MODEL_IN_WISHLIST);
        assertEquals(Boolean.TRUE, value);
    }

    /**
     * Если модель из процесса отсутствует в отложенных на стороне персов
     * в качестве значения переменной "hasModelInWishlist" устанавливается false
     */
    @Test
    public void testDoNotSetPresentFlagIfModelIsNotInList() throws Exception {
        persBasketTestHelper.prepareWishlist(Uid.asPuid(PUID), Color.GREEN,
                product(222),
                product(333)
        );

        DelegateExecution execution = runTask();

        Object value = execution.getVariable(ProcessVariablesNames.HAS_MODEL_IN_WISHLIST);
        assertEquals(Boolean.FALSE, value);
    }

    private DelegateExecution runTask() throws Exception {
        String taskId = "injector_task";

        BpmnModelInstance model = TriggersHelper.triggerBuilder()
                .startEvent().message(MessageTypes.WISHLIST_ITEM_ADDED)
                .injectTask(taskId, Color.GREEN.name()).injector(WishlistModelPresenceInjector.NAME)
                .endEvent()
                .done();

        ProcessDefinition procDef = triggerService.addTrigger(model, null);

        ProcessInstance instance = new ProcessInstance(Uid.asPuid(PUID))
                .setVariable(ProcessVariablesNames.PRODUCT_ITEM_ID, String.valueOf(MODEL_1));

        return triggersHelper.runTask(injector, procDef.getId(), taskId, instance);
    }
}
