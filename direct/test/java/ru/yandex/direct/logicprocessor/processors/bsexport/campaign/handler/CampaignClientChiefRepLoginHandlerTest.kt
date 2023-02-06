package ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.campaign.Campaign
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.user.repository.UserRepository
import ru.yandex.direct.currency.CurrencyCode
import javax.annotation.ParametersAreNonnullByDefault

@ParametersAreNonnullByDefault
class CampaignClientChiefRepLoginHandlerTest {
    private val UID_1 = 1L
    private val UID_2 = 123L
    private val LOGIN_1 = "login1"
    private val LOGIN_2 = "any_Login"
    private val userRepository = mock<UserRepository> {
        on(it.getLoginsByUids(any())) doReturn mapOf(
            UID_1 to LOGIN_1,
            UID_2 to LOGIN_2,
        )
    }
    private val handler = CampaignClientChiefRepLoginHandler(userRepository)

    @Test
    fun `text campaign`() {
        val expectedProto = Campaign.newBuilder()
            .setClientChiefRepLogin(LOGIN_1)
            .buildPartial()

        CampaignHandlerAssertions.assertCampaignHandledCorrectly(
            handler,
            campaign = TextCampaign()
                .withId(1)
                .withCurrency(CurrencyCode.RUB)
                .withName("")
                .withStatusArchived(false)
                .withStatusShow(true)
                .withClientId(15L)
                .withType(CampaignType.TEXT)
                .withUid(UID_1),
            expectedProto = expectedProto,
        )
    }
}
