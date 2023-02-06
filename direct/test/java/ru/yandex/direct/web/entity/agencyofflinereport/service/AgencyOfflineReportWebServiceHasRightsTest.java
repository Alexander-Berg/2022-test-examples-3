package ru.yandex.direct.web.entity.agencyofflinereport.service;

import org.assertj.core.api.SoftAssertions;
import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.core.entity.agencyofflinereport.service.AgencyOfflineReportService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.security.authorization.Permission;
import ru.yandex.direct.core.testing.data.TestUsers;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.RbacRepType;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.web.configuration.mock.auth.DirectWebAuthenticationSourceMock;
import ru.yandex.direct.web.validation.kernel.ValidationResultConversionService;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.rbac.RbacRole.AGENCY;
import static ru.yandex.direct.rbac.RbacRole.MANAGER;
import static ru.yandex.direct.rbac.RbacRole.SUPER;
import static ru.yandex.direct.rbac.RbacRole.SUPERREADER;
import static ru.yandex.direct.rbac.RbacRole.SUPPORT;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

public class AgencyOfflineReportWebServiceHasRightsTest {

    private static RbacService rbacService;
    private static DirectWebAuthenticationSourceMock authenticationSource;
    private static AgencyOfflineReportWebService webService;

    @BeforeClass
    public static void setUp() {
        rbacService = mock(RbacService.class);
        authenticationSource = new DirectWebAuthenticationSourceMock();
        webService = new AgencyOfflineReportWebService(
                mock(AgencyOfflineReportWebValidationService.class),
                mock(AgencyOfflineReportService.class),
                authenticationSource,
                rbacService,
                mock(TranslationService.class),
                mock(ValidationResultConversionService.class));
    }

    @Test
    public void subjectIsNotAgency_NoPermission() {
        when(rbacService.isOwner(anyLong(), anyLong())).thenReturn(true);
        SoftAssertions softAssertions = new SoftAssertions();
        int testsCount = 0;
        for (RbacRole operatorRole : RbacRole.values()) {
            for (RbacRole subjectRole : RbacRole.values()) {
                if (subjectRole.anyOf(AGENCY)) {
                    continue;
                }
                authenticationSource.withOperator(user(operatorRole, 1L, 11L))
                        .withSubjectUser(user(subjectRole, 2L, 21L));
                softAssertions.assertThat(webService.hasRights(Permission.READ))
                        .withFailMessage("%s has no read permission for %s", operatorRole, subjectRole)
                        .isFalse();
                softAssertions.assertThat(webService.hasRights(Permission.WRITE))
                        .withFailMessage("%s has no write permission for %s", operatorRole, subjectRole)
                        .isFalse();
                ++testsCount;
            }
        }
        assumeThat("test cases", testsCount, greaterThan(30));
        softAssertions.assertAll();
    }

    @Test
    public void notOwnedInRbac_NoPermission() {
        when(rbacService.isOwner(anyLong(), anyLong())).thenReturn(false);
        SoftAssertions softAssertions = new SoftAssertions();
        int testsCount = 0;
        authenticationSource.withSubjectUser(user(AGENCY, 2L, 21L));
        for (RbacRole operatorRole : RbacRole.values()) {
            authenticationSource.withOperator(user(operatorRole, 1L, 11L));
            softAssertions.assertThat(webService.hasRights(Permission.READ))
                    .withFailMessage("%s has no read permission", operatorRole)
                    .isFalse();
            softAssertions.assertThat(webService.hasRights(Permission.WRITE))
                    .withFailMessage("%s has no write permission", operatorRole)
                    .isFalse();
            ++testsCount;
        }
        assumeThat("test cases", testsCount, greaterThan(5));
        softAssertions.assertAll();
    }

