package ru.yandex.market.wms.timetracker.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.logistics.util.client.HttpTemplate;
import ru.yandex.market.wms.shared.libs.its.settings.provider.MockSettingsProvider;
import ru.yandex.market.wms.shared.libs.its.settings.provider.SettingsProvider;
import ru.yandex.market.wms.timetracker.dao.SystemActivityDao;
import ru.yandex.market.wms.timetracker.dao.postgres.EmployeeStatusDao;
import ru.yandex.market.wms.timetracker.dao.postgres.EmployeeStatusHistoryDao;
import ru.yandex.market.wms.timetracker.dao.postgres.ShiftDao;
import ru.yandex.market.wms.timetracker.dao.postgres.WarehouseDao;
import ru.yandex.market.wms.timetracker.dto.CurrentShiftModel;
import ru.yandex.market.wms.timetracker.dto.EmployeeInShiftDto;
import ru.yandex.market.wms.timetracker.model.WarehouseModel;
import ru.yandex.market.wms.timetracker.model.settings.Settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        ShiftServiceTest.CommonTestConfig.class,
        ShiftService.class
})
@TestPropertySource(locations = "classpath:application-test.properties")
public class ShiftServiceTest {

    @TestConfiguration
    public static class CommonTestConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2021-11-23T12:00:00.00Z"), ZoneOffset.UTC);
        }

        @Bean
        SettingsProvider<Settings> settingsProvider() {
            return new MockSettingsProvider<>(new Settings());
        }
    }

    @Autowired
    private ShiftService shiftService;

    @MockBean
    private HttpTemplate httpTemplate;

    @MockBean
    private ShiftDao shiftDao;

    @MockBean
    private EmployeeStatusDao employeeStatusDao;

    @MockBean
    private EmployeeStatusHistoryDao employeeStatusHistoryDao;

    @MockBean
    private WarehouseDao warehouseDao;

    @MockBean
    private WarehouseTimeZoneConvertorService warehouseTimeZoneConvertorService;

    @MockBean
    private SystemActivityDao systemActivityDao;

    @MockBean
    private WmsCoreClient wmsCoreClient;

    @MockBean
    private WmsTaskRouterClient wmsTaskRouterClient;

    @BeforeEach
    void init() {
        Mockito.reset(httpTemplate);
    }

    @Test
    void updateShiftsAndStatuses() {

        when(warehouseDao.findAll()).thenReturn(
                List.of(
                        WarehouseModel.builder()
                                .id(1L)
                                .name("SOF").build()));

        final EmployeeInShiftDto[] restResult =
                List.of(EmployeeInShiftDto.builder()
                        .position("Кладовщик")
                        .shiftStart(Instant.parse("2021-11-26T05:15:00Z"))
                        .shiftEnd(Instant.parse("2021-11-26T17:15:00Z"))
                        .build()).toArray(EmployeeInShiftDto[]::new);

        when(warehouseTimeZoneConvertorService.fromUtc(eq(1L), any())).thenReturn(
                LocalDateTime.parse("2021-11-26 05:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        when(httpTemplate.executeGet(any(), any(), any())).thenReturn(restResult);

        doNothing().when(shiftDao).insertAll(anyList());

        doNothing().when(employeeStatusDao).insert(anyList());
        doNothing().when(employeeStatusDao).startTransaction();
        doNothing().when(employeeStatusDao).endTransaction();
        doNothing().when(employeeStatusHistoryDao).insert(anyList());
        doNothing().when(systemActivityDao).deleteByWarehouseId(any(Long.class));

        when(employeeStatusDao.findByNaturalId(any(Long.class), anyCollection())).thenReturn(Collections.emptyList());

        shiftService.updateEmployeesInShift(Instant.parse("2021-11-26T08:00:00Z"), true);
    }

    @Test
    void getShiftIdByUserAndWarehouse() {
        String username = "test";
        Long whsId = 1L;
        final CurrentShiftModel shiftModel = CurrentShiftModel.builder()
                .username(username)
                .warehouseId(whsId)
                .position("Кладовщик")
                .shiftName("Смена")
                .shiftStart(Instant.parse("2022-07-21T20:00:00Z"))
                .shiftEnd(Instant.parse("2022-07-22T05:00:00Z"))
                .build();

        when(shiftDao.findByUserAndWarehouse(username, whsId)).thenReturn(Optional.of(shiftModel));

        final Optional<String> shiftId = shiftService.getShiftIdByUserAndWarehouse(username, whsId);
        assertTrue(shiftId.isPresent());
        assertEquals("SHIFT_FROM_2022-07-21Z_TO_2022-07-22Z", shiftId.get());
    }
}
