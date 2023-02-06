package ru.yandex.market.tpl.carrier.core.domain.usershift.listener;

import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.carrier.core.CoreTestV2;
import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.RunStatus;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftStatus;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;

@RequiredArgsConstructor(onConstructor_ = @Autowired)

@CoreTestV2
class OnUserShiftOpenedStartRunTest {

    private final ConfigurationServiceAdapter configurationServiceAdapter;

    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final TestUserHelper testUserHelper;
    private final RunGenerator runGenerator;

    private final RunHelper runHelper;
    private final RunRepository runRepository;
    private final UserShiftRepository userShiftRepository;

    private Run run;
    private UserShift userShift;
    private User user;

    @BeforeEach
    void setUp() {
        var company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);

        user = testUserHelper.findOrCreateUser(1L);
        var transport = testUserHelper.findOrCreateTransport();

        run = runGenerator.generate(RunGenerator.RunGenerateParam.builder()
                .externalId("1")
                .deliveryServiceId(123L)
                .campaignId(company.getCampaignId())
                .runDate(LocalDate.now())
                .items(
                        List.of(
                                new RunGenerator.RunItemGenerateParam(
                                        MovementCommand.Create
                                                .builder()
                                                .externalId("123")
                                                .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                                .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                                                .build()
                                        ,
                                        1,
                                        null,
                                        null
                                )
                        )
                )
                .build()
        );

        userShift = runHelper.assignUserAndTransport(run, user, transport);
    }

    @Test
    void shouldStartRunIfUserShiftIsOpened() {
        testUserHelper.openShift(user, userShift.getId());

        userShift = userShiftRepository.findByIdOrThrow(userShift.getId());

        Assertions.assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.ON_TASK);
        run = runRepository.findByIdOrThrow(run.getId());

        Assertions.assertThat(run.getStatus()).isEqualTo(RunStatus.STARTED);
    }

}
