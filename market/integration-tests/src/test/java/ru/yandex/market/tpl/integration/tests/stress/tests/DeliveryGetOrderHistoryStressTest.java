package ru.yandex.market.tpl.integration.tests.stress.tests;

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
import ru.yandex.market.tpl.integration.tests.stress.shooter.StepwiseStressShooter;
import ru.yandex.market.tpl.integration.tests.stress.shooter.stat.ShootingResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.common.util.StringFormatter.sf;
import static ru.yandex.market.tpl.integration.tests.stress.StressTestsUtil.STRESS_TEST_ENABLED;

@Slf4j
@SpringBootTest(classes = AutoTestsConfiguration.class)
@Epic("Доставка")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DeliveryGetOrderHistoryStressTest {
    private final DeliveryApiFacade deliveryApiFacade;
    private final ManualApiClient manualApiClient;
    private final StepwiseStressShooter stressShooter = new StepwiseStressShooter(10, 100, 10, 15);
    private final StressStat stressStat;
    private List<String> externalOrderIds;
    private DeliveryTestConfiguration configuration;
    private ShootingResult shootingResult = new ShootingResult();

    @BeforeEach
    void before() {
        this.externalOrderIds = IntStream.range(0, 100)
                .mapToObj(i -> 7_000_000_000L + i + "")
                .collect(Collectors.toList());
        manualApiClient.deleteOrders(externalOrderIds);
        this.configuration = AutoTestContextHolder.getContext().getDeliveryTestConfiguration();
        configuration.setCreateOrderRequestPath("/order/create_order.xml");
        configuration.setUpdateOrderRequestPath("/order/update_order.xml");
        configuration.setGetOrdersStatusRequestPath("/order/get_many_orders_status.xml");
        configuration.setGetOrderHistoryRequestPath("/order/get_order_history.xml");

        externalOrderIds.forEach(id -> deliveryApiFacade.createOrder(id, configuration));
    }

    @AfterEach
    void after() {
        try {
            manualApiClient.deleteOrders(externalOrderIds);
            AutoTestContextHolder.clearContext();
            log.info(new StressStatCsv(stressStat).statByRequestsCsv());
        } finally {
            shootingResult.printResult();
        }
    }

    @Test
    @Feature("DS API")
    @DisplayName(value = "Получение истории заказа")
    @EnabledIfSystemProperty(named = STRESS_TEST_ENABLED, matches = "true")
    void test() {
        List<Runnable> actions =
                IntStream.rangeClosed(1, stressShooter.maxActionsCount())
                        .mapToObj(i -> (Runnable) () -> deliveryApiFacade.getOrderStatusHistory(RandomUtil.getRandomFromList(externalOrderIds), configuration))
                        .collect(Collectors.toList());

        stressStat.clear();
        this.shootingResult = stressShooter.shoot(actions);
        shootingResult.checkRps();
        stressStat.checkAllFinished();
        shootingResult.checkAllFinishedSuccessfully();
        Long duration = stressStat.getPercentileDuration(0.9);
        log.error("0.99 duration: " + stressStat.getPercentileDuration(0.99));
        log.error("0.90 duration: " + stressStat.getPercentileDuration(0.9));
        log.error("0.80 duration: " + stressStat.getPercentileDuration(0.8));
        log.error("0.70 duration: " + stressStat.getPercentileDuration(0.7));
        assertThat(duration).withFailMessage(sf("Тайминг 0.90 перцентиля выше 2sec: {}ms", duration)).isLessThan(2000L);

    }
}
