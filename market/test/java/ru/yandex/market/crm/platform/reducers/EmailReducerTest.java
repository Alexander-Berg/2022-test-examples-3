package ru.yandex.market.crm.platform.reducers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;

import ru.yandex.market.crm.platform.YieldMock;
import ru.yandex.market.crm.platform.common.FactContainer;
import ru.yandex.market.crm.platform.common.UidQuery;
import ru.yandex.market.crm.platform.common.UidQuery.Criterion;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.common.UserId;
import ru.yandex.market.crm.platform.commons.CrmInfo;
import ru.yandex.market.crm.platform.commons.SendingPayload;
import ru.yandex.market.crm.platform.commons.SendingType;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.config.FactConfig;
import ru.yandex.market.crm.platform.models.Email;
import ru.yandex.market.crm.platform.models.Email.Click;
import ru.yandex.market.crm.platform.models.Email.DeliveryStatus;
import ru.yandex.market.crm.platform.models.Email.EventType;
import ru.yandex.market.crm.platform.models.Email.SenderInfo;
import ru.yandex.market.crm.util.LiluCollectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.crm.platform.reducers.EmailReducer.FACT_ID;

/**
 * @author apershukov
 */
public class EmailReducerTest {

    private static void assertRemoved(Email email, YieldMock collector) {
        Collection<Email> removed = collector.getRemoved(FACT_ID);

        boolean wasRemoved = removed.stream()
                .anyMatch(item ->
                        email.getUid().equals(item.getUid()) &&
                        email.getFactId().equals(item.getFactId()) &&
                        email.getTimestamp() == item.getTimestamp()
                );

        assertTrue("Fact was not removed", wasRemoved);
    }

    private static Email.Builder email() {
        return Email.newBuilder()
                .setTimestamp(RandomUtils.nextInt(0, 100_000))
                .setUid(Uids.create(UidType.EMAIL, "user@yandex.ru"))
                .setMessageId(MESSAGE_ID)
                .setSenderInfo(
                        SenderInfo.newBuilder()
                                .setCampaignId(CAMPAIGN_ID)
                );
    }

    private static final long CAMPAIGN_ID = 123;
    private static final String MESSAGE_ID = "message_id";
    private final EmailReducer reducer = new EmailReducer();

    /**
     * В случае если письмо приходит сразу с заполненным messageId (как это происходит с триггерными письмами)
     * оно сохраняется с ключем формата "campaignId#messageId"
     */
    @Test
    public void testReceiveNewTriggerEmail() {
        Email email = email()
                .setEventType(EventType.SENDING)
                .setSendingType(SendingType.TRIGGER)
                .build();

        YieldMock collector = reduce(Collections.emptyList(), Collections.singleton(email));

        Collection<Email> added = collector.getAdded(FACT_ID);
        assertEquals(1, added.size());
        assertEquals(CAMPAIGN_ID + "#" + MESSAGE_ID, added.iterator().next().getFactId());
    }

    /**
     * В случае если письмо приходит без messageId (как это происходит с письмами из промо рассылок)
     * оно сохраняется с ключем формата "campaignId#null"
     */
    @Test
    public void testReceiveNewPromoEmail() {
        Email email = email()
                .setEventType(EventType.SENDING)
                .clearMessageId()
                .setSendingType(SendingType.PROMO)
                .setSenderInfo(
                        SenderInfo.newBuilder()
                                .setCampaignId(123)
                )
                .build();

        YieldMock collector = reduce(Collections.emptyList(), Collections.singleton(email));

        Collection<Email> added = collector.getAdded(FACT_ID);
        assertEquals(1, added.size());
        assertEquals(CAMPAIGN_ID + "#null", added.iterator().next().getFactId());
    }

