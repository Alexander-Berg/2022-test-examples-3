package ru.yandex.market.checkout.checkouter.promo.bundles.utils;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.order.promo.ReportPromoType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider.OrderBuilder;
import ru.yandex.market.checkout.util.CheckoutRequestUtils;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.loyalty.LoyaltyParameters;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.common.report.model.OfferPromo;
import ru.yandex.market.common.report.model.PromoDetails;

import static java.util.stream.Collectors.toList;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.shopSelfDelivery;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.yandexDelivery;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.FEED_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

public final class BlueGiftsOrderUtils {

    private BlueGiftsOrderUtils() {
    }

    public static OfferItemKey offerItemKey(String offerId) {
        return OfferItemKey.of(offerId, FEED_ID, null);
    }

    public static OfferItemKey offerItemKey(String offerId, String bundleId) {
        return OfferItemKey.of(offerId, FEED_ID, bundleId);
    }

    public static OrderBuilder orderWithYandexDelivery() {
        return OrderProvider.orderBuilder()
                .someLabel()
                .someBuyer()
                .stubApi()
                .shopId(SHOP_ID_WITH_SORTING_CENTER)
                .paymentMethod(PaymentMethod.YANDEX)
                .deliveryBuilder(yandexDelivery());
    }

    public static OrderBuilder orderWithShopDelivery() {
        return OrderProvider.orderBuilder()
                .someLabel()
                .someBuyer()
                .stubApi()
                .shopId(SHOP_ID_WITH_SORTING_CENTER)
                .paymentMethod(PaymentMethod.YANDEX)
                .deliveryBuilder(shopSelfDelivery());
    }

    public static Parameters fbyRequestFor(
            MultiCart multiCart,
            Collection<FoundOffer> offerToReport,
            Consumer<LoyaltyParameters> loyaltyConfigurerConsumer
    ) {
        return CheckoutRequestUtils.requestFor(multiCart, offerToReport, ShopSettingsHelper::getDefaultMeta, null, null,
                null, loyaltyConfigurerConsumer);
    }

    public static Parameters dropshipRequestFor(
            MultiCart multiCart,
            Collection<FoundOffer> offerToReport,
            Supplier<ShopMetaData> shopMetaDataSupplier,
            Consumer<LoyaltyParameters> loyaltyConfigurerConsumer
    ) {
        return CheckoutRequestUtils.shopRequestFor(multiCart, offerToReport.stream()
                        .map(FoundOfferBuilder::createFrom)
                        .peek(ob -> ob.isFulfillment(false)
                                .atSupplierWarehouse(true)
                                .supplierType(SupplierType.THIRD_PARTY)
                        )
                        .map(FoundOfferBuilder::build)
                        .collect(Collectors.toUnmodifiableList()),
                shopMetaDataSupplier, null,
                null, loyaltyConfigurerConsumer);
    }


    public static Parameters fbyRequestFor(
            MultiCart multiCart,
            String promo,
            Consumer<LoyaltyParameters> loyaltyConfigurerConsumer
    ) {
        return fbyRequestFor(multiCart, multiCart.getCarts().stream()
                .flatMap(o -> o.getItems().stream())
                .map(FoundOfferBuilder::createFrom)
                .peek(builder -> builder
                        .promoKey(promo)
                        .promoType(ReportPromoType.GENERIC_BUNDLE.getCode())
                        .promo(offerPromo(promo, ReportPromoType.GENERIC_BUNDLE)))
                .map(FoundOfferBuilder::build)
                .collect(toList()), loyaltyConfigurerConsumer);
    }

    @Nonnull
    public static OfferPromo offerPromo(@Nonnull String promoKey, @Nonnull ReportPromoType promoType) {
        PromoDetails pd = PromoDetails.builder()
                .promoKey(promoKey)
                .anaplanId(promoKey)
                .promoType(promoType.getCode())
                .build();
        OfferPromo offerPromo = new OfferPromo();
        offerPromo.setPromoMd5(pd.getPromoKey());
        offerPromo.setPromoType(pd.getPromoType());
        offerPromo.setPromoDetails(pd);
        return offerPromo;
    }
}
