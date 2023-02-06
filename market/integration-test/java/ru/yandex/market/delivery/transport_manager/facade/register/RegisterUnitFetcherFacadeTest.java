package ru.yandex.market.delivery.transport_manager.facade.register;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TmPropertyKey;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.inbound.abo.TransferRegisterAboProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.inbound.ffwf.ReturnRegisterTaskProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.transfer.ffwf.TransferRegisterFfwfProducer;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationUnitMapper;
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;
import ru.yandex.market.ff.client.dto.RegistryUnitCountDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitCountsInfoDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitDTOContainer;
import ru.yandex.market.ff.client.dto.RegistryUnitIdDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitInfoDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitMetaDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitPartialIdDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitsFilterDTO;
import ru.yandex.market.ff.client.dto.RequestItemDTO;
import ru.yandex.market.ff.client.dto.RequestItemDTOContainer;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.client.enums.RegistryUnitType;
import ru.yandex.market.ff.client.enums.UnitCountType;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DatabaseSetup("/repository/axapta_document_request/empty.xml")
class RegisterUnitFetcherFacadeTest extends AbstractContextualTest {
    @Autowired
    private FulfillmentWorkflowClientApi ffwfClient;
    @Autowired
    private RegisterUnitFetcherFacade facade;
    @Autowired
    private TransferRegisterFfwfProducer transferRegisterFfwfProducer;
    @Autowired
    private TransferRegisterAboProducer transferRegisterAboProducer;
    @Autowired
    private ReturnRegisterTaskProducer returnRegisterTaskProducer;
    @Autowired
    private TransportationUnitMapper unitMapper;

    @Test
    @DatabaseSetup(value = {
        "/repository/facade/register_unit_fetcher_facade/registers.xml",
        "/repository/facade/register_facade/register_links.xml",
    })
    @ExpectedDatabase(
        value = "/repository/facade/register_unit_fetcher_facade/linked_register_units.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/axapta_document_request/empty.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void fetchRegisters() {
        checkFetchRegisters(RegistryUnitType.ITEM);
    }

    @Test
    @DatabaseSetup(value = {
        "/repository/facade/register_unit_fetcher_facade/registers.xml",
        "/repository/facade/register_facade/register_links_xdoc.xml",
    })
    @ExpectedDatabase(
        value = "/repository/axapta_document_request/empty.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void fetchRegistersUnsupportedTransportationType() {
        checkFetchRegisters(RegistryUnitType.BOX);
    }

    private void checkFetchRegisters(RegistryUnitType secondRegistryUnitType) {
        RegistryUnitDTO registryUnit1 = createRegistryUnit(1, RegistryUnitType.PALLET, "1");
        RegistryUnitDTO registryUnit2 = createRegistryUnit(1, secondRegistryUnitType, "2");
        var registryUnitDTOContainer1 = createRegistryUnitContainer(List.of(registryUnit1, registryUnit2));

        var registryUnitDTOContainer2 = createRegistryUnitContainer(
            List.of(
                createRegistryUnit(2, RegistryUnitType.PALLET, "3"),
                createRegistryUnit(2, RegistryUnitType.BOX, "4")
            )
        );

        var filter1 = RegistryUnitsFilterDTO.Builder
            .builder(100L, 19)
            .size(500)
            .page(0)
            .build();

        var filter2 = RegistryUnitsFilterDTO.Builder
            .builder(100L, 26)
            .size(500)
            .page(1)
            .build();

        when(ffwfClient.getRegistryUnits(filter1)).thenReturn(registryUnitDTOContainer1);
        when(ffwfClient.getRegistryUnits(filter2)).thenReturn(registryUnitDTOContainer2);

        facade.fetchAndSaveRegisterUnitsAndScheduleTransferRegister(1L, 100L, TransportationUnitType.OUTBOUND);

        verify(transferRegisterFfwfProducer).produce(1L);
    }

    @Test
    @DatabaseSetup(value = "/repository/facade/register_unit_fetcher_facade/registers.xml")
    @ExpectedDatabase(
        value = "/repository/facade/register_unit_fetcher_facade/registers.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/axapta_document_request/empty.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void fetchRegisters_getRegistryUnitsError() {
        when(ffwfClient.getRegistryUnits(any())).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        assertThrows(RuntimeException.class, () -> facade.fetchAndSaveRegisterUnitsAndScheduleTransferRegister(
            1L,
            100L,
            TransportationUnitType.OUTBOUND
        ));
        verify(transferRegisterFfwfProducer, never()).produce(any());
    }

