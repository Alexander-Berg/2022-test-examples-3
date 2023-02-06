package ru.yandex.market.balance;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link SyncAgencyUsersExecutor}.
 */
@DbUnitDataSet(
        before = "SyncAgencyUsersExecutorTest.before.csv",
        after = "SyncAgencyUsersExecutorTest.after.csv")
public class SyncAgencyUsersExecutorTest extends FunctionalTest {

    @Autowired
    @Qualifier("balanceContactService")
    private BalanceContactService balanceContactService;

    @Autowired
    private SyncAgencyUsersExecutor syncAgencyUsersExecutor;

    @Test
    void testSync() {
        //пользователей не было и появились
        when(balanceContactService.getUidsByClient(eq(100L))).thenReturn(List.of(10L, 20L));
        //пользователи были, но всех удалили
        when(balanceContactService.getUidsByClient(eq(200L))).thenReturn(List.of());
        //добавился один пользователь
        when(balanceContactService.getUidsByClient(eq(300L))).thenReturn(List.of(50L, 60L));
        //удалился один пользователь
        when(balanceContactService.getUidsByClient(eq(400L))).thenReturn(List.of(70L));
        //один пользователь добавился один удалился
        when(balanceContactService.getUidsByClient(eq(500L))).thenReturn(List.of(100L));
        //полностью поменялись пользователи
        when(balanceContactService.getUidsByClient(eq(600L))).thenReturn(List.of(130L, 140L));
        //пользователи не поменялись
        when(balanceContactService.getUidsByClient(eq(700L))).thenReturn(List.of(150L, 160L));
        //пользователь был на агентстве 900, а сейчас на агентстве 800
        when(balanceContactService.getUidsByClient(eq(800L))).thenReturn(List.of(170L));
        when(balanceContactService.getUidsByClient(eq(900L))).thenReturn(List.of());

        syncAgencyUsersExecutor.doJob(null);
    }
}
