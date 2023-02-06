package ru.yandex.market.tpl.integration.tests.stress.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.qameta.allure.Step;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ru.yandex.market.tpl.api.model.manual.CreateDeliveryTasksRequest;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.api.model.user.partner.PartnerUserDto;
import ru.yandex.market.tpl.api.model.user.partner.PartnerUserRoutingPropertiesDto;
import ru.yandex.market.tpl.common.util.RandomUtil;
import ru.yandex.market.tpl.integration.tests.client.ManualApiClient;
import ru.yandex.market.tpl.integration.tests.client.PartnerApiClient;
import ru.yandex.market.tpl.integration.tests.client.PublicApiClient;
import ru.yandex.market.tpl.integration.tests.configuration.AutoTestsConfiguration;
import ru.yandex.market.tpl.integration.tests.context.AutoTestContextHolder;
import ru.yandex.market.tpl.integration.tests.facade.ApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.ManualApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.PublicApiFacade;
import ru.yandex.market.tpl.integration.tests.stress.StressStat;
import ru.yandex.market.tpl.integration.tests.stress.StressStatCsv;
import ru.yandex.market.tpl.integration.tests.stress.shooter.ConstStressShooter;
import ru.yandex.market.tpl.integration.tests.stress.shooter.StepwiseStressShooter;
import ru.yandex.market.tpl.integration.tests.stress.shooter.stat.ShootingResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.integration.tests.stress.StressTestsUtil.currentCourierEmail;

@SpringBootTest(classes = AutoTestsConfiguration.class)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Log4j2
public class PublicApiMixedGetStressTest {
    private static final Integer COURIERS_COUNT = 5;
    private static final Integer ORDERS_PER_COURIER_COUNT = 1;
    private final ApiFacade apiFacade;
    private final ManualApiClient manualApiClient;
    private final ManualApiFacade manualApiFacade;
    private final PublicApiFacade publicApiFacade;
    private final PartnerApiClient partnerApiClient;
    private final StepwiseStressShooter stressShooter = new ConstStressShooter(3, 90);
    private final StressStat stressStat;
    private final PublicApiClient publicApiClient;
    private ShootingResult shootingResult = new ShootingResult();
    private Map<String, Long> usersEmailUidMap;
    private List<String> externalOrderIds = new ArrayList<>(COURIERS_COUNT * ORDERS_PER_COURIER_COUNT);
    private List<String> emails;

    @BeforeEach
    void before() {
        this.usersEmailUidMap = IntStream.rangeClosed(1, COURIERS_COUNT)
                .boxed()
                .collect(Collectors.toMap(i -> "stress-test-user-" + i + "@yandex.ru", i -> 1_000_000_000L + i));
        this.emails = new ArrayList<>(usersEmailUidMap.keySet());

        manualApiClient.deleteCouriers(emails);
        stressStat.clear();

        List<CreateDeliveryTasksRequest.CreateDeliveryTask> allCreateDeliveryTasks = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger(0);
        emails.forEach(email -> {
            createCourierContext(email, counter.getAndIncrement());
            var createDeliveryTasks = manualApiFacade.generateCreateDeliveryTasks(ORDERS_PER_COURIER_COUNT);
            allCreateDeliveryTasks.addAll(createDeliveryTasks);
        });
        List<String> extOrderIds = allCreateDeliveryTasks.stream()
                .map(CreateDeliveryTasksRequest.CreateDeliveryTask::getExternalOrderId)
                .collect(Collectors.toList());
        manualApiClient.deleteOrders(extOrderIds);
        List<String> ids = manualApiFacade.createManyDeliveryTasks(allCreateDeliveryTasks).getExternalOrderIds();
        this.externalOrderIds.addAll(ids);

        emails.stream().parallel().forEach(email -> {
            currentCourierEmail(email);
            AutoTestContextHolder.getContext().setCourierTkn("some-token");
            apiFacade.pickupOrders();
            publicApiFacade.successCallToRecipient();
        });

    }

