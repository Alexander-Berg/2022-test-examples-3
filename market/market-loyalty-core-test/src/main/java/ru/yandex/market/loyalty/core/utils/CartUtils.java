package ru.yandex.market.loyalty.core.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.commons.codec.digest.DigestUtils;

import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.model.order.Item;
import ru.yandex.market.loyalty.core.model.order.ItemKey;
import ru.yandex.market.loyalty.core.service.discount.DiscountCalculationRequest.Cart;
import ru.yandex.market.loyalty.core.service.discount.PromoCalculationList;
import ru.yandex.market.loyalty.core.service.discount.SpendMode;

import static ru.yandex.market.loyalty.core.utils.BuildCustomizer.Util.customize;

public final class CartUtils {
    private CartUtils() {
    }

    @Nonnull
    public static String generateBundleId(
            @Nonnull PromoBundleDescription bundleDescription,
            long feedId,
            String... sskus
    ) {
        return DigestUtils.md5Hex(bundleDescription.getShopPromoId() +
                '-' + bundleDescription.getPromoKey() +
                '-' + Arrays.stream(sskus)
                .sorted()
                .map(offer -> String.join(":", String.valueOf(feedId),
                        offer
                ))
                .collect(Collectors.joining(",", "[", "]")));
    }

    @Nonnull
    @SafeVarargs
    public static Cart cart(@Nonnull BuildCustomizer<Cart, CartBuilder>... customizers) {
        return customize(CartBuilder::new, customizers).build();
    }

    public static class CartBuilder implements Builder<Cart> {
        private final List<Item> items = new ArrayList<>();
        private PaymentType paymentType;
        private CoreMarketPlatform platform;

        public CartBuilder withItem(Item item) {
            items.add(item);
            return this;
        }

        public CartBuilder withPaymentType(PaymentType paymentType) {
            this.paymentType = paymentType;
            return this;
        }

        public CartBuilder withPlatform(CoreMarketPlatform platform) {
            this.platform = platform;
            return this;
        }

        @SafeVarargs
        public static BuildCustomizer<Cart, CartBuilder> item(BuildCustomizer<Item, TestItemBuilder>... customizers) {
            return cb -> cb.withItem(customize(TestItemBuilder::new, customizers).build());
        }

        public Cart build() {
            return new Cart(
                    "",
                    0L,
                    paymentType,
                    null,
                    platform,
                    BigDecimal.ZERO,
                    0L,
                    items,
                    Collections.emptyList(),
                    SpendMode.SPEND,
                    PromoCalculationList.empty()
            );
        }
    }

    public static class TestItemBuilder extends Item.Builder implements Builder<Item> {

        public TestItemBuilder() {
            withQuantity(1);
        }

        @SafeVarargs
        public static Item item(BuildCustomizer<Item, TestItemBuilder>... customizers) {
            return customize(TestItemBuilder::new, customizers).build();
        }

        public TestItemBuilder withQuantity(Number quantity) {
            withQuantity(BigDecimal.valueOf(quantity.longValue()));
            return this;
        }

        public static BuildCustomizer<Item, TestItemBuilder> key(long feedId, String offerId) {
            return b -> b.withKey(ItemKey.ofFeedOffer(feedId, offerId));
        }

        public static BuildCustomizer<Item, TestItemBuilder> price(BigDecimal price) {
            return b -> b.withPrice(price);
        }

        public static BuildCustomizer<Item, TestItemBuilder> price(Number price) {
            return b -> b.withPrice(BigDecimal.valueOf(price.doubleValue()));
        }

        public static BuildCustomizer<Item, TestItemBuilder> quantity(Integer quantity) {
            return b -> b.withQuantity(quantity);
        }

        public static BuildCustomizer<Item, TestItemBuilder> promo(String... promoKeys) {
            return b -> b.withPromoKeys(Arrays.stream(promoKeys).collect(Collectors.toSet()));
        }
    }
}
