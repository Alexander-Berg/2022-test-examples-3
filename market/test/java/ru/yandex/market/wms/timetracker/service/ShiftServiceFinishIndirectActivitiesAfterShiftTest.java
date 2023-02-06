package ru.yandex.market.wms.timetracker.service;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import ru.yandex.market.logistics.util.client.HttpTemplate;
import ru.yandex.market.wms.shared.libs.its.settings.provider.SettingsProvider;
import ru.yandex.market.wms.timetracker.config.DbConfiguration;
import ru.yandex.market.wms.timetracker.config.TtsTestConfig;
import ru.yandex.market.wms.timetracker.dao.SystemActivityDao;
import ru.yandex.market.wms.timetracker.dao.postgres.EmployeeStatusDao;
import ru.yandex.market.wms.timetracker.dao.postgres.EmployeeStatusHistoryDao;
import ru.yandex.market.wms.timetracker.dao.postgres.ShiftDao;
import ru.yandex.market.wms.timetracker.dao.postgres.WarehouseDao;
import ru.yandex.market.wms.timetracker.dto.enums.WarehouseName;
import ru.yandex.market.wms.timetracker.model.settings.Settings;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        WarehouseDao.class,
        ShiftDao.class,
        ShiftService.class,
        WarehouseTimeZoneConvertorService.class,
        WarehouseService.class,
})
@Import({
        TtsTestConfig.class,
        DbConfiguration.class
})
@EnableConfigurationProperties
@TestPropertySource(locations = "classpath:application-test.properties")
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        DbUnitTestExecutionListener.class
})
@DbUnitConfiguration(
        databaseConnection = {"postgresConnection"})
class ShiftServiceFinishIndirectActivitiesAfterShiftTest {

    @Autowired
    private ShiftService shiftService;

    @Autowired
    private WarehouseService warehouseService;

    @MockBean
    private HttpTemplate httpTemplate;

    @Autowired
    private ShiftDao shiftDao;

    @MockBean
    private EmployeeStatusDao employeeStatusDao;

    @MockBean
    private AchievementMetricService achievementMetricService;

    @MockBean
    private EmployeeStatusHistoryDao employeeStatusHistoryDao;

    @Autowired
    private WarehouseDao warehouseDao;

    @Autowired
    private WarehouseTimeZoneConvertorService warehouseTimeZoneConvertorService;

    @MockBean
    private SystemActivityDao systemActivityDao;

    @MockBean
    private SettingsProvider<Settings> settingsProvider;

    @Autowired
    @MockBean
    private WmsCoreClient wmsCoreClient;

    @Autowired
    @MockBean
    private WmsTaskRouterClient wmsTaskRouterClient;

    private final static Set<String> ACTIVITIES = Set.of("LUNCH", "PAUSE");

    @BeforeEach
    void init() {
        Mockito.reset(wmsCoreClient);
    }

    @Test
    @DatabaseSetup(
            value = "/repository/shift-service-finish-indirect-activities/happy-path.xml",
            connection = "postgresConnection")
    void shiftAt20ByMoscow() {
        shiftService.finishIndirectActivitiesAfterShift(Instant.parse("2022-04-05T17:00:00.00Z"));
        Mockito.verify(wmsCoreClient).completeIndirectActivity(
                List.of("sof-user1", "sof-user2", "sof-user3"), ACTIVITIES, WarehouseName.SOF);
        Mockito.verifyNoMoreInteractions(wmsCoreClient);
    }

    @Test
    @DatabaseSetup(
            value = "/repository/shift-service-finish-indirect-activities/happy-path.xml",
            connection = "postgresConnection")
    void shiftAt21ByMoscow() {
        shiftService.finishIndirectActivitiesAfterShift(Instant.parse("2022-04-05T18:00:00.00Z"));
        Mockito.verify(wmsCoreClient).completeIndirectActivity(
                List.of("sof-user4"), ACTIVITIES, WarehouseName.SOF);
        Mockito.verify(wmsCoreClient).completeIndirectActivity(
                List.of("sam-user5", "sam-user6"), ACTIVITIES, WarehouseName.SAM);
        Mockito.verifyNoMoreInteractions(wmsCoreClient);
    }

    @Test
    @DatabaseSetup(
            value = "/repository/shift-service-finish-indirect-activities/happy-path.xml",
            connection = "postgresConnection")
    void shiftAt22ByMoscow() {
        shiftService.finishIndirectActivitiesAfterShift(Instant.parse("2022-04-05T19:00:00.00Z"));
        Mockito.verify(wmsCoreClient).completeIndirectActivity(
                List.of("sam-user7"), ACTIVITIES, WarehouseName.SAM);
        Mockito.verifyNoMoreInteractions(wmsCoreClient);
    }

    @Test
    @DatabaseSetup(
            value = "/repository/shift-service-finish-indirect-activities/happy-path.xml",
            connection = "postgresConnection")
    void shiftAt23ByMoscow() {
        shiftService.finishIndirectActivitiesAfterShift(Instant.parse("2022-04-05T20:00:00.00Z"));
        Mockito.verifyNoMoreInteractions(wmsCoreClient);
    }

    @Test
    @DatabaseSetup(
            value = "/repository/shift-service-finish-indirect-activities/happy-path.xml",
            connection = "postgresConnection")
    void shiftAt01ByMoscow() {
        shiftService.finishIndirectActivitiesAfterShift(Instant.parse("2022-04-05T21:00:00.00Z"));
        Mockito.verify(wmsCoreClient).completeIndirectActivity(
                List.of("nsk-user8"), ACTIVITIES, WarehouseName.NSK);
        Mockito.verifyNoMoreInteractions(wmsCoreClient);
    }

    @Test
    @DatabaseSetup(
            value = "/repository/shift-service-finish-indirect-activities/happy-path.xml",
            connection = "postgresConnection")
    void shiftAt02ByMoscow() {
        shiftService.finishIndirectActivitiesAfterShift(Instant.parse("2022-04-05T22:00:00.00Z"));
        Mockito.verify(wmsCoreClient).completeIndirectActivity(
                List.of("nsk-user9"), ACTIVITIES, WarehouseName.NSK);
        Mockito.verifyNoMoreInteractions(wmsCoreClient);
    }

}
