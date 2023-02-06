package ru.yandex.market.loyalty.admin.yt.mapper.exporter;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.api.model.UsageClientDeviceType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.PromoParameterName;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.rule.RuleContainer;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.cashback.CashbackDetailsGroupService;
import ru.yandex.market.loyalty.core.service.discount.constants.SupplierFlagRestrictionType;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import Market.Promo.Promo.PromoDetails;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.CATEGORY_ID;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.CLIENT_PLATFORM;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MSKU_ID;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.NOT_LOGIC;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.SUPPLIER_ID;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.VENDOR_ID;
import static ru.yandex.market.loyalty.core.service.cashback.CashbackDetailsGroupService.DEFAULT_CASHBACK_DETAILS_CART_GROUP_NAME;
import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY;
import static Market.Promo.Promo.PromoDetails.newBuilder;

public class CashbackMapperFactoryTest extends MarketLoyaltyAdminMockedDbTest {

    private static final String MSKU_1 = "123";
    private static final String MSKU_2 = "1234";
    private static final String MSKU_3 = "1235";
    private static final int CATEGORY_1 = 1;
    private static final int CATEGORY_2 = 2;
    private static final long VENDOR_1 = 12;
    private static final long VENDOR_2 = 13;
    private static final long SUPPLIER_1 = 151L;
    private static final String PARENT_PROMO_ID = "parent promo id";

    @Autowired
    private CashbackMapperFactory cashbackMapperFactory;
    @Autowired
    private PromoManager promoManager;

