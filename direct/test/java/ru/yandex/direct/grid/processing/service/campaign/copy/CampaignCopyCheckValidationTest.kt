package ru.yandex.direct.grid.processing.service.campaign.copy

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.common.spring.AbstractSpringTest
import ru.yandex.direct.core.entity.campaign.service.validation.CopyCampaignDefects.mustBeClient
import ru.yandex.direct.core.entity.campaign.service.validation.CopyCampaignDefects.mustBeSameSubClient
import ru.yandex.direct.core.entity.user.service.validation.UserDefects.userNotFound
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.validation.defects.RightsDefects.noRights
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext
import ru.yandex.direct.grid.processing.model.campaign.GdCheckCopyCampaigns
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.grid.processing.util.validation.asGridValidationException
import ru.yandex.direct.grid.processing.util.validation.extractingValidationResult
import ru.yandex.direct.grid.processing.util.validation.hasErrorsWith
import ru.yandex.direct.rbac.RbacRole
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.path

@GridProcessingTest
@RunWith(JUnitParamsRunner::class)
class CampaignCopyCheckValidationTest : AbstractSpringTest() {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var campaignCopyMutationService: CampaignCopyMutationService

    private lateinit var context: GridGraphQLContext

    private lateinit var operator: ClientInfo
    private lateinit var clientFrom: ClientInfo
    private lateinit var clientTo: ClientInfo

