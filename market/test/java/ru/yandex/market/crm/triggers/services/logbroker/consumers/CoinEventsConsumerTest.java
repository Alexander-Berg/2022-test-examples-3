package ru.yandex.market.crm.triggers.services.logbroker.consumers;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.crm.core.services.logbroker.LogTypesResolver;
import ru.yandex.market.crm.core.services.trigger.MessageTypes;
import ru.yandex.market.crm.external.loyalty.Coin;
import ru.yandex.market.crm.lb.LBInstallation;
import ru.yandex.market.crm.lb.LogIdentifier;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;
import ru.yandex.market.crm.triggers.services.bpm.correlation.MessageSender;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames.COIN;
import static ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames.COIN_ID;

/**
 * @author apershukov
 */
@Ignore
@RunWith(MockitoJUnitRunner.class)
public class CoinEventsConsumerTest {

    private static class MessageMatcher extends BaseMatcher<UidBpmMessage> {

        private final UidBpmMessage expected;

        private MessageMatcher(UidBpmMessage expected) {
            this.expected = expected;
        }

        @Override
        public boolean matches(Object item) {
            if (!(item instanceof  UidBpmMessage)) {
                return false;
            }

            UidBpmMessage message = (UidBpmMessage) item;

            return Objects.equals(expected.getType(), message.getType()) &&
                    Objects.equals(expected.getUid(), message.getUid()) &&
                    Objects.equals(expected.getCorrelationVariables(), message.getCorrelationVariables()) &&
                    varsEqual(expected.getVariables(), message.getVariables());
        }

        private boolean varsEqual(Map<String, Object> expected, Map<String, Object> actual) {
            if (expected.size() != actual.size()) {
                return false;
            }

            for (Entry<String, Object> e : expected.entrySet()) {
                Object expectedValue = e.getValue();
                Object actualValue = actual.get(e.getKey());

                if (COIN.equals(e.getKey()) && !coinsEqual((Coin) expectedValue, (Coin) actualValue)) {
                    return false;
                }

                if (!Objects.equals(expectedValue, actualValue)) {
                    return false;
                }
            }

            return true;
        }

        private boolean coinsEqual(Coin expected, Coin actual) {
            return expected.getId() == actual.getId() &&
                    Objects.equals(expected.getTitle(), actual.getTitle()) &&
                    Objects.equals(expected.getSubtitle(), actual.getSubtitle()) &&
                    Objects.equals(expected.getPromoId(), actual.getPromoId()) &&
                    Objects.equals(expected.getType(), actual.getType()) &&
                    Objects.equals(expected.getNominal(), actual.getNominal()) &&
                    Objects.equals(expected.getDescription(), actual.getDescription()) &&
                    Objects.equals(expected.getInactiveDescription(), actual.getInactiveDescription()) &&
                    Objects.equals(expected.getCreationDate(), actual.getCreationDate()) &&
                    Objects.equals(expected.getStartDate(), actual.getStartDate()) &&
                    Objects.equals(expected.getEndDate(), actual.getEndDate()) &&
                    Objects.equals(expected.getImage(), actual.getImage()) &&
                    Objects.equals(expected.getImages(), actual.getImages()) &&
                    Objects.equals(expected.getBackgroundColor(), actual.getBackgroundColor()) &&
                    Objects.equals(expected.getStatus(), actual.getStatus()) &&
                    expected.isRequireAuth() == actual.isRequireAuth() &&
                    Objects.equals(expected.getActivationToken(), actual.getActivationToken()) &&
                    Objects.equals(expected.getReason(), actual.getReason()) &&
                    Objects.equals(expected.getReasonParam(), actual.getReasonParam()) &&
                    Objects.equals(expected.getOutgoingLink(), actual.getOutgoingLink()) &&
                    Objects.equals(expected.getMergeTag(), actual.getMergeTag());
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(expected.toString());
        }
    }

    private static class MessageListMatcher implements ArgumentMatcher<List<UidBpmMessage>> {

