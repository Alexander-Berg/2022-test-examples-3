package ru.yandex.market.wms.timetracker.service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestClientException;

import ru.yandex.market.logistics.util.client.HttpTemplate;
import ru.yandex.market.wms.timetracker.dao.postgres.AreaDao;
import ru.yandex.market.wms.timetracker.dao.postgres.EmployeeRelocationDao;
import ru.yandex.market.wms.timetracker.dao.postgres.WarehouseDao;
import ru.yandex.market.wms.timetracker.model.AreaModel;
import ru.yandex.market.wms.timetracker.model.WarehouseModel;
import ru.yandex.market.wms.timetracker.response.EmployeeRelocationRequest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        EmployeeRelocationServiceTest.CommonTestConfig.class,
        EmployeeRelocationService.class
})
@TestPropertySource(locations = "classpath:application-test.properties")
class EmployeeRelocationServiceTest {

    @Autowired
    private EmployeeRelocationService employeeRelocationService;

    @Autowired
    private HttpTemplate httpTemplate;

    @Autowired
    private EmployeeRelocationDao employeeRelocationDao;

    @Autowired
    private WarehouseDao warehouseDao;

    @Autowired
    private AreaDao areaDao;

    @TestConfiguration
    public static class CommonTestConfig {

        @Bean
        @Qualifier("hrmsHttpTemplate")
        HttpTemplate httpTemplate() {
            return Mockito.mock(HttpTemplate.class);
        }

        @Bean
        EmployeeRelocationDao employeeRelocationDao() {
            return Mockito.mock(EmployeeRelocationDao.class);
        }

        @Bean
        WarehouseDao warehouseDao() {
            return Mockito.mock(WarehouseDao.class);
        }

        @Bean
        AreaDao areaDao() {
            return Mockito.mock(AreaDao.class);
        }

        @Bean
        StatusProcessorService statusProcessorService() {
            return Mockito.mock(StatusProcessorService.class);
        }
    }

    @BeforeEach
    void init() {
        Mockito.reset(httpTemplate);
    }

    @Test
    void updateEmployeeRelocationWhenAllAreaExist() {

        when(warehouseDao.findAll()).thenReturn(
                List.of(
                        WarehouseModel.builder()
                                .id(1L)
                                .name("SOF").build()));

        final EmployeeRelocationRequest[] restResult =
                List.of(EmployeeRelocationRequest.builder()
                        .position("Кладовщик")
                        .area(AreaModel.builder()
                                .name("test")
                                .build())
                        .build()).toArray(EmployeeRelocationRequest[]::new);

        when(httpTemplate.executeGet(any(), any(), any())).thenReturn(restResult);

        when(areaDao.findIdByName(any())).thenReturn(Map.of("test", 1L));

        doNothing().when(employeeRelocationDao).insert(eq(1), anyCollection());

        Instant fromTime = Instant.parse("2021-10-29T12:00:00Z");

        Instant toTime = Instant.parse("2021-10-29T12:05:00Z");

        employeeRelocationService.updateEmployeeRelocation(fromTime, toTime);
    }

    @Test
    void updateEmployeeRelocationWhenAreaNotExist() {

        when(warehouseDao.findAll()).thenReturn(
                List.of(
                        WarehouseModel.builder()
                                .id(1L)
                                .name("SOF").build()));

        final EmployeeRelocationRequest[] restResult =
                List.of(EmployeeRelocationRequest.builder()
                        .position("Старший кладовщик")
                        .area(AreaModel.builder()
                                .name("test")
                                .build())
                        .build()).toArray(EmployeeRelocationRequest[]::new);

        when(httpTemplate.executeGet(any(), any(), any())).thenReturn(restResult);

        when(areaDao.findIdByName(any())).thenReturn(Collections.emptyMap());

        when(areaDao.save(any())).thenReturn(Map.of("test", 1L));

        doNothing().when(employeeRelocationDao).insert(eq(1), anyCollection());

        Instant fromTime = Instant.parse("2021-10-29T12:00:00Z");

        Instant toTime = Instant.parse("2021-10-29T12:05:00Z");

        employeeRelocationService.updateEmployeeRelocation(fromTime, toTime);
    }

    @Test
    void updateEmployeeRelocationWhenAreaNotExistAndReceiveDuplicate() {

        when(warehouseDao.findAll()).thenReturn(
                List.of(
                        WarehouseModel.builder()
                                .id(1L)
                                .name("SOF").build()));

        final EmployeeRelocationRequest[] restResult =
                List.of(EmployeeRelocationRequest.builder()
                                .position("test")
                                .area(AreaModel.builder()
                                        .name("test")
                                        .build())
                                .build(),
                        EmployeeRelocationRequest.builder()
                                .position("test")
                                .area(AreaModel.builder()
                                        .name("test")
                                        .build())
                                .build()).toArray(EmployeeRelocationRequest[]::new);

        when(httpTemplate.executeGet(any(), any(), any())).thenReturn(restResult);

        when(areaDao.findIdByName(any())).thenReturn(Collections.emptyMap());

        when(areaDao.save(any())).thenReturn(Map.of("test", 1L));

        doNothing().when(employeeRelocationDao).insert(eq(1), anyCollection());

        Instant fromTime = Instant.parse("2021-10-29T12:00:00Z");

        Instant toTime = Instant.parse("2021-10-29T12:05:00Z");

        employeeRelocationService.updateEmployeeRelocation(fromTime, toTime);
    }

    @Test
    void updateEmployeeRelocationWhenException() {

        when(warehouseDao.findAll()).thenReturn(
                List.of(
                        WarehouseModel.builder()
                                .id(1L)
                                .name("SOF").build()));

        when(httpTemplate.executeGet(any(), any(), any()))
                .thenThrow(RestClientException.class);

        doNothing().when(employeeRelocationDao).insert(eq(1), anyCollection());

        Instant fromTime = Instant.parse("2021-10-29T12:00:00Z");

        Instant toTime = Instant.parse("2021-10-29T12:05:00Z");

        assertThrows(RestClientException.class,
                () -> employeeRelocationService.updateEmployeeRelocation(fromTime, toTime));
    }

}
