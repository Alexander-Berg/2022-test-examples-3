package ru.yandex.market.wms.timetracker.service;

import java.time.Instant;
import java.util.Optional;

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

import ru.yandex.market.wms.shared.libs.its.settings.provider.MockSettingsProvider;
import ru.yandex.market.wms.shared.libs.its.settings.provider.SettingsProvider;
import ru.yandex.market.wms.timetracker.dao.EmployeeProcessTypeCurrentStateDao;
import ru.yandex.market.wms.timetracker.dao.EmployeeProcessTypeHistoryDao;
import ru.yandex.market.wms.timetracker.dao.postgres.PutAwayZoneDao;
import ru.yandex.market.wms.timetracker.mapper.ProcessTypeClickHouseMapper;
import ru.yandex.market.wms.timetracker.model.EmployeeProcessTypeModel;
import ru.yandex.market.wms.timetracker.model.WarehouseModel;
import ru.yandex.market.wms.timetracker.model.enums.AssigmentType;
import ru.yandex.market.wms.timetracker.model.enums.ProcessType;
import ru.yandex.market.wms.timetracker.model.settings.Settings;
import ru.yandex.market.wms.timetracker.response.EmployeeProcessTypeRequest;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        EmployeeProcessTypeServiceTest.TestConfig.class,
        EmployeeProcessTypeService.class
})
@TestPropertySource(locations = "classpath:application-test.properties")
class EmployeeProcessTypeServiceTest {

    @Autowired
    private EmployeeProcessTypeHistoryDao historyDao;

    @Autowired
    private EmployeeProcessTypeCurrentStateDao currentStateDao;

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private EmployeeProcessTypeService employeeProcessTypeService;

    @Autowired
    private AchievementMetricService achievementMetricService;

    @TestConfiguration
    public static class TestConfig {
        @Bean
        EmployeeProcessTypeHistoryDao employeeProcessTypeHistoryDao() {
            return Mockito.mock(EmployeeProcessTypeHistoryDao.class);
        }

        @Bean
        EmployeeProcessTypeCurrentStateDao employeeProcessTypeCurrentStateDao() {
            return Mockito.mock(EmployeeProcessTypeCurrentStateDao.class);
        }

        @Bean
        WarehouseService warehouseService() {
            final WarehouseService mock = Mockito.mock(WarehouseService.class);
            Mockito.doNothing().when(mock).init();
            return mock;
        }

        @Bean
        StatusProcessorService statusProcessorService() {
            return Mockito.mock(StatusProcessorService.class);
        }

        @Bean
        PutAwayZoneDao putAwayZoneDao() {
            return Mockito.mock(PutAwayZoneDao.class);
        }

        @Bean
        ProcessTypeClickHouseService processTypeClickHouseService() {
            return Mockito.mock(ProcessTypeClickHouseService.class);
        }

        @Bean
        ProcessTypeClickHouseMapper processTypeClickHouseMapper() {
            return Mockito.mock(ProcessTypeClickHouseMapper.class);
        }

        @Bean
        AchievementMetricService achievementMetricService() {
            return Mockito.mock(AchievementMetricService.class);
        }

        @Bean
        SettingsProvider<Settings> settingsProvider() {
            return new MockSettingsProvider<>(new Settings());
        }

    }

    @BeforeEach
    void init() {
        Mockito.reset(historyDao, currentStateDao, warehouseService);
    }

    @Test
    void changeProcessTypeWhenCurrentStateEmpty() {

        EmployeeProcessTypeRequest request = EmployeeProcessTypeRequest.builder()
                .user("test")
                .processType(ProcessType.PLACEMENT)
                .putAwayZoneName(null)
                .assigmentType(AssigmentType.SYSTEM)
                .assigner("assigner")
                .build();

        EmployeeProcessTypeModel model = EmployeeProcessTypeModel.builder()
                .warehouseId(1L)
                .user("test")
                .processType(ProcessType.PLACEMENT)
                .putAwayZoneId(null)
                .assigmentType(AssigmentType.SYSTEM)
                .assigner("assigner")
                .build();

        Mockito.when(warehouseService.readWarehouse(ArgumentMatchers.eq("sof")))
                .thenReturn(WarehouseModel.builder()
                        .id(1L)
                        .build());

        Mockito.doNothing().when(historyDao).save(model);

        Mockito.doNothing().when(currentStateDao).insert(model);

        Mockito.when(currentStateDao.findByUserName(
                        ArgumentMatchers.eq(1L), ArgumentMatchers.eq("test")))
                .thenReturn(Optional.empty());

        employeeProcessTypeService.changeProcessType("sof", request);

        Assertions.assertAll(
                //() -> Mockito.verify(historyDao, Mockito.times(1)).save(1L, model),
                () -> Mockito.verify(currentStateDao, Mockito.times(1)).insert(model)
        );
    }