    /**
     * В случае если при сохраненном письме с указанным messageId приходит событие
     * доставки статус доставки меняется на SENT. При этом никаких дополнительных действий не предпринимается
     * и factId не меняется
     */
    @Test
    public void testReceiveSenderEventWithSpecifiedMessageId() {
        Email email = email()
                .setSubject("Email")
                .setEventType(EventType.SENDING)
                .setSendingType(SendingType.TRIGGER)
                .setFactId(CAMPAIGN_ID + "#" + MESSAGE_ID)
                .build();

        Email deliveredEvent = email()
                .setDeliveryStatus(DeliveryStatus.SENT)
                .setEventType(EventType.DELIVERING)
                .build();

        YieldMock collector = reduce(Collections.singletonList(email), Collections.singleton(deliveredEvent));

        assertTrue(collector.getRemoved(FACT_ID).isEmpty());

        Collection<Email> added = collector.getAdded(FACT_ID);
        assertEquals(1, added.size());

        Email addedFact = added.iterator().next();
        assertEquals(CAMPAIGN_ID + "#" + MESSAGE_ID, addedFact.getFactId());
        assertEquals(email.getSubject(), addedFact.getSubject());
        assertEquals(email.getMessageId(), addedFact.getMessageId());
        assertEquals(email.getSenderInfo().getCampaignId(), addedFact.getSenderInfo().getCampaignId());
        assertEquals(DeliveryStatus.SENT, addedFact.getDeliveryStatus());
    }

    /**
     * В случае если при сохраненном письме без messageId приходит событие
     * доставки статус доставки меняется на SENT. При этом:
     * <p>
     * - у письма заполняется messageId
     * - у письма меняется формат factId на "campaignId#messageId"
     * - происходит удаление факта со старым factId
     */
    @Test
    public void testChangeFactIdForPromoMessageOnSenderEvent() {
        Email email = email()
                .clearMessageId()
                .setSubject("Email")
                .setEventType(EventType.SENDING)
                .setSendingType(SendingType.PROMO)
                .setFactId(CAMPAIGN_ID + "#null")
                .build();

        Email deliveredEvent = email()
                .setDeliveryStatus(DeliveryStatus.SENT)
                .setEventType(EventType.DELIVERING)
                .build();

        Email openEvent = email()
                .setEventType(EventType.OPENING)
                .addOpenTime(Instant.now().toEpochMilli())
                .build();

        YieldMock collector = reduce(Collections.singletonList(email), Arrays.asList(deliveredEvent, openEvent));

        assertRemoved(email, collector);

        Collection<Email> added = collector.getAdded(FACT_ID);
        assertEquals(1, added.size());

        Email addedFact = added.iterator().next();
        assertEquals(CAMPAIGN_ID + "#message_id", addedFact.getFactId());
        assertEquals(email.getSubject(), addedFact.getSubject());
        assertEquals(deliveredEvent.getMessageId(), addedFact.getMessageId());
        assertEquals(email.getSenderInfo().getCampaignId(), addedFact.getSenderInfo().getCampaignId());
        assertEquals(DeliveryStatus.DELIVERED, addedFact.getDeliveryStatus());
        assertEquals(openEvent.getOpenTimeList(), addedFact.getOpenTimeList());
    }

    /**
     * В случае, если от рассылятора приходит событие, не имеющее соответствующего
     * ему письма в Платформе, новый факт сохраняется с ключем формата "campaignId#null"
     * <p>
     * Это делается для того чтобы, когда придет событие отправки промо-письма, можно было сматчить его
     * с этим событием от рассылятора
     */
    @Test
    public void testFirstSenderEventIsSavedWithoutMessageIdInFactId() {
        Email deliveredEvent = email()
                .setMessageId("message_id-2")
                .setDeliveryStatus(DeliveryStatus.SENT)
                .setEventType(EventType.DELIVERING)
                .build();

        YieldMock collector = reduce(Collections.emptyList(), Collections.singleton(deliveredEvent));

        assertTrue(collector.getRemoved(FACT_ID).isEmpty());

        Collection<Email> added = collector.getAdded(FACT_ID);
        assertEquals(1, added.size());

        Email addedFact = added.iterator().next();
        assertEquals(CAMPAIGN_ID + "#null", addedFact.getFactId());
        assertEquals(deliveredEvent.getMessageId(), addedFact.getMessageId());
        assertEquals(deliveredEvent.getUid(), addedFact.getUid());
        assertEquals(EventType.NULL, addedFact.getEventType());

        Collection<Email> events = collector.getAdded("EmailDelivery");
        assertEquals(1, events.size());
        assertEquals(deliveredEvent.getUid(), events.iterator().next().getUid());
    }

