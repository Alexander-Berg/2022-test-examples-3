package ru.yandex.direct.grid.processing.service.group

import org.apache.commons.lang.RandomStringUtils
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.rules.ExpectedException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository
import ru.yandex.direct.core.testing.data.TestClients
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.FeedInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifiers
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddAdGroupPayload
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddTextAdGroup
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddTextAdGroupItem
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.regions.Region

open class AdGroupMutationServiceTextGroupWithFeedTestBase {
    @Rule
    @JvmField
    val methodRule = SpringMethodRule()

    @Rule
    @JvmField
    var thrown: ExpectedException = ExpectedException.none()


    @Autowired
    protected lateinit var steps: Steps

    @Autowired
    protected lateinit var adGroupMutationService: AdGroupMutationService

    @Autowired
    protected lateinit var adGroupRepository: AdGroupRepository

    protected lateinit var clientInfo: ClientInfo
    protected lateinit var clientId: ClientId
    protected var uid: Long = 0
    protected var shard: Int = 0
    protected var campaignId: Long = 0
    protected lateinit var feedInfo: FeedInfo

    @Before
    fun before() {
        clientInfo =
            steps.clientSteps().createClient(TestClients.defaultClient().withCountryRegionId(Region.RUSSIA_REGION_ID))
        clientId = clientInfo.clientId!!

        val user = clientInfo.chiefUserInfo!!.user!!
        uid = user.uid
        shard = clientInfo.shard

        val campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo)
        campaignId = campaignInfo.campaignId

        feedInfo = steps.feedSteps().createDefaultFeed(clientInfo)

        TestAuthHelper.setDirectAuthentication(user)
    }

    private fun makeGdAddTextAdGroup(additionalConfig: GdAddTextAdGroupItem.() -> Unit) =
        GdAddTextAdGroup().apply {
            this.addItems = listOf(
                GdAddTextAdGroupItem().apply {
                    this.name = ADGROUP_NAME
                    this.campaignId = this@AdGroupMutationServiceTextGroupWithFeedTestBase.campaignId
                    this.adGroupMinusKeywords = listOf()
                    this.libraryMinusKeywordsIds = listOf()
                    this.bidModifiers = GdUpdateBidModifiers()
                    this.regionIds = listOf(Region.MOSCOW_REGION_ID.toInt())
                }.apply(additionalConfig)
            )
        }

    protected fun addTextAdGroups(additionalConfig: GdAddTextAdGroupItem.() -> Unit): GdAddAdGroupPayload =
        adGroupMutationService.addTextAdGroups(clientId, uid, uid, makeGdAddTextAdGroup(additionalConfig))


    companion object {
        @ClassRule
        @JvmField
        val classRule = SpringClassRule()

        val ADGROUP_NAME: String = RandomStringUtils.randomAlphanumeric(16)
    }
}
