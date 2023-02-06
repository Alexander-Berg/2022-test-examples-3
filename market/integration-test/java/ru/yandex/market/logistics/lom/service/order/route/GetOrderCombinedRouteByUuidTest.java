package ru.yandex.market.logistics.lom.service.order.route;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.entity.combinator.embedded.CombinedRoute;
import ru.yandex.market.logistics.lom.repository.OrderRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;

@ParametersAreNonnullByDefault
@DisplayName("Получение маршрута по его идентификатору из ydb")
class GetOrderCombinedRouteByUuidTest extends AbstractOrderCombinedRouteHistoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Override
    @AfterEach
    void tearDown() {
        verify(uuidGenerator).randomUuid();
        super.tearDown();
    }

    @DisplayName("Получение маршрутов по их идентификатору")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    void getRouteCases(
        String displayName,
        UUID routeUuid,
        boolean isPresent
    ) {
        transactionTemplate.execute(ts -> {
            orderCombinedRouteHistoryService.saveRoute(orderRepository.getById(1L), combinedRoute());
            return null;
        });

        Optional<CombinedRoute> routeOpt = orderCombinedRouteHistoryService.getRouteByUuid(routeUuid);
        softly.assertThat(routeOpt.isPresent()).isEqualTo(isPresent);

        verify(ydbRepository).saveRoute(any(), refEq(combinedRoute()));

        if (isPresent) {
            verify(ydbRepository).getRouteByUuid(routeUuid);
        }
    }

    @Nonnull
    private static Stream<Arguments> getRouteCases() {
        return Stream.of(
            Arguments.of(
                "Маршрут существует в ydb",
                MOCKED_UUID,
                true
            ),
            Arguments.of(
                "Маршрута нет в YDB",
                UUID.fromString("00000000-0000-0000-0000-000000000000"),
                false
            )
        );
    }
}
