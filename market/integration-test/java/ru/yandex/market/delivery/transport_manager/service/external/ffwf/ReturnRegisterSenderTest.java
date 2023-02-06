package ru.yandex.market.delivery.transport_manager.service.external.ffwf;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.Register;
import ru.yandex.market.delivery.transport_manager.service.register.RegisterService;
import ru.yandex.market.ff.client.FulfillmentWorkflowReturnRegistryClientApi;
import ru.yandex.market.ff.client.dto.PutSupplyRequestWithInboundRegisterDTO;
import ru.yandex.market.logistic.gateway.common.model.common.PartialIdType;
import ru.yandex.market.logistic.gateway.common.model.common.RegistryBox;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ReturnRegisterSenderTest extends AbstractContextualTest {
    @Autowired
    ReturnRegisterSender sender;
    @Autowired
    RegisterService registerService;
    @Autowired
    FulfillmentWorkflowReturnRegistryClientApi ffwfApi;
    @Autowired
    LMSClient lmsClient;

    ArgumentCaptor<PutSupplyRequestWithInboundRegisterDTO> registryRequestCaptor =
        ArgumentCaptor.forClass(PutSupplyRequestWithInboundRegisterDTO.class);

    @BeforeEach
    void initMocks() {
        when(lmsClient.getLogisticsPoints(any())).thenReturn(
            List.of(LogisticsPointResponse.newBuilder().id(123L).build()));
    }

    @Test
    @DatabaseSetup("/repository/register/undelivered_register.xml")
    @DatabaseSetup("/repository/register_unit/register_unit_with_missing_order_id.xml")
    @ExpectedDatabase(value = "/repository/register_unit/register_unit_with_missing_order_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void checkNotUpdatedIfOrderIdMissing() throws ReturnRegistryException {
        Register inbRegister = registerService.getByRegisterIdWithUnits(2L);

        sender.send(createTransportation(), inbRegister);

        Mockito.verify(ffwfApi).createRequestAndPutRegistry(registryRequestCaptor.capture());
        Assertions.assertEquals(123L, registryRequestCaptor.getValue().getLogisticsPointId());
    }

    @Test
    @DatabaseSetup("/repository/register/undelivered_register.xml")
    @DatabaseSetup("/repository/register_unit/register_unit_with_missing_order_id.xml")
    @ExpectedDatabase(value = "/repository/register_unit/register_unit_with_missing_order_id.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void checkTransportationUnitProvided() throws ReturnRegistryException {
        Register inbRegister = registerService.getByRegisterIdWithUnits(2L);

        TransportationUnit transportationUnit = new TransportationUnit();
        transportationUnit.setLogisticPointId(999L);

        sender.send(createTransportation(transportationUnit), inbRegister);

        Mockito.verify(ffwfApi).createRequestAndPutRegistry(registryRequestCaptor.capture());
        Assertions.assertEquals(999L, registryRequestCaptor.getValue().getLogisticsPointId());
    }

    @Test
    @DatabaseSetup("/repository/register/undelivered_register.xml")
    @DatabaseSetup("/repository/register_unit/register_unit_with_missing_order_id.xml")
    void checkErrorNotSuppressedIfNotAccepted() {
        Register inbRegister = registerService.getByRegisterIdWithUnits(2L);

        Mockito.doThrow(HttpClientErrorException.class).when(ffwfApi).createRequestAndPutRegistry(any());

        softly.assertThatThrownBy(() -> sender.send(createTransportation(), inbRegister))
                .isInstanceOf(ReturnRegistryException.class);
    }

    @Test
    @DatabaseSetup("/repository/register/undelivered_register.xml")
    @DatabaseSetup("/repository/register_unit/register_unit_with_correct_box.xml")
    @ExpectedDatabase(value = "/repository/register_unit/register_unit_with_correct_box.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void checkNotUpdatedIfOrderIdPresent() throws ReturnRegistryException {
        Register inbRegister = registerService.getByRegisterIdWithUnits(2L);

        sender.send(createTransportation(), inbRegister);

        Mockito.verify(ffwfApi).createRequestAndPutRegistry(registryRequestCaptor.capture());
        List<RegistryBox> boxes = registryRequestCaptor.getValue().getInboundRegistry().getBoxes();
        Assertions.assertEquals(
            "1", getPartialIdValue(boxes.get(0), PartialIdType.ORDER_ID)
        );
    }

    @Test
    @DatabaseSetup("/repository/register/delivered_register.xml")
    @DatabaseSetup("/repository/register_unit/register_unit_with_missing_order_id.xml")
    @ExpectedDatabase(value = "/repository/register_unit/register_unit_with_missing_order_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void checkNotUpdatedForDelivered() throws ReturnRegistryException {
        Register inbRegister = registerService.getByRegisterIdWithUnits(2L);
        sender.send(createTransportation(), inbRegister);
        Mockito.verify(ffwfApi).createRequestAndPutRegistry(registryRequestCaptor.capture());
        List<RegistryBox> boxes = registryRequestCaptor.getValue().getInboundRegistry().getBoxes();
        Assertions.assertNull(getPartialIdValue(boxes.get(0), PartialIdType.ORDER_ID));
    }

    private String getPartialIdValue(RegistryBox registryBox, PartialIdType idType) {
        return registryBox
            .getUnitInfo().getCompositeId().getPartialIds().stream()
            .filter(id -> id.getIdType().equals(idType)).findFirst()
            .map(ru.yandex.market.logistic.gateway.common.model.common.PartialId::getValue).orElse(null);
    }

    private Transportation createTransportation() {
        return createTransportation(null);
    }
    private Transportation createTransportation(TransportationUnit transportationUnit) {
        return new Transportation()
                .setInboundUnit(new TransportationUnit().setPartnerId(1L))
                .setOutboundUnit(transportationUnit);
    }

}
