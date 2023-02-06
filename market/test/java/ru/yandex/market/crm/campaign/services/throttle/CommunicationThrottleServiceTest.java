package ru.yandex.market.crm.campaign.services.throttle;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.campaign.services.appmetrica.FakeTimeProvider;
import ru.yandex.market.crm.campaign.services.appmetrica.ServerDateTimeProvider;
import ru.yandex.market.mcrm.lock.LockService;
import ru.yandex.market.mcrm.lock.MockLockService;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author zloddey
 */
public class CommunicationThrottleServiceTest {

    private static ContactAttempt push(String id) {
        return new ContactAttempt(id, 3);
    }

    private static final int HOUR_OF_DAY = 17;
    private static final ServerDateTimeProvider DATE_TIME_PROVIDER = new FakeTimeProvider(2021, 8, 26, HOUR_OF_DAY, 0);
    private static final LocalDateTime CURRENT_TIME = DATE_TIME_PROVIDER.getDateTime();
    private static final String UUID = "1234567890";
    private static final ContactAttempt ATTEMPT = push(UUID);
    private static final ChannelDescription MARKET_APP = new PushChannelDescription("market");

    private final LockService lockService = new MockLockService();
    private final POJOCommunicationsDAO dao = new POJOCommunicationsDAO();
    private final DaoFactory daoFactory = channel -> dao;

    private CommunicationThrottleService service = new CommunicationThrottleService(
            lockService, DATE_TIME_PROVIDER, daoFactory
    );

    /**
     * По контакту не было прошлых коммуникаций - разрешаем новую
     */
    @Test
    public void addContactWhenHistoryIsEmpty() {
        assertAdded(3);
    }

    /**
     * По контакту не было коммуникаций, но лимит новых коммуникаций равен 0 - запрещаем
     */
    @Test
    public void doNotAddContactWithZeroLimit() {
        assertNotAdded(0, "No communication is allowed for contact");
    }

    /**
     * По контакту были коммуникации, но их число меньше лимита - разрешаем
     */
    @Test
    public void addContactWithAlmostFullHistory() {
        oldCommunications(ATTEMPT, hoursAgo(14), hoursAgo(5));
        assertAdded(3);
    }

    /**
     * По контакту были коммуникации, и их число достигло лимита - запрещаем
     */
    @Test
    public void doNotAddContactWhenHistoryIsFull() {
        oldCommunications(ATTEMPT, "promo", "buy more", hoursAgo(HOUR_OF_DAY).plus(1, SECONDS));
        oldCommunications(ATTEMPT, "transaction", "order delivered", hoursAgo(5));
        oldCommunications(ATTEMPT, "trigger", "gentle reminder", hoursAgo(1));
        assertNotAdded(3, "Contact already had 3 or more communications per day " +
                "(promo \"buy more\", transaction \"order delivered\", trigger \"gentle reminder\")");
    }

    /**
     * А вот если у контакта был бы лимит повыше, то при такой же истории коммуникаций мы ему разрешили бы новую попытку
     */
    @Test
    public void addContactWithLargerLimit() {
        oldCommunications(ATTEMPT, "promo", "buy more", hoursAgo(HOUR_OF_DAY).plus(1, SECONDS));
        oldCommunications(ATTEMPT, "transaction", "order delivered", hoursAgo(5));
        oldCommunications(ATTEMPT, "trigger", "gentle reminder", hoursAgo(1));
        assertAdded(4);
    }

    /**
     * Коммуникации за прошлый день не нужно учитывать. Свежих коммуникаций только 2, поэтому разрешаем новую
     */
    @Test
    public void ignoreYesterdayCommunications() {
        oldCommunications(ATTEMPT, "promo", "buy more", hoursAgo(26), hoursAgo(25), hoursAgo(24));
        oldCommunications(ATTEMPT, "transaction", "order delivered", hoursAgo(5));
        oldCommunications(ATTEMPT, "trigger", "gentle reminder", hoursAgo(1));
        assertAdded(3);
    }