    @Test
    public void unsupportedRoles_NoPermission() {
        when(rbacService.isOwner(anyLong(), anyLong())).thenReturn(true);
        SoftAssertions softAssertions = new SoftAssertions();
        int testsCount = 0;
        authenticationSource.withSubjectUser(user(AGENCY, 2L, 21L));
        for (RbacRole operatorRole : RbacRole.values()) {
            if (operatorRole.anyOf(SUPER, SUPPORT, SUPERREADER, MANAGER, AGENCY)) {
                continue;
            }
            authenticationSource.withOperator(user(operatorRole, 1L, 11L));
            softAssertions.assertThat(webService.hasRights(Permission.READ))
                    .withFailMessage("%s has no read permission", operatorRole)
                    .isFalse();
            softAssertions.assertThat(webService.hasRights(Permission.WRITE))
                    .withFailMessage("%s has no write permission", operatorRole)
                    .isFalse();
            ++testsCount;
        }
        assumeThat("test cases", testsCount, greaterThanOrEqualTo(2));
        softAssertions.assertAll();
    }

    @Test
    public void agenciesWithDifferentId_NoPermission() {
        when(rbacService.isOwner(anyLong(), anyLong())).thenReturn(true);
        authenticationSource.withOperator(user(AGENCY, 1L, 11L)).withSubjectUser(user(AGENCY, 2L, 21L));
        assertFalse("AGENCY has no read permission for other AGENCY", webService.hasRights(Permission.READ));
        assertFalse("AGENCY has no write permission for other AGENCY", webService.hasRights(Permission.WRITE));
    }

    @Test
    public void support_HasReadOnlyPermission() {
        authenticationSource.withOperator(user(SUPPORT, 1L, 11L))
                .withSubjectUser(user(AGENCY, 2L, 21L));
        setSubjectIsOwnedByOperatorInRbac();
        assertTrue("SUPPORT has read permission", webService.hasRights(Permission.READ));
        assertFalse("SUPPORT has no write permission", webService.hasRights(Permission.WRITE));
    }

    @Test
    public void superreader_HasReadOnlyPermission() {
        authenticationSource.withOperator(user(SUPERREADER, 1L, 11L))
                .withSubjectUser(user(AGENCY, 2L, 21L));
        setSubjectIsOwnedByOperatorInRbac();
        assertTrue("SUPERREADER has read permission", webService.hasRights(Permission.READ));
        assertFalse("SUPERREADER has no write permission", webService.hasRights(Permission.WRITE));
    }

    @Test
    public void super_HasReadWritePermission() {
        authenticationSource.withOperator(user(SUPER, 1L, 11L))
                .withSubjectUser(user(AGENCY, 2L, 21L));
        setSubjectIsOwnedByOperatorInRbac();
        assertTrue("SUPER has read permission", webService.hasRights(Permission.READ));
        assertTrue("SUPER has write permission", webService.hasRights(Permission.WRITE));
    }

    @Test
    public void manager_HasReadWritePermission() {
        authenticationSource.withOperator(user(MANAGER, 1L, 11L))
                .withSubjectUser(user(AGENCY, 2L, 21L));
        setSubjectIsOwnedByOperatorInRbac();
        assertTrue("MANAGER has read permission", webService.hasRights(Permission.READ));
        assertTrue("MANAGER has write permission", webService.hasRights(Permission.WRITE));
    }

    @Test
    public void anyAgencyRepTypeOnItself_HasReadWritePermission() {
        SoftAssertions softAssertions = new SoftAssertions();
        int testsCount = 0;
        for (RbacRepType repType : RbacRepType.values()) {
            if (repType == RbacRepType.READONLY) {
                continue;
            }

            User agency = user(AGENCY, 1L, 11L).withRepType(repType);
            authenticationSource.withOperator(agency).withSubjectUser(agency);
            setSubjectIsOwnedByOperatorInRbac();
            softAssertions.assertThat(webService.hasRights(Permission.READ))
                    .withFailMessage("Agency rep %s has read permission", repType)
                    .isTrue();
            softAssertions.assertThat(webService.hasRights(Permission.WRITE))
                    .withFailMessage("Agency rep %s has write permission", repType)
                    .isTrue();
            ++testsCount;
        }
        assumeThat("test cases", testsCount, greaterThanOrEqualTo(2));
        softAssertions.assertAll();
    }

