package ru.yandex.market.wms.timetracker.dao.postgres;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import cz.jirutka.rsql.parser.RSQLParser;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import ru.yandex.market.wms.timetracker.config.DbConfiguration;
import ru.yandex.market.wms.timetracker.config.TtsTestConfig;
import ru.yandex.market.wms.timetracker.model.SystemActivityModel;
import ru.yandex.market.wms.timetracker.model.enums.EmployeeStatus;
import ru.yandex.market.wms.timetracker.service.WarehouseTimeZoneConvertorService;
import ru.yandex.market.wms.timetracker.specification.SystemActivitySpecification;
import ru.yandex.market.wms.timetracker.specification.rsql.Filter;
import ru.yandex.market.wms.timetracker.specification.rsql.RecursivePredicateBuilder;
import ru.yandex.market.wms.timetracker.specification.rsql.SearchOperators;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        SystemActivityDaoImpl.class,
        RecursivePredicateBuilder.class
})
@Import({
        TtsTestConfig.class,
        DbConfiguration.class,
        SystemActivityDaoImplTest.TestConfig.class
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
class SystemActivityDaoImplTest {

    @TestConfiguration
    public static class TestConfig {
        @Bean
        WarehouseTimeZoneConvertorService warehouseTimeZoneConvertorService() {
            return Mockito.mock(WarehouseTimeZoneConvertorService.class);
        }
    }

    @Autowired
    private SystemActivityDaoImpl systemActivityDao;

    @Autowired
    private WarehouseTimeZoneConvertorService warehouseTimeZoneConvertorService;

    @Test
    @DatabaseSetup(value = "/repository/system-activity-dao-impl/find-all-before.xml")
    void findAll() {
        SystemActivityModel expectedTest = SystemActivityModel.builder()
                .id(1L)
                .warehouseId(1L)
                .user("test")
                .process(EmployeeStatus.CONSOLIDATION)
                .assigner("assigner")
                .createTime(Instant.parse("2021-12-16T15:00:00Z"))
                .userStartedActivity(false)
                .build();

        SystemActivityModel expectedSecond = SystemActivityModel.builder()
                .id(2L)
                .warehouseId(1L)
                .user("second")
                .process(EmployeeStatus.CONSOLIDATION)
                .assigner("assigner")
                .createTime(Instant.parse("2021-12-16T15:00:00Z"))
                .expectedEndTime(Instant.parse("2021-12-16T18:00:00Z"))
                .userStartedActivity(false)
                .build();

        final List<SystemActivityModel> result = systemActivityDao.findAll(1L, null, null);
        Assertions.assertAll(
                () -> Assertions.assertEquals(2, result.size()),
                () -> MatcherAssert.assertThat(result, Matchers.hasItem(expectedTest))
        );
    }


    @Test
    @DatabaseSetup(value = "/repository/system-activity-dao-impl/find-all-before.xml")
    void findAllWithFilter() {

        SystemActivityModel expectedSecond = SystemActivityModel.builder()
                .id(2L)
                .warehouseId(1L)
                .user("second")
                .process(EmployeeStatus.CONSOLIDATION)
                .assigner("assigner")
                .createTime(Instant.parse("2021-12-16T15:00:00Z"))
                .expectedEndTime(Instant.parse("2021-12-16T18:00:00Z"))
                .userStartedActivity(false)
                .build();

        RSQLParser parser = new RSQLParser(SearchOperators.OPERATORS);

        Filter<SystemActivityModel> filter = Filter.of(
                "userName=='second';process=='CONSOLIDATION';assigner=='assigner';create_time=='2021-12-16 15:00:00';" +
                        "expected_end_time=='2021-12-16 18:00:00';user_started_activity=='false'", parser,
                new SystemActivitySpecification(1L, warehouseTimeZoneConvertorService));

        Mockito.when(warehouseTimeZoneConvertorService.toUtc(ArgumentMatchers.eq(1L),
                        ArgumentMatchers.eq(LocalDateTime.parse("2021-12-16T15:00:00"))))
                .thenReturn(LocalDateTime.parse("2021-12-16T15:00:00"));

        Mockito.when(warehouseTimeZoneConvertorService.toUtc(ArgumentMatchers.eq(1L),
                        ArgumentMatchers.eq(LocalDateTime.parse("2021-12-16T18:00:00"))))
                .thenReturn(LocalDateTime.parse("2021-12-16T18:00:00"));

        final List<SystemActivityModel> result = systemActivityDao.findAll(1L, filter, null);
        Assertions.assertAll(
                () -> Assertions.assertEquals(1, result.size()),
                () -> MatcherAssert.assertThat(result, Matchers.containsInAnyOrder(expectedSecond))
        );
    }

    @Test
    void findByNaturalId() {
    }

    @Test
    @DatabaseSetup(value = "/repository/system-activity-dao-impl/empty.xml")
    @ExpectedDatabase(
            value = "/repository/system-activity-dao-impl/after-insert.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void insert() {
        List<SystemActivityModel> models = List.of(SystemActivityModel.builder()
                        .warehouseId(1L)
                        .user("test")
                        .process(EmployeeStatus.CONSOLIDATION)
                        .assigner("assigner")
                        .createTime(Instant.parse("2021-12-16T15:00:00Z"))
                        .userStartedActivity(false)
                        .build(),
                SystemActivityModel.builder()
                        .warehouseId(1L)
                        .user("second")
                        .process(EmployeeStatus.CONSOLIDATION)
                        .assigner("assigner")
                        .createTime(Instant.parse("2021-12-16T15:00:00Z"))
                        .expectedEndTime(Instant.parse("2021-12-16T18:00:00Z"))
                        .userStartedActivity(false)
                        .build());

        systemActivityDao.insert(models);
    }

    @Test
    @DatabaseSetup(value = "/repository/system-activity-dao-impl/find-all-before.xml")
    void deleteByWarehouseIdAndUserName() {
        int sizeBefore = systemActivityDao.findAll(1L, null, null).size();
        systemActivityDao.deleteByWarehouseIdAndUserName(1L, List.of("second", "test"));
        int size = systemActivityDao.findAll(1L, null, null).size();
        Assertions.assertAll(
                () -> Assertions.assertEquals(2, sizeBefore),
                () -> Assertions.assertEquals(0, size)
        );
    }
}
