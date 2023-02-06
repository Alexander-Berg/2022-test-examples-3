package ru.yandex.direct.grid.processing.service.user;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.security.AccessDeniedException;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.user.mutation.GdUpdateUserPhone;
import ru.yandex.direct.rbac.RbacRepType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UserDataServiceTest {
    private static final String NEW_PHONE = "+79998887766";

    @Autowired
    UserService userService;

    @Autowired
    UserDataService serviceUnderTest;

    @Autowired
    Steps steps;

    private User chiefUser;
    private User repUser;

    @Before
    public void setUp() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        chiefUser = clientInfo.getChiefUserInfo().getUser();
        repUser = enrichUser(steps.userSteps().createRepresentative(clientInfo, RbacRepType.MAIN).getUser());
    }

    private User enrichUser(User user) {
        return userService.getUser(user.getUid());
    }

    private GdUpdateUserPhone updatePhoneInput() {
        return new GdUpdateUserPhone().withPhone(NEW_PHONE);
    }

    @Test
    public void updateRepPhone_success_chiefUpdateHimself() {
        User targetUser = this.chiefUser;
        User operatorUser = this.chiefUser;
        serviceUnderTest.updateRepPhone(operatorUser, targetUser, updatePhoneInput());

        User actual = userService.getUser(targetUser.getUid());
        assertThat(actual).extracting(User::getPhone).isEqualTo(NEW_PHONE);
    }

    @Test
    public void updateRepPhone_success_chiefUpdateRep() {
        User operatorUser = this.chiefUser;
        User targetUser = this.repUser;
        serviceUnderTest.updateRepPhone(operatorUser, targetUser, updatePhoneInput());

        User actual = userService.getUser(targetUser.getUid());
        assertThat(actual).extracting(User::getPhone).isEqualTo(NEW_PHONE);
    }

    @Test
    public void updateRepPhone_success_repUserUpdateRep() {
        User operatorUser = this.repUser;
        User targetUser = this.repUser;
        serviceUnderTest.updateRepPhone(operatorUser, targetUser, updatePhoneInput());

        User actual = userService.getUser(targetUser.getUid());
        assertThat(actual).extracting(User::getPhone).isEqualTo(NEW_PHONE);
    }

    @Test
    public void updateRepPhone_error_repUserUpdateChief() {
        User operatorUser = this.repUser;
        User targetUser = this.chiefUser;

        assertThatThrownBy(() -> serviceUnderTest.updateRepPhone(operatorUser, targetUser, updatePhoneInput()))
                .isInstanceOf(AccessDeniedException.class);
    }

}
