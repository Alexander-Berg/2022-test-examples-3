package ru.yandex.market.tpl.integration.tests.stress.tests;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.qameta.allure.Feature;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ru.yandex.market.tpl.api.model.routepoint.RoutePointSummaryDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskDto;
import ru.yandex.market.tpl.api.model.task.TaskDto;
import ru.yandex.market.tpl.api.model.user.partner.PartnerUserDto;
import ru.yandex.market.tpl.integration.tests.client.ManualApiClient;
import ru.yandex.market.tpl.integration.tests.client.PartnerApiClient;
import ru.yandex.market.tpl.integration.tests.client.RecipientApiClient;
import ru.yandex.market.tpl.integration.tests.configuration.AutoTestsConfiguration;
import ru.yandex.market.tpl.integration.tests.context.AutoTestContextHolder;
import ru.yandex.market.tpl.integration.tests.facade.ApiFacade;
import ru.yandex.market.tpl.integration.tests.stress.StressStat;
import ru.yandex.market.tpl.integration.tests.stress.StressStatCsv;
import ru.yandex.market.tpl.integration.tests.stress.shooter.StepwiseStressShooter;
import ru.yandex.market.tpl.integration.tests.stress.shooter.stat.ShootingResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.integration.tests.stress.StressTestsUtil.currentCourierEmail;

@SpringBootTest(classes = AutoTestsConfiguration.class)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Log4j2
public class WhereIsMyCourierStressTest {
    private final ApiFacade apiFacade;
    private final ManualApiClient manualApiClient;
    private final RecipientApiClient recipientApiClient;
    private final PartnerApiClient partnerApiClient;
    private final StepwiseStressShooter stressShooter = new StepwiseStressShooter(10, 30, 10, 20);
    private final StressStat stressStat;
    private ShootingResult shootingResult = new ShootingResult();

    String email = "stress-test-user-17@yandex.ru";
    Long uid = 1_000_000_017L;
    String externalOrderId;
    String trackingId;

    @BeforeEach
    void before() {
        currentCourierEmail(email);
        stressStat.clear();

        manualApiClient.deleteCourier(null, email);
        AutoTestContextHolder.getContext().setCourierTkn("some-token");

        PartnerUserDto courier = manualApiClient.createCourier(email, uid, false, null, "0");
        partnerApiClient.createCourierSchedule(courier.getId());
        AutoTestContextHolder.getContext().setUserId(courier.getId());
        AutoTestContextHolder.getContext().setUid(courier.getUid());

        apiFacade.createAndPickupOrder();
        RoutePointSummaryDto currentRoutePoint = AutoTestContextHolder.getContext().getCurrentRoutePoint();
        assertThat(currentRoutePoint.getType()).isEqualTo(RoutePointType.DELIVERY);
        TaskDto task = AutoTestContextHolder.getContext().getRoutePoint().getTasks().iterator().next();
        OrderDeliveryTaskDto deliveryTask = (OrderDeliveryTaskDto) task;
        this.externalOrderId = deliveryTask.getOrder().getExternalOrderId();
        this.trackingId = recipientApiClient.getTrackingLinkByOrder(externalOrderId);
    }

    @AfterEach
    void after() {
        try {
            manualApiClient.deleteCourier(null, email);
            new StressStatCsv(stressStat).statByRequestsCsv();
            AutoTestContextHolder.clearContext();
        } finally {
            shootingResult.printResult();
        }
    }

    @Test
    @Feature("Где курьер")
    @Stories({@Story("Забрать посылку из СЦ"), @Story("Успешная доставка")})
    @DisplayName(value = "Тест нагрузки на \"Где курьер\"")
    @EnabledIfSystemProperty(named = "stress.tests.enabled", matches = "true")
    void test() {
        List<Runnable> actions = IntStream.rangeClosed(1, stressShooter.maxActionsCount())
                .mapToObj(i -> (Runnable) () -> {
                    var courierLocation = recipientApiClient.getCourierLocation(trackingId);
                    assertThat(courierLocation.getLatitude()).isNotNull();
                    assertThat(courierLocation.getLongitude()).isNotNull();
                })
                .collect(Collectors.toList());
        stressStat.clear();
        this.shootingResult = stressShooter.shoot(actions);

        shootingResult.checkRps();
        stressStat.checkAllFinished();
        shootingResult.checkAllFinishedSuccessfully();

        Long percentile07Duration = stressStat.getPercentileDuration(0.7);
        assertThat(percentile07Duration)
                .withFailMessage("Тайминг 0.7 перцентиля выше 550ms: %dms", percentile07Duration)
                .isLessThan(550);
        Long percentile099Duration = stressStat.getPercentileDuration(0.9);
        assertThat(percentile099Duration)
                .withFailMessage("Тайминг 0.9 перцентиля выше 600ms: %dms", percentile099Duration)
                .isLessThan(600);
    }
}
