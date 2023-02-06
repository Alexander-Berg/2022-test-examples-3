package ru.yandex.travel.orders.factories;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Data;
import org.javamoney.moneta.Money;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.orders.entities.FiscalItem;
import ru.yandex.travel.orders.entities.TrainOrder;
import ru.yandex.travel.orders.entities.TrainOrderItem;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.train.model.AdditionalPlaceRequirements;
import ru.yandex.travel.train.model.CabinGenderKind;
import ru.yandex.travel.train.model.CabinPlaceDemands;
import ru.yandex.travel.train.model.CarStorey;
import ru.yandex.travel.train.model.CarType;
import ru.yandex.travel.train.model.DocumentType;
import ru.yandex.travel.train.model.Insurance;
import ru.yandex.travel.train.model.InsuranceStatus;
import ru.yandex.travel.train.model.PassengerCategory;
import ru.yandex.travel.train.model.RailwayBonusCard;
import ru.yandex.travel.train.model.ReservationPlaceType;
import ru.yandex.travel.train.model.Sex;
import ru.yandex.travel.train.model.TariffType;
import ru.yandex.travel.train.model.TrainPassenger;
import ru.yandex.travel.train.model.TrainPlace;
import ru.yandex.travel.train.model.TrainReservation;
import ru.yandex.travel.train.model.TrainReservationRequestData;
import ru.yandex.travel.train.model.TrainReservationUiData;
import ru.yandex.travel.train.model.TrainTicket;
import ru.yandex.travel.train.partners.im.model.ImBlankStatus;
import ru.yandex.travel.train.partners.im.model.orderinfo.ImOperationStatus;
import ru.yandex.travel.workflow.entities.Workflow;

@Data
public class TrainOrderItemFactory {
    // TrainReservationRequestData
    private String stationFromCode = "2064110";
    private String stationToCode = "2064001";
    private Instant departureTime = Instant.parse("2019-07-09T16:55:00Z");
    private String trainNumber = "820Э";
    private String trainTicketNumber = "821Э";
    private AdditionalPlaceRequirements additionalPlaceRequirements = AdditionalPlaceRequirements.NO_VALUE;
    private boolean bedding = true;
    private CabinGenderKind cabinGenderKind = CabinGenderKind.NO_VALUE;
    private CabinPlaceDemands cabinPlaceDemands = CabinPlaceDemands.NO_VALUE;
    private String carNumber = null;
    private CarStorey carStorey = CarStorey.NO_VALUE;
    private CarType carType = CarType.SEDENTARY;
    private String serviceClass = "2Ж";
    private String internationalServiceClass = "1/2";
    private boolean giveChildWithoutPlace;
    private Integer lowerPlaceQuantity;
    private Integer upperPlaceQuantity;
    private Integer placeNumberFrom;
    private Integer placeNumberTo;

    // TrainReservationUiData
    private String stationFromTitleGenitive = "Москвы (Ленинградский вокзал)";
    private String stationFromTimezone = "Europe/Moscow";
    private String stationToTimezone = "Europe/Moscow";
    private String stationToPreposition = "в";
    private String stationToTitleAccusative = "Санкт-Петербург (Московский вокзал)";

    // TrainReservation
    private Instant arrivalTime = Instant.parse("2019-07-10T16:55:00Z");
    private String reservationNumber = null;
    private int partnerOrderId = 777777777;
    private int partnerOrderItemId = 70000001;
    private String partnerDescription = "ПРЕДВАРИТЕЛЬНЫЙ ДОСМОТР НА ВОКЗАЛЕ.";
    private String carrier = "ДОСС РЖД";
    private boolean isSuburban = false;
    private boolean onlyFullReturnPossible = false;
    private List<TrainPassenger> passengers = new ArrayList<>() {
    };

    // TrainPassenger
    private int customerId = 2220001;
    private String firstName = "John";
    private String lastName = "McClane";
    private String patronymic = "Джонович";
    private Sex sex = Sex.MALE;
    private LocalDate birthday = LocalDate.parse("1988-07-15");
    private DocumentType documentType = DocumentType.RUSSIAN_PASSPORT;
    private String documentNumber = "020291191100";
    private String citizenshipCode = "RU";
    private PassengerCategory category = PassengerCategory.ADULT;
    private TariffType tariffType = TariffType.FULL;
    private String tariffCode = "full";
    private List<RailwayBonusCard> bonusCards;
    private String phone = "79123456789";
    private String email = "email@email.com";

