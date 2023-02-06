package ru.yandex.market.wms.autostart.service;

import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderSelectionServiceTest extends IntegrationTest {
    @Autowired
    private OrderSelectionService orderSelectionService;

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/service/order-selection/setup.xml")
    void selectOrderKeysToRemoveFromWave() {
        final Set<String> orderKeysWave1 = orderSelectionService.selectNotReservedRealOrderKeysFromWave("WAVE001");
        assertEquals(Set.of("ORDER002", "ORDER001"), orderKeysWave1);

        final Set<String> orderKeysWave2 = orderSelectionService.selectNotReservedRealOrderKeysFromWave("WAVE002");
        assertEquals(Set.of("ORDER024"), orderKeysWave2);
    }

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/service/order-selection/big-withdrawal-setup.xml")
    void selectOrderKeysToRemoveFromBigWithdrawalWave() {
        final Set<String> orderKeysWave1 = orderSelectionService.selectNotReservedRealOrderKeysFromWave("WAVE001");
        assertEquals(Set.of("ORDER001"), orderKeysWave1);

        final Set<String> orderKeysWave2 = orderSelectionService.selectNotReservedRealOrderKeysFromWave("WAVE002");
        assertEquals(Set.of("ORDER021"), orderKeysWave2);
    }
}
