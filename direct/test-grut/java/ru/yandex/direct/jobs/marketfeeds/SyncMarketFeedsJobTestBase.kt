package ru.yandex.direct.jobs.marketfeeds

import com.google.common.base.Preconditions.checkState
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository
import ru.yandex.direct.core.entity.banner.service.BannerSuspendResumeService
import ru.yandex.direct.core.entity.client.repository.ClientRepository
import ru.yandex.direct.core.entity.feature.service.FeatureService
import ru.yandex.direct.core.entity.feed.model.BusinessType
import ru.yandex.direct.core.entity.feed.model.Feed
import ru.yandex.direct.core.entity.feed.model.FeedRow
import ru.yandex.direct.core.entity.feed.model.FeedType
import ru.yandex.direct.core.entity.feed.model.MasterSystem.MARKET
import ru.yandex.direct.core.entity.feed.model.UpdateStatus
import ru.yandex.direct.core.entity.feed.model.UpdateStatus.DONE
import ru.yandex.direct.core.entity.feed.model.UpdateStatus.ERROR
import ru.yandex.direct.core.entity.feed.repository.DataCampFeedYtRepository
import ru.yandex.direct.core.entity.feed.repository.FeedRepository
import ru.yandex.direct.core.entity.feed.repository.MarketFeedsConfig
import ru.yandex.direct.core.entity.feed.service.FeedService
import ru.yandex.direct.core.entity.ppcproperty.model.PpcPropertyEnum.SYNC_MARKET_FEED_JOB_ENABLED
import ru.yandex.direct.core.testing.data.TestFeeds.defaultFeed
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.FeedInfo
import ru.yandex.direct.core.testing.steps.ClientSteps
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.sharding.ShardSupport
import ru.yandex.direct.feature.FeatureName.MARKET_FEEDS_ALLOWED
import ru.yandex.direct.feature.FeatureName.SEND_FEEDS_TO_MBI_ALLOWED
import ru.yandex.direct.feature.FeatureName.SYNC_MARKET_FEEDS_BY_SHOP_ID
import ru.yandex.direct.jobs.marketfeeds.ytmodels.generated.YtFeeds
import ru.yandex.direct.scheduler.hourglass.TaskParametersMap
import ru.yandex.direct.scheduler.support.DirectShardedJob
import ru.yandex.direct.scheduler.support.PeriodicJobWrapper
import ru.yandex.direct.solomon.SolomonPushClient
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.ytwrapper.client.YtProvider
import ru.yandex.direct.ytwrapper.model.YqlRowMapper
import ru.yandex.direct.ytwrapper.model.YtCluster
import ru.yandex.direct.ytwrapper.model.YtOperator

private val YESTERDAY = LocalDate.now().minusDays(1).atTime(0, 0, 0, 0)
private const val SHARD: Int = ClientSteps.DEFAULT_SHARD
private const val DEF_URL = "http://my.domain/feed"
private const val DEF_SHOP_NAME = "shop"
private const val DEF_LOGIN = "my_username"
private const val DEF_PASS = "my_password"
private const val NO_LOGIN = "NO_LOGIN"
private const val NO_PASS = "NO_PASS"
private const val DELETED_FEED_NAME = "DELETED"
private const val FEED_NAME_PATTERN = "Фид из Яндекс.Маркета #%d"

abstract class SyncMarketFeedsJobTestBase {
    @Autowired
    lateinit var steps: Steps

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    @Autowired
    private lateinit var shardSupport: ShardSupport

    @Autowired
    private lateinit var featureService: FeatureService

    @Autowired
    private lateinit var feedRepository: FeedRepository

    @Autowired
    private lateinit var clientRepository: ClientRepository

    @Autowired
    private lateinit var feedService: FeedService

    @Autowired
    private lateinit var adGroupRepository: AdGroupRepository

    @Autowired
    private lateinit var bannerTypedRepository: BannerTypedRepository

    @Autowired
    private lateinit var bannerSuspendResumeService: BannerSuspendResumeService

    @Autowired
    private lateinit var marketFeedsConfig: MarketFeedsConfig

    @Autowired
    private lateinit var solomonPushClient: SolomonPushClient

    private lateinit var ytProvider: YtProvider
    private lateinit var ytOperator: YtOperator
    private lateinit var service: MarketFeedsUpdateService
    private lateinit var clientInfo: ClientInfo
    private lateinit var clientId: ClientId
    private lateinit var startSyncedFeedInfo: FeedInfo
    private lateinit var syncedFeed: Feed