    /**
     * В случае если при имеющемся факте события от рассылятора, приходит еще одно
     * с таким же messageId, factId не меняется.
     * <p>
     * Нужно для того чтобы можно было сматчить событие отправки промо-письма со связанными
     * с ним событиями вне зависимости от того сколько этих событий успело прийти до события
     * отправки.
     */
    @Test
    public void testSecondSenderEventWithSameMessageIdDoesNotChangeFactId() {
        Email deliveredEvent = email()
                .setEventType(EventType.DELIVERING)
                .setDeliveryStatus(DeliveryStatus.SENT)
                .setFactId(CAMPAIGN_ID + "#null")
                .build();

        Email clickEvent = email()
                .setEventType(EventType.CLICK)
                .addClick(
                        Click.newBuilder()
                                .setUrl("https://market.yandex.ru/products/45")
                )
                .build();

        YieldMock collector = reduce(Collections.singletonList(deliveredEvent), Collections.singleton(clickEvent));

        assertTrue(collector.getRemoved(FACT_ID).isEmpty());

        Collection<Email> added = collector.getAdded(FACT_ID);
        assertEquals(1, added.size());

        Email addedFact = added.iterator().next();
        assertEquals(CAMPAIGN_ID + "#null", addedFact.getFactId());
        assertEquals(deliveredEvent.getMessageId(), addedFact.getMessageId());
    }

    /**
     * В случае если приходит событие из рассылятора для которого в Платформе нет соответствующего
     * письма и при этом по той же кампании уже хранится событие без письма с другим messageId,
     * у старого события меняется формат factId на "campaignId#messageId" а новое событие сохраняется
     * с factId формата "campaignId#null".
     * <p>
     * Это нужно для того чтобы события от разных писем не склеивались в одно.
     * Ожидается что подобные перестановки будут происходить исключительно для триггерных писем, которых
     * может быть несколько у одной кампании.
     */
    @Test
    public void testSecondSenderEventWithDifferentMessageIdChangesFactId() {
        Email deliveredEvent1 = email()
                .setEventType(EventType.DELIVERING)
                .setMessageId("message_id_1")
                .setDeliveryStatus(DeliveryStatus.SENT)
                .setFactId(CAMPAIGN_ID + "#null")
                .build();

        Email deliveredEvent2 = email()
                .setEventType(EventType.DELIVERING)
                .setMessageId("message_id_2")
                .setDeliveryStatus(DeliveryStatus.SENT)
                .build();

        YieldMock collector = reduce(Collections.singletonList(deliveredEvent1), Collections.singleton(deliveredEvent2));

        assertRemoved(deliveredEvent1, collector);

        Map<String, Email> added = collector.<Email>getAdded(FACT_ID).stream()
                .collect(LiluCollectors.index(Email::getFactId));

        assertEquals(2, added.size());

        Email fact1 = added.get(CAMPAIGN_ID + "#message_id_1");
        assertNotNull(fact1);
        assertEquals(deliveredEvent1.getUid(), fact1.getUid());
        assertEquals("message_id_1", fact1.getMessageId());

        Email fact2 = added.get(CAMPAIGN_ID + "#null");
        assertNotNull(fact2);
        assertEquals(deliveredEvent2.getUid(), fact2.getUid());
        assertEquals("message_id_2", fact2.getMessageId());
    }