    @AfterEach
    void after() {
        try {
            manualApiClient.deleteCouriers(emails);
            manualApiClient.deleteOrders(externalOrderIds);
            new StressStatCsv(stressStat).statByRequestsCsv();
            AutoTestContextHolder.clearContext();
        } finally {
            shootingResult.printResult();
        }
    }

    @Test
    @DisplayName(value = "Тест нагрузки на GET-ручки в tpl-api")
    @EnabledIfSystemProperty(named = "stress.tests.enabled", matches = "true")
    void test() {
        List<Runnable> actions = IntStream.rangeClosed(1, stressShooter.maxActionsCount())
                .mapToObj(i -> (Runnable) () -> {
                    String email = RandomUtil.getRandomFromList(emails);
                    log.debug("Choosed email: {}", email);
                    currentCourierEmail(email);
                    var userShift = publicApiClient.getCurrentUserShift();
                    assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.ON_TASK);
                    Long currentRoutePointId = userShift.getCurrentRoutePointId();
                    assertThat(currentRoutePointId).isNotNull();
                    var currentRoutePoint = publicApiClient.getRoutePoint(currentRoutePointId);
                    assertThat(currentRoutePoint.getType()).isEqualTo(RoutePointType.DELIVERY);
                    long taskId = currentRoutePoint.getTasks().iterator().next().getId();
                    publicApiClient.getRoutePoints();
                    publicApiClient.getOrderDeliveryTask(taskId);
                    publicApiClient.getOrderDeliveryTasks();
                })
                .collect(Collectors.toList());
        stressStat.clear();
        this.shootingResult = stressShooter.shoot(actions);

        shootingResult.checkRps();
        stressStat.checkAllFinished();
        shootingResult.checkAllFinishedSuccessfully();

        Map<String, StressStat> stressStatMap = stressStat.splitByEndpoints();
        StressStat getUserShiftStressStat = stressStatMap.get("GET /api/shifts/current");
        StressStat getCurrentRoutePointStressStat = stressStatMap.get("GET /api/route-points/{}");
        Long percentile07Duration = getUserShiftStressStat.getPercentileDuration(0.7);
        assertThat(percentile07Duration)
                .withFailMessage("Тайминг GET /api/shifts/current 0.70 перцентиля выше 300ms: %dms",
                        percentile07Duration)
                .isLessThan(300);
        Long percentile09Duration = getUserShiftStressStat.getPercentileDuration(0.9);
        assertThat(percentile09Duration)
                .withFailMessage("Тайминг GET /api/shifts/current 0.90 перцентиля выше 500ms: %dms",
                        percentile09Duration)
                .isLessThan(500);


        Long percentile07Duration2 = getCurrentRoutePointStressStat.getPercentileDuration(0.7);
        assertThat(percentile07Duration2)
                .withFailMessage("Тайминг GET /api/route-points/{} 0.70 перцентиля выше 1sec: %dms",
                        percentile07Duration2)
                .isLessThan(1000);
        Long percentile09Duration2 = getCurrentRoutePointStressStat.getPercentileDuration(0.9);
        assertThat(percentile09Duration2)
                .withFailMessage("Тайминг GET /api/route-points/{} 0.90 перцентиля выше 2sec: %dms",
                        percentile09Duration2)
                .isLessThan(2000);
    }

    @Step("Создание курьера, расписания, смены. email: {email}")
    private void createCourierContext(String email, Integer counter) {
        currentCourierEmail(email);
        AutoTestContextHolder.getContext().setCourierTkn("some-token");
        Long uid = usersEmailUidMap.get(email);
        PartnerUserRoutingPropertiesDto routingProperties = new PartnerUserRoutingPropertiesDto();
        routingProperties.setDeliveryPhotoEnabled(true);
        routingProperties.setRerouteEnabled(false);
        PartnerUserDto courier = manualApiClient.createCourier(email, uid, true, routingProperties, counter.toString());
        AutoTestContextHolder.getContext().setUserId(courier.getId());
        AutoTestContextHolder.getContext().setUid(courier.getUid());
        partnerApiClient.createCourierSchedule(courier.getId());
        manualApiFacade.createShift();
        manualApiFacade.createUserShift();
    }
}
