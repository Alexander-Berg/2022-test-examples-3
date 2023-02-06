package ru.yandex.market.wms.autostart.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.autostart.AutostartIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

class PromoOrderServiceTest extends AutostartIntegrationTest {

    @Autowired
    private PromoOrderService promoOrderService;


    @Test
    @DatabaseSetup("/service/promo/no-pick-skus.xml")
    void getPromoOrderKeysWhenNoPickSkus() {
        assertThat(promoOrderService.getPromoOrderKeys(null))
                .containsExactlyInAnyOrder("ORD0001", "ORD0002", "ORD0003");
    }

    @Test
    @DatabaseSetup("/service/promo/non-promo-orders-covers-expiring-pick-skus.xml")
    void getPromoOrderKeysWhenNonPromoOrdersCoversExpiringPickSkus() {
        assertThat(promoOrderService.getPromoOrderKeys(null))
                .containsExactlyInAnyOrder("ORD0001", "ORD0002", "ORD0003");
    }

    @Test
    @DatabaseSetup("/service/promo/non-promo-orders-does-not-cover-expiring-pick-skus.xml")
    void getPromoOrderKeysWhenNonPromoOrdersDoesNotCoverExpiringPickSkus() {
        assertThat(promoOrderService.getPromoOrderKeys(null))
                .containsExactlyInAnyOrder("ORD0002");
    }

    @Test
    @DatabaseSetup("/service/promo/non-promo-orders-does-not-cover-expiring-pick-skus.xml")
    void getPromoOrderKeysWhenNoNonPromoOrdersToCoverExpiringPickSkus() {
        assertThat(promoOrderService.getPromoOrderKeys(null))
                .containsExactlyInAnyOrder("ORD0002");
    }




}
