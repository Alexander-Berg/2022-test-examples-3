package ru.yandex.market.logistics.management.service.export.dynamic;


import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.Logistics;
import ru.yandex.market.logistics.management.domain.dto.DeliveryDistributorParamsProjectionDto;
import ru.yandex.market.logistics.management.domain.entity.type.StrictBoundsType;

@SuppressWarnings({"unchecked", "magicNumbers"})
class DeliveryConditionsBuilderTest {

    @Test
    void builderTest() {
        List<DeliveryDistributorParamsProjectionDto> deliveryDistributorParamList = getDeliveryExperimentParams();

        DeliveryConditionsBuilder deliveryConditionsBuilder = new DeliveryConditionsBuilder();
        Logistics.DeliveryConditionsMetaInfo result = deliveryConditionsBuilder.build(deliveryDistributorParamList);
        SoftAssertions.assertSoftly(softAssertions -> {

            softAssertions.assertThat(result.getWeightBoundsList())
                .extracting(Logistics.WeightBoundInfo::getId, Logistics.WeightBoundInfo::getWeight,
                    Logistics.WeightBoundInfo::getIncluding)
                .as("weight bound: id, weight, strict bound -> experiment params with id 1")
                .contains(new Tuple(1, 3.0, true), new Tuple(2, 10.0, false))
                .as("weight bound: id, weight, strict bound -> experiment params with id 2")
                .contains(new Tuple(2, 10.0, false), new Tuple(3, 50.0, true))
                .as("weight bound: id, weight, strict bound -> experiment params with id 3")
                .contains(new Tuple(4, 1.0, false), new Tuple(5, 5.0, false))
                .as("weight bound: id, weight, strict bound -> experiment params with id 4")
                .contains(new Tuple(6, 6.0, true), new Tuple(7, 12.0, true));


            softAssertions.assertThat(result.getWeightRangesList())
                .extracting(Logistics.WeightRangeInfo::getId, Logistics.WeightRangeInfo::getWeightLowerBoundId,
                    Logistics.WeightRangeInfo::getWeightUpperBoundId)
                .as("weight range: id, lowerId, upperId -> experiment params with id 1")
                .contains(new Tuple(1, 1, 2))
                .as("weight range: id, lowerId, upperId -> experiment params with id 2")
                .contains(new Tuple(2, 2, 3))
                .as("weight range: id, lowerId, upperId -> experiment params with id 3")
                .contains(new Tuple(3, 4, 5))
                .as("weight range: id, lowerId, upperId -> experiment params with id 4")
                .contains(new Tuple(4, 6, 7));

            softAssertions.assertThat(result.getDeliveryThresholdsList())
                .extracting(Logistics.DeliveryThresholdsInfo::getId, Logistics.DeliveryThresholdsInfo::getPrice,
                    Logistics.DeliveryThresholdsInfo::getDays)
                .as("threshold info: id, price, delivery days - > experiment params with id 1")
                .contains(new Tuple(1, 1000.0, 2))
                .as("threshold info: id, price, delivery days - > experiment params with id 2")
                .contains(new Tuple(2, 5000.0, 3))
                .as("threshold info: id, price, delivery days - > experiment params with id 3")
                .contains(new Tuple(3, 500.0, 1))
                .as("threshold info: id, price, delivery days - > experiment params with id 4")
                .contains(new Tuple(4, 100.0, 10));

            softAssertions.assertThat(result.getDeliveryConditionsList())
                .extracting(Logistics.DeliveryConditionsReferenceInfo::getId,
                    Logistics.DeliveryConditionsReferenceInfo::getDeliveryThresholdsId,
                    Logistics.DeliveryConditionsReferenceInfo::getWeightRangeId)
                .as("condition list: id, thresholdId, weightRangeId -> experiment params with id 1")
                .contains(new Tuple(1, 1, 1))
                .as("condition list: id, thresholdId, weightRangeId -> experiment params with id 2")
                .contains(new Tuple(2, 2, 2))
                .as("condition list: id, thresholdId, weightRangeId -> experiment params with id 3")
                .contains(new Tuple(3, 3, 3))
                .as("condition list: id, thresholdId, weightRangeId -> experiment params with id 4")
                .contains(new Tuple(4, 4, 4));

            softAssertions.assertThat(result.getDeliveryConditionsByRegionList())
                .extracting(Logistics.DeliveryConditionsByRegionInfo::getId,
                    Logistics.DeliveryConditionsByRegionInfo::getRegionId,
                    Logistics.DeliveryConditionsByRegionInfo::getDeliveryConditionReferenceIdsList)
                .as("grouping delivery condition id by region. From test data we have only 2 regions " +
                    "id, regionId, deliveryConditionsIdList")
                .contains(
                    new Tuple(1, 1, Arrays.asList(1, 2)),
                    new Tuple(2, 2, Collections.singletonList(4)),
                    new Tuple(3, 2, Collections.singletonList(3))
                );

            softAssertions.assertThat(result.getDeliveryConditionsSplitsList())
                .extracting(Logistics.DeliveryConditionsSplitInfo::getId,
                    Logistics.DeliveryConditionsSplitInfo::getDeliveryConditionsByRegionIdsList)
                .as("grouping conditions by flagId which is grouped by regions " +
                    "id -> as flag_id, deliveryConditionsByRegionIds")
                .contains(
                    new Tuple(1, Arrays.asList(1, 2)),
                    new Tuple(2, Collections.singletonList(3))
                );
        });

    }

