package ru.yandex.market.tpl.core.domain.partial_return_order;

import java.time.Duration;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.service.partial_return.FashionDurationCalculator;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.domain.user.UserProperties.CUSTOM_FASHION_TIME_FITTING_ENABLED;
import static ru.yandex.market.tpl.core.domain.user.UserProperties.CUSTOM_FASHION_TIME_FITTING_IN_SECONDS;

@RequiredArgsConstructor
public class FashionDurationCalculatorTest extends TplAbstractTest {
    private final FashionDurationCalculator fashionDurationCalculator;
    private final TestUserHelper testUserHelper;
    private final UserPropertyService userPropertyService;
    private final TransactionTemplate transactionTemplate;

    private User user;

    @BeforeEach
    public void init() {
        user = testUserHelper.findOrCreateUser(123);
    }

    @Test
    void testWithCustomTimeDisabled() {
        Duration timeForFittingClothes = fashionDurationCalculator.getTimeForFittingClothes(List.of(1L, 2L), user);

        assertThat(timeForFittingClothes).isEqualTo(Duration.ofMinutes(20));
    }

    @Test
    void testWithCustomTimeEnabled() {
        transactionTemplate.execute(ts -> {
            userPropertyService.addPropertyToUser(user, CUSTOM_FASHION_TIME_FITTING_ENABLED, true);
            return null;
        });
        Duration timeForFittingClothes = fashionDurationCalculator.getTimeForFittingClothes(List.of(1L, 2L, 3L), user);

        assertThat(timeForFittingClothes).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void testWithCustomTimeEnabledAndWithCustomFittingDuration() {
        transactionTemplate.execute(ts -> {
            userPropertyService.addPropertyToUser(user, CUSTOM_FASHION_TIME_FITTING_ENABLED, true);
            userPropertyService.addPropertyToUser(user, CUSTOM_FASHION_TIME_FITTING_IN_SECONDS, 2L);
            return null;
        });

        Duration timeForFittingClothes = fashionDurationCalculator.getTimeForFittingClothes(List.of(1L, 2L, 3L), user);

        assertThat(timeForFittingClothes).isEqualTo(Duration.ofSeconds(2));
    }

}
