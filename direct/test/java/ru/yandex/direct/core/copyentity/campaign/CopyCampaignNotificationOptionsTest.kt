package ru.yandex.direct.core.copyentity.campaign

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.copyentity.model.CopyCampaignFlags
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.entity.user.service.UserService
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestUsers
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo
import ru.yandex.direct.rbac.RbacRole

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyCampaignNotificationOptionsTest : BaseCopyCampaignTest() {

    @Autowired
    private lateinit var userService: UserService

    @Before
    fun before() {
        superClient = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER)
        client = steps.clientSteps().createDefaultClient()
        steps.featureSteps().setCurrentClient(client.clientId)
    }

    private fun customCopyNotifications() = listOf(
        listOf(false, false, false),
        listOf(false, false, true),
        listOf(false, true, false),
        listOf(false, true, true),
        listOf(true, false, false),
        listOf(true, false, true),
        listOf(true, true, false),
        listOf(true, true, true),
    )

    @Test
    @TestCaseName(
        "{method}(campaign.setNotificationFields={0}, targetChiefUser.setNotificationFields={1}, " +
            "copyNotificationSettings={2})"
    )
    @Parameters(method = "customCopyNotifications")
    fun testEnableCheckPositionEventCopy(
        setNotificationFieldsInCampaign: Boolean,
        setNotificationFieldsInUser: Boolean,
        copyNotificationSettings: Boolean,
    ) {
        val user: User = TestUsers.generateNewUser()
        if (!setNotificationFieldsInUser) {
            user.sendAccNews = false
        }

        targetClient = steps.clientSteps().createDefaultClient(user)

        val chiefUser: User = userService.getUser(targetClient.chiefUserInfo!!.chiefUid)!!

        assertThat(chiefUser.sendAccNews)
            .describedAs("Saved target chief user has unexpected 'sendAccNews' value")
            .isEqualTo(user.sendAccNews)

        val textCampaign = TestTextCampaigns.fullTextCampaign().withEmail("test@test.ru")
        if (setNotificationFieldsInCampaign) {
            textCampaign.fio = "Иванов Иван Иванович"
            textCampaign.enableSendAccountNews = true
        } else {
            textCampaign.fio = ""
            textCampaign.enableSendAccountNews = false
        }

        val textCampaignInfo: TextCampaignInfo = steps.textCampaignSteps().createCampaign(client, textCampaign)

        val copiedCampaign: TextCampaign = copyValidCampaignBetweenClients(
            campaign = textCampaignInfo,
            flags = CopyCampaignFlags(isCopyNotificationSettings = copyNotificationSettings)
        )

        if (copyNotificationSettings) {
            softly {
                assertThat(copiedCampaign.fio)
                    .describedAs("Copied text campaign has wrong 'fio' value")
                    .isEqualTo(textCampaignInfo.typedCampaign.fio)

                assertThat(copiedCampaign.email)
                    .describedAs("Copied text campaign has wrong 'email' value")
                    .isEqualTo(textCampaignInfo.typedCampaign.email)

                assertThat(copiedCampaign.enableSendAccountNews)
                    .describedAs("Copied text campaign has wrong 'enableSendAccountNews' value")
                    .isEqualTo(textCampaignInfo.typedCampaign.enableSendAccountNews)
            }
        } else {
            softly {
                assertThat(copiedCampaign.fio)
                    .describedAs("Copied text campaign 'fio' value not equals to target chief user 'fio' value")
                    .isEqualTo(chiefUser.fio)


                assertThat(copiedCampaign.email)
                    .describedAs(
                        "Copied text campaign 'email' value not equals to target chief user 'email' value"
                    )
                    .isEqualTo(chiefUser.email)

                assertThat(copiedCampaign.enableSendAccountNews)
                    .describedAs(
                        "Copied text campaign 'enableSendAccountNews' value not equals to " +
                            "target chief user 'sendAccNews' value"
                    )
                    .isEqualTo(chiefUser.sendAccNews)
            }
        }
    }
}
