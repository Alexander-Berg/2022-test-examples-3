package ru.yandex.market.deliverycalculator.indexerclient;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXBException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.common.test.matcher.XmlMatcher;
import ru.yandex.market.common.test.util.StringTestUtil;
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
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.DeliveryOptionInfoDTO;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.DeliveryType;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.LocationRuleInfoDTO;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.OfferRuleInfoDTO;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.RulesHolderInfoDTO;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.ShopDeliveryCostRequest;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.ShopDeliveryCostResponse;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.TariffInfoDTO;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.YaDeliveryTariffTypeDTO;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.ActionDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.ConditionDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.DeliveryModifierDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.PercentValueLimiterDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.ValueModificationRuleDto;
import ru.yandex.market.deliverycalculator.model.DeliveryTariffStrategy;

/**
 * Created by berest on 07.07.16.
 */
class DeliveryCalculatorIndexerClientXmlUtilsTest {

    private static final Logger log = LoggerFactory.getLogger(DeliveryCalculatorIndexerClientXmlUtilsTest.class);

    @Test
    void testDeserialization() throws Exception {
        String serializedToString = StringTestUtil.getString(DeliveryCalculatorIndexerClientXmlUtilsTest.class,
                "DeliveryCalculatorIndexerClientXmlUtilsTest.testDeserialization.xml");
        UpdateShopRulesRequest actualRequest =
                (UpdateShopRulesRequest) DeliveryCalculatorIndexerClientXmlUtils.createUnmarshaller()
                        .unmarshal(new StringReader(serializedToString));
        Assertions.assertEquals(createRequest(), actualRequest);
    }

    @Test
    void testDeserializationTariffStrategy() throws Exception {
        String serializedToString = StringTestUtil.getString(DeliveryCalculatorIndexerClientXmlUtilsTest.class,
                "DeliveryCalculatorIndexerClientXmlUtilsTest.testDeserializationTariffStrategy.xml");
        UpdateShopRulesRequest actualRequest =
                (UpdateShopRulesRequest) DeliveryCalculatorIndexerClientXmlUtils.createUnmarshaller()
                        .unmarshal(new StringReader(serializedToString));
        UpdateShopRulesRequest expectedRequest = createRequest();
        expectedRequest.getShops().stream()
                .map(ShopDeliveryInfo::getTariffInfos)
                .flatMap(List::stream)
                .forEach(tariffInfo -> tariffInfo.setStrategy(DeliveryTariffStrategy.UNKNOWN_COST_TIME));
        Assertions.assertEquals(expectedRequest, actualRequest);
    }

    @Test
    void testShopDeliveryCostRequest() {
        ShopDeliveryCostRequest request = new ShopDeliveryCostRequest();
        request.setWidth(123.5);
        request.setHeight(456.54);
        request.setLength(3323.213);
        request.setAccessedValue(BigDecimal.valueOf(657.2));
        request.setRegionTo(32);
        request.setDeliveryType(DeliveryType.PICKUP);
        String serialized = DeliveryCalculatorIndexerClientXmlUtils.toXmlString(request);
        log.info("serialized: {}", serialized);
        ShopDeliveryCostRequest deserialized = DeliveryCalculatorIndexerClientXmlUtils.fromXmlString(serialized);
        Assertions.assertEquals(request, deserialized);
    }

    @Test
    void testShopDeliveryCostResponse() {
        ShopDeliveryCostResponse response = new ShopDeliveryCostResponse();
        response.setDeliveryCost(BigDecimal.valueOf(124.5));
        response.setInsuranceCost(BigDecimal.valueOf(456.7));
        String serialized = DeliveryCalculatorIndexerClientXmlUtils.toXmlString(response);
        log.info("serialized: {}", serialized);
        ShopDeliveryCostResponse deserialized = DeliveryCalculatorIndexerClientXmlUtils.fromXmlString(serialized);
        Assertions.assertEquals(response, deserialized);
    }

