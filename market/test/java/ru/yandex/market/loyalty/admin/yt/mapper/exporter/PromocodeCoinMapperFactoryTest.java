package ru.yandex.market.loyalty.admin.yt.mapper.exporter;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import com.google.protobuf.ProtocolStringList;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.exclusions.ExcludedOffersService;
import ru.yandex.market.loyalty.core.dao.coin.CoinDao;
import ru.yandex.market.loyalty.core.model.coin.CoinProps;
import ru.yandex.market.loyalty.core.model.promo.BudgetSourceType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.rule.RuleContainer;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.rule.RulesContainer;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping;
import ru.yandex.market.loyalty.test.SameCollection;
import Market.Promo.Promo.PromoDetails;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.CATEGORY_ID;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.EXPERIMENTS;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MIN_ORDER_TOTAL;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MSKU_ID;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.NOT_DBS_SUPPLIER_FLAG_RESTRICTION;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.NOT_LOGIC;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.REGION_ID;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.SEGMENT;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.SUPPLIER_FLAG_RESTRICTION_TYPE;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.SUPPLIER_ID;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.VENDOR_ID;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.NOT_EXPORT_PREDICATES_TO_PROMOCODE_COIN;
import static ru.yandex.market.loyalty.core.service.discount.constants.SupplierFlagRestrictionType.EVERYTHING;
import static ru.yandex.market.loyalty.core.service.discount.constants.SupplierFlagRestrictionType.EVERYTHING_EXCEPT_EXPRESS;
import static ru.yandex.market.loyalty.core.service.discount.constants.SupplierFlagRestrictionType.EXPRESS_WAREHOUSE;
import static Market.Promo.Promo.BudgetSourceType.VENDOR;
import static Market.Promo.Promo.BudgetSourceType.BRAND;
import static Market.Promo.Promo.BudgetSourceType.MERCHANT;
import static Market.Promo.Promo.PromoDetails.newBuilder;
import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY;

public class PromocodeCoinMapperFactoryTest extends MarketLoyaltyAdminMockedDbTest {

    private static final String PROMOCODE = "some_promocode";
    private static final String MSKU_1 = "123";
    private static final String MSKU_2 = "1234";
    private static final String MSKU_3 = "1235";
    private static final int CATEGORY_1 = 1;
    private static final int CATEGORY_2 = 2;
    private static final long VENDOR_1 = 12;
    private static final long VENDOR_2 = 13;
    private static final long SUPPLIER_1 = 10;
    private static final long SUPPLIER_2 = 11;
    private static final int REGION_1 = 20;
    private static final int REGION_2 = 21;
    private static final int REGION_3 = 22;
    private static final int REGION_4 = 23;
    private static final int MIN_TOTAL = 2000;
    private static final String PARENT_PROMO_ID = "parent promo id";

    @Autowired
    private PromocodeCoinMapperFactory promocodeCoinMapperFactory;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CoinDao coinDao;
    @Value("${market.loyalty.admin.url}")
    private String adminUrl;
    @Autowired
    private ExcludedOffersService excludedOffersService;
    @Autowired
    private ConfigurationService configurationService;

    @Test
    public void shouldMapPriority() {
        RulesContainer rc = new RulesContainer();

        rc.add(RuleContainer.builder(RuleType.MSKU_FILTER_RULE)
                .withParams(MSKU_ID, Set.of(
                        MSKU_1,
                        MSKU_2,
                        MSKU_3
                ))
                .build());

        Promo promo = promoManager.createPromocodePromo(SmartShopping.defaultFixedPromocode()
                .setGlobalPriority(100)
                .setRulesContainer(rc)
                .setCode(PROMOCODE));

        CoinProps coinProps = coinDao.getCoinPropsByIds(Set.of(promo.getCoinPropsId()))
                .get(promo.getCoinPropsId())
                .orElseThrow();

        PromoDetailsMapper detailsMapper = promocodeCoinMapperFactory.createMapper(Map.of(promo, coinProps));

        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();

        assertThat(promoDetails.getPriority(), comparesEqualTo(100));
    }

