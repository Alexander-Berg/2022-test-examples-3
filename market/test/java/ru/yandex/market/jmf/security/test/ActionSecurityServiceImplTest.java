package ru.yandex.market.jmf.security.test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import ru.yandex.market.jmf.entity.EntityAdapterService;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.metaclass.Metaclass;
import ru.yandex.market.jmf.metainfo.MetaInfoService;
import ru.yandex.market.jmf.script.Script;
import ru.yandex.market.jmf.script.ScriptService;
import ru.yandex.market.jmf.script.storage.ScriptStorageService;
import ru.yandex.market.jmf.security.AuthRunnerService;
import ru.yandex.market.jmf.security.MetaclassPermissionContext;
import ru.yandex.market.jmf.security.Profile;
import ru.yandex.market.jmf.security.SecurityDataService;
import ru.yandex.market.jmf.security.SecurityException;
import ru.yandex.market.jmf.security.action.Action;
import ru.yandex.market.jmf.security.impl.action.SecurityServiceImpl;
import ru.yandex.market.jmf.security.impl.action.domain.SecurityDomain;
import ru.yandex.market.jmf.security.impl.action.domain.SecurityDomains;
import ru.yandex.market.jmf.security.impl.action.domain.TestSecurityDomain;
import ru.yandex.market.jmf.security.impl.action.domain.TestSecurityDomains;
import ru.yandex.market.jmf.tx.TxService;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ActionSecurityServiceImplTest {
    @Mock
    private MetaInfoService metaInfoService;
    @Mock
    private SecurityDataService securityDataService;
    @Mock
    private ScriptService scriptService;
    @Mock
    private ScriptStorageService scriptStorageService;
    @Mock
    private TxService txService;
    @Mock
    private AuthRunnerService authRunnerService;
    @Mock
    private EntityAdapterService entityAdapterService;

    @InjectMocks
    private SecurityServiceImpl securityService;

    private TestSecurityDomain childDomain;
    private TestSecurityDomain parentDomain;
    private Profile profile1;
    private Profile profile2;
    private Profile profile3;
    private Action action;
    private Metaclass parent;
    private Metaclass child;
    private TestSecurityDomain anotherParentDomain;

    @BeforeEach
    public void setUp() {
        parent = mock(Metaclass.class);
        child = mock(Metaclass.class);
        Metaclass anotherParent = mock(Metaclass.class);

        when(parent.getFqn()).thenReturn(Fqn.parse("parent"));

        when(child.getFqn()).thenReturn(Fqn.parse("child"));
        when(child.getParent()).thenReturn(parent);

        when(anotherParent.getFqn()).thenReturn(Fqn.parse("anotherParent"));

        profile1 = mock(Profile.class);
        when(profile1.getGid()).thenReturn("profile1");
        profile2 = mock(Profile.class);
        when(profile2.getGid()).thenReturn("profile2");
        profile3 = mock(Profile.class);
        when(profile3.getGid()).thenReturn("profile3");

        action = mock(Action.class);
        when(action.getGid()).thenReturn("@create");

        parentDomain = new TestSecurityDomain(parent, SecurityDomain.DEFAULT);
        childDomain = new TestSecurityDomain(child, parentDomain);
        anotherParentDomain = new TestSecurityDomain(anotherParent, SecurityDomain.DEFAULT);

        parentDomain.addProfile(profile1);
        parentDomain.addProfile(profile2);
        parentDomain.addProfile(profile3);

        parentDomain.addAction(action);

        anotherParentDomain.addProfile(profile1);
        anotherParentDomain.addProfile(profile2);
        anotherParentDomain.addProfile(profile3);

        anotherParentDomain.addAction(action);

        Map<Fqn, SecurityDomain> securityDomainMap = new HashMap<>();
        securityDomainMap.put(parent.getFqn(), parentDomain);
        securityDomainMap.put(child.getFqn(), childDomain);
        securityDomainMap.put(anotherParent.getFqn(), anotherParentDomain);
        SecurityDomains securityDomains = new TestSecurityDomains(securityDomainMap);

        when(metaInfoService.get(SecurityDomains.class)).thenReturn(securityDomains);
        when(securityDataService.getCurrentUserProfiles()).then(inv -> Stream.of(profile1.getGid(), profile2.getGid()));

    }

    @Test
    public void testAllowedIfReallyAllowed() {
        childDomain.setAllowed(profile1.getGid(), action.getGid(), true);

        securityService.checkPermission(new MetaclassPermissionContext(child, Collections.emptyMap()), action.getGid());
    }

    @Test
    public void testAllowedIfAllowedInParentDomain() {
        parentDomain.setAllowed(profile1.getGid(), action.getGid(), true);

        securityService.checkPermission(new MetaclassPermissionContext(child, Collections.emptyMap()), action.getGid());
    }

    @Test
    public void testAllowedIfDisallowedInParentDomainButAllowedInChild() {
        parentDomain.setAllowed(profile1.getGid(), action.getGid(), false);
        childDomain.setAllowed(profile1.getGid(), action.getGid(), true);

        securityService.checkPermission(new MetaclassPermissionContext(child, Collections.emptyMap()), action.getGid());
    }

    @Test
    public void testDisallowedForParentIfDisallowedInParentDomainButAllowedInChild() {
        parentDomain.setAllowed(profile1.getGid(), action.getGid(), false);
        childDomain.setAllowed(profile1.getGid(), action.getGid(), true);

        Assertions.assertThrows(SecurityException.class,
                () -> securityService.checkPermission(new MetaclassPermissionContext(parent, Collections.emptyMap()),
                action.getGid()));
    }

    @Test
    public void testDisallowedIfDisallowedForAllProfiles() {
        parentDomain.setAllowed(profile1.getGid(), action.getGid(), false);
        parentDomain.setAllowed(profile2.getGid(), action.getGid(), false);

        Assertions.assertThrows(SecurityException.class,
                () -> securityService.checkPermission(new MetaclassPermissionContext(parent, Collections.emptyMap()),
                action.getGid()));
    }

    @Test
    public void testDisallowedIfAllowedForAnotherProfile() {
        parentDomain.setAllowed(profile3.getGid(), action.getGid(), true);

        Assertions.assertThrows(SecurityException.class,
                () -> securityService.checkPermission(new MetaclassPermissionContext(parent, Collections.emptyMap()),
                action.getGid()));
    }

    @Test
    public void testAllowedIfAllowedToAtLeastOneProfile() {
        parentDomain.setAllowed(profile1.getGid(), action.getGid(), false);
        parentDomain.setAllowed(profile2.getGid(), action.getGid(), true);

        securityService.checkPermission(new MetaclassPermissionContext(parent, Collections.emptyMap()),
                action.getGid());
    }

    @Test
    public void testDisallowedIfAllowedInAnotherDomainHierarchy() {
        anotherParentDomain.setAllowed(profile1.getGid(), action.getGid(), true);

        Assertions.assertThrows(SecurityException.class,
                () -> securityService.checkPermission(new MetaclassPermissionContext(parent, Collections.emptyMap()),
                action.getGid()));
    }

    @Test
    public void testAllowedToSuperUser() {
        doAnswer((inv) -> Boolean.TRUE).when(authRunnerService).isCurrentUserSuperUser();
        securityService.checkPermission(new MetaclassPermissionContext(parent, Collections.emptyMap()),
                action.getGid());
    }

    @Test
    public void testDisallowedIfDecisionScriptReturnsFalse() {
        var falsyScriptBody = "return false";
        var falsyScriptCode = "falsy";
        parentDomain.setAllowed(profile1.getGid(), action.getGid(), true);
        parentDomain.setDecisionScriptCode(profile1.getGid(), action.getGid(), falsyScriptCode);

        var script = mock(Script.class);
        when(script.getBody()).thenReturn(falsyScriptBody);
        when(scriptStorageService.getScriptOrError(falsyScriptCode)).thenReturn(script);
        when(scriptService.execute(eq(falsyScriptBody), any())).thenReturn(false);

        Assertions.assertThrows(SecurityException.class,
                () -> securityService.checkPermission(new MetaclassPermissionContext(parent, Collections.emptyMap()),
                action.getGid()));
    }

    /**
     * Проверяет, что профили вообще не вычисляются, если для запрашиваемого действия нет вообще никаких выданных
     * разрешений (строка в матрице прав пуста)
     */
    @Test
    public void testProfilesAreNotComputedIfThereIsNoPermissionsForAction() {
        securityService.hasPermission(new MetaclassPermissionContext(parent, Collections.emptyMap()), action.getGid());

        verify(authRunnerService, only()).isCurrentUserSuperUser();
    }

    /**
     * Проверяет, что наличие профиля вообще не вычисляется, если для этого профиля не выдано ни одно разрешение
     * (колонка в матрице прав пуста)
     */
    @Test
    public void testProfileIsNotComputedIfThereIsNoPermissionsForIt() {
        parentDomain.setAllowed(profile3.getGid(), action.getGid(), true);
        securityService.hasPermission(new MetaclassPermissionContext(parent, Collections.emptyMap()), action.getGid());

        verify(profile3, atLeastOnce()).getRoles();
        verify(profile2, times(0)).getRoles();
    }
}
