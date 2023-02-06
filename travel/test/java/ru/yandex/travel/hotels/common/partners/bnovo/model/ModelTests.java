package ru.yandex.travel.hotels.common.partners.bnovo.model;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.Test;

import ru.yandex.travel.hotels.common.partners.bnovo.DefaultBNovoClient;

import static org.assertj.core.api.Assertions.assertThat;

public class ModelTests {
    private ObjectMapper objectMapper = DefaultBNovoClient.createObjectMapper();

    private String load(String name) throws IOException {
        return Resources.toString(Resources.getResource(String.format("bnovoResponses/%s.json", name)),
                Charset.defaultCharset());
    }

    @Test
    public void testRoomTypes() throws IOException {
        var response = this.load("roomTypeList");
        RoomTypeList roomTypeList = objectMapper.readerFor(RoomTypeList.class).readValue(response);
        assertThat(roomTypeList.getRooms()).hasSize(17);
        assertThat(roomTypeList.getRooms().get(0).getName()).isEqualTo("Двухместный номер с 2 отдельными кроватями " +
                "2Se");
        assertThat(roomTypeList.getRooms().get(0).getDefaultName()).isEqualTo("Двухместный номер с 2 отдельными " +
                "кроватями");
        assertThat(roomTypeList.getRooms().get(0).getAccommodationType()).isEqualTo(AccommodationType.HOTEL_ROOM);
    }

    @Test
    public void testRatePlan() throws IOException {
        var response = load("ratePlan");
        RatePlan ratePlan = objectMapper.readerFor(RatePlan.class).readValue(response);
        assertThat(ratePlan.isEnabled()).isTrue();
        assertThat(ratePlan.isEnabledOTA()).isTrue();
        assertThat(ratePlan.isDefault()).isTrue();
        assertThat(ratePlan.isForPromoCode()).isFalse();
        assertThat(ratePlan.getNutrition()[0]).isTrue();
        assertThat(ratePlan.getNutrition()[1]).isTrue();
        assertThat(ratePlan.getNutrition()[2]).isTrue();
        assertThat(ratePlan.getCancellationFineType()).isEqualTo(CancellationFineType.PERCENTAGE);
    }

    @Test
    public void testPriceList() throws IOException {
        var response = load("priceLosResponse");
        HotelStayMap prices = objectMapper.readerFor(HotelStayMap.class).readValue(response);
        assertThat(prices.keySet()).hasSize(2);
        assertThat(prices.get("69").get(LocalDate.of(2020, 3, 21))).isNotNull();
        assertThat(prices.get("69").get(LocalDate.of(2020, 3, 21)).getRates().get(0).getPrice().intValue()).isEqualTo(-1);
        assertThat(prices.get("69").get(LocalDate.of(2020, 3, 21)).getRates().get(0).getPricesByDates()).isEmpty();
        assertThat(prices.get("69").get(
                LocalDate.of(2020, 3, 21)).getRates().get(17).getPrice().setScale(2, RoundingMode.HALF_UP))
                .isEqualByComparingTo("3604.78");
        assertThat(prices.get("69").get(LocalDate.of(2020, 3, 21)).getRates().get(17).getPricesByDates()).isNotEmpty();
    }

    @Test
    public void testEmptyPriceList() throws IOException {
        var response = load("priceLosEmptyResponse");
        HotelStayMap prices = objectMapper.readerFor(HotelStayMap.class).readValue(response);
        assertThat(prices).hasSize(1);
        assertThat(prices.get("3056")).isEmpty();
    }

    @Test
    public void testHotelList() throws IOException {
        var response = load("hotelList");
        HotelList hotelList = objectMapper.readerFor(HotelList.class).readValue(response);
        assertThat(hotelList.getAccounts().get(0).getCheckinInstantForDate(LocalDate.now())).isNotNull();
        assertThat(hotelList.getAccounts().get(0).getGeoData().getLatitude()).isEqualTo(59.93166);
        assertThat(hotelList.getAccounts().get(0).getGeoData().getLongitude()).isEqualTo(30.353736);
    }

