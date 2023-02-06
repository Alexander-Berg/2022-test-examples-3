package ru.yandex.market.loyalty.back.controller;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.partner.PartnerCashbackMarketTariffsResponse;
import ru.yandex.market.loyalty.api.model.partner.PartnerCashbackStandardPromosResponse;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;
import ru.yandex.market.loyalty.core.dao.CategoryTypeGroupReferenceDao;
import ru.yandex.market.loyalty.core.dao.CategoryTypeGroupTariffReferenceDao;
import ru.yandex.market.loyalty.core.dao.CategoryTypeReferenceDao;
import ru.yandex.market.loyalty.core.dao.PartnerCashbackStandardPromoDao;
import ru.yandex.market.loyalty.core.dao.PartnerCashbackVersionDao;
import ru.yandex.market.loyalty.core.model.cashback.partner.entity.CategoryTypeEntry;
import ru.yandex.market.loyalty.core.model.cashback.partner.entity.CategoryTypeGroupEntry;
import ru.yandex.market.loyalty.core.model.cashback.partner.entity.CategoryTypeGroupTariffEntry;
import ru.yandex.market.loyalty.core.model.cashback.partner.entity.StandardPromoEntry;
import ru.yandex.market.loyalty.core.model.cashback.partner.entity.VersionEntry;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.loyalty.core.model.cashback.partner.entity.VersionStatus.ACTIVE;

@TestFor(PartnerCashbackController.class)
public class PartnerCashbackControllerTest extends MarketLoyaltyBackMockedDbTestBase {
    @Autowired
    private MarketLoyaltyClient client;
    @Autowired
    private PartnerCashbackStandardPromoDao partnerCashbackStandardPromoDao;
    @Autowired
    private PartnerCashbackVersionDao partnerCashbackVersionDao;
    @Autowired
    private CategoryTypeGroupReferenceDao categoryTypeGroupReferenceDao;
    @Autowired
    private CategoryTypeReferenceDao categoryTypeReferenceDao;
    @Autowired
    private CategoryTypeGroupTariffReferenceDao categoryTypeGroupTariffReferenceDao;

    @Before
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
        categoryTypeGroupReferenceDao.save(
                CategoryTypeGroupEntry.builder()
                        .setName("default")
                        .build()
        );
        categoryTypeReferenceDao.save(
                CategoryTypeEntry.builder()
                        .setHid(1000)
                        .setCategoryTypeGroupName("default")
                        .build()
        );
        categoryTypeGroupTariffReferenceDao.save(
                CategoryTypeGroupTariffEntry.builder()
                        .setCategoryTypeGroupName("default")
                        .setDeleted(false)
                        .setMinCashbackNominal(BigDecimal.valueOf(1))
                        .setMaxCashbackNominal(BigDecimal.valueOf(10))
                        .setPartnerCashbackVersionId(version.getId())
                        .setMarketTariff(BigDecimal.valueOf(1.3))
                        .build()
        );
        partnerCashbackStandardPromoDao.save(StandardPromoEntry.builder()
                .setCodeName("default")
                .setCategoryTypeReferenceGroupName("default")
                .setDefaultCashbackNominal(BigDecimal.valueOf(5))
                .setDescription("")
                .setPartnerCashbackVersionId(version.getId())
                .setPriority(10)
                .build());
    }

    @Test
    public void shouldGetStandardPromos() {
        final PartnerCashbackStandardPromosResponse v1 =
                client.getPartnerStandardStandardPromos(null);
        final PartnerCashbackStandardPromosResponse v2 =
                client.getPartnerStandardStandardPromos(v1.getMarketTariffsVersionId());
        assertEquals(v1.getMarketTariffsVersionId(), v2.getMarketTariffsVersionId());
        assertEquals(v1.getPromos().size(), 1);
        assertEquals(v1.getPromos().get(0).getCodeName(), "default");
        assertThat(v1.getPromos().get(0).getDefaultCashbackNominal(), comparesEqualTo(BigDecimal.valueOf(5)));
        assertThat(v1.getPromos().get(0).getMarketTariff(), comparesEqualTo(BigDecimal.valueOf(1.3)));
        assertThat(v1.getPromos().get(0).getMaxCashbackNominal(), comparesEqualTo(BigDecimal.valueOf(10)));
        assertThat(v1.getPromos().get(0).getMinCashbackNominal(), comparesEqualTo(BigDecimal.valueOf(1)));
        assertThat(v1.getPromos().get(0).getCategories(),
                contains(
                        allOf(
                                hasProperty("hid", equalTo(1000L)),
                                hasProperty("name", equalTo("category_1000"))
                        )
                )
        );
        assertEquals(v2.getPromos().get(0).getCodeName(), "default");
        assertThat(v2.getPromos().get(0).getDefaultCashbackNominal(), comparesEqualTo(BigDecimal.valueOf(5)));
        assertThat(v2.getPromos().get(0).getMarketTariff(), comparesEqualTo(BigDecimal.valueOf(1.3)));
        assertThat(v2.getPromos().get(0).getMaxCashbackNominal(), comparesEqualTo(BigDecimal.valueOf(10)));
        assertThat(v2.getPromos().get(0).getMinCashbackNominal(), comparesEqualTo(BigDecimal.valueOf(1)));
        assertThat(v2.getPromos().get(0).getCategories(),
                contains(
                        allOf(
                                hasProperty("hid", equalTo(1000L)),
                                hasProperty("name", equalTo("category_1000"))
                        )
                )
        );
    }

    @Test
    public void shouldGetMarketTariffs() {
        final PartnerCashbackMarketTariffsResponse v1 = client.getMarketTariffs(null,
                Collections.singletonList(1000));
        final PartnerCashbackMarketTariffsResponse v2 =
                client.getMarketTariffs(v1.getMarketTariffsVersionId(), Collections.singletonList(1000));

        assertEquals(v1.getMarketTariffsVersionId(), v2.getMarketTariffsVersionId());

        assertEquals(v1.getTariffs().size(), 1);
        assertEquals(v1.getTariffs().get(0).getHid(), 1000);
        assertEquals("default", v1.getTariffs().get(0).getCategoryTypeGroupName());
        assertThat(v1.getTariffs().get(0).getMarketTariff(), comparesEqualTo(BigDecimal.valueOf(1.3)));
        assertThat(v1.getTariffs().get(0).getMinCashbackNominal(), comparesEqualTo(BigDecimal.valueOf(1)));
        assertThat(v1.getTariffs().get(0).getMaxCashbackNominal(), comparesEqualTo(BigDecimal.valueOf(10)));

        assertEquals(v2.getTariffs().size(), 1);
        assertEquals(v2.getTariffs().get(0).getHid(), 1000);
        assertEquals("default", v2.getTariffs().get(0).getCategoryTypeGroupName());
        assertThat(v2.getTariffs().get(0).getMarketTariff(), comparesEqualTo(BigDecimal.valueOf(1.3)));
        assertThat(v2.getTariffs().get(0).getMinCashbackNominal(), comparesEqualTo(BigDecimal.valueOf(1)));
        assertThat(v2.getTariffs().get(0).getMaxCashbackNominal(), comparesEqualTo(BigDecimal.valueOf(10)));
    }
}
