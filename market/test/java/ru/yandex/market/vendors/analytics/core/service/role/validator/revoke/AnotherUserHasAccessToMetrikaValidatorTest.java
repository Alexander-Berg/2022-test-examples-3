package ru.yandex.market.vendors.analytics.core.service.role.validator.revoke;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.FunctionalTest;
import ru.yandex.market.vendors.analytics.core.exception.badrequest.role.LostAccessToCountersException;
import ru.yandex.market.vendors.analytics.core.jpa.repository.role.UserPartnerRoleRepository;
import ru.yandex.market.vendors.analytics.core.model.partner.shop.ShopDataSourceType;
import ru.yandex.market.vendors.analytics.core.service.metric.MetricsService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author antipov93.
 */
@DbUnitDataSet(before = "AnotherUserHasAccessToMetrikaValidatorTest.before.csv")
class AnotherUserHasAccessToMetrikaValidatorTest extends FunctionalTest {

    @Autowired
    private AnotherUserHasAccessToMetrikaValidator validator;
    @Autowired
    private UserPartnerRoleRepository userPartnerRoleRepository;

    @MockBean
    private MetricsService metricsService;

    @ParameterizedTest(name = "{3}")
    @MethodSource("validateNoLostCountersArguments")
    void validateNoLostCounters(
            long roleId,
            boolean canDelete,
            Consumer<MetricsService> initMocksAction,
            /*unused*/String description
    ) {
        var roleForRevoke = userPartnerRoleRepository.findById(roleId)
                .orElseThrow(RuntimeException::new);
        initMocksAction.accept(metricsService);
        Executable executable = () -> validator.validate(null, roleForRevoke);
        if (canDelete) {
            assertDoesNotThrow(executable);
        } else {
            assertThrows(LostAccessToCountersException.class, executable);
        }
    }

    private static Stream<Arguments> validateNoLostCountersArguments() {
        return Stream.of(
                Arguments.of(1L, true, EMPTY_ACTION, "Не влияет на вендоров"),
                Arguments.of(2L, true, EMPTY_ACTION, "У пользователя это не последняя роль в рамках партнёра"),
                Arguments.of(4L, true, EMPTY_ACTION, "У магазина нет счётчиков"),
                Arguments.of(5L, true, EMPTY_ACTION, "У пользователя нет доступа к счётчикам"),
                Arguments.of(6L, false, USER_HAS_EXCLUSIVE_ACCESS, "У пользователя эксклюзивный доступ к счётчику"),
                Arguments.of(7L, true, OTHER_USERS_HAS_ACCESS, "У пользователя неэксклюзивный доступ к счётчикам")
        );
    }

    private static final Consumer<MetricsService> EMPTY_ACTION = (metricsService) -> {
    };

    private static final Consumer<MetricsService> USER_HAS_EXCLUSIVE_ACCESS = (metricsService) -> {
        when(metricsService.getCountersWithAccess(
                eq(ShopDataSourceType.METRIKA),
                eq(Set.of(2)),
                eq(5L))
        ).thenReturn(Set.of(2));

        when(metricsService.getCountersWithAccess(
                eq(ShopDataSourceType.METRIKA),
                eq(Set.of(2)),
                eq(Set.of()))
        ).thenReturn(Set.of());
    };

    private static final Consumer<MetricsService> OTHER_USERS_HAS_ACCESS = (metricsService) -> {
        when(metricsService.getCountersWithAccess(
                eq(ShopDataSourceType.METRIKA),
                eq(Set.of(3, 4, 5)),
                eq(6L)
        )).thenReturn(Set.of(3, 4));

        when(metricsService.getCountersWithAccess(
                eq(ShopDataSourceType.METRIKA),
                eq(Set.of(3, 4)),
                eq(Set.of(7L, 8L))
        )).thenReturn(Set.of(3, 4));
    };
}