    @Test
    void builderTestWithNullThresholdsAndWeight() {
        List<DeliveryDistributorParamsProjectionDto> deliveryDistributorParams =
            getDeliveryExperimentParamsWithEmptyParams();

        DeliveryConditionsBuilder deliveryConditionsBuilder = new DeliveryConditionsBuilder();
        Logistics.DeliveryConditionsMetaInfo result = deliveryConditionsBuilder.build(deliveryDistributorParams);

        SoftAssertions.assertSoftly(softAssertions -> {

            softAssertions.assertThat(result.getWeightBoundsList()).isEmpty();

            softAssertions.assertThat(result.getWeightRangesList()).isEmpty();

            softAssertions.assertThat(result.getDeliveryThresholdsList())
                .extracting(Logistics.DeliveryThresholdsInfo::getId, Logistics.DeliveryThresholdsInfo::getPrice,
                    Logistics.DeliveryThresholdsInfo::getDays)
                .as("threshold info: id, price, delivery days")
                .containsOnly(new Tuple(1, 0.0, 0));

            softAssertions.assertThat(result.getDeliveryConditionsList())
                .extracting(Logistics.DeliveryConditionsReferenceInfo::getId,
                    Logistics.DeliveryConditionsReferenceInfo::getDeliveryThresholdsId,
                    Logistics.DeliveryConditionsReferenceInfo::getWeightRangeId)
                .as("condition list: id, thresholdId, weightRangeId")
                .containsOnly(new Tuple(1, 1, 0));

            softAssertions.assertThat(result.getDeliveryConditionsByRegionList())
                .extracting(Logistics.DeliveryConditionsByRegionInfo::getId,
                    Logistics.DeliveryConditionsByRegionInfo::getRegionId,
                    Logistics.DeliveryConditionsByRegionInfo::getDeliveryConditionReferenceIdsList)
                .as("grouping delivery condition id by region: " +
                    "id, regionId, deliveryConditionsIdList")
                .containsOnly(
                    new Tuple(1, 1, Collections.singletonList(1))
                );

            softAssertions.assertThat(result.getDeliveryConditionsSplitsList())
                .extracting(Logistics.DeliveryConditionsSplitInfo::getId,
                    Logistics.DeliveryConditionsSplitInfo::getDeliveryConditionsByRegionIdsList)
                .as("grouping conditions by flagId which is grouped by regions: " +
                    "id -> as flag_id, deliveryConditionsByRegionIds")
                .containsOnly(
                    new Tuple(1, Collections.singletonList(1))
                );
        });

    }

