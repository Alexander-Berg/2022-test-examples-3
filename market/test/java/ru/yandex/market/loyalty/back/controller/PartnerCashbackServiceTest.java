package ru.yandex.market.loyalty.back.controller;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.dao.CategoryTypeGroupReferenceDao;
import ru.yandex.market.loyalty.core.dao.CategoryTypeGroupTariffReferenceDao;
import ru.yandex.market.loyalty.core.dao.CategoryTypeReferenceDao;
import ru.yandex.market.loyalty.core.dao.PartnerCashbackStandardPromoDao;
import ru.yandex.market.loyalty.core.dao.PartnerCashbackVersionDao;
import ru.yandex.market.loyalty.core.model.cashback.partner.CategoryType;
import ru.yandex.market.loyalty.core.model.cashback.partner.MarketTariff;
import ru.yandex.market.loyalty.core.model.cashback.partner.StandardPromo;
import ru.yandex.market.loyalty.core.model.cashback.partner.entity.CategoryTypeEntry;
import ru.yandex.market.loyalty.core.model.cashback.partner.entity.CategoryTypeGroupEntry;
import ru.yandex.market.loyalty.core.model.cashback.partner.entity.CategoryTypeGroupTariffEntry;
import ru.yandex.market.loyalty.core.model.cashback.partner.entity.StandardPromoEntry;
import ru.yandex.market.loyalty.core.model.cashback.partner.entity.VersionEntry;
import ru.yandex.market.loyalty.core.service.cashback.PartnerCashbackService;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.loyalty.core.model.cashback.partner.entity.VersionStatus.ACTIVE;

public class PartnerCashbackServiceTest extends MarketLoyaltyBackMockedDbTestBase {
    @Autowired
    private PartnerCashbackService partnerCashbackService;
    @Autowired
    private PartnerCashbackVersionDao partnerCashbackVersionDao;
    @Autowired
    private PartnerCashbackStandardPromoDao partnerCashbackStandardPromoDao;
    @Autowired
    private CategoryTypeGroupReferenceDao categoryTypeGroupReferenceDao;
    @Autowired
    private CategoryTypeReferenceDao categoryTypeReferenceDao;
    @Autowired
    private CategoryTypeGroupTariffReferenceDao categoryTypeGroupTariffReferenceDao;

    @Autowired
    public void init() {
        final VersionEntry version = partnerCashbackVersionDao.save(
                VersionEntry.builder()
                        .setId(0)
                        .setStatus(ACTIVE)
                        .setComment("initial")
                        .setCustomCashbackPromoBucketName("test")
                        .setStandardCashbackPromoBucketName("test")
                        .setCustomCashbackPromoPriorityHigh(10000)
                        .setCustomCashbackPromoPriorityLow(0)
                        .build()
        );
        createStandardPromo(version, "default", 0, 1, 10, 0.9, 5, -199);
        createStandardPromo(version, "cehac", 1, 1, 25, 0.3, 1, -89);
        createStandardPromo(version, "diy", 2, 2, 25, 0.6, 2, -79);
    }

    @Test
    public void shouldFindParentTariff() {
        final List<MarketTariff> tariffs = partnerCashbackService.getMarketTariffs(
                partnerCashbackService.getPartnerCashbackCurrentVersionId(),
                Collections.singletonList(1000L)
        );
        assertEquals(1, tariffs.size());
        assertEquals(1000, tariffs.get(0).getHid());
        assertEquals("default", tariffs.get(0).getCategoryTypeGroupName());
        assertThat(tariffs.get(0).getMarketTariff(), comparesEqualTo(BigDecimal.valueOf(0.9)));
    }

    @Test
    public void shouldFindStandardPromos() {
        final List<StandardPromo> partnerCashbackStandardPromos =
                partnerCashbackService.getPartnerCashbackStandardPromos(
                partnerCashbackService.getPartnerCashbackCurrentVersionId()
        );
        assertEquals(3, partnerCashbackStandardPromos.size());
        assertEquals(List.of(new CategoryType(0, "category_0")), partnerCashbackStandardPromos.get(0).getCategories());
        assertThat(partnerCashbackStandardPromos.get(0).getDefaultCashbackNominal(),
                comparesEqualTo(BigDecimal.valueOf(5)));
        assertThat(partnerCashbackStandardPromos.get(0).getCodeName(), equalTo("default"));
        assertThat(partnerCashbackStandardPromos.get(0).getMinCashbackNominal(),
                comparesEqualTo(BigDecimal.valueOf(1)));
        assertThat(partnerCashbackStandardPromos.get(0).getMaxCashbackNominal(),
                comparesEqualTo(BigDecimal.valueOf(10)));
    }

    @Test
    public void shouldFindPromosMaximalPercentage() {
        partnerCashbackService.recalculatePromosMaximalPercentage();
        assertThat(partnerCashbackService.getPromosMaximalPercentage(), equalTo(BigDecimal.valueOf(5)));
    }


    private void createStandardPromo(VersionEntry version, String codeName, long hid, int minNominal, int maxNominal,
                                     double marketTariff, int defaultNominal, int priority) {
        categoryTypeGroupReferenceDao.save(
                CategoryTypeGroupEntry.builder()
                        .setName(codeName)
                        .build()
        );
        categoryTypeReferenceDao.save(
                CategoryTypeEntry.builder()
                        .setHid(hid)
                        .setCategoryTypeGroupName(codeName)
                        .build()
        );
        categoryTypeGroupTariffReferenceDao.save(
                CategoryTypeGroupTariffEntry.builder()
                        .setCategoryTypeGroupName(codeName)
                        .setDeleted(false)
                        .setMinCashbackNominal(BigDecimal.valueOf(minNominal))
                        .setMaxCashbackNominal(BigDecimal.valueOf(maxNominal))
                        .setPartnerCashbackVersionId(version.getId())
                        .setMarketTariff(BigDecimal.valueOf(marketTariff))
                        .build()
        );
        partnerCashbackStandardPromoDao.save(
                StandardPromoEntry.builder()
                        .setCodeName(codeName)
                        .setCategoryTypeReferenceGroupName(codeName)
                        .setDefaultCashbackNominal(BigDecimal.valueOf(defaultNominal))
                        .setDescription("")
                        .setPartnerCashbackVersionId(version.getId())
                        .setPriority(priority)
                        .build()
        );
    }

}
