package ru.yandex.market.marketpromo.core.test.generator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Sets;
import org.apache.commons.codec.digest.DigestUtils;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import ru.yandex.market.marketpromo.model.BuildCustomizer;
import ru.yandex.market.marketpromo.model.CacheLocalOffer;
import ru.yandex.market.marketpromo.model.DatacampOffer;
import ru.yandex.market.marketpromo.model.DatacampOfferPromo;
import ru.yandex.market.marketpromo.model.InternalOffer;
import ru.yandex.market.marketpromo.model.InternalOffer.OfferBuilder;
import ru.yandex.market.marketpromo.model.LocalOffer;
import ru.yandex.market.marketpromo.model.LocalOfferPromo;
import ru.yandex.market.marketpromo.model.OfferDisabledSource;
import ru.yandex.market.marketpromo.model.OfferPromoBase;
import ru.yandex.market.marketpromo.model.PromoKey;
import ru.yandex.market.marketpromo.model.SupplierType;
import ru.yandex.market.marketpromo.model.WarehouseFeedKey;
import ru.yandex.market.marketpromo.utils.IdentityUtils;

import static ru.yandex.market.marketpromo.model.BuildCustomizer.Util.customize;
import static ru.yandex.market.marketpromo.model.BuildCustomizer.Util.mixin;
import static ru.yandex.misc.lang.StringUtils.randomString;

public final class Offers {

    public static long DEFAULT_BUSINESS_ID = 1;
    public static long DEFAULT_SHOP_ID = 2;
    public static long DEFAULT_WAREHOUSE_ID = 123;
    public static long DEFAULT_FEED_ID = 12;
    public static long DEFAULT_CATEGORY_ID = 124;
    public static long DEFAULT_VENDOR_ID = 125;

    private Offers() {
    }

    @Nonnull
    @SafeVarargs
    public static DatacampOffer datacampOffer(BuildCustomizer<DatacampOffer, DatacampOffer.OfferBuilder>... customizers) {
        return customize(DatacampOffer::builder, mixin(defaults(), mixin(
                b -> b.warehouseId(DEFAULT_WAREHOUSE_ID)
                        .feedId(DEFAULT_FEED_ID), customizers))
        ).build();
    }

    @Nonnull
    @SafeVarargs
    public static LocalOffer localOffer(BuildCustomizer<LocalOffer, LocalOffer.OfferBuilder>... customizers) {
        return customize(LocalOffer::builder, mixin(
                stocksByWarehouse(WarehouseFeedKey.of(DEFAULT_WAREHOUSE_ID, DEFAULT_FEED_ID), 1L),
                mixin(defaults(), customizers))
        ).build();
    }

    @Nonnull
    @SafeVarargs
    public static CacheLocalOffer cacheLocalOffer(
            BuildCustomizer<CacheLocalOffer,
                    CacheLocalOffer.OfferBuilder>... customizers
    ) {
        return customize(CacheLocalOffer::builder, mixin(
                stocksByWarehouseCache(WarehouseFeedKey.of(DEFAULT_WAREHOUSE_ID, DEFAULT_FEED_ID), 1L),
                mixin(defaults(), customizers))
        ).build();
    }

