package ru.yandex.direct.logicprocessor.processors.mysql2grut

import org.assertj.core.api.Assertions
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AuditoriumGeoSegmentsAdGroupAdditionalTargeting
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.ContentCategoriesAdGroupAdditionalTargeting
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.ShowDatesAdGroupAdditionalTargeting
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.BrowserEnginesAdGroupAdditionalTargeting
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.VersionedTargetingHelper
import ru.yandex.direct.core.grut.api.utils.moscowLocalDateToGrut
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.mysql2grut.CryptaSegmentsCache
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.ess.logicobjects.mysql2grut.Mysql2GrutReplicationObject
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorGrutTestConfiguration
import ru.yandex.direct.logicprocessor.processors.mysql2grut.steps.GrutReplicationSteps
import ru.yandex.direct.mysql2grut.enummappers.AdGroupEnumMappers.Companion.toGrutJoinType
import ru.yandex.direct.mysql2grut.enummappers.AdGroupEnumMappers.Companion.toGrutTargetingMode
import ru.yandex.grut.objects.proto.AdGroupAdditionalTargeting.TAdGroupAdditionalTargeting.ETargetingType
import ru.yandex.grut.objects.proto.RetargetingGoalType.ERetargetingGoalType

@ContextConfiguration(classes = [EssLogicProcessorGrutTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class AdGroupShowConditionsReplicationTest {
    @Autowired
    private lateinit var processor: Mysql2GrutReplicationProcessor

    @Autowired
    private lateinit var replicationService: GrutApiService

    @Autowired
    private lateinit var grutSteps: GrutReplicationSteps

    @Autowired
    private lateinit var cryptaSegmentsCache: CryptaSegmentsCache

    private lateinit var adGroupWithTargeting: AdGroupInfo

    private lateinit var auditoriumGeosegmentsTargeting: AuditoriumGeoSegmentsAdGroupAdditionalTargeting
    private lateinit var browserEnginesTargeting: BrowserEnginesAdGroupAdditionalTargeting
    private lateinit var contentCategoriesTargeting: ContentCategoriesAdGroupAdditionalTargeting
    private lateinit var datesTargeting: ShowDatesAdGroupAdditionalTargeting

    @BeforeEach
    private fun setup() {
        cryptaSegmentsCache.clearCache()
        grutSteps.createCryptaCategories()
        val (adGroup, orderId) = grutSteps.createAdGroupInMySqlWithGrutHierarchy()
        auditoriumGeosegmentsTargeting = grutSteps.createAuditoriumGeosegmentsTargeting(adGroup)
        browserEnginesTargeting = grutSteps.createBrowserEnginesTargeting(adGroup)
        contentCategoriesTargeting = grutSteps.createContentCategoriesAdditionalTargeting(adGroup)
        datesTargeting = grutSteps.createShowDatesAdditoinalTargetings(adGroup)
        adGroupWithTargeting = adGroup
        processor.withShard(adGroup.shard)
    }

    @AfterEach
    private fun tearDown() {
        grutSteps.cleanupClientsWithChildren()
    }

    @Test
    fun replicateAdGroupWithTargeting() {
        //arrange
        val processorArgs = mutableListOf(Mysql2GrutReplicationObject(adGroupId = adGroupWithTargeting.adGroupId))
        //act
        processor.process(processorArgs)
        //assert
        val adGroup = replicationService.adGroupGrutDao.getAdGroup(adGroupWithTargeting.adGroupId)
        Assertions.assertThat(adGroup).isNotNull
        val targetingByType = adGroup!!.spec.additionalTargetingsList.groupBy { it.targetingType }
        val agTargeting = targetingByType[ETargetingType.TT_AUDITORIUM_GEOSEGMENTS.number]!!.first()
        val beTargeting = targetingByType[ETargetingType.TT_BROWSER_ENGINES.number]!!.first()
        val contentCategory = targetingByType[ETargetingType.TT_CONTENT_CATEGORIES.number]!!.first()
        val datesTargetingGrut = targetingByType[ETargetingType.TT_SHOW_DATES.number]!!.first()


        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(agTargeting.targetingMode).isEqualTo(toGrutTargetingMode(auditoriumGeosegmentsTargeting.targetingMode).number)
            softly.assertThat(agTargeting.joinType).isEqualTo(toGrutJoinType(auditoriumGeosegmentsTargeting.joinType).number)
            softly.assertThat(agTargeting.targetingType).isEqualTo(ETargetingType.TT_AUDITORIUM_GEOSEGMENTS.number)
            softly.assertThat(agTargeting.ints.valuesList).isEqualTo(auditoriumGeosegmentsTargeting.value.toList())

            softly.assertThat(beTargeting.targetingMode).isEqualTo(toGrutTargetingMode(browserEnginesTargeting.targetingMode).number)
            softly.assertThat(beTargeting.joinType).isEqualTo(toGrutJoinType(browserEnginesTargeting.joinType).number)
            softly.assertThat(beTargeting.targetingType).isEqualTo(ETargetingType.TT_BROWSER_ENGINES.number)

            val versionedEntry = beTargeting.versionedEntries.valuesList.first()
            softly.assertThat(versionedEntry.id).isEqualTo(browserEnginesTargeting.value.first().targetingValueEntryId)
            softly.assertThat(versionedEntry.minVersion).isEqualTo(browserEnginesTargeting.value.first()
                .minVersion.let { VersionedTargetingHelper.versionToInt(it).toLong() })
            softly.assertThat(versionedEntry.maxVersion).isEqualTo(browserEnginesTargeting.value.first()
                .maxVersion.let { VersionedTargetingHelper.versionToInt(it).toLong() })

            // content categories
            softly.assertThat(contentCategory).isNotNull
            softly.assertThat(contentCategory.targetingMode).isEqualTo(toGrutTargetingMode(auditoriumGeosegmentsTargeting.targetingMode).number)
            softly.assertThat(contentCategory.joinType).isEqualTo(toGrutJoinType(auditoriumGeosegmentsTargeting.joinType).number)
            softly.assertThat(contentCategory.targetingType).isEqualTo(ETargetingType.TT_CONTENT_CATEGORIES.number)
            val contentGenresEntry = contentCategory.contentCategories.valuesList.firstOrNull { it.goalType == ERetargetingGoalType.RGT_CRYPTA_GENRES_CATEGORY.number }
            softly.assertThat(contentGenresEntry).isNotNull
            val contentCategoryEntry = contentCategory.contentCategories.valuesList.firstOrNull { it.goalType == ERetargetingGoalType.RGT_CRYPTA_CONTENT_CATEGORY.number }
            softly.assertThat(contentCategoryEntry).isNotNull
            // dates
            softly.assertThat(datesTargetingGrut.dates.valuesList.first()).isEqualTo(moscowLocalDateToGrut(datesTargeting.value.first()))
            softly.assertThat(datesTargetingGrut.dates.valuesList.first()).isEqualTo(moscowLocalDateToGrut(datesTargeting.value.first()))
            softly.assertThat(datesTargetingGrut.joinType).isEqualTo(toGrutJoinType(datesTargeting.joinType).number)
            softly.assertThat(datesTargetingGrut.targetingMode).isEqualTo(toGrutTargetingMode(datesTargeting.targetingMode).number)

        }
    }

    @Test
    fun removeAdGroupTargeting() {
        //arrange
        val processorArgs = mutableListOf(Mysql2GrutReplicationObject(adGroupId = adGroupWithTargeting.adGroupId))
        //replicate adgroup with two targetings
        processor.process(processorArgs)
        //remove one targeting from mysql
        grutSteps.deleteTargetingFromMySql(adGroupWithTargeting.shard, browserEnginesTargeting.id)
        //act
        processor.process(processorArgs)
        //assert
        val adGroup = replicationService.adGroupGrutDao.getAdGroup(adGroupWithTargeting.adGroupId)
        Assertions.assertThat(adGroup!!.spec.additionalTargetingsCount).isEqualTo(3)
    }
}
