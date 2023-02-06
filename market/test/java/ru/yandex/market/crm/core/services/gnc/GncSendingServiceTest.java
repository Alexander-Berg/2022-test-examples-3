package ru.yandex.market.crm.core.services.gnc;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.crm.core.domain.HasUtmLinks;
import ru.yandex.market.crm.core.services.external.gnc.GlobalNotifierHttpClient;
import ru.yandex.market.crm.core.services.external.gnc.domain.AddNotificationInfo;
import ru.yandex.market.crm.core.services.logging.LogSource;
import ru.yandex.market.crm.core.services.logging.SentLogService;
import ru.yandex.market.crm.core.services.logging.TriggerLogSource;
import ru.yandex.market.crm.core.services.sending.UtmLinks;
import ru.yandex.market.crm.json.serialization.JsonSerializer;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GncSendingServiceTest {

    @Mock
    private GlobalNotifierHttpClient client;

    @Mock
    private SentLogService logger;

    @Mock
    private JsonSerializer jsonSerializer;

    private GncNotificationSendingService service;

    @Before
    public void before() {
        when(client.addNotification(any())).thenReturn(CompletableFuture.completedFuture(null));
        service = new GncNotificationSendingService(client, logger);
    }

    @Test
    public void clientIsInvoked() {
        service.sendNotification(
                "new-answer", 666, 777L, null, null,
                ImmutableMap.of("model", "whatever"), UtmLinks.forGncSending("id", null), LogSource.NOOP
        );

        service.sendNotification(
                "new-answer", 666, 777L, "http://ya.ru", "http://ya.ru/img",
                ImmutableMap.of("model", "whatever"), UtmLinks.forGncTrigger("id", null), LogSource.NOOP
        );
        verify(client, times(2)).addNotification(any(AddNotificationInfo.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsIfLinksAreIncorrect() {
        service.sendNotification(
                "new-answer", 666, 777L, "wrongLink", null,
                ImmutableMap.of("model", "whatever"), UtmLinks.forGncTrigger("id", null), LogSource.NOOP
        );
    }

    @Test
    public void testActionLinkIsUpdatedWithUtm() {
        UtmLinks utmLinks = UtmLinks.forGncTrigger(
                "testId",
                HasUtmLinks.from(
                        "utm_campaign_1",
                        "utm_source_1",
                        "utm_medium_1",
                        "utm_referrer_1",
                        123L
                )
        );

        service.sendNotification(
                "new-answer", 666, 777L, "http://yandex.ru", "http://ya.ru/img",
                ImmutableMap.of("model", "whatever"), utmLinks, LogSource.NOOP
        );

        ArgumentCaptor<AddNotificationInfo> infoCaptor = ArgumentCaptor.forClass(AddNotificationInfo.class);
        verify(client).addNotification(infoCaptor.capture());

        String actionLink = infoCaptor.getValue().getMeta().getAction().getLink();
        assertTrue(actionLink.contains("utm_campaign=utm_campaign_1"));
        assertTrue(actionLink.contains("utm_source=utm_source_1"));
        assertTrue(actionLink.contains("utm_medium=utm_medium_1"));
        assertTrue(actionLink.contains("utm_referrer=utm_referrer_1"));
        assertTrue(actionLink.contains("clid=123"));
    }

    @Test
    public void testNotificationWaLoggedWhenSent() {
        TriggerLogSource logSource = new TriggerLogSource("triggerA", "blockA", "processA", "segmentA", "templateA", Map.of(), jsonSerializer);
        service.sendNotification(
                "new-answer",
                666,
                777L,
                null,
                null,
                ImmutableMap.of("model", "whatever"),
                UtmLinks.forGncSending("id", null),
                logSource
        );

        verify(logger, atLeastOnce())
                .logGncSent(
                        eq("new-answer"),
                        eq("market"),
                        eq(666L),
                        eq(777L),
                        isNull(String.class),
                        isNull(String.class),
                        eq(ImmutableMap.of("model", "whatever")),
                        same(logSource),
                        anyLong()
                );
    }
}
