package ru.yandex.market.crm.campaign.services.messages.gnc;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.crm.campaign.services.messages.MessageTemplatesService;
import ru.yandex.market.crm.core.domain.messages.GncMessageConf;
import ru.yandex.market.crm.core.domain.messages.GncMessageConf.LinkType;
import ru.yandex.market.crm.core.domain.messages.MessageTemplate;
import ru.yandex.market.crm.core.services.gnc.GncNotificationSendingService;
import ru.yandex.market.crm.core.services.logging.LogSource;
import ru.yandex.market.crm.core.services.sending.UtmLinks;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GncMessagesServiceTest {

    @Mock
    public GncNotificationSendingService sendingService;

    @Mock
    public MessageTemplatesService templatesService;

    private GncMessagesService messagesService;

    @Before
    public void before() {
        when(sendingService.sendNotification(any(), anyLong(), any(), any(), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        messagesService = new GncMessagesService(sendingService, templatesService);
    }

    @Test
    public void testParseAndExtractVariablesForTestNotification() {
        prepareMessageTemplate(
            "questionAuthorPuidVar",
                LinkType.VAR,
                "questionLink",
                LinkType.VAR,
                "modelLinkImg",
                Map.of("model", "modelNameInProcess"));

        Map<String, Object> vars = Map.of(
                "questionAuthorPuidVar", 111222333,
                "questionLink", "http://question",
                "modelLinkImg", "http://model/img",
                "modelNameInProcess", "samsung");

        messagesService.sendTest("id_1", vars, Collections.singletonList(777L));

        verify(sendingService, atLeastOnce()).sendNotification(
                eq("new-answer"),
                eq(777L),
                eq(111222333L),
                eq("http://question"),
                eq("http://model/img"),
                eq(Map.of("model", "samsung")),
                any(UtmLinks.class),
                same(LogSource.NOOP)
        );
    }

    @Test
    public void tesActionAndPreviewAreEmpty() {
        prepareMessageTemplate("actorVar", null, null, null, null, Map.of("model",
                "modelNameInProcess"));
        Map<String, Object> vars = Map.of(
                "actorVar", 101010,
                "modelNameInProcess", "айфон");

        messagesService.sendTest("id_1", vars, Collections.singletonList(123L));

        verify(sendingService, atLeastOnce()).sendNotification(
                eq("new-answer"),
                eq(123L),
                eq(101010L),
                eq(null),
                eq(null),
                eq(Map.of("model", "айфон")),
                any(UtmLinks.class),
                same(LogSource.NOOP)
        );
    }

    private void prepareMessageTemplate(String actorVar,
                                        LinkType actionLinkType,
                                        String action,
                                        LinkType resourceLinkType,
                                        String resource,
                                        Map<String, String> vars) {
        GncMessageConf config = new GncMessageConf();
        config.setType("new-answer");
        config.setActorVar(actorVar);

        config.setActionLinkType(actionLinkType);
        config.setActionLink(action);

        config.setResourceLinkType(resourceLinkType);
        config.setResourceLink(resource);

        config.setTemplateVars(vars);

        var messageTemplate = new MessageTemplate<>();
        messageTemplate.setId("id_1");
        messageTemplate.setConfig(config);

        when(templatesService.getTemplateById("id_1")).thenReturn(messageTemplate);
    }
}
