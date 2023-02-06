package ru.yandex.market.jmf.security.test;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.jmf.metadata.metaclass.Metaclass;
import ru.yandex.market.jmf.security.Profile;
import ru.yandex.market.jmf.security.action.Action;
import ru.yandex.market.jmf.security.impl.action.domain.TestSecurityDomain;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.jmf.security.SecurityConstants.Permissions.SYSTEM_DEFINED_PERMISSIONS_PREFIX;

public class SecurityDomainTest {
    @Test
    public void testCouldNotAddMoreThanOneProfileWithSpecificId() {
        Metaclass metaclass = mock(Metaclass.class);
        Profile profile1 = mock(Profile.class);
        Profile profile2 = mock(Profile.class);
        when(profile1.getGid()).thenReturn("id");
        when(profile2.getGid()).thenReturn("id");
        TestSecurityDomain domain = new TestSecurityDomain(metaclass, TestSecurityDomain.DEFAULT);
        domain.addProfile(profile1);
        var exception = Assertions.assertThrows(IllegalArgumentException.class, () -> domain.addProfile(profile2));
        Assertions.assertEquals("Profile id is already defined", exception.getMessage());
    }

    @Test
    public void testCouldNotAddMoreThanOneActionWithSpecificId() {
        Metaclass metaclass = mock(Metaclass.class);
        Action action1 = mock(Action.class);
        Action action2 = mock(Action.class);
        when(action1.getGid()).thenReturn("@id");
        when(action2.getGid()).thenReturn("@id");
        TestSecurityDomain domain = new TestSecurityDomain(metaclass, TestSecurityDomain.DEFAULT);
        domain.addAction(action1);
        var exception = Assertions.assertThrows(IllegalArgumentException.class, () -> domain.addAction(action2));
        Assertions.assertEquals("Action @id is already defined", exception.getMessage());
    }

    @Test
    public void testCouldNotAddActionWithoutCorrectPrefix() {
        Metaclass metaclass = mock(Metaclass.class);
        Action action = mock(Action.class);
        when(action.getGid()).thenReturn("id");
        TestSecurityDomain domain = new TestSecurityDomain(metaclass, TestSecurityDomain.DEFAULT);
        var exception = Assertions.assertThrows(IllegalArgumentException.class, () -> domain.addAction(action));
        Assertions.assertEquals("Action id must start with `" + SYSTEM_DEFINED_PERMISSIONS_PREFIX + "` sign: " +
                "id", exception.getMessage());
    }

    @Test
    public void testNotAllowedByDefaultIfNotExplicitlySet() {
        Metaclass metaclass = mock(Metaclass.class);

        Profile profile = mock(Profile.class);
        when(profile.getGid()).thenReturn("profileId");

        Action action = mock(Action.class);
        when(action.getGid()).thenReturn("@create");

        TestSecurityDomain domain = new TestSecurityDomain(metaclass, TestSecurityDomain.DEFAULT);
        domain.addProfile(profile);
        domain.addAction(action);

        Assertions.assertFalse(domain.isAllowed(profile, action));
    }

    @Test
    public void testNotAllowedIfExplicitlySetToFalse() {
        Metaclass metaclass = mock(Metaclass.class);

        Profile profile = mock(Profile.class);
        when(profile.getGid()).thenReturn("profileId");

        Action action = mock(Action.class);
        when(action.getGid()).thenReturn("@create");

        TestSecurityDomain domain = new TestSecurityDomain(metaclass, TestSecurityDomain.DEFAULT);
        domain.addProfile(profile);
        domain.addAction(action);

        domain.setAllowed(profile.getGid(), action.getGid(), false);

        Assertions.assertFalse(domain.isAllowed(profile, action));
    }

    @Test
    public void testAllowedIfExplicitlySetToTrue() {
        Metaclass metaclass = mock(Metaclass.class);

        Profile profile = mock(Profile.class);
        when(profile.getGid()).thenReturn("profileId");

        Action action = mock(Action.class);
        when(action.getGid()).thenReturn("@create");

        TestSecurityDomain domain = new TestSecurityDomain(metaclass, TestSecurityDomain.DEFAULT);
        domain.addProfile(profile);
        domain.addAction(action);

        domain.setAllowed(profile.getGid(), action.getGid(), true);

        Assertions.assertTrue(domain.isAllowed(profile, action));
    }

