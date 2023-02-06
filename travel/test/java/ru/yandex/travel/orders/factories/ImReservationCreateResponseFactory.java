package ru.yandex.travel.orders.factories;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.Data;

import ru.yandex.travel.orders.entities.TrainOrderItem;
import ru.yandex.travel.train.model.CarType;
import ru.yandex.travel.train.model.PassengerCategory;
import ru.yandex.travel.train.model.ReservationPlaceType;
import ru.yandex.travel.train.model.TrainPassenger;
import ru.yandex.travel.train.model.TrainTicket;
import ru.yandex.travel.train.partners.im.model.OrderCreateReservationCustomerResponse;
import ru.yandex.travel.train.partners.im.model.PlaceWithType;
import ru.yandex.travel.train.partners.im.model.RailwayFareInfo;
import ru.yandex.travel.train.partners.im.model.RailwayPassengerResponse;
import ru.yandex.travel.train.partners.im.model.RailwayReservationBlankResponse;
import ru.yandex.travel.train.partners.im.model.RailwayReservationResponse;
import ru.yandex.travel.train.partners.im.model.RateValue;
import ru.yandex.travel.train.partners.im.model.ReservationCreateResponse;
import ru.yandex.travel.train.partners.im.model.TariffInfo;

@Data
public class ImReservationCreateResponseFactory {
    private TrainOrderItem trainOrderItem;
    private List<TrainTicket> tickets;
    private int orderId = 1234567890;
    private int orderItemId = 30000001;
    private int orderCustomerStartId = 10000001;
    private int orderBlankStartId = 20000001;
    private int placeStartNumber = 10;
    private ReservationPlaceType placeType = ReservationPlaceType.NEAR_TABLE_FORWARD;

    private BigDecimal tariffAmount = BigDecimal.valueOf(3000.0);
    private BigDecimal tariffVatAmount = BigDecimal.ZERO;
    private Double tariffVatRate = 0.0;
    private BigDecimal serviceAmount = BigDecimal.valueOf(156.0);
    private BigDecimal serviceVatAmount = BigDecimal.valueOf(26.0);
    private Double serviceVatRate = 20.0;
    private int tripDuration = 60*24;
    private String carrierInn = "77777777";
    private String carDescription = "";
    private String carNumber = "02";
    private String carrier = "ФПК";
    private String carrierCode = "42";
    private CarType carType = CarType.SEDENTARY;
    private LocalDateTime confirmTill = LocalDateTime.now(Clock.system(ZoneId.of("UTC+3"))).plusMinutes(15);
    private String countryCode = "RU";
    private int destinationTimeZoneDifference = 0;
    private int originTimeZoneDifference = 0;
    private boolean onlyFullReturnPossible = false;
    private String timeDescription = "ПРЕДВАРИТЕЛЬНЫЙ ДОСМОТР НА ВОКЗАЛЕ.";
    private String trainDescription = "СКОРЫЙ";

    public ImReservationCreateResponseFactory(TrainOrderItem trainOrderItem) {
        this(trainOrderItem, 1);
    }

    public ImReservationCreateResponseFactory(TrainOrderItem trainOrderItem, int passengers) {
        if (trainOrderItem == null) {
            TrainOrderItemFactory factory = new TrainOrderItemFactory();
            for (int i = 0; i < passengers; i++) {
                factory.getPassengers().add(factory.createTrainPassenger());
            }
            trainOrderItem = factory.createTrainOrderItem();
        }
        this.trainOrderItem = trainOrderItem;
        var requestData = trainOrderItem.getPayload().getReservationRequestData();
        if (requestData.getCarNumber() != null) {
            carNumber = requestData.getCarNumber();
        }
        if (requestData.getPlaceNumberFrom() != null) {
            placeStartNumber = requestData.getPlaceNumberFrom();
        }
        if (requestData.getCarType() != null && requestData.getCarType() != CarType.UNKNOWN) {
            carType = requestData.getCarType();
        }
        if (trainOrderItem.getExpiresAt() != null) {
            confirmTill = LocalDateTime.ofInstant(trainOrderItem.getExpiresAt(), Clock.systemUTC().getZone());
        }
    }