    @Test
    void builderTestWithNullThresholdsAndOneWeightParam() {
        List<DeliveryDistributorParamsProjectionDto> deliveryDistributorParams =
            getDeliveryExperimentParamsWithOnlyOneWeightParam();

        DeliveryConditionsBuilder deliveryConditionsBuilder = new DeliveryConditionsBuilder();
        Logistics.DeliveryConditionsMetaInfo result = deliveryConditionsBuilder.build(deliveryDistributorParams);

        SoftAssertions.assertSoftly(softAssertions -> {

            softAssertions.assertThat(result.getWeightBoundsList())
                .extracting(Logistics.WeightBoundInfo::getId, Logistics.WeightBoundInfo::getWeight,
                    Logistics.WeightBoundInfo::getIncluding)
                .as("weight bound: id, weight, strict bound")
                .containsOnly(new Tuple(1, 1.0, true));


            softAssertions.assertThat(result.getWeightRangesList())
                .extracting(Logistics.WeightRangeInfo::getId, Logistics.WeightRangeInfo::getWeightLowerBoundId,
                    Logistics.WeightRangeInfo::getWeightUpperBoundId)
                .as("weight range: id, lowerId, upperId")
                .containsOnly(new Tuple(1, 1, 0));

            softAssertions.assertThat(result.getDeliveryThresholdsList())
                .extracting(Logistics.DeliveryThresholdsInfo::getId, Logistics.DeliveryThresholdsInfo::getPrice,
                    Logistics.DeliveryThresholdsInfo::getDays)
                .as("threshold info: id, price, delivery days")
                .containsOnly(new Tuple(1, 0.0, 0));

            softAssertions.assertThat(result.getDeliveryConditionsList())
                .extracting(Logistics.DeliveryConditionsReferenceInfo::getId,
                    Logistics.DeliveryConditionsReferenceInfo::getDeliveryThresholdsId,
                    Logistics.DeliveryConditionsReferenceInfo::getWeightRangeId)
                .as("condition list: id, thresholdId, weightRangeId")
                .containsOnly(new Tuple(1, 1, 1));

            softAssertions.assertThat(result.getDeliveryConditionsByRegionList())
                .extracting(Logistics.DeliveryConditionsByRegionInfo::getId,
                    Logistics.DeliveryConditionsByRegionInfo::getRegionId,
                    Logistics.DeliveryConditionsByRegionInfo::getDeliveryConditionReferenceIdsList)
                .as("grouping delivery condition id by region: " +
                    "id, regionId, deliveryConditionsIdList")
                .containsOnly(
                    new Tuple(1, 1, Collections.singletonList(1))
                );

            softAssertions.assertThat(result.getDeliveryConditionsSplitsList())
                .extracting(Logistics.DeliveryConditionsSplitInfo::getId,
                    Logistics.DeliveryConditionsSplitInfo::getDeliveryConditionsByRegionIdsList)
                .as("grouping conditions by flagId which is grouped by regions: " +
                    "id -> as flag_id, deliveryConditionsByRegionIds")
                .containsOnly(
                    new Tuple(1, Collections.singletonList(1))
                );
        });

    }

    @Test
    void builderTestDefaultDeliveryId() {
        List<DeliveryDistributorParamsProjectionDto> deliveryDistributorParams = List.of(
            new DeliveryDistributorParamsProjectionDto()
                .setId(5L)
                .setLocationId(2)
                .setFlagId(null) //This makes experiment default
                .setMinWeight(new BigDecimal(1))
                .setMaxWeight(new BigDecimal(2))
                .setStrictBoundsType(StrictBoundsType.NONE)
                .setDeliveryDuration(3)
                .setDeliveryCost(new BigDecimal("750"))
        );

        DeliveryConditionsBuilder deliveryConditionsBuilder = new DeliveryConditionsBuilder();
        Logistics.DeliveryConditionsMetaInfo result = deliveryConditionsBuilder.build(deliveryDistributorParams);
        Assertions.assertThat(result.getDefaultDeliveryConditionsSplitId()).isEqualTo(0);
    }

