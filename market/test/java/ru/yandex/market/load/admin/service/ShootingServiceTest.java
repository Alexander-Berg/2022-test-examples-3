package ru.yandex.market.load.admin.service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.load.admin.AbstractFunctionalTest;
import ru.yandex.market.load.admin.client.SimplifiedTsumApiClient;
import ru.yandex.market.load.admin.configs.MockClientsConfig;
import ru.yandex.market.load.admin.configs.MockClientsConfig.IssuesTracker;
import ru.yandex.market.load.admin.converter.in.ShootingConfigConverterIn;
import ru.yandex.market.load.admin.dao.ProjectDao;
import ru.yandex.market.load.admin.dao.ShootingConfigDao;
import ru.yandex.market.load.admin.dao.ShootingDataDao;
import ru.yandex.market.load.admin.dao.ShootingInfoDao;
import ru.yandex.market.load.admin.dao.ShootingPlanDao;
import ru.yandex.market.load.admin.entity.Project;
import ru.yandex.market.load.admin.entity.ShootingConfig;
import ru.yandex.market.load.admin.entity.ShootingConfigValue;
import ru.yandex.market.load.admin.entity.ShootingData;
import ru.yandex.market.load.admin.entity.ShootingInfo;
import ru.yandex.market.load.admin.entity.ShootingPlan;
import ru.yandex.market.load.admin.tsum.resources.LoyaltyShootingOptions;
import ru.yandex.market.load.admin.tsum.resources.PandoraCheckouterConfig;
import ru.yandex.market.load.admin.tsum.resources.PandoraRegionSpecificConfig;
import ru.yandex.market.load.admin.tsum.resources.PandoraTankConfig;
import ru.yandex.market.load.admin.tsum.resources.PerTankShootingOptions;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.Transition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;

@Log4j2
@Import(MockClientsConfig.class)
@ContextConfiguration(classes = {MockClientsConfig.class})
public class ShootingServiceTest extends AbstractFunctionalTest {
    public static final ShootingConfigValue SHOOTING_CONFIG_VALUE = ShootingConfigValue.builder()
            .pipeId("load-test-production-dev")
            .arcadiaArcBranchRef("arcadia:/arc/trunk/arcadia")
            .stocksRequiredRate(1.0)
            .preferredOphPerTank(30_000)
            .loyaltyShootingOptions(LoyaltyShootingOptions.builder()
                    .percentOfFlashOrders(0)
                    .percentOfCashbackOrders(0)
                    .percentOfOrdersPaidByCoins(0)
                    .percentOfOrdersUsingPromo(0)
                    .coinsPromoId(0)
                    .build())
            .checkouterConfig(PandoraCheckouterConfig.builder()
                    .cartRepeats(2)
                    .offersDistribution("[{'offersCount': 1, 'ordersDistribution': 1}]")
                    .cartsDistribution("[{'internalCarts':1, 'ordersDistribution': 1}]")
                    .cartDurationSec(1)
                    .handlesCommonDelayMs(100)
                    .handles("handles")
                    .build())
            .perTankShootingOptions(Arrays.asList(PerTankShootingOptions.builder()
                    .checkouterConfig(PandoraCheckouterConfig.builder()
                            .balancer("http://checkouter.tst.vs.market.yandex.net:39001")
                            .build())
                    .tankConfig(PandoraTankConfig.builder()
                            .tankBaseUrl("http://tank01ht.market.yandex.net:8083")
                            .build())
                    .regionSpecificConfig(PandoraRegionSpecificConfig.builder()
                            .regionId("11030")
                            .deliveryServices("1005429")
                            .warehouseId("147")
                            .build())
                    .build()))
            .build();
    ;
    @Autowired
    private ShootingService shootingService;
    @Autowired
    private ProjectDao projectDao;
    @Autowired
    private ShootingDataDao shootingDataDao;
    @Autowired
    private ShootingConfigDao configDao;
    @Autowired
    private ShootingInfoDao shootingInfoDao;
    @Autowired
    private ShootingPlanDao shootingPlanDao;
    @Autowired
    private Issues startrekIssues;
    @Autowired
    private IssuesTracker issuesTracker;
    @Autowired
    private SimplifiedTsumApiClient tsumApiClient;

    @AfterEach
    public void tearDown() {
        issuesTracker.getStore().clear();
        shootingInfoDao.deleteAll();
        shootingPlanDao.deleteAll();
        configDao.deleteAll();
        projectDao.deleteAll();
    }