    private var ytFeedRows = ArrayList<FeedRow>()
    private var uniqIdCounter = 8888L

    @BeforeEach
    fun before() {
        clientInfo = getClientInfo()
        clientId = clientInfo.clientId!!
        steps.featureSteps().addClientFeature(clientId, MARKET_FEEDS_ALLOWED, true)
        steps.featureSteps().addClientFeature(clientId, SEND_FEEDS_TO_MBI_ALLOWED, true)

        startSyncedFeedInfo = steps.feedSteps().createDefaultSyncedFeed(clientInfo)
        syncedFeed = startSyncedFeedInfo.feed

        ytFeedRows.clear()
        ytOperator = Mockito.mock(YtOperator::class.java)
        `when`(ytOperator.readTableModificationTime(any())).thenReturn(YESTERDAY.toString())
        `when`(
            ytOperator.readTableField(
                any(),
                ArgumentMatchers.eq(YtFeeds.CLIENT_ID)
            )
        ).thenAnswer { listOf(clientId.asLong()) }
        `when`(ytOperator.yqlQuery(any(), any<YqlRowMapper<FeedRow>>(), any(), any(), any()))
            .thenReturn(ytFeedRows)
        ytProvider = Mockito.mock(YtProvider::class.java)
        `when`(ytProvider.getOperator(any(YtCluster::class.java))).thenReturn(ytOperator)

        ppcPropertiesSupport[SYNC_MARKET_FEED_JOB_ENABLED.getName()] = true.toString()
        val lastSuccessTimeKey = String.format(LAST_TIME_PROPERTY_KEY_PATTERN, SHARD)
        ppcPropertiesSupport[lastSuccessTimeKey] = ""

        val dataCampFeedYtRepository = DataCampFeedYtRepository(
            ytProvider, marketFeedsConfig
        )

        service = MarketFeedsUpdateService(
            ytProvider,
            ppcPropertiesSupport, shardSupport, featureService, feedRepository,
            clientRepository, adGroupRepository, feedService, bannerTypedRepository, bannerSuspendResumeService,
            dataCampFeedYtRepository, marketFeedsConfig
        )
    }

    @AfterEach
    fun afterEach() {
        //Чтобы исключить влияние на другие тесты.
        Mockito.clearInvocations(ytOperator)
        Mockito.clearInvocations(ytProvider)
    }

    abstract fun getClientInfo(): ClientInfo

    @Test
    fun execute_addNewMarketFeedWithoutUserInfo() {
        val feedsRow = addFeedRow(login = NO_LOGIN, password = NO_PASS)

        executeJob()

        val actualFeed = getMarketFeed(feedsRow.feedId)!!
        val expectedUrl = createMarketUrl(feedsRow.businessId, feedsRow.shopId, feedsRow.feedId, DEF_URL)
        SoftAssertions.assertSoftly {
            it.assertThat(actualFeed.login).describedAs("login").isNull()
            it.assertThat(actualFeed.plainPassword).describedAs("password").isNull()
            it.assertThat(actualFeed.url).describedAs("url").isEqualTo(expectedUrl)
        }
    }

    @Test
    fun execute_addNewMarketFeed_whenUrlContainsUserInfo() {
        //Логин-пароль из урла игнорируем
        val urlWithUserInfo = "http://admin:root@ya.ru/feed"
        val feedRow = addFeedRow(url = urlWithUserInfo)

        executeJob()

        val actualFeed = getMarketFeed(feedRow.feedId)!!
        val expectedUrl = createMarketUrl(feedRow.businessId, feedRow.shopId, feedRow.feedId, urlWithUserInfo)
        SoftAssertions.assertSoftly {
            it.assertThat(actualFeed.login).describedAs("login").isEqualTo(DEF_LOGIN)
            it.assertThat(actualFeed.plainPassword).describedAs("password").isEqualTo(DEF_PASS)
            it.assertThat(actualFeed.url).describedAs("url").isEqualTo(expectedUrl)
        }
    }

