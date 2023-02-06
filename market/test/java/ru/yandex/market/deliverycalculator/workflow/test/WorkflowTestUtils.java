package ru.yandex.market.deliverycalculator.workflow.test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.randomizers.collection.SetRandomizer;
import io.github.benas.randombeans.randomizers.range.DoubleRangeRandomizer;
import io.github.benas.randombeans.randomizers.range.IntegerRangeRandomizer;
import io.github.benas.randombeans.randomizers.range.LongRangeRandomizer;
import org.apache.commons.collections4.CollectionUtils;

import ru.yandex.market.deliverycalculator.common.CurrencyUtils;
import ru.yandex.market.deliverycalculator.model.DeliveryOption;
import ru.yandex.market.deliverycalculator.model.DeliveryPickpoint;
import ru.yandex.market.deliverycalculator.model.DeliveryRule;
import ru.yandex.market.deliverycalculator.model.DeliveryRuleRegion;
import ru.yandex.market.deliverycalculator.model.DeliveryTariffProgramType;
import ru.yandex.market.deliverycalculator.model.DeliveryTariffSource;
import ru.yandex.market.deliverycalculator.model.LocationCargoType;
import ru.yandex.market.deliverycalculator.model.YaDeliveryTariffProgram;
import ru.yandex.market.deliverycalculator.model.YaDeliveryTariffType;
import ru.yandex.market.deliverycalculator.model.YaDeliveryTariffUpdatedInfo;
import ru.yandex.market.deliverycalculator.storage.model.DeliveryRuleEntity;
import ru.yandex.market.deliverycalculator.storage.model.DeliveryShop;
import ru.yandex.market.deliverycalculator.storage.model.DeliveryShopFeed;
import ru.yandex.market.deliverycalculator.storage.model.OutletType;
import ru.yandex.market.deliverycalculator.storage.model.ShopOutlet;

import static io.github.benas.randombeans.FieldDefinitionBuilder.field;

public final class WorkflowTestUtils {

    private static final EnhancedRandom randomYaDeliveryTariff = createYaDeliveryTariffRandomizer();

    private WorkflowTestUtils() {
        throw new UnsupportedOperationException();
    }

    public static YaDeliveryTariffUpdatedInfo createMardoPickupTariff(long id, int carrierId, Double m3Weight, String currency,
                                                                      Set<Integer> tariffCargoTypesBlacklist,
                                                                      Set<LocationCargoType> locationCargoTypesBlacklist) {
        return createBaseMardoTariff(id, carrierId, m3Weight, currency, YaDeliveryTariffType.PICKUP,
                DeliveryTariffProgramType.MARKET_DELIVERY, tariffCargoTypesBlacklist, locationCargoTypesBlacklist);
    }

    public static YaDeliveryTariffUpdatedInfo createMardoPostTariff(long id, Double m3Weight, String currency,
                                                                    Set<Integer> tariffCargoTypesBlacklist,
                                                                    Set<LocationCargoType> locationCargoTypesBlacklist) {
        return createBaseMardoTariff(id, 0, m3Weight, currency, YaDeliveryTariffType.POST,
                DeliveryTariffProgramType.MARKET_DELIVERY, tariffCargoTypesBlacklist, locationCargoTypesBlacklist);
    }

    public static YaDeliveryTariffUpdatedInfo createCourierTariff(long id, int carrierId, Double m3Weight,
                                                                  boolean isForCustomer,
                                                                  Set<Integer> customerTariffCargoTypesBlacklist,
                                                                  Set<LocationCargoType> customerLocationCargoTypesBlacklist) {
        return YaDeliveryTariffUpdatedInfo.builder()
                .id(id)
                .type(YaDeliveryTariffType.COURIER)
                .sourceType(DeliveryTariffSource.YADO)
                .forCustomer(isForCustomer)
                .forShop(!isForCustomer)
                .m3Weight(m3Weight)
                .currency(CurrencyUtils.DEFAULT_CURRENCY)
                .programs(Collections.singletonList(new YaDeliveryTariffProgram(DeliveryTariffProgramType.MARKET_DELIVERY)))
                .carrierId(carrierId)
                .tariffCargoTypesBlacklist(customerTariffCargoTypesBlacklist)
                .locationCargoTypesBlacklist(customerLocationCargoTypesBlacklist)
                .contentUrl(id + ".xml")
                .build();
    }

    public static YaDeliveryTariffUpdatedInfo createMardoWhiteCourierTariff(long id, int carrierId, Double m3Weight) {
        return createBaseMardoTariff(id, carrierId, m3Weight, CurrencyUtils.DEFAULT_CURRENCY,
                YaDeliveryTariffType.COURIER, DeliveryTariffProgramType.WHITE_MARKET_DELIVERY, Collections.emptySet());
    }

