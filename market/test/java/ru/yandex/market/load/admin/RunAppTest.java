package ru.yandex.market.load.admin;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.repository.CrudRepository;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.javaframework.main.config.SpringApplicationConfig;
import ru.yandex.market.load.admin.configs.MockClientsConfig;
import ru.yandex.market.load.admin.converter.in.ShootingConfigConverterIn;
import ru.yandex.market.load.admin.dao.ProjectDao;
import ru.yandex.market.load.admin.dao.ShootingConfigDao;
import ru.yandex.market.load.admin.entity.Project;
import ru.yandex.market.load.admin.entity.ShootingConfig;
import ru.yandex.market.load.admin.entity.ShootingConfigValue;
import ru.yandex.market.load.admin.entity.ShootingPlan;
import ru.yandex.market.load.admin.service.ShootingService;
import ru.yandex.market.load.admin.service.ShootingServiceTest;
import ru.yandex.market.load.admin.service.ShootingStatus;

@Log4j2
@Disabled
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        classes = {
                SpringApplicationConfig.class
        }
)
@Import(MockClientsConfig.class)
@ContextConfiguration(classes = {MockClientsConfig.class})
public class RunAppTest extends AbstractFunctionalTest {
    @Autowired
    private ShootingConfigDao configDao;
    @Autowired
    private ShootingService shootingService;
    @Autowired
    private ProjectDao projectDao;
    @Autowired
    private List<CrudRepository<?, ?>> cruds;

    @BeforeEach
    public void setUp() {
        cruds.forEach(CrudRepository::deleteAll);
    }

    @Test
    public void runApp() throws InterruptedException, JsonProcessingException {
        createMarketApiProject();
        createOneMoreProject();
        ShootingConfig config = createTestConfig();
        createTestConfigForMini();

        ShootingPlan plan = ShootingPlan.builder()
                .configId(config.getId())
                .ordersPerHour(3600)
                .durationSeconds(10)
                .coinsPerHour(100)
                .createdAt(Timestamp.from(Instant.now()))
                .startTime(Timestamp.from(Instant.now().plus(Duration.ofDays(1))))
                .operator("user-name")
                .build();
        plan = shootingService.planShooting(plan);
        shootingService.startShooting(plan.getId());
        shootingService.applyStatus(plan.getId(), ShootingStatus.IN_PROGRESS);
        Thread.sleep(Integer.MAX_VALUE);
    }

    private void createMarketApiProject() {
        Project project = Project.builder().build();
        project.setDescription("test-desc");
        project.setTitle("marketapi");
        project.setAbcRoles(Collections.singletonList("role"));
        project.setAbcServices(Collections.singletonList("marketapi"));
        projectDao.save(project);
    }

    private void createOneMoreProject() {
        final Project project = Project.builder()
                .abcServices(Collections.singletonList("test_abc_service"))
                .abcRoles(Collections.singletonList("test_abc_role"))
                .description("todo")
                .title("one more project")
                .build();
        projectDao.save(project);
    }

    private ShootingConfig createTestConfig() throws JsonProcessingException {
        ShootingConfig config = new ShootingConfig();
        config.setTitle("default");
        config.setDescription("config description (parameters)");
        config.setProjectId(findMarketapiProject().getId());
        config.setValue(ShootingConfigConverterIn.YAML_MAPPER
                .writeValueAsString(ShootingServiceTest.SHOOTING_CONFIG_VALUE));
        config.setModifiedAt(Timestamp.from(Instant.now()));
        return configDao.save(config);
    }

    private ShootingConfig createTestConfigForMini() throws JsonProcessingException {
        ShootingConfig config = new ShootingConfig();
        config.setTitle("mini");
        config.setDescription("config description (parameters)");
        config.setProjectId(findMarketapiProject().getId());
        ShootingConfigValue configValue = copyConfigValue(ShootingServiceTest.SHOOTING_CONFIG_VALUE);
        configValue.setOrdersPerHour(3600L);
        configValue.setCoinsPerHour(0L);
        configValue.setDurationSeconds(10L);
        config.setValue(ShootingConfigConverterIn.YAML_MAPPER.writeValueAsString(configValue));
        config.setModifiedAt(Timestamp.from(Instant.now()));
        return configDao.save(config);
    }

    private Project findMarketapiProject() {
        for (Project project : projectDao.findAll()) {
            if (project.getAbcServices().contains("marketapi")) {
                return project;
            }
        }
        return null;
    }

    private ShootingConfigValue copyConfigValue(ShootingConfigValue shootingConfigValue) {
        try {
            final String rawConfig = ShootingConfigConverterIn.YAML_MAPPER.writeValueAsString(shootingConfigValue);
            return ShootingConfigConverterIn.YAML_MAPPER.readValue(rawConfig, ShootingConfigValue.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
