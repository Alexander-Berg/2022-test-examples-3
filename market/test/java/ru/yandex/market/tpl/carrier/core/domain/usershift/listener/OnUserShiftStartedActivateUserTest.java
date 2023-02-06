package ru.yandex.market.tpl.carrier.core.domain.usershift.listener;


import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.carrier.core.CoreTestV2;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.UserQueryService;
import ru.yandex.market.tpl.carrier.core.domain.user.UserStatus;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.carrier.core.domain.usershift.commands.UserShiftCommand;

@RequiredArgsConstructor(onConstructor_={@Autowired})

@CoreTestV2
class OnUserShiftStartedActivateUserTest {
    private static final long UID = 1L;

    private final TestUserHelper testUserHelper;
    private final RunGenerator runGenerator;
    private final RunHelper runHelper;
    private final UserShiftCommandService userShiftCommandService;
    private final UserQueryService userQueryService;

    private User user;
    private UserShift userShift;

    @BeforeEach
    void setUp() {
        user = testUserHelper.findOrCreateUser(UID);
        var transport = testUserHelper.findOrCreateTransport();
        Assertions.assertThat(user.getStatus()).isEqualTo(UserStatus.NOT_ACTIVE);

        var run = runGenerator.generate();
        userShift = runHelper.assignUserAndTransport(run, user, transport);
    }

    @Test
    void shouldActivateUserOnUserShiftStart() {
        userShiftCommandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));

        user = userQueryService.findById(user.getId()).orElseThrow();

        Assertions.assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

}