    @Test
    @DatabaseSetup({
        "/repository/facade/register_unit_fetcher_facade/registers.xml",
        "/repository/facade/register_facade/register_links.xml",
    })
    @ExpectedDatabase(
        value = "/repository/facade/register_unit_fetcher_facade/after_relations_persisted.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/axapta_document_request/empty.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void checkPersistRelations() {
        var pallet = createRegistryUnit(2, RegistryUnitType.PALLET, "3");
        var box = createRegistryUnit(2, RegistryUnitType.BOX, "4");
        var item = createRegistryUnit(2, RegistryUnitType.ITEM, "5");

        item.getUnitInfo().setParentUnitIds(List.of(
            pallet.getUnitInfo().getUnitId(),
            box.getUnitInfo().getUnitId()
        ));

        box.getUnitInfo().setParentUnitIds(List.of(pallet.getUnitInfo().getUnitId()));

        var registryUnitDTOContainer = createRegistryUnitContainer(List.of(pallet, box, item));

        when(ffwfClient.getRegistryUnits(Mockito.any())).thenReturn(registryUnitDTOContainer);

        facade.fetchAndSaveRegisterUnitsAndScheduleTransferRegister(1L, 100L, TransportationUnitType.OUTBOUND);

    }

    @Test
    @DatabaseSetup({
        "/repository/facade/register_unit_fetcher_facade/registers.xml",
        "/repository/facade/register_facade/register_links.xml",
    })
    @ExpectedDatabase(
        value = "/repository/facade/register_unit_fetcher_facade/after_relations_persisted_one_id.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/axapta_document_request/empty.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void checkPersistRelationsWithOneUnitId() {
        var box = createRegistryUnit(2, RegistryUnitType.BOX, "4");
        var item1 = createRegistryUnit(2, RegistryUnitType.ITEM, "5");
        var item2 = createRegistryUnit(2, RegistryUnitType.ITEM, "5");

        item1.getUnitInfo().setParentUnitIds(List.of(
            box.getUnitInfo().getUnitId()
        ));

        item2.getUnitInfo().setParentUnitIds(List.of(
            box.getUnitInfo().getUnitId()
        ));


        var registryUnitDTOContainer = createRegistryUnitContainer(List.of(box, item1, item2));

        when(ffwfClient.getRegistryUnits(Mockito.any())).thenReturn(registryUnitDTOContainer);

        facade.fetchAndSaveRegisterUnitsAndScheduleTransferRegister(1L, 100L, TransportationUnitType.OUTBOUND);

    }

    @Test
    @DatabaseSetup({
        "/repository/facade/register_unit_fetcher_facade/return_undelivered_registers.xml",
        "/repository/facade/register_facade/register_links.xml",
    })
    @ExpectedDatabase(
        value = "/repository/facade/register_unit_fetcher_facade/linked_return_undelivered_register_units.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/axapta_document_request/empty.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void fetchReturnUndeliveredRegistersAndPutToAboQueue() {
        fetchReturnRegisters();
        verify(transferRegisterAboProducer).produce(1L);
    }

    @Test
    @DatabaseSetup({
        "/repository/facade/register_unit_fetcher_facade/return_undelivered_registers.xml",
        "/repository/facade/register_facade/register_links.xml",
    })
    @ExpectedDatabase(
        value = "/repository/facade/register_unit_fetcher_facade/linked_return_undelivered_register_units.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/axapta_document_request/empty.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void fetchReturnUndeliveredRegisters() {
        when(propertyService.getBoolean(TmPropertyKey.PUT_FACT_UNDELIVERED_ORDERS_RETURN_REGISTER_TO_FFWF))
            .thenReturn(true);
        when(propertyService.getList(TmPropertyKey.PARTNERS_SUPPORT_RETURN_REGISTER_TO_FFWF))
                .thenReturn(List.of(6L));
        fetchReturnRegisters();
        verify(returnRegisterTaskProducer).produce(1L);
    }

    @Test
    @DatabaseSetup({
        "/repository/facade/register_unit_fetcher_facade/return_delivered_registers.xml",
        "/repository/facade/register_facade/register_links.xml",
    })
    @ExpectedDatabase(
        value = "/repository/facade/register_unit_fetcher_facade/linked_return_delivered_register_units.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/axapta_document_request/empty.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void fetchReturnDeliveredRegistersAndPutToAboQueue() {
        fetchReturnRegisters();
        verify(transferRegisterAboProducer).produce(1L);
    }