    @Test
    public void openToFinishOk() throws JsonProcessingException {
        ShootingData shooting = createShooting();
        long infoId = shooting.getInfo().getId();
        long planId = shooting.getPlan().getId();

        InOrder inOrder = Mockito.inOrder(issuesTracker);
        shootingService.startShooting(planId);
        verifyDeploying(inOrder, planId);

        Issue issue = issuesTracker.getStore().entrySet().iterator().next().getValue();
        inOrder = Mockito.inOrder(issue, startrekIssues, tsumApiClient);

        shootingService.applyStatus(infoId, ShootingStatus.IN_PROGRESS, null);
        verifyInProgress(inOrder, planId);

        shootingService.applyStatus(infoId, ShootingStatus.WAITING_ORDERS);
        verifyWaitingOrders(inOrder, planId);

        shootingService.applyStatus(infoId, ShootingStatus.CANCELLING_ORDERS);
        verifyCancellingOrders(inOrder, planId);

        shootingService.applyStatus(infoId, ShootingStatus.SUCCESS);
        verifySuccess(inOrder, planId);
    }

    private void verifyDeploying(InOrder inOrder, long planId) {
        inOrder.verify(issuesTracker, Mockito.timeout(3000)).onSave();
        Issue issue = issuesTracker.getStore().entrySet().iterator().next().getValue();
        assertEquals(ShootingStatus.DEPLOYING, shootingDataDao.findByShootingPlanId(planId).getStatus());
        assertEquals(ShootingTicketStatus.OPEN, ShootingTicketStatus
                .fromIssueKey(issue.getStatus().getKey()));
    }

    private void verifyInProgress(InOrder inOrder, long planId) {
        ShootingInfo info = shootingInfoDao.findByPlanId(planId);
        Issue issue = startrekIssues.get(info.getTicket());
        inOrder.verify(issue, Mockito.timeout(2000))
                .executeTransition(any(Transition.class));
        assertEquals(ShootingStatus.IN_PROGRESS, shootingDataDao.findByShootingPlanId(planId).getStatus());
        inOrder.verify(issue, times(1))
                .comment(anyString());
        assertEquals(ShootingTicketStatus.IN_PROGRESS, ShootingTicketStatus
                .fromIssueKey(issue.getStatus().getKey()));
        inOrder.verify(tsumApiClient, Mockito.timeout(2000))
                .launchPipeline(anyString(), anyList());
    }

    private void verifyWaitingOrders(InOrder inOrder, long planId) {
        Issue issue = startrekIssues.get(shootingInfoDao.findByPlanId(planId).getTicket());
        assertEquals(ShootingStatus.WAITING_ORDERS, shootingDataDao.findByShootingPlanId(planId).getStatus());
        inOrder.verify(issue, timeout(2000))
                .comment(anyString());
        assertEquals(ShootingTicketStatus.IN_PROGRESS, ShootingTicketStatus
                .fromIssueKey(issue.getStatus().getKey()));
    }

    private void verifyCancellingOrders(InOrder inOrder, long planId) {
        Issue issue = startrekIssues.get(shootingInfoDao.findByPlanId(planId).getTicket());
        assertEquals(ShootingStatus.CANCELLING_ORDERS, shootingDataDao.findByShootingPlanId(planId).getStatus());
        inOrder.verify(issue, timeout(2000).times(2))
                .comment(anyString());
        assertEquals(ShootingTicketStatus.IN_PROGRESS, ShootingTicketStatus
                .fromIssueKey(issue.getStatus().getKey()));
    }

    private void verifySuccess(InOrder inOrder, long planId) {
        Issue issue = startrekIssues.get(shootingInfoDao.findByPlanId(planId).getTicket());
        assertEquals(ShootingStatus.SUCCESS, shootingDataDao.findByShootingPlanId(planId).getStatus());
        inOrder.verify(issue, Mockito.timeout(2000))
                .executeTransition(any(Transition.class));
        assertEquals(ShootingTicketStatus.CLOSED, ShootingTicketStatus
                .fromIssueKey(issue.getStatus().getKey()));
        inOrder.verify(tsumApiClient, Mockito.timeout(2000))
                .launchPipeline(anyString(), anyList());
    }

    private ShootingData createShooting() throws JsonProcessingException {
        Project project = Project.builder()
                .title("test title")
                .description("test description")
                .abcRoles(Collections.singletonList("roles"))
                .abcServices(Collections.singletonList("service"))
                .build();
        project = projectDao.save(project);
        ShootingConfig config = new ShootingConfig();
        config.setProjectId(project.getId());
        config.setTitle("config title");
        config.setDescription("config description (parameters)");
        config.setValue(ShootingConfigConverterIn.YAML_MAPPER.writeValueAsString(SHOOTING_CONFIG_VALUE));
        config.setModifiedAt(Timestamp.from(Instant.now()));
        config = configDao.save(config);

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
        return shootingDataDao.findByShootingPlanId(plan.getId());
    }
}