    private static YaDeliveryTariffUpdatedInfo createBaseMardoTariff(long id, int carrierId, Double m3Weight, String currency,
                                                                     YaDeliveryTariffType type,
                                                                     DeliveryTariffProgramType program,
                                                                     Set<Integer> tariffCargoTypesBlacklist) {
        return createBaseMardoTariff(id, carrierId, m3Weight, currency, type, program,
                tariffCargoTypesBlacklist, Collections.emptySet());
    }

    private static YaDeliveryTariffUpdatedInfo createBaseMardoTariff(long id, int carrierId, Double m3Weight, String currency,
                                                                     YaDeliveryTariffType type,
                                                                     DeliveryTariffProgramType program,
                                                                     Set<Integer> tariffCargoTypesBlacklist,
                                                                     Set<LocationCargoType> locationCargoTypes) {
        return YaDeliveryTariffUpdatedInfo.builder()
                .id(id)
                .type(type)
                .sourceType(DeliveryTariffSource.YADO)
                .forCustomer(true)
                .m3Weight(m3Weight)
                .currency(currency)
                .programs(Collections.singletonList(new YaDeliveryTariffProgram(program)))
                .carrierId(carrierId)
                .tariffCargoTypesBlacklist(tariffCargoTypesBlacklist)
                .locationCargoTypesBlacklist(locationCargoTypes)
                .contentUrl(id + ".xml")
                .build();
    }

    public static void addChildRule(DeliveryRule parent, DeliveryRule child) {
        if (parent == null) {
            return;
        }
        child.setParent(parent);
        if (parent.getChildren().isEmpty()) {
            parent.setChildren(new ArrayList<>());
        }
        parent.getChildren().add(child);
    }

    public static void addChildRule(DeliveryRuleEntity parent, DeliveryRuleEntity child) {
        if (parent == null) {
            return;
        }
        child.setParent(parent);
        if (parent.getChildren() == null) {
            parent.setChildren(new HashSet<>());
        }
        parent.getChildren().add(child);
    }

    public static DeliveryRule createOfferRule(DeliveryRule parent,
                                               Double maxWeight, Double maxWidht, Double maxHeight, Double maxLength,
                                               Double maxDimSum) {
        return createOfferRuleWithMaxPrice(parent, maxWeight, maxWidht, maxHeight, maxLength, maxDimSum, null);
    }

    public static DeliveryRule createOfferRuleWithMaxPrice(DeliveryRule parent,
                                                           Double maxWeight, Double maxWidht, Double maxHeight, Double maxLength,
                                                           Double maxDimSum, Double maxPrice) {
        DeliveryRule rule = new DeliveryRule();
        rule.setMaxWeight(maxWeight);
        rule.setMaxWidth(maxWidht);
        rule.setMaxHeight(maxHeight);
        rule.setMaxLength(maxLength);
        rule.setMaxDimSum(maxDimSum);
        rule.setMaxPrice(maxPrice);
        addChildRule(parent, rule);
        return rule;
    }

    public static DeliveryRule createLocationRule(DeliveryRule parent, Set<Integer> fromRegionIds,
                                                  Set<Integer> toRegionIds) {
        DeliveryRule rule = new DeliveryRule();
        HashSet<DeliveryRuleRegion> ruleRegions = new HashSet<>();
        if (CollectionUtils.isNotEmpty(fromRegionIds)) {
            fromRegionIds.stream()
                    .map(regionId -> {
                        DeliveryRuleRegion region = new DeliveryRuleRegion();
                        region.setRegionId(regionId);
                        region.setStarting(true);
                        return region;
                    })
                    .forEach(ruleRegions::add);
        }
        if (CollectionUtils.isNotEmpty(toRegionIds)) {
            toRegionIds.stream()
                    .map(regionId -> {
                        DeliveryRuleRegion region = new DeliveryRuleRegion();
                        region.setRegionId(regionId);
                        return region;
                    })
                    .forEach(ruleRegions::add);
        }
        rule.setRegions(ruleRegions);
        addChildRule(parent, rule);
        return rule;
    }

    public static DeliveryRule createWeightRule(DeliveryRule parent, Double minWeight, Double maxWeight) {
        DeliveryRule rule = new DeliveryRule();
        rule.setMinWeight(minWeight);
        rule.setMaxWeight(maxWeight);
        addChildRule(parent, rule);
        return rule;
    }

    public static DeliveryPickpoint createPickpoint(DeliveryRule rule, long id) {
        DeliveryPickpoint pickpoint = new DeliveryPickpoint(id);
        if (rule.getPickpoints() == null) {
            rule.setPickpoints(new HashSet<>());
        }
        rule.getPickpoints().add(pickpoint);
        return pickpoint;
    }

    public static DeliveryShopFeed createFeed(DeliveryShop shop, long feedId) {
        DeliveryShopFeed feed = new DeliveryShopFeed();
        feed.setId(feedId);
        if (shop.getFeeds() == null) {
            shop.setFeeds(new HashSet<>());
        }
        shop.getFeeds().add(feed);
        return feed;
    }