    @Test
    fun execute_addNewMarketFeedCheckFields_success() {
        val feedRow = addFeedRow()

        executeJob()

        val actualFeed = getMarketFeed(feedRow.feedId)
        val expectedUrl = createMarketUrl(feedRow.businessId, feedRow.shopId, feedRow.feedId, DEF_URL)
        val expectedName = String.format(FEED_NAME_PATTERN, feedRow.feedId)
        SoftAssertions.assertSoftly {
            it.assertThat(actualFeed.businessType).describedAs("businessType").isEqualTo(BusinessType.RETAIL)
            it.assertThat(actualFeed.feedType).describedAs("feedType").isEqualTo(FeedType.YANDEX_MARKET)
            it.assertThat(actualFeed.name).describedAs("name").isEqualTo(expectedName)
            it.assertThat(actualFeed.url).describedAs("url").isEqualTo(expectedUrl)
            it.assertThat(actualFeed.login).describedAs("login").isEqualTo(DEF_LOGIN)
            it.assertThat(actualFeed.plainPassword).describedAs("password").isEqualTo(DEF_PASS)
            it.assertThat(actualFeed.masterSystem).describedAs("masterSystem").isEqualTo(MARKET)
            it.assertThat(actualFeed.refreshInterval).describedAs("refreshInterval").isGreaterThan(0L)
            it.assertThat(actualFeed.updateStatus).describedAs("updateStatus").isEqualTo(UpdateStatus.NEW)
            it.assertThat(actualFeed.marketBusinessId)
                .describedAs("marketBusinessId").isEqualTo(feedRow.businessId)
            it.assertThat(actualFeed.marketShopId).describedAs("marketShopId").isEqualTo(feedRow.shopId)
            it.assertThat(actualFeed.marketFeedId).describedAs("marketFeedId").isEqualTo(feedRow.feedId)
        }
    }

    @Test
    fun execute_addNewMarketFeeds_withSameShopId() {
        val businessId = getNewBusinessId(clientId)
        val shopId = getNewMarketShopId(clientId)
        repeat(4) {
            addFeedRow(businessId = businessId, shopId = shopId)
        }

        executeJob()

        val actualFeeds = getMarketFeedsByShopId(shopId)
        SoftAssertions.assertSoftly {
            it.assertThat(actualFeeds).describedAs("feedsCount").hasSize(4)
            actualFeeds.forEach { feed ->
                val expectedUrl = createMarketUrl(businessId, shopId, feed.marketFeedId, DEF_URL)
                it.assertThat(feed.url).describedAs("url").isEqualTo(expectedUrl)
            }
        }
    }

    @Test
    fun execute_addNewMarketFeeds_withSameShopIdAndSyncByShopId() {
        steps.featureSteps().addClientFeature(clientId, SYNC_MARKET_FEEDS_BY_SHOP_ID, true)
        val businessId = getNewBusinessId(clientId)
        val shopId = getNewMarketShopId(clientId)
        repeat(4) {
            addFeedRow(businessId = businessId, shopId = shopId)
        }

        executeJob()

        val actualFeeds = getMarketFeedsByShopId(shopId)
        SoftAssertions.assertSoftly {
            it.assertThat(actualFeeds).describedAs("feedsCount").hasSize(1)
            val actualFeed = actualFeeds.first()

            val expectedUrl = createMarketUrl(businessId, shopId, actualFeed.marketFeedId, DEF_URL, true)
            it.assertThat(actualFeed.url).describedAs("url").isEqualTo(expectedUrl)
            it.assertThat(actualFeed.marketFeedId).describedAs("marketFeedId").isEqualTo(shopId)
        }
    }

    @Test
    fun execute_addNewMarketFeeds_withSyncByShopId() {
        steps.featureSteps().addClientFeature(clientId, SYNC_MARKET_FEEDS_BY_SHOP_ID, true)
        val businessId = getNewBusinessId(clientId)
        val shopId1 = getNewMarketShopId(clientId)
        val shopId2 = getNewMarketShopId(clientId)
        addFeedRow(businessId = businessId, shopId = shopId1)
        addFeedRow(businessId = businessId, shopId = shopId1)
        addFeedRow(businessId = businessId, shopId = shopId2)

        executeJob()

        val actualFeeds1 = getMarketFeedsByShopId(shopId1)
        val actualFeeds2 = getMarketFeedsByShopId(shopId2)
        SoftAssertions.assertSoftly {
            it.assertThat(actualFeeds1).describedAs("feedsCount").hasSize(1)
            val actualFeed1 = actualFeeds1.first()
            val expectedUrl1 = createMarketUrl(businessId, shopId1, actualFeed1.marketFeedId, DEF_URL, true)
            it.assertThat(actualFeed1.url).describedAs("url").isEqualTo(expectedUrl1)
            it.assertThat(actualFeed1.marketFeedId).describedAs("marketFeedId").isEqualTo(shopId1)

            it.assertThat(actualFeeds2).describedAs("feedsCount").hasSize(1)
            val actualFeed2 = actualFeeds2.first()
            val expectedUrl2 = createMarketUrl(businessId, shopId2, actualFeed2.marketFeedId, DEF_URL, true)
            it.assertThat(actualFeed2.url).describedAs("url").isEqualTo(expectedUrl2)
            it.assertThat(actualFeed2.marketFeedId).describedAs("marketFeedId").isEqualTo(shopId2)
        }
    }

