package ru.yandex.direct.logicprocessor.processors.mysql2grut.steps

import java.math.BigDecimal
import java.time.LocalDate
import org.apache.commons.lang.RandomStringUtils
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.entity.StatusBsSynced
import ru.yandex.direct.core.entity.adgroup.model.AdGroupBsTags
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroup
import ru.yandex.direct.core.entity.adgroup.model.PageGroupTagEnum
import ru.yandex.direct.core.entity.adgroup.model.TargetTagEnum
import ru.yandex.direct.core.entity.adgroup.repository.internal.AdGroupBsTagsRepository
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargeting
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AuditoriumGeoSegmentsAdGroupAdditionalTargeting
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.ContentCategoriesAdGroupAdditionalTargeting
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.ShowDatesAdGroupAdditionalTargeting
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.repository.AdGroupAdditionalTargetingRepository
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.BrowserEngine
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.BrowserEnginesAdGroupAdditionalTargeting
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign
import ru.yandex.direct.core.entity.client.repository.ClientRepository
import ru.yandex.direct.core.entity.crypta.repository.CryptaSegmentRepository
import ru.yandex.direct.core.entity.mobilecontent.repository.MobileContentRepository
import ru.yandex.direct.core.entity.offerretargeting.model.OfferRetargeting
import ru.yandex.direct.core.entity.performancefilter.model.NowOptimizingBy
import ru.yandex.direct.core.entity.performancefilter.model.Operator
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterCondition
import ru.yandex.direct.core.entity.performancefilter.model.TargetFunnel
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatchCategory
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.generateRandomIdLong
import ru.yandex.direct.core.grut.api.AdGroupGrut
import ru.yandex.direct.core.grut.api.BiddableShowConditionType
import ru.yandex.direct.core.grut.api.CampaignGrutModel
import ru.yandex.direct.core.grut.api.ClientGrutModel
import ru.yandex.direct.core.grut.api.InternalAdGroupOptions
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.mysql2grut.repository.MinusPhrase
import ru.yandex.direct.core.testing.data.TestGroups
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.KeywordInfo
import ru.yandex.direct.core.testing.info.MobileContentInfo
import ru.yandex.direct.core.testing.info.PerformanceFilterInfo
import ru.yandex.direct.core.testing.info.RetargetingInfo
import ru.yandex.direct.core.testing.steps.CryptaGoalsSteps
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbschema.ppc.Tables
import ru.yandex.direct.dbschema.ppc.enums.AdgroupAdditionalTargetingsTargetingType
import ru.yandex.direct.dbschema.ppc.tables.records.BidsDynamicRecord
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.logicprocessor.processors.bsexport.utils.SupportedCampaignsService
import ru.yandex.direct.logicprocessor.processors.mysql2grut.steps.repository.BidModifierTestRepository
import ru.yandex.direct.logicprocessor.processors.mysql2grut.steps.repository.BiddableShowConditionsTestRepository
import ru.yandex.direct.logicprocessor.processors.mysql2grut.steps.repository.MinusPhraseTestRepository

class GrutReplicationSteps {
    @Autowired
    private lateinit var replicationService: GrutApiService

    @Autowired
    lateinit var steps: Steps

    @Autowired
    private lateinit var clientRepository: ClientRepository

    @Autowired
    private lateinit var campaignTypedRepository: SupportedCampaignsService

    @Autowired
    private lateinit var adGroupBsTagsRepository: AdGroupBsTagsRepository

    @Autowired
    lateinit var minusPhraseTestRepository: MinusPhraseTestRepository

    @Autowired
    lateinit var mobileContentRepository: MobileContentRepository

    @Autowired
    private lateinit var adGroupAdditionalTargetingRepository: AdGroupAdditionalTargetingRepository

    @Autowired
    private lateinit var biddableShowConditionTestRepository: BiddableShowConditionsTestRepository

    @Autowired
    private lateinit var cryptaSegmentRepository: CryptaSegmentRepository

