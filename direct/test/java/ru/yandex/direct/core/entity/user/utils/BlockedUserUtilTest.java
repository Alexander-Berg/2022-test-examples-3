package ru.yandex.direct.core.entity.user.utils;

import java.util.Arrays;
import java.util.Collection;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.rbac.RbacRole;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class BlockedUserUtilTest {

    public static Collection<Object[]> parametrizedTestData_ForCheckOperatorIsBlocked() {
        return Arrays.asList(new Object[][]{
                //operatorStatusBlocked = null
                {RbacRole.SUPER, null, false, false},

                //operatorStatusBlocked = false && methodAllowedForBlockedOperator = false
                {RbacRole.SUPER, false, false, false},
                {RbacRole.SUPERREADER, false, false, false},
                {RbacRole.SUPPORT, false, false, false},
                {RbacRole.PLACER, false, false, false},
                {RbacRole.MEDIA, false, false, false},
                {RbacRole.MANAGER, false, false, false},
                {RbacRole.INTERNAL_AD_ADMIN, false, false, false},
                {RbacRole.INTERNAL_AD_MANAGER, false, false, false},
                {RbacRole.INTERNAL_AD_SUPERREADER, false, false, false},
                {RbacRole.LIMITED_SUPPORT, false, false, false},
                {RbacRole.AGENCY, false, false, false},
                {RbacRole.CLIENT, false, false, false},
                {RbacRole.EMPTY, false, false, false},

                //operatorStatusBlocked = true && methodAllowedForBlockedOperator = false
                {RbacRole.SUPER, true, false, true},
                {RbacRole.SUPERREADER, true, false, true},
                {RbacRole.SUPPORT, true, false, true},
                {RbacRole.PLACER, true, false, true},
                {RbacRole.MEDIA, true, false, true},
                {RbacRole.MANAGER, true, false, true},
                {RbacRole.INTERNAL_AD_ADMIN, true, false, true},
                {RbacRole.INTERNAL_AD_MANAGER, true, false, true},
                {RbacRole.INTERNAL_AD_SUPERREADER, true, false, true},
                {RbacRole.LIMITED_SUPPORT, true, false, true},
                {RbacRole.AGENCY, true, false, true},
                {RbacRole.CLIENT, true, false, false},
                {RbacRole.EMPTY, true, false, false},

                //operatorStatusBlocked = true && methodAllowedForBlockedOperator = true
                {RbacRole.SUPER, true, true, true},
                {RbacRole.SUPERREADER, true, true, true},
                {RbacRole.SUPPORT, true, true, true},
                {RbacRole.PLACER, true, true, true},
                {RbacRole.MEDIA, true, true, true},
                {RbacRole.MANAGER, true, true, true},
                {RbacRole.INTERNAL_AD_ADMIN, true, true, true},
                {RbacRole.INTERNAL_AD_MANAGER, true, true, true},
                {RbacRole.INTERNAL_AD_SUPERREADER, true, true, true},
                {RbacRole.LIMITED_SUPPORT, true, true, true},
                {RbacRole.AGENCY, true, true, false},
                {RbacRole.CLIENT, true, true, false},
                {RbacRole.EMPTY, true, true, false},
        });
    }

    @Test
    @Parameters(method = "parametrizedTestData_ForCheckOperatorIsBlocked")
    @TestCaseName("role = {0}, statusBlocked = {1}, methodAllowedForBlockedOperator = {2}, expectedResult = {3}")
    public void checkOperatorIsBlocked(RbacRole operatorRole, Boolean operatorStatusBlocked,
                                       boolean methodAllowedForBlockedOperator,
                                       boolean expectedResult) {
        User operator = createUser(operatorRole, operatorStatusBlocked);
        boolean result = BlockedUserUtil.checkOperatorIsBlocked(operator, methodAllowedForBlockedOperator);

        assertThat(result)
                .isEqualTo(expectedResult);
    }


    public static Collection<Object[]> parametrizedTestData_ForCheckSubjectUserIsBlocked() {
        return Arrays.asList(new Object[][]{
                //userStatusBlocked = null
                {RbacRole.SUPER, null, false, false},

                //userStatusBlocked = false && methodAllowedForBlockedUser = false
                {RbacRole.SUPER, false, false, false},
                {RbacRole.SUPERREADER, false, false, false},
                {RbacRole.SUPPORT, false, false, false},
                {RbacRole.PLACER, false, false, false},
                {RbacRole.MEDIA, false, false, false},
                {RbacRole.MANAGER, false, false, false},
                {RbacRole.INTERNAL_AD_ADMIN, false, false, false},
                {RbacRole.INTERNAL_AD_MANAGER, false, false, false},
                {RbacRole.INTERNAL_AD_SUPERREADER, false, false, false},
                {RbacRole.LIMITED_SUPPORT, false, false, false},
                {RbacRole.AGENCY, false, false, false},
                {RbacRole.CLIENT, false, false, false},
                {RbacRole.EMPTY, false, false, false},

                //userStatusBlocked = true && methodAllowedForBlockedUser = false
                {RbacRole.SUPER, true, false, false},
                {RbacRole.SUPERREADER, true, false, false},
                {RbacRole.SUPPORT, true, false, false},
                {RbacRole.PLACER, true, false, false},
                {RbacRole.MEDIA, true, false, true},
                {RbacRole.MANAGER, true, false, true},
                {RbacRole.INTERNAL_AD_ADMIN, true, false, false},
                {RbacRole.INTERNAL_AD_MANAGER, true, false, false},
                {RbacRole.INTERNAL_AD_SUPERREADER, true, false, false},
                {RbacRole.LIMITED_SUPPORT, true, false, false},
                {RbacRole.AGENCY, true, false, true},
                {RbacRole.CLIENT, true, false, true},
                {RbacRole.EMPTY, true, false, true},

                //userStatusBlocked = true && methodAllowedForBlockedUser = true
                {RbacRole.SUPER, true, true, false},
                {RbacRole.SUPERREADER, true, true, false},
                {RbacRole.SUPPORT, true, true, false},
                {RbacRole.PLACER, true, true, false},
                {RbacRole.MEDIA, true, true, false},
                {RbacRole.MANAGER, true, true, false},
                {RbacRole.INTERNAL_AD_ADMIN, true, true, false},
                {RbacRole.INTERNAL_AD_MANAGER, true, true, false},
                {RbacRole.INTERNAL_AD_SUPERREADER, true, true, false},
                {RbacRole.LIMITED_SUPPORT, true, true, false},
                {RbacRole.AGENCY, true, true, false},
                {RbacRole.CLIENT, true, true, false},
                {RbacRole.EMPTY, true, true, false},
        });
    }

    @Test
    @Parameters(method = "parametrizedTestData_ForCheckSubjectUserIsBlocked")
    @TestCaseName("role = {0}, statusBlocked = {1}, methodAllowedForBlockedUser = {2}, expectedResult = {3}")
    public void checkSubjectUserIsBlocked(RbacRole userRole, Boolean userStatusBlocked,
                                          boolean methodAllowedForBlockedUser,
                                          boolean expectedResult) {
        User user = createUser(userRole, userStatusBlocked);
        boolean result = BlockedUserUtil.checkSubjectUserIsBlocked(user, methodAllowedForBlockedUser, user);

        assertThat(result)
                .isEqualTo(expectedResult);
    }


    public static Collection<Object[]> parametrizedTestData_ForCheckSubjectUserIsBlocked_WithAnotherOperatorRole() {
        return Arrays.asList(new Object[][]{
                //userStatusBlocked = true && methodAllowedForBlockedUser = false
                {RbacRole.AGENCY, false, RbacRole.SUPERREADER, false},
                {RbacRole.AGENCY, false, RbacRole.MANAGER, true},
                {RbacRole.CLIENT, false, RbacRole.SUPERREADER, false},
                {RbacRole.CLIENT, false, RbacRole.MANAGER, true},
                {RbacRole.EMPTY, false, RbacRole.SUPERREADER, false},
                {RbacRole.EMPTY, false, RbacRole.MANAGER, true},

                //userStatusBlocked = true && methodAllowedForBlockedUser = true
                {RbacRole.AGENCY, true, RbacRole.SUPPORT, false},
                {RbacRole.AGENCY, true, RbacRole.MANAGER, false},
                {RbacRole.CLIENT, true, RbacRole.SUPPORT, false},
                {RbacRole.CLIENT, true, RbacRole.MANAGER, false},
                {RbacRole.EMPTY, true, RbacRole.SUPPORT, false},
                {RbacRole.EMPTY, true, RbacRole.MANAGER, false},
        });
    }

    @Test
    @Parameters(method = "parametrizedTestData_ForCheckSubjectUserIsBlocked_WithAnotherOperatorRole")
    @TestCaseName("userRole = {0}, methodAllowedForBlockedUser = {1}, operatorRole = {2}, expectedResult = {3}")
    public void checkSubjectUserIsBlocked_WithAnotherOperatorRole(RbacRole userRole,
                                                                  boolean methodAllowedForBlockedUser,
                                                                  RbacRole operatorRole,
                                                                  boolean expectedResult) {
        User user = createUser(userRole, true);
        User operator = createUser(operatorRole, false);
        boolean result = BlockedUserUtil.checkSubjectUserIsBlocked(user, methodAllowedForBlockedUser, operator);

        assertThat(result)
                .isEqualTo(expectedResult);
    }


    public static Collection<Object[]> parametrizedTestData_ForCheckClientIsBlockedForEdit() {
        return Arrays.asList(new Object[][]{
                {RbacRole.CLIENT, false, RbacRole.SUPER, false, false},
                {RbacRole.CLIENT, false, RbacRole.SUPER, true, true},
                {RbacRole.CLIENT, true, RbacRole.SUPER, false, false},
                {RbacRole.CLIENT, true, RbacRole.SUPER, true, true},

                {RbacRole.AGENCY, false, RbacRole.SUPERREADER, false, false},
                {RbacRole.AGENCY, false, RbacRole.SUPERREADER, true, true},
                {RbacRole.AGENCY, true, RbacRole.SUPERREADER, false, false},
                {RbacRole.AGENCY, true, RbacRole.SUPERREADER, true, true},

                {RbacRole.CLIENT, false, RbacRole.MANAGER, false, false},
                {RbacRole.CLIENT, false, RbacRole.MANAGER, true, true},
                {RbacRole.CLIENT, true, RbacRole.MANAGER, false, true},
                {RbacRole.CLIENT, true, RbacRole.MANAGER, true, true},
        });
    }

    @Test
    @Parameters(method = "parametrizedTestData_ForCheckClientIsBlockedForEdit")
    @TestCaseName("userRole = {0}, userStatusBlocked = {1}, operatorRole = {2}, operatorStatusBlocked = {3}," +
            " expectedResult = {4}")
    public void checkClientIsBlockedForEdit(RbacRole userRole, boolean userStatusBlocked,
                                            RbacRole operatorRole, boolean operatorStatusBlocked,
                                            boolean expectedResult) {
        User user = createUser(userRole, userStatusBlocked);
        User operator = createUser(operatorRole, operatorStatusBlocked);
        boolean result = BlockedUserUtil.checkClientIsBlockedForEdit(user, operator);

        assertThat(result)
                .isEqualTo(expectedResult);
    }

    private static User createUser(RbacRole role, @Nullable Boolean statusBlocked) {
        return new User()
                .withRole(role)
                .withStatusBlocked(statusBlocked);
    }

}