    @Test
    fun execute_addDuplicateOfSyncedToMbiFeed_failure() {
        addFeedRow(syncedFeed.marketBusinessId, syncedFeed.marketShopId, syncedFeed.marketFeedId, syncedFeed.url)

        executeJob()

        val feedsCount = feedRepository.getAllDataCampFeedsSimple(SHARD)
            .filter { it.clientId == clientId.asLong() }
            .count()
        val actualFeed = getActualFeed(syncedFeed.id)
        SoftAssertions.assertSoftly {
            it.assertThat(feedsCount).describedAs("feedsCount").isEqualTo(1)
            it.assertThat(actualFeed).`as`("actualFeed")
                .`is`(matchedBy(beanDiffer(syncedFeed).useCompareStrategy(onlyExpectedFields())))
        }
    }

    @Test
    fun execute_updateMarketFeed_success() {
        val marketFeed = createMarketFeed(clientInfo).feed

        val newUrl = "https://new.feed.url/"
        val newLogin = "new_login"
        val newPass = "new_password"
        val businessId = getNewBusinessId(clientId)
        val shopId = getNewMarketShopId(clientId)
        addFeedRow(businessId, shopId, marketFeed.marketFeedId, newUrl, newLogin, newPass)

        executeJob()

        val actualFeed = getActualFeed(marketFeed.id)!!
        val expectedUrl =
            createMarketUrl(businessId, shopId, marketFeed.marketFeedId, newUrl)
        SoftAssertions.assertSoftly {
            it.assertThat(actualFeed.login).describedAs("login").isEqualTo(newLogin)
            it.assertThat(actualFeed.plainPassword).describedAs("password").isEqualTo(newPass)
            it.assertThat(actualFeed.url).describedAs("url").isEqualTo(expectedUrl)
            it.assertThat(actualFeed.marketBusinessId).describedAs("businessId").isEqualTo(businessId)
            it.assertThat(actualFeed.marketShopId).describedAs("shopId").isEqualTo(shopId)
        }
    }

    @Test
    fun execute_addAndUpdateMarketFeeds_withSyncByShopId() {
        steps.featureSteps().addClientFeature(clientId, SYNC_MARKET_FEEDS_BY_SHOP_ID, true)

        val marketFeed1 = createMarketFeed(clientInfo, true).feed
        val marketFeed2 = createMarketFeed(clientInfo, true).feed

        val newUrl = "https://new.feed.url/"
        val newLogin = "new_login"
        val newPass = "new_password"
        repeat(4) {
            addFeedRow(
                marketFeed1.marketBusinessId,
                marketFeed1.marketShopId,
                getNewMarketFeedId(clientId),
                newUrl,
                newLogin,
                newPass
            )
        }

        val addedShopId = getNewMarketShopId(clientId)
        val addedBusinessId = getNewBusinessId(clientId)
        repeat(4) {
            addFeedRow(businessId = addedBusinessId, shopId = addedShopId)
        }

        executeJob()

        val actualFeeds1 = getMarketFeedsByShopId(marketFeed1.marketShopId) // updated feed
        val actualFeeds2 = getMarketFeedsByShopId(marketFeed2.marketShopId) // deleted feed
        val actualFeeds3 = getMarketFeedsByShopId(addedShopId) // added feed
        SoftAssertions.assertSoftly {
            it.assertThat(actualFeeds1).describedAs("feedsCount").hasSize(1)
            val actualFeed1 = actualFeeds1.first()
            val expectedUrl1 = createMarketUrl(
                marketFeed1.marketBusinessId,
                marketFeed1.marketShopId,
                marketFeed1.marketFeedId,
                newUrl,
                true
            )
            it.assertThat(actualFeed1.id).describedAs("feedId").isEqualTo(marketFeed1.id)
            it.assertThat(actualFeed1.login).describedAs("login").isEqualTo(newLogin)
            it.assertThat(actualFeed1.plainPassword).describedAs("password").isEqualTo(newPass)
            it.assertThat(actualFeed1.url).describedAs("url").isEqualTo(expectedUrl1)

            it.assertThat(actualFeeds2).describedAs("feedsCount").hasSize(0)

            it.assertThat(actualFeeds3).describedAs("feedsCount").hasSize(1)
            val actualFeed3 = actualFeeds3.first()
            val expectedUrl3 = createMarketUrl(addedBusinessId, addedShopId, actualFeed3.marketFeedId, DEF_URL, true)
            it.assertThat(actualFeed3.url).describedAs("url").isEqualTo(expectedUrl3)
        }
    }

