package ru.yandex.direct.jobs.crypta

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyCollection
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcProperty
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.core.entity.crypta.model.CaCryptaSegment
import ru.yandex.direct.core.entity.crypta.model.CaCryptaSegmentExport
import ru.yandex.direct.core.entity.crypta.repository.CryptaSegmentRepository
import ru.yandex.direct.core.entity.crypta.repository.CryptaSegmentsYtRepository
import ru.yandex.direct.core.entity.retargeting.model.CryptaGoalScope
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.util.concurrent.atomic.AtomicInteger

@ExtendWith(MockitoExtension::class)
internal class ImportCryptaSegmentsForCaJobTest {

    companion object {
        private const val TANKER_NAME_KEY = "tanker_name_key"
        private const val TANKER_DESCRIPTION_KEY = "tanker_description_key"
        private const val TANKER_SEGMENT_TYPE_KEY = "tanker_segment_type_key"
    }

    @InjectMocks
    private lateinit var job: ImportCryptaSegmentsForCaJob

    @Mock
    private lateinit var cryptaSegmentsYtRepository: CryptaSegmentsYtRepository
    @Mock
    private lateinit var cryptaSegmentRepository: CryptaSegmentRepository
    @Mock
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport
    @Mock
    private lateinit var isJobEnabled: PpcProperty<Boolean>

    @Captor
    private lateinit var argumentCaptor: ArgumentCaptor<Collection<Goal>>

    private val counter = AtomicInteger(1)

    @BeforeEach
    fun setUp() {
        `when`(ppcPropertiesSupport.get(PpcPropertyNames.IMPORT_CRYPTA_SEGMENTS_FOR_CA_ENABLED))
            .thenReturn(isJobEnabled)
        `when`(isJobEnabled.getOrDefault(any()))
            .thenCallRealMethod()
        `when`(isJobEnabled.get())
            .thenReturn(true)
    }

    @Test
    fun shouldImportDataFromCrypta_withLongTermSegmentOnly() {
        val cryptaSegment = createCryptaSegment()
        val expectedGoal = Goal()
            .withId(Goal.CRYPTA_INTERESTS_CA_LOWER_BOUND)
            .withKeyword(cryptaSegment.exports[0].keywordId.toString())
            .withKeywordValue(cryptaSegment.exports[0].segmentId.toString())
            .withCryptaScope(setOf(CryptaGoalScope.MEDIA))
            .withTankerNameKey(TANKER_NAME_KEY)
            .withTankerDescriptionKey(TANKER_DESCRIPTION_KEY)
            .withTankerAudienceTypeKey(TANKER_SEGMENT_TYPE_KEY)
                as Goal

        `when`(cryptaSegmentsYtRepository.getAll())
            .thenReturn(listOf(cryptaSegment))

        job.execute()

        verify(cryptaSegmentRepository, times(1)).add(argumentCaptor.capture())

        val differ = beanDiffer(listOf(expectedGoal)).useCompareStrategy(onlyExpectedFields())
        assertThat(argumentCaptor.value)
            .hasSize(1)
            .`is`(matchedBy(differ))
    }

    @Test
    fun shouldImportDataFromCrypta_withShortTermSegment() {
        val cryptaSegment = createCryptaSegment(2)
        val expectedGoal = Goal()
            .withId(Goal.CRYPTA_INTERESTS_CA_LOWER_BOUND)
            .withKeyword(cryptaSegment.exports[0].keywordId.toString())
            .withKeywordValue(cryptaSegment.exports[0].segmentId.toString())
            .withKeywordShort(cryptaSegment.exports[1].keywordId.toString())
            .withKeywordValueShort(cryptaSegment.exports[1].segmentId.toString())
            .withCryptaScope(setOf(CryptaGoalScope.MEDIA))
            .withTankerNameKey(TANKER_NAME_KEY)
            .withTankerDescriptionKey(TANKER_DESCRIPTION_KEY)
            .withTankerAudienceTypeKey(TANKER_SEGMENT_TYPE_KEY)
                as Goal

        `when`(cryptaSegmentsYtRepository.getAll())
            .thenReturn(listOf(cryptaSegment))

        job.execute()

        verify(cryptaSegmentRepository, times(1)).add(argumentCaptor.capture())

        val differ = beanDiffer(listOf(expectedGoal)).useCompareStrategy(onlyExpectedFields())
        assertThat(argumentCaptor.value)
            .hasSize(1)
            .`is`(matchedBy(differ))
    }

    @Test
    fun shouldSkipImport_ifPropertyIsNotSet() {
        `when`(isJobEnabled.get())
            .thenReturn(null)

        job.execute()

        verify(cryptaSegmentsYtRepository, never()).getAll()
        verify(cryptaSegmentRepository, never()).getAll(anyList())
        verify(cryptaSegmentRepository, never()).add(anyCollection())
    }

    @Test
    fun shouldSkipImport_ifPropertyIsDisabled() {
        `when`(isJobEnabled.get())
            .thenReturn(false)

        job.execute()

        verify(cryptaSegmentsYtRepository, never()).getAll()
        verify(cryptaSegmentRepository, never()).getAll(anyList())
        verify(cryptaSegmentRepository, never()).add(anyCollection())
    }

    @Test
    fun shouldSkipImport_ifYtContainsEmptyExports() {
        val cryptaSegment = CaCryptaSegment()

        `when`(cryptaSegmentsYtRepository.getAll())
            .thenReturn(listOf(cryptaSegment))

        job.execute()

        verify(cryptaSegmentRepository, never()).add(anyCollection())
    }

    @Test
    fun shouldSkipImport_ifCryptaGoalsIsUpToDate() {
        val cryptaSegment = createCryptaSegment()
        val directGoal = Goal()
            .withId(Goal.CRYPTA_INTERESTS_CA_LOWER_BOUND)
            .withKeyword(cryptaSegment.exports[0].keywordId.toString())
            .withKeywordValue(cryptaSegment.exports[0].segmentId.toString())
                as Goal

        `when`(cryptaSegmentsYtRepository.getAll())
            .thenReturn(listOf(cryptaSegment))
        `when`(cryptaSegmentRepository.getAll(anyList()))
            .thenReturn(mapOf(directGoal.id to directGoal))

        job.execute()

        verify(cryptaSegmentRepository, never()).add(anyCollection())
    }

    @Test
    fun shouldThrowExceptionIfYtDataIsEmpty() {
        assertThrows<IllegalStateException> { job.execute() }
    }

    @Test
    fun shouldThrowExceptionIfYtDataHasMoreThanTwoElementInExports() {
        val cryptaSegment = createCryptaSegment(3)
        `when`(cryptaSegmentsYtRepository.getAll())
            .thenReturn(listOf(cryptaSegment))

        val exception = assertThrows<IllegalArgumentException> { job.execute() }
        assertThat(exception)
            .hasMessageContaining(cryptaSegment.exports.joinToString { it.toString() })
    }

    private fun createCryptaSegment(exportLimit: Int = 1) = CaCryptaSegment()
        .withExports((1..exportLimit).map {  CaCryptaSegmentExport()
            .withSegmentId(counter.getAndIncrement())
            .withKeywordId(counter.getAndIncrement())
        })
        .withNames(listOf())
        .withCampaignTypes(listOf("media"))
        .withTankerNameKey(TANKER_NAME_KEY)
        .withTankerDescriptionKey(TANKER_DESCRIPTION_KEY)
        .withTankerSegmentTypeKey(TANKER_SEGMENT_TYPE_KEY)
}
