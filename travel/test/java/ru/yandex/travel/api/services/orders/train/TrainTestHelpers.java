package ru.yandex.travel.api.services.orders.train;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.javamoney.moneta.Money;

import ru.yandex.travel.api.endpoints.generic_booking_flow.model.CreateTrainServiceData;
import ru.yandex.travel.api.endpoints.trains_booking_flow.req_rsp.CreateOrderReqV2;
import ru.yandex.travel.api.models.subscriptions.PromoSubscriptionRequestParams;
import ru.yandex.travel.api.models.train.CreateOrderPassengerV2;
import ru.yandex.travel.api.models.train.CreateOrderUserInfo;
import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.commons.proto.ProtoUtils;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.testing.misc.TestResources;
import ru.yandex.travel.train.model.AdditionalPlaceRequirements;
import ru.yandex.travel.train.model.CabinGenderKind;
import ru.yandex.travel.train.model.CabinPlaceDemands;
import ru.yandex.travel.train.model.CarStorey;
import ru.yandex.travel.train.model.CarType;
import ru.yandex.travel.train.model.DocumentType;
import ru.yandex.travel.train.model.PassengerCategory;
import ru.yandex.travel.train.model.ReservationPlaceType;
import ru.yandex.travel.train.model.Sex;
import ru.yandex.travel.train.model.TariffType;
import ru.yandex.travel.train.model.TrainPassenger;
import ru.yandex.travel.train.model.TrainPlace;
import ru.yandex.travel.train.model.TrainReservation;
import ru.yandex.travel.train.model.TrainReservationRequestData;
import ru.yandex.travel.train.model.TrainTicket;
import ru.yandex.travel.train.partners.im.model.ImBlankStatus;
import ru.yandex.travel.trains.proto.ECarType;
import ru.yandex.travel.trains.proto.EDocumentType;
import ru.yandex.travel.trains.proto.EPassengerCategory;
import ru.yandex.travel.trains.proto.ESex;
import ru.yandex.travel.trains.proto.TPassenger;
import ru.yandex.travel.trains.proto.TTrainServiceOffer;

// todo(tlg-13,ganintsev): move it out, the test object factories should be defined in a separate lib w/o copy-pasting
public class TrainTestHelpers {

    private TrainTestHelpers() {
    }

    public static TTrainServiceOffer createOffer() {
        var departure = OffsetDateTime.of(LocalDateTime.parse("2019-07-31T04:55:00"), ZoneOffset.UTC);
        var offer = TTrainServiceOffer.newBuilder();
        offer.setOfferId("8b25e3b6-86bb-4b51-9342-793458f82ea2");
        offer.addAllPlaces(List.of(9, 10));
        offer.setCarType(ECarType.CT_COMPARTMENT);
        offer.setCarNumber("14");
        offer.setBedding(true);
        offer.setDeparture(ProtoUtils.fromInstant(departure.toInstant()));
        offer.setElectronicRegistration(true);
        offer.setInternationalServiceClass("1/2");
        offer.setPartner("im");
        offer.setServiceClass("2Л");
        offer.setStationFromId(9612620);
        offer.setStationToId(9607404);
        offer.getTrainInfoBuilder()
                .setTrainNumber("118Н")
                .setTrainTicketNumber("118Н")
                .setTrainTitle("Москва — Санкт-Петербург");
        offer.addPassengers(createOfferPassenger(0));
        offer.addPassengers(createOfferPassenger(1));
        return offer.build();
    }

    public static CreateOrderReqV2 createOrderRequestV2() {
        var request = new CreateOrderReqV2();
        request.setOfferId(UUID.fromString("8b25e3b6-86bb-4b51-9342-793458f82ea2"));
        request.setLabel("qwertyuiopasdfghjklzxcvbnm");
        request.setOrderHistory(createOrderHistory());
        request.setDeduplicationKey("5fcc3c21-255e-4eee-a886-045c66d91728");
        request.setPassengers(new ArrayList<>());
        request.getPassengers().add(createPassengerV2(0, "Евгений", "1990-05-01", "3709554356"));
        request.getPassengers().add(createPassengerV2(1, "Иван", "1990-10-05", "3709534511"));
        request.setUserInfo(new CreateOrderUserInfo());
        request.setCustomerEmail("ganintsev@yandex-team.ru");
        request.setCustomerPhone("+79122865336");
        request.getUserInfo().setIp("2a02:6b8:0:2807:d15b:2e28:ef16:33ee");
        request.getUserInfo().setMobile(true);
        request.getUserInfo().setGeoId(54);
        request.getUserInfo().setYandexUid("218319051539955724");
        request.setSubscriptionParams(createSubscriptionRequestParams());
        return request;
    }

