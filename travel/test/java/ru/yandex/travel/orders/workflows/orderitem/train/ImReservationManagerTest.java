package ru.yandex.travel.orders.workflows.orderitem.train;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.orders.factories.TrainOrderItemFactory;
import ru.yandex.travel.orders.services.train.ImReservationManager;
import ru.yandex.travel.orders.services.train.tariffinfo.TrainTariffInfoDataProvider;
import ru.yandex.travel.train.model.AdditionalPlaceRequirements;
import ru.yandex.travel.train.model.CabinGenderKind;
import ru.yandex.travel.train.model.CabinPlaceDemands;
import ru.yandex.travel.train.model.CarStorey;
import ru.yandex.travel.train.model.CarType;
import ru.yandex.travel.train.model.DocumentType;
import ru.yandex.travel.train.model.PassengerCategory;
import ru.yandex.travel.train.model.Sex;
import ru.yandex.travel.train.model.TariffType;
import ru.yandex.travel.train.partners.im.model.OrderFullCustomerRequest;
import ru.yandex.travel.train.partners.im.model.RailwayPassengerRequest;
import ru.yandex.travel.train.partners.im.model.RailwayReservationRequest;
import ru.yandex.travel.train.partners.im.model.ReservationCreateRequest;

import static org.assertj.core.api.Assertions.assertThat;

public class ImReservationManagerTest {
    TrainTariffInfoDataProvider trainTariffInfoDataProvider;

    @Before
    public void setUp() {
        trainTariffInfoDataProvider = HandlerTestHelper.createTrainTariffInfoDataProvider();
    }

    @Test
    public void testCreateRequest() {
        var factory = new TrainOrderItemFactory();
        factory.setFirstName("Светлана");
        factory.setPatronymic("Владимировна");
        factory.setSex(Sex.FEMALE);
        factory.setLowerPlaceQuantity(1);
        factory.setUpperPlaceQuantity(0);
        factory.setBirthday(LocalDate.parse("1990-07-26"));
        var trainOrderItem = factory.createTrainOrderItem();
        trainOrderItem.getReservation().getReservationRequestData().setElectronicRegistrationEnabled(true);
        trainOrderItem.getReservation().getPassengers().get(0).setUsePhoneForReservation(false);
        ReservationCreateRequest actualRequest = ImReservationManager.createRequest(
                trainTariffInfoDataProvider, trainOrderItem, List.of());
        var request = new ReservationCreateRequest();
        request.setCustomers(new ArrayList<>());
        var requestCustomer = new OrderFullCustomerRequest();
        request.getCustomers().add(requestCustomer);
        requestCustomer.setBirthday(LocalDateTime.parse("1990-07-26T00:00"));
        requestCustomer.setCitizenshipCode("RU");
        requestCustomer.setDocumentNumber("020291191100");
        requestCustomer.setDocumentType(DocumentType.RUSSIAN_PASSPORT);
        requestCustomer.setFirstName("Светлана");
        requestCustomer.setLastName("McClane");
        requestCustomer.setMiddleName("Владимировна");
        requestCustomer.setSex(Sex.FEMALE);
        request.setReservationItems(new ArrayList<>());
        var requestItem = new RailwayReservationRequest();
        request.getReservationItems().add(requestItem);
        requestItem.setAdditionalPlaceRequirements(AdditionalPlaceRequirements.NO_VALUE);
        requestItem.setCabinGenderKind(CabinGenderKind.NO_VALUE);
        requestItem.setCabinPlaceDemands(CabinPlaceDemands.NO_VALUE);
        requestItem.setCarStorey(CarStorey.NO_VALUE);
        requestItem.setBedding(true);
        requestItem.setCarType(CarType.SEDENTARY);
        requestItem.setDepartureDate(LocalDateTime.parse("2019-07-09T19:55:00"));
        requestItem.setDestinationCode("2064001");
        requestItem.setOriginCode("2064110");
        requestItem.setLowerPlaceQuantity(1);
        requestItem.setUpperPlaceQuantity(0);
        requestItem.setProviderPaymentForm("Card");
        requestItem.setServiceClass("2Ж");
        requestItem.setInternationalServiceClass("1/2");
        requestItem.setSetElectronicRegistration(true);
        requestItem.setTrainNumber("820Э");
        requestItem.setPassengers(new ArrayList<>());
        var requestPassenger = new RailwayPassengerRequest();
        requestItem.getPassengers().add(requestPassenger);
        requestPassenger.setCategory(PassengerCategory.ADULT);
        requestPassenger.setPreferredAdultTariffType("Full");
        requestPassenger.setContactEmailOrPhone("email@email.com");
        assertThat(actualRequest).isEqualTo(request);
    }

