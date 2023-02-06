package ru.yandex.market.deliverycalculator.storage;

import java.io.InputStream;
import java.util.HashSet;
import java.util.function.Function;

import com.google.common.collect.Sets;

import ru.yandex.market.deliverycalculator.indexerclient.modelv2.TariffInfoDTO;
import ru.yandex.market.deliverycalculator.model.DeliveryTariffStrategy;
import ru.yandex.market.deliverycalculator.storage.model.DeliveryCarrierEntity;
import ru.yandex.market.deliverycalculator.storage.model.DeliveryOptionEntity;
import ru.yandex.market.deliverycalculator.storage.model.DeliveryPickpointEntity;
import ru.yandex.market.deliverycalculator.storage.model.DeliveryRuleEntity;
import ru.yandex.market.deliverycalculator.storage.model.DeliveryRuleFeedCategoryEntity;
import ru.yandex.market.deliverycalculator.storage.model.DeliveryRuleRegionEntity;
import ru.yandex.market.deliverycalculator.storage.model.DeliveryShop;
import ru.yandex.market.deliverycalculator.storage.model.DeliveryShopFeed;
import ru.yandex.market.deliverycalculator.storage.model.MarketDeliveryTariff;
import ru.yandex.market.deliverycalculator.storage.service.impl.TariffInfoProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Created by berest on 09.07.16.
 */
public final class StorageTestUtils {

    private StorageTestUtils() {
        throw new UnsupportedOperationException();
    }

    public static DeliveryShop getShop(long id) {
        long feedId = id + 20;
        DeliveryShop shop = new DeliveryShop();
        shop.setId(id);

        MarketDeliveryTariff deliveryTariff = new MarketDeliveryTariff();
        deliveryTariff.setCurrency("JPY");
        DeliveryRuleEntity rootRule = new DeliveryRuleEntity();
        deliveryTariff.setRule(rootRule);

        deliveryTariff.setCarriers(Sets.newHashSet(new DeliveryCarrierEntity(102, DeliveryTariffStrategy.UNKNOWN_COST_TIME, DeliveryTariffStrategy.UNKNOWN_COST_TIME)));

        shop.setMarketTariffs(Sets.newHashSet(deliveryTariff));

        DeliveryShopFeed feed = new DeliveryShopFeed();
        feed.setId(feedId);
        shop.setFeeds(Sets.newHashSet(feed));

        DeliveryRuleEntity rule = new DeliveryRuleEntity();
        rule.setMinPrice(120.0);
        rule.setMaxPrice(130.0);
        rule.setMinWeight(140.0);
        rule.setMaxWeight(150.0);
        rule.setExcludeDefaultFeedCategory(true);

        rootRule.setChildren(Sets.newHashSet(rule));

        DeliveryRuleRegionEntity includedRegion1 = new DeliveryRuleRegionEntity();
        includedRegion1.setRegionId(160);

        DeliveryRuleRegionEntity includedRegion2 = new DeliveryRuleRegionEntity();
        includedRegion2.setRegionId(170);

        DeliveryRuleRegionEntity excludedRegion = new DeliveryRuleRegionEntity();
        excludedRegion.setRegionId(180);
        excludedRegion.setExcluded(true);

        rule.setRegions(Sets.newHashSet(includedRegion1, includedRegion2, excludedRegion));

        DeliveryRuleFeedCategoryEntity includedFeedCategory1 = new DeliveryRuleFeedCategoryEntity();
        includedFeedCategory1.setFeedCategoryId(1L);
        includedFeedCategory1.setFeed(feed);

        DeliveryRuleFeedCategoryEntity includedFeedCategory2 = new DeliveryRuleFeedCategoryEntity();
        includedFeedCategory2.setFeedCategoryId(2L);
        includedFeedCategory2.setFeed(feed);

        DeliveryRuleFeedCategoryEntity excludedFeedCategory1 = new DeliveryRuleFeedCategoryEntity();
        excludedFeedCategory1.setFeedCategoryId(10L);
        excludedFeedCategory1.setExcluded(true);
        excludedFeedCategory1.setFeed(feed);

        DeliveryRuleFeedCategoryEntity excludedFeedCategory2 = new DeliveryRuleFeedCategoryEntity();
        excludedFeedCategory2.setFeedCategoryId(20L);
        excludedFeedCategory2.setExcluded(true);
        excludedFeedCategory2.setFeed(feed);

        rule.setFeedCategories(Sets.newHashSet(includedFeedCategory1, includedFeedCategory2, excludedFeedCategory1, excludedFeedCategory2));

        DeliveryOptionEntity option = new DeliveryOptionEntity();
        option.setMinDaysCount(1);
        option.setMaxDaysCount(3);
        option.setCost(234);
        option.setOrderBefore(5);

        rule.setOptions(Sets.newHashSet(option));

        return shop;
    }

    public static void addChildRule(DeliveryRuleEntity parent, DeliveryRuleEntity child) {
        if (parent.getChildren() == null) {
            parent.setChildren(new HashSet<>());
        }
        parent.getChildren().add(child);
        child.setParent(parent);
    }

    public static DeliveryOptionEntity createOption(DeliveryRuleEntity rule, int cost, int minDaysCount, int maxDaysCount,
                                                    Integer orderBefore) {
        DeliveryOptionEntity option = new DeliveryOptionEntity();
        option.setRule(rule);
        option.setCost(cost);
        option.setMinDaysCount(minDaysCount);
        option.setMaxDaysCount(maxDaysCount);
        option.setOrderBefore(orderBefore);
        if (rule.getOptions() == null) {
            rule.setOptions(new HashSet<>());
        }
        rule.getOptions().add(option);
        return option;
    }

    public static DeliveryPickpointEntity createPickpoint(DeliveryRuleEntity rule, long pickpointId, Integer intCode) {
        DeliveryPickpointEntity pickpoint = new DeliveryPickpointEntity();
        pickpoint.setRule(rule);
        pickpoint.setPickpointId(pickpointId);
        pickpoint.setIntCode(intCode);
        if (rule.getPickpoints() == null) {
            rule.setPickpoints(new HashSet<>());
        }
        rule.getPickpoints().add(pickpoint);
        return pickpoint;
    }

    /**
     * Инициализировать мок TariffInfoProvider: задать, каким способом он будет выдавать файлы с тарифами.
     *
     * @param tariffInfoProvider мок TariffInfoProvider
     * @param fileNameProvider   функция, которая по аргументу url метода tariffInfoProvider.getTariffInfoStream
     *                           вернет имя файла с тарифом, который нужно предоставить в тесте
     * @param clazz              класс, по чьему ClassLoader'у будет искаться файлик (просто передавайте сюда getClass())
     */
    public static void initProviderMock(TariffInfoProvider tariffInfoProvider,
                                        Function<String, String> fileNameProvider, Class clazz) {
        when(tariffInfoProvider.getTariffInfoStream(any(), any()))
                .thenAnswer(invocation -> {
                    String fileName = fileNameProvider.apply(invocation.getArgument(0));
                    Function<InputStream, TariffInfoDTO> fn = invocation.getArgument(1);
                    try (InputStream in = clazz.getResourceAsStream(fileName)) {
                        return fn.apply(in);
                    }
                });
    }
}
