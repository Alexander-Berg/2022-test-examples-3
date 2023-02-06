package ru.yandex.market.logistics.nesu.service;

import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.model.entity.Shop;
import ru.yandex.market.logistics.nesu.service.sender.SenderService;

@DatabaseSetup("/service/sender/before/get_senders.xml")
class SenderServiceTest extends AbstractContextualTest {

    @Autowired
    private SenderService senderService;

    @Test
    @DisplayName("Поиск магазинов по идентификаторам сендеров")
    void findShopIdsBySenderIds() {
        List<Shop> shops = senderService.findShopsBySenderIds(Set.of(1L, 2L));
        softly.assertThat(shops).hasSize(1);
        softly.assertThat(shops.get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Поиск магазинов по идентификаторам сендеров (пустой список)")
    void findShopIdsBySenderIdsEmpty() {
        List<Shop> shops = senderService.findShopsBySenderIds(Set.of());
        softly.assertThat(shops).hasSize(0);
    }
}
