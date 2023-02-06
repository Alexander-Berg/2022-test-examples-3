package ru.yandex.market.mbi.tms.monitor;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.junit.JupiterDbUnitTest;
import ru.yandex.market.core.database.EmbeddedPostgresConfig;
import ru.yandex.market.core.database.PreserveDictionariesDbUnitDataSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тестовый класс для {@link DBExecutorInfoService}.
 */
@SpringJUnitConfig(classes = EmbeddedPostgresConfig.class)
@PreserveDictionariesDbUnitDataSet
class DBExecutorInfoServiceTest extends JupiterDbUnitTest {
    private static final String GET_ALL_SQL = "select " +
            "executor_component, executor_name, criticality, mbi_team " +
            "from mbi_core.executor_info";

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ExecutorInfoService rgExecutorInfoService;

    private ExecutorInfoService billingExecutorInfoService;

    private static void saveAllComponents(ExecutorInfoService service, Set<String> componentNames) {
        for (var componentName : componentNames) {
            var executorInfo = new ExecutorInfo(MbiComponent.RG, componentName, MonitoringCriticality.CRIT_ALWAYS,
                    MbiTeam.BILLING);
            saveInfo(service, executorInfo);
        }
    }

    private static void saveInfo(ExecutorInfoService service, ExecutorInfo executorInfo) {
        service.save(executorInfo.getExecutorName(), executorInfo.getCriticality(), executorInfo.getMbiTeam());
    }

    @BeforeEach
    void setUp() {
        rgExecutorInfoService = new DBExecutorInfoService(
                MbiComponent.RG,
                namedParameterJdbcTemplate,
                transactionTemplate
        );
        billingExecutorInfoService = new DBExecutorInfoService(
                MbiComponent.BILLING,
                namedParameterJdbcTemplate,
                transactionTemplate
        );
    }

    @Test
    void save() {
        assertThat(getAllComponents()).isEmpty();
        var info = new ExecutorInfo(MbiComponent.RG, "myAwesomeExecutor", MonitoringCriticality.CRIT_ALWAYS,
                MbiTeam.BILLING);
        saveInfo(rgExecutorInfoService, info);
        assertThat(getAllComponents()).containsExactly(info);
    }

    @Test
    void saveTwiceSameExecutor() {
        var info = new ExecutorInfo(MbiComponent.RG, "myAwesomeExecutor", MonitoringCriticality.CRIT_ALWAYS,
                MbiTeam.BILLING);
        saveInfo(rgExecutorInfoService, info);
        saveInfo(rgExecutorInfoService, info);

        assertThat(getAllComponents()).containsExactly(info);
    }

    @Test
    void saveSameExecutorAtTwoDiffServices() {
        var rgInfo = new ExecutorInfo(MbiComponent.RG, "myAwesomeExecutor",
                MonitoringCriticality.CRIT_ALWAYS, MbiTeam.BILLING);
        var billingInfo = new ExecutorInfo(MbiComponent.BILLING, "myAwesomeExecutor",
                MonitoringCriticality.CRIT_ALWAYS, MbiTeam.BILLING);

        saveInfo(rgExecutorInfoService, rgInfo);
        saveInfo(billingExecutorInfoService, billingInfo);

        var infoComparator = Comparator
                .comparing(ExecutorInfo::getMbiTeam)
                .thenComparing(ExecutorInfo::getExecutorName);

        var actual = getAllComponents();
        actual.sort(infoComparator);

        var expected = Arrays.asList(rgInfo, billingInfo);
        expected.sort(infoComparator);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void update() {
        var executorName = "myAwesomeExecutor";
        var info = new ExecutorInfo(MbiComponent.RG, executorName, MonitoringCriticality.CRIT_ALWAYS,
                MbiTeam.BILLING);
        saveInfo(rgExecutorInfoService, info);

        var updated = rgExecutorInfoService.update(executorName, MonitoringCriticality.IGNORED);
        assertThat(updated).isTrue();

        var data = getAllComponents();
        assertThat(data).hasSize(1);

        var executorInfo = data.get(0);
        assertThat(executorInfo.getExecutorName()).isEqualTo(executorName);
        assertThat(executorInfo.getCriticality()).isEqualTo(MonitoringCriticality.IGNORED);
    }

    @Test
    void updateUnExistedExecutor() {
        assertThat(getAllComponents()).isEmpty();
        assertThat(rgExecutorInfoService.update("myAwesomeExecutor", MonitoringCriticality.CRIT_ALWAYS)).isFalse();
        assertThat(getAllComponents()).isEmpty();
    }

    @Test
    void retainAll() {
        var executorNames = Set.of("myAwesomeExecutor1", "myAwesomeExecutor2");
        var obsoleteExecutorNames = Set.of("myObsoleteExecutor1", "myObsoleteExecutor2");
        var allNames = Stream.concat(
                executorNames.stream(),
                obsoleteExecutorNames.stream()
        ).collect(Collectors.toSet());

        for (var executorName : allNames) {
            var executorInfo = new ExecutorInfo(MbiComponent.RG, executorName,
                    MonitoringCriticality.CRIT_ALWAYS, MbiTeam.BILLING);
            saveInfo(rgExecutorInfoService, executorInfo);
        }
        assertThat(getAllComponentNames()).isEqualTo(allNames);

        rgExecutorInfoService.retainAll(executorNames);
        assertThat(getAllComponentNames()).isEqualTo(executorNames);
    }

    @Test
    void retainAllNotDeleteComponentsOfAnotherService() {
        var rgExecutorNames = Set.of("myAwesomeExecutor1", "myAwesomeExecutor2");
        saveAllComponents(rgExecutorInfoService, rgExecutorNames);

        var billingObsoleteExecutorNames = Set.of("myObsoleteExecutor1", "myObsoleteExecutor2");
        saveAllComponents(billingExecutorInfoService, billingObsoleteExecutorNames);

        var allNames = Stream.concat(
                rgExecutorNames.stream(),
                billingObsoleteExecutorNames.stream()
        ).collect(Collectors.toSet());
        assertThat(getAllComponentNames()).isEqualTo(allNames);

        rgExecutorInfoService.retainAll(rgExecutorNames);
        assertThat(getAllComponentNames()).isEqualTo(allNames);
    }

    private Set<String> getAllComponentNames() {
        return getAllComponents().stream().map(ExecutorInfo::getExecutorName).collect(Collectors.toSet());
    }

    private List<ExecutorInfo> getAllComponents() {
        return jdbcTemplate.query(GET_ALL_SQL, DBExecutorInfoService.rowMapper());
    }
}