    @Test
    public void testAllowedIfExplicitlySetToTrueAboveHierarchy() {
        Metaclass parent = mock(Metaclass.class);
        Metaclass child = mock(Metaclass.class);

        Profile profile = mock(Profile.class);
        when(profile.getGid()).thenReturn("profileId");

        Action action = mock(Action.class);
        when(action.getGid()).thenReturn("@create");

        TestSecurityDomain parentDomain = new TestSecurityDomain(parent, TestSecurityDomain.DEFAULT);
        parentDomain.addProfile(profile);
        parentDomain.addAction(action);

        parentDomain.setAllowed(profile.getGid(), action.getGid(), true);

        TestSecurityDomain childDomain = new TestSecurityDomain(child, parentDomain);

        Assertions.assertTrue(childDomain.isAllowed(profile, action));
    }

    @Test
    public void testGetProfileFromHierarchy() {
        Metaclass parent = mock(Metaclass.class);
        Metaclass child = mock(Metaclass.class);

        Profile profile = mock(Profile.class);
        when(profile.getGid()).thenReturn("profileId");

        TestSecurityDomain parentDomain = new TestSecurityDomain(parent, TestSecurityDomain.DEFAULT);
        parentDomain.addProfile(profile);

        TestSecurityDomain childDomain = new TestSecurityDomain(child, parentDomain);

        Optional<Profile> childDomainProfile = childDomain.getProfile(profile.getGid());
        Assertions.assertTrue(childDomainProfile.isPresent());
        Assertions.assertEquals(profile, childDomainProfile.get());
    }

    @Test
    public void testGetProfileFromHierarchyIfItDoesNotDefined() {
        Metaclass parent = mock(Metaclass.class);
        Metaclass child = mock(Metaclass.class);

        TestSecurityDomain parentDomain = new TestSecurityDomain(parent, TestSecurityDomain.DEFAULT);
        TestSecurityDomain childDomain = new TestSecurityDomain(child, parentDomain);

        Optional<Profile> childDomainProfile = childDomain.getProfile("profileId");
        Assertions.assertFalse(childDomainProfile.isPresent());
    }

    @Test
    public void testGetActionFromHierarchy() {
        Metaclass parent = mock(Metaclass.class);
        Metaclass child = mock(Metaclass.class);

        Action action = mock(Action.class);
        when(action.getGid()).thenReturn("@create");

        TestSecurityDomain parentDomain = new TestSecurityDomain(parent, TestSecurityDomain.DEFAULT);
        parentDomain.addAction(action);

        TestSecurityDomain childDomain = new TestSecurityDomain(child, parentDomain);

        Optional<Action> childDomainAction = childDomain.getAction(action.getGid());
        Assertions.assertTrue(childDomainAction.isPresent());
        Assertions.assertEquals(action, childDomainAction.get());
    }

    @Test
    public void testGetActionFromHierarchyIfItDoesNotDefined() {
        Metaclass parent = mock(Metaclass.class);
        Metaclass child = mock(Metaclass.class);

        TestSecurityDomain parentDomain = new TestSecurityDomain(parent, TestSecurityDomain.DEFAULT);
        TestSecurityDomain childDomain = new TestSecurityDomain(child, parentDomain);

        Optional<Action> childDomainAction = childDomain.getAction("actionId");
        Assertions.assertFalse(childDomainAction.isPresent());
    }

    @Test
    public void testIsExplicitlyDefinedTrue() {
        Metaclass parent = mock(Metaclass.class);
        Metaclass child = mock(Metaclass.class);

        Profile profile = mock(Profile.class);
        when(profile.getGid()).thenReturn("profileId");

        Action action = mock(Action.class);
        when(action.getGid()).thenReturn("@create");

        TestSecurityDomain parentDomain = new TestSecurityDomain(parent, TestSecurityDomain.DEFAULT);
        parentDomain.addProfile(profile);
        parentDomain.addAction(action);

        parentDomain.setAllowed(profile.getGid(), action.getGid(), false);

        TestSecurityDomain childDomain = new TestSecurityDomain(child, parentDomain);

        Assertions.assertTrue(childDomain.isDefined(profile, action));
    }

    @Test
    public void testIsExplicitlyDefinedFalse() {
        Metaclass parent = mock(Metaclass.class);
        Metaclass child = mock(Metaclass.class);

        Profile profile = mock(Profile.class);
        when(profile.getGid()).thenReturn("profileId");

        Action action = mock(Action.class);
        when(action.getGid()).thenReturn("@create");

        TestSecurityDomain parentDomain = new TestSecurityDomain(parent, TestSecurityDomain.DEFAULT);
        parentDomain.addProfile(profile);
        parentDomain.addAction(action);

        TestSecurityDomain childDomain = new TestSecurityDomain(child, parentDomain);

        Assertions.assertFalse(childDomain.isDefined(profile, action));
    }
}