    @Autowired
    lateinit var bidModifierTestRepository: BidModifierTestRepository

    @Autowired
    lateinit var dslContextProvider: DslContextProvider

    private val clientsToCleanup: MutableSet<Long> = mutableSetOf()

    val DEFAULT_SHARD = 1

    /**
     * Создает группу с кампанией и клиентом в MySql плюс иерархию клиент-кампания в Grut
     */
    fun createAdGroupInMySqlWithGrutHierarchy(
        pageGroupTags: List<PageGroupTagEnum> = emptyList(),
        targetTags: List<TargetTagEnum> = emptyList(),
    ): OrderAdGroupInfo {
        val campaign = steps.campaignSteps().createDefaultCampaignByCampaignType(CampaignType.TEXT)
        val shard = campaign.shard
        val adGroupFromMySql = steps.adGroupSteps().createDefaultAdGroup(campaign)
        //internal ad groups
        if (targetTags.isNotEmpty() || pageGroupTags.isNotEmpty()) {
            val obj = AdGroupBsTags()
            if (targetTags.isNotEmpty()) {
                obj.withTargetTags(targetTags)
            }
            if (pageGroupTags.isNotEmpty()) {
                obj.withPageGroupTags(pageGroupTags)
            }
            adGroupBsTagsRepository.addAdGroupPageTags(shard, mapOf(adGroupFromMySql.adGroupId to obj))
        }

        val orderId = createAdGroupCampaignInGrut(adGroupFromMySql)
        return OrderAdGroupInfo(adGroupFromMySql, orderId)
    }

    fun linkMinusPhrasesToAdGroupInDirect(
        adGroupInfo: AdGroupInfo,
        libraryMinusPhrasesIds: List<Long> = emptyList(),
        nonLibraryMinusPhraseId: Long? = null
    ) {
        if (nonLibraryMinusPhraseId != null) {
            minusPhraseTestRepository.linkNonLibraryMinusPhrase(
                adGroupInfo.shard,
                nonLibraryMinusPhraseId,
                adGroupInfo.adGroupId
            )
        }
        if (libraryMinusPhrasesIds.isNotEmpty()) {
            minusPhraseTestRepository.linkLibraryMinusPhrasesToAdGroup(
                adGroupInfo.shard,
                libraryMinusPhrasesIds,
                adGroupInfo.adGroupId
            )
        }
    }

    fun createCryptaCategories() {
        steps.cryptaGoalsSteps().addCryptaContentCategoriesGoals()
    }

    fun createAuditoriumGeosegmentsTargeting(adGroup: AdGroupInfo): AuditoriumGeoSegmentsAdGroupAdditionalTargeting {
        val targeting = AuditoriumGeoSegmentsAdGroupAdditionalTargeting()
            .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
            .withValue(setOf(2019646844))
            .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
        steps.adGroupAdditionalTargetingSteps().addValidTargetingsToAdGroup(adGroup, listOf(targeting))
        return createAdditionalTargeting(targeting, AdgroupAdditionalTargetingsTargetingType.auditorium_geosegments, adGroup)
            as AuditoriumGeoSegmentsAdGroupAdditionalTargeting
    }

    fun createBrowserEnginesTargeting(adGroup: AdGroupInfo): BrowserEnginesAdGroupAdditionalTargeting {
        val targeting = BrowserEnginesAdGroupAdditionalTargeting()
            .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
            .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
            .withAdGroupId(adGroup.adGroupId)
            .withValue(
                listOf(BrowserEngine()
                    .withMinVersion("1.1")
                    .withMaxVersion("2.2")
                    .withTargetingValueEntryId(123L)
                )
            )
        return createAdditionalTargeting(targeting, AdgroupAdditionalTargetingsTargetingType.browser_engines, adGroup)
            as BrowserEnginesAdGroupAdditionalTargeting
    }