    @Test
    void builderDuplicationTest() {
        List<DeliveryDistributorParamsProjectionDto> deliveryDistributorParams =
            getDeliveryDistributorParamsForDeduplicate();

        DeliveryConditionsBuilder deliveryConditionsBuilder = new DeliveryConditionsBuilder();
        Logistics.DeliveryConditionsMetaInfo result = deliveryConditionsBuilder.build(deliveryDistributorParams);

        SoftAssertions.assertSoftly(softAssertions -> {

            softAssertions.assertThat(result.getWeightBoundsList())
                .extracting(Logistics.WeightBoundInfo::getId, Logistics.WeightBoundInfo::getWeight,
                    Logistics.WeightBoundInfo::getIncluding)
                .as("weight bound: id, weight, strict bound")
                .containsOnly(new Tuple(1, 1.0, true));


            softAssertions.assertThat(result.getWeightRangesList())
                .extracting(Logistics.WeightRangeInfo::getId, Logistics.WeightRangeInfo::getWeightLowerBoundId,
                    Logistics.WeightRangeInfo::getWeightUpperBoundId)
                .as("weight range: id, lowerId, upperId")
                .containsOnly(new Tuple(1, 1, 1));

            softAssertions.assertThat(result.getDeliveryThresholdsList())
                .extracting(Logistics.DeliveryThresholdsInfo::getId, Logistics.DeliveryThresholdsInfo::getPrice,
                    Logistics.DeliveryThresholdsInfo::getDays)
                .as("threshold info: id, price, delivery days")
                .containsOnly(new Tuple(1, 100.0, 1));

            softAssertions.assertThat(result.getDeliveryConditionsList())
                .extracting(Logistics.DeliveryConditionsReferenceInfo::getId,
                    Logistics.DeliveryConditionsReferenceInfo::getDeliveryThresholdsId,
                    Logistics.DeliveryConditionsReferenceInfo::getWeightRangeId)
                .as("condition list: id, thresholdId, weightRangeId")
                .containsOnly(new Tuple(1, 1, 1));

            softAssertions.assertThat(result.getDeliveryConditionsByRegionList())
                .extracting(Logistics.DeliveryConditionsByRegionInfo::getId,
                    Logistics.DeliveryConditionsByRegionInfo::getRegionId,
                    Logistics.DeliveryConditionsByRegionInfo::getDeliveryConditionReferenceIdsList)
                .as("grouping delivery condition id by region: " +
                    "id, regionId, deliveryConditionsIdList")
                .containsOnly(
                    new Tuple(1, 1, Collections.singletonList(1))
                );

            softAssertions.assertThat(result.getDeliveryConditionsSplitsList())
                .extracting(Logistics.DeliveryConditionsSplitInfo::getId,
                    Logistics.DeliveryConditionsSplitInfo::getDeliveryConditionsByRegionIdsList)
                .as("grouping conditions by flagId which is grouped by regions: " +
                    "id -> as flag_id, deliveryConditionsByRegionIds")
                .containsOnly(
                    new Tuple(1, Collections.singletonList(1)),
                    new Tuple(2, Collections.singletonList(1)),
                    new Tuple(3, Collections.singletonList(1))
                );
        });
    }