    @Test
    public void shouldMapSourceParameters() {
        RulesContainer rc = new RulesContainer();

        rc.add(RuleContainer.builder(RuleType.MSKU_FILTER_RULE)
                .withParams(MSKU_ID, Set.of(
                        MSKU_1,
                        MSKU_2,
                        MSKU_3
                ))
                .build());

        Promo promo = promoManager.createPromocodePromo(SmartShopping.defaultFixedPromocode()
                .setRulesContainer(rc)
                .setCode(PROMOCODE));

        CoinProps coinProps = coinDao.getCoinPropsByIds(Set.of(promo.getCoinPropsId()))
                .get(promo.getCoinPropsId())
                .orElseThrow();

        PromoDetailsMapper detailsMapper = promocodeCoinMapperFactory.createMapper(Map.of(promo, coinProps));

        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();

        assertThat(promoDetails.getSourceType(), is(LOYALTY));
        assertThat(promoDetails.getSourceReference(), is(adminUrl + "promo/promocode/" + promo.getId()));
    }

    @Test
    public void shouldMapMarketSku() {
        RulesContainer rc = new RulesContainer();

        rc.add(RuleContainer.builder(RuleType.MSKU_FILTER_RULE)
                .withParams(MSKU_ID, Set.of(
                        MSKU_1,
                        MSKU_2,
                        MSKU_3
                ))
                .build());

        Promo promo = promoManager.createPromocodePromo(SmartShopping.defaultFixedPromocode()
                .setRulesContainer(rc)
                .setCode(PROMOCODE));

        CoinProps coinProps = coinDao.getCoinPropsByIds(Set.of(promo.getCoinPropsId()))
                .get(promo.getCoinPropsId())
                .orElseThrow();

        PromoDetailsMapper detailsMapper = promocodeCoinMapperFactory.createMapper(Map.of(promo, coinProps));

        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();

        assertThat(promoDetails.getOffersMatchingRulesList(), hasSize(1));
        assertThat(promoDetails.getOffersMatchingRulesList(), hasItem(allOf(
                hasProperty("categoryRestriction", allOf(
                        hasProperty("categoriesList", empty())
                )),
                hasProperty("suppliers", hasProperty("idsList", empty())),
                hasProperty("excludedSuppliers", hasProperty("idsList",
                        SameCollection.sameCollectionInAnyOrder(excludedOffersService.getExcluded().getSuppliers()))),
                hasProperty("vendors", hasProperty("idsList", empty())),
                hasProperty("excludedVendors", hasProperty("idsList", empty())),
                hasProperty("excludedMskus", hasProperty("idsList", empty())),
                hasProperty("mskus", hasProperty("idsList", hasItems(
                        Long.parseLong(MSKU_1),
                        Long.parseLong(MSKU_2),
                        Long.parseLong(MSKU_3)
                )))
        )));
    }

    @Test
    public void shouldMapExcludedMarketSku() {
        RulesContainer rc = new RulesContainer();

        rc.add(RuleContainer.builder(RuleType.MSKU_FILTER_RULE)
                .withParams(NOT_LOGIC, Collections.singleton(true))
                .withParams(MSKU_ID, Set.of(
                        MSKU_1,
                        MSKU_2,
                        MSKU_3
                ))
                .build());

        Promo promo = promoManager.createPromocodePromo(SmartShopping.defaultFixedPromocode()
                .setRulesContainer(rc)
                .setCode(PROMOCODE));

        CoinProps coinProps = coinDao.getCoinPropsByIds(Set.of(promo.getCoinPropsId()))
                .get(promo.getCoinPropsId())
                .orElseThrow();

        PromoDetailsMapper detailsMapper = promocodeCoinMapperFactory.createMapper(Map.of(promo, coinProps));

        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();

        assertThat(promoDetails.getOffersMatchingRulesList(), hasSize(1));
        assertThat(promoDetails.getOffersMatchingRulesList(), hasItem(allOf(
                hasProperty("categoryRestriction", allOf(
                        hasProperty("categoriesList", empty())
                )),
                hasProperty("suppliers", hasProperty("idsList", empty())),
                hasProperty("excludedSuppliers", hasProperty("idsList",
                        SameCollection.sameCollectionInAnyOrder(excludedOffersService.getExcluded().getSuppliers()))),
                hasProperty("vendors", hasProperty("idsList", empty())),
                hasProperty("excludedVendors", hasProperty("idsList", empty())),
                hasProperty("mskus", hasProperty("idsList", empty())),
                hasProperty("excludedMskus", hasProperty("idsList", hasItems(
                        Long.parseLong(MSKU_1),
                        Long.parseLong(MSKU_2),
                        Long.parseLong(MSKU_3)
                )))
        )));
    }