    @Test
    public void testBooking() throws IOException {
        var response = load("booking");
        Booking booking = objectMapper.readerFor(Booking.class).readValue(response);
        assertThat(booking.getArrival()).isEqualTo("2020-03-19T14:00:00");
        assertThat(booking.getDeparture()).isEqualTo("2020-03-20T12:00:00");
        assertThat(booking.getAmount()).isEqualByComparingTo("6000.40");
        assertThat(booking.getPrices().get(LocalDate.of(2020, 3, 19))).isEqualByComparingTo("6000.40");
        assertThat(booking.getOnlineWarrantyDeadlineDate()).isEqualTo("2020-03-18T09:54:02Z");
        assertThat(booking.getStatusId()).isEqualTo(BookingStatusId.CONFIRMED);
        assertThat(booking.isPaid()).isTrue();
        assertThat(booking.isBookingGuaranteeAutoBookingCancel()).isTrue();
        assertThat(booking.getStatusId()).isEqualTo(BookingStatusId.CONFIRMED);
        assertThat(booking.isAutomaticallyCancelled()).isFalse();

        response = load("bookingAutoCancelled");
        booking = objectMapper.readerFor(Booking.class).readValue(response);
        assertThat(booking.getStatusId()).isEqualTo(BookingStatusId.CANCELLED);
        assertThat(booking.isAutomaticallyCancelled()).isTrue();


        response = load("bookingManuallyCancelled");
        booking = objectMapper.readerFor(Booking.class).readValue(response);
        assertThat(booking.getStatusId()).isEqualTo(BookingStatusId.CANCELLED);
        assertThat(booking.isAutomaticallyCancelled()).isFalse();
    }

    @Test
    public void testConfirmationResponse() throws IOException {
        var response = load("confirmationResponse");
        ConfirmationResponse confirmation = objectMapper.readerFor(ConfirmationResponse.class).readValue(response);
        assertThat(confirmation).isNotNull();
        assertThat(confirmation.getConfirmedBookings()).isNotEmpty();
        assertThat(confirmation.getAlreadyCancelledBookings()).isEmpty();
    }

    @Test
    public void testServiceDeserializationFromMap() throws IOException {
        var response = load("servicesAsMapResponse");
        AdditionalServicesResponse resp = objectMapper.readerFor(AdditionalServicesResponse.class).readValue(response);
        assertThat(resp.getAdditionalServices()).isNotEmpty();
        assertThat(resp.getAdditionalServices()).hasSize(21);
        assertThat(resp.getAdditionalServices().get(0).getPackageAdditionalServicesIds()).hasSize(0);
        assertThat(resp.getAdditionalServices().stream().filter(s -> s.getId() == 32329).findFirst().get().getPackageAdditionalServicesIds()).hasSize(3);
    }

    @Test
    public void testServiceDeserializationFromList() throws IOException {
        var response = load("servicesAsListResponse");
        AdditionalServicesResponse resp = objectMapper.readerFor(AdditionalServicesResponse.class).readValue(response);
        assertThat(resp.getAdditionalServices()).isNotEmpty();
        assertThat(resp.getAdditionalServices()).hasSize(1);
        assertThat(resp.getAdditionalServices().get(0).getPackageAdditionalServicesIds()).hasSize(0);
    }

    @Test
    public void testRoomTypesWithChildrenAges() throws IOException {
        var response = load("roomTypeListWithChildren");
        RoomTypeList roomTypeList = objectMapper.readerFor(RoomTypeList.class).readValue(response);
        assertThat(roomTypeList.getRooms()).hasSize(5);
        assertThat(roomTypeList.getRooms().get(0).getExtraArray().getChildrenAges()).isNullOrEmpty();
        assertThat(roomTypeList.getRooms().get(1).getExtraArray().getChildrenAges()).isEqualTo(Map.of("268", 1));

    }

    @Test
    public void testHotelInfoWithAgeGroups() throws IOException {
        var response = load("hotelInfoWithChildren");
        HotelInfoResponse r = objectMapper.readerFor(HotelInfoResponse.class).readValue(response);
        assertThat(r.getAccount().getChildrenAges()).hasSize(2);
        assertThat(r.getAccount().getChildrenAges()).isEqualTo(Map.of(
                "268",
                new AgeGroup.AgeGroupBuilder().id(268).minAge(0).maxAge(5).build(),
                "269",
                new AgeGroup.AgeGroupBuilder().id(269).minAge(6).maxAge(17).build()
        ));
    }

    @Test
    public void testHotelInfoWithoutAgeGroups() throws IOException {
        var response = load("hotelInfoWithoutChildren");
        HotelInfoResponse r = objectMapper.readerFor(HotelInfoResponse.class).readValue(response);
        assertThat(r.getAccount().getChildrenAges()).isEmpty();
    }

    @Test
    public void testBookingJsonWithChildrenData() throws JsonProcessingException {
        var bookingJson = BookingJson.build("YA-123",
                42, Stay.builder()
                        .rate(Offer.builder()
                                .adults(2)
                                .planId(420)
                                .roomtypeId(4200)
                                .price(BigDecimal.valueOf(100))
                                .pricesByDate(LocalDate.of(2022, 6, 24), BigDecimal.valueOf(100))
                                .build())
                        .checkin(LocalDate.of(2022, 6, 24))
                        .currency("RUB")
                        .nights(1)
                        .timestamp(Instant.now().atZone(ZoneId.systemDefault())).build(),
                "Foo", "Bar", "+71234567890", "test@example.com", 1, List.of(5));
        var res = DefaultBNovoClient.createObjectMapper().writeValueAsString(bookingJson);
        assertThat(res).isNotEmpty();
    }
}
