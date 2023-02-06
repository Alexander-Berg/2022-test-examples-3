package ru.yandex.market.crm.platform.reducers;

import java.util.List;

import javax.annotation.Nullable;

import org.junit.Test;

import ru.yandex.market.crm.platform.YieldMock;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.PushTokenStatuses;
import ru.yandex.market.crm.platform.models.PushTokenStatuses.TokenStatus;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author apershukov
 */
public class PushTokenStatusesReducerTest {

    private static PushTokenStatuses fact(TokenStatus... statuses) {
        return PushTokenStatuses.newBuilder()
                .setUid(Uids.create(UidType.MM_DEVICE_ID_HASH, "8025896208285132860"))
                .setAppId("1389598")
                .addAllStatuses(List.of(statuses))
                .build();
    }

    private static TokenStatus tokenStatus(String type, long version, boolean isValid) {
        return TokenStatus.newBuilder()
                .setType(type)
                .setVersion(version)
                .setIsValid(isValid)
                .build();
    }

    private final PushTokenStatusesReducer reducer = new PushTokenStatusesReducer();

    /**
     * Если для приложения нет информации об активных токенах добавляется новая запись
     */
    @Test
    public void testSaveNewTokenStatus() {
        PushTokenStatuses statuses = fact(tokenStatus("1", 123, true));
        PushTokenStatuses result = reduce(null, List.of(statuses));
        assertEquals(statuses, result);
    }

    /**
     * Если для токена того же типа приходит обновление статуса, хранимые поля токена обновляются
     */
    @Test
    public void testInvalidateKnownToken() {
        PushTokenStatuses stored = fact(tokenStatus("1", 123, true));
        PushTokenStatuses newStatus = fact(tokenStatus("1", 123, false));

        PushTokenStatuses result = reduce(stored, List.of(newStatus));
        assertEquals(newStatus, result);
    }

    /**
     * Если у приложения уже есть активный токен и для того же девайса приходит токен другого типа,
     * статус нового токена сохраняется рядом со старым
     */
    @Test
    public void testProcessAnotherTypeTokenUpdate() {
        PushTokenStatuses stored = fact(tokenStatus("1", 123, true));
        PushTokenStatuses newStatus = fact(tokenStatus("2", 321, true));

        PushTokenStatuses result = reduce(stored, List.of(newStatus));

        PushTokenStatuses expected = fact(
                tokenStatus("1", 123, true),
                tokenStatus("2", 321, true)
        );

        assertEquals(expected, result);
    }

    /**
     * Обновление статуса токена игнорируется если оно принадлежит токену предыдущей версии
     */
    @Test
    public void testIgnoreEarlyVersionStatusUpdate() {
        PushTokenStatuses stored = fact(tokenStatus("1", 321, true));
        PushTokenStatuses newStatus = fact(tokenStatus("1", 123, false));

        PushTokenStatuses result = reduce(stored, List.of(newStatus));

        assertEquals(stored, result);
    }

    /**
     * Если для невалидного токена пришло событие о выдаче токена той же версии это событие игнорируется
     * т. к. событие инвализации просто добралось до Платформы раньше.
     */
    @Test
    public void testDoNotReviveInvalidatedToken() {
        PushTokenStatuses stored = fact(tokenStatus("1", 123, false));
        PushTokenStatuses newStatus = fact(tokenStatus("1", 123, true));

        PushTokenStatuses result = reduce(stored, List.of(newStatus));

        assertEquals(stored, result);
    }

    private PushTokenStatuses reduce(@Nullable PushTokenStatuses stored, List<PushTokenStatuses> newStatuses) {
        YieldMock collector = new YieldMock();
        reducer.reduce(stored == null ? List.of() : List.of(stored), newStatuses, collector);
        List<PushTokenStatuses> added = List.copyOf(collector.getAdded("PushTokenStatuses"));
        assertThat(added, hasSize(1));
        return added.get(0);
    }
}