    @Test
    public void shouldMapCategories() {
        RulesContainer rc = new RulesContainer();

        rc.add(RuleContainer.builder(RuleType.CATEGORY_FILTER_RULE)
                .withParams(CATEGORY_ID, Set.of(
                        CATEGORY_1,
                        CATEGORY_2
                ))
                .build());

        Promo promo = promoManager.createPromocodePromo(SmartShopping.defaultFixedPromocode()
                .setRulesContainer(rc)
                .setCode(PROMOCODE));

        CoinProps coinProps = coinDao.getCoinPropsByIds(Set.of(promo.getCoinPropsId()))
                .get(promo.getCoinPropsId())
                .orElseThrow();

        PromoDetailsMapper detailsMapper = promocodeCoinMapperFactory.createMapper(Map.of(promo, coinProps));

        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();

        assertThat(promoDetails.getOffersMatchingRulesList(), hasSize(1));
        assertThat(promoDetails.getOffersMatchingRulesList(), hasItem(allOf(
                hasProperty("categoryRestriction", allOf(
                        hasProperty("categoriesList", hasItems(
                                (long) CATEGORY_1,
                                (long) CATEGORY_2
                        ))
                )),
                hasProperty("suppliers", hasProperty("idsList", empty())),
                hasProperty("excludedSuppliers", hasProperty("idsList",
                        SameCollection.sameCollectionInAnyOrder(excludedOffersService.getExcluded().getSuppliers()))),
                hasProperty("vendors", hasProperty("idsList", empty())),
                hasProperty("excludedVendors", hasProperty("idsList", empty())),
                hasProperty("excludedMskus", hasProperty("idsList", empty())),
                hasProperty("mskus", hasProperty("idsList", empty()))
        )));
    }

    @Test
    public void shouldMapExcludedCategories() {
        RulesContainer rc = new RulesContainer();

        rc.add(RuleContainer.builder(RuleType.CATEGORY_FILTER_RULE)
                .withParams(NOT_LOGIC, Collections.singleton(true))
                .withParams(CATEGORY_ID, Set.of(
                        CATEGORY_1,
                        CATEGORY_2
                ))
                .build());

        Promo promo = promoManager.createPromocodePromo(SmartShopping.defaultFixedPromocode()
                .setRulesContainer(rc)
                .setCode(PROMOCODE));

        CoinProps coinProps = coinDao.getCoinPropsByIds(Set.of(promo.getCoinPropsId()))
                .get(promo.getCoinPropsId())
                .orElseThrow();

        PromoDetailsMapper detailsMapper = promocodeCoinMapperFactory.createMapper(Map.of(promo, coinProps));

        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();

        assertThat(promoDetails.getOffersMatchingRulesList(), hasSize(1));
        assertThat(promoDetails.getOffersMatchingRulesList(), hasItem(allOf(
                hasProperty("categoryRestriction", allOf(
                        hasProperty("excludedCategoriesList", hasItems(
                                (long) CATEGORY_1,
                                (long) CATEGORY_2
                        )),
                        hasProperty("categoriesList", empty())
                )),
                hasProperty("suppliers", hasProperty("idsList", empty())),
                hasProperty("excludedSuppliers", hasProperty("idsList",
                        SameCollection.sameCollectionInAnyOrder(excludedOffersService.getExcluded().getSuppliers()))),
                hasProperty("vendors", hasProperty("idsList", empty())),
                hasProperty("excludedVendors", hasProperty("idsList", empty())),
                hasProperty("mskus", hasProperty("idsList", empty())),
                hasProperty("excludedMskus", hasProperty("idsList", empty()))
        )));
    }