    @Nonnull
    public static <T extends InternalOffer, B extends OfferBuilder<T, B>> BuildCustomizer<T, B> defaults() {
        return b -> b.businessId(DEFAULT_BUSINESS_ID)
                .shopId(DEFAULT_SHOP_ID)
                .categoryId(DEFAULT_CATEGORY_ID)
                .vendorId(DEFAULT_VENDOR_ID)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now());
    }

    @Nonnull
    public static <T extends InternalOffer, B extends OfferBuilder<T, B>> BuildCustomizer<T, B>
    shopSku(@Nonnull String shopSku) {
        return b -> b.shopSku(shopSku).id(IdentityUtils.hashId(shopSku));
    }

    @Nonnull
    public static <T extends InternalOffer, B extends OfferBuilder<T, B>> BuildCustomizer<T, B>
    business(Number businessId) {
        return b -> b.businessId(businessId.longValue());
    }

    @Nonnull
    public static <T extends InternalOffer, B extends OfferBuilder<T, B>> BuildCustomizer<T, B>
    shop(Number shopId) {
        return b -> b.shopId(shopId.longValue());
    }

    @Nonnull
    public static <T extends InternalOffer, B extends OfferBuilder<T, B>> BuildCustomizer<T, B>
    marketSku(long msku) {
        return b -> b.marketSku(msku);
    }

    @Nonnull
    public static <T extends InternalOffer, B extends OfferBuilder<T, B>> BuildCustomizer<T, B>
    wareMd5(String wareMd5) {
        return b -> b.wareMd5(wareMd5);
    }

    @Nonnull
    public static <T extends InternalOffer, B extends OfferBuilder<T, B>> BuildCustomizer<T, B>
    categoryId(Number categoryId) {
        return b -> b.categoryId(categoryId == null ? null : categoryId.longValue());
    }

    @Nonnull
    public static <T extends InternalOffer, B extends OfferBuilder<T, B>> BuildCustomizer<T, B>
    vendorId(Number vendorId) {
        return b -> b.vendorId(vendorId == null ? null : vendorId.longValue());
    }

    @Nonnull
    public static BuildCustomizer<DatacampOffer, DatacampOffer.OfferBuilder> stocks(long stocks) {
        return b -> b.stocks(stocks);
    }

    @Nonnull
    public static BuildCustomizer<DatacampOffer, DatacampOffer.OfferBuilder> emptyStocks() {
        return b -> b.stocks(null);
    }

    @Nonnull
    public static BuildCustomizer<DatacampOffer, DatacampOffer.OfferBuilder> feed(Number feedId) {
        return b -> b.feedId(feedId.longValue());
    }

    @Nonnull
    public static <T extends InternalOffer, B extends OfferBuilder<T, B>> BuildCustomizer<T, B>
    name(@Nullable String name) {
        return b -> b.name(name);
    }

    @Nonnull
    public static <T extends InternalOffer, B extends OfferBuilder<T, B>> BuildCustomizer<T, B>
    price(@Nullable Number price) {
        return b -> b.price(price == null ? null : BigDecimal.valueOf(price.doubleValue()));
    }

    @Nonnull
    public static <T extends InternalOffer, B extends OfferBuilder<T, B>> BuildCustomizer<T, B>
    disabled(boolean disabled) {
        return b -> b.disabled(disabled);
    }

    @Nonnull
    public static BuildCustomizer<DatacampOffer, DatacampOffer.OfferBuilder> disabledSource(
            OfferDisabledSource disabledSource) {
        return b -> b.disabledSource(disabledSource);
    }

    @Nonnull
    public static <T extends InternalOffer, B extends OfferBuilder<T, B>> BuildCustomizer<T, B>
    basePrice(@Nonnull Number price) {
        return b -> b.basePrice(BigDecimal.valueOf(price.doubleValue()));
    }

    @Nonnull
    public static BuildCustomizer<DatacampOffer, DatacampOffer.OfferBuilder> warehouse(long warehouseId) {
        return b -> b.warehouseId(warehouseId);
    }

    @Nonnull
    public static BuildCustomizer<DatacampOffer, DatacampOffer.OfferBuilder> supplierType(SupplierType supplierType) {
        return b -> b.supplierType(supplierType);
    }

    @Nonnull
    public static BuildCustomizer<DatacampOffer, DatacampOffer.OfferBuilder> potentialPromo(@Nonnull String promoId,
                                                                                            @Nonnull BigDecimal basePrice) {
        return b -> b.potentialPromo(OfferPromoBase.builder()
                .id(promoId)
                .basePrice(basePrice)
                .updatedAt(LocalDateTime.now())
                .build());
    }

    @Nonnull
    public static BuildCustomizer<DatacampOffer, DatacampOffer.OfferBuilder> potentialPromo(@Nonnull String promoId) {
        return b -> b.potentialPromo(OfferPromoBase.builder()
                .id(promoId)
                .updatedAt(LocalDateTime.now())
                .build());
    }

    @Nonnull
    public static BuildCustomizer<DatacampOffer, DatacampOffer.OfferBuilder> potentialPromos(
            @Nonnull Set<OfferPromoBase> promos
    ) {
        return b -> b.potentialPromos(promos.stream()
                .collect(Collectors.toUnmodifiableMap(OfferPromoBase::getId,
                        Function.identity(),
                        (e1, e2) -> e1
                )));
    }

    @Nonnull
    public static BuildCustomizer<DatacampOffer, DatacampOffer.OfferBuilder> activePromos(
            DatacampOfferPromo... promoMechanics
    ) {
        return b -> Stream.of(promoMechanics).forEach(b::activePromo);
    }

    @Nonnull
    public static BuildCustomizer<DatacampOffer, DatacampOffer.OfferBuilder> activePromos(
            @Nonnull Set<DatacampOfferPromo> mechanics
    ) {
        return b -> mechanics.forEach(b::activePromo);
    }

    @Nonnull
    public static BuildCustomizer<LocalOffer, LocalOffer.OfferBuilder> stocksByWarehouse(
            @Nonnull WarehouseFeedKey warehouseFeedKey,
            @Nonnull Long count
    ) {
        return b -> b.stocksByWarehouse(warehouseFeedKey, count);
    }

    @Nonnull
    public static BuildCustomizer<CacheLocalOffer, CacheLocalOffer.OfferBuilder> stocksByWarehouseCache(
            @Nonnull WarehouseFeedKey warehouseFeedKey,
            @Nonnull Long count
    ) {
        return b -> b.stocksByWarehouse(warehouseFeedKey, count);
    }

    @Nonnull
    public static BuildCustomizer<LocalOffer, LocalOffer.OfferBuilder> promos(
            @Nonnull Collection<LocalOfferPromo> mechanics
    ) {
        return b -> b.promos(mechanics.stream().collect(Collectors.toUnmodifiableMap(p ->
                        PromoKey.of(p.getPromoBase().getId(), p.getType()),
                Function.identity(),
                (e1, e2) -> e1
        )));
    }

    @Nonnull
    public static BuildCustomizer<CacheLocalOffer, CacheLocalOffer.OfferBuilder> promosCache(
            @Nonnull Collection<LocalOfferPromo> mechanics
    ) {
        return b -> b.promos(mechanics.stream().collect(Collectors.toUnmodifiableMap(p ->
                        PromoKey.of(p.getPromoBase().getId(), p.getType()),
                Function.identity(),
                (e1, e2) -> e1
        )));
    }

    @Nonnull
    public static BuildCustomizer<LocalOffer, LocalOffer.OfferBuilder> promo(
            @Nonnull LocalOfferPromo offerPromoData
    ) {
        return b -> b.promo(PromoKey.of(offerPromoData.getPromoBase().getId(), offerPromoData.getType()),
                offerPromoData);
    }

    @Nonnull
    public static <T> Matcher<T> hasDefaults() {
        return Matchers.allOf(
                Matchers.hasProperty("businessId", Matchers.is(DEFAULT_BUSINESS_ID)),
                Matchers.hasProperty("shopId", Matchers.is(DEFAULT_SHOP_ID))
        );
    }

    @SafeVarargs
    @Nonnull
    public static List<DatacampOffer> generateDatacampOfferList(
            int size,
            BuildCustomizer<DatacampOffer, DatacampOffer.OfferBuilder>... customizers
    ) {
        return IntStream.range(1, size + 1)
                .mapToObj(someNumber -> datacampOffer(
                        name("offer " + someNumber + " " + randomString(50)),
                        shopSku("offer shop sku " + someNumber),
                        price(someNumber * 100),
                        potentialPromos(generatePromos(10)),
                        activePromos(
                                Sets.union(
                                        generateDatacampDDPromos(1, 3),
                                        generateDatacampCAGPromos(4, 3)
                                )
                        ),
                        warehouse(someNumber % 12 + 1),
                        mixin(List.of(customizers))
                )).collect(Collectors.toUnmodifiableList());
    }

    @SafeVarargs
    @Nonnull
    public static List<LocalOffer> generateLocalOfferList(
            int size,
            BuildCustomizer<LocalOffer, LocalOffer.OfferBuilder>... customizers
    ) {
        return IntStream.range(1, size + 1)
                .mapToObj(someNumber -> localOffer(
                        name("offer " + someNumber + randomString(50)),
                        shopSku("offer shop sku " + someNumber),
                        price(someNumber * 100),
                        promos(
                                Sets.union(
                                        generateLocalDDPromos(1, 3),
                                        generateLocalCAGPromos(4, 3)
                                )
                        ),
                        mixin(List.of(customizers))
                )).collect(Collectors.toUnmodifiableList());
    }

    @SafeVarargs
    @Nonnull
    public static List<CacheLocalOffer> generateCacheLocalOfferList(
            int size,
            BuildCustomizer<CacheLocalOffer, CacheLocalOffer.OfferBuilder>... customizers
    ) {
        return IntStream.range(1, size + 1)
                .mapToObj(someNumber -> cacheLocalOffer(
                        name("offer " + someNumber + randomString(50)),
                        shopSku("offer shop sku " + someNumber),
                        price(someNumber * 100),
                        promosCache(
                                Sets.union(
                                        generateLocalDDPromos(1, 3),
                                        generateLocalCAGPromos(4, 3)
                                )
                        )
                        //mixin(List.of(customizers))
                )).collect(Collectors.toUnmodifiableList());
    }

    @Nonnull
    public static Set<OfferPromoBase> generatePromos(int size) {
        return IntStream.range(1, size + 1)
                .mapToObj(someNumber -> OfferPromoBases.promoBase(
                        OfferPromoBases.id(IdentityUtils.hashId("promo " + someNumber)),
                        OfferPromoBases.basePrice(BigDecimal.TEN.multiply(BigDecimal.valueOf(someNumber)))
                )).collect(Collectors.toUnmodifiableSet());
    }

    @Nonnull
    public static Set<DatacampOfferPromo> generateDatacampDDPromos(int start, int size) {
        return IntStream.range(start, start + size)
                .mapToObj(someNumber -> DatacampOfferPromoMechanics.directDiscount(
                        IdentityUtils.hashId("promo " + someNumber),
                        DatacampOfferPromoMechanics.price(BigDecimal.TEN))).collect(Collectors.toUnmodifiableSet());
    }

    @Nonnull
    public static Set<LocalOfferPromo> generateLocalDDPromos(int start, int size) {
        return IntStream.range(start, start + size)
                .mapToObj(someNumber -> LocalOfferPromoMechanics.directDiscount(
                        IdentityUtils.hashId("promo " + someNumber),
                        DatacampOfferPromoMechanics.basePrice(BigDecimal.TEN),
                        DatacampOfferPromoMechanics.price(BigDecimal.TEN)
                ).participation(true).build()).collect(Collectors.toUnmodifiableSet());
    }

    @Nonnull
    public static Set<DatacampOfferPromo> generateDatacampCAGPromos(int start, int size) {
        return IntStream.range(start, start + size)
                .mapToObj(someNumber -> DatacampOfferPromoMechanics.cheapestAsGift(
                        IdentityUtils.hashId("promo " + someNumber)
                ))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Nonnull
    public static Set<LocalOfferPromo> generateLocalCAGPromos(int start, int size) {
        return IntStream.range(start, start + size)
                .mapToObj(someNumber -> LocalOfferPromoMechanics.cheapestAsGift(
                        IdentityUtils.hashId("promo " + someNumber)
                ).participation(true).build())
                .collect(Collectors.toUnmodifiableSet());
    }
}
