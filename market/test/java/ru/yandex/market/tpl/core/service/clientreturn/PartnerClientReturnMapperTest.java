package ru.yandex.market.tpl.core.service.clientreturn;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.order.clientreturn.PartnerClientReturnDetailsDto;
import ru.yandex.market.tpl.api.model.order.clientreturn.PartnerClientReturnItemDto;
import ru.yandex.market.tpl.api.model.order.clientreturn.clientdelivery.PartnerClientDto;
import ru.yandex.market.tpl.api.model.order.clientreturn.clientdelivery.PartnerClientReturnDeliveryToClient;
import ru.yandex.market.tpl.api.model.order.clientreturn.clientdelivery.PartnerDeliveryDetails;
import ru.yandex.market.tpl.api.model.order.partner.PartnerOrderAddressDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskDto;
import ru.yandex.market.tpl.core.domain.client.Client;
import ru.yandex.market.tpl.core.domain.client.ClientData;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnStatus;
import ru.yandex.market.tpl.core.domain.clientreturn.mapper.PartnerClientReturnMapper;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.logistic_request.LogisticRequestPoint;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskDto.ActionType.CANCEL;

@RequiredArgsConstructor
public class PartnerClientReturnMapperTest extends TplAbstractTest {
    private final ClientReturnGenerator clientReturnGenerator;
    private final PartnerClientReturnMapper mapper;
    private final SortingCenterService sortingCenterService;
    private final UserShiftRepository userShiftRepository;
    private final ClientReturnRepository clientReturnRepository;

    private ClientReturn clientReturn;

    private static final List<OrderDeliveryTaskDto.Action> EXPECTED_EDIT_ACTIONS = List.of(
            new OrderDeliveryTaskDto.Action(OrderDeliveryTaskDto.ActionType.EDIT_RECIPIENT),
            new OrderDeliveryTaskDto.Action(OrderDeliveryTaskDto.ActionType.RESCHEDULE_CLIENT_RETURN),
            new OrderDeliveryTaskDto.Action(OrderDeliveryTaskDto.ActionType.EDIT_ADDRESS),
            new OrderDeliveryTaskDto.Action(OrderDeliveryTaskDto.ActionType.CHANGE_COORDINATES),
            new OrderDeliveryTaskDto.Action(OrderDeliveryTaskDto.ActionType.CANCEL)
    );

    @BeforeEach
    void init() {
        clientReturn = clientReturnGenerator.generateReturnFromClient();
    }

    @Test
    void testMapClientDeliveryReturnWithoutTask() {
        clientReturn.getItems().get(0).setCourierComment("comment");
        var result = mapper.mapClientDeliveryReturn(clientReturn);

        var expectedDetails = PartnerClientReturnDetailsDto.builder()
                .externalReturnId(clientReturn.getExternalReturnId())
                .id(clientReturn.getId())
                .status(clientReturn.getStatus().getDescription())
                .type("Клиент")
                .items(List.of(
                        PartnerClientReturnItemDto.builder()
                                .name("Phone")
                                .returnReason("BAD_QUALITY")
                                .returnReasonComment("Does not work")
                                .buyerPrice(BigDecimal.TEN)
                                .clientPhotoUrls(List.of("pic1", "pic2"))
                                .courierComment("comment")
                                .build()
                ))
                .totalBuyerPrice(BigDecimal.TEN)
                .checkouterReturnId(clientReturn.getCheckouterReturnId())
                .externalOrderId(clientReturn.getExternalOrderId())
                .build();

        Client client = clientReturn.getClient();
        var logisticRequestPoint = clientReturn.getLogisticRequestPointFrom();
        var expectedClient = getExpectedClient(client, logisticRequestPoint);
        var expectedAddressDetails = getExpectedAddressDetails(clientReturn);
        var partnerDeliveryDetails = getExpectedDeliveryDetails(clientReturn);

        var expectedDeliveryToClient = getExpectedDeliveryToClient(
                logisticRequestPoint, expectedClient, expectedAddressDetails, partnerDeliveryDetails
        );

        var deliveryToClient = result.getDeliveryToClient();
        assertThat(deliveryToClient).isEqualTo(expectedDeliveryToClient);
        assertThat(result.getDetails()).isEqualTo(expectedDetails);
        assertThat(deliveryToClient.getPartnerDeliveryDetails().getCourierId()).isNull();
        assertThat(deliveryToClient.getActions().containsAll(EXPECTED_EDIT_ACTIONS)).isTrue();

    }