    fun createContentCategoriesAdditionalTargeting(adGroup: AdGroupInfo): ContentCategoriesAdGroupAdditionalTargeting {

        val targeting = ContentCategoriesAdGroupAdditionalTargeting()
            .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
            .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
            .withAdGroupId(adGroup.adGroupId)
            .withValue(setOf(
                CryptaGoalsSteps.CONTENT_GENRE_GOAL_ID,
                CryptaGoalsSteps.CONTENT_CATEGORY_GOAL_ID,
            ))
        return createAdditionalTargeting(targeting, AdgroupAdditionalTargetingsTargetingType.content_categories, adGroup)
            as ContentCategoriesAdGroupAdditionalTargeting
    }

    fun createAdditionalTargeting(
        targeting: AdGroupAdditionalTargeting,
        type: AdgroupAdditionalTargetingsTargetingType,
        adGroup: AdGroupInfo,
    ): AdGroupAdditionalTargeting {
        steps.adGroupAdditionalTargetingSteps().addValidTargetingsToAdGroup(adGroup, listOf(targeting))
        return adGroupAdditionalTargetingRepository.getByAdGroupIdsAndType(
            adGroup.shard,
            listOf(adGroup.adGroupId),
            type,
        ).first()
    }


    fun createShowDatesAdditoinalTargetings(adGroup: AdGroupInfo): ShowDatesAdGroupAdditionalTargeting {
        val targeting = ShowDatesAdGroupAdditionalTargeting()
            .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
            .withJoinType(AdGroupAdditionalTargetingJoinType.ALL)
            .withValue(setOf(LocalDate.of(2022, 3, 3)))

        return createAdditionalTargeting(targeting, AdgroupAdditionalTargetingsTargetingType.show_dates, adGroup)
            as ShowDatesAdGroupAdditionalTargeting
    }

    fun deleteTargetingFromMySql(shard: Int, id: Long) {
        steps.adGroupAdditionalTargetingSteps().deleteTargetingFromMySql(shard, id)
    }


    /**
     * Создает внутреннюю группу
     */
    fun createInternalAdGroupInMySqlWithGrutHierarchy(
        internalLevel: Long,
        rfOptions: InternalAdGroupOptions
    ): OrderAdGroupInfo {
        val campaign = steps.campaignSteps().createDefaultCampaignByCampaignType(CampaignType.INTERNAL_FREE)
        dslContextProvider.ppc(campaign.shard)
            .insertInto(Tables.CAMPAIGNS_INTERNAL, Tables.CAMPAIGNS_INTERNAL.CID)
            .values(campaign.campaignId)
            .execute()
        val adGroupFromMySql = steps.adGroupSteps().createDefaultInternalAdGroup(
            internalLevel,
            rfOptions.rf,
            rfOptions.rfReset,
            rfOptions.startTime,
            rfOptions.finishTime,
            rfOptions.maxClicksCount,
            rfOptions.maxClicksPeriod,
            rfOptions.maxStopsCount,
            rfOptions.maxStopsPeriod,
            campaign,
        )
        val orderId = createAdGroupCampaignInGrut(adGroupFromMySql)
        return OrderAdGroupInfo(adGroupFromMySql, orderId)
    }

    fun createMobileContentAdGroup(mobileContentExistInGrut: Boolean = true): AdGroupInfo {
        val campaign = steps.campaignSteps().createDefaultCampaignByCampaignType(CampaignType.MOBILE_CONTENT)
        val adGroup = steps.adGroupSteps().createActiveMobileContentAdGroup(campaign)
        createAdGroupCampaignInGrut(adGroup)
        if (mobileContentExistInGrut) {
            val mobileContentId = (adGroup.adGroup as MobileContentAdGroup).mobileContentId
            val mobileContent = mobileContentRepository.getMobileContent(adGroup.shard, mobileContentId)
            replicationService.mobileContentGrutDao.createOrUpdateMobileContent(listOf(mobileContent))
        }
        return adGroup
    }

