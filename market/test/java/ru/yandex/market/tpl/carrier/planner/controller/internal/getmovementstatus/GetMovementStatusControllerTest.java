package ru.yandex.market.tpl.carrier.planner.controller.internal.getmovementstatus;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.tpl.carrier.core.db.QueryCountAssertions;
import ru.yandex.market.tpl.carrier.core.domain.movement.Movement;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;

@Sql("classpath:mockPartner/deliveryServiceWithSCWithTokens.sql")

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class GetMovementStatusControllerTest extends BasePlannerWebTest {

    private final MovementGenerator movementGenerator;
    private final ExternalApiMovementHelper getMovementStatusHistoryHelper;

    @SneakyThrows
    @Test
    void shouldGetMovementStatus() {
        List<Movement> movements = IntStream.rangeClosed(0, 9)
                .mapToObj(id -> movementGenerator.generate(MovementCommand.Create.builder().build()))
                .collect(Collectors.toList());

        List<ResourceId> resources = movements.stream()
                .map(m -> new ResourceId(m.getExternalId(), String.valueOf(m.getId())))
                .collect(Collectors.toList());

        QueryCountAssertions.assertQueryCountTotalEqual(3, () -> {
            return getMovementStatusHistoryHelper.performGetMovementStatus(ExternalApiMovementTestUtil.wrap(
                    ExternalApiMovementTestUtil.prepareGetMovementStatus(resources)
            ));
        });
    }

    @SneakyThrows
    @Test
    void shouldGetMovementStatusHistory() {
        List<Movement> movements = IntStream.rangeClosed(0, 9)
                .mapToObj(id -> movementGenerator.generate(MovementCommand.Create.builder().build()))
                .collect(Collectors.toList());

        List<ResourceId> resources = movements.stream()
                        .map(m -> new ResourceId(m.getExternalId(), String.valueOf(m.getId())))
                        .collect(Collectors.toList());

        QueryCountAssertions.assertQueryCountTotalEqual(3, () -> {
            return getMovementStatusHistoryHelper.performGetMovementStatusHistory(ExternalApiMovementTestUtil.wrap(
                    ExternalApiMovementTestUtil.prepareGetMovementStatusHistory(resources)
            ));
        });
    }
}
