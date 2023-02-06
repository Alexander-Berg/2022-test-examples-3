package ru.yandex.direct.grid.processing.service.client

import com.nhaarman.mockitokotlin2.any
import jdk.jfr.Description
import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.campaign.repository.CampAdditionalDataRepository
import ru.yandex.direct.core.entity.campaign.service.CampMetrikaCountersService
import ru.yandex.direct.core.entity.client.mcc.ClientMccService
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.grid.core.entity.campaign.service.GridCampaignService
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.exception.GridPublicException
import ru.yandex.direct.grid.processing.model.client.GdClientMccCommonInfo
import ru.yandex.direct.grid.processing.model.client.GdClientSearchRequest
import ru.yandex.direct.grid.processing.processor.util.ClientResolverFetchedFieldsUtil
import ru.yandex.direct.grid.processing.service.banner.BannerDataService
import ru.yandex.direct.grid.processing.service.campaign.CampaignInfoService
import ru.yandex.direct.grid.processing.service.operator.OperatorAccessService
import ru.yandex.direct.grid.processing.service.organizations.OrganizationsDataService
import ru.yandex.direct.grid.processing.service.validation.GridValidationService
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.rbac.PpcRbac
import ru.yandex.direct.rbac.RbacRepType
import ru.yandex.direct.rbac.RbacRole
import ru.yandex.direct.rbac.RbacService
import ru.yandex.direct.xiva.client.XivaClient

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class ClientGraphQlServiceClientMccTest {

    @Autowired
    private lateinit var ppcRbac: PpcRbac

    @Autowired
    private lateinit var clientDataService: ClientDataService

    @Autowired
    private lateinit var campaignsInfoService: CampaignInfoService

    @Autowired
    private lateinit var operatorAccessService: OperatorAccessService

    @Autowired
    private lateinit var gridValidationService: GridValidationService

    @Autowired
    private lateinit var campMetrikaCountersService: CampMetrikaCountersService

    @Autowired
    private lateinit var organizationsDataService: OrganizationsDataService

    @Autowired
    private lateinit var  gridCampaignService: GridCampaignService

    @Autowired
    private lateinit var  campAdditionalDataRepository: CampAdditionalDataRepository

    @Autowired
    private lateinit var  xivaClient: XivaClient

    @Autowired
    private lateinit var   bannerDataService: BannerDataService

    @Autowired
    private lateinit var   clientMccService: ClientMccService

    @Autowired
    private lateinit var rbacService: RbacService

    @Autowired
    private lateinit var steps: Steps

    private lateinit var clientGraphQlService: ClientGraphQlService

    private lateinit var controlClientUser: User
    private lateinit var controlClientMainRepUser: User
    private lateinit var controlClientReadonlyRepUser: User

    private lateinit var notMccClientUser: User
    private lateinit var superUser: User
    private lateinit var superreaderUser: User
    private lateinit var supportUser: User
    private lateinit var limitedSupportUser: User

    private lateinit var mockedStatic: MockedStatic<ClientResolverFetchedFieldsUtil>

    @Before
    fun setUp() {

        clientGraphQlService = ClientGraphQlService(
            ppcRbac,
            clientDataService,
            campaignsInfoService,
            operatorAccessService,
            gridValidationService,
            campMetrikaCountersService,
            organizationsDataService,
            gridCampaignService,
            campAdditionalDataRepository,
            xivaClient,
            bannerDataService,
            clientMccService,
            rbacService
        )

        val controlClientUserInfo = steps.userSteps().createDefaultUser()
        controlClientUser = controlClientUserInfo.user!!
        controlClientMainRepUser = steps.userSteps().createRepresentative(controlClientUserInfo.clientInfo!!, RbacRepType.MAIN).user!!
        controlClientReadonlyRepUser = steps.userSteps().createReadonlyRepresentative(controlClientUserInfo.clientInfo!!).user!!

        notMccClientUser = steps.userSteps().createDefaultUser().user!!

        superUser = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER).chiefUserInfo!!.user!!
        superreaderUser = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPERREADER).chiefUserInfo!!.user!!
        supportUser = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPPORT).chiefUserInfo!!.user!!
        val limitedSupportUserInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.LIMITED_SUPPORT).chiefUserInfo!!
        limitedSupportUser = limitedSupportUserInfo.user!!
        steps.idmGroupSteps().addSupportForClient(limitedSupportUserInfo, controlClientUserInfo.clientInfo)

        val managedClientUserInfo = steps.userSteps().createDefaultUser()
        val managedClient2UserInfo = steps.userSteps().createDefaultUser()

        steps.clientMccSteps().createClientMccLink(controlClientUserInfo.clientId, managedClientUserInfo.clientId)

        steps.clientMccSteps().addMccRequest(controlClientUserInfo.clientId, managedClient2UserInfo.clientId)

        mockedStatic = Mockito.mockStatic(ClientResolverFetchedFieldsUtil::class.java)
        mockedStatic.`when`<Any> { ClientResolverFetchedFieldsUtil.resolve(any()) }.thenReturn(null)

        TestAuthHelper.setDirectAuthentication(controlClientUser)
    }

    @After
    fun cleanUp() {
        mockedStatic.close()
    }

    @Test
    @Description("получение MCC-флажков оператором - главным представителем управляющего MCC")
    fun getClient_ClientMccCommonInfo_ControlMccChiefRep_Success() {
        val result = clientGraphQlService.getClient(
            ContextHelper.buildContext(controlClientUser, controlClientUser),
            GdClientSearchRequest().withLogin(controlClientUser.login),
            null
        )
        Assertions.assertThat(result.info.clientMccCommonInfo).usingRecursiveComparison()
            .isEqualTo(
                GdClientMccCommonInfo()
                    .withHasControlRequests(true)
                    .withHasManagedClients(true)
                    .withCanManageControlRequests(true)
                    .withCanUseClientMccCommonInfo(false)
            )
    }

    @Test
    @Description("получение MCC-флажков оператором - главным представителем управляющего MCC")
    fun getClient_ClientMccCommonInfo_ControlMccChiefRep_WithoutManagedClients_Success() {
        val controlClient2User = steps.userSteps().createDefaultUser().user!!
        val managedClientUserInfo = steps.userSteps().createDefaultUser()
        steps.clientMccSteps().createClientMccLink(controlClient2User.clientId, managedClientUserInfo.clientId)

        val result = clientGraphQlService.getClient(
            ContextHelper.buildContext(controlClient2User, controlClient2User),
            GdClientSearchRequest().withLogin(controlClient2User.login),
            null
        )
        Assertions.assertThat(result.info.clientMccCommonInfo).usingRecursiveComparison()
            .isEqualTo(
                GdClientMccCommonInfo()
                    .withHasControlRequests(false)
                    .withHasManagedClients(true)
                    .withCanManageControlRequests(false)
                    .withCanUseClientMccCommonInfo(false)
            )
    }

    @Test
    @Description("получение MCC-флажков оператором - главным представителем управляющего MCC")
    fun getClient_ClientMccCommonInfo_ControlMccChiefRep_WithoutControlRequests_Success() {
        val controlClient2User = steps.userSteps().createDefaultUser().user!!
        val managedClient2UserInfo = steps.userSteps().createDefaultUser()
        steps.clientMccSteps().addMccRequest(controlClient2User.clientId, managedClient2UserInfo.clientId)
        val result = clientGraphQlService.getClient(
            ContextHelper.buildContext(controlClient2User, controlClient2User),
            GdClientSearchRequest().withLogin(controlClient2User.login),
            null
        )
        Assertions.assertThat(result.info.clientMccCommonInfo).usingRecursiveComparison()
            .isEqualTo(
                GdClientMccCommonInfo()
                    .withHasControlRequests(true)
                    .withHasManagedClients(false)
                    .withCanManageControlRequests(true)
                    .withCanUseClientMccCommonInfo(false)
            )
    }

    @Test
    @Description("получение MCC-флажков оператором - обычным представителем управляющего MCC")
    fun getClient_ClientMccCommonInfo_ControlMccMainRep_Success() {
        val result = clientGraphQlService.getClient(
            ContextHelper.buildContext(controlClientMainRepUser, controlClientUser),
            GdClientSearchRequest().withLogin(controlClientUser.login),
            null
        )
        Assertions.assertThat(result.info.clientMccCommonInfo).usingRecursiveComparison()
            .isEqualTo(
                GdClientMccCommonInfo()
                    .withHasControlRequests(true)
                    .withHasManagedClients(true)
                    .withCanManageControlRequests(false)
                    .withCanUseClientMccCommonInfo(false)
            )
    }

    @Test
    @Description("получение MCC-флажков оператором - readonly представителем управляющего MCC")
    fun getClient_ClientMccCommonInfo_ControlMccReadonlynRep_Success() {
        val result = clientGraphQlService.getClient(
            ContextHelper.buildContext(controlClientReadonlyRepUser, controlClientUser),
            GdClientSearchRequest().withLogin(controlClientUser.login),
            null
        )
        Assertions.assertThat(result.info.clientMccCommonInfo).usingRecursiveComparison()
            .isEqualTo(
                GdClientMccCommonInfo()
                    .withHasControlRequests(true)
                    .withHasManagedClients(true)
                    .withCanManageControlRequests(false)
                    .withCanUseClientMccCommonInfo(false)
            )
    }

    @Test
    @Description("получение MCC-флажков оператором - супером")
    fun getClient_ClientMccCommonInfo_Super_Success() {
        val result = clientGraphQlService.getClient(
            ContextHelper.buildContext(superUser, controlClientUser),
            GdClientSearchRequest().withLogin(controlClientUser.login),
            null
        )
        Assertions.assertThat(result.info.clientMccCommonInfo).usingRecursiveComparison()
            .isEqualTo(
                GdClientMccCommonInfo()
                    .withHasControlRequests(true)
                    .withHasManagedClients(true)
                    .withCanManageControlRequests(true)
                    .withCanUseClientMccCommonInfo(false)
            )
    }

    @Test
    @Description("получение MCC-флажков оператором - суперридером")
    fun getClient_ClientMccCommonInfo_Superreader_Success() {
        val result = clientGraphQlService.getClient(
            ContextHelper.buildContext(superreaderUser, controlClientUser),
            GdClientSearchRequest().withLogin(controlClientUser.login),
            null
        )
        Assertions.assertThat(result.info.clientMccCommonInfo).usingRecursiveComparison()
            .isEqualTo(
                GdClientMccCommonInfo()
                    .withHasControlRequests(true)
                    .withHasManagedClients(true)
                    .withCanManageControlRequests(false)
                    .withCanUseClientMccCommonInfo(false)
            )
    }

    @Test
    @Description("получение MCC-флажков оператором - саппортом")
    fun getClient_ClientMccCommonInfo_Support_Success() {
        val result = clientGraphQlService.getClient(
            ContextHelper.buildContext(supportUser, controlClientUser),
            GdClientSearchRequest().withLogin(controlClientUser.login),
            null
        )
        Assertions.assertThat(result.info.clientMccCommonInfo).usingRecursiveComparison()
            .isEqualTo(
                GdClientMccCommonInfo()
                    .withHasControlRequests(true)
                    .withHasManagedClients(true)
                    .withCanManageControlRequests(true)
                    .withCanUseClientMccCommonInfo(false)
            )
    }

    @Test
    @Description("получение MCC-флажков оператором - ограниченным саппортом")
    fun getClient_ClientMccCommonInfo_LimitedSupport_Success() {
        val result = clientGraphQlService.getClient(
            ContextHelper.buildContext(limitedSupportUser, controlClientUser),
            GdClientSearchRequest().withLogin(controlClientUser.login),
            null
        )
        Assertions.assertThat(result.info.clientMccCommonInfo).usingRecursiveComparison()
            .isEqualTo(
                GdClientMccCommonInfo()
                    .withHasControlRequests(true)
                    .withHasManagedClients(true)
                    .withCanManageControlRequests(false)
                    .withCanUseClientMccCommonInfo(false)
            )
    }

    @Test
    @Description("получение MCC-флажков оператором - представителем другого клиента")
    fun getClient_ClientMccCommonInfo_NotControlMccUser_Success() {
        Assertions.assertThatThrownBy{
            clientGraphQlService.getClient(
                ContextHelper.buildContext(notMccClientUser, controlClientUser),
                GdClientSearchRequest().withLogin(controlClientUser.login),
                null
            )
        }.isInstanceOf(GridPublicException::class.java)
    }
}