    fun createCpmAdGroup(): AdGroupInfo {
        val campaign = steps.campaignSteps().createDefaultCampaignByCampaignType(CampaignType.CPM_DEALS)
        val adGroup = steps.adGroupSteps().createActiveCpmIndoorAdGroup(campaign)
        createAdGroupCampaignInGrut(adGroup)
        return adGroup
    }

    /**
     * Создает динамическую группу
     */
    fun createDynamicAdGroupInMySqlWithGrutHierarchy(categories: Set<RelevanceMatchCategory>): Pair<AdGroupInfo, Long> {
        val clientInfo = steps.clientSteps().createDefaultClient()
        val campaign = steps.campaignSteps().createDefaultCampaignByCampaignType(CampaignType.DYNAMIC)
        val dynamicAdGroupInfo = steps.adGroupSteps().createDynamicTextAdGroup(
            clientInfo,
            campaign,
            TestGroups.activeDynamicTextAdGroup(null)
                .withRelevanceMatchCategories(categories)
        )
        clientsToCleanup.add(dynamicAdGroupInfo.clientId.asLong())
        val orderId = createAdGroupCampaignInGrut(dynamicAdGroupInfo)
        return Pair(dynamicAdGroupInfo, orderId)
    }

    fun createPerformanceAdGroupWithGrutHierarchy(): Pair<AdGroupInfo, Long> {
        val dynamicAdGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup()
        val orderId = createAdGroupWithGrutHierarchy(dynamicAdGroupInfo)
        return Pair(dynamicAdGroupInfo, orderId)
    }

    fun createAdGroupWithGrutHierarchy(adGroupInfo: AdGroupInfo): Long {
        clientsToCleanup.add(adGroupInfo.clientId.asLong())
        return createAdGroupCampaignInGrut(adGroupInfo)
    }

    /**
     * Создает ключевое слово в MySql и кампанию-клиента для него в Грут
     */
    public fun createKeywordInMySqlWithGrutClient(): AdGroupChild<KeywordInfo> {
        //создаем keyword в mysql
        val keyword = steps.keywordSteps().createDefaultKeyword()
        clientsToCleanup.add(keyword.adGroupInfo.clientId.asLong())
        return AdGroupChild(keyword, keyword.adGroupInfo)
    }

    /**
     * Создает relevance_match bid в MySql и кампанию-клиента для него в Грут
     */
    fun createRelevanceMatch(): AdGroupChild<RelevanceMatch> {
        val adGroupInfo = steps.adGroupSteps().createDefaultAdGroup()
        val relevanceMatch = steps.relevanceMatchSteps().addDefaultRelevanceMatch(adGroupInfo)
        //создаем клиента в грут
        clientsToCleanup.add(adGroupInfo.clientId.asLong())
        return AdGroupChild(relevanceMatch, adGroupInfo)
    }

    fun createOfferRetargeting(): AdGroupChild<OfferRetargeting> {
        val adGroupInfo = steps.adGroupSteps().createDefaultAdGroup()
        clientsToCleanup.add(adGroupInfo.clientId.asLong())

        val offerRetargeting = OfferRetargeting()
            .withAdGroupId(adGroupInfo.adGroupId)
            .withCampaignId(adGroupInfo.campaignId)
            .withIsSuspended(false)
            .withStatusBsSynced(StatusBsSynced.NO)
        steps.offerRetargetingSteps().addOfferRetargetingToAdGroup(offerRetargeting, adGroupInfo)
        return AdGroupChild(offerRetargeting, adGroupInfo)
    }

    fun createDynamic(): AdGroupChild<BidsDynamicRecord> {
        val adGroupInfo = steps.adGroupSteps().createDefaultAdGroup()
        clientsToCleanup.add(adGroupInfo.clientId.asLong())

        val dynamic = steps.dynamicConditionsSteps().addDefaultBidsDynamic(adGroupInfo)
        return AdGroupChild(dynamic!!, adGroupInfo)
    }

