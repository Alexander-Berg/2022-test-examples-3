package ru.yandex.market.delivery.transport_manager.facade.register;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.RegisterUnit;
import ru.yandex.market.delivery.transport_manager.domain.enums.UnitType;
import ru.yandex.market.delivery.transport_manager.service.core.TmEventPublisher;
import ru.yandex.market.delivery.transport_manager.service.register.RegisterService;
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;
import ru.yandex.market.ff.client.dto.RegistryUnitDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitDTOContainer;
import ru.yandex.market.ff.client.dto.RegistryUnitIdDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitInfoDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitPartialIdDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitsFilterDTO;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.client.enums.RegistryUnitType;

class RegisterUnitFetcherFacadePersistUnitsTest extends AbstractContextualTest {
    public static final long REQUEST_ID = 1000L;
    public static final long REGISTER_ID = 2L;
    public static final long FFWF_REGISTER_ID = 3L;

    private final RegistryUnitIdDTO pallet1Id;
    private final RegistryUnitIdDTO pallet2Id;
    private final RegistryUnitIdDTO box5Id;
    private final RegistryUnitIdDTO box6Id;
    private final RegistryUnitIdDTO itemId;

    private final RegisterUnit pallet1;
    private final RegisterUnit pallet2;
    private final RegisterUnit item3;
    private final RegisterUnit item4;
    private final RegisterUnit box5;
    private final RegisterUnit box6;

    private final RegistryUnitDTO pallet1Dto;
    private final RegistryUnitDTO pallet2Dto;
    private final RegistryUnitDTO item3Dto;
    private final RegistryUnitDTO item4Dto;
    private final RegistryUnitDTO box5Dto;
    private final RegistryUnitDTO box6Dto;

    @Autowired
    private RegisterUnitFetcherFacade facade;

    @Autowired
    private TmEventPublisher applicationEventPublisher;

    @Autowired
    private FulfillmentWorkflowClientApi ffwfClient;

    @Autowired
    private RegisterService registerService;

    RegisterUnitFetcherFacadePersistUnitsTest() {
        pallet1Id = new RegistryUnitIdDTO();
        pallet1Id.setParts(Set.of(new RegistryUnitPartialIdDTO(RegistryUnitIdType.PALLET_ID, "1")));
        pallet2Id = new RegistryUnitIdDTO();
        pallet2Id.setParts(Set.of(new RegistryUnitPartialIdDTO(RegistryUnitIdType.PALLET_ID, "2")));
        // Одинаковый ID для двух item-ов в разных коробках
        itemId = new RegistryUnitIdDTO();
        itemId.setParts(Set.of(
            new RegistryUnitPartialIdDTO(RegistryUnitIdType.SHOP_SKU, "3"),
            new RegistryUnitPartialIdDTO(RegistryUnitIdType.VENDOR_ID, "3")
        ));
        box5Id = new RegistryUnitIdDTO();
        box5Id.setParts(Set.of(new RegistryUnitPartialIdDTO(RegistryUnitIdType.PALLET_ID, "5")));
        box6Id = new RegistryUnitIdDTO();
        box6Id.setParts(Set.of(new RegistryUnitPartialIdDTO(RegistryUnitIdType.PALLET_ID, "6")));

        RegistryUnitInfoDTO pallet1UnitInfo = new RegistryUnitInfoDTO();
        pallet1UnitInfo.setUnitId(pallet1Id);
        pallet1Dto = new RegistryUnitDTO();
        pallet1Dto.setUnitInfo(pallet1UnitInfo);
        pallet1Dto.setType(RegistryUnitType.PALLET);
        pallet1 = new RegisterUnit().setType(UnitType.PALLET).setRegisterId(REGISTER_ID);

        RegistryUnitInfoDTO pallet2UnitInfo = new RegistryUnitInfoDTO();
        pallet2UnitInfo.setUnitId(pallet2Id);
        pallet2Dto = new RegistryUnitDTO();
        pallet2Dto.setUnitInfo(pallet2UnitInfo);
        pallet2Dto.setType(RegistryUnitType.PALLET);
        pallet2 = new RegisterUnit().setType(UnitType.PALLET).setRegisterId(REGISTER_ID);

        RegistryUnitInfoDTO item3UnitInfo = new RegistryUnitInfoDTO();
        item3UnitInfo.setUnitId(itemId);
        item3UnitInfo.setParentUnitIds(List.of(box5Id));
        item3Dto = new RegistryUnitDTO();
        item3Dto.setUnitInfo(item3UnitInfo);
        item3Dto.setType(RegistryUnitType.ITEM);
        item3 = new RegisterUnit().setType(UnitType.ITEM).setRegisterId(REGISTER_ID);

        RegistryUnitInfoDTO item4UnitInfo = new RegistryUnitInfoDTO();
        item4UnitInfo.setUnitId(itemId);
        item4UnitInfo.setParentUnitIds(List.of(box6Id));
        item4Dto = new RegistryUnitDTO();
        item4Dto.setUnitInfo(item4UnitInfo);
        item4Dto.setType(RegistryUnitType.ITEM);
        item4 = new RegisterUnit().setType(UnitType.ITEM).setRegisterId(REGISTER_ID);

        RegistryUnitInfoDTO box5UnitInfo = new RegistryUnitInfoDTO();
        box5UnitInfo.setUnitId(box5Id);
        box5UnitInfo.setParentUnitIds(List.of(pallet1Id));
        box5Dto = new RegistryUnitDTO();
        box5Dto.setUnitInfo(box5UnitInfo);
        box5Dto.setType(RegistryUnitType.BOX);
        box5 = new RegisterUnit().setType(UnitType.BOX).setRegisterId(REGISTER_ID);

        RegistryUnitInfoDTO box6UnitInfo = new RegistryUnitInfoDTO();
        box6UnitInfo.setUnitId(box6Id);
        box6UnitInfo.setParentUnitIds(List.of(pallet1Id));
        box6Dto = new RegistryUnitDTO();
        box6Dto.setUnitInfo(box6UnitInfo);
        box6Dto.setType(RegistryUnitType.BOX);
        box6 = new RegisterUnit().setType(UnitType.BOX).setRegisterId(REGISTER_ID);
    }