    @Test
    public void shouldMapVendors() {
        RulesContainer rc = new RulesContainer();

        rc.add(RuleContainer.builder(RuleType.VENDOR_FILTER_RULE)
                .withParams(VENDOR_ID, Set.of(
                        VENDOR_1,
                        VENDOR_2
                ))
                .build());

        Promo promo = promoManager.createPromocodePromo(SmartShopping.defaultFixedPromocode()
                .setRulesContainer(rc)
                .setCode(PROMOCODE));

        CoinProps coinProps = coinDao.getCoinPropsByIds(Set.of(promo.getCoinPropsId()))
                .get(promo.getCoinPropsId())
                .orElseThrow();

        PromoDetailsMapper detailsMapper = promocodeCoinMapperFactory.createMapper(Map.of(promo, coinProps));

        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();

        assertThat(promoDetails.getOffersMatchingRulesList(), hasSize(1));
        assertThat(promoDetails.getOffersMatchingRulesList(), hasItem(allOf(
                hasProperty("categoryRestriction", allOf(
                        hasProperty("categoriesList", empty())
                )),
                hasProperty("suppliers", hasProperty("idsList", empty())),
                hasProperty("excludedSuppliers", hasProperty("idsList",
                        SameCollection.sameCollectionInAnyOrder(excludedOffersService.getExcluded().getSuppliers()))),
                hasProperty("vendors", hasProperty("idsList", hasItems(
                        VENDOR_1,
                        VENDOR_2
                ))),
                hasProperty("excludedVendors", hasProperty("idsList", empty())),
                hasProperty("excludedMskus", hasProperty("idsList", empty())),
                hasProperty("mskus", hasProperty("idsList", empty()))
        )));
    }

    @Test
    public void shouldMapExcludedVendors() {
        RulesContainer rc = new RulesContainer();

        rc.add(RuleContainer.builder(RuleType.VENDOR_FILTER_RULE)
                .withParams(NOT_LOGIC, Collections.singleton(true))
                .withParams(VENDOR_ID, Set.of(
                        VENDOR_1,
                        VENDOR_2
                ))
                .build());

        Promo promo = promoManager.createPromocodePromo(SmartShopping.defaultFixedPromocode()
                .setRulesContainer(rc)
                .setCode(PROMOCODE));

        CoinProps coinProps = coinDao.getCoinPropsByIds(Set.of(promo.getCoinPropsId()))
                .get(promo.getCoinPropsId())
                .orElseThrow();

        PromoDetailsMapper detailsMapper = promocodeCoinMapperFactory.createMapper(Map.of(promo, coinProps));

        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();

        assertThat(promoDetails.getOffersMatchingRulesList(), hasSize(1));
        assertThat(promoDetails.getOffersMatchingRulesList(), hasItem(allOf(
                hasProperty("categoryRestriction", allOf(
                        hasProperty("categoriesList", empty())
                )),
                hasProperty("suppliers", hasProperty("idsList", empty())),
                hasProperty("excludedSuppliers", hasProperty("idsList",
                        SameCollection.sameCollectionInAnyOrder(excludedOffersService.getExcluded().getSuppliers()))),
                hasProperty("vendors", hasProperty("idsList", empty())),
                hasProperty("excludedVendors", hasProperty("idsList", hasItems(
                        VENDOR_1,
                        VENDOR_2
                ))),
                hasProperty("mskus", hasProperty("idsList", empty())),
                hasProperty("excludedMskus", hasProperty("idsList", empty()))
        )));
    }

    @Test
    public void shouldMapSuppliers() {
        RulesContainer rc = new RulesContainer();

        rc.add(RuleContainer.builder(RuleType.SUPPLIER_FILTER_RULE)
                .withParams(SUPPLIER_ID, Set.of(
                        SUPPLIER_1,
                        SUPPLIER_2
                ))
                .build());

        Promo promo = promoManager.createPromocodePromo(SmartShopping.defaultFixedPromocode()
                .setRulesContainer(rc)
                .setCode(PROMOCODE));

        CoinProps coinProps = coinDao.getCoinPropsByIds(Set.of(promo.getCoinPropsId()))
                .get(promo.getCoinPropsId())
                .orElseThrow();

        PromoDetailsMapper detailsMapper = promocodeCoinMapperFactory.createMapper(Map.of(promo, coinProps));

        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();

        assertThat(promoDetails.getOffersMatchingRulesList(), hasSize(1));
        assertThat(promoDetails.getOffersMatchingRulesList(), hasItem(allOf(
                hasProperty("categoryRestriction", allOf(
                        hasProperty("categoriesList", empty())
                )),
                hasProperty("suppliers", hasProperty("idsList", hasItems(
                        SUPPLIER_1,
                        SUPPLIER_2
                ))),
                hasProperty("excludedSuppliers", hasProperty("idsList", empty())),
                hasProperty("vendors", hasProperty("idsList", empty())),
                hasProperty("excludedVendors", hasProperty("idsList", empty())),
                hasProperty("excludedMskus", hasProperty("idsList", empty())),
                hasProperty("mskus", hasProperty("idsList", empty()))
        )));
    }