    @Test
    public void shouldMapSourceParameters() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.ZERO)
                .addCashbackRule(RuleContainer.builder(RuleType.MSKU_FILTER_RULE)
                        .withParams(MSKU_ID, Set.of(
                                MSKU_1,
                                MSKU_2,
                                MSKU_3
                        ))));

        PromoDetailsMapper detailsMapper = cashbackMapperFactory.createMapper(List.of(promo));

        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();

        assertThat(promoDetails.getSourceType(), is(LOYALTY));
    }

    @Test
    public void shouldMapMarketSku() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.ZERO)
                .addCashbackRule(RuleContainer.builder(RuleType.MSKU_FILTER_RULE)
                        .withParams(MSKU_ID, Set.of(
                                MSKU_1,
                                MSKU_2,
                                MSKU_3
                        )))
                .addCashbackRule(RuleContainer.builder(RuleType.SUPPLIER_FILTER_RULE)
                        .withParams(SUPPLIER_ID, Set.of(SUPPLIER_1))));

        PromoDetailsMapper detailsMapper = cashbackMapperFactory.createMapper(List.of(promo));

        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();

        assertThat(promoDetails.getOffersMatchingRulesList(), hasSize(1));
        assertThat(promoDetails.getOffersMatchingRulesList(), hasItem(allOf(
                hasProperty("categoryRestriction", allOf(
                        hasProperty("categoriesList", empty())
                )),
                hasProperty("suppliers", hasProperty("idsList", hasItems(SUPPLIER_1))),
                hasProperty("excludedSuppliers", hasProperty("idsList", empty())),
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
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.ZERO)
                .addCashbackRule(RuleContainer.builder(RuleType.MSKU_FILTER_RULE)
                        .withParams(NOT_LOGIC, Collections.singleton(true))
                        .withParams(MSKU_ID, Set.of(
                                MSKU_1,
                                MSKU_2,
                                MSKU_3
                        )))
                .addCashbackRule(RuleContainer.builder(RuleType.SUPPLIER_FILTER_RULE)
                        .withParams(SUPPLIER_ID, Set.of(SUPPLIER_1))));

        PromoDetailsMapper detailsMapper = cashbackMapperFactory.createMapper(List.of(promo));

        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();

        assertThat(promoDetails.getOffersMatchingRulesList(), hasSize(1));
        assertThat(promoDetails.getOffersMatchingRulesList(), hasItem(allOf(
                hasProperty("categoryRestriction", allOf(
                        hasProperty("categoriesList", empty())
                )),
                hasProperty("suppliers", hasProperty("idsList", hasItems(SUPPLIER_1))),
                hasProperty("excludedSuppliers", hasProperty("idsList", empty())),
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
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.ZERO)
                .addCashbackRule(RuleContainer.builder(RuleType.CATEGORY_FILTER_RULE)
                        .withParams(CATEGORY_ID, Set.of(
                                CATEGORY_1,
                                CATEGORY_2
                        )))
                .addCashbackRule(RuleContainer.builder(RuleType.SUPPLIER_FILTER_RULE)
                        .withParams(SUPPLIER_ID, Set.of(SUPPLIER_1))));

        PromoDetailsMapper detailsMapper = cashbackMapperFactory.createMapper(List.of(promo));

        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();

        assertThat(promoDetails.getOffersMatchingRulesList(), hasSize(1));
        assertThat(promoDetails.getOffersMatchingRulesList(), hasItem(allOf(
                hasProperty("categoryRestriction", allOf(
                        hasProperty("categoriesList", hasItems(
                                (long) CATEGORY_1,
                                (long) CATEGORY_2
                        ))
                )),
                hasProperty("suppliers", hasProperty("idsList", hasItems(SUPPLIER_1))),
                hasProperty("excludedSuppliers", hasProperty("idsList", empty())),
                hasProperty("vendors", hasProperty("idsList", empty())),
                hasProperty("excludedVendors", hasProperty("idsList", empty())),
                hasProperty("excludedMskus", hasProperty("idsList", empty())),
                hasProperty("mskus", hasProperty("idsList", empty()))
        )));
    }

    @Test
    public void shouldMapExcludedCategories() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.ZERO)
                .addCashbackRule(RuleContainer.builder(RuleType.CATEGORY_FILTER_RULE)
                        .withParams(NOT_LOGIC, Collections.singleton(true))
                        .withParams(CATEGORY_ID, Set.of(
                                CATEGORY_1,
                                CATEGORY_2
                        )))
                .addCashbackRule(RuleContainer.builder(RuleType.SUPPLIER_FILTER_RULE)
                        .withParams(SUPPLIER_ID, Set.of(SUPPLIER_1))));

        PromoDetailsMapper detailsMapper = cashbackMapperFactory.createMapper(List.of(promo));

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
                hasProperty("suppliers", hasProperty("idsList", hasItems(SUPPLIER_1))),
                hasProperty("excludedSuppliers", hasProperty("idsList", empty())),
                hasProperty("vendors", hasProperty("idsList", empty())),
                hasProperty("excludedVendors", hasProperty("idsList", empty())),
                hasProperty("mskus", hasProperty("idsList", empty())),
                hasProperty("excludedMskus", hasProperty("idsList", empty()))
        )));
    }

    @Test
    public void shouldMapVendors() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.ZERO)
                .addCashbackRule(RuleContainer.builder(RuleType.VENDOR_FILTER_RULE)
                        .withParams(VENDOR_ID, Set.of(
                                VENDOR_1,
                                VENDOR_2
                        )))
                .addCashbackRule(RuleContainer.builder(RuleType.SUPPLIER_FILTER_RULE)
                        .withParams(SUPPLIER_ID, Set.of(SUPPLIER_1))));

        PromoDetailsMapper detailsMapper = cashbackMapperFactory.createMapper(List.of(promo));

        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();

        assertThat(promoDetails.getOffersMatchingRulesList(), hasSize(1));
        assertThat(promoDetails.getOffersMatchingRulesList(), hasItem(allOf(
                hasProperty("categoryRestriction", allOf(
                        hasProperty("categoriesList", empty())
                )),
                hasProperty("suppliers", hasProperty("idsList", hasItems(SUPPLIER_1))),
                hasProperty("excludedSuppliers", hasProperty("idsList", empty())),
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
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.ZERO)
                .addCashbackRule(RuleContainer.builder(RuleType.VENDOR_FILTER_RULE)
                        .withParams(NOT_LOGIC, Collections.singleton(true))
                        .withParams(VENDOR_ID, Set.of(
                                VENDOR_1,
                                VENDOR_2
                        )))
                .addCashbackRule(RuleContainer.builder(RuleType.SUPPLIER_FILTER_RULE)
                        .withParams(SUPPLIER_ID, Set.of(SUPPLIER_1))));

        PromoDetailsMapper detailsMapper = cashbackMapperFactory.createMapper(List.of(promo));

        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();

        assertThat(promoDetails.getOffersMatchingRulesList(), hasSize(1));
        assertThat(promoDetails.getOffersMatchingRulesList(), hasItem(allOf(
                hasProperty("categoryRestriction", allOf(
                        hasProperty("categoriesList", empty())
                )),
                hasProperty("suppliers", hasProperty("idsList", hasItems(SUPPLIER_1))),
                hasProperty("excludedSuppliers", hasProperty("idsList", empty())),
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
    @SuppressWarnings("rawtypes")
    public void shouldMapUiPromoFlags() {
        List<String> uiPromoFlags = Arrays.asList("flag_1", "flag_2", "flag_3");
        Map<PromoParameterName, Object> paramsMap = new HashMap<>();
        paramsMap.put(PromoParameterName.UI_PROMO_FLAGS, String.join(",", uiPromoFlags));
        Promo promo = promoManager.createCashbackPromoWithParams(
                PromoUtils.Cashback.defaultPercent(BigDecimal.ZERO), paramsMap);
        PromoDetailsMapper detailsMapper = cashbackMapperFactory.createMapper(List.of(promo));
        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();
        assertEquals(promoDetails.getUiPromoTagsCount(), 3);
        assertEquals(promoDetails.getUiPromoTags(0), "flag_1");
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void shouldMapCmsSemanticId() {
        String semanticId = "default_cashback";
        Map<PromoParameterName, Object> paramsMap = new HashMap<>();
        paramsMap.put(PromoParameterName.CMS_DESCRIPTION_SEMANTIC_ID, semanticId);
        Promo promo = promoManager.createCashbackPromoWithParams(
                PromoUtils.Cashback.defaultPercent(BigDecimal.ZERO), paramsMap);
        PromoDetailsMapper detailsMapper = cashbackMapperFactory.createMapper(List.of(promo));
        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();
        assertEquals(promoDetails.getCmsDescriptionSemanticId(), semanticId);
    }

    @Test
    public void shouldMapExperimentFlags() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.ZERO)
                .addCashbackRule(RuleContainer.builder(RuleType.EXPERIMENTS_CUTTING_RULE)
                        .withSingleParam(RuleParameterName.EXPERIMENTS, "test1=1;test2")
                ));
        PromoDetailsMapper detailsMapper = cashbackMapperFactory.createMapper(List.of(promo));
        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();
        assertThat(promoDetails.getBlueCashback().getPredicatesList().get(0).getExperimentRearrFlagsList(),
                contains(equalTo("test1=1"), equalTo("test2=1")));
    }

    @Test
    public void shouldMapExpressFlag() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.ZERO)
                .addCashbackRule(RuleContainer.builder(RuleType.SUPPLIER_FLAG_RESTRICTION_FILTER_RULE)
                        .withSingleParam(RuleParameterName.SUPPLIER_FLAG_RESTRICTION_TYPE,
                                SupplierFlagRestrictionType.EXPRESS_WAREHOUSE)
                ));
        PromoDetailsMapper detailsMapper = cashbackMapperFactory.createMapper(List.of(promo));
        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();
        assertTrue(promoDetails.getBlueCashback().getOnlyForExpressOffers());
    }

    @Test
    public void shouldMapParentPromoId() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.ZERO)
                .setParentPromoId(PARENT_PROMO_ID));
        PromoDetailsMapper detailsMapper = cashbackMapperFactory.createMapper(List.of(promo));
        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();
        assertThat(promoDetails.getParentPromoId(), is(PARENT_PROMO_ID));
    }

    @Test
    public void shouldMapAllowedClientDevice() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.ZERO)
                .addCashbackRule(RuleContainer.builder(RuleType.CLIENT_PLATFORM_CUTTING_RULE)
                        .withParams(CLIENT_PLATFORM, Set.of(UsageClientDeviceType.ALICE))));

        PromoDetailsMapper detailsMapper = cashbackMapperFactory.createMapper(List.of(promo));

        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();

        assertThat(promoDetails.getUserDeviceTypesList(), hasSize(1));
    }

    @Test
    public void shouldMapPriority() {
        int directPriority = 77;
        int reversePriority = -77;
        var promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultFixed(BigDecimal.TEN)
                        .setPriority(directPriority));
        PromoDetailsMapper detailsMapper = cashbackMapperFactory.createMapper(List.of(promo));
        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();

        assertThat(promoDetails.getSameTypePriority(), is(directPriority));
        assertThat(promoDetails.getBlueCashback().getPriority(), is(reversePriority));
    }

    @Test
    public void shouldExportAnaplanIdIfExists() {
        final String ANAPLAN_ID = "#10101";
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.ZERO)
                .addCashbackRule(RuleContainer.builder(RuleType.CLIENT_PLATFORM_CUTTING_RULE)
                        .withParams(CLIENT_PLATFORM, Set.of(UsageClientDeviceType.ALICE)))
                .setAnaplanId(ANAPLAN_ID)
        );

        PromoDetailsMapper detailsMapper = cashbackMapperFactory.createMapper(List.of(promo));

        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();

        assertThat(promoDetails.getAnaplanPromoId(), equalTo(ANAPLAN_ID));
    }

    @Test
    public void shouldNullAnaplanIdIfNotExists() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.ZERO)
                .addCashbackRule(RuleContainer.builder(RuleType.CLIENT_PLATFORM_CUTTING_RULE)
                        .withParams(CLIENT_PLATFORM, Set.of(UsageClientDeviceType.ALICE)))
        );

        PromoDetailsMapper detailsMapper = cashbackMapperFactory.createMapper(List.of(promo));

        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();

        assertThat(promoDetails.getAnaplanPromoId(), equalTo(StringUtils.EMPTY));
    }



    @Test
    public void shouldMapCashbackDetailsGroupNameId() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.ZERO)
                .setCashbackDetailsCartGroupName("default"));
        PromoDetailsMapper detailsMapper = cashbackMapperFactory.createMapper(List.of(promo));
        PromoDetails promoDetails = detailsMapper.mapPromo(newBuilder(), promo).orElseThrow().build();
        assertThat(promoDetails.getBlueCashback().getDetailsGroupKey(), is("default"));
        assertThat(promoDetails.getBlueCashback().getDetailsGroupName(), is("Стандартный кешбэк"));
    }
}