    @Test
    void testSerializationV2() throws JAXBException {
        TariffInfoDTO tariff = new TariffInfoDTO();
        tariff.setId(234);
        tariff.setM3weight(4.0);
        tariff.setCarrierId(567);
        RulesHolderInfoDTO rules = new RulesHolderInfoDTO();
        tariff.setRule(new OfferRuleInfoDTO());

        DeliveryOptionInfoDTO option = new DeliveryOptionInfoDTO(34, (short) 2, (short) 4, null, null);
        rules.setOptions(Collections.singletonList(option));

        List<LocationRuleInfoDTO> locationRules = new ArrayList<>();

        locationRules.add(new LocationRuleInfoDTO(Arrays.asList(2, 4, 8, 16, 32), Arrays.asList(3, 9, 27, 81), null));
        rules.setLocationRules(locationRules);

        DeliveryCalculatorIndexerClientXmlUtils.createMarshaller().marshal(tariff, System.out);
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
        shopDeliveryInfo.setId(774);
        shopDeliveryInfo.setCampaignType("SHOP");
        shopDeliveryInfo.setTariffInfos(new ArrayList<>());
        shopDeliveryInfo.getTariffInfos().add(createTariff());
        shopDeliveryInfo.setRegionId(213);
        shopDeliveryInfo.setUnitedCatalog(true);
        shopDeliveryInfo.setPlacementPrograms(Collections.singletonList(ShopPlacementProgram.DROPSHIP_BY_SELLER));
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
        child.setMinPrice(0.0);
        child.setMaxPrice(500D);
        child.setOptions(ImmutableList.of(deliveryOption));

        DeliveryRule child2 = new DeliveryRule();
        child2.setMinPrice(500D);
        child2.setMaxPrice(10000D);
        child2.setOptions(ImmutableList.of(deliveryOption));

        deliveryRule.getChildren().add(child);
        deliveryRule.getChildren().add(child2);

        DeliveryRule result = new DeliveryRule();
        result.setChildren(new ArrayList<>());
        result.getChildren().add(deliveryRule);

        result.setExcludedRegions(Arrays.asList(2, 213));
        result.setIncludedRegions(Arrays.asList(1, 10740));

        return result;
    }

    @Test
    void testShopDeliveryInfoWithModifiersSerialization() {
        final ValueModificationRuleDto valueModificationRule = new ValueModificationRuleDto.Builder()
                .withOperation(ValueModificationRuleDto.OperationEnum.ADD)
                .withParameter(BigDecimal.valueOf(360.0))
                .build();

        final ActionDto action = new ActionDto.Builder().withCostModificationRule(valueModificationRule).build();

        final ValueModificationRuleDto valueModificationRule2 = new ValueModificationRuleDto.Builder()
                .withOperation(ValueModificationRuleDto.OperationEnum.DIVIDE)
                .withParameter(BigDecimal.valueOf(2L))
                .withResultLimit(new PercentValueLimiterDto.Builder()
                        .withPercent(BigDecimal.valueOf(75))
                        .withMaxValue(BigDecimal.valueOf(80.5))
                        .withMinValue(BigDecimal.valueOf(70L))
                        .build())
                .build();

        final ActionDto action2 = new ActionDto.Builder()
                .withTimeModificationRule(valueModificationRule2)
                .withIsCarrierTurnedOn(true)
                .build();

        final ConditionDto condition = new ConditionDto.Builder()
                .withCarrierIds(ImmutableSet.of(213L))
                .withChargeableWeight(new PercentValueLimiterDto.Builder()
                        .withMinValue(BigDecimal.valueOf(0.01))
                        .withMaxValue(BigDecimal.valueOf(30.9))
                        .build())
                .withDeliveryTypes(ImmutableSet.of(YaDeliveryTariffTypeDTO.PICKUP))
                .build();

        final List<DeliveryModifierDto> modifiers =
                ImmutableList.of(
                        new DeliveryModifierDto.Builder()
                                .withAction(action)
                                .build(),
                        new DeliveryModifierDto.Builder()
                                .withAction(action2)
                                .withCondition(condition)
                                .withId(0L)
                                .withTimestamp(1582176717553L)
                                .build());

        ShopDeliveryInfo shopDeliveryInfo = new ShopDeliveryInfo();
        shopDeliveryInfo.setDeliveryModifiers(modifiers);

        final String serialized = DeliveryCalculatorIndexerClientXmlUtils.toXmlString(shopDeliveryInfo);

        final String expected = StringTestUtil.getString(DeliveryCalculatorIndexerClientXmlUtilsTest.class,
                "DeliveryCalculatorIndexerClientXmlUtilsTest.testShopDeliveryInfoWithModifiersSerialization.xml");

        MatcherAssert.assertThat(serialized, new XmlMatcher(expected));

        final Object o = DeliveryCalculatorIndexerClientXmlUtils.fromXmlString(serialized);

        Assertions.assertEquals(shopDeliveryInfo, o);
    }
}