    private List<DeliveryDistributorParamsProjectionDto> getDeliveryDistributorParamsForDeduplicate() {
        return Arrays.asList(new DeliveryDistributorParamsProjectionDto()
                .setId(1L)
                .setLocationId(1)
                .setFlagId(1)
                .setMinWeight(new BigDecimal(1))
                .setMaxWeight(new BigDecimal(1))
                .setStrictBoundsType(StrictBoundsType.FULL)
                .setDeliveryDuration(1)
                .setDeliveryCost(new BigDecimal("100")),
            new DeliveryDistributorParamsProjectionDto()
                .setId(2L)
                .setLocationId(1)
                .setFlagId(2)
                .setMinWeight(new BigDecimal(1))
                .setMaxWeight(new BigDecimal(1))
                .setStrictBoundsType(StrictBoundsType.FULL)
                .setDeliveryDuration(1)
                .setDeliveryCost(new BigDecimal("100")),
            new DeliveryDistributorParamsProjectionDto()
                .setId(3L)
                .setLocationId(1)
                .setFlagId(3)
                .setMinWeight(new BigDecimal(1))
                .setMaxWeight(new BigDecimal(1))
                .setStrictBoundsType(StrictBoundsType.FULL)
                .setDeliveryDuration(1)
                .setDeliveryCost(new BigDecimal("100")));
    }

    private List<DeliveryDistributorParamsProjectionDto> getDeliveryExperimentParams() {
        return Arrays.asList(
            new DeliveryDistributorParamsProjectionDto()
                .setId(1L)
                .setLocationId(1)
                .setFlagId(1)
                .setMinWeight(new BigDecimal(3))
                .setMaxWeight(new BigDecimal(10))
                .setStrictBoundsType(StrictBoundsType.LEFT)
                .setDeliveryDuration(2)
                .setDeliveryCost(new BigDecimal("1000")),
            new DeliveryDistributorParamsProjectionDto()
                .setId(2L)
                .setLocationId(1)
                .setFlagId(1)
                .setMinWeight(new BigDecimal(10))
                .setMaxWeight(new BigDecimal(50))
                .setStrictBoundsType(StrictBoundsType.RIGHT)
                .setDeliveryDuration(3)
                .setDeliveryCost(new BigDecimal("5000")),
            new DeliveryDistributorParamsProjectionDto()
                .setId(3L)
                .setLocationId(2)
                .setFlagId(2)
                .setMinWeight(new BigDecimal(1))
                .setMaxWeight(new BigDecimal(5))
                .setStrictBoundsType(StrictBoundsType.NONE)
                .setDeliveryDuration(1)
                .setDeliveryCost(new BigDecimal("500")),
            new DeliveryDistributorParamsProjectionDto()
                .setId(4L)
                .setLocationId(2)
                .setFlagId(1)
                .setMinWeight(new BigDecimal(6))
                .setMaxWeight(new BigDecimal(12))
                .setStrictBoundsType(StrictBoundsType.FULL)
                .setDeliveryDuration(10)
                .setDeliveryCost(new BigDecimal("100"))
        );
    }

    private List<DeliveryDistributorParamsProjectionDto> getDeliveryExperimentParamsWithEmptyParams() {
        return Collections.singletonList(
            new DeliveryDistributorParamsProjectionDto()
                .setId(1L)
                .setLocationId(1)
                .setFlagId(1)
                .setMinWeight(null)
                .setMaxWeight(null)
                .setStrictBoundsType(StrictBoundsType.FULL)
                .setDeliveryDuration(null)
                .setDeliveryCost(null)

        );
    }

    private List<DeliveryDistributorParamsProjectionDto> getDeliveryExperimentParamsWithOnlyOneWeightParam() {
        return Collections.singletonList(
            new DeliveryDistributorParamsProjectionDto()
                .setId(1L)
                .setLocationId(1)
                .setFlagId(1)
                .setMinWeight(BigDecimal.ONE)
                .setMaxWeight(null)
                .setStrictBoundsType(StrictBoundsType.FULL)
                .setDeliveryDuration(null)
                .setDeliveryCost(null)

        );
    }
}
