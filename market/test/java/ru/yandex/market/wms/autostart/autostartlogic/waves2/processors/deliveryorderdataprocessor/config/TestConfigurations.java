package ru.yandex.market.wms.autostart.autostartlogic.waves2.processors.deliveryorderdataprocessor.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Builder;
import lombok.Data;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.wms.autostart.autostartlogic.nonsort.FilterOutReason;
import ru.yandex.market.wms.autostart.autostartlogic.service.SortingStationService;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.CandidateSortStation;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.services.InventoryValidator;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.services.InventoryValidatorService;
import ru.yandex.market.wms.common.spring.dao.entity.Order;
import ru.yandex.market.wms.common.spring.dao.implementation.ConsolidationLocationDao;
import ru.yandex.market.wms.common.spring.dao.implementation.PickDetailDao;
import ru.yandex.market.wms.common.spring.enums.AutoStartSortingStationMode;
import ru.yandex.market.wms.common.spring.enums.ConsolidationLocationType;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

@TestConfiguration
public class TestConfigurations {
    @Mock
    private SortingStationService sortingStationService;
    @Mock
    private InventoryValidatorService inventoryValidatorService;
    @Mock
    private ConsolidationLocationDao consolidationLocationDao;
    @Mock
    private PickDetailDao pickDetailDao;

    @Bean
    @Primary
    public SortingStationService sortingStationService(Properties properties) {
        MockitoAnnotations.openMocks(this);
        LinkedHashSet<CandidateSortStation> stations = properties.sortingStations.stream()
                .map(station -> CandidateSortStation.builder().name(station).build())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        when(sortingStationService.stationsAndUsableSlots(false, AutoStartSortingStationMode.ORDERS))
                .thenReturn(stations);
        return sortingStationService;
    }

    @Bean
    @Primary
    public ConsolidationLocationDao consolidationLocationDao(Properties properties) {
        MockitoAnnotations.openMocks(this);
        Map<ConsolidationLocationType, List<String>> consolidationLocations = new HashMap<>();
        if (!properties.singleLines.isEmpty()) {
            consolidationLocations.put(ConsolidationLocationType.SINGLES, properties.singleLines);
        }
        if (!properties.oversizeLines.isEmpty()) {
            consolidationLocations.put(ConsolidationLocationType.OVERSIZE, properties.oversizeLines);
        }
        when(consolidationLocationDao.getAllConsolidationLocations(null)).thenReturn(consolidationLocations);
        return consolidationLocationDao;
    }

    @Bean
    @Primary
    public PickDetailDao pickDetailDao(Properties properties) {
        MockitoAnnotations.openMocks(this);
        List<String> allLines =
                Stream.concat(properties.singleLines.stream(), properties.oversizeLines.stream()).toList();
        Map<String, Integer> allLinesMap =
                allLines.stream().collect(Collectors.toMap(Function.identity(), (k) -> 1));
        when(pickDetailDao.countNonSortPickDetailsInSortLocations(allLines))
                .thenReturn(allLinesMap);
        return pickDetailDao;
    }

    @Bean
    @Primary
    public InventoryValidatorService inventoryValidatorService() {
        MockitoAnnotations.openMocks(this);
        InventoryValidator inventoryValidator = new InventoryValidator() {
            @Override
            public Optional<FilterOutReason> canAddOrderToTotal(Order order) {
                return Optional.empty();
            }

            @Override
            public void revertSkuOnStock(Order order) {

            }
        };
        when(inventoryValidatorService.buildEntity(anyList(), nullable(Integer.class)))
                .thenReturn(inventoryValidator);
        return inventoryValidatorService;
    }

    @Data
    @Builder
    public static class Properties {
        @Builder.Default
        List<String> sortingStations = new ArrayList<>();
        @Builder.Default
        List<String> singleLines = new ArrayList<>();
        @Builder.Default
        List<String> oversizeLines = new ArrayList<>();
    }
}
