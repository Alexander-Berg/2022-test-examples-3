package ru.yandex.market.loyalty.admin.tms;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.model.EnumWithCode;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.core.model.cashback.CashbackDetailsGroupDescriptor;
import ru.yandex.market.loyalty.core.model.promo.CashbackLevelType;
import ru.yandex.market.loyalty.core.model.promo.PromoParameterName;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.cashback.CashbackDetailsGroupService;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.loyalty.core.model.cashback.group.CashbackDetailsGroupHealthStatus.INCONSISTENT;
import static ru.yandex.market.loyalty.core.model.cashback.group.CashbackDetailsGroupHealthStatus.OK;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.Cashback.defaultFixed;

@TestFor(CashbackHealthCheckProcessor.class)
public class CashbackHealthCheckProcessorTest extends MarketLoyaltyAdminMockedDbTest {

    @Autowired
    private CashbackHealthCheckProcessor cashbackHealthCheckProcessor;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CashbackDetailsGroupService cashbackDetailsGroupService;


    @Before
    public void init() {
        Arrays.stream(DetailsGroup.values())
                .map(DetailsGroup::getCode)
                .forEach(g -> cashbackDetailsGroupService.createOrUpdateGroup(new CashbackDetailsGroupDescriptor(g, g)));
        cashbackDetailsGroupService.inactivateGroup(DetailsGroup.INACTIVE.getCode());

        // две промки с разными semantic_id
        promoManager.createCashbackPromoWithParams(defaultFixed(BigDecimal.TEN, CashbackLevelType.ITEM),
                Map.of(PromoParameterName.CASHBACK_DETAILS_CART_GROUP_NAME, DetailsGroup.DEFAULT.getCode(),
                        PromoParameterName.CMS_DESCRIPTION_SEMANTIC_ID, "electrolux-cashback15"));
        promoManager.createCashbackPromoWithParams(defaultFixed(BigDecimal.TEN, CashbackLevelType.ITEM),
                Map.of(PromoParameterName.CASHBACK_DETAILS_CART_GROUP_NAME, DetailsGroup.DEFAULT.getCode(),
                        PromoParameterName.CMS_DESCRIPTION_SEMANTIC_ID, "electrolux-cashback20"));
        // две промки с разными levelType
        promoManager.createCashbackPromoWithParams(defaultFixed(BigDecimal.TEN, CashbackLevelType.ITEM),
                Map.of(PromoParameterName.CASHBACK_DETAILS_CART_GROUP_NAME, DetailsGroup.EXTRA.getCode(),
                        PromoParameterName.CMS_DESCRIPTION_SEMANTIC_ID, "default-cashback"));
        promoManager.createCashbackPromoWithParams(defaultFixed(BigDecimal.TEN, CashbackLevelType.MULTI_ORDER),
                Map.of(PromoParameterName.CASHBACK_DETAILS_CART_GROUP_NAME, DetailsGroup.EXTRA.getCode(),
                        PromoParameterName.CMS_DESCRIPTION_SEMANTIC_ID, "default-cashback"));
        // валидная группа
        promoManager.createCashbackPromoWithParams(defaultFixed(BigDecimal.TEN, CashbackLevelType.ITEM),
                Map.of(PromoParameterName.CASHBACK_DETAILS_CART_GROUP_NAME, DetailsGroup.PARTNER_EXTRA.getCode(),
                        PromoParameterName.CMS_DESCRIPTION_SEMANTIC_ID, "same_semantic"));
        promoManager.createCashbackPromoWithParams(defaultFixed(BigDecimal.ONE, CashbackLevelType.ITEM),
                Map.of(PromoParameterName.CASHBACK_DETAILS_CART_GROUP_NAME, DetailsGroup.PARTNER_EXTRA.getCode(),
                        PromoParameterName.CMS_DESCRIPTION_SEMANTIC_ID, "same_semantic"));
        // одна промка без semantic_id
        promoManager.createCashbackPromoWithParams(defaultFixed(BigDecimal.TEN, CashbackLevelType.MULTI_ORDER),
                Map.of(PromoParameterName.CASHBACK_DETAILS_CART_GROUP_NAME, DetailsGroup.SINGLE_INCONSISTENT.getCode()));
        // промки в неактивной группе с разными semantic
        promoManager.createCashbackPromoWithParams(defaultFixed(BigDecimal.TEN, CashbackLevelType.ITEM),
                Map.of(PromoParameterName.CASHBACK_DETAILS_CART_GROUP_NAME, DetailsGroup.INACTIVE.getCode(),
                        PromoParameterName.CMS_DESCRIPTION_SEMANTIC_ID, "bosch-cashback-20"));
        promoManager.createCashbackPromoWithParams(defaultFixed(BigDecimal.TEN, CashbackLevelType.ITEM),
                Map.of(PromoParameterName.CASHBACK_DETAILS_CART_GROUP_NAME, DetailsGroup.INACTIVE.getCode(),
                        PromoParameterName.CMS_DESCRIPTION_SEMANTIC_ID, "bosch-cashback-25"));

        reloadPromoCache();
        cashbackDetailsGroupService.reloadCashbackDetailsGroupCache();
    }

    @Test
    public void test() {
        cashbackHealthCheckProcessor.checkCashbackDetailsGroupsHealth();
        cashbackDetailsGroupService.reloadCashbackDetailsGroupCache();

        Map<String, CashbackDetailsGroupDescriptor> detailsGroups = cashbackDetailsGroupService.getGroupCache();

        assertThat(detailsGroups.get(DetailsGroup.DEFAULT.getCode()).getHealthStatusReport(), is(INCONSISTENT));
        assertThat(detailsGroups.get(DetailsGroup.EXTRA.getCode()).getHealthStatusReport(), is(INCONSISTENT));
        assertThat(detailsGroups.get(DetailsGroup.PARTNER_EXTRA.getCode()).getHealthStatusReport(), is(OK));
        assertThat(detailsGroups.get(DetailsGroup.SINGLE_INCONSISTENT.getCode()).getHealthStatusReport(), is(INCONSISTENT));
        assertThat(detailsGroups.get(DetailsGroup.INACTIVE.getCode()), nullValue());
        assertThat(detailsGroups.get(DetailsGroup.EMPTY.getCode()).getHealthStatusReport(), is(OK));
    }


    private enum DetailsGroup implements EnumWithCode {
        DEFAULT("default"),
        EXTRA("extra"),
        PARTNER_EXTRA("partner_extra"),
        INACTIVE("inactive"),
        SINGLE_INCONSISTENT("single_inconsistent"),
        EMPTY("empty");

        private final String code;

        DetailsGroup(String code) {
            this.code = code;
        }

        @Nonnull
        @Override
        public String getCode() {
            return code;
        }
    }

}
