package ru.yandex.market.delivery.transport_manager.service.external.abo;

import java.util.Collections;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.abo.api.client.AboAPI;
import ru.yandex.market.abo.api.entity.resupply.registry.UploadRegistryRequest;
import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.Register;
import ru.yandex.market.delivery.transport_manager.facade.register.RegisterFacade;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.register.RegisterMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.register.RegisterUnitMapper;
import ru.yandex.market.delivery.transport_manager.service.register.RegisterService;
import ru.yandex.market.tpl.common.lrm.client.api.ReturnsApi;
import ru.yandex.market.tpl.common.lrm.client.model.ReturnBox;
import ru.yandex.market.tpl.common.lrm.client.model.SearchReturn;
import ru.yandex.market.tpl.common.lrm.client.model.SearchReturnsResponse;

import static org.mockito.ArgumentMatchers.any;

class AboInboundRegisterSenderTest extends AbstractContextualTest {
    @Autowired
    AboInboundRegisterSender sender;
    @Autowired
    TransportationMapper transportationMapper;
    @Autowired
    RegisterService registerService;
    @Autowired
    RegisterMapper registerMapper;
    @Autowired
    RegisterUnitMapper mapper;
    @Autowired
    RegisterFacade facade;
    @Autowired
    ReturnsApi service;
    @Autowired
    AboAPI aboAPI;

    ArgumentCaptor<UploadRegistryRequest> registryRequestCaptor = ArgumentCaptor.forClass(UploadRegistryRequest.class);

    @Test
    @DatabaseSetup("/repository/register/undelivered_register.xml")
    @DatabaseSetup("/repository/register_unit/register_unit_with_missing_order_id.xml")
    @ExpectedDatabase(value = "/repository/register_unit/after/register_unit_after_lrm_enrichment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void checkUpdatedAfterSend() throws InvalidAboRegisterException {
        Register inbRegister = registerService.getByRegisterIdWithUnits(2L);

        SearchReturnsResponse searchReturnsResponse = new SearchReturnsResponse();
        SearchReturn searchReturn = new SearchReturn();
        ReturnBox returnBox = new ReturnBox();
        returnBox.setExternalId("1");
        searchReturn.addBoxesItem(returnBox);
        searchReturn.setOrderExternalId("-1");
        searchReturnsResponse.setReturns(Collections.singletonList(searchReturn));
        Mockito.when(service.searchReturns(any())).thenReturn(searchReturnsResponse);

        sender.send(1L, inbRegister, Optional.empty());

        Mockito.verify(aboAPI).uploadRegistry(registryRequestCaptor.capture());
        Assertions.assertEquals(
                registryRequestCaptor.getValue().getRegistryPositions().get(0).getOrderId(), "-1"
        );
    }

    @Test
    @DatabaseSetup("/repository/register/undelivered_register.xml")
    @DatabaseSetup("/repository/register_unit/register_unit_with_missing_order_id.xml")
    @ExpectedDatabase(value = "/repository/register_unit/after/register_unit_after_lrm_enrichment.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void checkTransportationUnitProvided() throws InvalidAboRegisterException {
        Register inbRegister = registerService.getByRegisterIdWithUnits(2L);

        SearchReturnsResponse searchReturnsResponse = new SearchReturnsResponse();
        SearchReturn searchReturn = new SearchReturn();
        ReturnBox returnBox = new ReturnBox();
        returnBox.setExternalId("1");
        searchReturn.addBoxesItem(returnBox);
        searchReturn.setOrderExternalId("-1");
        searchReturnsResponse.setReturns(Collections.singletonList(searchReturn));
        Mockito.when(service.searchReturns(any())).thenReturn(searchReturnsResponse);
        TransportationUnit transportationUnit = new TransportationUnit();
        transportationUnit.setLogisticPointId(999L);

        sender.send(1L, inbRegister, Optional.of(transportationUnit));

        Mockito.verify(aboAPI).uploadRegistry(registryRequestCaptor.capture());
        Assertions.assertEquals(999L,
                registryRequestCaptor.getValue().getRegistryDetails().getLogisticPointId());
    }

