package ru.yandex.direct.core.security.authorization;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.security.DirectAuthentication;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.RbacClientsRelations;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.rbac.model.ClientsRelation;
import ru.yandex.direct.rbac.model.ClientsRelationType;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.core.security.authorization.DirectPermissionEvaluatorTestData.AGENCY_UID;
import static ru.yandex.direct.core.security.authorization.DirectPermissionEvaluatorTestData.INTERNAL_AD_MANAGER_CLIENT_ID;
import static ru.yandex.direct.core.security.authorization.DirectPermissionEvaluatorTestData.INTERNAL_AD_PRODUCT_CLIENT_ID;
import static ru.yandex.direct.core.security.authorization.DirectPermissionEvaluatorTestData.MANAGER_UID;
import static ru.yandex.direct.core.security.authorization.DirectPermissionEvaluatorTestData.MCC_CONTROL_UID;
import static ru.yandex.direct.core.security.authorization.DirectPermissionEvaluatorTestData.MCC_MANAGED_UID;
import static ru.yandex.direct.core.security.authorization.DirectPermissionEvaluatorTestData.SERVICED_CLIENT_UID;
import static ru.yandex.direct.core.security.authorization.DirectPermissionEvaluatorTestData.SUBCLIENT_UID;
import static ru.yandex.direct.core.security.authorization.DirectPermissionEvaluatorTestData.SUPER_SUBCLIENT_CLIENT_ID;
import static ru.yandex.direct.core.security.authorization.DirectPermissionEvaluatorTestData.SUPER_SUBCLIENT_UID;

@RunWith(Parameterized.class)
public class DirectPermissionEvaluatorTest {

    @Mock
    private RbacService rbacService;

    @Mock
    private ClientService clientService;

    @Mock
    private RbacClientsRelations rbacClientsRelations;

    @InjectMocks
    private DirectPermissionEvaluator evaluatorUnderTest;

    private DirectAuthentication authentication;

    @Parameter(0)
    public User operator;

    @Parameter(1)
    public User client;

    @Parameter(2)
    public String targetId;

    @Parameter(3)
    public Permission permission;

    @Parameter(4)
    public boolean expectedResult;

    @Parameters(name = "{0} : {1} : {3}")
    public static Iterable<Object[]> parameters() {
        return DirectPermissionEvaluatorTestData.provideData();
    }

    @Before
    public void before() {
        initMocks(this);

        authentication = new StubDirectAuthentication(operator, client);

        when(rbacService.isOwner(eq(MANAGER_UID), eq(SERVICED_CLIENT_UID))).thenReturn(true);
        when(rbacService.canAgencyCreateCampaign(eq(AGENCY_UID), eq(SUBCLIENT_UID))).thenReturn(true);
        when(rbacService.isUnderAgency(SUBCLIENT_UID)).thenReturn(true);
        when(rbacService.isUnderAgency(SUPER_SUBCLIENT_UID)).thenReturn(true);

        when(clientService.isSuperSubclient(SUPER_SUBCLIENT_CLIENT_ID)).thenReturn(true);

        when(rbacService.getEffectiveOperatorUid(MCC_CONTROL_UID, MCC_MANAGED_UID)).thenReturn(MCC_MANAGED_UID);
        when(rbacService.getEffectiveOperatorUid(MCC_CONTROL_UID, SUBCLIENT_UID)).thenReturn(SUBCLIENT_UID);
        when(rbacService.getEffectiveOperatorUid(MCC_CONTROL_UID, SUPER_SUBCLIENT_UID)).thenReturn(SUPER_SUBCLIENT_UID);

        var productRelationWithManager = new ClientsRelation()
                .withRelationType(ClientsRelationType.INTERNAL_AD_READER);
        when(rbacClientsRelations.getInternalAdProductRelation(ClientId.fromLong(INTERNAL_AD_MANAGER_CLIENT_ID),
                ClientId.fromLong(INTERNAL_AD_PRODUCT_CLIENT_ID)))
                .thenReturn(Optional.of(productRelationWithManager));
    }

    @Test
    public void hasPermissionByTargetTypeWorksFine() {
        boolean actual = evaluatorUnderTest.hasPermission(
                authentication, targetId, TargetType.CLIENT.toString(), permission.toString());
        assertThat(actual, is(expectedResult));
    }
}
