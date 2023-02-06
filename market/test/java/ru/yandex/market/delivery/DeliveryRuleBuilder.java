package ru.yandex.market.delivery;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.math.NumberUtils;

import ru.yandex.market.deliverycalculator.indexerclient.model.DeliveryOption;
import ru.yandex.market.deliverycalculator.indexerclient.model.DeliveryRule;
import ru.yandex.market.deliverycalculator.indexerclient.model.DeliveryRuleFeedCategory;
import ru.yandex.market.deliverycalculator.indexerclient.model.DeliveryRuleFeedCategoryItem;
import ru.yandex.market.deliverycalculator.indexerclient.model.TreeCriteriaDefaultPolicy;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class DeliveryRuleBuilder {

    private Integer minPrice;
    private Integer maxPrice;

    private Integer minWeight;
    private Integer maxWeight;

    private Set<Long> includedRegions;
    private Set<Long> excludedRegions;

    private Set<TestCategoryId> includedCategories;
    private TreeCriteriaDefaultPolicy treePolicy;

    private Integer cost;
    private Integer orderBeforeHour;

    private List<DeliveryRule> children;

    private Integer minDaysCount;
    private Integer maxDaysCount;
    private boolean noDelivery;

    private DeliveryRuleBuilder withRegions(Set<Long> includedRegions, Set<Long> excludedRegions) {
        this.includedRegions = includedRegions;
        this.excludedRegions = excludedRegions;
        return this;
    }

    DeliveryRuleBuilder withRegions(Set<Long> includedRegions) {
        return withRegions(includedRegions, null);
    }


    DeliveryRuleBuilder withCategories(Set<TestCategoryId> includedCategories,
                                       TreeCriteriaDefaultPolicy treePolicy) {
        this.includedCategories = includedCategories;
        this.treePolicy = treePolicy;
        return this;
    }

    DeliveryRuleBuilder withCategories(Set<TestCategoryId> includedCategories) {
        return withCategories(includedCategories, TreeCriteriaDefaultPolicy.EXCLUDE);
    }

    DeliveryRuleBuilder withPrice(Integer minPrice, Integer maxPrice) {
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        return this;
    }

    DeliveryRuleBuilder withWeight(Integer minWeight, Integer maxWeight) {
        this.minWeight = minWeight;
        this.maxWeight = maxWeight;
        return this;
    }

    DeliveryRuleBuilder withCost(Integer cost) {
        this.cost = cost;
        return this;
    }

    DeliveryRuleBuilder withOrderBeforeHour(Integer orderBeforeHour) {
        this.orderBeforeHour = orderBeforeHour;
        return this;
    }

    DeliveryRuleBuilder withMinDaysCount(Integer minDaysCount) {
        this.minDaysCount = minDaysCount;
        return this;
    }

    DeliveryRuleBuilder withMaxDaysCount(Integer maxDaysCount) {
        this.maxDaysCount = maxDaysCount;
        return this;
    }

    DeliveryRuleBuilder withChildren(List<DeliveryRule> children) {
        this.children = children;
        return this;
    }

    DeliveryRuleBuilder withChildren(DeliveryRule... children) {
        return withChildren(Arrays.asList(children));
    }

    DeliveryRuleBuilder withNoDelivery() {
        this.noDelivery = true;
        return this;
    }

    DeliveryRule build() {
        DeliveryRule rule = new DeliveryRule();
        rule.setMinPrice(minPrice != null ? minPrice.doubleValue() : null);
        rule.setMaxPrice(maxPrice != null ? maxPrice.doubleValue() : null);
        rule.setMinWeight(minWeight != null ? minWeight.doubleValue() : null);
        rule.setMaxWeight(maxWeight != null ? maxWeight.doubleValue() : null);
        if (cost != null) {
            DeliveryOption option = new DeliveryOption();
            option.setDeliveryCost(cost);
            if (orderBeforeHour != null) {
                option.setOrderBeforeHour(orderBeforeHour);
            }
            option.setMinDaysCount(minDaysCount != null ? minDaysCount : 0);
            option.setMaxDaysCount(maxDaysCount != null ? maxDaysCount : 2);
            rule.setOptions(Collections.singletonList(option));
        } else if (noDelivery) {
            DeliveryOption option = new DeliveryOption();
            option.setDelivery(false);
            rule.setOptions(Collections.singletonList(option));
        }

        Function<Set<Long>, List<Integer>> converter = data -> data.stream()
                .map(Long::intValue)
                .collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(includedRegions)) {
            rule.setIncludedRegions(converter.apply(includedRegions));
        }
        if (CollectionUtils.isNotEmpty(excludedRegions)) {
            rule.setExcludedRegions(converter.apply(excludedRegions));
        }

        if (CollectionUtils.isNotEmpty(includedCategories) || treePolicy != null) {
            rule.setFeedCategory(new DeliveryRuleFeedCategory());
            rule.getFeedCategory().setDefaultPolicy(treePolicy);
            rule.getFeedCategory().setIncludeItems(getCategoryItems(includedCategories));
        }
        if (children != null) {
            rule.setChildren(children);
        }
        return rule;
    }

    private List<DeliveryRuleFeedCategoryItem> getCategoryItems(Collection<TestCategoryId> categoryIds) {
        if (CollectionUtils.isEmpty(categoryIds)) {
            return null;
        }
        return categoryIds.stream().map(
                categoryId -> {
                    DeliveryRuleFeedCategoryItem item = new DeliveryRuleFeedCategoryItem();
                    item.setFeedId(categoryId.getFeedId());
                    item.setCategoryId(NumberUtils.createLong(categoryId.getCategoryId()));
                    return item;
                }
        ).collect(Collectors.toList());
    }

}
