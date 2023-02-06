package ru.yandex.direct.jobs.feeds

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.StatusBsSynced
import ru.yandex.direct.core.entity.adgroup.model.AdGroupWithFeedId
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService
import ru.yandex.direct.core.entity.feed.model.Source
import ru.yandex.direct.core.entity.feed.model.UpdateStatus
import ru.yandex.direct.core.entity.feed.repository.FeedRepository
import ru.yandex.direct.core.entity.feed.service.FeedService
import ru.yandex.direct.core.entity.uac.service.GrutUacCampaignService
import ru.yandex.direct.core.testing.data.TestFeeds.defaultFeed
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.FeedInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.jobs.configuration.GrutJobsTest
import ru.yandex.direct.jobs.feed.DeleteDuplicatesSiteFeedsJob
import ru.yandex.direct.jobs.feed.repository.DeleteDuplicatesSiteFeedsRepository
import ru.yandex.direct.libs.mirrortools.utils.HostingsHandler.stripDomainTail
import ru.yandex.direct.libs.mirrortools.utils.HostingsHandler.stripProtocol
import ru.yandex.direct.rbac.RbacService
import ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher
import java.time.LocalDateTime

@GrutJobsTest
@ExtendWith(SpringExtension::class)
class DeleteDuplicatesSiteFeedsJobTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var grutSteps: GrutSteps

    @Autowired
    private lateinit var deleteDuplicatesSiteFeedsRepository: DeleteDuplicatesSiteFeedsRepository

    @Autowired
    private lateinit var rbacService: RbacService

    @Autowired
    private lateinit var feedService: FeedService

    @Autowired
    private lateinit var feedRepository: FeedRepository

    @Autowired
    private lateinit var adGroupService: AdGroupService

    @Autowired
    private lateinit var grutUacCampaignService: GrutUacCampaignService

    private lateinit var job: DeleteDuplicatesSiteFeedsJob

    private lateinit var clientInfo: ClientInfo

    private var shard: Int = 0

    @BeforeEach
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        shard = clientInfo.shard
        job = DeleteDuplicatesSiteFeedsJob(
            deleteDuplicatesSiteFeedsRepository,
            rbacService,
            feedService,
            feedRepository,
            grutUacCampaignService,
        ).withShard(shard) as DeleteDuplicatesSiteFeedsJob
        grutSteps.createClient(clientInfo)
    }

    @Test
    fun execute_success() {
        val commonUrl = "https://ya.ru"
        val url = "https://abc.com"

        val feed1 = defaultFeed(null)
            .withUrl(commonUrl)
            .withName("feed1")
            .withTargetDomain(stripDomainTail(stripProtocol(commonUrl)))
            .withSource(Source.SITE)
        val feedInfo1 = FeedInfo().withFeed(feed1).withClientInfo(clientInfo)
        val feedInDynamicAdGroup = steps.feedSteps().createFeed(feedInfo1)
        val dynamicAdGroup = steps.adGroupSteps().createActiveDynamicFeedAdGroup(feedInDynamicAdGroup)

        val feed2 = defaultFeed(null).withUpdateStatus(UpdateStatus.NEW)
            .withUrl(commonUrl)
            .withName("feed2")
            .withTargetDomain(stripDomainTail(stripProtocol(commonUrl)))
            .withSource(Source.SITE)
            .withUpdateStatus(UpdateStatus.NEW)
        val feedInfo2 = FeedInfo().withFeed(feed2).withClientInfo(clientInfo)
        val feedInPerformanceAdGroup = steps.feedSteps().createFeed(feedInfo2)
        val performanceAdGroup = steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feedInPerformanceAdGroup.feedId)

        val feed3 = defaultFeed(null).withUpdateStatus(UpdateStatus.NEW)
            .withUrl(url)
            .withName("feed3")
            .withTargetDomain(stripDomainTail(stripProtocol(url)))
            .withSource(Source.SITE)
        val feedInfo3 = FeedInfo().withFeed(feed3).withClientInfo(clientInfo)
        val feedWithUniqueUrlInPerformanceAdGroup = steps.feedSteps().createFeed(feedInfo3)
        val performanceAdGroupWithUniqueUrl = steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feedWithUniqueUrlInPerformanceAdGroup.feedId)

        val duplicatesBeforeExecute = deleteDuplicatesSiteFeedsRepository.getDuplicateSiteFeedIds(shard)

        job.execute()

        val duplicatesAfterExecute = deleteDuplicatesSiteFeedsRepository.getDuplicateSiteFeedIds(shard)

        val actualDynamicAdGroup = adGroupService.getAdGroup(dynamicAdGroup.adGroupId)
        val actualPerformanceAdGroup = adGroupService.getAdGroup(performanceAdGroup.adGroupId) as AdGroupWithFeedId
        val actualPerformanceAdGroupWithUniqueUrl = adGroupService.getAdGroup(performanceAdGroupWithUniqueUrl.adGroupId)
        softly {
            assertThat(duplicatesBeforeExecute).contains(feed1.id, feed2.id)
            assertThat(duplicatesAfterExecute).doesNotContain(feed1.id, feed2.id, feed3.id)
            assertThat(actualDynamicAdGroup).usingRecursiveComparison()
                .ignoringFields("lastChange", "relevanceMatchCategories")
                .withComparatorForType(LocalDateTimeMatcher.approximatelyTimeComparator(1), LocalDateTime::class.java)
                .isEqualTo(dynamicAdGroup.adGroup.withStatusBsSynced(StatusBsSynced.YES))
            assertThat(actualPerformanceAdGroup).usingRecursiveComparison()
                .ignoringFields("lastChange", "relevanceMatchCategories")
                .withComparatorForType(LocalDateTimeMatcher.approximatelyTimeComparator(1), LocalDateTime::class.java)
                .isEqualTo((performanceAdGroup.adGroup.withStatusBsSynced(StatusBsSynced.NO) as AdGroupWithFeedId).withFeedId(feed1.id))
            assertThat(actualPerformanceAdGroupWithUniqueUrl).usingRecursiveComparison()
                .ignoringFields("lastChange", "relevanceMatchCategories")
                .withComparatorForType(LocalDateTimeMatcher.approximatelyTimeComparator(1), LocalDateTime::class.java)
                .isEqualTo(performanceAdGroupWithUniqueUrl.adGroup.withStatusBsSynced(StatusBsSynced.YES))
        }
    }

    @Test
    fun execute_withNullTagetDomain() {
        val url = "https://url1.ru"

        val feed1 = defaultFeed(null)
            .withUrl(url)
            .withName("feed1")
            .withTargetDomain(null)
            .withSource(Source.SITE)
        val feedInfo1 = FeedInfo().withFeed(feed1).withClientInfo(clientInfo)
        val feedInDynamicAdGroup = steps.feedSteps().createFeed(feedInfo1)
        steps.adGroupSteps().createActiveDynamicFeedAdGroup(feedInDynamicAdGroup)

        val feed2 = defaultFeed(null).withUpdateStatus(UpdateStatus.NEW)
            .withUrl(url)
            .withName("feed2")
            .withTargetDomain(null)
            .withSource(Source.SITE)
            .withUpdateStatus(UpdateStatus.NEW)
        val feedInfo2 = FeedInfo().withFeed(feed2).withClientInfo(clientInfo)
        val feedInPerformanceAdGroup = steps.feedSteps().createFeed(feedInfo2)
        steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feedInPerformanceAdGroup.feedId)

        val duplicatesBeforeExecute = deleteDuplicatesSiteFeedsRepository.getDuplicateSiteFeedIds(shard)
        assertThat(duplicatesBeforeExecute).doesNotContain(feed1.id, feed2.id)
    }

    @Test
    fun execute_withFeedsIsUsedInCampaignBrief() {
        val url = "https://url2.ru"

        val feed1 = defaultFeed(null)
            .withUrl(url)
            .withName("feed1")
            .withTargetDomain(stripDomainTail(stripProtocol(url)))
            .withSource(Source.SITE)
        val feedInfo1 = FeedInfo().withFeed(feed1).withClientInfo(clientInfo)
        val feedInDynamicAdGroup = steps.feedSteps().createFeed(feedInfo1)
        val dynamicAdGroup = steps.adGroupSteps().createActiveDynamicFeedAdGroup(feedInDynamicAdGroup)
        grutSteps.createAndGetTextCampaign(clientInfo, true, feed1.id, null, null)

        val feed2 = defaultFeed(null).withUpdateStatus(UpdateStatus.NEW)
            .withUrl(url)
            .withName("feed2")
            .withTargetDomain(stripDomainTail(stripProtocol(url)))
            .withSource(Source.SITE)
            .withUpdateStatus(UpdateStatus.NEW)
        val feedInfo2 = FeedInfo().withFeed(feed2).withClientInfo(clientInfo)
        val feedInPerformanceAdGroup = steps.feedSteps().createFeed(feedInfo2)
        val performanceAdGroup = steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feedInPerformanceAdGroup.feedId)
        grutSteps.createAndGetTextCampaign(clientInfo, true, feed2.id, null, null)

        val feedBriefs = grutUacCampaignService.getCampaignsByFeedIds(clientInfo.clientId!!.toString(), listOf(feed1.id, feed2.id))

        job.execute()

        val duplicatesAfterExecution = deleteDuplicatesSiteFeedsRepository.getDuplicateSiteFeedIds(shard)

        val actualDynamicAdGroup = adGroupService.getAdGroup(dynamicAdGroup.adGroupId)
        val actualPerformanceAdGroup = adGroupService.getAdGroup(performanceAdGroup.adGroupId) as AdGroupWithFeedId

        val feedBriefsAfterExecution = grutUacCampaignService.getCampaigns(feedBriefs.values.flatten().map { it.id })
        softly {
            assertThat(duplicatesAfterExecution).doesNotContain(feed1.id, feed2.id)
            assertThat(actualDynamicAdGroup).usingRecursiveComparison()
                .ignoringFields("lastChange", "relevanceMatchCategories")
                .withComparatorForType(LocalDateTimeMatcher.approximatelyTimeComparator(1), LocalDateTime::class.java)
                .isEqualTo(dynamicAdGroup.adGroup.withStatusBsSynced(StatusBsSynced.YES))
            assertThat(actualPerformanceAdGroup).usingRecursiveComparison()
                .ignoringFields("lastChange", "relevanceMatchCategories")
                .withComparatorForType(LocalDateTimeMatcher.approximatelyTimeComparator(1), LocalDateTime::class.java)
                .isEqualTo((performanceAdGroup.adGroup.withStatusBsSynced(StatusBsSynced.NO) as AdGroupWithFeedId).withFeedId(feed1.id))
            assertThat(feedBriefs.keys).containsExactlyInAnyOrder(feed1.id, feed2.id)
            assertThat(feedBriefsAfterExecution.map { it.spec.campaignBrief.ecom.feedId }).containsOnly(feed1.id)
        }
    }

    @Test
    fun execute_withFeedsFromDifferentClients() {
        val anotherClientInfo = steps.clientSteps().createDefaultClient()
        val url = "https://url3.ru"

        val feed1 = defaultFeed(null)
            .withUrl(url)
            .withName("feed1")
            .withTargetDomain(stripDomainTail(stripProtocol(url)))
            .withSource(Source.SITE)
        val feedInfo1 = FeedInfo().withFeed(feed1).withClientInfo(clientInfo)
        val feedInDynamicAdGroup = steps.feedSteps().createFeed(feedInfo1)
        val dynamicAdGroup = steps.adGroupSteps().createActiveDynamicFeedAdGroup(feedInDynamicAdGroup)

        val feed2 = defaultFeed(null).withUpdateStatus(UpdateStatus.NEW)
            .withUrl(url)
            .withName("feed2")
            .withTargetDomain(stripDomainTail(stripProtocol(url)))
            .withSource(Source.SITE)
            .withUpdateStatus(UpdateStatus.NEW)
        val feedInfo2 = FeedInfo().withFeed(feed2).withClientInfo(anotherClientInfo)
        val feedInPerformanceAdGroup = steps.feedSteps().createFeed(feedInfo2)
        val performanceAdGroup = steps.adGroupSteps().createActivePerformanceAdGroup(anotherClientInfo, feedInPerformanceAdGroup.feedId)

        val duplicatesBeforeExecute = deleteDuplicatesSiteFeedsRepository.getDuplicateSiteFeedIds(shard)

        job.execute()

        val duplicatesAfterExecute = deleteDuplicatesSiteFeedsRepository.getDuplicateSiteFeedIds(shard)

        val actualDynamicAdGroup = adGroupService.getAdGroup(dynamicAdGroup.adGroupId)
        val actualPerformanceAdGroup = adGroupService.getAdGroup(performanceAdGroup.adGroupId) as AdGroupWithFeedId
        softly {
            assertThat(duplicatesBeforeExecute).doesNotContain(feed1.id, feed2.id)
            assertThat(duplicatesAfterExecute).doesNotContain(feed1.id, feed2.id)
            assertThat(actualDynamicAdGroup).usingRecursiveComparison()
                .ignoringFields("lastChange", "relevanceMatchCategories")
                .withComparatorForType(LocalDateTimeMatcher.approximatelyTimeComparator(1), LocalDateTime::class.java)
                .isEqualTo(dynamicAdGroup.adGroup.withStatusBsSynced(StatusBsSynced.YES))
            assertThat(actualPerformanceAdGroup).usingRecursiveComparison()
                .ignoringFields("lastChange", "relevanceMatchCategories")
                .withComparatorForType(LocalDateTimeMatcher.approximatelyTimeComparator(1), LocalDateTime::class.java)
                .isEqualTo((performanceAdGroup.adGroup.withStatusBsSynced(StatusBsSynced.YES)))
        }
    }
}