    /**
     * В случае если при наличии в Платформе факта отправки промо-письма от рассылятора
     * приходит событие отправки его от mCRM формат factId события меняется на campaignId#messageId.
     * MessageId берется из события от рассылятора.
     */
    @Test
    public void testPromoEmailSendEventChangesFactId() {
        Email deliveredEvent = email()
                .setEventType(EventType.DELIVERING)
                .setDeliveryStatus(DeliveryStatus.SENT)
                .setFactId(CAMPAIGN_ID + "#null")
                .build();

        Email email = email()
                .setEventType(EventType.SENDING)
                .clearMessageId()
                .setSubject("Email")
                .setSendingType(SendingType.PROMO)
                .setCrmInfo(
                        CrmInfo.newBuilder()
                                .setStepId("step_id")
                )
                .build();

        YieldMock collector = reduce(Collections.singletonList(deliveredEvent), Collections.singleton(email));

        assertRemoved(deliveredEvent, collector);

        Collection<Email> added = collector.getAdded(FACT_ID);
        assertEquals(1, added.size());

        Email addedFact = added.iterator().next();
        assertEquals(email.getUid(), addedFact.getUid());
        assertEquals(CAMPAIGN_ID + "#message_id", addedFact.getFactId());
        assertEquals(email.getSubject(), addedFact.getSubject());
        assertEquals(deliveredEvent.getMessageId(), addedFact.getMessageId());
        assertEquals(email.getSenderInfo().getCampaignId(), addedFact.getSenderInfo().getCampaignId());
        assertEquals(DeliveryStatus.SENT, addedFact.getDeliveryStatus());
        assertEquals(email.getCrmInfo().getStepId(), addedFact.getCrmInfo().getStepId());
    }

    /**
     * В случае если при наличии в Платформе факта отправки триггерного письма от рассылятора
     * приходит событие отправки его от mCRM формат factId события меняется на campaignId#messageId.
     */
    @Test
    public void testTriggerEmailSendEventChangesFactId() {
        Email deliveredEvent = email()
                .setEventType(EventType.DELIVERING)
                .setDeliveryStatus(DeliveryStatus.SENT)
                .setFactId(CAMPAIGN_ID + "#null")
                .build();

        Email email = email()
                .setEventType(EventType.SENDING)
                .setSubject("Email")
                .setSendingType(SendingType.TRIGGER)
                .build();

        YieldMock collector = reduce(Collections.singletonList(deliveredEvent), Collections.singleton(email));

        assertRemoved(deliveredEvent, collector);

        Collection<Email> added = collector.getAdded(FACT_ID);
        assertEquals(1, added.size());

        Email addedFact = added.iterator().next();
        assertEquals(CAMPAIGN_ID + "#message_id", addedFact.getFactId());
        assertEquals(email.getSubject(), addedFact.getSubject());
        assertEquals(deliveredEvent.getMessageId(), addedFact.getMessageId());
        assertEquals(email.getSenderInfo().getCampaignId(), addedFact.getSenderInfo().getCampaignId());
        assertEquals(DeliveryStatus.SENT, addedFact.getDeliveryStatus());
    }

    /**
     * Тестирование обработки нескольких событий от рассылятора с разными messageid
     */
    @Test
    public void testReduceFactsWithDifferedMessageIds() {
        Email deliveredEvent1 = email()
                .setEventType(EventType.DELIVERING)
                .setMessageId("message_id_1")
                .setDeliveryStatus(DeliveryStatus.SENT)
                .setFactId(CAMPAIGN_ID + "#null")
                .build();

        Email deliveredEvent2 = email()
                .setEventType(EventType.DELIVERING)
                .setMessageId("message_id_2")
                .setDeliveryStatus(DeliveryStatus.SENT)
                .build();

        Email deliveredEvent3 = email()
                .setEventType(EventType.DELIVERING)
                .setMessageId("message_id_3")
                .setDeliveryStatus(DeliveryStatus.SENT)
                .build();

        YieldMock collector = reduce(
                Collections.singletonList(deliveredEvent1),
                Arrays.asList(deliveredEvent2, deliveredEvent3)
        );

        Map<String, Email> added = collector.<Email>getAdded(FACT_ID).stream()
                .collect(LiluCollectors.index(Email::getFactId));

        assertEquals(3, added.size());

        Email fact1 = added.get(CAMPAIGN_ID + "#message_id_1");
        assertNotNull(fact1);
        assertEquals("message_id_1", fact1.getMessageId());

        Email fact2 = added.get(CAMPAIGN_ID + "#message_id_2");
        assertNotNull(fact2);
        assertEquals("message_id_2", fact2.getMessageId());

        Email fact3 = added.get(CAMPAIGN_ID + "#null");
        assertNotNull(fact3);
        assertEquals("message_id_3", fact3.getMessageId());
    }

