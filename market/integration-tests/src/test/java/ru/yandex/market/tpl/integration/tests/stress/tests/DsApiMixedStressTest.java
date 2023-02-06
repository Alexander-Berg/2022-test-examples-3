package ru.yandex.market.tpl.integration.tests.stress.tests;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ru.yandex.market.tpl.common.util.RandomUtil;
import ru.yandex.market.tpl.integration.tests.client.ManualApiClient;
import ru.yandex.market.tpl.integration.tests.configuration.AutoTestsConfiguration;
import ru.yandex.market.tpl.integration.tests.configuration.DeliveryTestConfiguration;
import ru.yandex.market.tpl.integration.tests.context.AutoTestContextHolder;
import ru.yandex.market.tpl.integration.tests.facade.DeliveryApiFacade;
import ru.yandex.market.tpl.integration.tests.stress.StressStat;
import ru.yandex.market.tpl.integration.tests.stress.StressStatCsv;
import ru.yandex.market.tpl.integration.tests.stress.shooter.ConstStressShooter;
import ru.yandex.market.tpl.integration.tests.stress.shooter.StepwiseStressShooter;
import ru.yandex.market.tpl.integration.tests.stress.shooter.stat.ShootingResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.integration.tests.stress.StressTestsUtil.STRESS_TEST_ENABLED;

@Slf4j
@SpringBootTest(classes = AutoTestsConfiguration.class)
@Epic("Доставка")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DsApiMixedStressTest {
    private final DeliveryApiFacade deliveryApiFacade;
    private final ManualApiClient manualApiClient;
    private final StepwiseStressShooter createOrderStressShooter = new ConstStressShooter(3, 120);
    private final StepwiseStressShooter getOrderHistoryStressShooter = new ConstStressShooter(30, 120);
    private final StepwiseStressShooter getOrdersStatusStressShooter = new ConstStressShooter(15, 120);
    private final StressStat stressStat;
    private List<String> externalOrderIds;
    private List<String> firstlyCreatedOrders = new ArrayList<>();
    private DeliveryTestConfiguration configuration;
    private ShootingResult shootingResult1 = new ShootingResult();
    private ShootingResult shootingResult2 = new ShootingResult();
    private ShootingResult shootingResult3 = new ShootingResult();

    @BeforeEach
    void before() {
        this.externalOrderIds = IntStream.range(0, createOrderStressShooter.maxActionsCount() + 100)
                .mapToObj(i -> 7_000_000_000L + i + "")
                .collect(Collectors.toList());
        manualApiClient.deleteOrders(externalOrderIds);
        this.configuration = AutoTestContextHolder.getContext().getDeliveryTestConfiguration();
        configuration.setCreateOrderRequestPath("/order/create_order.xml");
        configuration.setUpdateOrderRequestPath("/order/update_order.xml");
        configuration.setGetOrdersStatusRequestPath("/order/get_many_orders_status.xml");
        configuration.setGetOrderHistoryRequestPath("/order/get_order_history.xml");

        externalOrderIds.stream()
                .limit(100)
                .peek(firstlyCreatedOrders::add)
                .forEach(id -> deliveryApiFacade.createOrder(id, configuration));
        assertThat(firstlyCreatedOrders.size()).isEqualTo(100);
        assertThat(firstlyCreatedOrders.stream().min(Comparator.comparing(x -> x)).orElseThrow()).isEqualTo(
                "7000000000");
        assertThat(firstlyCreatedOrders.stream().max(Comparator.comparing(x -> x)).orElseThrow()).isEqualTo(
                "7000000099");
    }

    @AfterEach
    void after() {
        try {
            manualApiClient.deleteOrders(externalOrderIds);
            AutoTestContextHolder.clearContext();
            log.info(new StressStatCsv(stressStat).statByRequestsCsv());
        } finally {
            shootingResult1.printResult();
            shootingResult2.printResult();
            shootingResult3.printResult();
        }
    }

    @Test
    @Feature("DS API")
    @DisplayName(value = "Получение истории заказа")
    @EnabledIfSystemProperty(named = STRESS_TEST_ENABLED, matches = "true")
    void test() {
        List<Runnable> createActions = externalOrderIds.stream()
                .skip(100)
                .map(id -> (Runnable) () -> deliveryApiFacade.createOrder(id, configuration))
                .collect(Collectors.toList());
        List<Runnable> getHistoryActions =
                IntStream.rangeClosed(1, getOrderHistoryStressShooter.maxActionsCount())
                        .mapToObj(i -> (Runnable) () -> deliveryApiFacade.getOrderStatusHistory(RandomUtil.getRandomFromList(firstlyCreatedOrders), configuration))
                        .collect(Collectors.toList());
        List<Runnable> getStatusesActions =
                IntStream.rangeClosed(1, getOrdersStatusStressShooter.maxActionsCount())
                        .mapToObj(i -> (Runnable) () -> deliveryApiFacade.getOrderStatus(RandomUtil.getRandomFromList(firstlyCreatedOrders), configuration))
                        .collect(Collectors.toList());

        stressStat.clear();
        createOrderStressShooter.shootAsync(createActions);
        getOrderHistoryStressShooter.shootAsync(getHistoryActions);
        getOrdersStatusStressShooter.shootAsync(getStatusesActions);
        this.shootingResult1 = createOrderStressShooter.waitAllFinish();
        this.shootingResult2 = getOrderHistoryStressShooter.waitAllFinish();
        this.shootingResult3 = getOrdersStatusStressShooter.waitAllFinish();
        shootingResult1.checkRps();
        shootingResult2.checkRps();
        shootingResult3.checkRps();
        shootingResult1.checkAllFinishedSuccessfully();
        shootingResult2.checkAllFinishedSuccessfully();
        shootingResult3.checkAllFinishedSuccessfully();
        stressStat.checkAllFinished();

    }
}
