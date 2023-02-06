package ru.yandex.market.core.balance.cache;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.balance.model.ClientInfo;

import static org.assertj.core.api.Assertions.assertThat;

class DbBalanceBackupServiceFunctionalTest extends FunctionalTest {
    @Autowired
    DbBalanceBackupService balanceBackupService;

    @Test
    void putClients() {
        var ci1Before = new ClientInfo(1L, ClientType.OOO, true, 1L);
        balanceBackupService.putClients(List.of(ci1Before));

        var clientsBefore = balanceBackupService.getClients(Set.of(1L, 2L, 3L));
        assertThat(clientsBefore).isEqualTo(Map.of(1L, ci1Before));

        var ci1After = new ClientInfo(1L, ClientType.OAO, false, 0L);
        var ci2After = new ClientInfo(2L, ClientType.OAO, true, 100500L);
        balanceBackupService.putClients(List.of(ci1After, ci2After));

        var clients = balanceBackupService.getClients(Set.of(1L, 2L, 3L));
        assertThat(clients).isEqualTo(Map.of(1L, ci1After, 2L, ci2After));
    }
}
