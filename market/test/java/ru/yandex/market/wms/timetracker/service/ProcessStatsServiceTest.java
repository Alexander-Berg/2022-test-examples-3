package ru.yandex.market.wms.timetracker.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.wms.timetracker.dao.dash.PerformanceByProcessDao;
import ru.yandex.market.wms.timetracker.dao.postgres.EmployeeStatusDao;
import ru.yandex.market.wms.timetracker.dao.postgres.OutstaffCompanyDao;
import ru.yandex.market.wms.timetracker.dao.postgres.StatsByProcessDao;
import ru.yandex.market.wms.timetracker.dao.postgres.WarehouseDao;
import ru.yandex.market.wms.timetracker.model.AreaStatsModel;
import ru.yandex.market.wms.timetracker.model.DetailsByProcessModel;
import ru.yandex.market.wms.timetracker.model.OutstaffCompanyModel;
import ru.yandex.market.wms.timetracker.model.ProcessModel;
import ru.yandex.market.wms.timetracker.model.ProcessStatDto;
import ru.yandex.market.wms.timetracker.model.WarehouseModel;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        ProcessStatsServiceTest.CommonTestConfig.class,
        ProcessStatsService.class
})
@TestPropertySource(locations = "classpath:application-test.properties")
@Import({
        ProcessStatsServiceTest.CommonTestConfig.class
})
class ProcessStatsServiceTest {
    @TestConfiguration
    public static class CommonTestConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2021-11-23T12:00:00.00Z"), ZoneOffset.UTC);
        }
    }

    @MockBean
    private WarehouseDao warehouseDao;

    @MockBean
    private OutstaffCompanyDao outstaffCompanyDao;

    @MockBean
    private WarehouseTimeZoneConvertorService warehouseTimeZoneConvertorService;

    @Autowired
    private ProcessStatsService processStatsService;

    @MockBean
    private StatsByProcessDao statsByProcessDao;

    @MockBean
    private EmployeeStatusDao employeeStatusDao;

    @MockBean
    private PerformanceByProcessDao performanceByProcessDao;

    @Test
    void updateWorkersNumberTest() {
        when(statsByProcessDao.findAllProcesses()).thenReturn(
                List.of(ProcessModel.builder()
                                .process("CONSOLIDATION")
                                .hasArea(false)
                                .hasPerformance(false)
                                .build(),
                        ProcessModel.builder()
                                .process("INDIRECTACTIVITY")
                                .hasArea(true)
                                .hasPerformance(false)
                                .build(),
                        ProcessModel.builder()
                                .process("PLACEMENT")
                                .hasArea(false)
                                .hasPerformance(true)
                                .build(),
                        ProcessModel.builder()
                                .process("SHIPPING")
                                .hasArea(false)
                                .hasPerformance(false)
                                .build()
                )
        );

        when(warehouseDao.findAll()).thenReturn(List.of(WarehouseModel.builder().id(1L).name("SOF").build()));

        when(outstaffCompanyDao.findAllByWhsId(eq(1L))).thenReturn(
                List.of(OutstaffCompanyModel.builder().id(1L).name("ЛайтЛог").prefix("sof-ll").build())
        );

        when(warehouseTimeZoneConvertorService.fromUtc(eq(1L), any())).thenReturn(
                LocalDateTime.parse("2021-11-23 15:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        when(employeeStatusDao.getProcessesStats(any(), eq(1L))).thenReturn(
                List.of(
                        ProcessStatDto.builder()
                                .process("CONSOLIDATION")
                                .numOfWorkers(5)
                                .build(),
                        ProcessStatDto.builder()
                                .process("INDIRECTACTIVITY")
                                .numOfWorkers(20)
                                .build(),
                        ProcessStatDto.builder()
                                .process("PLACEMENT")
                                .numOfWorkers(10)
                                .build()
                )
        );

        when(statsByProcessDao.findLastHourStats(eq(1L))).thenReturn(
                List.of(
                        ProcessStatDto.builder()
                                .process("CONSOLIDATION")
                                .numOfWorkers(7)
                                .build(),
                        ProcessStatDto.builder()
                                .process("INDIRECTACTIVITY")
                                .numOfWorkers(18)
                                .build(),
                        ProcessStatDto.builder()
                                .process("PLACEMENT")
                                .numOfWorkers(10)
                                .build(),
                        ProcessStatDto.builder()
                                .process("SHIPPING")
                                .numOfWorkers(3)
                                .build()
                )
        );

        when(employeeStatusDao.getProcessesStatsOutstaff(any(), eq(1L), eq("sof-ll"))).thenReturn(
                List.of(
                        ProcessStatDto.builder()
                                .process("CONSOLIDATION")
                                .numOfWorkers(2)
                                .build(),
                        ProcessStatDto.builder()
                                .process("INDIRECTACTIVITY")
                                .numOfWorkers(5)
                                .build(),
                        ProcessStatDto.builder()
                                .process("PLACEMENT")
                                .numOfWorkers(1)
                                .build()
                )
        );

        when(statsByProcessDao.findLastHourStatsOutstaff(eq(1L), eq(1L))).thenReturn(
                List.of(
                        ProcessStatDto.builder()
                                .process("CONSOLIDATION")
                                .numOfWorkers(1)
                                .build(),
                        ProcessStatDto.builder()
                                .process("INDIRECTACTIVITY")
                                .numOfWorkers(5)
                                .build(),
                        ProcessStatDto.builder()
                                .process("PLACEMENT")
                                .numOfWorkers(2)
                                .build(),
                        ProcessStatDto.builder()
                                .process("SHIPPING")
                                .numOfWorkers(0)
                                .build()
                )
        );

        doNothing().when(statsByProcessDao).updateProcesses(anyList(), eq(1L));

        doNothing().when(statsByProcessDao).updateProcessesOutstaff(anyList(), eq(1L), eq(1L));

        when(employeeStatusDao.getProcessesStatsInArea(any(), eq(1L), anyList())).thenReturn(
                List.of(AreaStatsModel.builder().process("INDIRECTACTIVITY").area("LUNCH").numOfWorkers(10).build())
        );

        when(statsByProcessDao.findLastHourAreaStats(eq(1L))).thenReturn(
                List.of(AreaStatsModel.builder().process("INDIRECTACTIVITY").area("LUNCH").numOfWorkers(13).build())
        );

        when(employeeStatusDao.getOutstaffProcessesStatsInArea(any(), eq(1L), any(), eq("sof-ll"))).thenReturn(
                Collections.emptyList()
        );

        when(statsByProcessDao.findLastHourOutstaffAreaStats(eq(1L), eq(1L))).thenReturn(
                Collections.emptyList()
        );

        doNothing().when(statsByProcessDao).clearOutstaffAreas(eq(1L), eq(1L));

        doNothing().when(statsByProcessDao).updateAreas(anyList(), eq(1L));

        when(performanceByProcessDao.getPerformanceByProcess(eq("SOF"), any())).thenReturn(List.of(
            DetailsByProcessModel.builder().process("Размещение").time(178).unit(499).nznTime(449).nznUnit(303).build()
        ));

        doNothing().when(statsByProcessDao).updatePerformance(anyList(), eq(1L));


        when(performanceByProcessDao.getOutstaffPerformanceByProcess(eq("SOF:ЛайтЛог"), any())).thenReturn(
                Collections.emptyList()
        );

        doNothing().when(statsByProcessDao).clearOutstaffPerformance(eq(1L), eq(1L));

        processStatsService.updateWorkersNumber();
    }

    @Test
    void updateWorkersNumberWithExceptionTest() {
        when(statsByProcessDao.findAllProcesses()).thenReturn(
                Collections.emptyList()
        );

        assertThrows(IllegalStateException.class,
                () -> processStatsService.updateWorkersNumber());
    }
}