    public static CreateTrainServiceData createTrainServiceData() {
        var request = new CreateTrainServiceData();
        request.setOfferId(UUID.fromString("8b25e3b6-86bb-4b51-9342-793458f82ea2"));
        request.setPassengers(new ArrayList<>());
        request.getPassengers().add(createPassengerV2(0, "Евгений", "1990-05-01", "3709554356"));
        request.getPassengers().add(createPassengerV2(1, "Иван", "1990-10-05", "3709534511"));
        return request;
    }

    public static PromoSubscriptionRequestParams createSubscriptionRequestParams() {
        PromoSubscriptionRequestParams params = new PromoSubscriptionRequestParams();
        params.setLanguage("ru");
        params.setNationalVersion("ru");
        params.setSubscribe(true);
        params.setTimezone("Europe/Moscow");
        return params;
    }

    private static JsonNode createOrderHistory() {
        try {
            var mapper = new ObjectMapper();
            return mapper.readTree(TestResources.readResource("trains_booking_flow/order_history.json"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static CreateOrderPassengerV2 createPassengerV2(int index, String name, String birthDate,
                                                            String documentNumber) {
        var passenger1 = new CreateOrderPassengerV2();
        passenger1.setIndex(index);
        passenger1.setBirthDate(LocalDate.parse(birthDate));
        passenger1.setDocId(documentNumber);
        passenger1.setFirstName(name);
        passenger1.setLastName(name);
        passenger1.setPatronymic("-");
        passenger1.setPhone("79123456789");
        passenger1.setEmail("email@email.com");
        return passenger1;
    }

    private static TPassenger createOfferPassenger(int index) {
        return TPassenger.newBuilder()
                .setPassengerCategory(EPassengerCategory.PC_ADULT)
                .setCitizenshipGeoId(225)
                .setDocumentType(EDocumentType.DT_RUSSIAN_PASSPORT)
                .setSex(ESex.S_MALE)
                .setTariffCode("full")
                .setPassengerId(index)
                .build();
    }

    public static TrainReservation createTrainReservation(EOrderItemState orderItemState) {
        return createTrainReservation(orderItemState, 1);
    }

    public static TrainReservation createTrainReservation(EOrderItemState orderItemState, int passengers) {
        LocalDateTime arrivalTime = LocalDateTime.parse("2019-07-10T16:55:00");
        String reservationNumber = null;
        int partnerOrderId = 777777777;
        String partnerDescription = "ПРЕДВАРИТЕЛЬНЫЙ ДОСМОТР НА ВОКЗАЛЕ.";
        String carrier = "ДОСС РЖД";
        boolean isSuburban = false;
        boolean onlyFullReturnPossible = false;
        String stationFromCode = "2064110";
        String stationToCode = "2064001";
        LocalDateTime departureTime = LocalDate.now().plusDays(1).atTime(16, 55);
        String trainNumber = "820Э";
        AdditionalPlaceRequirements additionalPlaceRequirements = AdditionalPlaceRequirements.NO_VALUE;
        boolean bedding = true;
        CabinGenderKind cabinGenderKind = CabinGenderKind.NO_VALUE;
        CabinPlaceDemands cabinPlaceDemands = CabinPlaceDemands.NO_VALUE;
        String carNumber = null;
        CarStorey carStorey = CarStorey.NO_VALUE;
        CarType carType = CarType.SEDENTARY;
        String serviceClass = "2Ж";

        var payload = new TrainReservation();
        payload.setStationFromCode(stationFromCode);
        payload.setStationToCode(stationToCode);
        var requestData = new TrainReservationRequestData();
        payload.setReservationRequestData(requestData);
        requestData.setAdditionalPlaceRequirements(additionalPlaceRequirements);
        requestData.setStationFromCode(stationFromCode);
        requestData.setStationToCode(stationToCode);
        requestData.setDepartureTime(departureTime.toInstant(ZoneOffset.UTC));
        requestData.setTrainNumber(trainNumber);
        requestData.setAdditionalPlaceRequirements(additionalPlaceRequirements);
        requestData.setBedding(bedding);
        requestData.setCabinGenderKind(cabinGenderKind);
        requestData.setCabinPlaceDemands(cabinPlaceDemands);
        requestData.setCarNumber(carNumber);
        requestData.setCarStorey(carStorey);
        requestData.setCarType(carType);
        requestData.setServiceClass(serviceClass);
        requestData.setImInitialStationName("МОСКВА ЯР");

        payload.setPassengers(new ArrayList<>());
        for (int i = 0; i < passengers; i++) {
            String firstName = "John";
            String lastName = "McClane";
            String patronymic = "Джонович";
            Sex sex = Sex.MALE;
            LocalDate birthday = LocalDate.parse("1988-07-15");
            DocumentType documentType = DocumentType.RUSSIAN_PASSPORT;
            String documentNumber = "020291191100";
            String citizenshipCode = "RU";
            PassengerCategory category = PassengerCategory.ADULT;
            TariffType tariffType = TariffType.FULL;
            String tariffCode = "full";
            boolean discountDenied = true;

            var passenger = new TrainPassenger();
            passenger.setFirstName(firstName);
            passenger.setLastName(lastName);
            passenger.setPatronymic(patronymic);
            passenger.setSex(sex);
            passenger.setBirthday(birthday);
            passenger.setDocumentType(documentType);
            passenger.setDocumentNumber(documentNumber);
            passenger.setCitizenshipCode(citizenshipCode);
            passenger.setCategory(category);
            passenger.setTariffType(tariffType);
            passenger.setTariffCode(tariffCode);
            passenger.setDiscountDenied(discountDenied);
            int blankId = 100000001;
            List<TrainPlace> places = new ArrayList<>();
            Money tariffAmount = Money.of(BigDecimal.valueOf(3000.0), ProtoCurrencyUnit.RUB);
            Money tariffVatAmount = Money.of(BigDecimal.ZERO, ProtoCurrencyUnit.RUB);
            Double tariffVatRate = 0.0;
            Money serviceAmount = Money.of(BigDecimal.valueOf(156.0), ProtoCurrencyUnit.RUB);
            Money serviceVatAmount = Money.of(BigDecimal.valueOf(26.0), ProtoCurrencyUnit.RUB);
            Double serviceVatRate = 20.0;
            Money feeAmount = Money.of(BigDecimal.valueOf(333.3), ProtoCurrencyUnit.RUB);
            String placeNumber = "1";
            ReservationPlaceType placeType = ReservationPlaceType.NEAR_TABLE_FORWARD;

            if (orderItemState != EOrderItemState.IS_NEW) {
                var trainTicket = new TrainTicket();
                var p = new TrainPlace();
                p.setType(placeType);
                p.setNumber(placeNumber);
                places.add(p);
                trainTicket.setPlaces(places);
                trainTicket.setBlankId(blankId);
                trainTicket.setTariffAmount(tariffAmount);
                trainTicket.setTariffVatAmount(tariffVatAmount);
                trainTicket.setTariffVatRate(tariffVatRate);
                trainTicket.setServiceAmount(serviceAmount);
                trainTicket.setServiceVatAmount(serviceVatAmount);
                trainTicket.setServiceVatRate(serviceVatRate);
                trainTicket.setFeeAmount(feeAmount);
                trainTicket.setRawTariffType("Child");

                if (orderItemState == EOrderItemState.IS_CONFIRMED) {
                    trainTicket.setImBlankStatus(ImBlankStatus.REMOTE_CHECK_IN);
                }
                passenger.setTicket(trainTicket);
            }
            payload.getPassengers().add(passenger);
        }

        if (orderItemState != EOrderItemState.IS_NEW) {
            payload.setStationFromCode(stationFromCode);
            payload.setStationToCode(stationToCode);
            payload.setDepartureTime(departureTime.toInstant(ZoneOffset.UTC));
            payload.setArrivalTime(arrivalTime.toInstant(ZoneOffset.UTC));
            payload.setTrainNumber(trainNumber);
            payload.setCarNumber(carNumber);
            payload.setCarType(carType);
            payload.setReservationNumber(reservationNumber);
            payload.setPartnerOrderId(partnerOrderId);
            payload.setPartnerDescription(partnerDescription);
            payload.setCarrier(carrier);
            payload.setSuburban(isSuburban);
            payload.setOnlyFullReturnPossible(onlyFullReturnPossible);
        }
        return payload;
    }
}
