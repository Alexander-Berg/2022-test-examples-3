package ru.yandex.market.crm.campaign.services.trigger;

import java.util.List;
import java.util.stream.Collectors;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.CatchEvent;
import org.camunda.bpm.model.bpmn.instance.IntermediateCatchEvent;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaExecutionListener;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.crm.campaign.services.bpm.MessageTypesDAO;
import ru.yandex.market.crm.core.domain.trigger.MessageTypeDto;
import ru.yandex.market.crm.core.services.trigger.MessageTypes;
import ru.yandex.market.crm.core.triggers.ExecutionListeners;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static ru.yandex.market.crm.core.triggers.ExecutionListeners.PDO_PRODUCT_ITEM_CHANGE_LISTENER;

@RunWith(MockitoJUnitRunner.class)
public class BpmnTriggerLogicEnricherTest {

    @Mock
    private MessageTypesDAO dao;

    private BpmnTriggerLogicEnricher bpmnTriggerLogicEnricher;

    private static final ru.yandex.market.crm.campaign.domain.trigger.MessageTypes MESSAGE_TYPES =
        new ru.yandex.market.crm.campaign.domain.trigger.MessageTypes(
            asList(
                    MessageTypeDto.builder()
                            .setId(MessageTypes.CART_ITEM_ADDED)
                            .setName("Добавление в корзину")
                            .setListener(ExecutionListeners.CART_ITEM_CHANGE)
                            .build(),
                    MessageTypeDto.builder()
                            .setId(MessageTypes.WISHLIST_ITEM_ADDED)
                            .setName("Удаление модели из отложенных")
                            .setListener(PDO_PRODUCT_ITEM_CHANGE_LISTENER)
                            .build(),
                    MessageTypeDto.builder()
                            .setId(MessageTypes.COMPARISON_ITEM_ADDED)
                            .setName("Добавление модели в сравнения")
                            .setListener(PDO_PRODUCT_ITEM_CHANGE_LISTENER)
                            .build()
            ),
            singletonList(
                    MessageTypeDto.builder()
                            .setId(MessageTypes.ORDER_CANCELLED)
                            .setName("Заказ. Отменен")
                            .build()
            )
    );

    @Before
    public void before() {
        when(dao.getMessageTypes()).thenReturn(MESSAGE_TYPES);
        bpmnTriggerLogicEnricher = new BpmnTriggerLogicEnricher(dao);
    }

    @Test
    public void executionListenerIsSetIfMissingTest() {
        BpmnModelInstance model = Bpmn.readModelFromStream(
                getClass().getResourceAsStream("events_with_an_without_listeners.xml"));
        bpmnTriggerLogicEnricher.ensureCatchEvents(model);

        StartEvent cartAddedEvent = model.getModelElementById("StartEvent_1");
        assertListener(cartAddedEvent, "cartItemChangeListener");
        StartEvent comparisonAddedEvent = model.getModelElementById("StartEvent_3");
        assertListener(comparisonAddedEvent, "wishlistItemChangeListener");
    }

    @Test
    public void executionListenerIsNotSetIfNotRequiredTest() {
        BpmnModelInstance model = Bpmn.readModelFromStream(
                getClass().getResourceAsStream("events_with_an_without_listeners.xml"));
        bpmnTriggerLogicEnricher.ensureCatchEvents(model);

        IntermediateCatchEvent orderCancelledEvent = model.getModelElementById("IntermediateThrowEvent_1");
        assertNull(orderCancelledEvent.getExtensionElements());
    }

    private static void assertListener(CatchEvent event, String listener) {
        List<CamundaExecutionListener> listeners = event.getExtensionElements().getElements().stream()
                .filter(CamundaExecutionListener.class::isInstance)
                .map(CamundaExecutionListener.class::cast)
                .collect(Collectors.toList());

        assertEquals(1, listeners.size());
        CamundaExecutionListener exListener = listeners.get(0);

        assertEquals("${" + listener + "}", exListener.getCamundaDelegateExpression());
    }
}
