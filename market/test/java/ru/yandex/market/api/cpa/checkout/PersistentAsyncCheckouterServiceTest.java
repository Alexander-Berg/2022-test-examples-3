package ru.yandex.market.api.cpa.checkout;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.api.cpa.CPADataPusher;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PersistentAsyncCheckouterServiceTest extends FunctionalTest {
    @Autowired
    PersistentAsyncCheckouterService asyncCheckouterService;

    @Autowired
    CPADataPusher cpaDataPusher;

    @Test
    @DbUnitDataSet(after = "PersistentAsyncCheckouterServiceTest.pushPartnerSettingsToCheckout.after.csv")
    void pushPartnerSettingsToCheckout() {
        var ids = Set.of(100500L, 100501L, 100502L);
        asyncCheckouterService.pushPartnerSettingsToCheckout(ids);
        var idsCaptor = ArgumentCaptor.forClass(Long.class);
        verify(cpaDataPusher, times(ids.size())).pushShopInfoToCheckout(idsCaptor.capture());
        assertThat(idsCaptor.getAllValues()).containsExactlyInAnyOrderElementsOf(ids);
    }
}
