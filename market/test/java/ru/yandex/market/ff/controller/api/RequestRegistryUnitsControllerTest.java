package ru.yandex.market.ff.controller.api;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageRequest;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.client.dto.RegistryUnitCountDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitCountsInfoDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitDTOContainer;
import ru.yandex.market.ff.client.dto.RegistryUnitIdDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitInfoDTO;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.client.enums.RegistryUnitType;
import ru.yandex.market.ff.client.enums.UnitCountType;
import ru.yandex.market.ff.model.bo.RegistryUnitsFilter;
import ru.yandex.market.ff.util.FileContentUtils;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ParametersAreNonnullByDefault
class RequestRegistryUnitsControllerTest extends MvcIntegrationTest {

    @Test
    void unitsSuccessGetMany() throws Exception {
        RegistryUnitsFilter filter = new RegistryUnitsFilter();
        filter.setRegistryIds(Set.of(2L));
        long requestId = 123L;
        PageRequest pageable = new PageRequest(0, 10);
        Mockito.doReturn(getRegistryUnitDTOContainer()).when(registryUnitService)
                .findAllByRequestIdAndFilter(requestId, filter, pageable);
        Mockito.doReturn(true).when(shopRequestFetchingService).requestExists(requestId);
        mockMvc.perform(get("/requests/123/registry-units").param("registryIds", "2"))
                .andExpect(status().isOk())
                .andExpect(content().json(FileContentUtils.getFileContent(
                        "controller/registry/units/get_registry_units.json"), true));
        Mockito.verify(registryUnitService).findAllByRequestIdAndFilter(requestId, filter, pageable);
        verifyNoMoreInteractions(registryUnitService);
    }

    @Test
    void getUnitsWithFilteringOnType() throws Exception {
        RegistryUnitsFilter filter = new RegistryUnitsFilter();
        filter.setRegistryIds(Set.of(2L, 3L));
        filter.setRegistryUnitTypes(EnumSet.of(RegistryUnitType.PALLET, RegistryUnitType.BOX));
        long requestId = 123L;
        PageRequest pageable = new PageRequest(0, 10);
        Mockito.doReturn(getRegistryUnitDTOContainer()).when(registryUnitService)
                .findAllByRequestIdAndFilter(requestId, filter, pageable);
        Mockito.doReturn(true).when(shopRequestFetchingService).requestExists(requestId);
        mockMvc.perform(get("/requests/123/registry-units").param("registryIds", "2", "3")
                .param("registryUnitTypes", "0", "10"))
                .andExpect(status().isOk())
                .andExpect(content().json(FileContentUtils.getFileContent(
                        "controller/registry/units/get_registry_units_filtered.json"), true));
        Mockito.verify(registryUnitService).findAllByRequestIdAndFilter(requestId, filter, pageable);
        verifyNoMoreInteractions(registryUnitService);
    }

    @Test
    void unitsBadRequestIfFilterNotProvided() throws Exception {
        mockMvc.perform(get("/requests/1/registry-units"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(FileContentUtils.getFileContent(
                        "controller/registry/units/empty-registry-ids.json")));
    }

    @Test
    @DatabaseSetup(value = "classpath:controller/registry/delete-all.xml", type = DatabaseOperation.DELETE_ALL)
    void units404NoRequest() throws Exception {
        mockMvc.perform(get("/requests/1/registry-units").param("registryIds", "1"))
                .andExpect(status().isNotFound())
                .andExpect(content().json(FileContentUtils.getFileContent(
                        "controller/registry/request-not-found.json")));
    }

    private RegistryUnitDTOContainer getRegistryUnitDTOContainer() {
        RegistryUnitDTOContainer expected = new RegistryUnitDTOContainer(3, 1, 9);
        RegistryUnitDTO unit1 = new RegistryUnitDTO();
        unit1.setRegistryId(2L);
        unit1.setUnitInfo(createInfo(RegistryUnitIdType.PALLET_ID, "PL0002", UnitCountType.FIT, 1));
        unit1.setType(RegistryUnitType.PALLET);
        expected.addUnit(unit1);

        RegistryUnitDTO unit2 = new RegistryUnitDTO();
        unit2.setRegistryId(2L);
        unit2.setUnitInfo(createInfo(
                RegistryUnitIdType.BOX_ID, "P0002",
                UnitCountType.FIT, 1,
                unit1.getUnitInfo().getUnitId())
        );
        unit2.setType(RegistryUnitType.BOX);
        expected.addUnit(unit2);
        return expected;
    }

    private RegistryUnitInfoDTO createInfo(RegistryUnitIdType idType, String idValue,
                                           UnitCountType countType, int countValue,
                                           RegistryUnitIdDTO... parents) {
        RegistryUnitInfoDTO info = new RegistryUnitInfoDTO();
        info.setUnitId(RegistryUnitIdDTO.of(idType, idValue));
        RegistryUnitCountsInfoDTO counts = new RegistryUnitCountsInfoDTO();
        RegistryUnitCountDTO count = new RegistryUnitCountDTO(
                countType,
                countValue,
                List.of(RegistryUnitIdDTO.of(RegistryUnitIdType.CIS, "CIS0002")),
            null);
        counts.setUnitCounts(singletonList(count));
        info.setUnitCountsInfo(counts);
        info.setParentUnitIds(Arrays.asList(parents));

        return info;
    }

}