    // TrainTicket
    private int blankId = 100000001;
    private List<TrainPlace> places = new ArrayList<>();
    private Money tariffAmount = Money.of(BigDecimal.valueOf(3000.0), ProtoCurrencyUnit.RUB);
    private Money tariffVatAmount = Money.of(BigDecimal.ZERO, ProtoCurrencyUnit.RUB);
    private Double tariffVatRate = 0.0;
    private Money serviceAmount = Money.of(BigDecimal.valueOf(156.0), ProtoCurrencyUnit.RUB);
    private Money serviceVatAmount = Money.of(BigDecimal.valueOf(26.0), ProtoCurrencyUnit.RUB);
    private Double serviceVatRate = 20.0;
    private Money feeAmount = Money.of(BigDecimal.valueOf(333.3), ProtoCurrencyUnit.RUB);
    private Money partnerFee = Money.of(BigDecimal.valueOf(50), ProtoCurrencyUnit.RUB);
    private Money partnerRefundFee = Money.of(BigDecimal.valueOf(50), ProtoCurrencyUnit.RUB);
    private String carrierInn;

    // TrainPlace
    private String placeNumber = "1";
    private ReservationPlaceType placeType = ReservationPlaceType.NEAR_TABLE_FORWARD;

    // TrainOrderItem
    private EOrderItemState orderItemState = EOrderItemState.IS_NEW;

    public TrainOrderItemFactory() {
    }

    public TrainPassenger createTrainPassenger() {
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
        passenger.setBonusCards(bonusCards);
        if (orderItemState != EOrderItemState.IS_NEW) {
            passenger.setCustomerId(customerId++);
            passenger.setTicket(createTrainTicket());
            passenger.setInsurance(createInsurance());
        }
        passenger.setPhone(phone);
        passenger.setUsePhoneForReservation(true);
        passenger.setEmail(email);
        passenger.setUseEmailForReservation(true);
        return passenger;
    }

    public Insurance createInsurance() {
        var insurance = new Insurance();
        insurance.setAmount(Money.of(150.0, ProtoCurrencyUnit.RUB));
        insurance.setPartnerOperationId(partnerOrderItemId++);
        insurance.setPartnerOperationStatus(ImOperationStatus.OK);
        insurance.setCompany("простострах");
        return insurance;
    }

    private TrainTicket createTrainTicket() {
        var trainTicket = new TrainTicket();
        var places = new ArrayList<>(this.places);
        if (places.size() == 0 && category != PassengerCategory.BABY) {
            var p = new TrainPlace();
            p.setType(placeType);
            p.setNumber(placeNumber);
            places.add(p);
        }
        trainTicket.setPlaces(places);
        trainTicket.setBlankId(blankId++);
        trainTicket.setPartnerFee(partnerFee);
        trainTicket.setPartnerRefundFee(partnerRefundFee);
        if (category != PassengerCategory.BABY) {
            trainTicket.setTariffAmount(tariffAmount);
            trainTicket.setTariffVatAmount(tariffVatAmount);
            trainTicket.setTariffVatRate(tariffVatRate);
            trainTicket.setServiceAmount(serviceAmount);
            trainTicket.setServiceVatAmount(serviceVatAmount);
            trainTicket.setServiceVatRate(serviceVatRate);
            trainTicket.setFeeAmount(feeAmount);
        }
        if (orderItemState == EOrderItemState.IS_CONFIRMED) {
            trainTicket.setImBlankStatus(ImBlankStatus.REMOTE_CHECK_IN);
        } else if (orderItemState == EOrderItemState.IS_REFUNDED) {
            trainTicket.setImBlankStatus(ImBlankStatus.REFUNDED);
        }
        trainTicket.setCarrierInn(carrierInn);
        return trainTicket;
    }

    public TrainOrderItem createTrainOrderItem() {
        var orderItem = new TrainOrderItem();
        var payload = createTrainReservation();
        orderItem.setReservation(payload);
        orderItem.setState(orderItemState);
        orderItem.setId(UUID.randomUUID());
        orderItem.setWorkflow(Workflow.createWorkflowForEntity(orderItem));
        var order = new TrainOrder();
        order.setWorkflow(new Workflow());
        order.addOrderItem(orderItem);
        order.setCurrency(ProtoCurrencyUnit.RUB);
        return orderItem;
    }