    @Test
    void changeProcessTypeWhenCurrentStateAfter() {

        EmployeeProcessTypeRequest request = EmployeeProcessTypeRequest.builder()
                .user("test")
                .processType(ProcessType.PLACEMENT)
                .putAwayZoneName(null)
                .assigmentType(AssigmentType.SYSTEM)
                .assigner("assigner")
                .eventTime(Instant.parse("2021-11-15T15:00:00Z"))
                .build();

        EmployeeProcessTypeModel model = EmployeeProcessTypeModel.builder()
                .warehouseId(1L)
                .user("test")
                .processType(ProcessType.PLACEMENT)
                .putAwayZoneId(null)
                .assigmentType(AssigmentType.SYSTEM)
                .assigner("assigner")
                .eventTime(Instant.parse("2021-11-15T15:00:00Z"))
                .build();

        Mockito.when(warehouseService.readWarehouse(ArgumentMatchers.eq("sof")))
                .thenReturn(WarehouseModel.builder()
                        .id(1L)
                        .build());

        Mockito.doNothing().when(historyDao).save(model);

        Mockito.doNothing().when(currentStateDao).insert(model);

        Mockito.when(currentStateDao.findByUserName(
                        ArgumentMatchers.eq(1L), ArgumentMatchers.eq("test")))
                .thenReturn(Optional.ofNullable(EmployeeProcessTypeModel.builder()
                        .warehouseId(1L)
                        .user("test")
                        .processType(ProcessType.PLACEMENT)
                        .putAwayZoneId(null)
                        .assigmentType(AssigmentType.SYSTEM)
                        .assigner("assigner")
                        .eventTime(Instant.parse("2021-11-15T12:00:00Z"))
                        .build()));

        employeeProcessTypeService.changeProcessType("sof", request);

        Assertions.assertAll(
                //() -> Mockito.verify(historyDao, Mockito.times(1)).save(1L, model),
                () -> Mockito.verify(currentStateDao, Mockito.times(1)).update(model),
                () -> Mockito.verify(currentStateDao, Mockito.times(0)).insert(model)
        );
    }

    @Test
    void changeProcessTypeWhenCurrentStateBefore() {

        EmployeeProcessTypeRequest request = EmployeeProcessTypeRequest.builder()
                .user("test")
                .processType(ProcessType.PLACEMENT)
                .putAwayZoneName(null)
                .assigmentType(AssigmentType.SYSTEM)
                .assigner("assigner")
                .eventTime(Instant.parse("2021-11-15T12:00:00Z"))
                .build();

        EmployeeProcessTypeModel model = EmployeeProcessTypeModel.builder()
                .warehouseId(1L)
                .user("test")
                .processType(ProcessType.PLACEMENT)
                .putAwayZoneId(null)
                .assigmentType(AssigmentType.SYSTEM)
                .assigner("assigner")
                .eventTime(Instant.parse("2021-11-15T12:00:00Z"))
                .build();

        Mockito.when(warehouseService.readWarehouse(ArgumentMatchers.eq("sof")))
                .thenReturn(WarehouseModel.builder()
                        .id(1L)
                        .build());

        Mockito.doNothing().when(historyDao).save(model);

        Mockito.doNothing().when(currentStateDao).insert(model);

        Mockito.when(currentStateDao.findByUserName(
                        ArgumentMatchers.eq(1L), ArgumentMatchers.eq("test")))
                .thenReturn(Optional.ofNullable(EmployeeProcessTypeModel.builder()
                        .warehouseId(1L)
                        .user("test")
                        .processType(ProcessType.PLACEMENT)
                        .eventTime(Instant.parse("2021-11-15T10:00:00Z"))
                        .putAwayZoneId(null)
                        .assigmentType(AssigmentType.SYSTEM)
                        .assigner("assigner")
                        .build()));

        employeeProcessTypeService.changeProcessType("sof", request);

        Assertions.assertAll(
                //() -> Mockito.verify(historyDao, Mockito.times(1)).save(1L, model),
                () -> Mockito.verify(currentStateDao, Mockito.times(1)).update(model),
                () -> Mockito.verify(currentStateDao, Mockito.times(0)).insert(model)
        );
    }
}
