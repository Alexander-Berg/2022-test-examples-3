package ru.yandex.market.loyalty.core.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import org.junit.Test;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.antifraud.orders.web.dto.LoyaltyVerdictRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountRequestDtoV2;
import ru.yandex.market.loyalty.core.model.coin.UserInfo;
import ru.yandex.market.loyalty.core.service.antifraud.AntiFraudService;
import ru.yandex.market.loyalty.core.service.antifraud.AntifraudClient;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.test.SourceScanner;
import ru.yandex.market.loyalty.test.TestFor;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:khamitov-rail@yandex-team.ru">Rail Khamitov</a>
 * @date 08.06.2021
 */
@TestFor(AntifraudClient.class)
public class AntiFraudClientTest extends MarketLoyaltyCoreMockedDbTestBase {

    @Autowired
    @Qualifier("failAntiFraudClient")
    private AntifraudClient failAntiFraudClient;

    @Test
    public void checkIgnoreExceptionsFromAntiFraud() {
        assertNotNull(failAntiFraudClient.checkUserRestriction(UserInfo.builder().setUid(1L).build()));
        assertNotNull(failAntiFraudClient.checkPromoFraud(LoyaltyVerdictRequestDto.builder().build()));
        assertNotNull(failAntiFraudClient.getOrdersCount(OrderCountRequestDtoV2.builder().build()));
    }

    @Test
    public void checkNobodyInvokesClientDirectly() throws IOException {
        var allowedClasses = Set.of(AntiFraudClientTest.class, AntiFraudService.class);
        var classesViolators = SourceScanner.findAllClasses("ru.yandex.market.loyalty")
                .filter(clazz -> Arrays.stream(clazz.getDeclaredFields())
                        .anyMatch(field -> AntifraudClient.class.isAssignableFrom(field.getType()))
                        && !allowedClasses.contains(clazz)
                )
                .map(Class::getName)
                .collect(Collectors.joining(", "));

        assertTrue("These classes shouldn't use AntifraudClient directly: " + classesViolators,
                classesViolators.isEmpty());
    }

}