    public TrainReservation createTrainReservation() {
        var payload = new TrainReservation();
        var requestData = new TrainReservationRequestData();
        payload.setReservationRequestData(requestData);
        requestData.setAdditionalPlaceRequirements(additionalPlaceRequirements);
        requestData.setStationFromCode(stationFromCode);
        requestData.setStationToCode(stationToCode);
        requestData.setDepartureTime(departureTime);
        requestData.setTrainNumber(trainNumber);
        requestData.setTrainTicketNumber(trainTicketNumber);
        requestData.setAdditionalPlaceRequirements(additionalPlaceRequirements);
        requestData.setBedding(bedding);
        requestData.setCabinGenderKind(cabinGenderKind);
        requestData.setCabinPlaceDemands(cabinPlaceDemands);
        requestData.setCarNumber(carNumber);
        requestData.setCarStorey(carStorey);
        requestData.setCarType(carType);
        requestData.setServiceClass(serviceClass);
        requestData.setInternationalServiceClass(internationalServiceClass);
        requestData.setGiveChildWithoutPlace(giveChildWithoutPlace);
        requestData.setLowerPlaceQuantity(lowerPlaceQuantity);
        requestData.setUpperPlaceQuantity(upperPlaceQuantity);
        requestData.setPlaceNumberFrom(placeNumberFrom);
        requestData.setPlaceNumberTo(placeNumberTo);
        payload.setStationFromTimezone(stationFromTimezone);
        payload.setStationFromRailwayTimezone(stationFromTimezone);
        payload.setStationToTimezone(stationToTimezone);
        payload.setStationToRailwayTimezone(stationToTimezone);

        var fiscalData = new TrainReservationUiData();
        payload.setUiData(fiscalData);
        fiscalData.setStationFromTitleGenitive(stationFromTitleGenitive);
        fiscalData.setStationToTitleAccusative(stationToTitleAccusative);
        fiscalData.setStationToPreposition(stationToPreposition);

        List<TrainPassenger> passengers = new ArrayList<>(this.passengers);
        payload.setPassengers(passengers);
        if (passengers.size() == 0) {
            passengers.add(createTrainPassenger());
        }
        if (orderItemState != EOrderItemState.IS_NEW) {
            payload.setStationFromCode(stationFromCode);
            payload.setStationToCode(stationToCode);
            payload.setDepartureTime(departureTime);
            payload.setArrivalTime(arrivalTime);
            payload.setTrainNumber(trainNumber);
            payload.setCarNumber(carNumber);
            payload.setCarType(carType);
            payload.setReservationNumber(reservationNumber);
            payload.setPartnerOrderId(partnerOrderId++);
            payload.setPartnerBuyOperationId(partnerOrderItemId++);
            payload.setPartnerDescription(partnerDescription);
            payload.setCarrier(carrier);
            payload.setSuburban(isSuburban);
            payload.setOnlyFullReturnPossible(onlyFullReturnPossible);
        }
        return payload;
    }

    public void fillFiscalItems(TrainOrderItem orderItem) {
        var payload = orderItem.getPayload();
        int internalId = 0;
        for (var p : payload.getPassengers()) {
            if (payload.getInsuranceStatus() == InsuranceStatus.CHECKED_OUT
                    && p.getInsurance() != null && p.getInsurance().getAmount().isPositive()) {
                internalId++;
                addFiscalItem(orderItem, p.getInsurance().getAmount(), internalId);
                p.getInsurance().setFiscalItemInternalId(internalId);
            }
            if (p.getTicket().getTariffAmount().isPositive()) {
                internalId++;
                addFiscalItem(orderItem, p.getTicket().getTariffAmount(), internalId);
                p.getTicket().setTariffFiscalItemInternalId(internalId);
            }
            if (p.getTicket().getServiceAmount().isPositive()) {
                internalId++;
                addFiscalItem(orderItem, p.getTicket().getServiceAmount(), internalId);
                p.getTicket().setServiceFiscalItemInternalId(internalId);
            }
            if (p.getTicket().getFeeAmount().isPositive()) {
                internalId++;
                addFiscalItem(orderItem, p.getTicket().getFeeAmount(), internalId);
                p.getTicket().setFeeFiscalItemInternalId(internalId);
            }
        }
    }

    private static void addFiscalItem(TrainOrderItem orderItem, Money amount, int internalId) {
        FiscalItem fiscalItem = new FiscalItem();
        fiscalItem.setMoneyAmount(amount);
        fiscalItem.setInternalId(internalId);
        fiscalItem.setId((long) (9000000 + internalId));
        orderItem.addFiscalItem(fiscalItem);
    }
}
