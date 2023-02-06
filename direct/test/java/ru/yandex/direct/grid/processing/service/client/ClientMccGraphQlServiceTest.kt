package ru.yandex.direct.grid.processing.service.client

import jdk.jfr.Description
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.communication.service.CommunicationEventService
import ru.yandex.direct.core.entity.client.mcc.ClientMccService
import ru.yandex.direct.core.entity.feature.service.FeatureService
import ru.yandex.direct.core.entity.user.service.UserService
import ru.yandex.direct.core.security.AccessDeniedException
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.client.GdClientMccRequestsAccess
import ru.yandex.direct.grid.processing.model.client.GdClientMccRequestsPayload
import ru.yandex.direct.grid.processing.model.client.GdMccClients
import ru.yandex.direct.grid.processing.model.client.GdMccClientsAccess
import ru.yandex.direct.grid.processing.model.client_mcc.mutation.GdAddClientMccRequest
import ru.yandex.direct.grid.processing.model.client_mcc.mutation.GdClientMccPayload
import ru.yandex.direct.grid.processing.service.client.converter.toGdMccClient
import ru.yandex.direct.grid.processing.service.client.converter.toGdMccRequestShort
import ru.yandex.direct.grid.processing.service.client.validation.clientAlreadyLinked
import ru.yandex.direct.grid.processing.service.client.validation.clientNotAllowed
import ru.yandex.direct.grid.processing.service.client.validation.mccLinkNotFound
import ru.yandex.direct.grid.processing.service.client.validation.requestNotFound
import ru.yandex.direct.grid.processing.service.validation.GridValidationResultConversionService
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdDefect
import ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdValidationResult
import ru.yandex.direct.rbac.RbacClientsRelations
import ru.yandex.direct.rbac.RbacRepType
import ru.yandex.direct.rbac.RbacService
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.path

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class ClientMccGraphQlServiceTest {

    @Autowired
    private lateinit var clientMccService: ClientMccService

    @Autowired
    private lateinit var rbacClientsRelations: RbacClientsRelations

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var rbacService: RbacService

    @Autowired
    private lateinit var gridValidationResultConversionService: GridValidationResultConversionService

    @Autowired
    private lateinit var featureService: FeatureService

    @Autowired
    private lateinit var communicationEventService: CommunicationEventService

    @Autowired
    private lateinit var steps: Steps

    private lateinit var clientMccGraphQlService: ClientMccGraphQlService

    private lateinit var agency: ClientInfo

    private lateinit var managedClientUserInfo: UserInfo
    private lateinit var managedClient2UserInfo: UserInfo
    private lateinit var managedClient3UserInfo: UserInfo
    private lateinit var managedClient4UserInfo: UserInfo
    private lateinit var controlSubclientInfo: ClientInfo
    private lateinit var managedSubclientInfo: ClientInfo
    private lateinit var controlClientUserInfo: UserInfo
    private lateinit var controlClientMainRepUserInfo: UserInfo
    private lateinit var controlClientReadonlyRepUserInfo: UserInfo
    private lateinit var controlClientWithLinkUserInfo: UserInfo

    private lateinit var controlClientForDeleteUserInfo: UserInfo
    private lateinit var controlClientForApproveUserInfo: UserInfo
    private lateinit var controlClientForDeclineUserInfo: UserInfo
    private lateinit var controlClientForUnlinkUserInfo: UserInfo
    private lateinit var managedClientForGetUserInfo: UserInfo
    private lateinit var clientWithoutRequestsUserInfo: UserInfo
    private lateinit var clientWithoutRequestsMainRepUserInfo: UserInfo
    private lateinit var controlClientForGetRequestsUserInfo: UserInfo

    @Before
    fun setUp() {

        clientMccGraphQlService = ClientMccGraphQlService(
            clientMccService,
            rbacClientsRelations,
            userService,
            rbacService,
            gridValidationResultConversionService,
            featureService,
            communicationEventService
        )

        managedClientUserInfo = steps.userSteps().createDefaultUser()
        managedClient2UserInfo = steps.userSteps().createDefaultUser()
        managedClient3UserInfo = steps.userSteps().createDefaultUser()
        managedClient4UserInfo = steps.userSteps().createDefaultUser()
        agency = steps.clientSteps().createDefaultAgency()
        controlSubclientInfo = steps.clientSteps().createDefaultClientUnderAgency(agency)
        managedSubclientInfo = steps.clientSteps().createDefaultClientUnderAgency(agency)
        controlClientUserInfo = steps.userSteps().createDefaultUser()
        controlClientMainRepUserInfo = steps.userSteps().createRepresentative(controlClientUserInfo.clientInfo!!, RbacRepType.MAIN)
        controlClientReadonlyRepUserInfo = steps.userSteps().createReadonlyRepresentative(controlClientUserInfo.clientInfo!!)
        controlClientWithLinkUserInfo = steps.userSteps().createDefaultUser()
        controlClientForDeleteUserInfo = steps.userSteps().createDefaultUser()
        controlClientForApproveUserInfo = steps.userSteps().createDefaultUser()
        controlClientForDeclineUserInfo = steps.userSteps().createDefaultUser()
        controlClientForUnlinkUserInfo = steps.userSteps().createDefaultUser()
        managedClientForGetUserInfo = steps.userSteps().createDefaultUser()
        setOf(controlClientForApproveUserInfo, controlClientForDeclineUserInfo).forEach {
            steps.clientMccSteps().createClientMccLink(it.clientId, managedClient4UserInfo.clientId)
        }
        clientWithoutRequestsUserInfo = steps.userSteps().createDefaultUser()
        clientWithoutRequestsMainRepUserInfo =
            steps.userSteps().createRepresentative(clientWithoutRequestsUserInfo.clientInfo!!)
        controlClientForGetRequestsUserInfo = steps.userSteps().createDefaultUser()

        setOf(
            managedClientUserInfo.clientId,
            controlClientUserInfo.clientId,
            controlSubclientInfo.clientId,
            controlClientWithLinkUserInfo.clientId,
            controlClientForDeleteUserInfo.clientId,
            managedSubclientInfo.clientId
        )
            .map {
                steps.featureSteps().addClientFeature(it, FeatureName.CLIENT_MCC, true)
            }

        setOf(controlClientWithLinkUserInfo, controlClientForUnlinkUserInfo)
            .map {
                steps.clientMccSteps().createClientMccLink(it.clientId, managedClientUserInfo.clientId)
                steps.clientMccSteps().createClientMccLink(it.clientId, managedClient3UserInfo.clientId)
                steps.clientMccSteps().createClientMccLink(it.clientId, managedClientForGetUserInfo.clientId)
                steps.clientMccSteps().createClientMccLink(managedClientForGetUserInfo.clientId, it.clientId)
            }
        setOf(controlClientForDeleteUserInfo, controlClientForApproveUserInfo, controlClientForDeclineUserInfo)
            .map {
                steps.clientMccSteps().addMccRequest(it.clientId, managedClientUserInfo.clientId)
                steps.clientMccSteps().addMccRequest(it.clientId, managedClientForGetUserInfo.clientId)
                steps.clientMccSteps().addMccRequest(it.clientId, managedClient3UserInfo.clientId)
            }

        setOf(managedClientUserInfo, managedClient2UserInfo, managedClientForGetUserInfo)
            .map {
                steps.clientMccSteps().addMccRequest(controlClientForGetRequestsUserInfo.clientId, it.clientId)
            }

        TestAuthHelper.setDirectAuthentication(managedClientUserInfo.user!!)
    }

    @Test
    fun addClientMccRequest_Success() {
        val result = clientMccGraphQlService.addClientMccRequest(
            ContextHelper.buildContext(managedClientUserInfo.user!!),
            GdAddClientMccRequest().withLogin(controlClientUserInfo.login)
        )
        assertThat(result).isEqualTo(
            GdClientMccPayload()
        )
    }

    @Test
    fun addClientMccRequest_AgencyForSubclient_Failed() {
        assertThatThrownBy {
            clientMccGraphQlService.addClientMccRequest(
                ContextHelper.buildContext(agency.chiefUserInfo!!.user!!, managedSubclientInfo.chiefUserInfo!!.user!!),
                GdAddClientMccRequest().withLogin(controlClientUserInfo.login)
            )
        }.isInstanceOf(AccessDeniedException::class.java)
    }

    @Test
    @Description("не даем создавать заявку для логина обычного представителя")
    fun addClientMccRequest_ControlMainRep_Failed() {
        val result = clientMccGraphQlService.addClientMccRequest(
            ContextHelper.buildContext(managedClientUserInfo.user!!),
            GdAddClientMccRequest().withLogin(controlClientMainRepUserInfo.login)
        )
        assertThat(result).isEqualTo(
            GdClientMccPayload().withValidationResult(
                toGdValidationResult(
                    toGdDefect(path(field("login")), clientNotAllowed())
                )
            )
        )
    }

    @Test
    @Description("не даем создавать заявку для логина readonly представителя")
    fun addClientMccRequest_ControlReadonlyRep_Failed() {
        val result = clientMccGraphQlService.addClientMccRequest(
            ContextHelper.buildContext(managedClientUserInfo.user!!),
            GdAddClientMccRequest().withLogin(controlClientReadonlyRepUserInfo.login)
        )
        assertThat(result).isEqualTo(
            GdClientMccPayload().withValidationResult(
                toGdValidationResult(
                    toGdDefect(path(field("login")), clientNotAllowed())
                )
            )
        )
    }

    @Test
    @Description("не даем создавать заявку на управляющего субклиента")
    fun addClientMccRequest_ControlSubclient_Failed() {
        val result = clientMccGraphQlService.addClientMccRequest(
            ContextHelper.buildContext(managedClientUserInfo.user!!),
            GdAddClientMccRequest().withLogin(controlSubclientInfo.login)
        )
        assertThat(result).isEqualTo(
            GdClientMccPayload().withValidationResult(
                toGdValidationResult(
                    toGdDefect(path(field("login")), clientNotAllowed())
                )
            )
        )
    }

    @Test
    @Description("не даем создавать заявку для самого себя")
    fun addClientMccRequest_SelfControlClient_Failed() {
        val result = clientMccGraphQlService.addClientMccRequest(
            ContextHelper.buildContext(managedClientUserInfo.user!!),
            GdAddClientMccRequest().withLogin(managedClientUserInfo.login)
        )
        assertThat(result).isEqualTo(
            GdClientMccPayload().withValidationResult(
                toGdValidationResult(
                    toGdDefect(path(field("login")), clientNotAllowed())
                )
            )
        )
    }

    @Test
    @Description("не даем создавать заявку для управляющего с ролью агентства")
    fun addClientMccRequest_ControlClient_Failed() {
        val result = clientMccGraphQlService.addClientMccRequest(
            ContextHelper.buildContext(managedClientUserInfo.user!!),
            GdAddClientMccRequest().withLogin(agency.chiefUserInfo!!.login)
        )
        assertThat(result).isEqualTo(
            GdClientMccPayload().withValidationResult(
                toGdValidationResult(
                    toGdDefect(path(field("login")), clientNotAllowed())
                )
            )
        )
    }

    @Test
    @Description("не даем создавать заявку для оператора с ролью агентства без указания субклиента")
    fun addClientMccRequest_ForAgency_Failed() {
        assertThatThrownBy {
            clientMccGraphQlService.addClientMccRequest(
                ContextHelper.buildContext(agency.chiefUserInfo!!.user!!),
                GdAddClientMccRequest().withLogin(controlClientUserInfo.login)
            )
        }.isInstanceOf(AccessDeniedException::class.java)
    }

    @Test
    @Description("не даем создавать заявку для управляющего с ролью агентства")
    fun addClientMccRequest_ControlClientWithLink_Failed() {
        val result = clientMccGraphQlService.addClientMccRequest(
            ContextHelper.buildContext(managedClientUserInfo.user!!),
            GdAddClientMccRequest().withLogin(controlClientWithLinkUserInfo.login)
        )
        assertThat(result).isEqualTo(
            GdClientMccPayload().withValidationResult(
                toGdValidationResult(
                    toGdDefect(path(field("login")), clientAlreadyLinked())
                )
            )
        )
    }

    @Test
    @Description("не даем создавать заявку оператору управляющему MCC")
    fun addClientMccRequest_ControlClientOperator_Failed() {
        assertThatThrownBy {
            clientMccGraphQlService.addClientMccRequest(
                ContextHelper.buildContext(controlClientUserInfo.user!!, managedClientUserInfo.user!!),
                GdAddClientMccRequest().withLogin(controlClientUserInfo.login)
            )
        }.isInstanceOf(AccessDeniedException::class.java)
    }

    @Test
    @Description("удаление заявки на управление")
    fun deleteClientMccRequest_Success() {
        val result = clientMccGraphQlService.deleteClientMccRequest(
            ContextHelper.buildContext(managedClientUserInfo.user!!),
            controlClientForDeleteUserInfo.login
        )
        assertThat(result).isEqualTo(
            GdClientMccPayload()
        )
    }

    @Test
    @Description("получение ошибки при попытке удалить несуществующую заявку")
    fun deleteClientMccRequest_Error_RequestNotFound() {
        val result = clientMccGraphQlService.deleteClientMccRequest(
            ContextHelper.buildContext(managedClientUserInfo.user!!),
            clientWithoutRequestsUserInfo.login
        )
        assertThat(result).isEqualTo(
            GdClientMccPayload().withValidationResult(
                toGdValidationResult(
                    toGdDefect(path(field("login")), requestNotFound())
                )
            )
        )
    }

    @Test
    @Description("не даем удалять заявку оператору управляющему MCC")
    fun deleteClientMccRequest_ControlClientOperator_Failed() {
        assertThatThrownBy {
            clientMccGraphQlService.deleteClientMccRequest(
                ContextHelper.buildContext(controlClientUserInfo.user!!, managedClientUserInfo.user!!),
                controlClientUserInfo.login
            )
        }.isInstanceOf(AccessDeniedException::class.java)
    }

    @Test
    @Description("подтверждение заявки на управление")
    fun approveClientMccRequest_Success() {
        val result = clientMccGraphQlService.approveClientMccRequest(
            ContextHelper.buildContext(controlClientForApproveUserInfo.user!!),
            managedClientUserInfo.login
        )
        assertThat(result).isEqualTo(
            GdClientMccPayload()
        )
    }

    @Test
    @Description("получение ошибки при попытке подтвердить несуществующую заявку")
    fun approveClientMccRequest_Error_RequestNotFound() {
        val result = clientMccGraphQlService.approveClientMccRequest(
            ContextHelper.buildContext(clientWithoutRequestsUserInfo.user!!),
            managedClientUserInfo.login
        )
        assertThat(result).isEqualTo(
            GdClientMccPayload().withValidationResult(
                toGdValidationResult(
                    toGdDefect(path(field("login")), requestNotFound())
                )
            )
        )
    }

    @Test
    @Description("даем подтверждать свою заявку оператору управляющему MCC")
    fun approveClientMccRequest_ControlClientOperator_Success() {
        val result = clientMccGraphQlService.approveClientMccRequest(
            ContextHelper.buildContext(controlClientForApproveUserInfo.user!!, managedClient4UserInfo.user!!),
            managedClient3UserInfo.login
        )
        assertThat(result).isEqualTo(
            GdClientMccPayload()
        )
    }

    @Test
    @Description("отклонение заявки на управление")
    fun declineClientMccRequest_Success() {
        val result = clientMccGraphQlService.declineClientMccRequest(
            ContextHelper.buildContext(controlClientForDeclineUserInfo.user!!),
            managedClientUserInfo.login
        )
        assertThat(result).isEqualTo(
            GdClientMccPayload()
        )
    }

    @Test
    @Description("получение ошибки при попытке отклонить несуществующую заявку")
    fun declineClientMccRequest_Error_RequestNotFound() {
        val result = clientMccGraphQlService.declineClientMccRequest(
            ContextHelper.buildContext(clientWithoutRequestsUserInfo.user!!),
            managedClientUserInfo.login
        )
        assertThat(result).isEqualTo(
            GdClientMccPayload().withValidationResult(
                toGdValidationResult(
                    toGdDefect(path(field("login")), requestNotFound())
                )
            )
        )
    }

    @Test
    @Description("даем отклонять свою заявку оператору управляющему MCC")
    fun declineClientMccRequest_ControlClientOperator_Success() {
        val result = clientMccGraphQlService.declineClientMccRequest(
            ContextHelper.buildContext(controlClientForDeclineUserInfo.user!!, managedClient4UserInfo.user!!),
            managedClient3UserInfo.login
        )
        assertThat(result).isEqualTo(
            GdClientMccPayload()
        )
    }

    @Test
    @Description("удаление связи с управляемым аккаунтом")
    fun unlinkClientMccManagedClient_Success() {
        val result = clientMccGraphQlService.unlinkClientMccManagedClient(
            ContextHelper.buildContext(controlClientForUnlinkUserInfo.user!!),
            managedClientUserInfo.login
        )
        assertThat(result).isEqualTo(
            GdClientMccPayload()
        )
    }

    @Test
    @Description("получение ошибки при попытке удалить несуществующую связь с управляемым клиентом")
    fun unlinkClientMccManagedClient_Error_MccLinkNotFound() {
        val result = clientMccGraphQlService.unlinkClientMccManagedClient(
            ContextHelper.buildContext(clientWithoutRequestsUserInfo.user!!),
            managedClientUserInfo.login
        )
        assertThat(result).isEqualTo(
            GdClientMccPayload().withValidationResult(
                toGdValidationResult(
                    toGdDefect(path(field("login")), mccLinkNotFound())
                )
            )
        )
    }

    @Test
    @Description("даем удалять свою связь с управляемым аккаунтом оператору управляющему MCC")
    fun unlinkClientMccManagedClient_ControlClientOperator_Success() {
        val result = clientMccGraphQlService.unlinkClientMccManagedClient(
            ContextHelper.buildContext(controlClientForUnlinkUserInfo.user!!, managedClientUserInfo.user!!),
            managedClient3UserInfo.login
        )
        assertThat(result).isEqualTo(
            GdClientMccPayload()
        )
    }

    @Test
    @Description("получение данных об управляющих аккаунтах и своих заявках")
    fun getMccClients_Success() {
        val result = clientMccGraphQlService.getMccClients(
            ContextHelper.buildContext(managedClientForGetUserInfo.user!!)
        )
        assertThat(result).usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*metrikaCountersNum").isEqualTo(
                GdMccClients()
                    .withRequests(
                        setOf(
                            controlClientForDeleteUserInfo, controlClientForApproveUserInfo,
                            controlClientForDeclineUserInfo, controlClientForGetRequestsUserInfo
                        )
                            .map { toGdMccRequestShort(it.user, null) }
                            .toSortedSet(
                                compareBy(
                                    { it.controlClient.chiefUser.name },
                                    { it.controlClient.chiefUser.login })
                            )
                    )
                    .withControlClients(
                        setOf(controlClientWithLinkUserInfo, controlClientForUnlinkUserInfo)
                            .map { toGdMccClient(it.user!!) }
                            .toSortedSet(compareBy({ it.info.chiefUser.name }, { it.info.chiefUser.login }))
                    )
                    .withManagedClients(
                        setOf(controlClientWithLinkUserInfo, controlClientForUnlinkUserInfo, managedClientForGetUserInfo)
                            .map { toGdMccClient(it.user!!) }
                            .toSortedSet(compareBy({ it.info.chiefUser.name }, { it.info.chiefUser.login }))
                    )
                    .withAccess(
                        GdMccClientsAccess()
                            .withCanModify(true)
                            .withCanDeleteRequest(true)
                    )
            )
    }

    @Test
    @Description("получение пустых наборов данных об управляющих аккаунтах и своих заявках")
    fun getMccClients_Empty_Success() {
        val result = clientMccGraphQlService.getMccClients(
            ContextHelper.buildContext(clientWithoutRequestsUserInfo.user!!)
        )
        assertThat(result).usingRecursiveComparison().isEqualTo(
                GdMccClients()
                    .withRequests(setOf())
                    .withControlClients(setOf())
                    .withManagedClients(setOf())
                    .withAccess(
                        GdMccClientsAccess()
                            .withCanModify(true)
                            .withCanDeleteRequest(true)
                    )
            )
    }

    @Test
    @Description("даем получать данные об управляющих аккаунтах и своих заявках неглавным представителям")
    fun getMccClients_MainRep_Success() {
        val result = clientMccGraphQlService.getMccClients(
            ContextHelper.buildContext(clientWithoutRequestsMainRepUserInfo.user!!)
        )
        assertThat(result).usingRecursiveComparison().ignoringFieldsMatchingRegexes(".*metrikaCountersNum").isEqualTo(
            GdMccClients().withRequests(
                setOf()
            ).withControlClients(
                setOf()
            ).withManagedClients(
                setOf()
            ).withAccess(
                GdMccClientsAccess().withCanModify(false).withCanDeleteRequest(false)
            )
        )
    }

    @Test
    @Description("не даем получить данные о МСС оператору управляющему MCC")
    fun getMccClients_ControlClientOperator_Failed() {
        assertThatThrownBy {
            clientMccGraphQlService.getMccClients(
                ContextHelper.buildContext(controlClientUserInfo.user!!, managedClientUserInfo.user!!)
            )
        }.isInstanceOf(AccessDeniedException::class.java)
    }

    @Test
    @Description("получение данных о заявках на управление для управляющего аккаунта")
    fun getClientMccControlRequests_Success() {
        val result = clientMccGraphQlService.getClientMccControlRequests(
            ContextHelper.buildContext(controlClientForGetRequestsUserInfo.user!!)
        )
        assertThat(result).usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*metrikaCountersNum").isEqualTo(
                GdClientMccRequestsPayload()
                    .withRequests(
                        setOf(managedClientUserInfo, managedClientForGetUserInfo, managedClient2UserInfo)
                            .map { toGdMccRequestShort(null, it.user) }
                            .toSortedSet(
                                compareBy(
                                    { it.managedClient.chiefUser.name },
                                    { it.managedClient.chiefUser.login })
                            )
                    )
                    .withAccess(
                        GdClientMccRequestsAccess()
                            .withCanApprove(true)
                            .withCanDecline(true)
                    )
            )
    }

    @Test
    @Description("получение пустого набора данных о заявках на управление для управляющего аккаунта")
    fun getClientMccControlRequests_Empty_Success() {
        val result = clientMccGraphQlService.getClientMccControlRequests(
            ContextHelper.buildContext(clientWithoutRequestsUserInfo.user!!)
        )
        assertThat(result).usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*metrikaCountersNum").isEqualTo(
                GdClientMccRequestsPayload()
                    .withRequests(setOf())
                    .withAccess(
                        GdClientMccRequestsAccess()
                            .withCanApprove(true)
                            .withCanDecline(true)
                    )
            )
    }

    @Test
    @Description("получение данных о заявках на управление для обычного представителя управляющего аккаунта")
    fun getClientMccControlRequests_MainRep_Success() {
        val result = clientMccGraphQlService.getClientMccControlRequests(
            ContextHelper.buildContext(clientWithoutRequestsMainRepUserInfo.user!!)
        )
        assertThat(result).usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*metrikaCountersNum").isEqualTo(
                GdClientMccRequestsPayload()
                    .withRequests(setOf())
                    .withAccess(
                        GdClientMccRequestsAccess()
                            .withCanApprove(false)
                            .withCanDecline(false)
                    )
            )
    }

    @Test
    @Description("получение данных о своих заявках оператором управляющим MCC")
    fun getClientMccControlRequests_ControlClientOperator_Success() {
        val result = clientMccGraphQlService.getClientMccControlRequests(
            ContextHelper.buildContext(controlClientForGetRequestsUserInfo.user!!, clientWithoutRequestsUserInfo.user!!)
        )
        assertThat(result).usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*metrikaCountersNum").isEqualTo(
                GdClientMccRequestsPayload()
                    .withRequests(
                        setOf(managedClientUserInfo, managedClientForGetUserInfo, managedClient2UserInfo)
                            .map { toGdMccRequestShort(null, it.user) }
                            .toSortedSet(
                                compareBy(
                                    { it.managedClient.chiefUser.name },
                                    { it.managedClient.chiefUser.login })
                            )
                    )
                    .withAccess(
                        GdClientMccRequestsAccess()
                            .withCanApprove(true)
                            .withCanDecline(true)
                    )
            )
    }
}