    @BeforeEach
    void setUp() {
        Mockito.doNothing().when(applicationEventPublisher).publishEvent(Mockito.any());
    }

    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register_unit/register_unit_different_types.xml",
    })
    @ExpectedDatabase(
        value = "/repository/register_unit/after/register_unit_single_relation_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void persistRelation() {
        RegistryUnitDTO dto = new RegistryUnitDTO();
        RegistryUnitInfoDTO unitInfo = new RegistryUnitInfoDTO();
        unitInfo.setParentUnitIds(List.of(
            new RegistryUnitIdDTO(Set.of(
                new RegistryUnitPartialIdDTO(RegistryUnitIdType.BOX_ID, "5")
            ))
        ));
        dto.setUnitInfo(unitInfo);

        facade.persistRelation(
            dto,
            new RegisterUnit().setId(3L),
            Map.of(
                new RegistryUnitIdDTO(Set.of(
                    new RegistryUnitPartialIdDTO(RegistryUnitIdType.BOX_ID, "5")
                )),
                List.of(new RegisterUnit().setId(5L)),
                new RegistryUnitIdDTO(Set.of(
                    new RegistryUnitPartialIdDTO(RegistryUnitIdType.BOX_ID, "6")
                )),
                List.of(new RegisterUnit().setId(6L))
            )
        );
    }

    @DatabaseSetup({
        "/repository/register/register.xml",
    })
    @ExpectedDatabase(
        value = "/repository/register_unit/register_unit_different_types.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/register_unit/after/register_unit_relations_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void saveRegisterUnitsAndUpdateRegister() {
        facade.saveRegisterUnitsAndUpdateRegister(
            REGISTER_ID,
            List.of(
                new AbstractMap.SimpleEntry<>(pallet1Dto, pallet1),
                new AbstractMap.SimpleEntry<>(pallet2Dto, pallet2),
                new AbstractMap.SimpleEntry<>(item3Dto, item3),
                new AbstractMap.SimpleEntry<>(item4Dto, item4),
                new AbstractMap.SimpleEntry<>(box5Dto, box5),
                new AbstractMap.SimpleEntry<>(box6Dto, box6)
            ),
            TransportationUnitType.OUTBOUND,
            Map.of(
                pallet1Id, List.of(pallet1),
                pallet2Id, List.of(pallet2),
                box5Id, List.of(box5),
                box6Id, List.of(box6)
            )
        );
    }

    @DatabaseSetup({
        "/repository/transportation/all_kinds_of_transportation.xml",
        "/repository/register/register.xml",
        "/repository/register/transportation_unit_register.xml",
    })
    @ExpectedDatabase(
        value = "/repository/register_unit/register_unit_different_types.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/register_unit/after/register_unit_relations_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void fetchAndSaveUnits() {
        RegistryUnitDTOContainer ffwfResp = new RegistryUnitDTOContainer(1, 0, 6);
        ffwfResp.addUnit(pallet1Dto);
        ffwfResp.addUnit(pallet2Dto);
        ffwfResp.addUnit(item3Dto);
        ffwfResp.addUnit(item4Dto);
        ffwfResp.addUnit(box5Dto);
        ffwfResp.addUnit(box6Dto);

        Mockito
            .when(ffwfClient.getRegistryUnits(Mockito.eq(
                RegistryUnitsFilterDTO.Builder.builder(REQUEST_ID, FFWF_REGISTER_ID).page(0).size(500).build()
            )))
            .thenReturn(ffwfResp);

        facade.fetchAndSaveUnits(
            registerService.getById(REGISTER_ID),
            REQUEST_ID,
            TransportationUnitType.OUTBOUND
        );
    }
}
