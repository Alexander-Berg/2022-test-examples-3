package ru.yandex.market.tpl.carrier.core.domain.run;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.carrier.core.CoreTestV2;
import ru.yandex.market.tpl.carrier.core.domain.movement.Movement;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunItemData;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunCommand;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.OrderReturnTask;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
@CoreTestV2
public class RunManagerCreateOrUpdateItemTest {

    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final RunGenerator runGenerator;
    private final RunHelper runHelper;
    private final TestUserHelper testUserHelper;
    private final RunManager runManager;
    private final MovementGenerator movementGenerator;
    private final RunCommandService runCommandService;

    private final UserShiftRepository userShiftRepository;
    private final RunRepository runRepository;

    private final TransactionTemplate transactionTemplate;

    private Run run;
    private OrderWarehouse dest;
    private UserShift userShift;

    @BeforeEach
    void setUp() {
        OrderWarehouse wh1 = orderWarehouseGenerator.generateWarehouse();
        OrderWarehouse wh2 = orderWarehouseGenerator.generateWarehouse();
        dest = orderWarehouseGenerator.generateWarehouse();

        run = runGenerator.generate(b -> b
                .clearItems()
                .items(List.of(
                        RunGenerator.RunItemGenerateParam.builder()
                                .movement(MovementCommand.Create.builder()
                                        .orderWarehouse(wh1)
                                        .orderWarehouseTo(dest)
                                        .build())
                                .orderNumber(1)
                                .build(),
                        RunGenerator.RunItemGenerateParam.builder()
                                .movement(MovementCommand.Create.builder()
                                        .orderWarehouse(wh2)
                                        .orderWarehouseTo(dest)
                                        .build())
                                .orderNumber(2)
                                .build()
                        ))
        );

        User user = testUserHelper.findOrCreateUser(1L);
        Transport transport = testUserHelper.findOrCreateTransport();
        userShift = runHelper.assignUserAndTransport(run, user, transport);
    }

    @Test
    void shouldAddRunItemAndBindTaskAndRunItem() {
        Movement movement = movementGenerator.generate(MovementCommand.Create.builder()
                .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                .orderWarehouseTo(dest)
                .build()
        );


        runCommandService.addOrUpdateItem(new RunCommand.CreateOrUpdateItem(run.getId(), new RunItemData(movement,
                3,
                null,
                null,
                null
        )));

        transactionTemplate.execute(tc -> {
            userShift = userShiftRepository.findByIdOrThrow(userShift.getId());

            OrderReturnTask returnTask =
                    userShift.streamRoutePoints().flatMap(RoutePoint::streamReturnTasks).findFirst().orElseThrow();

            Assertions.assertThat(returnTask.getRunItemIds()).hasSize(3);

            return null;
        });


    }
}
