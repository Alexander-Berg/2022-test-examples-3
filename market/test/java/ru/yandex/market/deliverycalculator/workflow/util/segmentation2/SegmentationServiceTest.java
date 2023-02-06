package ru.yandex.market.deliverycalculator.workflow.util.segmentation2;

import java.io.BufferedInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.xml.bind.JAXBContext;

import com.google.common.collect.Sets;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.delivery.net.protobuf.DeliveryCalcProtos;
import ru.yandex.market.deliverycalculator.PbSnUtils;
import ru.yandex.market.deliverycalculator.model.DeliveryRule;
import ru.yandex.market.deliverycalculator.workflow.TariffGrid;
import ru.yandex.market.deliverycalculator.workflow.daas.model.DaasCourierMetaRule;
import ru.yandex.market.deliverycalculator.workflow.daas.model.DaasCourierMetaTariff;
import ru.yandex.market.deliverycalculator.workflow.mardobasemodel.rule.BaseMardoMetaRule;
import ru.yandex.market.deliverycalculator.workflow.test.AssertionsTestUtils;
import ru.yandex.market.deliverycalculator.workflow.test.WorkflowTestUtils;
import ru.yandex.market.deliverycalculator.workflow.util.XmlUtils;
import ru.yandex.market.deliverycalculator.workflow.util.yadelivery.RegionsPair;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SegmentationServiceTest {

    private static final JAXBContext JAXB_CONTEXT =
            XmlUtils.jaxbContext(DaasCourierMetaTariff.class, DaasCourierMetaRule.class);

    private SegmentationService segmentationService;

    @BeforeEach
    void setUp() {
        segmentationService = new SegmentationService();
    }

    @Test
    void courierSegment() {

        DeliveryRule rootRule = new DeliveryRule();
        DeliveryRule locationRule1 = WorkflowTestUtils.createLocationRule(rootRule, Sets.newHashSet(1), Sets.newHashSet(2, 3));
        DeliveryRule locationRule2 = WorkflowTestUtils.createLocationRule(rootRule, Sets.newHashSet(4, 5), Sets.newHashSet(6));

        final DeliveryRule weightRule11 = WorkflowTestUtils.createWeightRule(locationRule1, null, 5d);
        final DeliveryRule weightRule12 = WorkflowTestUtils.createWeightRule(locationRule1, 3d, null);

        final DeliveryRule weightRule21 = WorkflowTestUtils.createWeightRule(locationRule2, 3d, 7d);
        final DeliveryRule weightRule22 = WorkflowTestUtils.createWeightRule(locationRule2, 4d, 9d);

        WorkflowTestUtils.createDeliveryOption(weightRule11, 1, 1, 1, 1);
        WorkflowTestUtils.createDeliveryOption(weightRule11, 2, 2, 2, 2);

        WorkflowTestUtils.createDeliveryOption(weightRule12, 3, 3, 3, 3);
        WorkflowTestUtils.createDeliveryOption(weightRule12, 4, 4, 4, 4);

        WorkflowTestUtils.createDeliveryOption(weightRule21, 5, 5, 5, 5);
        WorkflowTestUtils.createDeliveryOption(weightRule21, 6, 6, 6, 6);

        WorkflowTestUtils.createDeliveryOption(weightRule22, 7, 7, 7, 7);
        WorkflowTestUtils.createDeliveryOption(weightRule22, 8, 8, 8, 8);

        final List<DeliveryRule> rules = new ArrayList<>();
        fillRules(rootRule, rules);

        final Map<WeightRange, Map<RegionsPair, Set<DeliveryCalcProtos.DeliveryOption>>> actual = new LinkedHashMap<>();
        segmentationService.courierSegment(rules, (wr, value) -> addWeightRangeToResultMap(wr, value, actual));

        Assertions.assertThat(actual).hasSize(6);
        Assertions.assertThat(actual).containsOnlyKeys(
                new WeightRange(-1, 3), new WeightRange(3, 4), new WeightRange(4, 5),
                new WeightRange(5, 7), new WeightRange(7, 9), new WeightRange(9, Double.MAX_VALUE)
        );

        assertCourierWeightRange3(actual);
        assertCourierWeightRange34(actual);
        assertCourierWeightRange45(actual);
        assertCourierWeightRange57(actual);
        assertCourierWeightRange79(actual);
        assertCourierWeightRange9(actual);
    }

    private void assertCourierWeightRange3(Map<WeightRange, Map<RegionsPair, Set<DeliveryCalcProtos.DeliveryOption>>> actual) {
        final Map<RegionsPair, Set<DeliveryCalcProtos.DeliveryOption>> map = actual.get(new WeightRange(-1, 3));
        Assertions.assertThat(map).hasSize(2);
        Assertions.assertThat(map).containsOnlyKeys(
                new RegionsPair(1, 2), new RegionsPair(1, 3)
        );

        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions12 = map.get(new RegionsPair(1, 2));
        Assertions.assertThat(deliveryOptions12).containsOnly(
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(1, 1, 1, 1),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(2, 2, 2, 2)
        );

        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions13 = map.get(new RegionsPair(1, 3));
        Assertions.assertThat(deliveryOptions13).containsOnly(
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(1, 1, 1, 1),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(2, 2, 2, 2)
        );
    }

    private void assertCourierWeightRange34(Map<WeightRange, Map<RegionsPair, Set<DeliveryCalcProtos.DeliveryOption>>> actual) {
        final Map<RegionsPair, Set<DeliveryCalcProtos.DeliveryOption>> map = actual.get(new WeightRange(3, 4));
        Assertions.assertThat(map).hasSize(4);
        Assertions.assertThat(map).containsOnlyKeys(
                new RegionsPair(1, 2), new RegionsPair(1, 3),
                new RegionsPair(4, 6), new RegionsPair(5, 6)
        );

        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions12 = map.get(new RegionsPair(1, 2));
        Assertions.assertThat(deliveryOptions12).containsOnly(
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(1, 1, 1, 1),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(2, 2, 2, 2),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(3, 3, 3, 3),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(4, 4, 4, 4)
        );

        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions13 = map.get(new RegionsPair(1, 3));
        Assertions.assertThat(deliveryOptions13).containsOnly(
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(1, 1, 1, 1),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(2, 2, 2, 2),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(3, 3, 3, 3),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(4, 4, 4, 4)
        );

        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions46 = map.get(new RegionsPair(4, 6));
        Assertions.assertThat(deliveryOptions46).containsOnly(
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(5, 5, 5, 5),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(6, 6, 6, 6)
        );

        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions56 = map.get(new RegionsPair(5, 6));
        Assertions.assertThat(deliveryOptions56).containsOnly(
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(5, 5, 5, 5),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(6, 6, 6, 6)
        );
    }

    private void assertCourierWeightRange45(Map<WeightRange, Map<RegionsPair, Set<DeliveryCalcProtos.DeliveryOption>>> actual) {
        final Map<RegionsPair, Set<DeliveryCalcProtos.DeliveryOption>> map = actual.get(new WeightRange(4, 5));
        Assertions.assertThat(map).hasSize(4);
        Assertions.assertThat(map).containsOnlyKeys(
                new RegionsPair(1, 2), new RegionsPair(1, 3),
                new RegionsPair(4, 6), new RegionsPair(5, 6)
        );

        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions12 = map.get(new RegionsPair(1, 2));
        Assertions.assertThat(deliveryOptions12).containsOnly(
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(1, 1, 1, 1),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(2, 2, 2, 2),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(3, 3, 3, 3),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(4, 4, 4, 4)
        );

        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions13 = map.get(new RegionsPair(1, 3));
        Assertions.assertThat(deliveryOptions13).containsOnly(
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(1, 1, 1, 1),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(2, 2, 2, 2),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(3, 3, 3, 3),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(4, 4, 4, 4)
        );

        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions46 = map.get(new RegionsPair(4, 6));
        Assertions.assertThat(deliveryOptions46).containsOnly(
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(5, 5, 5, 5),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(6, 6, 6, 6),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(7, 7, 7, 7),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(8, 8, 8, 8)
        );

        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions56 = map.get(new RegionsPair(5, 6));
        Assertions.assertThat(deliveryOptions56).containsOnly(
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(5, 5, 5, 5),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(6, 6, 6, 6),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(7, 7, 7, 7),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(8, 8, 8, 8)
        );
    }

    private void assertCourierWeightRange57(Map<WeightRange, Map<RegionsPair, Set<DeliveryCalcProtos.DeliveryOption>>> actual) {
        final Map<RegionsPair, Set<DeliveryCalcProtos.DeliveryOption>> map = actual.get(new WeightRange(5, 7));
        Assertions.assertThat(map).hasSize(4);
        Assertions.assertThat(map).containsOnlyKeys(
                new RegionsPair(1, 2), new RegionsPair(1, 3),
                new RegionsPair(4, 6), new RegionsPair(5, 6)
        );

        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions12 = map.get(new RegionsPair(1, 2));
        Assertions.assertThat(deliveryOptions12).containsOnly(
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(3, 3, 3, 3),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(4, 4, 4, 4)
        );

        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions13 = map.get(new RegionsPair(1, 3));
        Assertions.assertThat(deliveryOptions13).containsOnly(
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(3, 3, 3, 3),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(4, 4, 4, 4)
        );

        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions46 = map.get(new RegionsPair(4, 6));
        Assertions.assertThat(deliveryOptions46).containsOnly(
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(5, 5, 5, 5),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(6, 6, 6, 6),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(7, 7, 7, 7),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(8, 8, 8, 8)
        );

        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions56 = map.get(new RegionsPair(5, 6));
        Assertions.assertThat(deliveryOptions56).containsOnly(
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(5, 5, 5, 5),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(6, 6, 6, 6),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(7, 7, 7, 7),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(8, 8, 8, 8)
        );
    }

    private void assertCourierWeightRange79(Map<WeightRange, Map<RegionsPair, Set<DeliveryCalcProtos.DeliveryOption>>> actual) {
        final Map<RegionsPair, Set<DeliveryCalcProtos.DeliveryOption>> map = actual.get(new WeightRange(7, 9));
        Assertions.assertThat(map).hasSize(4);
        Assertions.assertThat(map).containsOnlyKeys(
                new RegionsPair(1, 2), new RegionsPair(1, 3),
                new RegionsPair(4, 6), new RegionsPair(5, 6)
        );

        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions12 = map.get(new RegionsPair(1, 2));
        Assertions.assertThat(deliveryOptions12).containsOnly(
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(3, 3, 3, 3),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(4, 4, 4, 4)
        );

        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions13 = map.get(new RegionsPair(1, 3));
        Assertions.assertThat(deliveryOptions13).containsOnly(
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(3, 3, 3, 3),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(4, 4, 4, 4)
        );

        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions46 = map.get(new RegionsPair(4, 6));
        Assertions.assertThat(deliveryOptions46).containsOnly(
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(7, 7, 7, 7),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(8, 8, 8, 8)
        );

        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions56 = map.get(new RegionsPair(5, 6));
        Assertions.assertThat(deliveryOptions56).containsOnly(
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(7, 7, 7, 7),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(8, 8, 8, 8)
        );
    }

    private void assertCourierWeightRange9(Map<WeightRange, Map<RegionsPair, Set<DeliveryCalcProtos.DeliveryOption>>> actual) {
        final Map<RegionsPair, Set<DeliveryCalcProtos.DeliveryOption>> map = actual.get(new WeightRange(9, Double.MAX_VALUE));
        Assertions.assertThat(map).hasSize(2);
        Assertions.assertThat(map).containsOnlyKeys(
                new RegionsPair(1, 2), new RegionsPair(1, 3)
        );

        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions12 = map.get(new RegionsPair(1, 2));
        Assertions.assertThat(deliveryOptions12).containsOnly(
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(3, 3, 3, 3),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(4, 4, 4, 4)
        );

        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions13 = map.get(new RegionsPair(1, 3));
        Assertions.assertThat(deliveryOptions13).containsOnly(
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(3, 3, 3, 3),
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(4, 4, 4, 4)
        );
    }

    @Test
    void pickupSegment() {

        DeliveryRule rootOfferRule = WorkflowTestUtils.createOfferRule(null, 31.0, 60.0, 60.0, 60.0, 180.0);

        // первый location from (Москва, id 213)
        DeliveryRule locationFromMoscowRule = WorkflowTestUtils.createLocationRule(rootOfferRule, Sets.newHashSet(213), null);
        DeliveryRule locationTo197Rule = WorkflowTestUtils.createLocationRule(locationFromMoscowRule, null, Sets.newHashSet(197));
        DeliveryRule childOfferRule1 = WorkflowTestUtils.createWeightRule(locationTo197Rule, null, 1.0);
        WorkflowTestUtils.createDeliveryOption(childOfferRule1, 17700, 3, 6, null);
        DeliveryRule childOfferRule2 = WorkflowTestUtils.createWeightRule(locationTo197Rule, 1.0, 2.0);
        WorkflowTestUtils.createDeliveryOption(childOfferRule2, 21200, 3, 6, null);
        DeliveryRule childOfferRule3 = WorkflowTestUtils.createOfferRule(locationTo197Rule, 15.0, 60.0, 57.0, 56.0, 175.0);
        WorkflowTestUtils.createPickpoint(childOfferRule3, 265221);
        DeliveryRule childOfferRule4 = WorkflowTestUtils.createOfferRule(locationTo197Rule, 32.0, 60.0, 60.0, 60.0, 180.0);
        WorkflowTestUtils.createPickpoint(childOfferRule4, 538322);
        WorkflowTestUtils.createPickpoint(childOfferRule4, 266979);

        DeliveryRule locationTo1093Rule = WorkflowTestUtils.createLocationRule(locationFromMoscowRule, null, Sets.newHashSet(1093));
        DeliveryRule childOfferRule21 = WorkflowTestUtils.createWeightRule(locationTo1093Rule, null, 2.0);
        WorkflowTestUtils.createDeliveryOption(childOfferRule21, 20800, 5, 5, null);
        DeliveryRule childOfferRule22 = WorkflowTestUtils.createOfferRule(locationTo1093Rule, 50.0, 60.0, 60.0, 60.0, 180.0);
        WorkflowTestUtils.createPickpoint(childOfferRule22, 538337);

        // второй location from (Ростов-на-Дону, id 39)
        DeliveryRule locationFromRostovOnDonRule = WorkflowTestUtils.createLocationRule(rootOfferRule, Sets.newHashSet(39), null);
        DeliveryRule locationTo116987Rule = WorkflowTestUtils.createLocationRule(locationFromRostovOnDonRule, null, Sets.newHashSet(116987));
        DeliveryRule childOfferRule31 = WorkflowTestUtils.createWeightRule(locationTo116987Rule, null, 1.0);
        WorkflowTestUtils.createDeliveryOption(childOfferRule31, 20300, 1, 3, null);
        DeliveryRule childOfferRule32 = WorkflowTestUtils.createOfferRule(locationTo116987Rule, 20.0, 60.0, 60.0, 60.0, 180.0);
        WorkflowTestUtils.createPickpoint(childOfferRule32, 536634);

        final List<DeliveryRule> rules = new ArrayList<>();
        fillRules(rootOfferRule, rules);

        final Map<WeightRange, Map<RegionsPair, PickupSegmentationValue>> actual = new LinkedHashMap<>();
        segmentationService.pickupSegment(rules, (wr, value) -> addWeightRangeToResultMap(wr, value, actual));

        Assertions.assertThat(actual).hasSize(6);
        Assertions.assertThat(actual).containsOnlyKeys(
                new WeightRange(-1, 1), new WeightRange(1, 2), new WeightRange(2, 15),
                new WeightRange(15, 20), new WeightRange(20, 32), new WeightRange(32, 50)
        );

        assertPickupWeightRange1(actual);
        assertPickupWeightRange2(actual);
        assertPickupWeightRange3(actual);
        assertPickupWeightRange4(actual);
        assertPickupWeightRange5(actual);
        assertPickupWeightRange6(actual);
    }

    private void assertPickupWeightRange1(Map<WeightRange, Map<RegionsPair, PickupSegmentationValue>> actual) {
        final Map<RegionsPair, PickupSegmentationValue> map = actual.get(new WeightRange(-1, 1));
        Assertions.assertThat(map).hasSize(3);
        Assertions.assertThat(map).containsOnlyKeys(
                new RegionsPair(213, 197), new RegionsPair(213, 1093), new RegionsPair(39, 116987)
        );

        final PickupSegmentationValue segmentationValue1 = map.get(new RegionsPair(213, 197));
        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions1 = segmentationValue1.getDeliveryOptions();
        Assertions.assertThat(deliveryOptions1).containsOnly(
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(17700, 3, 6, 13)
        );

        final Map<OutletDimensions, Set<Long>> pickupPointIds1 = segmentationValue1.getPickupPointIds();
        Assertions.assertThat(pickupPointIds1).containsOnlyKeys(
                new OutletDimensions(new double[]{56, 57, 60}, 175), new OutletDimensions(new double[]{60, 60, 60}, 180)
        );

        final Set<Long> pickupPointIds11 = pickupPointIds1.get(new OutletDimensions(new double[]{56, 57, 60}, 175));
        Assertions.assertThat(pickupPointIds11).containsOnly(265221L);

        final Set<Long> pickupPointIds12 = pickupPointIds1.get(new OutletDimensions(new double[]{60, 60, 60}, 180));
        Assertions.assertThat(pickupPointIds12).containsOnly(266979L, 538322L);


        final PickupSegmentationValue segmentationValue2 = map.get(new RegionsPair(213, 1093));
        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions2 = segmentationValue2.getDeliveryOptions();
        Assertions.assertThat(deliveryOptions2).containsOnly(
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(20800, 5, 5, 13)
        );

        final Map<OutletDimensions, Set<Long>> pickupPointIds2 = segmentationValue2.getPickupPointIds();
        Assertions.assertThat(pickupPointIds2).containsOnlyKeys(
                new OutletDimensions(new double[]{60, 60, 60}, 180)
        );

        final Set<Long> pickupPointIds21 = pickupPointIds2.get(new OutletDimensions(new double[]{60, 60, 60}, 180));
        Assertions.assertThat(pickupPointIds21).containsOnly(538337L);


        final PickupSegmentationValue segmentationValue3 = map.get(new RegionsPair(39, 116987));
        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions3 = segmentationValue3.getDeliveryOptions();
        Assertions.assertThat(deliveryOptions3).containsOnly(
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(20300, 1, 3, 13)
        );

        final Map<OutletDimensions, Set<Long>> pickupPointIds3 = segmentationValue3.getPickupPointIds();
        Assertions.assertThat(pickupPointIds3).containsOnlyKeys(
                new OutletDimensions(new double[]{60, 60, 60}, 180)
        );

        final Set<Long> pickupPointIds31 = pickupPointIds3.get(new OutletDimensions(new double[]{60, 60, 60}, 180));
        Assertions.assertThat(pickupPointIds31).containsOnly(536634L);
    }

    private void assertPickupWeightRange2(Map<WeightRange, Map<RegionsPair, PickupSegmentationValue>> actual) {
        final Map<RegionsPair, PickupSegmentationValue> map = actual.get(new WeightRange(1, 2));
        Assertions.assertThat(map).hasSize(3);
        Assertions.assertThat(map).containsOnlyKeys(
                new RegionsPair(213, 197), new RegionsPair(213, 1093), new RegionsPair(39, 116987)
        );

        final PickupSegmentationValue segmentationValue1 = map.get(new RegionsPair(213, 197));
        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions1 = segmentationValue1.getDeliveryOptions();
        Assertions.assertThat(deliveryOptions1).containsOnly(
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(21200, 3, 6, 13)
        );

        final Map<OutletDimensions, Set<Long>> pickupPointIds1 = segmentationValue1.getPickupPointIds();
        Assertions.assertThat(pickupPointIds1).containsOnlyKeys(
                new OutletDimensions(new double[]{56, 57, 60}, 175), new OutletDimensions(new double[]{60, 60, 60}, 180)
        );

        final Set<Long> pickupPointIds11 = pickupPointIds1.get(new OutletDimensions(new double[]{56, 57, 60}, 175));
        Assertions.assertThat(pickupPointIds11).containsOnly(265221L);

        final Set<Long> pickupPointIds12 = pickupPointIds1.get(new OutletDimensions(new double[]{60, 60, 60}, 180));
        Assertions.assertThat(pickupPointIds12).containsOnly(266979L, 538322L);


        final PickupSegmentationValue segmentationValue2 = map.get(new RegionsPair(213, 1093));
        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions2 = segmentationValue2.getDeliveryOptions();
        Assertions.assertThat(deliveryOptions2).containsOnly(
                AssertionsTestUtils.buildDeliveryOptionFromRequiredParams(20800, 5, 5, 13)
        );

        final Map<OutletDimensions, Set<Long>> pickupPointIds2 = segmentationValue2.getPickupPointIds();
        Assertions.assertThat(pickupPointIds2).containsOnlyKeys(
                new OutletDimensions(new double[]{60, 60, 60}, 180)
        );

        final Set<Long> pickupPointIds21 = pickupPointIds2.get(new OutletDimensions(new double[]{60, 60, 60}, 180));
        Assertions.assertThat(pickupPointIds21).containsOnly(538337L);


        final PickupSegmentationValue segmentationValue3 = map.get(new RegionsPair(39, 116987));
        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions3 = segmentationValue3.getDeliveryOptions();
        Assertions.assertThat(deliveryOptions3).isEmpty();

        final Map<OutletDimensions, Set<Long>> pickupPointIds3 = segmentationValue3.getPickupPointIds();
        Assertions.assertThat(pickupPointIds3).containsOnlyKeys(
                new OutletDimensions(new double[]{60, 60, 60}, 180)
        );

        final Set<Long> pickupPointIds31 = pickupPointIds3.get(new OutletDimensions(new double[]{60, 60, 60}, 180));
        Assertions.assertThat(pickupPointIds31).containsOnly(536634L);
    }

    private void assertPickupWeightRange3(Map<WeightRange, Map<RegionsPair, PickupSegmentationValue>> actual) {
        final Map<RegionsPair, PickupSegmentationValue> map = actual.get(new WeightRange(2, 15));
        Assertions.assertThat(map).hasSize(3);
        Assertions.assertThat(map).containsOnlyKeys(
                new RegionsPair(213, 197), new RegionsPair(213, 1093), new RegionsPair(39, 116987)
        );

        final PickupSegmentationValue segmentationValue1 = map.get(new RegionsPair(213, 197));
        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions1 = segmentationValue1.getDeliveryOptions();
        Assertions.assertThat(deliveryOptions1).isEmpty();

        final Map<OutletDimensions, Set<Long>> pickupPointIds1 = segmentationValue1.getPickupPointIds();
        Assertions.assertThat(pickupPointIds1).containsOnlyKeys(
                new OutletDimensions(new double[]{56, 57, 60}, 175), new OutletDimensions(new double[]{60, 60, 60}, 180)
        );

        final Set<Long> pickupPointIds11 = pickupPointIds1.get(new OutletDimensions(new double[]{56, 57, 60}, 175));
        Assertions.assertThat(pickupPointIds11).containsOnly(265221L);

        final Set<Long> pickupPointIds12 = pickupPointIds1.get(new OutletDimensions(new double[]{60, 60, 60}, 180));
        Assertions.assertThat(pickupPointIds12).containsOnly(266979L, 538322L);


        final PickupSegmentationValue segmentationValue2 = map.get(new RegionsPair(213, 1093));
        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions2 = segmentationValue2.getDeliveryOptions();
        Assertions.assertThat(deliveryOptions2).isEmpty();

        final Map<OutletDimensions, Set<Long>> pickupPointIds2 = segmentationValue2.getPickupPointIds();
        Assertions.assertThat(pickupPointIds2).containsOnlyKeys(
                new OutletDimensions(new double[]{60, 60, 60}, 180)
        );

        final Set<Long> pickupPointIds21 = pickupPointIds2.get(new OutletDimensions(new double[]{60, 60, 60}, 180));
        Assertions.assertThat(pickupPointIds21).containsOnly(538337L);


        final PickupSegmentationValue segmentationValue3 = map.get(new RegionsPair(39, 116987));
        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions3 = segmentationValue3.getDeliveryOptions();
        Assertions.assertThat(deliveryOptions3).isEmpty();

        final Map<OutletDimensions, Set<Long>> pickupPointIds3 = segmentationValue3.getPickupPointIds();
        Assertions.assertThat(pickupPointIds3).containsOnlyKeys(
                new OutletDimensions(new double[]{60, 60, 60}, 180)
        );

        final Set<Long> pickupPointIds31 = pickupPointIds3.get(new OutletDimensions(new double[]{60, 60, 60}, 180));
        Assertions.assertThat(pickupPointIds31).containsOnly(536634L);
    }

    private void assertPickupWeightRange4(Map<WeightRange, Map<RegionsPair, PickupSegmentationValue>> actual) {
        final Map<RegionsPair, PickupSegmentationValue> map = actual.get(new WeightRange(15, 20));
        Assertions.assertThat(map).hasSize(3);
        Assertions.assertThat(map).containsOnlyKeys(
                new RegionsPair(213, 197), new RegionsPair(213, 1093), new RegionsPair(39, 116987)
        );

        final PickupSegmentationValue segmentationValue1 = map.get(new RegionsPair(213, 197));
        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions1 = segmentationValue1.getDeliveryOptions();
        Assertions.assertThat(deliveryOptions1).isEmpty();

        final Map<OutletDimensions, Set<Long>> pickupPointIds1 = segmentationValue1.getPickupPointIds();
        Assertions.assertThat(pickupPointIds1).containsOnlyKeys(
                new OutletDimensions(new double[]{60, 60, 60}, 180)
        );

        final Set<Long> pickupPointIds11 = pickupPointIds1.get(new OutletDimensions(new double[]{60, 60, 60}, 180));
        Assertions.assertThat(pickupPointIds11).containsOnly(266979L, 538322L);


        final PickupSegmentationValue segmentationValue2 = map.get(new RegionsPair(213, 1093));
        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions2 = segmentationValue2.getDeliveryOptions();
        Assertions.assertThat(deliveryOptions2).isEmpty();

        final Map<OutletDimensions, Set<Long>> pickupPointIds2 = segmentationValue2.getPickupPointIds();
        Assertions.assertThat(pickupPointIds2).containsOnlyKeys(
                new OutletDimensions(new double[]{60, 60, 60}, 180)
        );

        final Set<Long> pickupPointIds21 = pickupPointIds2.get(new OutletDimensions(new double[]{60, 60, 60}, 180));
        Assertions.assertThat(pickupPointIds21).containsOnly(538337L);


        final PickupSegmentationValue segmentationValue3 = map.get(new RegionsPair(39, 116987));
        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions3 = segmentationValue3.getDeliveryOptions();
        Assertions.assertThat(deliveryOptions3).isEmpty();

        final Map<OutletDimensions, Set<Long>> pickupPointIds3 = segmentationValue3.getPickupPointIds();
        Assertions.assertThat(pickupPointIds3).containsOnlyKeys(
                new OutletDimensions(new double[]{60, 60, 60}, 180)
        );

        final Set<Long> pickupPointIds31 = pickupPointIds3.get(new OutletDimensions(new double[]{60, 60, 60}, 180));
        Assertions.assertThat(pickupPointIds31).containsOnly(536634L);
    }

    private void assertPickupWeightRange5(Map<WeightRange, Map<RegionsPair, PickupSegmentationValue>> actual) {
        final Map<RegionsPair, PickupSegmentationValue> map = actual.get(new WeightRange(20, 32));
        Assertions.assertThat(map).hasSize(2);
        Assertions.assertThat(map).containsOnlyKeys(
                new RegionsPair(213, 197), new RegionsPair(213, 1093)
        );

        final PickupSegmentationValue segmentationValue1 = map.get(new RegionsPair(213, 197));
        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions1 = segmentationValue1.getDeliveryOptions();
        Assertions.assertThat(deliveryOptions1).isEmpty();

        final Map<OutletDimensions, Set<Long>> pickupPointIds1 = segmentationValue1.getPickupPointIds();
        Assertions.assertThat(pickupPointIds1).containsOnlyKeys(
                new OutletDimensions(new double[]{60, 60, 60}, 180)
        );

        final Set<Long> pickupPointIds11 = pickupPointIds1.get(new OutletDimensions(new double[]{60, 60, 60}, 180));
        Assertions.assertThat(pickupPointIds11).containsOnly(266979L, 538322L);


        final PickupSegmentationValue segmentationValue2 = map.get(new RegionsPair(213, 1093));
        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions2 = segmentationValue2.getDeliveryOptions();
        Assertions.assertThat(deliveryOptions2).isEmpty();

        final Map<OutletDimensions, Set<Long>> pickupPointIds2 = segmentationValue2.getPickupPointIds();
        Assertions.assertThat(pickupPointIds2).containsOnlyKeys(
                new OutletDimensions(new double[]{60, 60, 60}, 180)
        );

        final Set<Long> pickupPointIds21 = pickupPointIds2.get(new OutletDimensions(new double[]{60, 60, 60}, 180));
        Assertions.assertThat(pickupPointIds21).containsOnly(538337L);
    }

    private void assertPickupWeightRange6(Map<WeightRange, Map<RegionsPair, PickupSegmentationValue>> actual) {
        final Map<RegionsPair, PickupSegmentationValue> map = actual.get(new WeightRange(32, 50));
        Assertions.assertThat(map).hasSize(1);
        Assertions.assertThat(map).containsOnlyKeys(
                new RegionsPair(213, 1093)
        );

        final PickupSegmentationValue segmentationValue1 = map.get(new RegionsPair(213, 1093));
        final Set<DeliveryCalcProtos.DeliveryOption> deliveryOptions1 = segmentationValue1.getDeliveryOptions();
        Assertions.assertThat(deliveryOptions1).isEmpty();

        final Map<OutletDimensions, Set<Long>> pickupPointIds1 = segmentationValue1.getPickupPointIds();
        Assertions.assertThat(pickupPointIds1).containsOnlyKeys(
                new OutletDimensions(new double[]{60, 60, 60}, 180)
        );

        final Set<Long> pickupPointIds11 = pickupPointIds1.get(new OutletDimensions(new double[]{60, 60, 60}, 180));
        Assertions.assertThat(pickupPointIds11).containsOnly(538337L);
    }

    /**
     * Рекурсивно обходит дерево правил и складывает все правила в список {@code result}.
     *
     * @param rule   входное правило
     * @param result выходной список
     */
    private void fillRules(DeliveryRule rule, List<DeliveryRule> result) {
        result.add(rule);

        for (DeliveryRule child : CollectionUtils.emptyIfNull(rule.getChildren())) {
            fillRules(child, result);
        }
    }

    @EnabledIfSystemProperty(named = "runManualTests", matches = "true")
    @ParameterizedTest
    @MethodSource("argumentsProvider")
    void compareOldAndNewSegmentationAlgorithms(int tariffIndex, long tariffId) throws Exception {
        final String expectedTariff = TestingTariffs.EXPECTED_TARIFFS.get(tariffIndex);
        final String actualTariff = TestingTariffs.ACTUAL_TARIFFS.get(tariffIndex);
        final String expectedBucketUrl = TestingTariffs.EXPECTED_BUCKETS_URLS.get(tariffIndex);
        final String actualBucketUrl = TestingTariffs.ACTUAL_BUCKETS_URLS.get(tariffIndex);

        DaasCourierMetaTariff expected = XmlUtils.deserialize(JAXB_CONTEXT, expectedTariff);
        DaasCourierMetaTariff actual = XmlUtils.deserialize(JAXB_CONTEXT, actualTariff);

        assertEquals(expected.getTariffId(), actual.getTariffId());
        assertEquals(expected.getCarrierId(), actual.getCarrierId());
        assertEquals(expected.getMinWeight(), actual.getMinWeight());
        assertEquals(expected.getMaxWeight(), actual.getMaxWeight());
        assertArrayEquals(expected.getMinDimension(), actual.getMinDimension());
        assertArrayEquals(expected.getMaxDimension(), actual.getMaxDimension());
        assertEquals(expected.getMinDimSum(), actual.getMinDimSum());
        assertEquals(expected.getMaxDimSum(), actual.getMaxDimSum());
        assertEquals(expected.getVolumeWeightCoefficient(), actual.getVolumeWeightCoefficient());
        assertArrayEquals(expected.getTariffCargoTypesBlacklist(), actual.getTariffCargoTypesBlacklist());
        assertEquals(expected.getPrograms(), actual.getPrograms());

        TariffGrid expectedGrid = TariffGrid.fromRules(expected.getRules());
        TariffGrid actualGrid = TariffGrid.fromRules(actual.getRules());

        assertTrue(expectedGrid.hasTheSameStructure(actualGrid));

        DeliveryCalcProtos.FeedDeliveryOptionsResp expectedFedDeliveryOptionsResp = readFeedDeliveryOptionsResp(expectedBucketUrl);
        DeliveryCalcProtos.FeedDeliveryOptionsResp actualFedDeliveryOptionsResp = readFeedDeliveryOptionsResp(actualBucketUrl);

        assertFeedDeliveryOptionsResp(expected, expectedGrid, expectedFedDeliveryOptionsResp, actual, actualGrid, actualFedDeliveryOptionsResp);
    }

    private void assertFeedDeliveryOptionsResp(
            DaasCourierMetaTariff expected, TariffGrid expectedGrid, DeliveryCalcProtos.FeedDeliveryOptionsResp expectedFedDeliveryOptionsResp,
            DaasCourierMetaTariff actual, TariffGrid actualGrid, DeliveryCalcProtos.FeedDeliveryOptionsResp actualFedDeliveryOptionsResp
    ) {
        DeliveryCalcProtos.DeliveryOptions expectedDeliveryOptions = expectedFedDeliveryOptionsResp.getDeliveryOptionsByFeed();
        DeliveryCalcProtos.DeliveryOptions actualDeliveryOptions = actualFedDeliveryOptionsResp.getDeliveryOptionsByFeed();

        List<DeliveryCalcProtos.DeliveryOptionsBucket> expectedBuckets = expectedDeliveryOptions.getDeliveryOptionBucketsList();
        List<DeliveryCalcProtos.DeliveryOptionsBucket> actualBuckets = actualDeliveryOptions.getDeliveryOptionBucketsList();

        assertEquals(expectedBuckets.size(), actualBuckets.size());
        Map<Long, DeliveryCalcProtos.DeliveryOptionsBucket> expectedBucketsById = getBucketsById(expectedBuckets);
        Map<Long, DeliveryCalcProtos.DeliveryOptionsBucket> actualBucketsById = getBucketsById(actualBuckets);

        final Map<Triple<Integer, Double, Double>, Long> expectedBucketIds = extractBucketIds(expected);
        final Map<Triple<Integer, Double, Double>, Long> actualBucketIds = extractBucketIds(actual);
        assertEquals(expectedBucketIds.size(), actualBucketIds.size());

        Assertions.assertThat(expectedBucketsById.keySet()).containsOnly(new HashSet<>(expectedBucketIds.values()).toArray(new Long[0]));
        Assertions.assertThat(actualBucketsById.keySet()).containsOnly(new HashSet<>(actualBucketIds.values()).toArray(new Long[0]));

        final Map<Long, Long> actualBucketIdByExpectedBucketId = expectedGrid.getOldBucketIdByNewBucketId(actualGrid);
        final Map<Long, Long> expectedBucketIdByActualBucketId = actualGrid.getOldBucketIdByNewBucketId(expectedGrid);
        expectedBucketIds.forEach((key, expectedBucketId) -> {
            final Long actualBucketId = actualBucketIds.get(key);
            assertEquals(actualBucketId, actualBucketIdByExpectedBucketId.get(expectedBucketId));
            assertEquals(expectedBucketId, expectedBucketIdByActualBucketId.get(actualBucketId));
        });

        final List<DeliveryCalcProtos.DeliveryOptionsGroup> expectedGroups = expectedDeliveryOptions.getDeliveryOptionGroupsList();
        final List<DeliveryCalcProtos.DeliveryOptionsGroup> actualGroups = actualDeliveryOptions.getDeliveryOptionGroupsList();
        assertEquals(expectedGroups.size(), actualGroups.size());

        final Map<Long, DeliveryCalcProtos.DeliveryOptionsGroup> expectedGroupsById = getGroupsById(expectedGroups);
        final Map<Long, DeliveryCalcProtos.DeliveryOptionsGroup> actualGroupsById = getGroupsById(actualGroups);

        actualBucketsById.forEach((actualBucketId, actualBucket) -> {
            final Long expectedBucketId = expectedBucketIdByActualBucketId.get(actualBucketId);
            final DeliveryCalcProtos.DeliveryOptionsBucket expectedBucket = expectedBucketsById.get(expectedBucketId);
            assertBucket(expectedBucket, expectedGroupsById, actualBucket, actualGroupsById);
        });
    }

    private Map<Long, DeliveryCalcProtos.DeliveryOptionsGroup> getGroupsById(List<DeliveryCalcProtos.DeliveryOptionsGroup> groups) {
        return groups.stream().collect(Collectors.toMap(DeliveryCalcProtos.DeliveryOptionsGroup::getDeliveryOptionGroupId, Function.identity()));
    }

    private Map<Long, DeliveryCalcProtos.DeliveryOptionsBucket> getBucketsById(List<DeliveryCalcProtos.DeliveryOptionsBucket> buckets) {
        return buckets.stream().collect(Collectors.toMap(DeliveryCalcProtos.DeliveryOptionsBucket::getDeliveryOptBucketId, Function.identity()));
    }

    private void assertBucket(
            DeliveryCalcProtos.DeliveryOptionsBucket expectedBucket, Map<Long, DeliveryCalcProtos.DeliveryOptionsGroup> expectedGroupsById,
            DeliveryCalcProtos.DeliveryOptionsBucket actualBucket, Map<Long, DeliveryCalcProtos.DeliveryOptionsGroup> actualGroupsById
    ) {
        assertEquals(expectedBucket.getCurrency(), actualBucket.getCurrency());
        Assertions.assertThat(expectedBucket.getCarrierIdsList()).containsOnly(actualBucket.getCarrierIdsList().toArray(new Integer[0]));
        assertEquals(expectedBucket.getProgram(), actualBucket.getProgram());
        assertEquals(expectedBucket.getTariffId(), actualBucket.getTariffId());

        final List<DeliveryCalcProtos.DeliveryOptionsGroupRegion> expectedRegions = expectedBucket.getDeliveryOptionGroupRegsList();
        final List<DeliveryCalcProtos.DeliveryOptionsGroupRegion> actualRegions = actualBucket.getDeliveryOptionGroupRegsList();
        assertEquals(expectedRegions.size(), actualRegions.size());

        final Map<Integer, DeliveryCalcProtos.DeliveryOptionsGroupRegion> expectedRegionsById = getRegionsById(expectedRegions);
        final Map<Integer, DeliveryCalcProtos.DeliveryOptionsGroupRegion> actualRegionsById = getRegionsById(actualRegions);
        assertEquals(expectedRegionsById.size(), actualRegionsById.size());
        assertEquals(expectedRegionsById.keySet(), actualRegionsById.keySet());

        expectedRegionsById.forEach((regionId, expectedRegion) -> {
            final DeliveryCalcProtos.DeliveryOptionsGroupRegion actualRegion = actualRegionsById.get(regionId);
            assertRegion(expectedRegion, expectedGroupsById, actualRegion, actualGroupsById);
        });
    }

    private Map<Integer, DeliveryCalcProtos.DeliveryOptionsGroupRegion> getRegionsById(List<DeliveryCalcProtos.DeliveryOptionsGroupRegion> expectedRegions) {
        return expectedRegions.stream().collect(Collectors.toMap(DeliveryCalcProtos.DeliveryOptionsGroupRegion::getRegion, Function.identity()));
    }

    private void assertRegion(
            DeliveryCalcProtos.DeliveryOptionsGroupRegion expectedRegion, Map<Long, DeliveryCalcProtos.DeliveryOptionsGroup> expectedGroupsById,
            DeliveryCalcProtos.DeliveryOptionsGroupRegion actualRegion, Map<Long, DeliveryCalcProtos.DeliveryOptionsGroup> actualGroupsById) {
        assertEquals(expectedRegion.getOptionType(), actualRegion.getOptionType());
        final DeliveryCalcProtos.DeliveryOptionsGroup expectedGroup = expectedGroupsById.get(expectedRegion.getDeliveryOptGroupId());
        final DeliveryCalcProtos.DeliveryOptionsGroup actualGroup = actualGroupsById.get(actualRegion.getDeliveryOptGroupId());
        assertGroup(expectedGroup, actualGroup);
    }

    private void assertGroup(DeliveryCalcProtos.DeliveryOptionsGroup expectedGroup, DeliveryCalcProtos.DeliveryOptionsGroup actualGroup) {
        Assertions.assertThat(expectedGroup.getPaymentTypesList()).containsOnly(actualGroup.getPaymentTypesList().toArray(new ru.yandex.market.delivery.net.protobuf.DeliveryCalcProtos.PaymentType[0]));
        Assertions.assertThat(expectedGroup.getDeliveryOptionsList()).containsOnly(actualGroup.getDeliveryOptionsList().toArray(new ru.yandex.market.delivery.net.protobuf.DeliveryCalcProtos.DeliveryOption[0]));
    }

    private Map<Triple<Integer, Double, Double>, Long> extractBucketIds(DaasCourierMetaTariff tariff) {
        return tariff.getRules().stream()
                .collect(Collectors.toMap(
                        it -> Triple.of(it.getLocationFrom(), it.getMinCustomerWeight(), it.getMaxCustomerWeight()),
                        BaseMardoMetaRule::getBucketId)
                );
    }

    private DeliveryCalcProtos.FeedDeliveryOptionsResp readFeedDeliveryOptionsResp(String url) throws Exception {
        try (BufferedInputStream in = new BufferedInputStream(new URL(url).openStream())) {
            return PbSnUtils.readPbSnMessage("DCFA", DeliveryCalcProtos.FeedDeliveryOptionsResp.parser(), in);
        }
    }

    private <T> void addWeightRangeToResultMap(WeightRange weightRange,
                                               Map<RegionsPair, T> regionsPairMap,
                                               Map<WeightRange, Map<RegionsPair, T>> resultMap) {
        Map<RegionsPair, T> prevValue = resultMap.put(weightRange, regionsPairMap);
        if (prevValue != null) {
            // дополнительная проверка, что каждый весовой брейк обрабатывается и попадает в результат один раз
            Assertions.fail(weightRange + " already contains an entry");
        }
    }

    static Stream<Arguments> argumentsProvider() {
        return IntStream.range(0, TestingTariffs.TARIFF_IDS.size())
                .mapToObj(i -> Arguments.arguments(
                        i,
                        TestingTariffs.TARIFF_IDS.get(i)
                ));
    }
}