    @Test
    @DatabaseSetup({
        "/repository/facade/register_unit_fetcher_facade/return_delivered_registers.xml",
        "/repository/facade/register_facade/register_links.xml",
    })
    @ExpectedDatabase(
        value = "/repository/facade/register_unit_fetcher_facade/linked_return_delivered_register_units.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/axapta_document_request/empty.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void fetchReturnDeliveredRegisters() {
        when(propertyService.getBoolean(TmPropertyKey.PUT_FACT_DELIVERED_ORDERS_RETURN_REGISTER_TO_FFWF))
            .thenReturn(true);
        when(propertyService.getList(TmPropertyKey.PARTNERS_SUPPORT_RETURN_REGISTER_TO_FFWF))
            .thenReturn(List.of(6L));
        fetchReturnRegisters();
        verify(returnRegisterTaskProducer).produce(1L);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation_unit/transportation_with_plan_inbound_register_units.xml",
        "/repository/transportation/xdoc_transport.xml"
    })
    @DatabaseSetup(value = "/repository/register/update/add_ffwf_id.xml", type = DatabaseOperation.UPDATE)
    @ExpectedDatabase(
        value = "/repository/facade/register_unit_fetcher_facade/prepared_register_unit_fetch.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/axapta_document_request/expected/new_request.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void fetchPreparedRegisters() {
        var registryUnitDTOContainer = createRegistryUnitContainer(
            List.of(
                createRegistryUnit(13L, RegistryUnitType.PALLET, "1", "abc")
            )
        );
        var filter = RegistryUnitsFilterDTO.Builder
            .builder(100L, 13L)
            .size(500)
            .page(0)
            .build();

        when(ffwfClient.getRegistryUnits(filter)).thenReturn(registryUnitDTOContainer);

        facade.fetchAndSaveRegisterUnitsAndScheduleTransferRegister(13L, 100L, TransportationUnitType.OUTBOUND);
    }


    @Test
    @DatabaseSetup("/repository/transportation/xdoc_to_ff_transportations_break_bulk_xdock.xml")
    @ExpectedDatabase(
        value = "/repository/facade/register_unit_fetcher_facade/fact_register_unit_fetch_break_bulk_xdock.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void fetchFactRegistersBreakBulkXDock() {
        var registryUnitDTOContainer = createRegistryUnitContainer(
            List.of(
                createRegistryUnit(33L, RegistryUnitType.PALLET, "1", "abc_pallet"),
                createRegistryUnit(33L, RegistryUnitType.BOX, "1", "abc_box"),
                createRegistryUnit(33L, RegistryUnitType.ITEM, "1", "abc_item")
            )
        );
        var filter = RegistryUnitsFilterDTO.Builder
            .builder(33L, 33L)
            .size(500)
            .page(0)
            .build();

        when(ffwfClient.getRegistryUnits(filter)).thenReturn(registryUnitDTOContainer);

        facade.fetchAndSaveRegisterUnitsAndScheduleTransferRegister(33L, 33L, TransportationUnitType.OUTBOUND);
    }

    @Test
    @DatabaseSetup(value = {
        "/repository/facade/register_unit_fetcher_facade/old_registers.xml",
        "/repository/facade/register_facade/transportation_units_registers.xml",
    })
    @ExpectedDatabase(
        value = "/repository/facade/register_unit_fetcher_facade/registers_with_deleted_units.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void fetchNewRegistryUnitsToOldRegister() {
        RegistryUnitDTO registryUnit1 = createRegistryUnit(1, RegistryUnitType.PALLET, "1");
        RegistryUnitDTO registryUnit2 = createRegistryUnit(1, RegistryUnitType.ITEM, "2");
        var registryUnitDTOContainer1 = createRegistryUnitContainer(List.of(registryUnit1, registryUnit2));

        var filter1 = RegistryUnitsFilterDTO.Builder
            .builder(100L, 19)
            .size(500)
            .page(0)
            .build();

        when(ffwfClient.getRegistryUnits(filter1)).thenReturn(registryUnitDTOContainer1);

        facade.fetchAndSaveRegisterUnitsAndScheduleTransferRegister(1L, 100L, TransportationUnitType.OUTBOUND);

        verify(transferRegisterFfwfProducer).produce(1L);
    }

    @Test
    @DatabaseSetup(value = {
        "/repository/facade/register_unit_fetcher_facade/registers.xml",
        "/repository/facade/register_facade/register_links.xml",
    })
    @ExpectedDatabase(
        value = "/repository/facade/register_unit_fetcher_facade/after_full_item_with_meta.xml",
        assertionMode = NON_STRICT
    )
    @ExpectedDatabase(
        value = "/repository/axapta_document_request/empty.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void fetchRegisterOfFullItemWithMeta() {
        RegistryUnitDTO item = createFullItem();
        var registryUnitDTOContainer = createRegistryUnitContainer(List.of(item));

        var filter = RegistryUnitsFilterDTO.Builder
            .builder(100L, 19)
            .size(500)
            .page(0)
            .build();

        when(ffwfClient.getRegistryUnits(filter)).thenReturn(registryUnitDTOContainer);

        facade.fetchAndSaveRegisterUnitsAndScheduleTransferRegister(1L, 100L, TransportationUnitType.OUTBOUND);
    }

    @DatabaseSetup(value = "/repository/facade/register_unit_fetcher_facade/fetch_registers_old.xml")
    @ExpectedDatabase(
        value = "/repository/facade/register_unit_fetcher_facade/after_fetch_inbound_registers_old.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/axapta_document_request/empty.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Test
    void fetchAndSaveInboundRegistersOldPlan() {
        RequestItemDTOContainer container = registerItemsOld();
        Mockito.when(ffwfClient.getRequestItems(Mockito.any()))
            .thenReturn(container);

        facade.fetchAndSaveRegistersOld(unitMapper.getById(3L), false, 123L);
    }

    @DatabaseSetup(value = "/repository/facade/register_unit_fetcher_facade/fetch_registers_old.xml")
    @ExpectedDatabase(
        value = "/repository/facade/register_unit_fetcher_facade/after_fetch_outbound_registers_old.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/axapta_document_request/empty.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Test
    void fetchAndSaveOutboundRegistersOldFact() {
        RequestItemDTOContainer container = registerItemsOld();
        Mockito.when(ffwfClient.getRequestItems(Mockito.any()))
            .thenReturn(container);

        facade.fetchAndSaveRegistersOld(unitMapper.getById(2L), true, 123L);
    }


    @Test
    @DatabaseSetup(value = {
        "/repository/facade/register_unit_fetcher_facade/registers.xml",
        "/repository/facade/register_facade/register_links.xml",
    })
    void testFailFetchingInIllegalStatus() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> facade.fetchAndSaveRegisterUnitsAndScheduleTransferRegister(
                2L,
                100L,
                TransportationUnitType.OUTBOUND
            )
        );
    }

    private RequestItemDTOContainer registerItemsOld() {
        RequestItemDTOContainer container = new RequestItemDTOContainer(1, 0, 1);
        RegistryUnitIdDTO unitId = new RegistryUnitIdDTO();
        unitId.setParts(Set.of(
            new RegistryUnitPartialIdDTO(RegistryUnitIdType.SHOP_SKU, "1234")
        ));
        RequestItemDTO item = new RequestItemDTO();
        item.setCount(10);
        item.setFactCount(1);
        item.setUnitId(unitId);
        item.setSupplyPrice(BigDecimal.TEN);
        item.setRealSupplierId("12345");
        item.setRealSupplierName("ИП Петров");
        item.setCargoTypes(List.of(520, 410));
        container.getItems().add(item);
        return container;
    }

    private void fetchReturnRegisters() {
        var registryUnitDTOContainer = createRegistryUnitContainer(
            List.of(
                createRegistryUnit(26L, RegistryUnitType.PALLET, "1"),
                createRegistryUnit(26L, RegistryUnitType.ITEM, "2"),
                createRegistryUnit(26L, RegistryUnitType.BOX, "3")
            )
        );
        var filter = RegistryUnitsFilterDTO.Builder
            .builder(100L, 26L)
            .size(500)
            .page(0)
            .build();

        when(ffwfClient.getRegistryUnits(filter)).thenReturn(registryUnitDTOContainer);

        facade.fetchAndSaveRegisterUnitsAndScheduleTransferRegister(1L, 100L, TransportationUnitType.OUTBOUND);
    }

    private RegistryUnitDTOContainer createRegistryUnitContainer(List<RegistryUnitDTO> registryUnitDtos) {
        var registryUnitDTOContainer = new RegistryUnitDTOContainer();
        for (RegistryUnitDTO registryUnitDto : registryUnitDtos) {
            registryUnitDTOContainer.addUnit(registryUnitDto);
        }
        registryUnitDTOContainer.setPageNumber(0);
        registryUnitDTOContainer.setTotalPages(1);
        return registryUnitDTOContainer;
    }


    private RegistryUnitDTO createRegistryUnit(
        long registryId,
        RegistryUnitType type,
        String cis
    ) {
        var unitId = new RegistryUnitIdDTO(Set.of(new RegistryUnitPartialIdDTO(
            RegistryUnitIdType.CIS,
            cis
        )));

        var unitInfo = new RegistryUnitInfoDTO();
        unitInfo.setUnitId(unitId);
        unitInfo.setParentUnitIds(List.of());

        var unit = new RegistryUnitDTO();
        unit.setRegistryId(registryId);
        unit.setType(type);
        unit.setUnitInfo(unitInfo);

        return unit;
    }

    private RegistryUnitDTO createRegistryUnit(
        long registryId,
        RegistryUnitType type,
        String cis,
        String barcode
    ) {
        var unitId = new RegistryUnitIdDTO(
            Stream.of(new RegistryUnitPartialIdDTO(
                    RegistryUnitIdType.CIS,
                    cis
                ), new RegistryUnitPartialIdDTO(
                    switch (type) {
                        case BOX -> RegistryUnitIdType.BOX_ID;
                        case PALLET -> RegistryUnitIdType.PALLET_ID;
                        default -> null;
                    },
                    barcode
                ))
                .filter(id -> id.getType() != null)
                .collect(Collectors.toSet())
        );

        var unitInfo = new RegistryUnitInfoDTO();
        unitInfo.setUnitId(unitId);
        unitInfo.setParentUnitIds(List.of());

        var unit = new RegistryUnitDTO();
        unit.setRegistryId(registryId);
        unit.setType(type);
        unit.setUnitInfo(unitInfo);

        return unit;
    }

    private RegistryUnitDTO createFullItem() {
        RegistryUnitDTO dto = createRegistryUnit(1, RegistryUnitType.ITEM, "1");

        dto.getUnitInfo().getUnitId().setParts(
            Set.of(
                new RegistryUnitPartialIdDTO(RegistryUnitIdType.SHOP_SKU, "article1"),
                new RegistryUnitPartialIdDTO(RegistryUnitIdType.VENDOR_ID, "vendor1"),
                new RegistryUnitPartialIdDTO(RegistryUnitIdType.CONSIGNMENT_ID, "cons1")
            )
        );

        RegistryUnitIdDTO unitId = new RegistryUnitIdDTO(
            Set.of(
                new RegistryUnitPartialIdDTO(RegistryUnitIdType.UIT, "uit"),
                new RegistryUnitPartialIdDTO(RegistryUnitIdType.SERIAL_NUMBER, "ser_num"),
                new RegistryUnitPartialIdDTO(RegistryUnitIdType.IMEI, "imei")
            )
        );
        RegistryUnitCountsInfoDTO counts = new RegistryUnitCountsInfoDTO();
        counts.setUnitCounts(
            List.of(
                new RegistryUnitCountDTO(UnitCountType.DEFECT, 2, List.of(unitId))
            )
        );

        RegistryUnitMetaDTO meta = new RegistryUnitMetaDTO();
        meta.setBarcodes(List.of("barcode1", "barcode2"));
        meta.setCargoTypes(List.of(100, 200, 300, 0));
        meta.setName("Item name");
        meta.setSupplyPrice(BigDecimal.TEN);
        meta.setBoxCount(1);
        meta.setComment("comment");
        meta.setManufacturedDate(OffsetDateTime.parse("2021-05-19T12:00+03:00"));
        meta.setExpirationDate(OffsetDateTime.parse("2021-06-16T12:00+03:00"));
        meta.setLifeTime(100);
        meta.setHasLifeTime(true);
        meta.setContractorId("123");
        meta.setContractorName("contractor");
        meta.setOutboundRemainingLifetimeDays(1);
        meta.setOutboundRemainingLifetimePercentage(10);
        meta.setInboundRemainingLifetimeDays(2);
        meta.setInboundRemainingLifetimePercentage(20);
        meta.setShelfLifeTemplate("ShelfLifeTemplate");
        meta.setSurplusAllowed(true);
        meta.setCisHandleMode(0);
        meta.setCheckImei(100);
        meta.setImeiMask("100");
        meta.setCheckSn(101);
        meta.setSnMask("101");

        dto.getUnitInfo().setUnitCountsInfo(counts);
        dto.setMeta(meta);
        return dto;
    }
}
