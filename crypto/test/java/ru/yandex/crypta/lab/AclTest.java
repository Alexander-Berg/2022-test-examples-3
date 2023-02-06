package ru.yandex.crypta.lab;

import java.security.Principal;
import java.util.Objects;

import javax.ws.rs.core.SecurityContext;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.crypta.idm.Roles;
import ru.yandex.crypta.lab.proto.EHashingMethod;
import ru.yandex.crypta.lab.proto.ELabIdentifierType;
import ru.yandex.crypta.lab.proto.TMatchingOptions;
import ru.yandex.crypta.lab.utils.Acl;

public class AclTest {

    private static SecurityContext createSecurityContext(String givenRole) {
        return new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return null;
            }

            @Override
            public boolean isUserInRole(String role) {
                return Objects.equals(givenRole, role);
            }

            @Override
            public boolean isSecure() {
                return false;
            }

            @Override
            public String getAuthenticationScheme() {
                return null;
            }
        };
    }

    @Test
    public void createViewHavingHigherHashingRole() {
        TMatchingOptions viewParams = TMatchingOptions.newBuilder()
                .setIncludeOriginal(false)
                .setHashingMethod(EHashingMethod.HM_MD5)
                .setIdType(ELabIdentifierType.LAB_ID_EMAIL)
                .build();
        SecurityContext higherHashingRole = createSecurityContext(Roles.Lab.Matching.role(
                Roles.Lab.Matching.Privacy.PRIVATE,
                Roles.Lab.Matching.Mode.CONVERTING_GROUP, // same
                Roles.Lab.Matching.Hashing.NON_HASHED, // higher
                Roles.Lab.Matching.Type.EMAIL
        ));
        Assert.assertTrue(Acl.canCreateSuchView(viewParams, higherHashingRole));
    }

    @Test
    public void createViewHavingLowerHashingRole() {
        TMatchingOptions viewParams = TMatchingOptions.newBuilder()
                .setIncludeOriginal(true)
                .setHashingMethod(EHashingMethod.HM_IDENTITY)
                .setIdType(ELabIdentifierType.LAB_ID_PHONE)
                .build();
        SecurityContext lowerHashingRole = createSecurityContext(Roles.Lab.Matching.role(
                Roles.Lab.Matching.Privacy.PRIVATE,
                Roles.Lab.Matching.Mode.CONVERTING_GROUP, // same
                Roles.Lab.Matching.Hashing.HASHED, // lower
                Roles.Lab.Matching.Type.PHONE
        ));
        Assert.assertFalse(Acl.canCreateSuchView(viewParams, lowerHashingRole));
    }

    @Test
    public void createViewHavingHigherMatchingRole() {
        TMatchingOptions viewParams = TMatchingOptions.newBuilder()
                .setIncludeOriginal(false)
                .setHashingMethod(EHashingMethod.HM_MD5)
                .setIdType(ELabIdentifierType.LAB_ID_LOGIN)
                .build();
        SecurityContext higherMatchingRole = createSecurityContext(Roles.Lab.Matching.role(
                Roles.Lab.Matching.Privacy.PRIVATE,
                Roles.Lab.Matching.Mode.MATCHING_SIDE_BY_SIDE, // higher
                Roles.Lab.Matching.Hashing.HASHED,
                Roles.Lab.Matching.Type.LOGIN
        ));
        Assert.assertTrue(Acl.canCreateSuchView(viewParams, higherMatchingRole));
    }

    @Test
    public void createViewHavingLowerMatchingRole() {
        TMatchingOptions viewParams = TMatchingOptions.newBuilder()
                .setIncludeOriginal(true)
                .setHashingMethod(EHashingMethod.HM_IDENTITY)
                .setIdType(ELabIdentifierType.LAB_ID_PUID)
                .build();
        SecurityContext lowerMatchingRole = createSecurityContext(Roles.Lab.Matching.role(
                Roles.Lab.Matching.Privacy.PRIVATE,
                Roles.Lab.Matching.Mode.CONVERTING_GROUP, // lower
                Roles.Lab.Matching.Hashing.NON_HASHED,
                Roles.Lab.Matching.Type.PUID
        ));
        Assert.assertFalse(Acl.canCreateSuchView(viewParams, lowerMatchingRole));
    }

    @Test
    public void createViewHavingExactRole() {
        TMatchingOptions viewParams = TMatchingOptions.newBuilder()
                .setIncludeOriginal(true)
                .setHashingMethod(EHashingMethod.HM_IDENTITY)
                .setIdType(ELabIdentifierType.LAB_ID_PUID)
                .build();
        SecurityContext lowerMatchingRole = createSecurityContext(Roles.Lab.Matching.role(
                Roles.Lab.Matching.Privacy.PRIVATE,
                Roles.Lab.Matching.Mode.MATCHING_SIDE_BY_SIDE,
                Roles.Lab.Matching.Hashing.NON_HASHED,
                Roles.Lab.Matching.Type.PUID
        ));
        Assert.assertTrue(Acl.canCreateSuchView(viewParams, lowerMatchingRole));
    }

}
