package ru.yandex.market.loyalty.core.utils;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import ru.yandex.market.loyalty.api.model.perk.PerkStatResponse;
import ru.yandex.market.loyalty.core.dao.ydb.model.UserOrder;
import ru.yandex.market.loyalty.core.model.CategoryTree;
import ru.yandex.market.loyalty.core.model.budgeting.Expectation;
import ru.yandex.market.loyalty.core.model.coupon.Coupon;
import ru.yandex.market.loyalty.core.model.promo.PromoParameterName;
import ru.yandex.market.loyalty.core.model.security.AdminUser;
import ru.yandex.market.loyalty.core.model.trigger.event.BaseTriggerEvent;
import ru.yandex.market.loyalty.core.model.trigger.EventParamName;
import ru.yandex.market.loyalty.core.model.trigger.RestrictionMatch;
import ru.yandex.market.loyalty.core.model.trigger.TriggerRestriction;
import ru.yandex.market.loyalty.core.rule.ClientDeviceTypeCuttingRule;
import ru.yandex.market.loyalty.core.service.ConfigParam;
import ru.yandex.market.loyalty.core.service.cashback.CashbackPromoImpl;
import ru.yandex.market.loyalty.core.service.cashback.ReportCashbackPromo;
import ru.yandex.market.loyalty.core.service.coin.SourceKey;
import ru.yandex.market.loyalty.core.service.discount.DiscountCalculationRequest;
import ru.yandex.market.loyalty.core.service.discount.PromoCalculation;
import ru.yandex.market.loyalty.core.service.perks.Perk;
import ru.yandex.market.loyalty.core.trigger.restrictions.TriggerRestrictionType;
import ru.yandex.market.loyalty.core.trigger.restrictions.actiononce.ActionOnceRestriction;
import ru.yandex.market.loyalty.core.trigger.restrictions.region.RegionRestriction;
import ru.yandex.market.loyalty.core.trigger.restrictions.segments.UserSegmentsRestriction;
import ru.yandex.market.loyalty.test.ToStringChecker;

import java.io.IOException;

import static ru.yandex.market.loyalty.test.ToStringChecker.checkToStringInSameModule;

public class CheckToStringTest {
    @Test
    public void checkToStringInCoreModule() throws IOException {
        checkToStringInSameModule(
                ToStringChecker.excludeFunctionInterfaces(),
                ToStringChecker.excludeSpringBeansByName(),
                ToStringChecker.excludeByClasses(
                        PromoParameterName.class,
                        EventParamName.class,
                        SourceKey.class,
                        TriggerRestriction.class,
                        TriggerRestrictionType.class,
                        RestrictionMatch.class,
                        Perk.class,
                        ConfigParam.class
                ),
                ToStringChecker.excludeByField(
                        Pair.of(AdminUser.class, "adminRoles"),
                        Pair.of(Expectation.class, "userExpectationConverter"),
                        Pair.of(Expectation.class, "userExpectationValidator"),
                        Pair.of(ClientDeviceTypeCuttingRule.class, "ruleContainer"),
                        Pair.of(Expectation.class, "globalDefaultValue"),
                        Pair.of(DiscountCalculationRequest.class, "perksOwnership"),
                        Pair.of(DiscountCalculationRequest.class, "extractedOwnership"),
                        Pair.of(DiscountCalculationRequest.class, "antifraudVerdict"),
                        Pair.of(DiscountCalculationRequest.class, "antifraudVerdictFuture"),
                        Pair.of(CategoryTree.Node.class, "children"),
                        Pair.of(CategoryTree.Node.class, "plane"),
                        Pair.of(CategoryTree.Node.class, "parent"),
                        Pair.of(RegionRestriction.class, "monitor"),
                        Pair.of(ActionOnceRestriction.class, "delegate"),
                        Pair.of(PromoCalculation.SuccessCoupon.class, "promo"),
                        Pair.of(UserSegmentsRestriction.class, "monitor"),
                        Pair.of(PerkStatResponse.class, "index"),
                        Pair.of(UserOrder.class, "userOrderBindingKey"),
                        Pair.of(ReportCashbackPromo.class, "billingInfoFactory"),
                        Pair.of(CashbackPromoImpl.class, "ruleFactory"),
                        Pair.of(CashbackPromoImpl.class, "billingInfoFactory"),
                        Pair.of(CashbackPromoImpl.class, "promoThresholdService"),
                        Pair.of(CashbackPromoImpl.class, "cashbackCacheService"),

                        //TODO sensitive data
                        Pair.of(BaseTriggerEvent.class, "params"),
                        Pair.of(Coupon.class, "params")
                )
        );
    }
}