    @Test
    fun execute_updateSyncedToMbiFeed_failure() {
        addFeedRow(
            syncedFeed.marketBusinessId,
            syncedFeed.marketShopId,
            syncedFeed.marketFeedId,
            "https://some.other.url/"
        )

        executeJob()

        val actualFeed = getActualFeed(syncedFeed.id)
        assertThat(actualFeed).`as`("actualFeed")
            .`is`(matchedBy(beanDiffer(syncedFeed).useCompareStrategy(onlyExpectedFields())))
    }

    @Test
    fun execute_deleteUnusedMarketFeed() {
        val marketFeedInfo = createMarketFeed(clientInfo)

        executeJob()

        val actualFeed = feedRepository.get(SHARD, clientId, listOf(marketFeedInfo.feedId)).firstOrNull()
        assertThat(actualFeed).isNull()
    }

    @Test
    fun execute_deleteUsedMarketFeed() {
        val marketFeedInfo = createMarketFeed(clientInfo)
        val adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(marketFeedInfo)
        val bannerId = steps.bannerCreativeSteps().createPerformanceBannerCreative(adGroupInfo).bannerId
        val bannerClass = BannerWithSystemFields::class.java
        val startBanner = bannerTypedRepository.getStrictlyFullyFilled(SHARD, listOf(bannerId), bannerClass).first()
        checkState(startBanner.statusShow)

        executeJob()

        val actualFeed = feedRepository.get(SHARD, clientId, listOf(marketFeedInfo.feedId)).first()
        val actualBanner = bannerTypedRepository.getStrictlyFullyFilled(SHARD, listOf(bannerId), bannerClass).first()
        SoftAssertions.assertSoftly {
            it.assertThat(actualFeed.name).describedAs("name").isEqualTo(DELETED_FEED_NAME)
            it.assertThat(actualFeed.updateStatus).describedAs("updateStatus").isEqualTo(ERROR)
            it.assertThat(actualFeed.refreshInterval).describedAs("refreshInterval").isEqualTo(0L)
            it.assertThat(actualBanner.statusShow).describedAs("statusShow").isFalse
        }
    }

    @Test
    fun execute_restoreMarkedAsDeletedFeed() {
        val marketFeedInfo = createMarketFeed(clientInfo)
        steps.feedSteps().setFeedProperty(marketFeedInfo, Feed.NAME, DELETED_FEED_NAME)
        steps.feedSteps().setFeedProperty(marketFeedInfo, Feed.UPDATE_STATUS, ERROR)
        steps.feedSteps().setFeedProperty(marketFeedInfo, Feed.REFRESH_INTERVAL, 0L)

        val businessId = marketFeedInfo.feed.marketBusinessId
        val shopId = marketFeedInfo.feed.marketShopId
        val marketFeedId = marketFeedInfo.feed.marketFeedId
        addFeedRow(businessId, shopId, marketFeedId, DEF_URL)

        executeJob()

        val actualFeed = feedRepository.get(SHARD, clientId, listOf(marketFeedInfo.feedId)).first()
        val expectedName = String.format(FEED_NAME_PATTERN, marketFeedId)
        val expectedUrl = createMarketUrl(businessId, shopId, marketFeedId, DEF_URL)
        SoftAssertions.assertSoftly {
            it.assertThat(actualFeed.name).describedAs("name").isEqualTo(expectedName)
            it.assertThat(actualFeed.url).describedAs("url").isEqualTo(expectedUrl)
            it.assertThat(actualFeed.updateStatus).describedAs("updateStatus").isEqualTo(UpdateStatus.NEW)
            it.assertThat(actualFeed.refreshInterval).describedAs("refreshInterval").isEqualTo(86_400L)
        }
    }