    /**
     * История других контактов не должна считаться при запросе по текущему контакту.
     * По запрашиваемому контакту история пустая - разрешаем коммуникацию
     */
    @Test
    public void ignoreHistoryFromOtherContacts() {
        oldCommunications(push("baz@yandex.ru"), hoursAgo(16), hoursAgo(15));
        oldCommunications(push("foo@yandex.ru"), hoursAgo(5), hoursAgo(1));
        oldCommunications(push("bar@yandex.ru"), hoursAgo(14), hoursAgo(8), hoursAgo(6));
        oldCommunications(push("example@yandex.ru"), hoursAgo(1));
        assertAdded(3);
    }

    /**
     * В одном запросе можно запрашивать коммуникацию для нескольких контактов одного типа
     */
    @Test
    public void allowSeveralCommunicationsInOneRequest() {
        assertAddedMany(List.of(push("foo@yandex.ru"), push("bar@yandex.ru")),
                // разрешённые
                List.of(push("foo@yandex.ru"), push("bar@yandex.ru")),
                // запрещённые
                List.of(),
                // неопределённый результат
                List.of());
    }

    /**
     * Для контактов, по которым история уже заполнена, новые коммуникации запрещаются
     */
    @Test
    public void partiallyAllowCommunications() {
        oldCommunications(push(UUID), hoursAgo(6), hoursAgo(6), hoursAgo(4), hoursAgo(3), hoursAgo(2), hoursAgo(1));
        assertAddedMany(List.of(push(UUID), push("bar@yandex.ru")),
                // разрешённые
                List.of(push("bar@yandex.ru")),
                // запрещённые
                List.of(push(UUID)),
                // неопределённый результат
                List.of());
    }

    /**
     * Список разрешённых коммуникаций может вообще быть пустым,
     * если для всех контактов уже было достаточно много недавних коммуникаций
     */
    @Test
    public void denyAllCommunications() {
        oldCommunications(push(UUID), hoursAgo(3), hoursAgo(2), hoursAgo(1));
        oldCommunications(push("bar@yandex.ru"), hoursAgo(3), hoursAgo(2), hoursAgo(1));
        assertAddedMany(List.of(push(UUID), push("bar@yandex.ru")),
                // разрешённые
                List.of(),
                // запрещённые
                List.of(push(UUID), push("bar@yandex.ru")),
                // неопределённый результат
                List.of());
    }

    /**
     * Если не удалось взять блокировку, возвращается неопределённый результат
     */
    @Test
    public void doNotAllowCommunicationWhenLockCouldNotBeTaken() {
        var lockService = new MockLockService() {
            @Override
            public <T> T doInLock(String key, long timeout, TimeUnit tu, Supplier<T> action) throws TimeoutException {
                throw new TimeoutException("Too busy");
            }
        };
        service = new CommunicationThrottleService(lockService, DATE_TIME_PROVIDER, daoFactory);
        String reason = "Failed to take lock (Too busy)";
        assertUnresolved(3, reason);
    }

    /**
     * Для строк, на которые не удалось взять блокировку, возвращается неопределённый результат (массовый запрос)
     */
    @Test
    public void returnUnresolvedResultForLockedContacts() {
        var lockService = new MockLockService() {
            @Override
            public Map<String, SimpleLock> tryLock(Collection<String> keys) {
                // Блокировка получена только на 1 контакт из запроса
                return Map.of(UUID, lock());
            }
        };
        service = new CommunicationThrottleService(lockService, DATE_TIME_PROVIDER, daoFactory);
        assertAddedMany(List.of(push(UUID), push("bar@yandex.ru")),
                // разрешённые
                List.of(push(UUID)),
                // запрещённые
                List.of(),
                // неопределённый результат
                List.of(push("bar@yandex.ru")));
    }

