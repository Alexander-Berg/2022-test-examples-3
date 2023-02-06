package ru.yandex.market.crm.platform.reducers;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import org.junit.Test;

import ru.yandex.market.crm.platform.YieldMock;
import ru.yandex.market.crm.platform.commons.CrmInfo;
import ru.yandex.market.crm.platform.commons.MobilePlatform;
import ru.yandex.market.crm.platform.commons.SendingPayload;
import ru.yandex.market.crm.platform.commons.SendingType;
import ru.yandex.market.crm.platform.models.Push;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PushReducerTest {

    private static void assertSendAndOpen(Push sent, Push opened, YieldMock collector) {
        Collection<Push> addedFacts = collector.getAdded(PUSH);
        assertEquals(1, addedFacts.size());

        Push added = addedFacts.iterator().next();

        assertEquals("Должны сохранить информацию о времени отправки", sent.getTimestamp(),
                added.getTimestamp());

        assertEquals("Должны сохранить информацию о времени открытия", opened.getReactionTimestamp(),
                added.getReactionTimestamp());

        assertEquals("Должны сохранить заголовок сообщения", sent.getTitle(), added.getTitle());
        assertEquals("Должны сохранить текст сообщения", sent.getText(), added.getText());
        assertThat(added.getApplication(), not(isEmptyOrNullString()));
    }

    private static void assertSendAndReceive(Push sent, Push received, YieldMock collector) {
        Collection<Push> addedFacts = collector.getAdded(PUSH);
        assertEquals(1, addedFacts.size());

        Push added = addedFacts.iterator().next();

        assertEquals("Должны сохранить информацию о времени отправки", sent.getTimestamp(),
                added.getTimestamp());

        assertEquals("Должны сохранить информацию о времени получения", received.getReceiveTimestamp(),
                added.getReceiveTimestamp());

        assertEquals("Должны сохранить заголовок сообщения", sent.getTitle(), added.getTitle());
        assertEquals("Должны сохранить текст сообщения", sent.getText(), added.getText());
        assertThat(added.getApplication(), not(isEmptyOrNullString()));
    }

    private static void assertTwoSend(Push sent, YieldMock collector) {
        Collection<Push> addedFacts = collector.getAdded(PUSH);
        assertEquals(1, addedFacts.size());

        Push added = addedFacts.iterator().next();

        assertEquals("Должны сохранить информацию о CRM", sent.getCrmInfo(), added.getCrmInfo());
        assertEquals("Должны сохранить информацию о типе отправки", sent.getSendingType(), added.getSendingType());
    }

    private static Push received() {
        long time = 5;

        return Push.newBuilder()
                .setTimestamp(time)
                .setReceiveTimestamp(time)
                .setMetrikaInfo(Push.MetrikaInfo.newBuilder()
                        .setAppId(APP_ID)
                        .setTransferId(TRANSFER_ID)
                        .build()
                )
                .build();
    }

    private static Push open() {
        long time = 7;

        return Push.newBuilder()
                .setTimestamp(time)
                .setReactionTimestamp(time)
                .setReaction(Push.Action.OPEN)
                .setMetrikaInfo(Push.MetrikaInfo.newBuilder()
                        .setAppId(APP_ID)
                        .setTransferId(TRANSFER_ID)
                        .build()
                )
                .build();
    }

    private static Push sent() {
        return Push.newBuilder()
                .setTimestamp(3)
                .setTitle(UUID.randomUUID().toString())
                .setText(UUID.randomUUID().toString())
                .setApplication("market")
                .setMetrikaInfo(Push.MetrikaInfo.newBuilder()
                        .setAppId(APP_ID)
                        .setTransferId(TRANSFER_ID)
                        .build()
                )
                .build();
    }

    private static final String APP_ID = "7";
    private static final String TRANSFER_ID = "11";
    private static final String PUSH = "Push";

    @Test
    public void mergeOpenedAndSent() {
        Push sent = sent();
        Push opened = open();

        YieldMock collector = reduce(opened, sent);

        assertSendAndOpen(sent, opened, collector);
    }

    @Test
    public void mergeOpenedAndSent_Remove() {
        Push sent = sent();
        Push opened = open();

        YieldMock collector = reduce(opened, sent);

        assertTrue("Должны удалить ранее сохраненный факт т.к. у него отличается время отправки",
                collector.isRemoved(PUSH, opened));
    }

    @Test
    public void mergeRecivedAndSended() {
        Push sent = sent();
        Push received = received();

        YieldMock collector = reduce(received, sent);

        assertSendAndReceive(sent, received, collector);
    }

    @Test
    public void mergeRecivedAndSentRemove() {
        Push sent = sent();
        Push received = received();

        YieldMock collector = reduce(received, sent);

        assertTrue("Должны удалить ранее сохраненный факт т.к. у него отличается время отправки",
                collector.isRemoved(PUSH, received));
    }

    @Test
    public void mergeSentAndOpened() {
        Push sent = sent();
        Push opened = open();

        YieldMock collector = reduce(sent, opened);

        assertSendAndOpen(sent, opened, collector);
    }

    @Test
    public void mergeSendedAndOpenedPushReaction() {
        Push sent = sent();
        Push opened = open();

        YieldMock collector = reduce(sent, opened);

        assertEquals(
                "Должны добавить факт реакции пользователя",
                1,
                collector.getAdded("PushReaction").size()
        );
    }

    @Test
    public void mergeSendedAndRecived() {
        Push sent = sent();
        Push received = received();

        YieldMock collector = reduce(sent, received);

        assertSendAndReceive(sent, received, collector);
    }

    @Test
    public void mergeSendedAndRecivedPushReceiving() {
        Push sent = sent();
        Push received = received();

        YieldMock collector = reduce(sent, received);

        assertEquals(
                "Должны добавить факт получения сообщения",
                1,
                collector.getAdded("PushReceiving").size()
        );
    }

    @Test
    public void mergeSeveralSended() {
        Push notCrmSent1 = sent();
        Push notCrmSent2 = sent();

        Push crmSent = sent()
                .toBuilder()
                .setCrmInfo(CrmInfo.newBuilder().setAccount("test"))
                .setSendingType(SendingType.ACTION)
                .build();

        YieldMock collector = reduce(notCrmSent1, crmSent);

        assertTwoSend(crmSent, collector);

        collector = reduce(crmSent, notCrmSent2);

        assertTwoSend(crmSent, collector);
    }

    /**
     * При мердже metrikaInfo новый appId должен замещать старый, а все остальные параметры должны сохраняться
     */
    @Test
    public void testMergeMetrikaInfo() {
        Push fact1 = Push.newBuilder().setMetrikaInfo(
                Push.MetrikaInfo.newBuilder()
                        .setCampaignId("campaignId")
                        .setGroupId("groupId")
                        .setAppId("appId1")
                        .build()
        ).build();
        Push fact2 = Push.newBuilder().setMetrikaInfo(
                Push.MetrikaInfo.newBuilder()
                        .setAppId("appId2")
                        .setTransferId("transferId")
                        .build()
        ).build();

        YieldMock collector = reduce(fact1, fact2);

        Collection<Push> addedFacts = collector.getAdded(PUSH);
        assertEquals(1, addedFacts.size());

        Push.MetrikaInfo info = addedFacts.iterator().next().getMetrikaInfo();
        assertEquals(fact1.getMetrikaInfo().getCampaignId(), info.getCampaignId());
        assertEquals(fact1.getMetrikaInfo().getGroupId(), info.getGroupId());
        assertEquals(fact2.getMetrikaInfo().getAppId(), info.getAppId());
        assertEquals(fact2.getMetrikaInfo().getTransferId(), info.getTransferId());
    }

    /**
     * Если в сохраненном факте пуша платформа присутствовала, а в новом она не известна, то оставляем старую
     */
    @Test
    public void testMergeKnownAndUnknownMobilePlatforms() {
        Push fact1 = Push.newBuilder().setMobilePlatform(MobilePlatform.ANDROID).build();
        Push fact2 = Push.newBuilder().setMobilePlatform(MobilePlatform.UNKNOWN_PLATFORM).build();

        YieldMock collector = reduce(fact1, fact2);

        Collection<Push> addedFacts = collector.getAdded(PUSH);
        assertEquals(1, addedFacts.size());

        Push resultFact = addedFacts.iterator().next();
        assertEquals(MobilePlatform.ANDROID, resultFact.getMobilePlatform());
    }

    /**
     * Если в сохраненном факте пуша платформа была не известна, а в новом она присутствует, то сохраняем новую
     */
    @Test
    public void testMergeUnknownAndKnownMobilePlatforms() {
        Push fact1 = Push.newBuilder().setMobilePlatform(MobilePlatform.UNKNOWN_PLATFORM).build();
        Push fact2 = Push.newBuilder().setMobilePlatform(MobilePlatform.ANDROID).build();

        YieldMock collector = reduce(fact1, fact2);

        Collection<Push> addedFacts = collector.getAdded(PUSH);
        assertEquals(1, addedFacts.size());

        Push resultFact = addedFacts.iterator().next();
        assertEquals(MobilePlatform.ANDROID, resultFact.getMobilePlatform());
    }

    /**
     * Установливаем payload, если в сохраненном факте payload отсутствует
     */
    @Test
    public void testSetPayloadIfStoredFactPayloadIsEmpty() {
        var storedFact = Push.newBuilder().setMobilePlatform(MobilePlatform.UNKNOWN_PLATFORM).build();

        var payload = SendingPayload.newBuilder()
                .setKey(SendingPayload.Key.ORDER_ID)
                .setValue("12345");
        var newFact = Push.newBuilder().addPayload(payload).build();

        var collector = reduce(storedFact, newFact);

        Collection<Push> addedFacts = collector.getAdded(PUSH);
        assertEquals(1, addedFacts.size());

        var resultFact = addedFacts.iterator().next();
        assertEquals(1, resultFact.getPayloadList().size());
        assertEquals(payload.build(), resultFact.getPayloadList().get(0));
    }

    private YieldMock reduce(Push sent, Push received) {
        YieldMock collector = new YieldMock();
        new PushReducer().reduce(Collections.singletonList(sent), Collections.singleton(received), collector);
        return collector;
    }
}
