package ru.yandex.market.tpl.integration.tests.stress.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ru.yandex.market.tpl.api.model.manual.CreateDeliveryTasksRequest;
import ru.yandex.market.tpl.api.model.user.partner.PartnerUserDto;
import ru.yandex.market.tpl.api.model.user.partner.PartnerUserRoutingPropertiesDto;
import ru.yandex.market.tpl.integration.tests.client.ManualApiClient;
import ru.yandex.market.tpl.integration.tests.client.PartnerApiClient;
import ru.yandex.market.tpl.integration.tests.configuration.AutoTestsConfiguration;
import ru.yandex.market.tpl.integration.tests.context.AutoTestContextHolder;
import ru.yandex.market.tpl.integration.tests.facade.ApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.ManualApiFacade;
import ru.yandex.market.tpl.integration.tests.stress.StressStat;
import ru.yandex.market.tpl.integration.tests.stress.StressStatCsv;
import ru.yandex.market.tpl.integration.tests.stress.shooter.ConstStressShooter;
import ru.yandex.market.tpl.integration.tests.stress.shooter.stat.ShootingResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.integration.tests.stress.StressTestsUtil.currentCourierEmail;

@SpringBootTest(classes = AutoTestsConfiguration.class)
@Epic("Курьерское приложение")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PickupOrdersStressTest {
    private static final Integer COURIERS_COUNT = 60;
    private static final Integer ORDERS_PER_COURIER_COUNT = 5;
    private final ApiFacade apiFacade;
    private final ManualApiFacade manualApiFacade;
    private final ManualApiClient manualApiClient;
    private final PartnerApiClient partnerApiClient;
    private final ConstStressShooter stressShooter = new ConstStressShooter(1, COURIERS_COUNT);
    private final StressStat stressStat;
    private Map<String, Long> usersEmailUidMap;
    private Map<String, Long> usersEmailUserIdMap = new HashMap<>();
    private ShootingResult shootingResult = new ShootingResult();
    private List<String> externalOrderIds = new ArrayList<>(COURIERS_COUNT * ORDERS_PER_COURIER_COUNT);


    @BeforeEach
    @Step("Подготовка данных к тесту")
    void before() {
        this.usersEmailUidMap = IntStream.rangeClosed(1, COURIERS_COUNT)
                .boxed()
                .collect(Collectors.toMap(i -> "stress-test-user-" + i + "@yandex.ru", i -> 1_000_000_000L + i));

        manualApiClient.deleteCouriers(usersEmailUidMap.keySet());
        List<CreateDeliveryTasksRequest.CreateDeliveryTask> allCreateDeliveryTasks = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger(0);
        usersEmailUidMap.keySet().forEach(email -> {
            createCourierContext(email, counter.getAndIncrement());
            var createDeliveryTasks =
                    manualApiFacade.generateCreateDeliveryTasks(ORDERS_PER_COURIER_COUNT);
            allCreateDeliveryTasks.addAll(createDeliveryTasks);
        });
        List<String> extOrderIds = allCreateDeliveryTasks.stream()
                .map(CreateDeliveryTasksRequest.CreateDeliveryTask::getExternalOrderId)
                .collect(Collectors.toList());
        manualApiClient.deleteOrders(extOrderIds);
        List<String> ids = manualApiFacade.createManyDeliveryTasks(allCreateDeliveryTasks).getExternalOrderIds();
        this.externalOrderIds.addAll(ids);
    }

    @Step("Создание курьера, расписания, смены. email: {email}")
    private void createCourierContext(String email, Integer count) {
        currentCourierEmail(email);
        AutoTestContextHolder.getContext().setCourierTkn("some-token");
        Long uid = usersEmailUidMap.get(email);
        PartnerUserRoutingPropertiesDto routingProperties = new PartnerUserRoutingPropertiesDto();
        routingProperties.setDeliveryPhotoEnabled(true);
        routingProperties.setRerouteEnabled(false);
        PartnerUserDto courier = manualApiClient.createCourier(email, uid, true, routingProperties, count.toString());
        AutoTestContextHolder.getContext().setUserId(courier.getId());
        AutoTestContextHolder.getContext().setUid(courier.getUid());
        usersEmailUserIdMap.put(email, courier.getId());
        partnerApiClient.createCourierSchedule(courier.getId());
        manualApiFacade.createShift();
        manualApiFacade.createUserShift();
    }

    @AfterEach
    @Step("Чистка после теста")
    void after() {
        try {
            new StressStatCsv(stressStat).statByRequestsCsv();
            manualApiClient.deleteCouriers(usersEmailUidMap.keySet());
            manualApiClient.deleteOrders(externalOrderIds);
            AutoTestContextHolder.clearContext();
        } finally {
            shootingResult.printResult();
        }
    }

    @Test
    @Step("Неопсредственно сам тест")
    @Feature("Доставка")
    @Stories({@Story("Забрать посылку из СЦ"), @Story("Успешная доставка")})
    @DisplayName(value = "Тест забора посылок под нагрузкой")
    @EnabledIfSystemProperty(named = "stress.tests.enabled", matches = "true")
    void test() {
        List<Runnable> actions = usersEmailUidMap.keySet().stream()
                .map(email -> (Runnable) () -> {
                    currentCourierEmail(email);
                    AutoTestContextHolder.getContext().setCourierTkn("some-token");
                    apiFacade.pickupOrders();
                })
                .collect(Collectors.toList());
        stressStat.clear();
        this.shootingResult = stressShooter.shoot(actions);
        shootingResult.checkRps();
        stressStat.checkAllFinished();
        shootingResult.checkAllFinishedSuccessfully();
        assertThat(stressStat.httpStatusCodeMap().keySet()).isEqualTo(Set.of(200));
        Map<String, StressStat> stressStats = stressStat.splitByEndpoints();
        stressStats.forEach((endpoint, stat) -> {
            if (endpoint.startsWith("GET /api/route-points/")) {
                assertThat(stat.get99Duration())
                        .withFailMessage("Тайминг GET /api/route-points/{} 0.99 перцентиля выше 2000ms: %dms",
                                stat.get99Duration())
                        .isLessThan(2000);
            } else if (endpoint.startsWith("GET /api/route-points")) {
                assertThat(stat.get99Duration())
                        .withFailMessage("Тайминг GET /api/route-points 0.99 перцентиля выше 1000ms: %dms",
                                stat.get99Duration())
                        .isLessThan(1000);
            } else if (endpoint.startsWith("GET")) {
                assertThat(stat.get99Duration())
                        .withFailMessage("Тайминг GET ручек 0.99 перцентиля выше 500ms: %dms",
                                stat.get99Duration())
                        .isLessThan(500);
            }
        });
    }
}