    @Test
    public void shouldMapExcludedSuppliers() {
        RulesContainer rc = new RulesContainer();

        rc.add(RuleContainer.builder(RuleType.SUPPLIER_FILTER_RULE)
                .withParams(NOT_LOGIC, Collections.singleton(true))
                .withParams(SUPPLIER_ID, Set.of(
                        SUPPLIER_1,
                        SUPPLIER_2
                ))
                .build());

        Promo promo = promoManager.createPromocodePromo(SmartShopping.defaultFixedPromocode()
                .setRulesContainer(rc)
                .setCode(PROMOCODE));

        CoinProps coinProps = coinDao.getCoinPropsByIds(Set.of(promo.getCoinPropsId()))
                .get(promo.getCoinPropsId())
                .orElseThrow();

        PromoDetailsMapper detailsMapper = promocodeCoinMapperFactory.createMapper(Map.of(promo, coinProps));

        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();

        var expectedExcludedSuppliers = Sets.union(
                excludedOffersService.getExcluded().getSuppliers(), Set.of(SUPPLIER_1, SUPPLIER_2));
        assertThat(promoDetails.getOffersMatchingRulesList(), hasSize(1));
        assertThat(promoDetails.getOffersMatchingRulesList(), hasItem(allOf(
                hasProperty("categoryRestriction", allOf(
                        hasProperty("categoriesList", empty())
                )),
                hasProperty("suppliers", hasProperty("idsList", empty())),
                hasProperty("excludedSuppliers", hasProperty("idsList",
                        SameCollection.sameCollectionInAnyOrder(expectedExcludedSuppliers))),
                hasProperty("vendors", hasProperty("idsList", empty())),
                hasProperty("excludedVendors", hasProperty("idsList", empty())),
                hasProperty("mskus", hasProperty("idsList", empty())),
                hasProperty("excludedMskus", hasProperty("idsList", empty()))
        )));
    }

    @Test
    public void shouldMapRegions() {
        RulesContainer rc = new RulesContainer();

        rc.add(RuleContainer.builder(RuleType.ALLOWED_REGION_CUTTING_RULE)
                .withParams(REGION_ID, Set.of(
                        REGION_1,
                        REGION_2
                ))
                .build());
        rc.add(RuleContainer.builder(RuleType.FORBIDDEN_REGION_CUTTING_RULE)
                .withParams(REGION_ID, Set.of(
                        REGION_3,
                        REGION_4
                ))
                .build());
        rc.add(RuleContainer.builder(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE)
                .withParams(MIN_ORDER_TOTAL, Set.of(
                        BigDecimal.valueOf(MIN_TOTAL)
                ))
                .build());

        Promo promo = promoManager.createPromocodePromo(SmartShopping.defaultFixedPromocode()
                .setRulesContainer(rc)
                .setCode(PROMOCODE));

        CoinProps coinProps = coinDao.getCoinPropsByIds(Set.of(promo.getCoinPropsId()))
                .get(promo.getCoinPropsId())
                .orElseThrow();

        PromoDetailsMapper detailsMapper = promocodeCoinMapperFactory.createMapper(Map.of(promo, coinProps));

        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();

        assertThat(promoDetails.getRestrictions().getRegionRestriction().getRegionsList(), hasSize(2));
        assertThat(promoDetails.getRestrictions().getRegionRestriction().getExcludedRegionsList(), hasSize(2));
        assertThat(promoDetails.getRestrictions().getRegionRestriction().getRegionsList(), hasItems(
                REGION_1, REGION_2
        ));
        assertThat(promoDetails.getRestrictions().getRegionRestriction().getExcludedRegionsList(), hasItems(
                REGION_3, REGION_4
        ));
    }