    public ReservationCreateResponse createReservationCreateResponse() {
        var response = new ReservationCreateResponse();
        var trainOrderItem = this.trainOrderItem;
        int customerId = this.orderCustomerStartId;
        int blankId = this.orderBlankStartId;
        int placeNumber = this.placeStartNumber;
        response.setCustomers(new ArrayList<>());
        var reservation = new RailwayReservationResponse();
        response.setReservationResults(new ArrayList<>());
        response.getReservationResults().add(reservation);
        reservation.setBlanks(new ArrayList<>());
        reservation.setPassengers(new ArrayList<>());
        var orderAmount = BigDecimal.ZERO;
        List<TrainPassenger> passengers = trainOrderItem.getPayload().getPassengers();
        for (int i = 0; i < passengers.size(); i++) {
            TrainPassenger p = passengers.get(i);
            var customer = new OrderCreateReservationCustomerResponse();
            response.getCustomers().add(customer);
            customer.setBirthday(p.getBirthday().atStartOfDay());
            customer.setCitizenshipCode(p.getCitizenshipCode());
            customer.setDocumentNumber(p.getDocumentNumber());
            customer.setDocumentType(p.getDocumentType());
            customer.setFirstName(p.getFirstName());
            customer.setLastName(p.getLastName());
            customer.setMiddleName(p.getPatronymic());
            customer.setSex(p.getSex());
            customer.setIndex(i);
            customer.setOrderCustomerId(customerId);

            var passenger = new RailwayPassengerResponse();
            reservation.getPassengers().add(passenger);
            passenger.setCategory(p.getCategory());
            passenger.setOrderCustomerId(customerId);
            passenger.setOrderItemBlankId(blankId);
            passenger.setPlacesWithType(new ArrayList<>());
            if (p.getCategory() != PassengerCategory.BABY) {
                passenger.setAmount(tariffAmount.add(serviceAmount));
                orderAmount = orderAmount.add(tariffAmount.add(serviceAmount));
                var place = new PlaceWithType();
                passenger.getPlacesWithType().add(place);
                place.setNumber(String.format("%03d", placeNumber));
                place.setType(placeType);
                placeNumber++;
            } else {
                passenger.setAmount(BigDecimal.ZERO);
            }

            if (p.getCategory() != PassengerCategory.BABY) {
                var blank = new RailwayReservationBlankResponse();
                reservation.getBlanks().add(blank);
                blank.setOrderItemBlankId(blankId);
                blank.setAmount(tariffAmount.add(serviceAmount));
                blank.setAdditionalPrice(tariffAmount.add(serviceAmount));
                blank.setBaseFare(BigDecimal.ZERO);
                blank.setFareInfo(new RailwayFareInfo());
                blank.getFareInfo().setCarrierTin(carrierInn);
                blank.setServicePrice(serviceAmount);
                blank.setTariffType(MAP_TARIFF_CODE_TO_IM_RESPONSE_CODE.get(p.getTariffCodeWithFallback()));
                blank.setTariffInfo(new TariffInfo());
                blank.getTariffInfo().setTariffType(MAP_TARIFF_CODE_TO_IM_RESPONSE_CODE.get(p.getTariffCodeWithFallback()));
                blank.setVatRateValues(new ArrayList<>());
                var tariffVat = new RateValue();
                blank.getVatRateValues().add(tariffVat);
                tariffVat.setRate(tariffVatRate);
                tariffVat.setValue(tariffVatAmount);
                var serviceVat = new RateValue();
                blank.getVatRateValues().add(serviceVat);
                serviceVat.setRate(serviceVatRate);
                serviceVat.setValue(serviceVatAmount);
                blankId++;
            }
            customerId++;
        }
        reservation.setAmount(orderAmount);
        var reservationRequestData = trainOrderItem.getPayload().getReservationRequestData();
        reservation.setTripDuration(tripDuration);
        reservation.setCarDescription(carDescription);
        reservation.setCarNumber(carNumber);
        reservation.setCarrier(carrier);
        reservation.setCarrierCode(carrierCode);
        reservation.setCarrierTin(carrierInn);
        reservation.setCarType(carType);
        reservation.setConfirmTill(confirmTill);
        reservation.setCountryCode(countryCode);
        reservation.setDestinationStation("");
        reservation.setDestinationStationCode(reservationRequestData.getStationToCode());
        reservation.setDestinationTimeZoneDifference(destinationTimeZoneDifference);
        reservation.setOriginStation("");
        reservation.setOriginStationCode(reservationRequestData.getStationFromCode());
        reservation.setOriginTimeZoneDifference(originTimeZoneDifference);
        reservation.setIndex(0);
        reservation.setDepartureDateTime(reservationRequestData.getDepartureTime().atOffset(ZoneOffset.ofHours(3)).toLocalDateTime());
        reservation.setArrivalDateTime(reservation.getDepartureDateTime().plusMinutes(tripDuration));
        reservation.setLocalDepartureDateTime(reservation.getDepartureDateTime().plusHours(originTimeZoneDifference));
        reservation.setLocalArrivalDateTime(reservation.getArrivalDateTime().plusHours(destinationTimeZoneDifference));
        reservation.setOnlyFullReturnPossible(onlyFullReturnPossible);
        reservation.setOrderItemId(orderItemId);
        reservation.setServiceClass(reservationRequestData.getServiceClass());
        reservation.setSuburban(false);
        reservation.setTimeDescription(timeDescription);
        reservation.setTrainDescription(trainDescription);
        reservation.setTrainNumber(reservationRequestData.getTrainNumber());

        response.setReservationNumber(null);
        response.setAmount(orderAmount);
        response.setOrderId(orderId);
        return response;
    }

    private static final Map<String, String> MAP_TARIFF_CODE_TO_IM_RESPONSE_CODE = Map.of(
            "full", "Full",
            "senior", "Senior",
            "junior", "Junior"
    );
}
