package ru.yandex.travel.hotels.searcher.services.cache.travelline.availability;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.javamoney.moneta.Money;

import ru.yandex.bolts.internal.NotImplementedException;
import ru.yandex.travel.hotels.common.partners.travelline.TravellineClient;
import ru.yandex.travel.hotels.common.partners.travelline.model.CancelReservationResponse;
import ru.yandex.travel.hotels.common.partners.travelline.model.ConfirmReservationResponse;
import ru.yandex.travel.hotels.common.partners.travelline.model.Hotel;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelChainDetailsResponse;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelDetailsResponse;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelInfo;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelInventory;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelInventoryResponse;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelListItem;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelOfferAvailability;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelRef;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelReservationRequest;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelReservationResponse;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelStatusChangedResponse;
import ru.yandex.travel.hotels.common.partners.travelline.model.ListHotelsResponse;
import ru.yandex.travel.hotels.common.partners.travelline.model.MakeExtraPaymentResponse;
import ru.yandex.travel.hotels.common.partners.travelline.model.ReadReservationResponse;
import ru.yandex.travel.hotels.common.partners.travelline.model.RoomStay;
import ru.yandex.travel.hotels.common.partners.travelline.model.VerifyReservationRequest;
import ru.yandex.travel.hotels.common.partners.travelline.model.VerifyReservationResponse;

public class StubbedTravellineClient implements TravellineClient {
    private UpdatesMap updatesMap;

    public StubbedTravellineClient(UpdatesMap updatesMap) {
        this.updatesMap = updatesMap;
    }

    @Override
    public CompletableFuture<HotelInfo> getHotelInfo(String hotelCode, String requestId) {
        HotelInfo hotelInfo = new HotelInfo();
        hotelInfo.setHotel(Hotel.builder().code(hotelCode).build());
        return CompletableFuture.completedFuture(hotelInfo);
    }

    @Override
    public CompletableFuture<HotelOfferAvailability> findOfferAvailability(String hotelCode, LocalDate checkinDate,
                                                                           LocalDate checkoutDate, String requestId) {
        var availability = new HotelOfferAvailability();
        availability.setRoomStays(List.of(RoomStay.builder().hotelRef(HotelRef.builder().code(hotelCode).build()).build()));
        return CompletableFuture.completedFuture(availability);
    }

    @Override
    public CompletableFuture<VerifyReservationResponse> verifyReservation(VerifyReservationRequest request) {
        throw new NotImplementedException();
    }

    @Override
    public CompletableFuture<HotelReservationResponse> createReservation(HotelReservationRequest request) {
        throw new NotImplementedException();
    }

    @Override
    public CompletableFuture<ConfirmReservationResponse> confirmReservation(String yandexNumber,
                                                                            String transactionNumber, Money amount) {
        throw new NotImplementedException();
    }

    @Override
    public CompletableFuture<MakeExtraPaymentResponse> makeExtraPayment(String yandexNumber, String transactionNumber,
                                                                        Money amount) {
        throw new NotImplementedException();
    }

    @Override
    public CompletableFuture<CancelReservationResponse> cancelReservation(String yandexNumber) {
        throw new NotImplementedException();
    }

    @Override
    public CompletableFuture<ReadReservationResponse> readReservation(String yandexNumber) {
        throw new NotImplementedException();
    }

    @Override
    public CompletableFuture<HotelInventoryResponse> getHotelInventory(String hotelCode) {
        var res = new HotelInventoryResponse(new ArrayList<>());
        for (var entry : updatesMap.getInventory(hotelCode)) {
            res.getHotelInventories().add(HotelInventory.builder()
                    .date(entry.getKey())
                    .version(entry.getValue())
                    .build());
        }
        return CompletableFuture.completedFuture(res);
    }

    @Override
    public CompletableFuture<ListHotelsResponse> listHotels() {
        var res = new ListHotelsResponse(new ArrayList<>());
        for (String code : updatesMap.getAllCodes()) {
            Long version = updatesMap.getVersion(code);
            if (version != null) {
                res.getHotels().add(HotelListItem.builder()
                        .code(code)
                        .inventoryVersion(version)
                        .build());
            }
        }
        return CompletableFuture.completedFuture(res);
    }

    @Override
    public CompletableFuture<HotelStatusChangedResponse> notifyHotelStatusChanged(String hotelCode) {
        throw new NotImplementedException();
    }

    @Override
    public CompletableFuture<HotelDetailsResponse> getHotelDetails(String hotelCode) {
        throw new NotImplementedException();
    }

    @Override
    public CompletableFuture<HotelChainDetailsResponse> getHotelChainDetails(String inn) {
        throw new NotImplementedException();
    }
}