    /**
     * Для строк, на которые не удалось взять блокировку, возвращается неопределённый результат (массовый запрос).
     * Важный дополнительный случай к предыдущему тесту - мы можем не получить блокировки для всех строк из запроса.
     */
    @Test
    public void returnUnresolvedResultForAllLockedContacts() {
        var lockService = new MockLockService() {
            @Override
            public Map<String, SimpleLock> tryLock(Collection<String> keys) {
                // Не получили ни одной блокировки
                return Map.of();
            }
        };
        service = new CommunicationThrottleService(lockService, DATE_TIME_PROVIDER, daoFactory);
        assertAddedMany(List.of(push(UUID), push("bar@yandex.ru")),
                // разрешённые
                List.of(),
                // запрещённые
                List.of(),
                // неопределённый результат
                List.of(push(UUID), push("bar@yandex.ru")));
    }

    /**
     * Возвращает время в N часов назад от "текущего времени".
     * Если N больше {@code HOUR_OF_DAY}, то мы попадаем во "вчерашний день".
     */
    private LocalDateTime hoursAgo(int hours) {
        return CURRENT_TIME.minus(hours, HOURS);
    }

    private void oldCommunications(ContactAttempt contactAttempt, LocalDateTime... previousTimestamps) {
        oldCommunications(contactAttempt, "promo", "unit test", previousTimestamps);
    }

    private void oldCommunications(ContactAttempt contactAttempt, String type, String label,
                                   LocalDateTime... previousTimestamps) {
        for (LocalDateTime ts : previousTimestamps) {
            dao.addMany(Map.of(contactAttempt, new Communication(ts, type, label)));
        }
    }

    private void assertAdded(int limit) {
        ContactAttempt contactAttempt = new ContactAttempt(UUID, limit);
        var response = service.request(contactAttempt, "promo", "unit test", MARKET_APP);
        assertAll(
                () -> assertTrue(response.isAllowed()),
                () -> assertTrue(response.isResolved()),
                () -> assertEquals("", response.getReasonHuman()),
                () -> assertTrue(dao.hasCommunication(contactAttempt, "promo", "unit test"))
        );
    }

    private void assertAddedMany(Collection<ContactAttempt> contactAttempts,
                                 Collection<ContactAttempt> allowedContacts,
                                 Collection<ContactAttempt> forbiddenContacts,
                                 Collection<ContactAttempt> unresolvedContacts) {
        var response = service.requestMany(contactAttempts, MARKET_APP, "promo", "unit test");
        assertAll(
                () -> assertEquals(contactAttempts.size(), response.size()),
                () -> assertTrue(response.keySet().containsAll(contactAttempts)),
                () -> assertTrue(response.entrySet().stream()
                        .filter(e -> allowedContacts.contains(e.getKey()))
                        .map(Map.Entry::getValue)
                        .allMatch(CommunicationThrottleResponse::isAllowed)),
                () -> assertTrue(response.entrySet().stream()
                        .filter(e -> forbiddenContacts.contains(e.getKey()))
                        .map(Map.Entry::getValue)
                        .noneMatch(CommunicationThrottleResponse::isAllowed)),
                () -> assertTrue(response.entrySet().stream()
                        .filter(e -> unresolvedContacts.contains(e.getKey()))
                        .map(Map.Entry::getValue)
                        .noneMatch(CommunicationThrottleResponse::isResolved))
        );
    }

    private void assertNotAdded(int limit, String reason) {
        ContactAttempt contact = new ContactAttempt(UUID, limit);
        var response = service.request(contact, "promo", "unit test neg", MARKET_APP);
        assertAll(
                () -> assertFalse(response.isAllowed()),
                () -> assertTrue(response.isResolved()),
                () -> assertEquals(reason, response.getReasonHuman()),
                () -> assertTrue(dao.hasNoCommunication(contact, "promo", "unit test neg"))
        );
    }

    private void assertUnresolved(int limit, String reason) {
        ContactAttempt contact = new ContactAttempt(UUID, limit);
        var response = service.request(contact, "promo", "unit test neg", MARKET_APP);
        assertAll(
                () -> assertFalse(response.isAllowed()),
                () -> assertFalse(response.isResolved()),
                () -> assertEquals(reason, response.getReasonHuman()),
                () -> assertTrue(dao.hasNoCommunication(contact, "promo", "unit test neg"))
        );
    }
}
