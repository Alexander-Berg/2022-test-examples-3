package ru.yandex.market.delivery.transport_manager.facade.register;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.FetchRegisterUnitsProducer;
import ru.yandex.market.delivery.transport_manager.service.register.RegisterService;
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;
import ru.yandex.market.ff.client.dto.RegistryDTO;
import ru.yandex.market.ff.client.dto.RegistryDTOContainer;
import ru.yandex.market.ff.client.enums.RegistryFlowType;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RegisterFetcherFacadeTest extends AbstractContextualTest {
    @Autowired
    private FulfillmentWorkflowClientApi ffwfClient;
    @Autowired
    private RegisterFetcherFacade facade;
    @Autowired
    private FetchRegisterUnitsProducer fetchRegisterUnitsProducer;
    @Autowired
    private RegisterService registerService;

    @Test
    @DatabaseSetup(value = "/repository/facade/register_fetcher_facade/transportation_unit.xml")
    @ExpectedDatabase(
        value = "/repository/facade/register_fetcher_facade/linked_registers.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void fetchRegisters() {
        var registryDTOContainer = new RegistryDTOContainer();
        RegistryDTO registryDTO1 = createRegistry(19);
        RegistryDTO registryDTO2 = createRegistry(26, RegistryFlowType.FACT, "externalId2");

        registryDTOContainer.setRegistries(List.of(
            registryDTO1,
            registryDTO2,
            createRegistry(34, RegistryFlowType.PLAN, "externalId")) // PLAN registry should be ignored
        );

        when(ffwfClient.getRegistries(100L)).thenReturn(registryDTOContainer);

        facade.fetchAndSaveRegisters(
            100L,
            42L,
            TransportationUnitType.INBOUND,
            10L
        );

        verify(fetchRegisterUnitsProducer)
            .produce(
                eq(1L),
                eq(100L),
                eq(TransportationUnitType.INBOUND)
            );

        verify(fetchRegisterUnitsProducer)
            .produce(
                eq(2L),
                eq(100L),
                eq(TransportationUnitType.INBOUND)
            );
    }

    @Test
    @DatabaseSetup(value = "/repository/facade/register_fetcher_facade/linked_registers.xml")
    @ExpectedDatabase(
            value = "/repository/facade/register_fetcher_facade/linked_registers.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void fetchRegisters_doNotCreateRegisterIfItAlreadyExists() {
        var registryDTOContainer = new RegistryDTOContainer();
        RegistryDTO registryDTO1 = createRegistry(19, "externalId");
        RegistryDTO registryDTO2 = createRegistry(26, "externalId2");

        registryDTOContainer.setRegistries(List.of(
                registryDTO1,
                registryDTO2,
                createRegistry(34, RegistryFlowType.PLAN, "externalId")) // PLAN registry should be ignored
        );

        when(ffwfClient.getRegistries(100L)).thenReturn(registryDTOContainer);
        facade.fetchAndSaveRegisters(
            100L,
            42L,
            TransportationUnitType.INBOUND,
            10L
        );

        verify(registerService, never()).create(any());
    }

    @Test
    @DatabaseSetup(value = "/repository/facade/register_fetcher_facade/create_one_register.xml")
    @ExpectedDatabase(
            value = "/repository/facade/register_fetcher_facade/expected_created_registers.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void fetchRegisters_createRegisterAndConnectWithAlreadyCreated() {
        var registryDTOContainer = new RegistryDTOContainer();
        RegistryDTO registryDTO1 = createRegistry(19);
        RegistryDTO registryDTO2 = createRegistry(26, RegistryFlowType.FACT, "externalId2");
        RegistryDTO registryDTO3 = createRegistry(34, RegistryFlowType.PLAN, "externalId3"); // ignored

        registryDTOContainer.setRegistries(List.of(registryDTO1, registryDTO2, registryDTO3));

        when(ffwfClient.getRegistries(100L)).thenReturn(registryDTOContainer);

        facade.fetchAndSaveRegisters(
            100L,
            42L,
            TransportationUnitType.INBOUND,
            10L
        );

        verify(fetchRegisterUnitsProducer).produce(
                eq(2L),
                eq(100L),
                eq(TransportationUnitType.INBOUND)
        );
        verify(fetchRegisterUnitsProducer).produce(
                eq(1L),
                eq(100L),
                eq(TransportationUnitType.INBOUND)
        );
    }

    @Test
    @DatabaseSetup(value = "/repository/facade/register_fetcher_facade/create_one_register.xml")
    void fetchRegisters_createRegisterAndConnectWithAlreadyCreated_without_force() {
        var registryDTOContainer = new RegistryDTOContainer();
        RegistryDTO registryDTO1 = createRegistry(19);
        RegistryDTO registryDTO2 = createRegistry(26, RegistryFlowType.FACT, "externalId2");
        RegistryDTO registryDTO3 = createRegistry(34, RegistryFlowType.PLAN, "externalId3"); // ignored

        registryDTOContainer.setRegistries(List.of(registryDTO1, registryDTO2, registryDTO3));

        when(ffwfClient.getRegistries(100L)).thenReturn(registryDTOContainer);

        facade.fetchAndSaveRegisters(
            100L,
            42L,
            TransportationUnitType.INBOUND,
            10L
        );

        verify(fetchRegisterUnitsProducer, times(0)).produce(
                eq(2L),
                eq(100L),
                eq(TransportationUnitType.OUTBOUND)
        );
        verify(fetchRegisterUnitsProducer).produce(
                eq(1L),
                eq(100L),
                eq(TransportationUnitType.INBOUND)
        );
    }

    @Test
    void fetchRegisters_getRegistriesError() {
        when(ffwfClient.getRegistries(100L)).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
        assertThrows(RuntimeException.class, () -> facade.fetchAndSaveRegisters(
            100L,
            42L,
            TransportationUnitType.OUTBOUND,
            10L
        ));
        verify(fetchRegisterUnitsProducer, never()).produce(anyLong(), anyLong(), any());
    }

    private RegistryDTO createRegistry(long id) {
        return createRegistry(id, RegistryFlowType.FACT, "externalId");
    }

    private RegistryDTO createRegistry(long id, String partnerId) {
        return createRegistry(id, RegistryFlowType.FACT, partnerId);
    }

    private RegistryDTO createRegistry(long id, RegistryFlowType flowType, String partnerId) {
        var registryDTO = new RegistryDTO();
        registryDTO.setId(id);
        registryDTO.setType(flowType);
        registryDTO.setPartnerDate(OffsetDateTime.parse("2020-10-22T13:17:29Z"));
        registryDTO.setComment("comment " + id);
        registryDTO.setRequestId(100L);
        registryDTO.setPartnerId(partnerId);
        registryDTO.setDocumentId("doc-id");
        registryDTO.setCreatedAt(LocalDateTime.of(2020, 10, 21, 11, 11, 11));

        return registryDTO;
    }
}