    @Test
    public void shouldMapBudgetSources() {
        Promo promo = promoManager.createPromocodePromo(SmartShopping.defaultFixedPromocode()
                .setBudgetSources(List.of(BudgetSourceType.VENDOR, BudgetSourceType.BRAND, BudgetSourceType.MERCHANT))
                .setCode(PROMOCODE));

        CoinProps coinProps = coinDao.getCoinPropsByIds(Set.of(promo.getCoinPropsId()))
                .get(promo.getCoinPropsId())
                .orElseThrow();

        PromoDetailsMapper detailsMapper = promocodeCoinMapperFactory.createMapper(Map.of(promo, coinProps));

        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();

        assertThat(promoDetails.getBudgetSources().getBudgetSourceTypeList(), hasSize(3));
        assertThat(promoDetails.getBudgetSources().getBudgetSourceTypeList(), hasItems(
                VENDOR, BRAND, MERCHANT));
    }

    @Test
    public void shouldMapEverythingExceptExpressSupplierFlagRestriction() {
        RulesContainer rc = new RulesContainer();

        rc.add(RuleContainer.builder(RuleType.SUPPLIER_FLAG_RESTRICTION_FILTER_RULE)
                .withSingleParam(SUPPLIER_FLAG_RESTRICTION_TYPE, EVERYTHING_EXCEPT_EXPRESS)
                .build());

        Promo promo = promoManager.createPromocodePromo(SmartShopping.defaultFixedPromocode()
                .setRulesContainer(rc)
                .setCode(PROMOCODE));

        CoinProps coinProps = coinDao.getCoinPropsByIds(Set.of(promo.getCoinPropsId()))
                .get(promo.getCoinPropsId())
                .orElseThrow();

        PromoDetailsMapper detailsMapper = promocodeCoinMapperFactory.createMapper(Map.of(promo, coinProps));

        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();

        assertThat(promoDetails.getOffersMatchingRulesList(), hasSize(1));
        PromoDetails.OffersMatchingRule.SupplierFlagRestriction supplierFlagRestriction =
                promoDetails.getOffersMatchingRulesList().get(0).getSupplierFlagRestriction();
        Assert.assertThat(supplierFlagRestriction.getExcludedSupplierFlags(), is(2L));
        Assert.assertThat(supplierFlagRestriction.getSupplierFlags(), is(0L));
    }

    @Test
    public void shouldMapExpressSupplierFlagRestriction() {
        RulesContainer rc = new RulesContainer();

        rc.add(RuleContainer.builder(RuleType.SUPPLIER_FLAG_RESTRICTION_FILTER_RULE)
                .withSingleParam(SUPPLIER_FLAG_RESTRICTION_TYPE, EXPRESS_WAREHOUSE)
                .build());

        Promo promo = promoManager.createPromocodePromo(SmartShopping.defaultFixedPromocode()
                .setRulesContainer(rc)
                .setCode(PROMOCODE));

        CoinProps coinProps = coinDao.getCoinPropsByIds(Set.of(promo.getCoinPropsId()))
                .get(promo.getCoinPropsId())
                .orElseThrow();

        PromoDetailsMapper detailsMapper = promocodeCoinMapperFactory.createMapper(Map.of(promo, coinProps));

        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();

        assertThat(promoDetails.getOffersMatchingRulesList(), hasSize(1));
        PromoDetails.OffersMatchingRule.SupplierFlagRestriction supplierFlagRestriction =
                promoDetails.getOffersMatchingRulesList().get(0).getSupplierFlagRestriction();
        Assert.assertThat(supplierFlagRestriction.getExcludedSupplierFlags(), is(0L));
        Assert.assertThat(supplierFlagRestriction.getSupplierFlags(), is(2L));
    }

