package ru.yandex.market.ff.controller.health;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.client.enums.RegistryUnitType;
import ru.yandex.market.ff.health.cache.RegistryHealthCache;
import ru.yandex.market.ff.model.dto.registry.RegistryUnitId;
import ru.yandex.market.ff.model.dto.registry.UnitCountsInfo;
import ru.yandex.market.ff.model.entity.registry.RegistryUnitInvalidEntity;
import ru.yandex.market.ff.model.entity.registry.RegistryUnitInvalidStatus;
import ru.yandex.market.ff.repository.RegistryUnitInvalidRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


class RegistryHealthControllerTest extends MvcIntegrationTest {

    @Autowired
    private RegistryHealthCache registryHealthCache;

    @Autowired
    private RegistryUnitInvalidRepository unitInvalidRepository;

    @AfterEach
    public void invalidateCache() {
        registryHealthCache.invalidateCache();
    }

    @Test
    @DatabaseSetup("classpath:controller/registry-health/before_ok.xml")
    public void checkOkStatus() throws Exception {
        mockMvc.perform(
                        get("/health/registry/units/invalidUnitsWithErrors")
                )
                .andExpect(status().isOk())
                .andExpect(content().string("0;ok"));
    }


    @Test
    @DatabaseSetup("classpath:controller/registry-health/before_warning_1.xml")
    public void checkWarningStatus() throws Exception {
        mockMvc.perform(
                        get("/health/registry/units/invalidUnitsWithErrors")
                )
                .andExpect(status().isOk())
                .andExpect(content().string("1;Some registry units are still invalid. Count = 6"));
    }

    @Test
    @DatabaseSetup("classpath:controller/registry-health/before_error_1.xml")
    public void checkErrorStatus() throws Exception {
        unitInvalidRepository.save(generateInvalidUnits(6));

        mockMvc.perform(
                        get("/health/registry/units/invalidUnitsWithErrors")
                )
                .andExpect(status().isOk())
                .andExpect(content().string("2;Increasing count of invalid registry units. " +
                        "Changes from previous period is 6 units. Current count = 12. Period is 24 hours."));
    }

    private Collection<RegistryUnitInvalidEntity> generateInvalidUnits(int count) {

        return IntStream.range(0, count).mapToObj(s ->
                RegistryUnitInvalidEntity.builder()
                        .reasons(Collections.emptyList())
                        .status(RegistryUnitInvalidStatus.INVALID)
                        .sourceRegistryId(1L)
                        .identifiers(RegistryUnitId.builder().build())
                        .unitCountsInfo(UnitCountsInfo.builder().build())
                        .type(RegistryUnitType.BOX)
                        .build()
        ).collect(Collectors.toList());
    }
}