    /**
     * Для reduce запрашиваются строки с пустым messageId в factIdы
     */
    @Test
    public void testRequestRowsWithMessageIdStub() {
        Email deliveredEvent = email()
                .setEventType(EventType.DELIVERING)
                .setDeliveryStatus(DeliveryStatus.SENT)
                .build();

        FactContainer container = new FactContainer(
                UserId.from(Uids.create(UidType.EMAIL, "user@yandex.ru")),
                null,
                0,
                "",
                deliveredEvent
        );

        UidQuery query = reducer.getSelectQuery(mock(FactConfig.class), Collections.singletonList(container));

        List<Criterion> criteria = new ArrayList<>(query.getCriteria());
        assertEquals(2, criteria.size());

        assertEquals(container.getUid(), criteria.get(0).getUid());
        assertEquals(CAMPAIGN_ID + "#" + deliveredEvent.getMessageId(), criteria.get(0).getId());

        assertEquals(container.getUid(), criteria.get(1).getUid());
        assertEquals(CAMPAIGN_ID + "#null", criteria.get(1).getId());
    }

    /**
     * Проверки аггрегации событий открытия, склика и отписки
     */
    @Test
    public void testAggregateEvents() {
        Click click1 = Click.newBuilder()
                .setUrl("http://111")
                .build();

        Email email = email()
                .setEventType(EventType.SENDING)
                .setFactId(CAMPAIGN_ID + "#" + MESSAGE_ID)
                .addClick(click1)
                .addOpenTime(111)
                .addUnsubTime(111)
                .build();

        Click click2 = Click.newBuilder()
                .setUrl("https://222")
                .build();

        Email clickEvent = email()
                .setEventType(EventType.CLICK)
                .addClick(click2)
                .build();

        Email openEvent = email()
                .setEventType(EventType.OPENING)
                .addOpenTime(222)
                .build();

        Email unsubEvent = email()
                .setEventType(EventType.UNSUB)
                .addUnsubTime(222)
                .build();

        YieldMock collector = reduce(
                Collections.singletonList(email),
                Arrays.asList(openEvent, clickEvent, unsubEvent)
        );

        Collection<Email> added = collector.getAdded(FACT_ID);
        assertEquals(1, added.size());

        Email addedFact = added.iterator().next();

        Set<Long> openTime = Sets.newHashSet(addedFact.getOpenTimeList());
        assertEquals(ImmutableSet.of(111L, 222L), openTime);

        Set<String> clickUrls = addedFact.getClickList().stream()
                .map(Click::getUrl)
                .collect(Collectors.toSet());

        assertEquals(ImmutableSet.of(click1.getUrl(), click2.getUrl()), clickUrls);

        Set<Long> unsubTime = Sets.newHashSet(addedFact.getUnsubTimeList());
        assertEquals(ImmutableSet.of(111L, 222L), unsubTime);
    }

    /**
     * Тестирование случая когда среди сохраненных фактов оказалось два
     * с совпадающими factId
     */
    @Test
    public void testReduceWithExistingFactsHavingSameFactId() {
        Email openEvent = email()
                .setFactId(CAMPAIGN_ID + "#null")
                .addOpenTime(222)
                .build();

        Email clickEvent = email()
                .setFactId(CAMPAIGN_ID + "#null")
                .addClick(
                        Click.newBuilder()
                                .setUrl("https://222")
                                .build()
                )
                .build();

        Email email = email()
                .setEventType(EventType.SENDING)
                .build();

        YieldMock collector = reduce(
                Arrays.asList(openEvent, clickEvent),
                Collections.singleton(email)
        );

        Collection<Email> added = collector.getAdded(FACT_ID);
        assertEquals(1, added.size());

        Email addedFact = added.iterator().next();
        assertEquals(email.getUid(), addedFact.getUid());
        assertEquals(CAMPAIGN_ID + "#" + MESSAGE_ID, addedFact.getFactId());
        assertEquals(openEvent.getOpenTimeList(), addedFact.getOpenTimeList());
        assertEquals(clickEvent.getClickList(), addedFact.getClickList());

        assertRemoved(openEvent, collector);
        assertRemoved(clickEvent, collector);
    }

