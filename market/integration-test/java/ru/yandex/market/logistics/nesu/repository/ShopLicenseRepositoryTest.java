package ru.yandex.market.logistics.nesu.repository;

import java.time.Instant;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.model.entity.ShopLicense;
import ru.yandex.market.logistics.nesu.model.entity.type.ShopLicenseType;

@DisplayName("Тест репорзитория shop_license")
class ShopLicenseRepositoryTest extends AbstractContextualTest {

    @Autowired
    ShopLicenseRepository shopLicenseRepository;

    @Test
    @DatabaseSetup("/repository/shop-license/before/basic.xml")
    void findAllByShopIdTest() {
        List<ShopLicense> licenses = shopLicenseRepository.findAllByShopId(123L);
        softly.assertThat(licenses.size()).isEqualTo(1);
        ShopLicense license = licenses.get(0);
        softly.assertThat(license.getShopId()).isEqualTo(123L);
        softly.assertThat(license.getLicenseType()).isEqualByComparingTo(ShopLicenseType.CAN_SELL_MEDICINE);
        softly.assertThat(license.getCreated()).isEqualTo(Instant.parse("2020-01-01T13:00:00Z"));
    }
}