    fun createRetargeting(): AdGroupChild<RetargetingInfo> {
        val retargeting = steps.retargetingSteps().createDefaultRetargeting()
        clientsToCleanup.add(retargeting.clientId.asLong())

        return AdGroupChild(retargeting, retargeting.adGroupInfo)
    }

    fun createPerformance(): AdGroupChild<PerformanceFilterInfo> {
        val campaignInfo = steps.campaignSteps().createDefaultCampaign()
        val condition = PerformanceFilterCondition<List<Double>>("old_price", Operator.GREATER, "[]").withParsedValue(listOf(1.0))

        val performance = steps.performanceFilterSteps().createPerformanceFilter(campaignInfo, PerformanceFilter()
            .withPriceCpa(BigDecimal.TEN)
            .withPriceCpc(BigDecimal.ONE)
            .withTargetFunnel(TargetFunnel.NEW_AUDITORY)
            .withName("test")
            .withStatusBsSynced(StatusBsSynced.YES)
            .withIsSuspended(false)
            .withIsDeleted(false)
            .withNowOptimizingBy(NowOptimizingBy.CPA)
            .withConditions(listOf(condition)))

        return AdGroupChild(performance, performance.adGroupInfo)
    }

    data class AdGroupChild<T>(val obj: T, val adGroupInfo: AdGroupInfo)


    /**
     * Создает клиента и кампанию в груте, соотвуетствующих клиенту и кампании группы из директа
     */
    private fun createAdGroupCampaignInGrut(adGroupFromMySql: AdGroupInfo): Long {
        val clientFromMySql = clientRepository.get(adGroupFromMySql.shard, listOf(adGroupFromMySql.clientId)).first()
        clientsToCleanup.add(adGroupFromMySql.clientId.asLong())
        replicationService.clientGrutDao.createOrUpdateClients(listOf(ClientGrutModel(clientFromMySql, listOf())))

        val campaignFromMySql =
            campaignTypedRepository.getTyped(adGroupFromMySql.shard, listOf(adGroupFromMySql.campaignId))
                .first() as CommonCampaign

        replicationService.campaignGrutDao.createOrUpdateCampaign(CampaignGrutModel(campaignFromMySql, orderType = 1))

        return replicationService.campaignGrutDao.getCampaignByDirectId(campaignFromMySql.id)!!.meta.id
    }

    /**
     * Создает группу в груте для кампании, которая уже есть в груте, возвращает идентификатор созданной группы
     */
    fun createAdGroupInGrut(orderId: Long): Long {
        val id = generateRandomIdLong()
        val segmentsMap = cryptaSegmentRepository.getContentSegments()
        val model = AdGroupGrut(id, orderId, "<someName>", AdGroupType.BASE, cryptaSegmentsMapping = segmentsMap)
        replicationService.adGroupGrutDao.createOrUpdateAdGroup(model)
        return id
    }

    fun createAdGroupInMySql(): AdGroupInfo {
        val campaign = steps.campaignSteps().createDefaultCampaignByCampaignType(CampaignType.TEXT)
        return steps.adGroupSteps().createDefaultAdGroup(campaign)
    }

    /**
     * Удаляет всех клиентов и объекты ниже по иерархии, которые были созданы при вызовe GrutSteps
     * используется в tear-down методах тестов
     */
    fun cleanupClientsWithChildren(additionalClients: Collection<Long> = emptyList()) {
        val clientIds = clientsToCleanup.plus(additionalClients)
        if (clientIds.isNotEmpty()) {
            replicationService.clientGrutDao.deleteObjects(clientIds, true)
        }
    }

    fun createMinusPhraseInDirectWithGrutHierarchy(
        name: String?,
        phrases: List<String>,
        isLibrary: Boolean,
        client: ClientInfo? = null
    ): MinusPhrase {
        val mwClient = when (client) {
            null -> steps.clientSteps().createDefaultClient()
            else -> client
        }
        clientsToCleanup.add(mwClient.clientId!!.asLong())
        replicationService.clientGrutDao.createOrUpdateClients(listOf(ClientGrutModel(mwClient.client!!, listOf())))


        val mwId = minusPhraseTestRepository.createMinusPhrase(
            mwClient.shard,
            mwClient.clientId!!.asLong(),
            name,
            phrases,
            isLibrary,
        )
        val minusPhrase = minusPhraseTestRepository.getMinusPhrases(mwClient.shard, listOf(mwId)).first()
        return minusPhrase

    }