        private final List<UidBpmMessage> messages;

        private MessageListMatcher(List<UidBpmMessage> messages) {
            this.messages = messages;
        }

        @Override
        public boolean matches(List<UidBpmMessage> argument) {
            List<Matcher<? super UidBpmMessage>> matchers = messages.stream()
                    .map(MessageMatcher::new)
                    .collect(Collectors.toList());

            return containsInAnyOrder(matchers).matches(argument);
        }
    }

    private static Coin coin() {
        return new Coin(
                19695,
                10382L,
                "Скидка на 100 рублей",
                "на все заказы",
                "FIXED",
                100.0,
                "Скидку 100 рублей можно применить на любой заказ.",
                "Описание неактивной монеты",
                LocalDateTime.of(2018, 8, 25, 13, 23, 11),
                LocalDateTime.of(2018, 8, 25, 20, 19, 7),
                LocalDateTime.of(2018, 9, 25, 13, 23, 11),
                "https://avatars.mdst.yandex.net/get-smart_shopping/1823/2080eff0-2a8e-4679-926c-032dfb3b29b8/",
                Collections.emptyMap(),
                "ffffff",
                "ACTIVE",
                true,
                "idkfa",
                "EMAIL_COMPANY",
                "gift0220",
                "https://beru.ru/bonus",
                "welcome_delivery_coin_2020"
        );
    }

    private static String line(String user, String eventType) {
        return String.format(
                "tskv\tdate=2018-09-07T16:07:50.241+03:00\t" +
                "user=%s\t" +
                "coin={\\\"id\\\":19695," +
                "\\\"title\\\":\\\"Скидка на 100 рублей\\\"," +
                "\\\"subtitle\\\":\\\"на все заказы\\\"," +
                "\\\"coinType\\\":\\\"FIXED\\\"," +
                "\\\"nominal\\\":100.00," +
                "\\\"description\\\":\\\"Скидку 100 рублей можно применить на любой заказ.\\\"," +
                "\\\"inactiveDescription\\\":\\\"Описание неактивной монеты\\\"," +
                "\\\"creationDate\\\":\\\"25-08-2018 13:23:11\\\"," +
                "\\\"startDate\\\":\\\"2018-08-25T17:19:07.000+0000\\\"," +
                "\\\"endDate\\\":\\\"25-09-2018 13:23:11\\\"," +
                "\\\"image\\\":\\\"https://avatars.mdst.yandex.net/get-smart_shopping/1823/2080eff0-2a8e-4679-926c-032dfb3b29b8/\\\"," +
                "\\\"backgroundColor\\\":\\\"ffffff\\\"," +
                "\\\"status\\\":\\\"ACTIVE\\\"," +
                "\\\"promoId\\\":10382," +
                "\\\"requireAuth\\\":true," +
                "\\\"activationToken\\\":\\\"idkfa\\\", " +
                "\\\"reason\\\":\\\"EMAIL_COMPANY\\\"," +
                "\\\"reasonParam\\\":\\\"gift0220\\\"," +
                "\\\"outgoingLink\\\":\\\"https://beru.ru/bonus\\\"," +
                "\\\"mergeTag\\\":\\\"welcome_delivery_coin_2020\\\"}\t" +
                "event_type=%s\n",
                user,
                eventType
        );
    }

    private static UidBpmMessage message(String messageType, Uid uid) {
        return new UidBpmMessage(
                messageType,
                uid,
                Map.of(
                        COIN_ID, 19695L,
                        COIN, coin()
                )
        );
    }

    @Mock
    private LogTypesResolver logTypesResolver;

    @Mock
    private MessageSender messageSender;

    private CoinEventsConsumer consumer;

    @Before
    public void setUp() {
        when(logTypesResolver.getLogIdentifier(anyString()))
                .thenReturn(new LogIdentifier("test", "test", LBInstallation.LOGBROKER));

        consumer = new CoinEventsConsumer(messageSender, logTypesResolver);
    }

