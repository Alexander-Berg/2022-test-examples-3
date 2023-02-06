package ru.yandex.direct.grid.processing.service.user;

import java.util.Collection;
import java.util.List;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.notification.NotificationService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.entity.user.service.validation.BlockUserValidationService;
import ru.yandex.direct.core.security.AccessDeniedException;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.model.user.mutation.GdBlockUser;
import ru.yandex.direct.grid.processing.model.user.mutation.GdBlockUserAction;
import ru.yandex.direct.grid.processing.service.payment.PaymentDataService;
import ru.yandex.direct.grid.processing.service.user.validation.UserMutationValidationService;
import ru.yandex.direct.grid.processing.service.validation.GridValidationService;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.core.testing.data.TestUsers.generateRandomLogin;

@RunWith(JUnitParamsRunner.class)
public class UserDataServiceBlockUserTest {
    @Mock
    private UserService userService;

    @Mock
    private BlockUserValidationService blockUserValidationService;

    @Mock
    private UserMutationValidationService userMutationValidationService;

    @Mock
    private GridValidationService gridValidationService;

    @Mock
    private PaymentDataService paymentDataService;

    @Mock
    private CampaignService campaignService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private UserDataService userDataService;

    private User user;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        doReturn(ValidationResult.success(null))
                .when(blockUserValidationService).validateAccess(anyList());
        doReturn(ValidationResult.success(null))
                .when(blockUserValidationService).validateCanUnblock(anyList());
        doReturn(ValidationResult.success(null))
                .when(blockUserValidationService).validateCampaignsToBlock(anyList(), anyMap(), anyBoolean());

        user = generateNewUser()
                .withUid(RandomNumberUtils.nextPositiveLong())
                .withLogin(generateRandomLogin())
                .withClientId(ClientId.fromLong(RandomNumberUtils.nextPositiveLong()));
        doReturn(List.of(user))
                .when(userService).massGetUser(anyList());
        doReturn(List.of(user))
                .when(userService).massGetUserByLogin(anyList());
        doReturn(ValidationResult.success(null))
                .when(userService).blockUsers(any(User.class), anyList(), anyMap(), any(), any(), anyBoolean());
        doReturn(ValidationResult.success(null))
                .when(userService).unblockUsers(anyList());
    }

    @Test
    @Parameters(method = "parametersForBlock")
    public void testService_CanBlock(RbacRole role, boolean developer, boolean canBlock) {
        User operator = new User().withRole(role).withDeveloper(developer);
        boolean error = false;
        try {
            userDataService.blockUser(operator, new GdBlockUser()
                    .withUserIds(List.of(user.getUid()))
                    .withAction(GdBlockUserAction.BLOCK));
        } catch (AccessDeniedException e) {
            error = true;
        }
        assertThat(error).isNotEqualTo(canBlock);
    }

    @Test
    @Parameters(method = "parametersForBlock")
    public void testService_CanUnBlock(RbacRole role, boolean developer, boolean canUnBlock) {
        User operator = new User().withRole(role).withDeveloper(developer);
        boolean error = false;
        try {
            userDataService.blockUser(operator, new GdBlockUser()
                    .withUserLogins(List.of(user.getLogin()))
                    .withAction(GdBlockUserAction.UNBLOCK));
        } catch (AccessDeniedException e) {
            error = true;
        }
        assertThat(error).isNotEqualTo(canUnBlock);
    }

    private static Collection<Object[]> parametersForBlock() {
        return asList(new Object[][]{
                {RbacRole.CLIENT, false, false},
                {RbacRole.AGENCY, false, false},
                {RbacRole.EMPTY, false, false},
                {RbacRole.INTERNAL_AD_ADMIN, false, false},
                {RbacRole.INTERNAL_AD_MANAGER, false, false},
                {RbacRole.LIMITED_SUPPORT, false, false},
                {RbacRole.MANAGER, false, false},
                {RbacRole.MEDIA, false, false},
                {RbacRole.PLACER, false, true},
                {RbacRole.SUPER, false, true},
                {RbacRole.SUPERREADER, true, true},
                {RbacRole.SUPERREADER, false, false},
                {RbacRole.SUPPORT, false, true}
        });
    }
}
