package ru.yandex.direct.grid.processing.service.client

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.whenever
import jdk.jfr.Description
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.client.service.ClientService
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.entity.user.service.UserService
import ru.yandex.direct.core.security.AccessDeniedException
import ru.yandex.direct.core.service.integration.balance.BalanceService
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.client.GdClientRepresentative
import ru.yandex.direct.grid.processing.model.client.GdClientRepresentatives
import ru.yandex.direct.grid.processing.model.client.GdClientRepresentativesAccess
import ru.yandex.direct.grid.processing.model.client.GdDeletedClientRepresentative
import ru.yandex.direct.grid.processing.model.client.GdDeletedClientRepresentativeAccess
import ru.yandex.direct.grid.processing.model.client.GdDeletedClientRepresentativeInfo
import ru.yandex.direct.grid.processing.service.operator.UserDataConverter.toGdUserInfo
import ru.yandex.direct.grid.processing.util.ContextHelper.buildContext
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.rbac.PpcRbac
import ru.yandex.direct.rbac.RbacRole
import java.util.Optional

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class ClientRepresentativesGraphQlServiceTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var clientService: ClientService

    @Autowired
    private lateinit var ppcRbac: PpcRbac

    private lateinit var clientInfo: ClientInfo
    private lateinit var client2Info: ClientInfo
    private lateinit var clientChiefRep: UserInfo
    private lateinit var client2ChiefRep: UserInfo
    private lateinit var clientRep1: UserInfo
    private lateinit var clientRep2: UserInfo
    private lateinit var clientReadonlyRep1: UserInfo
    private lateinit var deletedRep1: User
    private lateinit var deletedRep2: User
    private lateinit var deletedRep3: User

    private lateinit var superUser: User
    private lateinit var supportUser: User
    private lateinit var managerUser: User
    private lateinit var mediaplannerUser: User
    private lateinit var mccControlUser: User

    private lateinit var clientRepresentativesGraphQlService: ClientRepresentativesGraphQlService
    private lateinit var balanceService: BalanceService

    @Before
    fun setUp() {

        balanceService = mock()
        clientRepresentativesGraphQlService = ClientRepresentativesGraphQlService(
            userService,
            clientService,
            ppcRbac,
            balanceService
        )

        managerUser = steps.clientSteps().createDefaultClientWithRole(RbacRole.MANAGER).chiefUserInfo!!.user!!

        clientInfo = steps.clientSteps().createDefaultClient()
        client2Info = steps.clientSteps().createDefaultClient()
        clientChiefRep = clientInfo.chiefUserInfo!!
        client2ChiefRep = client2Info.chiefUserInfo!!
        clientRep1 = steps.userSteps().createRepresentativeWithFio(clientInfo, "fio3")
        clientRep2 = steps.userSteps().createRepresentativeWithFio(clientInfo, "fio1")
        clientReadonlyRep1 = steps.userSteps().createReadonlyRepresentativeWithFio(clientInfo, "fio2")

        deletedRep1 = steps.userSteps().createUserInBlackboxStub()
        deletedRep1.apply {
            fio = "fio3"
            phone = "111111"
        }
        deletedRep2 = steps.userSteps().createUserInBlackboxStub()
        deletedRep2.apply {
            fio = "fio1"
            phone = "222222"
        }
        deletedRep3 = steps.userSteps().createUserInBlackboxStub()
        deletedRep3.apply {
            fio = "fio2"
            phone = "333333"
        }

        superUser = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER).chiefUserInfo!!.user!!
        supportUser = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPPORT).chiefUserInfo!!.user!!
        mediaplannerUser = steps.clientSteps().createDefaultClientWithRole(RbacRole.MEDIA).chiefUserInfo!!.user!!
        mccControlUser = steps.clientSteps().createDefaultClient().chiefUserInfo!!.user!!

        steps.userSteps().setDeletedUsers(clientInfo, setOf(deletedRep1, deletedRep2, deletedRep3))

        whenever(balanceService.findClientIdByUid(eq(deletedRep1.uid))).thenReturn(Optional.of(client2Info.clientId!!))
        whenever(balanceService.findClientIdByUid(eq(deletedRep2.uid))).thenReturn(Optional.empty())

        TestAuthHelper.setDirectAuthentication(clientChiefRep.user!!)
    }

    @After
    fun cleanUp() {
        reset(balanceService)
    }

    @Test
    @Description("получение данных о представителях клиента")
    fun getClientRepresentatives_Success() {
        val result = clientRepresentativesGraphQlService.getClientRepresentatives(
            buildContext(clientChiefRep.user!!).withFetchedFieldsReslover(null)
        )
        assertThat(result).usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*metrikaCountersNum").isEqualTo(
                GdClientRepresentatives()
                    .withChief(
                        GdClientRepresentative().withInfo(
                            toGdUserInfo(clientChiefRep.user!!)
                        )
                    )
                    .withActive(
                        setOf(clientRep1, clientRep2, clientReadonlyRep1)
                            .map {
                                GdClientRepresentative().withInfo(toGdUserInfo(it.user!!))
                            }
                            .toSortedSet(compareBy({ it.info.name }, { it.info.login }))
                    )
                    .withDeleted(
                        setOf(deletedRep1, deletedRep2, deletedRep3).map {
                            GdDeletedClientRepresentative()
                                .withInfo(
                                    GdDeletedClientRepresentativeInfo()
                                        .withUserId(it.uid)
                                        .withLogin(it.login)
                                        .withName(it.fio)
                                        .withEmail(it.email)
                                        .withPhone(it.phone)
                                )
                                .withAccess(
                                    GdDeletedClientRepresentativeAccess()
                                        .withCanRestore(it.uid != deletedRep1.uid)
                                )
                        }.toSortedSet(compareBy({ it.info.name }, { it.info.login }))
                    )
                    .withAccess(
                        GdClientRepresentativesAccess()
                            .withCanAddRepresentative(true)
                            .withCanChangeChiefRepresentative(true)
                    )
            )
    }

    @Test
    @Description("не даем получать данные о представителях клиента readonly-оператором")
    fun getClientRepresentatives_ByReadonlyOperator() {
        assertThatThrownBy {
            clientRepresentativesGraphQlService
                .getClientRepresentatives(buildContext(clientReadonlyRep1.user!!).withFetchedFieldsReslover(null))
        }.isInstanceOf(AccessDeniedException::class.java)
    }

    @Test
    @Description("получение данных о представителях клиента для случая только главного представителя")
    fun getClientRepresentatives_OnlyChief() {
        val result = clientRepresentativesGraphQlService.getClientRepresentatives(
            buildContext(client2ChiefRep.user!!).withFetchedFieldsReslover(null)
        )
        assertThat(result).usingRecursiveComparison().isEqualTo(
            getExpectedResult(client2ChiefRep)
        )
    }

    @Test
    @Description("запрещаем получение данных о представителях клиента для управляющего MCC")
    fun getClientRepresentatives_ByOperatorControlMCC_Failed() {
        assertThatThrownBy {
            clientRepresentativesGraphQlService.getClientRepresentatives(
                buildContext(
                    mccControlUser,
                    client2ChiefRep.user!!
                ).withFetchedFieldsReslover(null)
            )
        }.isInstanceOf(AccessDeniedException::class.java)
    }

    @Test
    @Description("запрещаем получение данных о представителях клиента для обычного представителя")
    fun getClientRepresentatives_ByOperatorMainRep_Failed() {
        assertThatThrownBy {
            clientRepresentativesGraphQlService
                .getClientRepresentatives(buildContext(clientRep1.user).withFetchedFieldsReslover(null))
        }.isInstanceOf(AccessDeniedException::class.java)
    }

    @Test
    @Description("запрещаем получение данных о представителях клиента для медиапланера")
    fun getClientRepresentatives_ByMediaplanner_Failed() {
        assertThatThrownBy {
            clientRepresentativesGraphQlService
                .getClientRepresentatives(
                    buildContext(
                        mediaplannerUser,
                        client2ChiefRep.user!!
                    ).withFetchedFieldsReslover(null)
                )
        }.isInstanceOf(AccessDeniedException::class.java)
    }

    private fun getExpectedResult(userInfo: UserInfo): GdClientRepresentatives {
        return GdClientRepresentatives().withChief(
            GdClientRepresentative().withInfo(
                toGdUserInfo(userInfo.user!!)
            )
        ).withActive(setOf()).withDeleted(setOf()).withAccess(
            GdClientRepresentativesAccess().withCanAddRepresentative(true).withCanChangeChiefRepresentative(true)
        )
    }

    @Test
    @Description("получение данных о представителях клиента для оператора-супера")
    fun getClientRepresentatives_ByOperatorSuper() {
        val result = clientRepresentativesGraphQlService.getClientRepresentatives(
            buildContext(
                superUser,
                client2ChiefRep.user!!
            ).withFetchedFieldsReslover(null)
        )
        assertThat(result).usingRecursiveComparison().isEqualTo(
            getExpectedResult(client2ChiefRep)
        )
    }

    @Test
    @Description("получение данных о представителях клиента для оператора-саппорта")
    fun getClientRepresentatives_ByOperatorSupport() {
        val result = clientRepresentativesGraphQlService.getClientRepresentatives(
            buildContext(
                supportUser,
                client2ChiefRep.user!!
            ).withFetchedFieldsReslover(null)
        )
        assertThat(result).usingRecursiveComparison().isEqualTo(
            getExpectedResult(client2ChiefRep)
        )
    }

    @Test
    @Description("получение данных о представителях клиента для оператора-менеджера")
    fun getClientRepresentatives_ByOperatorManager() {
        val result = clientRepresentativesGraphQlService.getClientRepresentatives(
            buildContext(
                managerUser,
                client2ChiefRep.user!!
            ).withFetchedFieldsReslover(null)
        )
        assertThat(result).usingRecursiveComparison().isEqualTo(
            getExpectedResult(client2ChiefRep)
        )
    }
}
