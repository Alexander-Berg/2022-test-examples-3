package ru.yandex.direct.logicprocessor.processors.mysql2grut

import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.grut.api.BiddableShowConditionGrutApi.Companion.directIdToGrutId
import ru.yandex.direct.core.grut.api.BiddableShowConditionType
import ru.yandex.direct.core.grut.api.utils.moneyFromGrut
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.ess.logicobjects.mysql2grut.BiddableShowConditionChangeType
import ru.yandex.direct.ess.logicobjects.mysql2grut.Mysql2GrutReplicationObject
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorGrutTestConfiguration
import ru.yandex.direct.logicprocessor.processors.mysql2grut.steps.GrutReplicationSteps
import ru.yandex.direct.mysql2grut.enummappers.BiddableShowConditionEnumMappers

@ContextConfiguration(classes = [EssLogicProcessorGrutTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class BiddableShowConditionReplicationTest {
    @Autowired
    private lateinit var processor: Mysql2GrutReplicationProcessor

    @Autowired
    private lateinit var replicationService: GrutApiService

    @Autowired
    private lateinit var grutSteps: GrutReplicationSteps

    @AfterEach
    fun tearDown() {
        grutSteps.cleanupClientsWithChildren()
    }

    private fun <T> replicateAdGroupWithHierarchy(bsbObject: GrutReplicationSteps.AdGroupChild<T>) {
        val processorArgs = mutableListOf(
            Mysql2GrutReplicationObject(
                clientId = bsbObject.adGroupInfo.clientId.asLong()
            ),
            Mysql2GrutReplicationObject(
                campaignId = bsbObject.adGroupInfo.campaignId
            ),
            Mysql2GrutReplicationObject(
                adGroupId = bsbObject.adGroupInfo.adGroupId
            ),
        )
        processor.withShard(bsbObject.adGroupInfo.shard)
        processor.process(processorArgs)
    }


    @Test
    fun replicateKeyword() {
        //arrange
        val keywordWithClientId = grutSteps.createKeywordInMySqlWithGrutClient()
        val keyword = keywordWithClientId.obj.keyword
        //реплицируем кампанию, группу и keyword, клиент уже есть в Грут
        replicateAdGroupWithHierarchy(keywordWithClientId)
        val processorArgs = mutableListOf(
            Mysql2GrutReplicationObject(
                biddableShowConditionType = BiddableShowConditionChangeType.KEYWORD,
                biddableShowConditionId = keywordWithClientId.obj.id)
        )

        //act
        processor.process(processorArgs)
        //assert
        val biddableShowCondition = replicationService.biddableShowConditionReplicationGrutDao.getObjectsByDirectIds(
            BiddableShowConditionType.KEYWORD,
            listOf(keyword.id)).first()

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(biddableShowCondition.meta.id).isEqualTo(keyword.id)
            softly.assertThat(biddableShowCondition.spec.keyword).isNotNull
            softly.assertThat(moneyFromGrut(biddableShowCondition.spec.keyword.price)).isEqualTo(keyword.price)
            softly.assertThat(moneyFromGrut(biddableShowCondition.spec.keyword.priceContext)).isEqualTo(keyword.priceContext)
            softly.assertThat(biddableShowCondition.spec.keyword.phrase).isEqualTo(keyword.phrase)
            softly.assertThat(biddableShowCondition.spec.keyword.isSuspended).isEqualTo(keyword.isSuspended)
            softly.assertThat(biddableShowCondition.spec.keyword.showsForecast).isEqualTo(keyword.showsForecast)
        }
    }

    @Test
    fun deleteKeyword() {
        //arrange
        val keywordWithClientId = grutSteps.createKeywordInMySqlWithGrutClient()
        replicateAdGroupWithHierarchy(keywordWithClientId)
        val keyword = keywordWithClientId.obj.keyword
        val replicateKeywordArgs = mutableListOf(
            Mysql2GrutReplicationObject(
                biddableShowConditionType = BiddableShowConditionChangeType.KEYWORD,
                biddableShowConditionId = keywordWithClientId.obj.id)
        )
        processor.process(replicateKeywordArgs)
        //удаляем ключевое слово в директе
        grutSteps.deleteBiddableShowConditionFromMySql(keywordWithClientId.obj.shard, BiddableShowConditionType.KEYWORD, keywordWithClientId.obj.id)
        //act
        //реплицируем удаление в груте
        val removeKeywordArgs = mutableListOf(
            Mysql2GrutReplicationObject(
                biddableShowConditionType = BiddableShowConditionChangeType.KEYWORD,
                biddableShowConditionId = keywordWithClientId.obj.id,
                isDeleted = true
            )
        )
        processor.process(removeKeywordArgs)
        //assert
        val biddableShowCondition = replicationService.biddableShowConditionReplicationGrutDao.getExistingObjects(listOf(keyword.id))
        assertThat(biddableShowCondition.size).isEqualTo(0)
    }

    @Test
    fun replicateRelevanceMatch() {
        //arrange
        val relevanceMatchWithClientId = grutSteps.createRelevanceMatch()
        val relevanceMatch = relevanceMatchWithClientId.obj
        //реплицируем кампанию, группу и keyword, клиент уже есть в Грут
        replicateAdGroupWithHierarchy(relevanceMatchWithClientId)
        val processorArgs = mutableListOf(
            Mysql2GrutReplicationObject(
                biddableShowConditionType = BiddableShowConditionChangeType.RELEVANCE_MATCH,
                biddableShowConditionId = relevanceMatch.id,
            )
        )
        //act
        processor.process(processorArgs)
        //assert
        val biddableShowCondition = replicationService.biddableShowConditionReplicationGrutDao.getObjectsByDirectIds(
            BiddableShowConditionType.RELEVANCE_MATCH,
            listOf(relevanceMatch.id)).first()

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(biddableShowCondition.meta.id).isEqualTo(relevanceMatch.id)
            softly.assertThat(biddableShowCondition.spec.relevanceMatch).isNotNull
            softly.assertThat(moneyFromGrut(biddableShowCondition.spec.relevanceMatch.price).longValueExact()).isEqualTo(relevanceMatch.price.longValueExact())
            softly.assertThat(moneyFromGrut(biddableShowCondition.spec.relevanceMatch.priceContext).longValueExact()).isEqualTo(relevanceMatch.priceContext.longValueExact())
            softly.assertThat(biddableShowCondition.spec.relevanceMatch.isSuspended).isEqualTo(relevanceMatch.isSuspended)
            softly.assertThat(biddableShowCondition.spec.relevanceMatch.categoriesCount).isEqualTo(relevanceMatch.relevanceMatchCategories.size)
        }
    }

    @Test
    fun deleteRelevanceMatch() {
        //arrange
        //создаем объект в груте и директе
        val relevanceMatchWithAdGroup = grutSteps.createRelevanceMatch()
        replicateAdGroupWithHierarchy(relevanceMatchWithAdGroup)
        val relevanceMatch = relevanceMatchWithAdGroup.obj
        val replicateKeywordArgs = mutableListOf(
            Mysql2GrutReplicationObject(
                biddableShowConditionType = BiddableShowConditionChangeType.RELEVANCE_MATCH,
                biddableShowConditionId = relevanceMatch.id)
        )
        processor.process(replicateKeywordArgs)
        //удаляем объект слово в директе
        grutSteps.deleteBiddableShowConditionFromMySql(relevanceMatchWithAdGroup.adGroupInfo.shard, BiddableShowConditionType.RELEVANCE_MATCH, relevanceMatch.id)
        //act
        //реплицируем удаление в груте
        val removeKeywordArgs = mutableListOf(
            Mysql2GrutReplicationObject(
                biddableShowConditionType = BiddableShowConditionChangeType.RELEVANCE_MATCH,
                biddableShowConditionId = relevanceMatchWithAdGroup.obj.id,
                isDeleted = true
            )
        )
        processor.process(removeKeywordArgs)
        //assert
        val biddableShowCondition = replicationService.biddableShowConditionReplicationGrutDao.getExistingObjects(listOf(relevanceMatch.id))
        assertThat(biddableShowCondition.size).isEqualTo(0)
    }

    @Test
    fun replicateOfferRetargeting() {
        //arrange
        val offerRetargetingInfo = grutSteps.createOfferRetargeting()
        val offerRetargeting = offerRetargetingInfo.obj
        //реплицируем кампанию, группу и keyword, клиент уже есть в Грут
        replicateAdGroupWithHierarchy(offerRetargetingInfo)
        val processorArgs = mutableListOf(
            Mysql2GrutReplicationObject(
                biddableShowConditionType = BiddableShowConditionChangeType.OFFER_RETARGETING,
                biddableShowConditionId = offerRetargeting.id,
            )
        )
        //act
        processor.process(processorArgs)
        //assert
        val biddableShowCondition = replicationService.biddableShowConditionReplicationGrutDao.getObjectsByDirectIds(
            BiddableShowConditionType.OFFER_RETARGETING,
            listOf(offerRetargeting.id)).first()

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(biddableShowCondition.meta.id).isEqualTo(offerRetargeting.id)
            softly.assertThat(biddableShowCondition.spec.offerRetargeting).isNotNull
            softly.assertThat(biddableShowCondition.spec.offerRetargeting.isSuspended).isEqualTo(offerRetargeting.isSuspended)
        }
    }

    @Test
    fun deleteOfferRetargeting() {
        //arrange
        //создаем объект в груте и директе
        val relevanceMatchWithAdGroup = grutSteps.createOfferRetargeting()
        replicateAdGroupWithHierarchy(relevanceMatchWithAdGroup)
        val relevanceMatch = relevanceMatchWithAdGroup.obj
        val replicateKeywordArgs = mutableListOf(
            Mysql2GrutReplicationObject(
                biddableShowConditionType = BiddableShowConditionChangeType.OFFER_RETARGETING,
                biddableShowConditionId = relevanceMatch.id)
        )
        processor.process(replicateKeywordArgs)
        //удаляем объект слово в директе
        grutSteps.deleteBiddableShowConditionFromMySql(relevanceMatchWithAdGroup.adGroupInfo.shard, BiddableShowConditionType.OFFER_RETARGETING, relevanceMatch.id)
        //act
        //реплицируем удаление в груте
        val removeKeywordArgs = mutableListOf(
            Mysql2GrutReplicationObject(
                biddableShowConditionType = BiddableShowConditionChangeType.OFFER_RETARGETING,
                biddableShowConditionId = relevanceMatchWithAdGroup.obj.id,
                isDeleted = true
            )
        )
        processor.process(removeKeywordArgs)
        //assert
        val biddableShowCondition = replicationService.biddableShowConditionReplicationGrutDao.getExistingObjects(listOf(relevanceMatch.id))
        assertThat(biddableShowCondition.size).isEqualTo(0)
    }


    @Test
    fun replicateDynamic() {
        //arrange
        val dynamicInfo = grutSteps.createDynamic()
        val dynamic = dynamicInfo.obj
        //реплицируем кампанию, группу и keyword, клиент уже есть в Грут
        replicateAdGroupWithHierarchy(dynamicInfo)
        val processorArgs = mutableListOf(
            Mysql2GrutReplicationObject(
                biddableShowConditionType = BiddableShowConditionChangeType.DYNAMIC,
                biddableShowConditionId = dynamic.dynId,
            )
        )
        //act
        processor.process(processorArgs)
        //assert
        val biddableShowCondition = replicationService.biddableShowConditionReplicationGrutDao.getObjectsByDirectIds(
            BiddableShowConditionType.DYNAMIC,
            listOf(dynamic.dynId)).first()

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(biddableShowCondition.meta.id).isEqualTo(directIdToGrutId(BiddableShowConditionType.DYNAMIC, dynamic.dynId))
            softly.assertThat(biddableShowCondition.spec.dynamic).isNotNull
            softly.assertThat(moneyFromGrut(biddableShowCondition.spec.dynamic.price).longValueExact()).isEqualTo(dynamic.price.longValueExact())
            softly.assertThat(moneyFromGrut(biddableShowCondition.spec.dynamic.priceContext).longValueExact()).isEqualTo(dynamic.priceContext.longValueExact())
            softly.assertThat(biddableShowCondition.spec.dynamic.isSuspended).isEqualTo(dynamic.opts.contains("suspended"))
            softly.assertThat(biddableShowCondition.spec.dynamic.condition).isNotNull
        }
    }

    @Test
    fun deleteDynamicAndPerformance() {
        //arrange
        //создаем объект в груте и директе
        val dynamicInfo = grutSteps.createDynamic()
        val dynamic = dynamicInfo.obj

        val performanceInfo = grutSteps.createPerformance()
        val performance = performanceInfo.obj        //реплицируем кампанию, группу и keyword, клиент уже есть в Грут

        replicateAdGroupWithHierarchy(dynamicInfo)
        val processorArgs = mutableListOf(
            Mysql2GrutReplicationObject(
                biddableShowConditionType = BiddableShowConditionChangeType.DYNAMIC,
                biddableShowConditionId = dynamic.dynId,
            ),
            Mysql2GrutReplicationObject(
                biddableShowConditionType = BiddableShowConditionChangeType.PERFORMANCE,
                biddableShowConditionId = performance.filterId,
            )
        )
        processor.process(processorArgs)
        //удаляем объект в директе
        grutSteps.deleteBiddableShowConditionFromMySql(dynamicInfo.adGroupInfo.shard, BiddableShowConditionType.DYNAMIC, dynamic.dynId)
        grutSteps.deleteBiddableShowConditionFromMySql(dynamicInfo.adGroupInfo.shard, BiddableShowConditionType.PERFORMANCE, performance.filterId)
        //act
        //реплицируем удаление в груте
        val removeArgs = mutableListOf(
            Mysql2GrutReplicationObject(
                biddableShowConditionType = BiddableShowConditionChangeType.DYNAMIC,
                biddableShowConditionId = dynamic.dynId,
                isDeleted = true
            ),
            Mysql2GrutReplicationObject(
                biddableShowConditionType = BiddableShowConditionChangeType.PERFORMANCE,
                biddableShowConditionId = performance.filterId,
                isDeleted = true
            )

        )
        processor.process(removeArgs)
        //assert
        val biddableShowCondition = replicationService.biddableShowConditionReplicationGrutDao.getExistingObjects(
            listOf(
                directIdToGrutId(BiddableShowConditionType.DYNAMIC, dynamic.dynId),
                directIdToGrutId(BiddableShowConditionType.PERFORMANCE, performance.filterId),
            ))
        assertThat(biddableShowCondition.size).isEqualTo(0)
    }

    @Test
    fun replicatePerformance() {
        val performanceInfo = grutSteps.createPerformance()
        val performance = performanceInfo.obj
        //реплицируем кампанию, группу и keyword, клиент уже есть в Грут
        replicateAdGroupWithHierarchy(performanceInfo)
        val processorArgs = mutableListOf(
            Mysql2GrutReplicationObject(
                biddableShowConditionType = BiddableShowConditionChangeType.PERFORMANCE,
                biddableShowConditionId = performance.filterId,
            )
        )
        //act
        processor.process(processorArgs)
        //assert
        val biddableShowCondition = replicationService.biddableShowConditionReplicationGrutDao.getObjectsByDirectIds(
            BiddableShowConditionType.PERFORMANCE,
            listOf(performance.filterId)).first()

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(biddableShowCondition.meta.id).isEqualTo(directIdToGrutId(BiddableShowConditionType.PERFORMANCE, performance.filterId))
            softly.assertThat(biddableShowCondition.spec.performance).isNotNull
            softly.assertThat(moneyFromGrut(biddableShowCondition.spec.performance.priceCpa).longValueExact()).isEqualTo(performance.filter.priceCpa.longValueExact())
            softly.assertThat(moneyFromGrut(biddableShowCondition.spec.performance.priceCpc).longValueExact()).isEqualTo(performance.filter.priceCpc.longValueExact())
            softly.assertThat(biddableShowCondition.spec.performance.name).isEqualTo(performance.filter.name)
            softly.assertThat(biddableShowCondition.spec.performance.targetFunnel)
                .isEqualTo(BiddableShowConditionEnumMappers.toGrutTargetFunnel(performance.filter.targetFunnel).number)
        }
    }

    @Test
    fun replicateRetargeting() {
        val retargetingInfo = grutSteps.createRetargeting()
        val retargeting = retargetingInfo.obj
        //реплицируем кампанию и группу и , клиент уже есть в Грут
        replicateAdGroupWithHierarchy(retargetingInfo)
        val processorArgs = listOf(
            Mysql2GrutReplicationObject(
                retargetingConditionId = retargeting.retConditionId,
            ),
            Mysql2GrutReplicationObject(
                biddableShowConditionType = BiddableShowConditionChangeType.RETARGETING,
                biddableShowConditionId = retargeting.retargetingId,
            )
        )
        //act
        processor.process(processorArgs)
        //assert
        val biddableShowCondition = replicationService.biddableShowConditionReplicationGrutDao.getObjectsByDirectIds(
            BiddableShowConditionType.RETARGETING,
            listOf(retargeting.retargetingId)).first()

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(biddableShowCondition.meta.id).isEqualTo(directIdToGrutId(BiddableShowConditionType.RETARGETING, retargeting.retargetingId))
            softly.assertThat(biddableShowCondition.spec.retargeting).isNotNull
            softly.assertThat(moneyFromGrut(biddableShowCondition.spec.retargeting.priceContext).longValueExact()).isEqualTo(retargeting.retargeting.priceContext.longValueExact())
            softly.assertThat(biddableShowCondition.spec.retargetingConditionId).isEqualTo(retargeting.retargeting.retargetingConditionId)
        }
    }


    @Test
    fun deleteRetargeting() {
        val retargetingInfo = grutSteps.createRetargeting()
        val retargeting = retargetingInfo.obj
        // Реплицируем кампанию и группу и , клиент уже есть в Грут
        replicateAdGroupWithHierarchy(retargetingInfo)
        val processorArgs = listOf(
            Mysql2GrutReplicationObject(
                retargetingConditionId = retargeting.retConditionId,
            ),
            Mysql2GrutReplicationObject(
                biddableShowConditionType = BiddableShowConditionChangeType.RETARGETING,
                biddableShowConditionId = retargeting.retargetingId,
            )
        )
        processor.process(processorArgs)
        val existing = replicationService.biddableShowConditionReplicationGrutDao.getObjectsByDirectIds(
            BiddableShowConditionType.RETARGETING,
            listOf(retargeting.retargetingId)
        )
        // Проверяем, что bsc есть в GrUT перед удалением
        Assertions.assertTrue(existing.size == 1)
        // Удаляем объект слово в директе
        grutSteps.deleteBiddableShowConditionFromMySql(retargetingInfo.adGroupInfo.shard, BiddableShowConditionType.RETARGETING, retargeting.retargetingId)
        //act
        // Реплицируем удаление в груте
        val removeKeywordArgs = mutableListOf(
            Mysql2GrutReplicationObject(
                biddableShowConditionType = BiddableShowConditionChangeType.RETARGETING,
                biddableShowConditionId = retargeting.retargetingId,
                isDeleted = true
            )
        )
        processor.process(removeKeywordArgs)
        //assert
        val biddableShowCondition = replicationService.biddableShowConditionReplicationGrutDao.getExistingObjects(listOf(retargeting.retargetingId))
        assertThat(biddableShowCondition.size).isEqualTo(0)
    }
}
