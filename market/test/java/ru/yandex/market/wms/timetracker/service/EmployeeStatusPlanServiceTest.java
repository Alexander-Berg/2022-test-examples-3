package ru.yandex.market.wms.timetracker.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.wms.timetracker.dao.postgres.EmployeeStatusPlanDao;
import ru.yandex.market.wms.timetracker.dto.EmployeeStatusPlanModel;
import ru.yandex.market.wms.timetracker.model.enums.EmployeeStatus;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        EmployeeStatusPlanServiceTest.CommonTestConfig.class,
        EmployeeStatusPlanService.class
})
@TestPropertySource(locations = "classpath:application-test.properties")
class EmployeeStatusPlanServiceTest {

    @Autowired
    private EmployeeStatusPlanService employeeStatusPlanService;

    @Autowired
    private EmployeeStatusPlanDao employeeStatusPlanDao;

    @BeforeEach
    void init() {
        Mockito.reset(employeeStatusPlanDao);
    }

    @TestConfiguration
    public static class CommonTestConfig {
        @Bean
        EmployeeStatusPlanDao employeeStatusPlanDao() {
            return Mockito.mock(EmployeeStatusPlanDao.class);
        }

        @Bean
        WarehouseService warehouseService() {
            return Mockito.mock(WarehouseService.class);
        }

        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2021-11-26T12:00:00.00Z"), ZoneOffset.UTC);
        }
    }

    @Test
    void saveAllExist() {
        final List<EmployeeStatusPlanModel> arguments = List.of(EmployeeStatusPlanModel.builder()
                        .warehouseId(1L)
                        .status(EmployeeStatus.SHIPPING)
                        .count(10)
                        .build(),
                EmployeeStatusPlanModel.builder()
                        .warehouseId(1L)
                        .status(EmployeeStatus.DROPPING)
                        .count(10)
                        .build());

        Mockito.when(employeeStatusPlanDao.findAllWhereStatusIn(ArgumentMatchers.eq(1L),
                        ArgumentMatchers.anyCollection()))
                .thenReturn(List.of(
                        EmployeeStatusPlanModel.builder()
                                .status(EmployeeStatus.SHIPPING)
                                .count(20)
                                .build(),
                        EmployeeStatusPlanModel.builder()
                                .status(EmployeeStatus.DROPPING)
                                .count(20)
                                .build()));

        Mockito.doNothing().when(employeeStatusPlanDao).update(ArgumentMatchers.anyCollection());

        employeeStatusPlanService.save(arguments);

        Assertions.assertAll(
                () -> Mockito.verify(employeeStatusPlanDao, Mockito.times(1))
                        .update(ArgumentMatchers.anyCollection())
        );
    }
}
