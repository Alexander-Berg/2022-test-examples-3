package ru.yandex.market.loyalty.core.utils;

import java.math.BigDecimal;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.yandex.market.loyalty.api.model.discount.MultiCartDiscountRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.applicability.PromoApplicabilityPolicy;
import ru.yandex.market.loyalty.core.service.discount.DiscountService;

import static ru.yandex.market.loyalty.api.model.bundle.util.BundleAdapterUtils.adaptFrom;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;

@Component
public class DiscountServiceTestingUtils {
    public static final Date ACTIVE_FROM_DATE = DateUtils.addDays(new Date(), -1);
    public static final Date ACTIVE_TO_DATE = DateUtils.addDays(new Date(), 10);

    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private DiscountService discountService;

    public static DiscountRequestBuilder discountRequest() {
        return DiscountRequestBuilder.builder(
                orderRequestBuilder()
                        .withOrderItem(
                                itemKey(DEFAULT_ITEM_KEY),
                                quantity(BigDecimal.valueOf(1_000_000)),
                                price(BigDecimal.valueOf(7))
                        )
                        .withOrderItem(
                                itemKey(ANOTHER_ITEM_KEY),
                                quantity(BigDecimal.valueOf(7)),
                                price(BigDecimal.valueOf(564))
                        )
                        .build()
        );
    }

    public MultiCartWithBundlesDiscountResponse spendDiscount(MultiCartDiscountRequest discountRequest) {
        return spendDiscountWithExperiments(discountRequest, null);
    }

    public MultiCartWithBundlesDiscountResponse spendDiscountWithExperiments(MultiCartDiscountRequest discountRequest,
                                                                             String experiments) {
        PromoApplicabilityPolicy applicabilityPolicy = configurationService.currentPromoApplicabilityPolicy();
        return discountService.spendDiscount(adaptFrom(discountRequest), applicabilityPolicy,
                experiments
        );
    }

    public MultiCartWithBundlesDiscountResponse calculateDiscounts(MultiCartDiscountRequest discountRequest) {
        PromoApplicabilityPolicy applicabilityPolicy = configurationService.currentPromoApplicabilityPolicy();
        return discountService.calculateDiscounts(adaptFrom(discountRequest), applicabilityPolicy, null);
    }

    public MultiCartWithBundlesDiscountResponse calculateDiscounts(
            MultiCartWithBundlesDiscountRequest discountRequest
    ) {
        PromoApplicabilityPolicy applicabilityPolicy = configurationService.currentPromoApplicabilityPolicy();
        return discountService.calculateDiscounts(discountRequest, applicabilityPolicy, null);
    }


}