    fun createRandomMinusPhraseInGrut(): MinusPhrase {
        val clientInfo = steps.clientSteps().createDefaultClient()
        clientsToCleanup.add(clientInfo.clientId!!.asLong())
        replicationService.clientGrutDao.createOrUpdateClients(listOf(ClientGrutModel(clientInfo.client!!, listOf())))

        val id = minusPhraseTestRepository.generateMinusPhraseId()
        val minusPhrase =
            MinusPhrase(id, clientInfo.clientId!!.asLong(), null, listOf(RandomStringUtils.randomAlphabetic(5)), false)
        replicationService.minusPhrasesGrutDao.createOrUpdateMinusPhrases(listOf(minusPhrase))
        return minusPhrase
    }

    fun createMobileContentInMySql(): MobileContentInfo {
        val clientInfo = steps.clientSteps().createDefaultClient()
        clientsToCleanup.add(clientInfo.clientId!!.asLong())

        replicationService.clientGrutDao.createOrUpdateClients(listOf(ClientGrutModel(clientInfo.client!!, listOf())))

        return steps.mobileContentSteps().createDefaultMobileContent(clientInfo)
    }

    fun createMobileContentInGrut(): Long {
        val mobileContentInMySql = createMobileContentInMySql()
        replicationService.mobileContentGrutDao.createOrUpdateMobileContent(listOf(mobileContentInMySql.mobileContent))
        mobileContentRepository.deleteMobileContentById(
            mobileContentInMySql.shard,
            listOf(mobileContentInMySql.mobileContentId)
        )
        return mobileContentInMySql.mobileContentId
    }

    fun createRandomMinusPhraseInDirectWithGrutHierarchy(
        client: ClientInfo? = null,
        isLibrary: Boolean = false
    ): MinusPhrase {
        val name = when (isLibrary) {
            true -> RandomStringUtils.randomAlphabetic(5)
            false -> null
        }
        val text = listOf(RandomStringUtils.randomAlphabetic(5))
        return createMinusPhraseInDirectWithGrutHierarchy(name, text, isLibrary, client)
    }

    fun createRandomMinusPhraseInDirectAndGrut(client: ClientInfo? = null, isLibrary: Boolean = false): MinusPhrase {
        val name = when (isLibrary) {
            true -> RandomStringUtils.randomAlphabetic(5)
            false -> null
        }
        val text = listOf(RandomStringUtils.randomAlphabetic(5))
        val directMinusPhrase = createMinusPhraseInDirectWithGrutHierarchy(name, text, isLibrary, client)
        replicationService.minusPhrasesGrutDao.createOrUpdateMinusPhrases(listOf(directMinusPhrase))
        return directMinusPhrase
    }

    fun deleteBiddableShowConditionFromMySql(shard: Int, type: BiddableShowConditionType, id: Long) {
        when (type) {
            BiddableShowConditionType.KEYWORD -> biddableShowConditionTestRepository.deleteKeyword(shard, id)
            BiddableShowConditionType.DYNAMIC -> biddableShowConditionTestRepository.deleteDynamicBid(shard, id)
            BiddableShowConditionType.PERFORMANCE -> biddableShowConditionTestRepository.deletePerformanceBid(shard, id)
            BiddableShowConditionType.RELEVANCE_MATCH, BiddableShowConditionType.OFFER_RETARGETING ->
                biddableShowConditionTestRepository.deleteBidsBase(shard, id)
            else -> {
                // ничего
            }
        }
    }

    data class OrderAdGroupInfo(val adGroup: AdGroupInfo, val orderId: Long)
}
