package ru.yandex.market.crm.campaign.services.throttle;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Генератор тестов для реализаций {@link CommunicationsDAO}.
 * Т.к. реализаций у интерфейса два, они должны вести себя одинаковым образом.
 * Чтобы не дублировать тесты, используем набор "контрактов" в виде enum-а.
 * Каждый элемент - это отдельный тест, который проверяет каждую из двух реализаций интерфейса CommunicationsDAO.
 *
 * @author zloddey
 */
public enum CommunicationsDAOContract {
    NO_COMMUNICATIONS_BEFORE_WRITE(dao -> {
        ContactAttempt contact1 = attempt("random-puid");
        ContactAttempt contact2 = attempt("another-puid");
        Map<String, List<Communication>> communications = dao.get(List.of(contact1, contact2));
        Map<String, List<Object>> expected = Map.of(
                contact1.getId(), List.of(),
                contact2.getId(), List.of()
        );
        assertEquals(expected, communications);
    }),

    READ_AND_WRITE_SEVERAL_PUSH_COMMUNICATIONS(dao -> {
        LocalDateTime now = LocalDateTime.of(2021, 6, 30, 16, 35, 0);

        ContactAttempt id1 = attempt("puid-123");
        ContactAttempt id2 = attempt("puid-456");
        ContactAttempt id3 = attempt("puid-789");
        Communication c1 = new Communication(now, "promo", "dao-test");
        Communication c2 = new Communication(now.plus(1L, ChronoUnit.HOURS), "trigger", "hello");
        Communication c3 = new Communication(now.plus(65L, ChronoUnit.MINUTES), "transaction", "order");

        dao.addMany(Map.of(id1, c1, id2, c2, id3, c3));
        Map<String, List<Communication>> saved = dao.get(List.of(id1, id2));
        assertAll(
                () -> assertNotNull(saved),
                () -> assertEquals(2, saved.size()),
                () -> assertEquals(List.of(c1), saved.get(id1.getId())),
                () -> assertEquals(List.of(c2), saved.get(id2.getId())),
                () -> assertFalse(saved.containsKey(id3.getId()))
        );
    }),

    READ_AND_WRITE_SEVERAL_EMAIL_COMMUNICATIONS(dao -> {
        LocalDateTime now = LocalDateTime.of(2021, 6, 30, 16, 35, 0);

        ContactAttempt id1 = attempt("foo@yandex.ru");
        ContactAttempt id2 = attempt("bar@yandex.ru");
        ContactAttempt id3 = attempt("baz@yandex.ru");
        Communication c1 = new Communication(now, "promo", "dao-test");
        Communication c2 = new Communication(now.plus(1L, ChronoUnit.HOURS), "trigger", "hello");
        Communication c3 = new Communication(now.plus(65L, ChronoUnit.MINUTES), "transaction", "order");

        dao.addMany(Map.of(id1, c1, id2, c2, id3, c3));
        Map<String, List<Communication>> saved = dao.get(List.of(id1, id2));
        assertAll(
                () -> assertNotNull(saved),
                () -> assertEquals(2, saved.size()),
                () -> assertEquals(List.of(c1), saved.get(id1.getId())),
                () -> assertEquals(List.of(c2), saved.get(id2.getId())),
                () -> assertFalse(saved.containsKey(id3.getId()))
        );
    }),

    DO_NOT_USE_COMMUNICATION_LIMIT_AS_A_KEY(dao -> {
        LocalDateTime now = LocalDateTime.of(2021, 8, 20, 15, 55, 0);
        ContactAttempt before = new ContactAttempt("777666555", 5);
        Communication savedCommunication = new Communication(now, "promo", "buy_fishos");
        dao.addMany(Map.of(before, savedCommunication));

        ContactAttempt after = new ContactAttempt("777666555", 1);
        Map<String, List<Communication>> result = dao.get(List.of(after));
        assertEquals(Map.of(after.getId(), List.of(savedCommunication)), result);
    }),

    DO_NOT_ALLOW_EMPTY_BATCH_GET(dao -> {
        assertThrows(IllegalArgumentException.class, () -> dao.get(List.of()));
    }),

    DO_NOT_ALLOW_EMPTY_BATCH_INSERT(dao -> {
        assertThrows(IllegalArgumentException.class, () -> dao.addMany(Map.of()));
    });

    private final Consumer<CommunicationsDAO> test;

    CommunicationsDAOContract(Consumer<CommunicationsDAO> test) {
        this.test = test;
    }

    public void verify(CommunicationsDAO dao) {
        test.accept(dao);
    }

    private static ContactAttempt attempt(String id) {
        return new ContactAttempt(id, 3);
    }
}
