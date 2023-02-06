package ru.yandex.market.tpl.integration.tests.stress.tests;

import java.math.BigDecimal;
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

import ru.yandex.market.tpl.api.model.location.LocationDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointSummaryDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.user.partner.PartnerUserDto;
import ru.yandex.market.tpl.integration.tests.client.ManualApiClient;
import ru.yandex.market.tpl.integration.tests.client.PartnerApiClient;
import ru.yandex.market.tpl.integration.tests.client.PublicApiClient;
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
public class PostLocationStressTest {
    private final ApiFacade apiFacade;
    private final ManualApiClient manualApiClient;
    private final PartnerApiClient partnerApiClient;
    private final StepwiseStressShooter stressShooter = new StepwiseStressShooter(50, 100, 10, 30);
    private final StressStat stressStat;
    private final PublicApiClient publicApiClient;
    String email = "stress-test-user-17@yandex.ru";
    Long uid = 1_000_000_017L;
    Long userShiftId;
    private ShootingResult shootingResult = new ShootingResult();

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
        this.userShiftId = AutoTestContextHolder.getContext().getUserShiftId();
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
    @Feature("Доставка")
    @Stories({@Story("Забрать посылку из СЦ"), @Story("Успешная доставка")})
    @DisplayName(value = "Тест нагрузки на отправку текущих координат")
    @EnabledIfSystemProperty(named = "stress.tests.enabled", matches = "true")
    void test() {
        List<Runnable> actions = IntStream.rangeClosed(1, stressShooter.maxActionsCount())
                .mapToObj(i -> (Runnable) () -> {
                    currentCourierEmail(email);
                    LocationDto locationDto = new LocationDto(BigDecimal.TEN, BigDecimal.TEN, "stress-test-device-id",
                            userShiftId);
                    var courierLocation = publicApiClient.postLocation(locationDto);
                    assertThat(courierLocation).isEqualTo(locationDto);
                })
                .collect(Collectors.toList());
        stressStat.clear();
        this.shootingResult = stressShooter.shoot(actions);
        shootingResult.checkRps();
        stressStat.checkAllFinished();
        shootingResult.checkAllFinishedSuccessfully();

        Long percentile07Duration = stressStat.getPercentileDuration(0.7);
        assertThat(percentile07Duration)
                .withFailMessage("Тайминг 0.70 перцентиля выше 300ms: %dms", percentile07Duration)
                .isLessThan(300);
        Long percentile09Duration = stressStat.getPercentileDuration(0.9);
        assertThat(percentile09Duration)
                .withFailMessage("Тайминг 0.90 перцентиля выше 500ms: %dms", percentile09Duration)
                .isLessThan(500);
    }
}
