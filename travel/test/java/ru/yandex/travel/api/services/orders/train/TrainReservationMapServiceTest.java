package ru.yandex.travel.api.services.orders.train;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.api.services.orders.TrainReservationMapService;
import ru.yandex.travel.train.model.TrainReservation;
import ru.yandex.travel.trains.proto.EPassengerCategory;

import static org.assertj.core.api.Assertions.assertThat;

public class TrainReservationMapServiceTest {
    private TrainReservationMapService mapService;

    @Before
    public void setUp() {
        mapService = new TrainReservationMapService(
                MockTrainDictionaryHelper.countryDataProvider(),
                MockTrainDictionaryHelper.trainReadableTimezoneDataProvider(),
                MockTrainDictionaryHelper.trainSettlementDataProvider(),
                MockTrainDictionaryHelper.trainStationDataProvider(),
                MockTrainDictionaryHelper.trainStationCodeDataProvider(),
                MockTrainDictionaryHelper.trainTimeZoneDataProvider(),
                MockTrainDictionaryHelper.trainStationExpressAliasDataProvider()
        );
    }

    @Test
    public void testCreateTrainReservation() {
        var orderRequest = TrainTestHelpers.createTrainServiceData();
        var offer = TrainTestHelpers.createOffer().toBuilder();
        offer.getTrainInfoBuilder().setTrainTitle("Ростов-на-Дону — Екатеринбург");
        offer.getTrainInfoBuilder().setBrandTitle("Малахит");

        TrainReservation payload = mapService.createTrainReservation(orderRequest, false, 213, offer.build());

        assertThat(payload.getReservationRequestData().getStartSettlementTitle()).isEqualTo("Ростов-на-Дону");
        assertThat(payload.getReservationRequestData().getBrandTitle()).isEqualTo("Малахит");
        assertThat(payload.isMoscowRegion()).isTrue();

        assertThat(payload.getStationFromTimezone()).isEqualTo("Europe/Moscow");
        assertThat(payload.getStationFromRailwayTimezone()).isEqualTo("Europe/Moscow");
        assertThat(payload.getStationToTimezone()).isEqualTo("Europe/Moscow");
        assertThat(payload.getStationToRailwayTimezone()).isEqualTo("Europe/Moscow");

        assertThat(payload.getUiData().getStationFromRailwayTimeZoneText()).isEqualTo("по Московскому времени");
        assertThat(payload.getUiData().getStationToRailwayTimeZoneText()).isEqualTo("по Московскому времени");

        assertThat(payload.getPassengers().get(0).getTariffCode()).isEqualTo("full");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTrainReservationErrorRequestedPlaces() {
        var orderRequest = TrainTestHelpers.createTrainServiceData();
        var offer = TrainTestHelpers.createOffer().toBuilder();
        offer.getPassengersBuilder(0).clearPlaces().addAllPlaces(List.of(10));
        offer.getPassengersBuilder(1).clearPlaces();
        mapService.createTrainReservation(orderRequest, false, 213, offer.build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTrainReservationErrorBabyRequestedPlaces() {
        var orderRequest = TrainTestHelpers.createTrainServiceData();
        var offer = TrainTestHelpers.createOffer().toBuilder();
        offer.getPassengersBuilder(0).clearPlaces().addAllPlaces(List.of(10));
        offer.getPassengersBuilder(1).setPassengerCategory(EPassengerCategory.PC_BABY)
                .clearPlaces().addAllPlaces(List.of(11));
        mapService.createTrainReservation(orderRequest, false, 213, offer.build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTrainReservationErrorDifferentPassengersCount() {
        var orderRequest = TrainTestHelpers.createTrainServiceData();
        var offer = TrainTestHelpers.createOffer().toBuilder();
        offer.removePassengers(1);
        mapService.createTrainReservation(orderRequest, false, 213, offer.build());
    }
}