    @Test
    fun execute_deleteSyncedToMbiFeed_failure() {
        executeJob()

        val actualFeed = feedRepository.get(SHARD, clientId, listOf(syncedFeed.id)).firstOrNull()
        assertThat(actualFeed).`as`("actualFeed")
            .`is`(matchedBy(beanDiffer(syncedFeed).useCompareStrategy(onlyExpectedFields())))
    }

    // обеспечиваем уникальность marketFeedId для каждого теста и клиента
    private fun getNewMarketFeedId(clientId: ClientId) = clientId.asLong() * 10_000L + uniqIdCounter++

    private fun getNewMarketShopId(clientId: ClientId) = clientId.asLong() * 50_000L + uniqIdCounter++

    private fun getNewBusinessId(clientId: ClientId) = clientId.asLong() * 100_000L + uniqIdCounter++

    private fun getMarketFeed(marketFeedId: Long) =
        feedRepository.getAllDataCampFeedsSimple(SHARD)
            .first { it.marketFeedId == marketFeedId }

    private fun getMarketFeedsByShopId(marketShopId: Long) =
        feedRepository.getAllDataCampFeedsSimple(SHARD)
            .filter { it.marketShopId == marketShopId }

    private fun getActualFeed(feedId: Long) =
        feedRepository.get(SHARD, clientId, setOf(feedId))
            .firstOrNull()

    private fun addFeedRow(
        businessId: Long? = null,
        shopId: Long? = null,
        marketFeedId: Long? = null,
        url: String? = null,
        login: String? = null,
        password: String? = null,
        shopName: String? = null
    ): FeedRow {
        val clientId = clientId
        val feedRow = FeedRow(
            clientId = clientId.asLong(),
            feedId = marketFeedId ?: getNewMarketFeedId(clientId),
            businessId = businessId ?: getNewBusinessId(clientId),
            shopId = shopId ?: getNewMarketShopId(clientId),
            url = url ?: DEF_URL,
            login = when (login) {
                null -> {
                    DEF_LOGIN
                }
                NO_LOGIN -> {
                    null
                }
                else -> {
                    login
                }
            },
            password = when (password) {
                null -> {
                    DEF_PASS
                }
                NO_PASS -> {
                    null
                }
                else -> {
                    password
                }
            },
            shopName = shopName ?: DEF_SHOP_NAME
        )
        ytFeedRows.add(feedRow)
        return feedRow
    }

    private fun executeJob() {
        val job = SyncMarketFeedsJob(ppcPropertiesSupport, service, solomonPushClient)
        val shardContext = TaskParametersMap.of(DirectShardedJob.SHARD_PARAM, SHARD.toString())
        PeriodicJobWrapper(job).execute(shardContext)
    }

    private fun createMarketUrl(
        businessId: Long,
        shopId: Long,
        marketFeedId: Long,
        url: String,
        isForceDC: Boolean = false
    ) =
        "https://market.feed/?url=${URLEncoder.encode(url, StandardCharsets.UTF_8)}" +
            "&business_id=$businessId" +
            "&shop_id=$shopId" +
            (if (!isForceDC) "&market_feed_id=$marketFeedId" else "") +
            "&market=true" +
            (if (isForceDC) "&force_dc=true" else "")

    private fun createMarketFeed(clientInfo: ClientInfo, syncByShopId: Boolean = false): FeedInfo {
        val businessId = getNewBusinessId(clientInfo.clientId!!)
        val shopId = getNewMarketShopId(clientInfo.clientId!!)
        val marketFeedId = if (!syncByShopId) {
            getNewMarketFeedId(clientInfo.clientId!!)
        } else shopId
        val marketFeedInfo = FeedInfo().apply {
            this.clientInfo = clientInfo
            feed = defaultFeed(clientInfo.clientId).apply {
                url = createMarketUrl(businessId, shopId, marketFeedId, "https://other.domain/feed", syncByShopId)
                login = "other_login"
                plainPassword = "other_password"
                marketBusinessId = businessId
                marketShopId = shopId
                this.marketFeedId = marketFeedId
                masterSystem = MARKET
                updateStatus = DONE
                refreshInterval = 86_400L
            }
        }
        val feedId = steps.feedSteps().createFeed(marketFeedInfo).feedId
        marketFeedInfo.feed = feedRepository.get(clientInfo.shard, clientInfo.clientId!!, listOf(feedId))[0]
        return marketFeedInfo
    }
}
