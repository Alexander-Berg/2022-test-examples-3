package ru.yandex.market.deliverycalculator.searchengine;

import java.util.Arrays;
import java.util.Collections;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.net.protobuf.DeliveryCalcProtos;
import ru.yandex.market.deliverycalculator.searchengine.controller.log.JsonRecordBuilder;

class JsonRecordBuilderTest {

    @Test
    void numbersConversionTest() {
        JsonRecordBuilder recordBuilder = JsonRecordBuilder.create()
                .add("Integer test", 135)
                .add("Integer test 2", (Integer) null)
                .add("Long test", 2478859L);

        Assertions.assertEquals("{\"Integer test\":135,\"Long test\":2478859}", recordBuilder.build());
    }

    @Test
    void collectionsConversionTest() {
        JsonRecordBuilder recordBuilder = JsonRecordBuilder.create()
                .add("Empty collection test", Collections.emptySet())
                .add("Collection test", Sets.newTreeSet(Arrays.asList(122L, 1256L, 4545L)))
                .add("Empty list test", Collections.<String>emptyList())
                .add("List test", Arrays.asList("one", "two", "three"))
                .add("Bucket info collection test", Arrays.asList(
                        DeliveryCalcProtos.BucketInfo.newBuilder()
                                .setBucketId(111L)
                                .setProgram(DeliveryCalcProtos.ProgramType.MARKET_DELIVERY_WHITE_PROGRAM)
                                .setIsNew(true)
                                .addAllCostModifiersIds(Arrays.asList(123L, 124L, 125L))
                                .addTimeModifiersIds(134L)
                                .build(),
                        DeliveryCalcProtos.BucketInfo.newBuilder()
                                .setBucketId(222L)
                                .setProgram(DeliveryCalcProtos.ProgramType.DAAS)
                                .build()
                ));

        Assertions.assertEquals(
                "{" +
                        "\"Collection test\":[122,1256,4545]," +
                        "\"List test\":[\"one\",\"two\",\"three\"]," +
                        "\"Bucket info collection test\":[" +
                        "{\"bucketId\":\"111\",\"costModifiersIds\":[\"123\",\"124\",\"125\"],\"timeModifiersIds\":[\"134\"],\"isNew\":true,\"program\":6}," +
                        "{\"bucketId\":\"222\",\"program\":8}" +
                        "]}",
                recordBuilder.build())
        ;
    }

    @Test
    void stringConversionTest() {
        JsonRecordBuilder recordBuilder = JsonRecordBuilder.create()
                .add("String test", "hi\nby")
                .add("String test 2", (String) null);

        Assertions.assertEquals("{\"String test\":\"hi\\nby\"}", recordBuilder.build());
    }

    @Test
    void offerConversionTest() {
        JsonRecordBuilder recordBuilder = JsonRecordBuilder.create()
                .add("Offer test", createOffer());

        Assertions.assertEquals("{\"Offer test\":{\"offerId\":\"456\",\"categories\":[\"20\",\"30\"]," +
                "\"weight\":1.77,\"width\":11.0,\"height\":44.0,\"length\":102.0,\"programType\":[0]," +
                "\"cargoTypes\":[567,773,112],\"priceMap\":[{\"currency\":\"USD\",\"value\":300.0}]}}", recordBuilder.build());
    }

    @Test
    void offerDefaultValuesTest() {
        DeliveryCalcProtos.Offer offer = DeliveryCalcProtos.Offer.newBuilder()
                .setOfferId("456")
                .clearStore()
                .clearPickup()
                .build();

        JsonRecordBuilder recordBuilder = JsonRecordBuilder.create()
                .add("Offer test", offer);

        Assertions.assertEquals("{\"Offer test\":{\"offerId\":\"456\"}}", recordBuilder.build());
        Assertions.assertTrue(offer.getPickup());
        Assertions.assertFalse(offer.getStore());
    }

    @Test
    void commonPrefixTest() {
        JsonRecordBuilder recordBuilder = JsonRecordBuilder.create()
                .add("Common field #1", "Value 1")
                .add("Common field #2", "Value 2");

        recordBuilder.markEndOfCommonFeedOffersPrefix();

        recordBuilder.add("First entity", DeliveryCalcProtos.Offer.newBuilder().setOfferId("111").build());
        Assertions.assertEquals("{\"Common field #1\":\"Value 1\",\"Common field #2\":\"Value 2\",\"First entity\":{\"offerId\":\"111\"}}", recordBuilder.build());

        recordBuilder.add("Second entity", DeliveryCalcProtos.Offer.newBuilder().setOfferId("222").build());
        Assertions.assertEquals("{\"Common field #1\":\"Value 1\",\"Common field #2\":\"Value 2\",\"Second entity\":{\"offerId\":\"222\"}}", recordBuilder.build());
    }

    private DeliveryCalcProtos.Offer createOffer() {
        return DeliveryCalcProtos.Offer.newBuilder()
            .setOfferId("456")
            .addProgramType(DeliveryCalcProtos.ProgramType.REGULAR_PROGRAM)
            .setWeight(1.77)
            .setLength(102)
            .setHeight(44)
            .setWidth(11)
            .addPriceMap(DeliveryCalcProtos.OfferPrice.newBuilder().setCurrency("USD").setValue(300))
            .addCargoTypes(567)
            .addCargoTypes(773)
            .addCargoTypes(112)
            .addCategories(20)
            .addCategories(30)
            .build();
    }
}
