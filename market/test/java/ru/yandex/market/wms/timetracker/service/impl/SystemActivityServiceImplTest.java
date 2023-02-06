package ru.yandex.market.wms.timetracker.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import cz.jirutka.rsql.parser.RSQLParser;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.wms.timetracker.dao.SystemActivityDao;
import ru.yandex.market.wms.timetracker.model.SystemActivityModel;
import ru.yandex.market.wms.timetracker.model.WarehouseModel;
import ru.yandex.market.wms.timetracker.model.enums.EmployeeStatus;
import ru.yandex.market.wms.timetracker.response.SystemActivityResponse;
import ru.yandex.market.wms.timetracker.service.WarehouseService;
import ru.yandex.market.wms.timetracker.service.WarehouseTimeZoneConvertorService;
import ru.yandex.market.wms.timetracker.specification.rsql.Filter;
import ru.yandex.market.wms.timetracker.specification.rsql.Pageable;
import ru.yandex.market.wms.timetracker.specification.rsql.SearchOperators;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        SystemActivityServiceImpl.class,
        WarehouseTimeZoneConvertorService.class
})
@Import({
        SystemActivityServiceImplTest.CommonTestConfig.class
})
class SystemActivityServiceImplTest {

    @Autowired
    private SystemActivityServiceImpl systemActivityService;

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private SystemActivityDao systemActivityDao;

    public static class CommonTestConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2021-12-15T12:00:00.00Z"), ZoneOffset.UTC);
        }

        @Bean
        WarehouseService warehouseService() {
            return Mockito.mock(WarehouseService.class);
        }

        @Bean
        RSQLParser rsqlParser() {
            return new RSQLParser(SearchOperators.OPERATORS);
        }

        @Bean
        SystemActivityDao systemActivityDao() {
            return Mockito.mock(SystemActivityDao.class);
        }
    }

    @Test
    void currentAssignedProcess() {

        Mockito.when(warehouseService.readWarehouse(ArgumentMatchers.any(String.class)))
                .thenReturn(WarehouseModel.builder()
                        .id(1L)
                        .build());

        Mockito.when(systemActivityDao.findAll(
                        ArgumentMatchers.eq(1L), (Filter<SystemActivityModel>) ArgumentMatchers.any(Filter.class),
                        ArgumentMatchers.any(Pageable.class)))
                .thenReturn(
                        List.of(SystemActivityModel.builder()
                                .id(1L)
                                .warehouseId(1L)
                                .assigner("assigner")
                                .process(EmployeeStatus.CONSOLIDATION)
                                .user("test")
                                .createTime(Instant.parse("2021-12-15T15:40:54Z"))
                                .userStartedActivity(false)
                                .expectedEndTime(Instant.parse("2021-12-15T18:40:54Z"))
                                .build())
                );

        Mockito.when(warehouseService.warehouseTimeZone(ArgumentMatchers.eq(1L)))
                .thenReturn(ZoneId.of("Europe/Moscow"));

        final SystemActivityResponse expected = SystemActivityResponse.builder()
                .userName("test")
                .process("CONSOLIDATION")
                .assigner("assigner")
                .createTime(LocalDateTime.of(2021, 12, 15, 18, 40, 54))
                .expectedEndTime(LocalDateTime.of(2021, 12, 15, 21, 40, 54))
                .userStartedActivity(false)
                .build();

        final List<SystemActivityResponse> result = systemActivityService.currentAssignedProcess("SOF", 10, 0);

        Assertions.assertAll(
                () -> Assertions.assertEquals(1, result.size()),
                () -> MatcherAssert.assertThat(result, Matchers.containsInAnyOrder(expected))
        );
    }
}