    /**
     * Несколько сохраненных фактов с одним factId среди которых есть событие
     * отправки письма
     */
    @Test
    public void testTwoExistingFactsWithEmail() {
        Email clickEvent = email()
                .setEventType(EventType.CLICK)
                .setFactId(CAMPAIGN_ID + "#null")
                .addClick(
                        Click.newBuilder()
                                .setUrl("http://host.ru/222")
                                .build()
                )
                .build();

        Email openEvent = email()
                .setEventType(EventType.OPENING)
                .setFactId(CAMPAIGN_ID + "#null")
                .addOpenTime(2221)
                .build();

        Email email = email()
                .setFactId(CAMPAIGN_ID + "#null")
                .setSendingType(SendingType.PROMO)
                .addClick(
                        Click.newBuilder()
                            .setUrl("http://host.ru/111")
                )
                .addOpenTime(1111)
                .addUnsubTime(1112)
                .build();

        Email unsubEvent = email()
                .setEventType(EventType.UNSUB)
                .addUnsubTime(2221)
                .build();

        YieldMock collector = reduce(
                Arrays.asList(clickEvent, openEvent, email),
                Collections.singleton(unsubEvent)
        );

        Collection<Email> added = collector.getAdded(FACT_ID);
        assertEquals(1, added.size());

        Email addedFact = added.iterator().next();
        assertEquals(email.getUid(), addedFact.getUid());
        assertEquals(email.getSubject(), addedFact.getSubject());
        assertEquals(email.getOriginalUid(), addedFact.getOriginalUid());
        assertEquals(email.getTimestamp(), addedFact.getTimestamp());

        Set<Click> expectedClicks = ImmutableSet.of(clickEvent.getClick(0), email.getClick(0));
        assertEquals(expectedClicks, ImmutableSet.copyOf(addedFact.getClickList()));

        Set<Long> expectedOpenTime = ImmutableSet.of(openEvent.getOpenTime(0), email.getOpenTime(0));
        assertEquals(expectedOpenTime, ImmutableSet.copyOf(addedFact.getOpenTimeList()));

        Set<Long> expectedUnsubTime = ImmutableSet.of(unsubEvent.getUnsubTime(0), email.getUnsubTime(0));
        assertEquals(expectedUnsubTime, ImmutableSet.copyOf(addedFact.getUnsubTimeList()));

        assertEquals(2, collector.getRemoved(FACT_ID).size());
        assertRemoved(openEvent, collector);
        assertRemoved(clickEvent, collector);
    }

    /**
     * Установливаем payload, если в сохраненном факте payload отсутствует
     */
    @Test
    public void testSetPayloadIfStoredFactPayloadIsEmpty() {
        var email = email()
                .setSubject("Email")
                .setEventType(EventType.SENDING)
                .setSendingType(SendingType.TRIGGER)
                .setFactId(CAMPAIGN_ID + "#" + MESSAGE_ID)
                .build();

        var payload = SendingPayload.newBuilder()
                .setKey(SendingPayload.Key.ORDER_ID)
                .setValue("12345");
        var newEvent = email().addPayload(payload).build();

        var collector = reduce(Collections.singletonList(email), Collections.singleton(newEvent));

        Collection<Email> added = collector.getAdded(FACT_ID);
        assertEquals(1, added.size());

        var addedFact = added.iterator().next();
        assertEquals(CAMPAIGN_ID + "#" + MESSAGE_ID, addedFact.getFactId());
        assertEquals(email.getSubject(), addedFact.getSubject());
        assertEquals(email.getMessageId(), addedFact.getMessageId());
        assertEquals(email.getSenderInfo().getCampaignId(), addedFact.getSenderInfo().getCampaignId());

        assertEquals(1, addedFact.getPayloadList().size());
        assertEquals(payload.build(), addedFact.getPayloadList().get(0));
    }

    private YieldMock reduce(List<Email> stored, Collection<Email> newFacts) {
        YieldMock mock = new YieldMock();
        reducer.reduce(stored, newFacts, mock);
        return mock;
    }
}