    /**
     * При получении события "COIN_CREATED", связанного с puid в триггерную пратформу
     * отправляется одноименное событие привязанное к этому puid
     */
    @Test
    public void testParseCoinEvent() {
        String user = "{" +
                "\\\"uid\\\":543647447," +
                "\\\"uuid\\\":null," +
                "\\\"yandexUid\\\":null," +
                "\\\"muid\\\":null," +
                "\\\"email\\\":null," +
                "\\\"phone\\\":null" +
                "}";

        String line = line(user, "COIN_CREATED");
        UidBpmMessage expected = message(MessageTypes.COIN_CREATED, Uid.asPuid(543647447L));
        assertMessages(List.of(expected), line);
    }

    /**
     * При получении события "COIN_CREATED", связанного с yuid в триггерную пратформу
     * отправляется одноименное событие привязанное к этому yuid
     */
    @Test
    public void testParseCoinEventLinkedWithYuid() {
        String user = "{" +
            "\\\"uid\\\":null," +
            "\\\"uuid\\\":null," +
            "\\\"yandexUid\\\":\\\"112233\\\"," +
            "\\\"muid\\\":null," +
            "\\\"email\\\":null," +
            "\\\"phone\\\":null" +
            "}";

        String line = line(user, "COIN_CREATED");
        UidBpmMessage expected = message(MessageTypes.COIN_CREATED, Uid.asYuid("112233"));
        assertMessages(List.of(expected), line);
    }

    /**
     * При получении события "COIN_USED", связанного с uuid в триггерную пратформу
     * отправляется одноименное событие привязанное к этому uuid
     */
    @Test
    public void testParseCoinUsedEvent() {
        String user = "{" +
                "\\\"uid\\\":null," +
                "\\\"uuid\\\":\\\"a1b2c3d4\\\"," +
                "\\\"yandexUid\\\":null," +
                "\\\"muid\\\":null," +
                "\\\"email\\\":null," +
                "\\\"phone\\\":null" +
                "}";

        String line = line(user, "COIN_USED");
        UidBpmMessage expected = message(MessageTypes.COIN_USED, Uid.asUuid("a1b2c3d4"));
        assertMessages(List.of(expected), line);
    }

    /**
     * При получении события "COIN_BIND" в триггерную платформу отправляется одноименное событие по
     * всем связанным идентификаторам
     */
    @Test
    public void testSendMultipleBindMessages() {
        String user = "{" +
                "\\\"yandexUid\\\":\\\"222\\\"," +
                "\\\"uuid\\\":null," +
                "\\\"uid\\\":111," +
                "\\\"muid\\\":null," +
                "\\\"email\\\":null," +
                "\\\"phone\\\":null" +
                "}";

        String line = line(user, "COIN_BIND");

        List<UidBpmMessage> expected = List.of(
                message(MessageTypes.COIN_BIND, Uid.asPuid(111L)),
                message(MessageTypes.COIN_BIND, Uid.asYuid("222"))
        );
        assertMessages(expected, line);
    }

    /**
     * В случае если в одном событии указан puid вместе с yuid для отправки сообщение выбирается puid
     */
    @Test
    public void testIfYuidAndPuidIsSpecifiedAtTheSameTime() {
        String user = "{" +
                "\\\"yandexUid\\\":\\\"222\\\"," +
                "\\\"uuid\\\":null," +
                "\\\"uid\\\":111," +
                "\\\"muid\\\":null," +
                "\\\"email\\\":null," +
                "\\\"phone\\\":null" +
                "}";

        String line = line(user, "COIN_CREATED");

        UidBpmMessage expected = message(MessageTypes.COIN_CREATED, Uid.asPuid(111L));
        assertMessages(List.of(expected), line);
    }

    private void assertMessages(List<UidBpmMessage> expected, String line) {
        List<Map<String, String>> rows = consumer.transform(line.getBytes());
        Preconditions.checkArgument( rows != null);
        consumer.accept(rows);

        verify(messageSender).send(argThat(new MessageListMatcher(expected)));
    }
}
