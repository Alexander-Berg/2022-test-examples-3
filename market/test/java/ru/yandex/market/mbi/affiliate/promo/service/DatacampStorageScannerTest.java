package ru.yandex.market.mbi.affiliate.promo.service;


import java.time.Clock;
import java.time.LocalDate;

import Market.DataCamp.DataCampPromo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.mbi.affiliate.promo.TestResourceUtils;
import ru.yandex.market.mbi.affiliate.promo.api.server.dto.CatalogPromoType;
import ru.yandex.market.mbi.affiliate.promo.api.server.dto.DeviceType;
import ru.yandex.market.mbi.affiliate.promo.config.FunctionalTestConfig;
import ru.yandex.market.mbi.affiliate.promo.model.CatalogPromoEntity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FunctionalTestConfig.class)
public class DatacampStorageScannerTest {

    @Autowired
    private Clock clock;

    @Test
    public void testBluePromocode() throws Exception {
        var input = DataCampPromo.PromoDescription.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/catalog_blue_promocode.json", input);
        CatalogPromoEntity entity = new DatacampStorageScanner(
                null, null, null, clock)
                .toCatalogPromoEntity(input.build());
        assertThat(entity, is(new CatalogPromoEntity()
                .setId("#10797")
                .setPromoType(CatalogPromoType.PERCENT_PROMOCODE)
                .setStartDate(LocalDate.parse("2021-08-01"))
                .setEndDate(LocalDate.parse("2021-12-31"))
                .setDiscountValuePercent(30)
                .setPromocodeValue("CLUBMAM")
                .setFirstMarketOrderPromocode(false)
                .setOneOrderPromocode(true)
                .setDeviceType(DeviceType.APPLICATION)
        ));
    }

    @Test
    public void testMarketPromocode() throws Exception {
        var input = DataCampPromo.PromoDescription.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/catalog_market_promocode.json", input);
        CatalogPromoEntity entity = new DatacampStorageScanner(
                null, null, null, clock)
                .toCatalogPromoEntity(input.build());
        assertThat(entity, is(new CatalogPromoEntity()
                .setId("1233384_CMRMCPFZ")
                .setPromoType(CatalogPromoType.PERCENT_PROMOCODE)
                .setStartDate(LocalDate.parse("2021-08-01"))
                .setEndDate(LocalDate.parse("2021-12-31"))
                .setDiscountValuePercent(5)
                .setPromocodeValue("CMRMCPFZ")
                .setFirstMarketOrderPromocode(false)
                .setLandingUrl("http://market.yandex.ru/special/ccc777")
        ));
    }

    //@Test
    public void testPartnerStandardCashback() throws Exception {
        var input = DataCampPromo.PromoDescription.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/catalog_standard_cashback.json", input);
        CatalogPromoEntity entity = new DatacampStorageScanner(
                null, null, null, clock)
                .toCatalogPromoEntity(input.build());
        assertThat(entity, is(new CatalogPromoEntity()
                .setId("1244849_PSC_1633423523_auto")
                .setPromoType(CatalogPromoType.CASHBACK)
                .setStartDate(LocalDate.parse("2021-08-01"))
                .setEndDate(LocalDate.parse("2021-12-31"))
                .setCashbackValue(5)
        ));
    }

    //@Test
    public void testPartnerCustomCashback() throws Exception {
        var input = DataCampPromo.PromoDescription.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/catalog_custom_cashback.json", input);
        CatalogPromoEntity entity = new DatacampStorageScanner(
                null, null, null, clock)
                .toCatalogPromoEntity(input.build());
        assertThat(entity, is(new CatalogPromoEntity()
                .setId("569796_PCC_1635875005")
                .setPromoType(CatalogPromoType.CASHBACK)
                .setStartDate(LocalDate.parse("2021-08-01"))
                .setEndDate(LocalDate.parse("2021-12-31"))
                .setCashbackValue(5)
        ));
    }

    @Test
    public void testDiscount() throws Exception {
        var input = DataCampPromo.PromoDescription.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/catalog_discount.json", input);
        CatalogPromoEntity entity = new DatacampStorageScanner(
                null, null, null, clock)
                .toCatalogPromoEntity(input.build());
        assertThat(entity, is(new CatalogPromoEntity()
                .setId("#10002")
                .setPromoType(CatalogPromoType.DISCOUNT)
                .setStartDate(LocalDate.parse("2021-08-01"))
                .setEndDate(LocalDate.parse("2021-12-31"))
                .setDiscountValuePercent(44)
        ));
    }

    @Test
    public void testNisNPlusOne() throws Exception {
        var input = DataCampPromo.PromoDescription.newBuilder();
        TestResourceUtils.loadProto("ru/yandex/market/mbi/affiliate/promo/service/catalog_n_is_n_plus_1.json", input);
        CatalogPromoEntity entity = new DatacampStorageScanner(
                null, null, null, clock)
                .toCatalogPromoEntity(input.build());
        assertThat(entity, is(new CatalogPromoEntity()
                .setId("#10008")
                .setPromoType(CatalogPromoType.N_IS_N_PLUS_1)
                .setStartDate(LocalDate.parse("2021-08-01"))
                .setEndDate(LocalDate.parse("2021-12-31"))
                .setExtraItemFreeMinCount(3)
        ));
    }

}