    @Test
    public void shouldMapEverythingSupplierFlagRestriction() {
        RulesContainer rc = new RulesContainer();

        rc.add(RuleContainer.builder(RuleType.SUPPLIER_FLAG_RESTRICTION_FILTER_RULE)
                .withSingleParam(SUPPLIER_FLAG_RESTRICTION_TYPE, EVERYTHING)
                .build());

        Promo promo = promoManager.createPromocodePromo(SmartShopping.defaultFixedPromocode()
                .setRulesContainer(rc)
                .setCode(PROMOCODE));

        CoinProps coinProps = coinDao.getCoinPropsByIds(Set.of(promo.getCoinPropsId()))
                .get(promo.getCoinPropsId())
                .orElseThrow();

        PromoDetailsMapper detailsMapper = promocodeCoinMapperFactory.createMapper(Map.of(promo, coinProps));

        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();

        assertThat(promoDetails.getOffersMatchingRulesList(), hasSize(1));
        PromoDetails.OffersMatchingRule.SupplierFlagRestriction supplierFlagRestriction =
                promoDetails.getOffersMatchingRulesList().get(0).getSupplierFlagRestriction();
        Assert.assertThat(supplierFlagRestriction.getExcludedSupplierFlags(), is(0L));
        Assert.assertThat(supplierFlagRestriction.getSupplierFlags(), is(1L));
    }

    @Test
    public void shouldMapSupplierFlagRestrictionIsNotExist() {
        RulesContainer rc = new RulesContainer();

        Promo promo = promoManager.createPromocodePromo(SmartShopping.defaultFixedPromocode()
                .setRulesContainer(rc)
                .setCode(PROMOCODE));

        CoinProps coinProps = coinDao.getCoinPropsByIds(Set.of(promo.getCoinPropsId()))
                .get(promo.getCoinPropsId())
                .orElseThrow();

        PromoDetailsMapper detailsMapper = promocodeCoinMapperFactory.createMapper(Map.of(promo, coinProps));

        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();

        assertThat(promoDetails.getOffersMatchingRulesList(), hasSize(1));
        PromoDetails.OffersMatchingRule.SupplierFlagRestriction supplierFlagRestriction =
                promoDetails.getOffersMatchingRulesList().get(0).getSupplierFlagRestriction();
        Assert.assertThat(supplierFlagRestriction.getExcludedSupplierFlags(), is(0L));
        Assert.assertThat(supplierFlagRestriction.getSupplierFlags(), is(0L));
    }

    @Test
    public void shouldMapNotDbsSupplierFlagRestriction() {
        RulesContainer rc = new RulesContainer();

        rc.add(RuleContainer.builder(RuleType.DBS_SUPPLIER_FLAG_RESTRICTION_FILTER_RULE)
                .withSingleParam(NOT_DBS_SUPPLIER_FLAG_RESTRICTION, Boolean.TRUE)
                .build());

        Promo promo = promoManager.createPromocodePromo(SmartShopping.defaultFixedPromocode()
                .setRulesContainer(rc)
                .setCode(PROMOCODE));

        CoinProps coinProps = coinDao.getCoinPropsByIds(Set.of(promo.getCoinPropsId()))
                .get(promo.getCoinPropsId())
                .orElseThrow();

        PromoDetailsMapper detailsMapper = promocodeCoinMapperFactory.createMapper(Map.of(promo, coinProps));

        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();

        assertThat(promoDetails.getOffersMatchingRulesList(), hasSize(1));
        PromoDetails.OffersMatchingRule.SupplierFlagRestriction supplierFlagRestriction =
                promoDetails.getOffersMatchingRulesList().get(0).getSupplierFlagRestriction();
        Assert.assertThat(supplierFlagRestriction.getExcludedSupplierFlags(), is(4L));
        Assert.assertThat(supplierFlagRestriction.getSupplierFlags(), is(0L));
    }

    @Test
    public void shouldMapParentPromoId() {

        Promo promo = promoManager.createPromocodePromo(SmartShopping.defaultFixedPromocode()
                .setParentPromoId(PARENT_PROMO_ID)
                .setCode(PROMOCODE));

        CoinProps coinProps = coinDao.getCoinPropsByIds(Set.of(promo.getCoinPropsId()))
                .get(promo.getCoinPropsId())
                .orElseThrow();

        PromoDetailsMapper detailsMapper = promocodeCoinMapperFactory.createMapper(Map.of(promo, coinProps));

        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();

        assertThat(promoDetails.getParentPromoId(), is(PARENT_PROMO_ID));
    }

