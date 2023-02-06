package ru.yandex.market.crm.campaign.services.trigger;

import java.util.Collections;
import java.util.Set;

import javax.inject.Provider;

import com.google.common.collect.ImmutableSet;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Message;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.crm.campaign.domain.trigger.MessageTypes;
import ru.yandex.market.crm.campaign.services.bpm.MessageTypesDAO;
import ru.yandex.market.crm.campaign.services.messages.MessageTemplatesService;
import ru.yandex.market.crm.core.domain.subscriptions.SubscriptionType;
import ru.yandex.market.crm.core.domain.subscriptions.SubscriptionType.Channel;
import ru.yandex.market.crm.core.domain.trigger.MessageTypeDto;
import ru.yandex.market.crm.core.services.bpm.CustomAttributesHelper;
import ru.yandex.market.crm.core.services.segments.SegmentsDAO;
import ru.yandex.market.crm.core.services.trigger.CustomAttributesNames;
import ru.yandex.market.crm.core.suppliers.SubscriptionsTypesSupplier;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static ru.yandex.market.crm.core.services.trigger.CustomAttributesNames.READABLE_NAME;
import static ru.yandex.market.crm.core.services.trigger.CustomAttributesNames.SEGMENT_NAME;

/**
 * @author apershukov
 */
@RunWith(MockitoJUnitRunner.class)
public class BpmnModelEnricherTest {

    @Mock
    private Provider<MessageTemplatesService> mtsProvider;
    @Mock
    private MessageTemplatesService messageTemplatesService;
    @Mock
    private SubscriptionsTypesSupplier subscriptionTypesSupplier;
    @Mock
    private SegmentsDAO segmentsDAO;
    @Mock
    private MessageTypesDAO messageTypesDAO;
    @InjectMocks
    private BpmnModelEnricher enricher;

    private static SubscriptionType subscriptionType(int id, String code) {
        return new SubscriptionType(id, code, "", false, false, Set.of(Channel.EMAIL));
    }

    private static void assertAttribute(String expected, ModelElementInstance task, String attribute) {
        String actual = CustomAttributesHelper.getAttribute(
                task,
                attribute,
                String.class
        );
        assertEquals(expected, actual);
    }

    private static void assertTemplateName(String expected, ServiceTask task) {
        assertAttribute(expected, task, CustomAttributesNames.TEMPLATE_NAME);
    }

    private static void assertAdvertisingName(ServiceTask task) {
        assertAttribute("ADVERTISING", task, CustomAttributesNames.SUBSCRIPTION_NAME);
    }

    @Before
    public void setUp() {
        when(mtsProvider.get()).thenReturn(messageTemplatesService);

        MessageTypes messageTypes = new MessageTypes(
                Collections.singletonList(
                        MessageTypeDto.builder()
                                .setId("COIN_CREATED")
                                .setName("Монета выдана")
                                .build()
                ),
                Collections.emptyList()
        );

        when(messageTypesDAO.getMessageTypes())
                .thenReturn(messageTypes);
    }

    @Test
    public void testEnrichSendEmailTask() {
        String templateId = "b25a69f8-e9f0-4ca4-9c4d-74cd89f28196:1";
        String templateName = "Template's name";

        when(messageTemplatesService.getTemplateNames(ImmutableSet.of(templateId)))
                .thenReturn(Collections.singletonMap(templateId, templateName));

        when(subscriptionTypesSupplier.resolve(2))
                .thenReturn(subscriptionType(2, "ADVERTISING"));

        when(subscriptionTypesSupplier.resolve(11))
                .thenReturn(subscriptionType(11, "PRICE_DROP"));

        BpmnModelInstance modelInstance = readModel("with_send_email.xml");

        enricher.enrich(modelInstance);

        ServiceTask serviceTask = modelInstance.getModelElementById("ServiceTask_0on2ad4");

        assertTemplateName(templateName, serviceTask);
        assertAdvertisingName(serviceTask);
        assertAttribute("PRICE_DROP", serviceTask, CustomAttributesNames.SUBSCRIPTION_NAME_2);
    }

    @Test
    public void testEnrichTwoSimilarEmailTasks() {
        String templateId = "b25a69f8-e9f0-4ca4-9c4d-74cd89f28196:4";
        String templateName = "Template's name";

        when(messageTemplatesService.getTemplateNames(ImmutableSet.of(templateId)))
                .thenReturn(Collections.singletonMap(templateId, templateName));

        when(subscriptionTypesSupplier.resolve(2))
                .thenReturn(subscriptionType(2, "ADVERTISING"));

        BpmnModelInstance model = readModel("with_two_similar_emails.xml");

        enricher.enrich(model);

        ServiceTask task1 = model.getModelElementById("ServiceTask_15b8bov");
        assertTemplateName(templateName, task1);
        assertAdvertisingName(task1);

        ServiceTask task2 = model.getModelElementById("ServiceTask_0scfle4");
        assertTemplateName(templateName, task2);
        assertAdvertisingName(task2);
    }

    @Test
    public void testEnrichPushTemplates() {
        String templateId = "b25a69f8-e9f0-4ca4-9c4d-74cd89f28196:4";
        String templateName = "Template's name";

        when(messageTemplatesService.getTemplateNames(ImmutableSet.of(templateId)))
                .thenReturn(Collections.singletonMap(templateId, templateName));

        when(subscriptionTypesSupplier.resolve(64))
                .thenReturn(subscriptionType(64, "STORE_PUSH_GENERAL_ADVERTISING"));

        BpmnModelInstance modelInstance = readModel("with_send_push.xml");

        enricher.enrich(modelInstance);

        ServiceTask serviceTask = modelInstance.getModelElementById("ServiceTask_1vlc5kg");
        assertTemplateName(templateName, serviceTask);
        assertAttribute("STORE_PUSH_GENERAL_ADVERTISING", serviceTask, CustomAttributesNames.SUBSCRIPTION_NAME);
    }

    @Test
    public void testEnrichStartEvent() {
        String segmentId = "seg_kg0vRm";
        String segmentName = "Segment's name";
        when(segmentsDAO.getSegmentNames(ImmutableSet.of(segmentId)))
                .thenReturn(Collections.singletonMap(segmentId, segmentName));

        BpmnModelInstance modelInstance = readModel("start_event_only.xml");

        enricher.enrich(modelInstance);

        StartEvent startEvent = modelInstance.getModelElementById("StartEvent_1");

        assertAttribute(segmentName, startEvent, SEGMENT_NAME);

        Message message = modelInstance.getModelElementById("Message_17nwmhc");
        assertAttribute("Монета выдана", message, READABLE_NAME);
    }

    private BpmnModelInstance readModel(String s) {
        return Bpmn.readModelFromStream(
                getClass().getResourceAsStream(s)
        );
    }
}