    @Test
    public void testCreateRequestWithoutTariffCode() {
        var factory = new TrainOrderItemFactory();
        var trainOrderItem = factory.createTrainOrderItem();
        trainOrderItem.getReservation().getPassengers().get(0).setTariffType(TariffType.FULL);
        trainOrderItem.getReservation().getPassengers().get(0).setTariffCode(null);

        var trainTariffInfoDataProvider = HandlerTestHelper.createTrainTariffInfoDataProvider();
        ReservationCreateRequest actualRequest = ImReservationManager.createRequest(
                trainTariffInfoDataProvider, trainOrderItem, List.of());

        assertThat(actualRequest.getReservationItems().get(0).getPassengers().get(0)
                .getPreferredAdultTariffType()).isEqualTo("Full");
    }

    @Test
    public void testSeparatePassengersReserving() {
        var factory = new TrainOrderItemFactory();
        var p1 = factory.createTrainPassenger();
        p1.setDocumentNumber("p1-doc");
        p1.setRequestedPlaces(List.of(5));
        var p2 = factory.createTrainPassenger();
        p2.setDocumentNumber("p2-doc");
        p2.setRequestedPlaces(List.of(10));
        factory.setPassengers(List.of(p1, p2));
        var trainOrderItem = factory.createTrainOrderItem();
        trainOrderItem.getPayload().setSeparatePassengersReserving(true);
        ReservationCreateRequest actualRequest = ImReservationManager.createRequest(
                trainTariffInfoDataProvider, trainOrderItem, List.of());
        assertThat(actualRequest.getCustomers().size()).isEqualTo(2);
        OrderFullCustomerRequest p1Customer = getCustomerByDocument(actualRequest, p1.getDocumentNumber());
        OrderFullCustomerRequest p2Customer = getCustomerByDocument(actualRequest, p2.getDocumentNumber());
        assertThat(actualRequest.getReservationItems().size()).isEqualTo(2);
        RailwayReservationRequest p1ReserveItem = findItemByCustomer(actualRequest, p1Customer.getIndex());
        RailwayReservationRequest p2ReserveItem = findItemByCustomer(actualRequest, p2Customer.getIndex());
        assertThat(p1ReserveItem.getPlaceRange().getFrom())
                .isEqualTo(p1ReserveItem.getPlaceRange().getTo())
                .isEqualTo(p1.getRequestedPlaces().get(0));
        assertThat(p2ReserveItem.getPlaceRange().getFrom())
                .isEqualTo(p2ReserveItem.getPlaceRange().getTo())
                .isEqualTo(p2.getRequestedPlaces().get(0));
        assertThat(p1ReserveItem.getPassengers().size()).isEqualTo(1);
        assertThat(p2ReserveItem.getPassengers().size()).isEqualTo(1);
    }

    private RailwayReservationRequest findItemByCustomer(ReservationCreateRequest actualRequest, int customerIndex) {
        return actualRequest.getReservationItems().stream()
                .filter(i -> i.getPassengers().stream().anyMatch(p -> p.getOrderCustomerIndex() == customerIndex))
                .findFirst().orElseThrow();
    }

    private OrderFullCustomerRequest getCustomerByDocument(ReservationCreateRequest request, String docNumber) {
        return request.getCustomers().stream().filter(c -> c.getDocumentNumber().equals(docNumber))
                .findFirst().orElseThrow();
    }

    @Test
    public void testSeparatePassengersReservingFalse() {
        var factory = new TrainOrderItemFactory();
        var p1 = factory.createTrainPassenger();
        var p2 = factory.createTrainPassenger();
        factory.setPassengers(List.of(p1, p2));
        var trainOrderItem = factory.createTrainOrderItem();
        trainOrderItem.getPayload().getReservationRequestData().setPlaceNumberFrom(22);
        trainOrderItem.getPayload().getReservationRequestData().setPlaceNumberTo(23);
        trainOrderItem.getPayload().setSeparatePassengersReserving(false);
        ReservationCreateRequest actualRequest = ImReservationManager.createRequest(
                trainTariffInfoDataProvider, trainOrderItem, List.of());
        assertThat(actualRequest.getCustomers().size()).isEqualTo(2);
        assertThat(actualRequest.getReservationItems().size()).isEqualTo(1);
        RailwayReservationRequest reserveItem = actualRequest.getReservationItems().get(0);
        assertThat(reserveItem.getPassengers().size()).isEqualTo(2);
        assertThat(reserveItem.getPlaceRange().getFrom()).isEqualTo(22);
        assertThat(reserveItem.getPlaceRange().getTo()).isEqualTo(23);
    }
}