    @Test
    public void shouldMapExperiments() {
        RulesContainer rc = new RulesContainer();
        rc.add(RuleContainer.builder(RuleType.EXPERIMENTS_CUTTING_RULE)
                .withSingleParam(EXPERIMENTS, "test_experiment_flag1=1;test_experiment_flag2")
                .build());

        Promo promo = promoManager.createPromocodePromo(SmartShopping.defaultFixedPromocode()
                .setRulesContainer(rc)
                .setCode(PROMOCODE));

        CoinProps coinProps = coinDao.getCoinPropsByIds(Set.of(promo.getCoinPropsId()))
                .get(promo.getCoinPropsId())
                .orElseThrow();

        PromoDetailsMapper detailsMapper = promocodeCoinMapperFactory.createMapper(Map.of(promo, coinProps));

        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();

        ProtocolStringList experimentRearrFlagsList =
                promoDetails.getRestrictions().getPredicates(0).getExperimentRearrFlagsList();
        assertThat(experimentRearrFlagsList, containsInAnyOrder("test_experiment_flag1=1", "test_experiment_flag2=1"));
    }

    @Test
    public void shouldNotMapExperimentsWhenConfiguaration() {
        configurationService.enable(NOT_EXPORT_PREDICATES_TO_PROMOCODE_COIN);

        RulesContainer rc = new RulesContainer();
        rc.add(RuleContainer.builder(RuleType.EXPERIMENTS_CUTTING_RULE)
                .withSingleParam(EXPERIMENTS, "test_experiment_flag1=1;test_experiment_flag2")
                .build());

        Promo promo = promoManager.createPromocodePromo(SmartShopping.defaultFixedPromocode()
                .setRulesContainer(rc)
                .setCode(PROMOCODE));

        CoinProps coinProps = coinDao.getCoinPropsByIds(Set.of(promo.getCoinPropsId()))
                .get(promo.getCoinPropsId())
                .orElseThrow();

        PromoDetailsMapper detailsMapper = promocodeCoinMapperFactory.createMapper(Map.of(promo, coinProps));

        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();

        assertThat(promoDetails.getRestrictions().getPredicatesList(), empty());
    }

    @Test
    public void shouldMapDisableByDefault() {
        Promo promo = promoManager.createPromocodePromo(SmartShopping.defaultFixedPromocode()
                .setDisabledByDefault(true)
                .setCode(PROMOCODE));

        CoinProps coinProps = coinDao.getCoinPropsByIds(Set.of(promo.getCoinPropsId()))
                .get(promo.getCoinPropsId())
                .orElseThrow();

        PromoDetailsMapper detailsMapper = promocodeCoinMapperFactory.createMapper(Map.of(promo, coinProps));

        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();

        assertThat(promoDetails.getDisabledByDefault(), is(true));
    }

    @Test
    public void shouldMapUserSegment() {
        RulesContainer rc = new RulesContainer();
        rc.add(RuleContainer.builder(RuleType.ALLOWED_SEGMENT_CUTTING_RULE)
                .withSingleParam(SEGMENT, "perk1")
                .build());
        rc.add(RuleContainer.builder(RuleType.RESTRICTED_SEGMENT_CUTTING_RULE)
                .withSingleParam(SEGMENT, "perk2")
                .build());

        Promo promo = promoManager.createPromocodePromo(SmartShopping.defaultFixedPromocode()
                .setRulesContainer(rc)
                .setCode(PROMOCODE));

        CoinProps coinProps = coinDao.getCoinPropsByIds(Set.of(promo.getCoinPropsId()))
                .get(promo.getCoinPropsId())
                .orElseThrow();

        PromoDetailsMapper detailsMapper = promocodeCoinMapperFactory.createMapper(Map.of(promo, coinProps));

        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();

        ProtocolStringList experimentRearrFlagsList =
                promoDetails.getRestrictions().getPredicates(0).getPerksList();
        assertThat(experimentRearrFlagsList, containsInAnyOrder("perk1", "!perk2"));
    }
}