    @Test
    public void chiefAgencyRep_HasReadWritePermission() {
        User operator = user(AGENCY, 1L, 11L);
        SoftAssertions softAssertions = new SoftAssertions();
        int testsCount = 0;
        for (RbacRepType repType : RbacRepType.values()) {
            if (repType == RbacRepType.CHIEF) {
                continue;
            }
            User subject = user(AGENCY, 1L, 12L).withRepType(repType);
            authenticationSource.withOperator(operator).withSubjectUser(subject);
            setSubjectIsOwnedByOperatorInRbac();
            softAssertions.assertThat(webService.hasRights(Permission.READ))
                    .withFailMessage("Chief agency rep has read permission on %s rep", repType)
                    .isTrue();
            softAssertions.assertThat(webService.hasRights(Permission.WRITE))
                    .withFailMessage("Chief agency rep has write permission on %s rep", repType)
                    .isTrue();
            ++testsCount;
        }
        assumeThat("test cases", testsCount, greaterThanOrEqualTo(1));
        softAssertions.assertAll();
    }

    @Test
    public void mainAgencyRep_HasReadWritePermission() {
        User operator = user(AGENCY, 1L, 11L).withRepType(RbacRepType.MAIN);
        SoftAssertions softAssertions = new SoftAssertions();
        int testsCount = 0;
        for (RbacRepType repType : RbacRepType.values()) {
            User subject = user(AGENCY, 1L, 12L).withRepType(repType);
            authenticationSource.withOperator(operator).withSubjectUser(subject);
            setSubjectIsOwnedByOperatorInRbac();
            softAssertions.assertThat(webService.hasRights(Permission.READ))
                    .withFailMessage("Main agency rep has read permission on another %s rep", repType)
                    .isTrue();
            softAssertions.assertThat(webService.hasRights(Permission.WRITE))
                    .withFailMessage("Main agency rep has write permission on another %s rep", repType)
                    .isTrue();
            ++testsCount;
        }
        assumeThat("test cases", testsCount, greaterThanOrEqualTo(2));
        softAssertions.assertAll();
    }

    @Test
    public void limitedAgencyRep_HasNoPermission() {
        User operator = user(AGENCY, 1L, 11L).withRepType(RbacRepType.LIMITED);
        SoftAssertions softAssertions = new SoftAssertions();
        int testsCount = 0;
        for (RbacRepType repType : RbacRepType.values()) {
            User subject = user(AGENCY, 1L, 12L).withRepType(repType);
            authenticationSource.withOperator(operator).withSubjectUser(subject);
            setSubjectIsOwnedByOperatorInRbac();
            softAssertions.assertThat(webService.hasRights(Permission.READ))
                    .withFailMessage("Limited agency rep has no read permission on another %s rep", repType)
                    .isFalse();
            softAssertions.assertThat(webService.hasRights(Permission.WRITE))
                    .withFailMessage("Limited agency rep has no write permission on another %s rep", repType)
                    .isFalse();
            ++testsCount;
        }
        assumeThat("test cases", testsCount, greaterThanOrEqualTo(2));
        softAssertions.assertAll();
    }


    private static User user(RbacRole role, Long clientId, long uid) {
        ClientId id = role != RbacRole.EMPTY ? ClientId.fromNullableLong(clientId) : null;
        return TestUsers.defaultUser()
                .withRole(role)
                .withUid(uid)
                .withClientId(id);
    }

    private void setSubjectIsOwnedByOperatorInRbac() {
        when(rbacService.isOwner(
                authenticationSource.getAuthentication().getOperator().getUid(),
                authenticationSource.getAuthentication().getSubjectUser().getUid())
        ).thenReturn(true);
    }
}
