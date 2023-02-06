package ru.yandex.market.deliverycalculator.indexer.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.deliverycalculator.indexer.FunctionalTest;
import ru.yandex.market.deliverycalculator.indexerclient.DeliveryCalculatorIndexerClient;
import ru.yandex.market.deliverycalculator.indexerclient.HttpDeliveryCalculatorIndexerClient;
import ru.yandex.market.deliverycalculator.indexerclient.model.DeleteShopRulesRequest;
import ru.yandex.market.deliverycalculator.indexerclient.model.DeliveryOption;
import ru.yandex.market.deliverycalculator.indexerclient.model.DeliveryRule;
import ru.yandex.market.deliverycalculator.indexerclient.model.DeliveryRuleFeedCategory;
import ru.yandex.market.deliverycalculator.indexerclient.model.DeliveryRuleFeedCategoryItem;
import ru.yandex.market.deliverycalculator.indexerclient.model.FeedInfo;
import ru.yandex.market.deliverycalculator.indexerclient.model.ShopDeliveryInfo;
import ru.yandex.market.deliverycalculator.indexerclient.model.ShopPlacementProgram;
import ru.yandex.market.deliverycalculator.indexerclient.model.TariffInfoV1;
import ru.yandex.market.deliverycalculator.indexerclient.model.TreeCriteriaDefaultPolicy;
import ru.yandex.market.deliverycalculator.indexerclient.model.UpdateShopRulesRequest;
import ru.yandex.market.deliverycalculator.storage.model.DeliveryShop;
import ru.yandex.market.deliverycalculator.storage.model.PartnerPlacementProgramType;
import ru.yandex.market.deliverycalculator.storage.service.DeliveryCalculatorStorageService;

class RulesImportControllerClientTest extends FunctionalTest {

    private static final int SHOP_ID = 774;

    @Autowired
    private DeliveryCalculatorStorageService storageService;

    private DeliveryCalculatorIndexerClient client;

    @BeforeEach
    void init() {
        client = new HttpDeliveryCalculatorIndexerClient(baseUrl);
    }

    @Test
    void testUpdateShopRules() {
        client.updateShopDeliveryRules(createRequest());
        DeliveryShop deliveryShop = storageService.getShop(SHOP_ID);
        MatcherAssert.assertThat(deliveryShop, Matchers.hasProperty("unitedCatalog", Matchers.is(true)));
        MatcherAssert.assertThat(deliveryShop,
                Matchers.hasProperty("placementPrograms",
                        Matchers.equalTo(Set.of(PartnerPlacementProgramType.DROPSHIP_BY_SELLER))));
    }

    @Test
    void testDeleteShopRules() {
        DeleteShopRulesRequest request = new DeleteShopRulesRequest();
        request.setDeletedShopIds(Lists.newArrayList(1234L, 5678L));
        client.deleteShopDeliveryRules(request);
    }

    private UpdateShopRulesRequest createRequest() {
        UpdateShopRulesRequest updateShopsDeliveryRulesRequest = new UpdateShopRulesRequest();
        updateShopsDeliveryRulesRequest.setShops(new ArrayList<>());
        updateShopsDeliveryRulesRequest.getShops().add(createShopInfo());
        return updateShopsDeliveryRulesRequest;
    }


    private ShopDeliveryInfo createShopInfo() {
        ShopDeliveryInfo shopDeliveryInfo = new ShopDeliveryInfo();
        shopDeliveryInfo.setFeeds(new ArrayList<>());
        shopDeliveryInfo.getFeeds().add(new FeedInfo(76632));
        shopDeliveryInfo.getFeeds().add(new FeedInfo(70004));
        shopDeliveryInfo.setId(SHOP_ID);
        shopDeliveryInfo.setTariffInfos(new ArrayList<>());
        shopDeliveryInfo.getTariffInfos().add(createTariff());
        shopDeliveryInfo.setUnitedCatalog(true);
        shopDeliveryInfo.setPlacementPrograms(List.of(ShopPlacementProgram.DROPSHIP_BY_SELLER));
        return shopDeliveryInfo;
    }

    private TariffInfoV1 createTariff() {
        TariffInfoV1 tariffInfo = new TariffInfoV1();
        tariffInfo.setCurrency("ROC");
        tariffInfo.setRules(Collections.singletonList(createDeliveryRuleNode()));
        return tariffInfo;
    }

    private DeliveryRule createDeliveryRuleNode() {
        DeliveryRuleFeedCategory feedCategory = new DeliveryRuleFeedCategory();

        feedCategory.setDefaultPolicy(TreeCriteriaDefaultPolicy.INCLUDE);

        DeliveryRuleFeedCategoryItem includedItem = new DeliveryRuleFeedCategoryItem();
        includedItem.setFeedId(76632);
        includedItem.setCategoryId(666L);

        DeliveryRuleFeedCategoryItem excludedItem = new DeliveryRuleFeedCategoryItem();
        excludedItem.setFeedId(70004);
        excludedItem.setCategoryId(777L);

        feedCategory.setIncludeItems(Collections.singletonList(includedItem));
        feedCategory.setExcludeItems(Collections.singletonList(excludedItem));


        DeliveryOption deliveryOption = new DeliveryOption();
        deliveryOption.setMinDaysCount(1);
        deliveryOption.setMaxDaysCount(2);
        deliveryOption.setOrderBeforeHour(22);
        deliveryOption.setDeliveryCost(500);

        DeliveryRule deliveryRule = new DeliveryRule();

        deliveryRule.setFeedCategory(feedCategory);

        deliveryRule.setChildren(new ArrayList<>());

        DeliveryRule child = new DeliveryRule();
        child.setMinPrice(0D);
        child.setMaxPrice(500D);
        child.setOptions(Lists.newArrayList(deliveryOption));

        DeliveryRule child2 = new DeliveryRule();
        child2.setMinPrice(500D);
        child2.setMaxPrice(10000D);
        child2.setOptions(Lists.newArrayList(deliveryOption));

        deliveryRule.getChildren().add(child);
        deliveryRule.getChildren().add(child2);

        DeliveryRule result = new DeliveryRule();
        result.setChildren(new ArrayList<>());
        result.getChildren().add(deliveryRule);

        result.setExcludedRegions(Arrays.asList(2, 213));
        result.setIncludedRegions(Arrays.asList(1, 10740));

        return result;
    }
}