    public static ShopOutlet createShopOutlet(DeliveryShop shop, long id, OutletType type, Integer regionId) {
        ShopOutlet outlet = new ShopOutlet();
        outlet.setShopId(shop.getId());
        outlet.setId(id);
        outlet.setType(type);
        outlet.setRegionId(regionId);
        return outlet;
    }

    public static DeliveryRuleEntity createPriceRuleEntity(DeliveryRuleEntity parent, Double minPrice, Double maxPrice) {
        DeliveryRuleEntity rule = new DeliveryRuleEntity();
        rule.setMinPrice(minPrice);
        rule.setMaxPrice(maxPrice);
        addChildRule(parent, rule);
        return rule;
    }

    public static DeliveryOption createDeliveryOption(DeliveryRule rule, int cost, int minDaysCount, int maxDaysCount, Integer orderBefore) {
        DeliveryOption deliveryOption = new DeliveryOption();
        deliveryOption.setCost(cost);
        deliveryOption.setMinDaysCount((short) minDaysCount);
        deliveryOption.setMaxDaysCount((short) maxDaysCount);
        deliveryOption.setOrderBefore(orderBefore != null ? orderBefore.byteValue() : null);
        if (CollectionUtils.isEmpty(rule.getOptions())) {
            rule.setOptions(new ArrayList<>());
        }
        rule.getOptions().add(deliveryOption);
        return deliveryOption;
    }

    public static YaDeliveryTariffUpdatedInfo createRandomDaasPickupTariff(long tariffId, int carrierId,
                                                                           DeliveryTariffSource source) {
        return randomYaDeliveryTariff.nextObject(YaDeliveryTariffUpdatedInfo.Builder.class)
                .id(tariffId)
                .carrierId(carrierId)
                .type(YaDeliveryTariffType.PICKUP)
                .sourceType(source)
                .programs(ImmutableList.of(
                        new YaDeliveryTariffProgram(DeliveryTariffProgramType.DAAS)))
                .build();
    }

    public static YaDeliveryTariffUpdatedInfo createRandomDaasPostTariff(long tariffId, int carrierId) {
        return randomYaDeliveryTariff.nextObject(YaDeliveryTariffUpdatedInfo.Builder.class)
                .id(tariffId)
                .carrierId(carrierId)
                .type(YaDeliveryTariffType.POST)
                .sourceType(DeliveryTariffSource.DAAS)
                .programs(ImmutableList.of(
                        new YaDeliveryTariffProgram(DeliveryTariffProgramType.DAAS)))
                .build();
    }

    private static EnhancedRandom createYaDeliveryTariffRandomizer() {
        return EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                .randomize(field().named("id").ofType(long.class).inClass(YaDeliveryTariffUpdatedInfo.Builder.class).get(),
                        new LongRangeRandomizer(100L, 999L))
                .randomize(field().named("currency").ofType(String.class).inClass(YaDeliveryTariffUpdatedInfo.Builder.class).get(), Suppliers.ofInstance(CurrencyUtils.DEFAULT_CURRENCY))
                .randomize(field().named("carrierId").ofType(int.class).inClass(YaDeliveryTariffUpdatedInfo.Builder.class).get(), new IntegerRangeRandomizer(1000, 9999))
                .randomize(field().named("m3Weight").ofType(Double.class).inClass(YaDeliveryTariffUpdatedInfo.Builder.class).get(), new DoubleRangeRandomizer(0.0, 100.0))
                .randomize(field().named("forCustomer").ofType(boolean.class).inClass(YaDeliveryTariffUpdatedInfo.Builder.class).get(), Suppliers.ofInstance(true))
                .randomize(field().named("forShop").ofType(boolean.class).inClass(YaDeliveryTariffUpdatedInfo.Builder.class).get(), Suppliers.ofInstance(false))
                .randomize(field().named("rulesCurrency").ofType(String.class).inClass(YaDeliveryTariffUpdatedInfo.Builder.class).get(), Suppliers.ofInstance(CurrencyUtils.DEFAULT_CURRENCY))
                .randomize(field().named("updateTime").ofType(Instant.class).inClass(YaDeliveryTariffUpdatedInfo.Builder.class).get(), Suppliers.ofInstance(Instant.now()))
                .randomize(field().named("cargoTypes").ofType(Set.class).inClass(YaDeliveryTariffUpdatedInfo.Builder.class).get(), new SetRandomizer<>(new IntegerRangeRandomizer(1, 100), 5))
                .exclude(field().named("rule").inClass(YaDeliveryTariffUpdatedInfo.Builder.class).get())
                .exclude(field().named("generationId").inClass(YaDeliveryTariffUpdatedInfo.Builder.class).get())
                .build();
    }

}
