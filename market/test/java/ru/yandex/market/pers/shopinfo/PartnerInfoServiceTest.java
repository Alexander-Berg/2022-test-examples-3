package ru.yandex.market.pers.shopinfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.pers.shopinfo.model.ShopInfo;
import ru.yandex.market.pers.shopinfo.model.SupplierInfo;
import ru.yandex.market.pers.shopinfo.test.context.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты для {@link PartnerInfoService} (магазинов и поставщиков).
 */
class PartnerInfoServiceTest extends FunctionalTest {
    @Autowired
    @Qualifier("shopPartnerInfoService")
    private PartnerInfoService<ShopInfo> shopPartnerInfoService;

    @Autowired
    @Qualifier("supplierPartnerInfoService")
    private PartnerInfoService<SupplierInfo> supplierPartnerInfoService;

    @Test
    @DbUnitDataSet(before = "controller/shopinfo/shops.csv")
    void testGetShops() {
        final Map<Long, ShopInfo> data = new HashMap<>();
        shopPartnerInfoService.loadPartnersInfo(data);
        assertThat(data.keySet()).hasSameElementsAs(Set.of(774L, 775L));

        //Убеждаемся, что из всей возможной юридической информации вернулась последняя, а не первая
        ShopInfo shop774 = data.get(774L);
        assertThat(shop774.getShopJurId()).isEqualTo("17743");
        assertThat(shop774.getName()).isEqualTo("NameAfterSecondEdit");
    }

    @Test
    @DbUnitDataSet(before = "controller/supplier/suppliers.csv")
    void testGetSuppliers() {
        final Map<Long, SupplierInfo> data = new HashMap<>();
        supplierPartnerInfoService.loadPartnersInfo(data);
        assertThat(data.keySet()).hasSameElementsAs(Set.of(774L, 775L, 776L, 777L));

        //контакты общения с покупателем из supplier_return_contact
        final SupplierInfo shop774 = data.get(774L);
        assertThat(shop774.getWorkSchedule()).isEqualTo("workSchedule1");
        assertThat(shop774.getShopPhoneNumber()).isEqualTo("79991111111");
        assertThat(shop774.getContactAddress()).isEqualTo("contactAddress1");
    }
}
