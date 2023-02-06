package ru.yandex.chemodan.app.psbilling.web.validation;

import java.util.Objects;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.config.featureflags.FeatureFlags;
import ru.yandex.chemodan.app.psbilling.web.BaseWebTest;
import ru.yandex.chemodan.app.psbilling.web.exceptions.AccessDeniedException;
import ru.yandex.chemodan.directory.client.DirectoryClient;
import ru.yandex.chemodan.directory.client.DirectoryUsersInfoResponse;
import ru.yandex.commune.a3.action.http.HttpStatus;
import ru.yandex.commune.a3.security.SecurityErrorNames;
import ru.yandex.misc.test.Assert;

import static org.mockito.ArgumentMatchers.eq;

public class GroupAdminPermissionValidationTest extends BaseWebTest {
    @Autowired
    private GroupAdminPermissionValidation validation;

    @Autowired
    private FeatureFlags featureFlags;

    @Autowired
    private DirectoryClient directoryClient;

    private final String ADMIN_ORG = "ADMIN";
    private final String NOT_ADMIN_ORG = "NOT_ADMIN";

    @Before
    public void setUp() {
        featureFlags.getHotFixGroupValidationEnabled().setValue(Boolean.TRUE.toString());
        Mockito.reset(directoryClient);
    }

    @Test
    public void testFeatureOff() {
        featureFlags.getHotFixGroupValidationEnabled().setValue(Boolean.FALSE.toString());

        validation.check(uid, ADMIN_ORG);
        validation.check(uid, NOT_ADMIN_ORG);
    }


    @Test
    public void testSuccessCheck() {
        givenDirectory();

        validation.check(uid, ADMIN_ORG);
    }

    @Test
    public void testFailureCheck() {
        givenDirectory();

        Assert.assertThrows(
                () -> validation.check(uid, NOT_ADMIN_ORG),
                AccessDeniedException.class,
                this::checkAccessDeniedException
        );
    }

    @Test
    public void testFailureCheckDirectoryNull() {
        givenDirectoryNull();

        Assert.assertThrows(
                () -> validation.check(uid, ADMIN_ORG),
                AccessDeniedException.class,
                this::checkAccessDeniedException
        );
    }

    @Test
    public void testFailureCheckDirectoryException() {
        givenDirectoryThrowException();

        Assert.assertThrows(() -> validation.check(uid, ADMIN_ORG), RuntimeException.class);
    }


    private boolean checkAccessDeniedException(AccessDeniedException exception) {
        return Objects.equals(HttpStatus.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode())
                && Objects.equals(SecurityErrorNames.NOT_AUTHORIZED, exception.getErrorName())
                && Objects.equals("Don't have permission: " + GroupAdminPermissionValidation.GROUP_ADMIN_VIEW_PERMISSION,
                exception.getMessage());
    }

    private void givenDirectory() {
        Mockito
                .when(directoryClient.getUserInfo(eq(uid), eq(ADMIN_ORG)))
                .thenReturn(Option.of(new DirectoryUsersInfoResponse(uid.getUid(), true)));

        Mockito
                .when(directoryClient.getUserInfo(eq(uid), eq(NOT_ADMIN_ORG)))
                .thenReturn(Option.of(new DirectoryUsersInfoResponse(uid.getUid(), false)));
    }

    private void givenDirectoryNull() {
        Mockito
                .when(directoryClient.getUserInfo(eq(uid), Mockito.anyString()))
                .thenReturn(Option.empty());
    }

    private void givenDirectoryThrowException() {
        Mockito
                .when(directoryClient.getUserInfo(eq(uid), Mockito.anyString()))
                .thenThrow(RuntimeException.class);
    }
}