    @Test
    void shouldAddSetLostAction() {
        clientReturn.setStatus(ClientReturnStatus.RECEIVED);
        clientReturn = clientReturnRepository.save(clientReturn);
        var result = mapper.mapClientDeliveryReturn(clientReturn);

        assertThat(result.getDeliveryToClient().getActions()).hasSize(1);
        assertThat(result.getDeliveryToClient().getActions().contains(new OrderDeliveryTaskDto.Action(CANCEL))).isTrue();
    }

    private PartnerClientReturnDeliveryToClient getExpectedDeliveryToClient(LogisticRequestPoint logisticRequestPoint,
                                                                            PartnerClientDto expectedClient,
                                                                            PartnerOrderAddressDto expectedAddressDetails,
                                                                            PartnerDeliveryDetails partnerDeliveryDetails) {
        return PartnerClientReturnDeliveryToClient.builder()
                .address(logisticRequestPoint.getAddress())
                .personalAddressId(logisticRequestPoint.getAddressPersonalId())
                .addressDetails(expectedAddressDetails)
                .partnerDeliveryDetails(partnerDeliveryDetails)
                .client(expectedClient)
                .actions(EXPECTED_EDIT_ACTIONS)
                .build();
    }

    private PartnerOrderAddressDto getExpectedAddressDetails(ClientReturn clientReturn) {
        var logisticRequestPoint = clientReturn.getLogisticRequestPointFrom();
        return PartnerOrderAddressDto.builder()
                .personalAddressId(logisticRequestPoint.getAddressPersonalId())
                .city(logisticRequestPoint.getCity())
                .street(logisticRequestPoint.getStreet())
                .house(logisticRequestPoint.getHouse())
                .entrance(logisticRequestPoint.getEntrance())
                .apartment(logisticRequestPoint.getApartment())
                .floor(Optional.ofNullable(logisticRequestPoint.getFloor()).map(String::valueOf).orElse(null))
                .entryPhone(logisticRequestPoint.getEntryPhone())
                .build();
    }

    private PartnerClientDto getExpectedClient(Client client, LogisticRequestPoint logisticRequestPoint) {
        ClientData clientData = client.getClientData();
        return PartnerClientDto.builder()
                .name(clientData.getFullName())
                .email(clientData.getEmail())
                .phone(clientData.getPhone())
                .personalEmailId(clientData.getPersonalEmailId())
                .personalPhoneId(clientData.getPersonalPhoneId())
                .personalFioId(clientData.getPersonalFioId())
                .notes(logisticRequestPoint.getClientNotes())
                .build();
    }

    private PartnerDeliveryDetails getExpectedDeliveryDetails(ClientReturn clientReturn) {
        var sortingCenter = sortingCenterService.findSortCenterForDs(clientReturn.getDeliveryServiceId());
        var deliveryService = sortingCenterService.findDsById(clientReturn.getDeliveryServiceId());
        var logisticRequestPointFrom = clientReturn.getLogisticRequestPointFrom();
        var deliveryDetails = PartnerDeliveryDetails.builder()
                .intervalFrom(clientReturn.getArriveIntervalFrom())
                .intervalTo(clientReturn.getArriveIntervalTo())
                .longitude(logisticRequestPointFrom.getLongitude())
                .latitude(logisticRequestPointFrom.getLatitude())
                .personalGpsId(logisticRequestPointFrom.getGpsPersonalId())
                .deliveryServiceId(clientReturn.getDeliveryServiceId())
                .deliveryServiceName(deliveryService.getName())
                .sortingCenterId(sortingCenter.getId())
                .sortingCenterName(sortingCenter.getName())
                .build();

        var activeTaskO = userShiftRepository.findOrderDeliveryTasksByClientReturnId(clientReturn.getId())
                .stream()
                .filter(task -> !task.isInTerminalStatus() || task.isPostponedSubtask())
                .findFirst();
        if (activeTaskO.isPresent()) {
            var activeTask = activeTaskO.get();
            var rp = activeTask.getRoutePoint();
            var user = rp.getUserShift().getUser();
            deliveryDetails.setExpectedDeliveryTime(rp.getExpectedDateTime());
            deliveryDetails.setCourierId(user.getId());
            deliveryDetails.setCourierUid(String.valueOf(user.getUid()));
            deliveryDetails.setCourierName(user.getFullName());
            deliveryDetails.setUserShiftId(rp.getUserShift().getId());
            deliveryDetails.setRoutePointId(rp.getId());
        }
        return deliveryDetails;
    }
}