    @Before
    fun before() {
        operator = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER)
        clientFrom = steps.clientSteps().createDefaultClient()
        clientTo = steps.clientSteps().createDefaultClient()
        context = ContextHelper.buildContext(operator.chiefUserInfo!!.user)
    }

    @Test
    fun `valid logins success`() {
        val input = GdCheckCopyCampaigns(loginFrom = clientFrom.login, loginTo = clientTo.login)

        assertThatCode { campaignCopyMutationService.checkCopyCampaigns(context, input) }
            .doesNotThrowAnyException()
    }

    @Test
    fun `non-existent loginFrom throws validation error`() {
        val input = GdCheckCopyCampaigns(
            loginFrom = "non-existent-login-from",
            loginTo = clientTo.login,
        )

        assertThatThrownBy { campaignCopyMutationService.checkCopyCampaigns(context, input) }
            .asGridValidationException().extractingValidationResult()
            .hasErrorsWith(path(field(GdCheckCopyCampaigns::loginFrom)), userNotFound())
    }

    @Test
    fun `non-existent loginTo throws validation error`() {
        val input = GdCheckCopyCampaigns(
            loginFrom = clientFrom.login,
            loginTo = "non-existent-login-to",
        )

        assertThatThrownBy { campaignCopyMutationService.checkCopyCampaigns(context, input) }
            .asGridValidationException().extractingValidationResult()
            .hasErrorsWith(path(field(GdCheckCopyCampaigns::loginTo)), userNotFound())
    }

    fun nonAllowedRoles() = RbacRole.values().toSet() - setOf(
        RbacRole.SUPER, RbacRole.MANAGER, RbacRole.SUPPORT,
        RbacRole.PLACER, RbacRole.AGENCY, RbacRole.LIMITED_SUPPORT,
    )

    @Test
    @Parameters(method = "nonAllowedRoles")
    fun `non-allowed role throws validation error`(operatorRole: RbacRole) {
        val operator = steps.clientSteps().createDefaultClientWithRole(operatorRole)
        val context = ContextHelper.buildContext(operator.chiefUserInfo!!.user)

        val input = GdCheckCopyCampaigns(loginFrom = clientFrom.login, loginTo = clientTo.login)

        assertThatThrownBy { campaignCopyMutationService.checkCopyCampaigns(context, input) }
            .asGridValidationException().extractingValidationResult()
            .hasErrorsWith(path(), noRights())
    }

    @Test
    fun `non-client throws validation error`() {
        val clientFrom = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY)
        val clientTo = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPERREADER)

        val input = GdCheckCopyCampaigns(loginFrom = clientFrom.login, loginTo = clientTo.login)

        assertThatThrownBy { campaignCopyMutationService.checkCopyCampaigns(context, input) }
            .asGridValidationException().extractingValidationResult()
            .hasErrorsWith(path(field(GdCheckCopyCampaigns::loginFrom)), mustBeClient())
            .hasErrorsWith(path(field(GdCheckCopyCampaigns::loginTo)), mustBeClient())
    }

    @Test
    fun `agency with access success`() {
        val operator = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY)
        val context = ContextHelper.buildContext(operator.chiefUserInfo!!.user)

        val clientFrom = steps.clientSteps().createClientUnderAgency(operator)
        val clientTo = clientFrom

        val input = GdCheckCopyCampaigns(loginFrom = clientFrom.login, loginTo = clientTo.login)

        assertThatCode { campaignCopyMutationService.checkCopyCampaigns(context, input) }
            .doesNotThrowAnyException()
    }

    @Test
    fun `agency different subclients throws validation error`() {
        val operator = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY)
        val context = ContextHelper.buildContext(operator.chiefUserInfo!!.user)

        val clientFrom = steps.clientSteps().createClientUnderAgency(operator)
        val clientTo = steps.clientSteps().createClientUnderAgency(operator)

        val input = GdCheckCopyCampaigns(loginFrom = clientFrom.login, loginTo = clientTo.login)

        assertThatThrownBy { campaignCopyMutationService.checkCopyCampaigns(context, input) }
            .asGridValidationException().extractingValidationResult()
            .hasErrorsWith(path(field(GdCheckCopyCampaigns::loginFrom)), mustBeSameSubClient())
            .hasErrorsWith(path(field(GdCheckCopyCampaigns::loginTo)), mustBeSameSubClient())
    }

    @Test
    fun `agency without access throws validation error`() {
        val operator = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY)
        val context = ContextHelper.buildContext(operator.chiefUserInfo!!.user)

        val input = GdCheckCopyCampaigns(loginFrom = clientFrom.login, loginTo = clientTo.login)

        assertThatThrownBy { campaignCopyMutationService.checkCopyCampaigns(context, input) }
            .asGridValidationException().extractingValidationResult()
            .hasErrorsWith(path(field(GdCheckCopyCampaigns::loginFrom)), noRights())
            .hasErrorsWith(path(field(GdCheckCopyCampaigns::loginTo)), noRights())
    }

    @Test
    fun `limited support with access success`() {
        val operator = steps.clientSteps().createDefaultClientWithRole(RbacRole.LIMITED_SUPPORT)
        val context = ContextHelper.buildContext(operator.chiefUserInfo!!.user)

        steps.idmGroupSteps().addSupportForClient(operator.chiefUserInfo, clientFrom)
        steps.idmGroupSteps().addSupportForClient(operator.chiefUserInfo, clientTo)

        val input = GdCheckCopyCampaigns(loginFrom = clientFrom.login, loginTo = clientTo.login)

        assertThatCode { campaignCopyMutationService.checkCopyCampaigns(context, input) }
            .doesNotThrowAnyException()
    }

    @Test
    fun `limited support no access throws validation error`() {
        val operator = steps.clientSteps().createDefaultClientWithRole(RbacRole.LIMITED_SUPPORT)
        val context = ContextHelper.buildContext(operator.chiefUserInfo!!.user)

        val input = GdCheckCopyCampaigns(loginFrom = clientFrom.login, loginTo = clientTo.login)

        assertThatThrownBy { campaignCopyMutationService.checkCopyCampaigns(context, input) }
            .asGridValidationException().extractingValidationResult()
            .hasErrorsWith(path(field(GdCheckCopyCampaigns::loginFrom)), noRights())
            .hasErrorsWith(path(field(GdCheckCopyCampaigns::loginTo)), noRights())
    }
}
