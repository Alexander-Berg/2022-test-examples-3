package ru.yandex.direct.grid.processing.service.group

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.adgroup.model.AdShowType
import ru.yandex.direct.core.entity.userssegments.repository.UsersSegmentRepository
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifiers
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddTextAdGroupItem
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateTextAdGroupItem
import ru.yandex.direct.grid.processing.util.KtGraphQLTestExecutor
import ru.yandex.direct.grid.processing.util.TestAuthHelper

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class AdGroupGraphQlServiceCollectAudienceTest {
    @Autowired
    private lateinit var steps: Steps
    @Autowired
    private lateinit var ktGraphQLTestExecutor: KtGraphQLTestExecutor
    @Autowired
    private lateinit var usersSegmentRepository: UsersSegmentRepository

    private lateinit var clientInfo: ClientInfo
    private lateinit var textCampaignInfo: TextCampaignInfo

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        val user = clientInfo.chiefUserInfo!!.user!!
        TestAuthHelper.setDirectAuthentication(user)
        ktGraphQLTestExecutor.withDefaultGraphQLContext(user)

        textCampaignInfo = steps.textCampaignSteps().createDefaultCampaign(clientInfo)
    }

    @Test
    fun enabledOnAdd_disabledOnUpdate() {
        val addAdGroupItem = createAddTextAdGroupItem(collectAudience = true)
        val payload = ktGraphQLTestExecutor.addTextAdGroup(addAdGroupItem)
        assertThat(payload.addedAdGroupItems).hasSize(1)
        val adGroupId = payload.addedAdGroupItems[0].adGroupId
        var segment = usersSegmentRepository.getSegmentByPrimaryKey(clientInfo.shard, adGroupId, AdShowType.START)
        assertThat(segment.isDisabled).isEqualTo(false)

        val updateAdGroupItem = createUpdateTextAdGroupItem(adGroupId, collectAudience = false)
        ktGraphQLTestExecutor.updateTextAdGroup(updateAdGroupItem)
        segment = usersSegmentRepository.getSegmentByPrimaryKey(clientInfo.shard, adGroupId, AdShowType.START)
        assertThat(segment.isDisabled).isEqualTo(true)
    }

    @Test
    fun disabledOnAdd_enabledOnUpdate() {
        val addAdGroupItem = createAddTextAdGroupItem(collectAudience = false)
        val payload = ktGraphQLTestExecutor.addTextAdGroup(addAdGroupItem)
        assertThat(payload.addedAdGroupItems).hasSize(1)
        val adGroupId = payload.addedAdGroupItems[0].adGroupId
        var segment = usersSegmentRepository.getSegmentByPrimaryKey(clientInfo.shard, adGroupId, AdShowType.START)
        assertThat(segment).isNull()

        val updateAdGroupItem = createUpdateTextAdGroupItem(adGroupId, collectAudience = true)
        ktGraphQLTestExecutor.updateTextAdGroup(updateAdGroupItem)
        segment = usersSegmentRepository.getSegmentByPrimaryKey(clientInfo.shard, adGroupId, AdShowType.START)
        assertThat(segment.isDisabled).isEqualTo(false)
    }

    private fun createAddTextAdGroupItem(collectAudience: Boolean) =
        GdAddTextAdGroupItem().apply {
            name = "group name"
            campaignId = textCampaignInfo.id
            regionIds = listOf(225)
            this.collectAudience = collectAudience
            adGroupMinusKeywords = listOf()
            libraryMinusKeywordsIds = listOf()
            bidModifiers = GdUpdateBidModifiers()
        }

    private fun createUpdateTextAdGroupItem(adGroupId: Long, collectAudience: Boolean) =
        GdUpdateTextAdGroupItem().apply {
            this.adGroupId = adGroupId
            adGroupName = "group name"
            regionIds = listOf(225)
            this.collectAudience = collectAudience
            adGroupMinusKeywords = listOf()
            libraryMinusKeywordsIds = listOf()
            bidModifiers = GdUpdateBidModifiers()
        }
}
