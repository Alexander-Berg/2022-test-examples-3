package ru.yandex.market.common.report.parser.json;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.Test;

import ru.yandex.market.common.report.model.ActualDelivery;
import ru.yandex.market.common.report.model.ActualDeliveryOption;
import ru.yandex.market.common.report.model.ActualDeliveryOutlet;
import ru.yandex.market.common.report.model.ActualDeliveryResult;
import ru.yandex.market.common.report.model.AddressPreset;
import ru.yandex.market.common.report.model.DeliveryAvailable;
import ru.yandex.market.common.report.model.DeliveryCustomizer;
import ru.yandex.market.common.report.model.DeliveryOffer;
import ru.yandex.market.common.report.model.DeliveryService;
import ru.yandex.market.common.report.model.DeliveryTimeInterval;
import ru.yandex.market.common.report.model.DeliveryTypeDistribution;
import ru.yandex.market.common.report.model.ExtraCharge;
import ru.yandex.market.common.report.model.ExtraChargeParameters;
import ru.yandex.market.common.report.model.OfferProblem;
import ru.yandex.market.common.report.model.OutletDeliveryTimeInterval;
import ru.yandex.market.common.report.model.PickupOption;
import ru.yandex.market.common.report.model.PresetParcel;
import ru.yandex.market.common.report.model.SupplierProcessing;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ActualDeliveryJsonParserTest extends AbstractDeliveryJsonParserTest {

    @Test
    public void shouldParseCommonAndOfferProblems() throws IOException {
        ActualDeliveryJsonParser parser = new ActualDeliveryJsonParser();
        ActualDelivery actualDelivery = parser.parse(ActualDeliveryJsonParserTest.class.getResourceAsStream(
                "/files/actual_delivery_offer_problems.json"
        ));

        assertThat(actualDelivery.getOfferProblems(), hasSize(1));

        OfferProblem offerProblem = actualDelivery.getOfferProblems().get(0);
        assertThat(offerProblem.getWareId(), is("P78u-wFgtz4fkH0iWUOi9A"));
        assertThat(offerProblem.getProblems(), hasItems("NONEXISTENT_OFFER"));

        assertThat(actualDelivery.getCommonProblems(), hasSize(1));
        assertThat(actualDelivery.getCommonProblems(), containsInAnyOrder("NO_POST_OFFICE_FOR_POST_CODE"));
    }

    @Test
    public void shouldParseAddressPreset() throws IOException {
        ActualDeliveryJsonParser parser = new ActualDeliveryJsonParser();
        ActualDelivery result = parser.parse(ActualDeliveryJsonParserTest.class.getResourceAsStream
                ("/files/actual_delivery_address_presets.json"));
        assertNotNull(result);
        assertThat(result.getAddressPresets(), hasSize(1));

        AddressPreset addressPreset = result.getAddressPresets().get(0);
        assertThat(addressPreset.getId(), is("123"));
        assertThat(addressPreset.getRid(), is(17L));
        assertThat(addressPreset.getOutletId(), is(234L));
        assertThat(addressPreset.getType(), is("courier"));

        assertThat(addressPreset.getParcels(), hasSize(1));

        PresetParcel parcel = addressPreset.getParcels().get(0);
        assertThat(parcel.getParcelIndex(), is(0));
        assertThat(parcel.getDeliveryAvailable(), is(DeliveryAvailable.AVAILABLE));
        assertTrue(parcel.getTryingAvailable());

        assertNotNull(addressPreset.getCoord());
        assertThat(addressPreset.getCoord().getLat(), equalTo(new BigDecimal("1.1")));
        assertThat(addressPreset.getCoord().getLon(), equalTo(new BigDecimal("2.7")));
    }

    @Test
    public void shouldParseActualDeliveryPlace() throws IOException {
        ActualDeliveryJsonParser parser = new ActualDeliveryJsonParser();
        ActualDelivery result = parser.parse(ActualDeliveryJsonParserTest.class.getResourceAsStream
                ("/files/actual_delivery.json"));

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(1000), result.getFreeDeliveryRemainder());
        assertEquals(BigDecimal.valueOf(3000), result.getFreeDeliveryThreshold());
        assertEquals(BigDecimal.valueOf(2000), result.getOffersTotalPrice());
        assertEquals(BigDecimal.valueOf(800), result.getCheaperDeliveryThreshold());
        assertEquals(BigDecimal.valueOf(500), result.getCheaperDeliveryRemainder());
        assertEquals(Boolean.TRUE, result.getBetterWithPlus());
        assertThat(result.getResults(), not(nullValue()));
        assertThat(result.getResults(), hasSize(1));
        ActualDeliveryResult deliveryResult = getOnlyElement(result.getResults());
        assertThat(deliveryResult.getEntity(), equalTo("deliveryGroup"));
        assertThat(deliveryResult.getWeight(), equalTo(BigDecimal.valueOf(52.5)));
        assertThat(deliveryResult.getDimensions(), not(nullValue()));
        assertThat(deliveryResult.getDimensions(), hasSize(3));
        assertThat(deliveryResult.getDimensions(), hasSize(3));
        assertThat(
                deliveryResult.getDimensions(),
                containsInAnyOrder(BigDecimal.valueOf(10), BigDecimal.valueOf(27), BigDecimal.valueOf(118.7))
        );
        assertThat(
                deliveryResult.getDelivery(),
                hasSize(1)
        );
        assertThat(deliveryResult.getLargeSize(), equalTo(false));
        assertDeliveryOptionContent(getOnlyElement(deliveryResult.getDelivery()));
        assertThat(
                deliveryResult.getPickup(),
                hasSize(1)
        );
        assertPickupOptionContent(getOnlyElement(deliveryResult.getPickup()));
        assertThat(
                deliveryResult.getPost(),
                hasSize(1)
        );
        assertPostOptionContent(getOnlyElement(deliveryResult.getPost()));

        ActualDeliveryOutlet nearestOutlet = deliveryResult.getNearestOutlet();
        assertThat(nearestOutlet, notNullValue());
        assertThat(
                nearestOutlet.getId(),
                equalTo(1234567890L)
        );

        assertThat(nearestOutlet.getGpsCoord(), notNullValue());

        assertThat(nearestOutlet.getGpsCoord().getLatitude().orElse(null), equalTo("54.946002"));
        assertThat(nearestOutlet.getGpsCoord().getLongitude().orElse(null), equalTo("82.932997"));

        assertThat(
                deliveryResult.getPickup().get(0).getPostCodes(),
                hasSize(3)
        );
        assertThat(
                deliveryResult.getPickup().get(0).getPostCodes(),
                containsInAnyOrder(111111L, 222222L, 333333L)
        );

        assertThat(
                deliveryResult.getPost().get(0).getPostCodes(),
                hasSize(2)
        );
        assertThat(
                deliveryResult.getPost().get(0).getPostCodes(),
                containsInAnyOrder(444444L, 555555L)
        );

        assertThat(
                deliveryResult.getAvailableDeliveryMethods(),
                hasSize(3)
        );

        assertTrue(deliveryResult.getDelivery().get(0).isExpress());
        assertTrue(deliveryResult.getDelivery().get(0).isOnDemand());
        assertTrue(deliveryResult.getDelivery().get(0).isDeferredCourier());
        assertTrue(deliveryResult.getDelivery().get(0).isWideExpress());
        assertTrue(deliveryResult.getDelivery().get(0).isFastestExpress());

        assertDeliveryOffer(deliveryResult.getOffers());
        assertBucketsActive(deliveryResult.getBucketActive());
        assertBucketsAll(deliveryResult.getBucketAll());
        assertCarriersActive(deliveryResult.getCarrierActive());
        assertCarriersAll(deliveryResult.getCarrierAll());
        assertAvailableServices(deliveryResult.getAvailableServices());
        assertEquals("w:5;p:5;pc:RUR;tp:5;tpc:RUR;d:10x20x30;ct:1/2/3;wh:145;ffwh:145;",
                deliveryResult.getParcelInfo());
    }

    @Test
    public void shouldParseActualDeliveryDisclaimers() throws IOException {
        ActualDeliveryJsonParser parser = new ActualDeliveryJsonParser();
        ActualDelivery result = parser.parse(ActualDeliveryJsonParserTest.class.getResourceAsStream
                ("/files/actual_delivery.json"));

        ActualDeliveryResult deliveryResult = getOnlyElement(result.getResults());
        assertNotNull(result);
        assertThat(
                deliveryResult.getDelivery(),
                hasSize(1)
        );

        ActualDeliveryOption option = deliveryResult.getDelivery().get(0);
        assertThat(option.getDisclaimers(), hasSize(1));
        assertThat(option.getDisclaimers(), contains("tariffWarning"));
    }

    @Test
    public void shouldParseActualDeliveryOption() throws IOException {
        ActualDeliveryJsonParser parser = new ActualDeliveryJsonParser();
        ActualDelivery result = parser.parse(ActualDeliveryJsonParserTest.class.getResourceAsStream
                ("/files/actual_delivery.json"));

        ActualDeliveryResult deliveryResult = getOnlyElement(result.getResults());
        assertNotNull(result);
        assertThat(
                deliveryResult.getDelivery(),
                hasSize(1)
        );

        ActualDeliveryOption option = deliveryResult.getDelivery().get(0);

        assertEquals(Instant.parse("2019-11-27T17:30:00Z"), option.getSupplierShipmentDateTime());
        assertEquals(ZonedDateTime.parse("2019-11-27T12:30:00-05:00"), option.getShipmentDateTimeBySupplier());
        assertEquals(ZonedDateTime.parse("2019-11-27T18:00:00-05:00"), option.getReceptionDateTimeByWarehouse());

        List<SupplierProcessing> supplierProcessings = option.getSupplierProcessings();
        assertThat(supplierProcessings, hasSize(2));
        SupplierProcessing firstSupplier = new SupplierProcessing(
                44874,
                Instant.parse("2019-11-25T12:00:00Z"),
                Instant.parse("2019-11-25T15:00:00Z"),
                ZonedDateTime.parse("2019-11-25T09:00:00-06:00"),
                ZonedDateTime.parse("2019-11-26T09:00:00-05:00")
        );
        SupplierProcessing secondSupplier = new SupplierProcessing(
                44875,
                Instant.parse("2019-11-27T14:30:00Z"),
                Instant.parse("2019-11-27T17:30:00Z"),
                ZonedDateTime.parse("2019-11-27T12:30:00-05:00"),
                ZonedDateTime.parse("2019-11-27T18:00:00-05:00")
        );
        assertThat(supplierProcessings, containsInAnyOrder(firstSupplier, secondSupplier));

        assertThat(
                deliveryResult.getPickup(),
                hasSize(1)
        );
        assertThat(deliveryResult.getPickup().get(0).getSupplierProcessings(), empty());

        assertThat(
                deliveryResult.getPost(),
                hasSize(1)
        );
        assertThat(deliveryResult.getPost().get(0).getSupplierProcessings(), empty());
        assertTrue(deliveryResult.getPickup().get(0).getMarketPartner());
        assertTrue(deliveryResult.getPickup().get(0).getMarketPostTerm());
        assertFalse(deliveryResult.getPost().get(0).getMarketPartner());
        assertFalse(deliveryResult.getPost().get(0).getMarketPostTerm());
        assertFalse(deliveryResult.getDelivery().get(0).getMarketPartner());
        assertFalse(deliveryResult.getDelivery().get(0).getMarketPostTerm());
    }

    // Проверяем, что все, при работе со старым форматом времени мы "теряем" только информацию о таймзоне,
    // локальное время остается тем же
    @Test
    public void shouldParseActualDeliveryOptionWithOldDateFormat() throws IOException {
        ActualDeliveryJsonParser parser = new ActualDeliveryJsonParser();
        ActualDelivery result = parser.parse(ActualDeliveryJsonParserTest.class.getResourceAsStream
                ("/files/actual_delivery_with_dates_without_tz.json"));

        ActualDeliveryOption option = getOnlyElement(result.getResults()).getDelivery().get(0);

        assertEquals(Instant.parse("2019-11-27T12:30:00Z"), option.getSupplierShipmentDateTime());
        assertEquals(ZonedDateTime.parse("2019-11-27T12:30:00+00:00"), option.getShipmentDateTimeBySupplier());
        assertEquals(ZonedDateTime.parse("2019-11-27T18:00:00+00:00"), option.getReceptionDateTimeByWarehouse());

        List<SupplierProcessing> supplierProcessings = option.getSupplierProcessings();
        assertThat(supplierProcessings, hasSize(2));
        SupplierProcessing firstSupplier = new SupplierProcessing(
                null,
                Instant.parse("2019-11-25T06:00:00Z"),
                Instant.parse("2019-11-25T09:00:00Z"),
                ZonedDateTime.parse("2019-11-25T09:00:00+00:00"),
                ZonedDateTime.parse("2019-11-26T09:00:00+00:00")
        );
        SupplierProcessing secondSupplier = new SupplierProcessing(
                null,
                Instant.parse("2019-11-27T09:30:00Z"),
                Instant.parse("2019-11-27T12:30:00Z"),
                ZonedDateTime.parse("2019-11-27T12:30:00+00:00"),
                ZonedDateTime.parse("2019-11-27T18:00:00+00:00")
        );
        assertThat(supplierProcessings, containsInAnyOrder(firstSupplier, secondSupplier));
    }

    @Test
    public void shouldParseActualDeliveryOptionsWithSupplierPriceAndSupplierDiscount() throws IOException {
        ActualDeliveryJsonParser parser = new ActualDeliveryJsonParser();
        ActualDelivery result = parser.parse(ActualDeliveryJsonParserTest.class.getResourceAsStream
                ("/files/actual_delivery.json"));

        ActualDeliveryResult res = getOnlyElement(result.getResults());
        ActualDeliveryOption deliveryOption = getOnlyElement(res.getDelivery());
        PickupOption pickupOption = getOnlyElement(res.getPickup());
        PickupOption postOption = getOnlyElement(res.getPost());

        assertThat(deliveryOption.getSupplierPrice(), equalTo(BigDecimal.valueOf(100)));
        assertThat(deliveryOption.getSupplierDiscount(), equalTo(BigDecimal.valueOf(200)));
        assertThat(pickupOption.getSupplierPrice(), equalTo(BigDecimal.valueOf(101)));
        assertThat(pickupOption.getSupplierDiscount(), equalTo(BigDecimal.valueOf(201)));
        assertThat(postOption.getSupplierPrice(), equalTo(BigDecimal.valueOf(102)));
        assertThat(postOption.getSupplierDiscount(), equalTo(BigDecimal.valueOf(202)));
    }

    @Test
    public void shouldParseActualDeliveryOptionsWithoutSupplierPriceAndSupplierDiscount() throws IOException {
        ActualDeliveryJsonParser parser = new ActualDeliveryJsonParser();
        ActualDelivery result = parser.parse(ActualDeliveryJsonParserTest.class.getResourceAsStream
                ("/files/actual_delivery_without_supplier_price.json"));

        ActualDeliveryResult res = getOnlyElement(result.getResults());
        ActualDeliveryOption deliveryOption = getOnlyElement(res.getDelivery());
        PickupOption pickupOption = getOnlyElement(res.getPickup());
        PickupOption postOption = getOnlyElement(res.getPost());

        assertThat(deliveryOption.getSupplierPrice(), nullValue());
        assertThat(deliveryOption.getSupplierDiscount(), nullValue());
        assertThat(pickupOption.getSupplierPrice(), nullValue());
        assertThat(pickupOption.getSupplierDiscount(), nullValue());
        assertThat(postOption.getSupplierPrice(), nullValue());
        assertThat(postOption.getSupplierDiscount(), nullValue());
    }

    @Test
    public void shouldParseActualDeliveryWithoutCheaperPriceAndPlusOptions() throws IOException {
        ActualDeliveryJsonParser parser = new ActualDeliveryJsonParser();
        ActualDelivery result = parser.parse(ActualDeliveryJsonParserTest.class.getResourceAsStream
                ("/files/actual_delivery_without_cheaper_options.json"));

        assertThat(result.getCheaperDeliveryThreshold(), nullValue());
        assertThat(result.getCheaperDeliveryRemainder(), nullValue());
        assertThat(result.getBetterWithPlus(), nullValue());
    }

    @Test
    public void shouldParseActualDeliveryDeliveryOptionsWithLeaveAtTheDoor() throws IOException {
        ActualDeliveryJsonParser parser = new ActualDeliveryJsonParser();
        ActualDelivery result = parser.parse(ActualDeliveryJsonParserTest.class.getResourceAsStream
                ("/files/actual_delivery.json"));

        ActualDeliveryResult res = getOnlyElement(result.getResults());
        ActualDeliveryOption deliveryOption = getOnlyElement(res.getDelivery());

        assertTrue(deliveryOption.getLeaveAtTheDoor());
    }

    @Test
    public void shouldParseActualDeliveryWithoutIsTryingAvailable() throws IOException {
        ActualDeliveryJsonParser parser = new ActualDeliveryJsonParser();
        ActualDelivery result = parser.parse(ActualDeliveryJsonParserTest.class.getResourceAsStream
                ("/files/actual_delivery_without_isTryingAvailable.json"));

        ActualDeliveryResult res = getOnlyElement(result.getResults());
        ActualDeliveryOption deliveryOption = getOnlyElement(res.getDelivery());
        PickupOption pickupOption = getOnlyElement(res.getPickup());

        assertFalse(deliveryOption.getTryingAvailable());
        assertFalse(pickupOption.getTryingAvailable());
    }

    @Test
    public void shouldParseActualDeliveryWithIsTryingAvailable() throws IOException {
        ActualDeliveryJsonParser parser = new ActualDeliveryJsonParser();
        ActualDelivery result = parser.parse(ActualDeliveryJsonParserTest.class.getResourceAsStream
                ("/files/actual_delivery.json"));

        ActualDeliveryResult res = getOnlyElement(result.getResults());
        ActualDeliveryOption deliveryOption = getOnlyElement(res.getDelivery());
        PickupOption pickupOption = getOnlyElement(res.getPickup());
        PickupOption postOption = getOnlyElement(res.getPost());

        assertTrue(deliveryOption.getTryingAvailable());
        assertTrue(pickupOption.getTryingAvailable());
        assertFalse(postOption.getTryingAvailable());
    }

    @Test
    public void shouldParseActualDeliveryWithIsExternalLogistics() throws IOException {
        ActualDeliveryJsonParser parser = new ActualDeliveryJsonParser();
        ActualDelivery result = parser.parse(ActualDeliveryJsonParserTest.class.getResourceAsStream
                ("/files/actual_delivery.json"));

        ActualDeliveryResult res = getOnlyElement(result.getResults());
        ActualDeliveryOption deliveryOption = getOnlyElement(res.getDelivery());
        PickupOption pickupOption = getOnlyElement(res.getPickup());
        PickupOption postOption = getOnlyElement(res.getPost());

        assertTrue(deliveryOption.isExternalLogistics());
        assertTrue(pickupOption.isExternalLogistics());
        assertFalse(postOption.isExternalLogistics());
    }

    @Test
    public void shouldParseActualDeliveryWithoutIsExternalLogistics() throws IOException {
        ActualDeliveryJsonParser parser = new ActualDeliveryJsonParser();
        ActualDelivery result = parser.parse(ActualDeliveryJsonParserTest.class.getResourceAsStream
                ("/files/actual_delivery_without_isExternalLogistics.json"));

        ActualDeliveryResult res = getOnlyElement(result.getResults());
        ActualDeliveryOption deliveryOption = getOnlyElement(res.getDelivery());
        PickupOption pickupOption = getOnlyElement(res.getPickup());

        assertFalse(deliveryOption.isExternalLogistics());
        assertFalse(pickupOption.isExternalLogistics());
    }

    @Test
    public void shouldParseActualDeliveryDeliveryOptionsWithNegativeValues() throws IOException {
        ActualDeliveryJsonParser parser = new ActualDeliveryJsonParser();
        ActualDelivery result = parser.parse(ActualDeliveryJsonParserTest.class.getResourceAsStream
                ("/files/actual_delivery_leaveAtTheDoor.json"));

        ActualDeliveryResult res = getOnlyElement(result.getResults());
        List<ActualDeliveryOption> deliveryOptions = res.getDelivery();

        //leaveAtTheDoor = false
        assertFalse(deliveryOptions.get(0).getLeaveAtTheDoor());
        //leaveAtTheDoor isn't present
        assertFalse(deliveryOptions.get(1).getLeaveAtTheDoor());
    }

    @Test
    public void shouldParseActualDeliveryOptionsWithOutletTimeIntervals() throws IOException {
        ActualDeliveryJsonParser parser = new ActualDeliveryJsonParser();
        ActualDelivery result = parser.parse(ActualDeliveryJsonParserTest.class.getResourceAsStream
                ("/files/actual_delivery_outletTimeIntervals.json"));

        ActualDeliveryResult res = getOnlyElement(result.getResults());
        PickupOption pickupOption = getOnlyElement(res.getPickup());

        assertOutletTimeIntervals(
                pickupOption.getOutletTimeIntervals(),
                new OutletDeliveryTimeInterval(123L,
                        LocalTime.of(12, 0), LocalTime.of(18, 30)),
                new OutletDeliveryTimeInterval(456L,
                        LocalTime.of(19, 0), LocalTime.of(21, 45))
        );
    }

    @Test
    public void shouldParseActualDeliveryDeliveryOptionsWithDeliveryCustomizers() throws IOException {
        ActualDeliveryJsonParser parser = new ActualDeliveryJsonParser();
        ActualDelivery result = parser.parse(ActualDeliveryJsonParserTest.class.getResourceAsStream("/files" +
                "/actual_delivery_with_customizers.json"));

        ActualDeliveryResult res = getOnlyElement(result.getResults());
        List<ActualDeliveryOption> deliveryOptions = res.getDelivery();

        assertEquals(4, deliveryOptions.size());
        //Customizers are present
        List<DeliveryCustomizer> customizers = deliveryOptions.get(0).getCustomizers();
        assertEquals(3, customizers.size());
        DeliveryCustomizer leaveAtTheDoorCustomizer = customizers.get(0);
        DeliveryCustomizer noCallCustomizer = customizers.get(1);
        DeliveryCustomizer stringCustomizer = customizers.get(2);
        assertEquals(new DeliveryCustomizer("leave_at_the_door", "Оставить у двери", "boolean"),
                leaveAtTheDoorCustomizer);
        assertEquals(new DeliveryCustomizer("no_call", "Не звонить", "boolean"), noCallCustomizer);
        assertEquals(new DeliveryCustomizer("some_string_customizer", "Текстовый кастомайзер", "string"),
                stringCustomizer);

        //customizers are null
        assertNotNull(deliveryOptions.get(1).getCustomizers());
        assertTrue(deliveryOptions.get(1).getCustomizers().isEmpty());
        //customizers are not present
        assertNotNull(deliveryOptions.get(2).getCustomizers());
        assertTrue(deliveryOptions.get(2).getCustomizers().isEmpty());
        //customizers are present but empty
        assertNotNull(deliveryOptions.get(3).getCustomizers());
        assertTrue(deliveryOptions.get(3).getCustomizers().isEmpty());
    }

    @Test
    public void tariffStatsParserTest() throws IOException {
        testTariffStatsParser(new ActualDeliveryJsonParser());
    }

    @Test
    public void testIsEstimated() throws IOException {
        ActualDeliveryJsonParser parser = new ActualDeliveryJsonParser();

        try (InputStream resourceAsStream = OfferInfoMarketReportJsonParserTest.class.getResourceAsStream("/files" +
                "/actual_delivery_with_customizers.json")) {
            ActualDelivery actualDelivery = parser.parse(resourceAsStream);

            List<ActualDeliveryOption> localDelivery = actualDelivery.getResults().get(0).getDelivery();
            List<PickupOption> pickupOptions = actualDelivery.getResults().get(0).getPickup();
            List<PickupOption> postOptions = actualDelivery.getResults().get(0).getPickup();

            assertTrue(localDelivery.get(0).getEstimated());
            assertFalse(localDelivery.get(1).getEstimated());
            assertNull(localDelivery.get(2).getEstimated());

            assertTrue(pickupOptions.get(0).getEstimated());

            assertTrue(postOptions.get(0).getEstimated());
        }
    }

    @Test
    public void shouldParseExtraCharge() throws IOException {
        ActualDeliveryJsonParser parser = new ActualDeliveryJsonParser();
        ActualDelivery result = parser.parse(ActualDeliveryJsonParserTest.class.getResourceAsStream
                ("/files/actual_delivery_extraCharge.json"));
        assertNotNull(result);
        ExtraChargeParameters extraChargeParameters = result.getExtraChargeParameters();
        assertNotNull(extraChargeParameters);
        assertEquals(new BigDecimal("1499.5"), extraChargeParameters.getMaxCharge());
        assertEquals(new BigDecimal("199"), extraChargeParameters.getMinCharge());
        assertEquals(new BigDecimal("50.0"), extraChargeParameters.getChargeQuant());
        assertEquals(new BigDecimal("1.2"), extraChargeParameters.getVatMultiplier());
        assertEquals(new BigDecimal("0.005"), extraChargeParameters.getMinChargeOfGmv());
        assertEquals(Integer.valueOf(1), extraChargeParameters.getVersion());

        assertEquals(1, result.getResults().size());
        List<ActualDeliveryOption> deliveryOptions = result.getResults().get(0).getDelivery();
        assertEquals(1, deliveryOptions.size());
        ExtraCharge extraChargeDeliveryOpt = deliveryOptions.get(0).getExtraCharge();
        assertNotNull(extraChargeDeliveryOpt);
        assertEquals(new BigDecimal("99"), extraChargeDeliveryOpt.getValue());
        assertEquals(new BigDecimal("-33"), extraChargeDeliveryOpt.getUnitEconomyValue());
        assertEquals("E_SOME_REASON_1", extraChargeDeliveryOpt.getReasonCodes().get(0));

        List<PickupOption> pickupOptions = result.getResults().get(0).getPickup();
        assertEquals(1, pickupOptions.size());
        ExtraCharge extraChargePickupOpt = pickupOptions.get(0).getExtraCharge();
        assertEquals(new BigDecimal("199"), extraChargePickupOpt.getValue());
        assertEquals(new BigDecimal("41"), extraChargePickupOpt.getUnitEconomyValue());
        assertEquals("E_SOME_REASON_2", extraChargePickupOpt.getReasonCodes().get(0));
    }

    private void assertPickupOptionContent(PickupOption pickupOption) {
        assertEquals(4, (int) pickupOption.getDayFrom());
        assertEquals(4, (int) pickupOption.getDayTo());
        assertEquals(3, (int) pickupOption.getShipmentDay());
        assertEquals(LocalDate.parse("2019-10-31"), pickupOption.getShipmentDate());
        assertEquals(Instant.parse("2019-10-28T14:58:00Z"), pickupOption.getSupplierShipmentDateTime());
        assertEquals(107, (long) pickupOption.getDeliveryServiceId());
        assertThat(
                pickupOption.getOutletIds(),
                containsInAnyOrder(123L, 456L)
        );
        assertThat(
                pickupOption.getOutletTimeIntervals(),
                empty()
        );
        assertThat(
                pickupOption.getPaymentMethods(),
                containsInAnyOrder("YANDEX", "CASH_ON_DELIVERY")
        );
        assertEquals(24L * 3600, pickupOption.getPackagingTime().getSeconds());
        assertThat(pickupOption.getTariffId(), equalTo(2234562L));
    }

    private static void assertDeliveryOptionContent(ActualDeliveryOption deliveryOption) {
        assertEquals("market_delivery", deliveryOption.getPartnerType());
        assertEquals(BigDecimal.ZERO, deliveryOption.getCost());
        assertEquals(BigDecimal.valueOf(80), deliveryOption.getPriceWithoutVat());
        assertEquals(7, (int) deliveryOption.getDayFrom());
        assertEquals(7, (int) deliveryOption.getDayTo());
        assertEquals(50, (long) deliveryOption.getDeliveryServiceId());
        assertThat(
                deliveryOption.getPaymentMethods(),
                containsInAnyOrder("YANDEX", "CASH_ON_DELIVERY")
        );
        assertTimeIntervals(
                deliveryOption,
                new DeliveryTimeInterval(LocalTime.of(12, 00), LocalTime.of(18, 30), true),
                new DeliveryTimeInterval(LocalTime.of(19, 00), LocalTime.of(21, 45), false)
        );
        assertEquals(25L * 3600 + 30 * 60, deliveryOption.getPackagingTime().getSeconds());
        assertThat(deliveryOption.getTariffId(), equalTo(2234562L));
    }

    private void assertAvailableServices(List<DeliveryService> availableServices) {
        assertThat(availableServices, hasSize(2));
        assertEquals(106, (long) availableServices.get(0).getServiceId());
        assertEquals("Boxberry", availableServices.get(0).getServiceName());
        assertEquals(107, (long) availableServices.get(1).getServiceId());
        assertEquals("PickPoint", availableServices.get(1).getServiceName());
    }

    private void assertDeliveryOffer(List<DeliveryOffer> deliveryOffers) {
        assertThat(deliveryOffers, hasSize(1));
        assertEquals(52L, (long) deliveryOffers.get(0).getSellerPrice());
        assertEquals("RUB", deliveryOffers.get(0).getCurrency().getAliases()[0]);
        assertEquals(15166435, (long) deliveryOffers.get(0).getMarketSku());
        assertEquals(73L, (long) deliveryOffers.get(0).getFulfillmentWarehouseId());
    }

    private void assertBucketsAll(DeliveryTypeDistribution deliveryTypeDistribution) {
        assertThat(deliveryTypeDistribution.getCourier(), hasSize(1));
        assertThat(deliveryTypeDistribution.getPickup(), hasSize(1));
        assertThat(deliveryTypeDistribution.getPost(), hasSize(2));
        assertEquals(111, (long) deliveryTypeDistribution.getCourier().get(0));
        assertEquals(112, (long) deliveryTypeDistribution.getPickup().get(0));
        assertEquals(113, (long) deliveryTypeDistribution.getPost().get(0));
        assertEquals(114, (long) deliveryTypeDistribution.getPost().get(1));
    }

    private void assertBucketsActive(DeliveryTypeDistribution deliveryTypeDistribution) {
        assertThat(deliveryTypeDistribution.getCourier(), hasSize(0));
        assertThat(deliveryTypeDistribution.getPickup(), hasSize(1));
        assertThat(deliveryTypeDistribution.getPost(), hasSize(1));
        assertEquals(112, (long) deliveryTypeDistribution.getPickup().get(0));
        assertEquals(113, (long) deliveryTypeDistribution.getPost().get(0));
    }

    private void assertCarriersActive(DeliveryTypeDistribution deliveryTypeDistribution) {
        assertThat(deliveryTypeDistribution.getCourier(), hasSize(0));
        assertThat(deliveryTypeDistribution.getPickup(), hasSize(1));
        assertThat(deliveryTypeDistribution.getPost(), hasSize(0));
        assertEquals(1112, (long) deliveryTypeDistribution.getPickup().get(0));
    }

    private void assertCarriersAll(DeliveryTypeDistribution deliveryTypeDistribution) {
        assertThat(deliveryTypeDistribution.getCourier(), hasSize(0));
        assertThat(deliveryTypeDistribution.getPickup(), hasSize(1));
        assertThat(deliveryTypeDistribution.getPost(), hasSize(0));
        assertEquals(1112, (long) deliveryTypeDistribution.getPickup().get(0));
    }

    public static void assertTimeIntervals(ActualDeliveryOption deliveryOption, DeliveryTimeInterval... intervals) {
        assertThat(deliveryOption.getTimeIntervals(), hasSize(intervals.length));
        for (int index = 0; index < intervals.length; index++) {
            assertThat(deliveryOption.getTimeIntervals().get(index).getTo(), is(intervals[index].getTo()));
            assertThat(deliveryOption.getTimeIntervals().get(index).getFrom(), is(intervals[index].getFrom()));
            assertThat(deliveryOption.getTimeIntervals().get(index).isDefault(), is(intervals[index].isDefault()));
        }
    }

    private void assertPostOptionContent(PickupOption deliveryOption) {
        assertEquals("market_delivery", deliveryOption.getPartnerType());
        assertEquals(BigDecimal.ONE, deliveryOption.getCost());
        assertEquals(8, (int) deliveryOption.getDayFrom());
        assertEquals(9, (int) deliveryOption.getDayTo());
        assertEquals(Instant.parse("2019-10-28T14:59:00Z"), deliveryOption.getSupplierShipmentDateTime());
        assertEquals(123456, (long) deliveryOption.getDeliveryServiceId());
        assertThat(
                deliveryOption.getOutletIds(),
                contains(123L)
        );
        assertThat(
                deliveryOption.getPaymentMethods(),
                containsInAnyOrder("YANDEX")
        );

        assertThat(deliveryOption.getOutlet(), notNullValue());
        assertThat(deliveryOption.getOutlet().getId(), is(24529553L));
        assertThat(deliveryOption.getOutlet().getType(), is("post"));
        assertThat(deliveryOption.getOutlet().getPostCode(), is("117216"));
        assertThat(deliveryOption.getOutlet().getPurpose(), hasItems("post"));
        assertThat(deliveryOption.getPackagingTime().getSeconds(), is(3600L + 45 * 60));
        assertThat(deliveryOption.getTariffId(), equalTo(2234562L));
    }

    private static void assertOutletTimeIntervals(List<OutletDeliveryTimeInterval> optionIntervals,
                                                  OutletDeliveryTimeInterval... expectedIntervals) {
        assertThat(optionIntervals, hasSize(expectedIntervals.length));
        for (int index = 0; index < expectedIntervals.length; index++) {
            assertThat(optionIntervals.get(index).getOutletId(), is(expectedIntervals[index].getOutletId()));
            assertThat(optionIntervals.get(index).getTo(), is(expectedIntervals[index].getTo()));
            assertThat(optionIntervals.get(index).getFrom(), is(expectedIntervals[index].getFrom()));
        }
    }
}