    @Test
    @DatabaseSetup("/repository/register/undelivered_failed_register.xml")
    @DatabaseSetup("/repository/register_unit/register_unit_with_missing_order_id.xml")
    @ExpectedDatabase(value = "/repository/register/undelivered_failed_register_resent.xml", assertionMode =
        DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void checkErrorSuppressedIfAccepted() throws InvalidAboRegisterException {
        Register inbRegister = registerService.getByRegisterIdWithUnits(2L);

        SearchReturnsResponse searchReturnsResponse = new SearchReturnsResponse();
        SearchReturn searchReturn = new SearchReturn();
        ReturnBox returnBox = new ReturnBox();
        returnBox.setExternalId("1");
        searchReturn.addBoxesItem(returnBox);
        searchReturn.setOrderExternalId("-1");
        searchReturnsResponse.setReturns(Collections.singletonList(searchReturn));
        Mockito.when(service.searchReturns(any())).thenReturn(searchReturnsResponse);
        Mockito.doThrow(new HttpClientErrorException(HttpStatus.CONFLICT)).when(aboAPI).uploadRegistry(any());

        sender.send(1L, inbRegister, Optional.empty());

        Mockito.verify(aboAPI).uploadRegistry(registryRequestCaptor.capture());
    }

    @Test
    @DatabaseSetup("/repository/register/undelivered_register.xml")
    @DatabaseSetup("/repository/register_unit/register_unit_with_missing_order_id.xml")
    void checkErrorNotSuppressedIfNotAccepted() {
        Register inbRegister = registerService.getByRegisterIdWithUnits(2L);

        SearchReturnsResponse searchReturnsResponse = new SearchReturnsResponse();
        SearchReturn searchReturn = new SearchReturn();
        ReturnBox returnBox = new ReturnBox();
        returnBox.setExternalId("1");
        searchReturn.addBoxesItem(returnBox);
        searchReturn.setOrderExternalId("-1");
        searchReturnsResponse.setReturns(Collections.singletonList(searchReturn));
        Mockito.when(service.searchReturns(any())).thenReturn(searchReturnsResponse);
        Mockito.doThrow(HttpClientErrorException.class).when(aboAPI).uploadRegistry(any());

        softly.assertThatThrownBy(() -> sender.send(1L, inbRegister, Optional.empty()))
                .isInstanceOf(InvalidAboRegisterException.class);
    }

    @Test
    @DatabaseSetup("/repository/register/undelivered_register.xml")
    @DatabaseSetup("/repository/register_unit/register_unit_with_correct_box.xml")
    @ExpectedDatabase(value = "/repository/register_unit/register_unit_with_correct_box.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void checkNotUpdatedAfterSend() throws InvalidAboRegisterException {
        Register inbRegister = registerService.getByRegisterIdWithUnits(2L);
        SearchReturnsResponse searchReturnsResponse = new SearchReturnsResponse();
        SearchReturn searchReturn = new SearchReturn();
        ReturnBox returnBox = new ReturnBox();
        returnBox.setExternalId("1");
        searchReturn.addBoxesItem(returnBox);
        searchReturn.setOrderExternalId("-1");
        searchReturnsResponse.setReturns(Collections.singletonList(searchReturn));
        Mockito.when(service.searchReturns(any())).thenReturn(searchReturnsResponse);

        sender.send(1L, inbRegister, Optional.empty());

        Mockito.verify(aboAPI).uploadRegistry(registryRequestCaptor.capture());
        Assertions.assertEquals("1", registryRequestCaptor.getValue().getRegistryPositions().get(0).getOrderId());
    }

    @Test
    @DatabaseSetup("/repository/register/delivered_register.xml")
    @DatabaseSetup("/repository/register_unit/register_unit_with_missing_order_id.xml")
    @ExpectedDatabase(value = "/repository/register_unit/register_unit_with_missing_order_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void shouldIgnoreOtherTypes() throws InvalidAboRegisterException {
        Register inbRegister = registerService.getByRegisterIdWithUnits(2L);
        SearchReturnsResponse searchReturnsResponse = new SearchReturnsResponse();
        SearchReturn searchReturn = new SearchReturn();
        ReturnBox returnBox = new ReturnBox();
        returnBox.setExternalId("1");
        searchReturn.addBoxesItem(returnBox);
        searchReturn.setOrderExternalId("-1");
        searchReturnsResponse.setReturns(Collections.singletonList(searchReturn));
        Mockito.when(service.searchReturns(any())).thenReturn(searchReturnsResponse);
        sender.send(1L, inbRegister, Optional.empty());
        Mockito.verify(aboAPI).uploadRegistry(registryRequestCaptor.capture());
        Assertions.assertEquals("1", registryRequestCaptor.getValue().getRegistryPositions().get(0).getOrderId());
    }
}
