package ru.yandex.market.ff.service;

import java.io.IOException;
import java.util.Collections;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.service.implementation.CachedWarehouseService;
import ru.yandex.market.ff.service.implementation.PalletLabelGenerationService;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.core.Phone;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;

/**
 * Функциональные тесты для {@link PalletLabelGenerationService}.
 */
public class PalletLabelGenerationServiceTest extends ActGenerationServiceTest {

    @Autowired
    private PalletLabelGenerationService generationService;
    @Autowired
    private CachedWarehouseService warehouseService;

    @BeforeEach
    void prepare() {
        warehouseService.invalidateCache();
    }

    @Test
    @DatabaseSetup("classpath:service/pdf-report/requests.xml")
    void generatePaletLabel() throws IOException {
        Mockito.when(lmsClient.getLogisticsPoints(Mockito.any(LogisticsPointFilter.class)))
            .thenReturn(Collections.singletonList(getWarehouseLogisticsPointResponse()));
        assertPdfActGeneration(1, "pl-small.txt", generationService::generateReport);
    }

    @Test
    @DatabaseSetup("classpath:service/pdf-report/requests.xml")
    void generatePaletLabelLongId() throws IOException {
        Mockito.when(lmsClient.getLogisticsPoints(Mockito.any(LogisticsPointFilter.class)))
            .thenReturn(Collections.singletonList(getWarehouseLogisticsPointResponse()));
        assertPdfActGeneration(4, "pl-long-id.txt", generationService::generateReport);
    }

    private LogisticsPointResponse getWarehouseLogisticsPointResponse() {
        return LogisticsPointResponse.newBuilder().id(1234L).name("Rostov").address(getAddress())
            .phones(Collections.singleton(getPhone())).build();
    }

    private Address getAddress() {
        return Address.newBuilder()
            .settlement("Котельники")
            .street("Яничкин проезд")
            .house("7")
            .comment("терминал БД-6")
            .build();
    }

    private Phone getPhone() {
        return new Phone("+ 7 (916) 567-89-01", null, null, null);
    }
}
