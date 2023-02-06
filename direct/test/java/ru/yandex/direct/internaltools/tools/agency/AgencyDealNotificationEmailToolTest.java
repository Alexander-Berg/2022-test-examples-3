package ru.yandex.direct.internaltools.tools.agency;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;

import ru.yandex.direct.core.entity.agency.service.AgencyService;
import ru.yandex.direct.core.entity.feature.container.LoginClientIdChiefLoginWithState;
import ru.yandex.direct.core.entity.feature.service.FeatureManagingService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.internaltools.tools.agency.model.AgencyDealNotificationEmail;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.Result;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.internaltools.tools.agency.AgencyDealNotificationEmailTool.AGENCY_LOGIN_NOT_FOUND;
import static ru.yandex.direct.internaltools.tools.agency.AgencyDealNotificationEmailTool.SUCCESS_MESSAGE;

public class AgencyDealNotificationEmailToolTest {

    private AgencyDealNotificationEmailTool tool;

    private static final String LOGIN = "login";
    private static final Long UID = 123L;
    private static final Long OPERATOR_UID = 456L;
    private static final Long CLIENT_ID = 789L;

    @Mock
    private AgencyService agencyService;

    @Mock
    private RbacService rbacService;

    @Mock
    private ShardHelper shardHelper;

    @Mock
    private FeatureManagingService featureManagingService;

    private static AgencyDealNotificationEmail request;

    @BeforeClass
    public static void initRequest() {
        request = new AgencyDealNotificationEmail().withLogin(LOGIN).withEmail("email");
    }

    @Before
    public void initTestData() {
        initMocks(this);
        doReturn(Result.successful(new ArrayList<List<LoginClientIdChiefLoginWithState>>()))
                .when(featureManagingService).switchFeaturesStateForClientIds(any());
        tool = new AgencyDealNotificationEmailTool(agencyService, rbacService, shardHelper, featureManagingService);
    }

    @Test
    public void uidByLoginIsNull() {
        doReturn(null).when(shardHelper).getUidByLogin(LOGIN);
        assertThat(tool.process(request).getMessage(), equalTo(AGENCY_LOGIN_NOT_FOUND));
    }

    @Test
    public void loginRoleIsNotAgency() {
        doReturn(UID).when(shardHelper).getUidByLogin(LOGIN);
        doReturn(RbacRole.CLIENT).when(rbacService).getUidRole(UID);
        assertThat(tool.process(request).getMessage(), equalTo(AGENCY_LOGIN_NOT_FOUND));
    }

    @Test
    public void managerHasNoRightsToAgency() {
        request.setOperator(new User().withRole(RbacRole.MANAGER).withUid(OPERATOR_UID));
        doReturn(UID).when(shardHelper).getUidByLogin(LOGIN);
        doReturn(RbacRole.AGENCY).when(rbacService).getUidRole(UID);
        doReturn(CLIENT_ID).when(shardHelper).getClientIdByLogin(LOGIN);
        doReturn(false).when(rbacService).isOperatorManagerOfAgency(OPERATOR_UID, UID, CLIENT_ID);
        assertThat(tool.process(request).getMessage(), equalTo(AGENCY_LOGIN_NOT_FOUND));
    }

    @Test
    public void managerHasRightsToAgency() {
        request.setOperator(new User().withRole(RbacRole.MANAGER).withUid(OPERATOR_UID));
        doReturn(UID).when(shardHelper).getUidByLogin(LOGIN);
        doReturn(RbacRole.AGENCY).when(rbacService).getUidRole(UID);
        doReturn(CLIENT_ID).when(shardHelper).getClientIdByLogin(LOGIN);
        doReturn(true).when(rbacService).isOperatorManagerOfAgency(OPERATOR_UID, UID, CLIENT_ID);
        assertThat(tool.process(request).getMessage(), equalTo(SUCCESS_MESSAGE));
    }

    @Test
    public void operatorIsSuper() {
        request.setOperator(new User().withRole(RbacRole.SUPER).withUid(OPERATOR_UID));
        doReturn(UID).when(shardHelper).getUidByLogin(LOGIN);
        doReturn(RbacRole.AGENCY).when(rbacService).getUidRole(UID);
        doReturn(CLIENT_ID).when(shardHelper).getClientIdByLogin(LOGIN);
        doReturn(false).when(rbacService).isOperatorManagerOfAgency(OPERATOR_UID, UID, CLIENT_ID);
        assertThat(tool.process(request).getMessage(), equalTo(SUCCESS_MESSAGE));
    }
}
