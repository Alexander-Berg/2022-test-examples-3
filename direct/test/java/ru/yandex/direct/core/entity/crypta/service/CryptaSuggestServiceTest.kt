package ru.yandex.direct.core.entity.crypta.service

import com.google.protobuf.util.JsonFormat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.isNull
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies
import ru.yandex.crypta.siberia.bin.custom_audience.common.proto.TCaRule
import ru.yandex.crypta.siberia.bin.custom_audience.suggester.grpc.TExport
import ru.yandex.crypta.siberia.bin.custom_audience.suggester.grpc.TItem
import ru.yandex.direct.core.entity.crypta.exception.InvalidCryptaSegmentException
import ru.yandex.direct.core.entity.crypta.model.CryptaSegmentValue
import ru.yandex.direct.core.entity.crypta.repository.CryptaSegmentRepository
import ru.yandex.direct.core.entity.crypta.service.CryptaSuggestServiceTest.HostTestSupporter.PRESENTED_HOST_IDS
import ru.yandex.direct.core.entity.crypta.service.CryptaSuggestServiceTest.HostTestSupporter.hostIdToTItem
import ru.yandex.direct.core.entity.crypta.service.CryptaSuggestServiceTest.HostTestSupporter.randomElementFrom
import ru.yandex.direct.core.entity.crypta.service.CryptaSuggestServiceTest.HostTestSupporter.randomElementNotFrom
import ru.yandex.direct.core.entity.crypta.service.CryptaSuggestServiceTest.HostTestSupporter.randomGoalId
import ru.yandex.direct.core.entity.crypta.service.CryptaSuggestServiceTest.HostTestSupporter.toGoal
import ru.yandex.direct.core.entity.lal.CaLalSegmentRepository
import ru.yandex.direct.core.entity.retargeting.Constants
import ru.yandex.direct.core.entity.retargeting.model.CryptaGoalScope
import ru.yandex.direct.core.entity.retargeting.model.CryptaGoalsSuggestItem
import ru.yandex.direct.core.entity.retargeting.model.CryptaGoalsSuggestSegment
import ru.yandex.direct.core.entity.retargeting.model.CryptaGoalsSuggestType
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.retargeting.model.Goal.CRYPTA_INTERESTS_UPPER_BOUND
import ru.yandex.direct.core.entity.retargeting.model.Goal.CRYPTA_INTERNAL_UPPER_BOUND
import ru.yandex.direct.core.entity.retargeting.model.Goal.HOST_LOWER_BOUND
import ru.yandex.direct.core.entity.retargeting.model.Goal.HOST_UPPER_BOUND
import ru.yandex.direct.core.entity.retargeting.model.Goal.LAL_SEGMENT_UPPER_BOUND
import ru.yandex.direct.core.entity.retargeting.model.Goal.computeType
import ru.yandex.direct.core.entity.retargeting.model.GoalType
import ru.yandex.direct.crypta.client.CryptaClient
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import java.util.Optional

@RunWith(MockitoJUnitRunner::class)
class CryptaSuggestServiceTest {

    private val printer = JsonFormat.printer()
        .omittingInsignificantWhitespace()
        .preservingProtoFieldNames()

    @Mock
    private lateinit var cryptaClient: CryptaClient

    @Mock
    private lateinit var cryptaSegmentRepository: CryptaSegmentRepository

    @Mock
    private lateinit var caLalSegmentRepository: CaLalSegmentRepository

    @InjectMocks
    private lateinit var cryptaSuggestService: CryptaSuggestService

    @Test
    fun shouldReturnSuggestFromCrypta() {
        val text = " test "
        val tItem = generateCryptaResponseItem()

        `when`(cryptaClient.getSuggestRules(text.trim()))
            .thenReturn(listOf(tItem))

        val result = cryptaSuggestService.getRetargetingSuggests(text)

        assertThat(result)
            .isNotNull
            .isNotEmpty
            .hasOnlyOneElementSatisfying {
                assertThat(it)
                    .isEqualTo(
                        CryptaGoalsSuggestItem()
                            .withText(tItem.text)
                            .withType(CryptaGoalsSuggestType.SEGMENT)
                            .withDescription(tItem.description)
                            .withSegments(
                                listOf(
                                    CryptaGoalsSuggestSegment()
                                        .withKeywordId(tItem.exportsList[0].keywordId)
                                        .withSegmentId(tItem.exportsList[0].segmentId)
                                )
                            )
                    )
            }
    }

    @Test
    fun shouldReturnGoalsForRetargeting_withExistingHost() {
        val interest = CryptaGoalsSuggestItem()
            .withText("cats")
            .withType(CryptaGoalsSuggestType.SEGMENT)
            .withSegments(
                listOf(
                    CryptaGoalsSuggestSegment().withSegmentId(1L).withKeywordId(1L)
                )
            )
        val goalInterest = Goal()
            .withId(CRYPTA_INTERESTS_UPPER_BOUND - 1)
            .withKeyword("1")
            .withKeywordValue("1") as Goal

        val host = CryptaGoalsSuggestItem()
            .withText("yandex.ru")
            .withType(CryptaGoalsSuggestType.HOST)
        val goalLal = Goal()
            .withId(LAL_SEGMENT_UPPER_BOUND - 1)
            .withCaText("yandex.ru") as Goal

        val customAudience = listOf(interest, host)

        `when`(cryptaSegmentRepository.findByKeywordIdSegmentId(eq(CryptaSegmentValue("1", "1")), isNull()))
            .thenReturn(Optional.of(goalInterest))

        `when`(caLalSegmentRepository.findAllByHosts(listOf(host.text)))
            .thenReturn(
                mapOf(
                    Pair(host.text, goalLal)
                )
            )

        val result = cryptaSuggestService.getRetargetingGoals(customAudience)

        verify(caLalSegmentRepository).createHostSegments(listOf())

        assertThat(result)
            .isNotNull
            .isNotEmpty
            .hasSameElementsAs(listOf(goalInterest, goalLal))
    }

    @Test
    fun shouldReturnGoalsForRetargeting_createNewLalSegment() {
        val interest = CryptaGoalsSuggestItem()
            .withText("cats")
            .withType(CryptaGoalsSuggestType.SEGMENT)
            .withSegments(
                listOf(
                    CryptaGoalsSuggestSegment().withSegmentId(1L).withKeywordId(1L)
                )
            )
        val goalInterest = Goal()
            .withId(CRYPTA_INTERESTS_UPPER_BOUND - 1)
            .withKeyword("1")
            .withKeywordValue("1") as Goal

        val host = CryptaGoalsSuggestItem()
            .withText("yandex.ru")
            .withType(CryptaGoalsSuggestType.HOST)
        val goalLal = Goal()
            .withTime(Constants.AUDIENCE_TIME_VALUE)
            .withCryptaParentRule(printer.print(TCaRule.newBuilder().addHosts(host.text).build()))
            .withCaText("yandex.ru") as Goal

        val customAudience = listOf(interest, host)

        `when`(cryptaSegmentRepository.findByKeywordIdSegmentId(eq(CryptaSegmentValue("1", "1")), isNull()))
            .thenReturn(Optional.of(goalInterest))

        `when`(caLalSegmentRepository.findAllByHosts(listOf(host.text)))
            .thenReturn(mapOf())

        val result = cryptaSuggestService.getRetargetingGoals(customAudience)

        verify(caLalSegmentRepository).createHostSegments(listOf(goalLal))

        assertThat(result)
            .isNotNull
            .isNotEmpty
            .containsExactly(goalInterest, goalLal)
    }

    @Test
    fun shouldThrowException_ifCryptaGoalNotFound() {
        val interest = CryptaGoalsSuggestItem()
            .withText("cats")
            .withType(CryptaGoalsSuggestType.SEGMENT)
            .withSegments(
                listOf(
                    CryptaGoalsSuggestSegment().withSegmentId(1L).withKeywordId(1L)
                )
            )

        val host = CryptaGoalsSuggestItem()
            .withText("yandex.ru")
            .withType(CryptaGoalsSuggestType.HOST)

        val customAudience = listOf(interest, host)

        `when`(cryptaSegmentRepository.findByKeywordIdSegmentId(eq(CryptaSegmentValue("1", "1")), isNull()))
            .thenReturn(Optional.empty())

        assertThatThrownBy { cryptaSuggestService.getRetargetingGoals(customAudience) }
            .isInstanceOf(InvalidCryptaSegmentException::class.java)
            .hasMessageContaining(interest.toString())
    }

    @Test
    fun shouldThrowException_ifSegmentsIsEmpty() {
        val interest = CryptaGoalsSuggestItem()
            .withText("cats")
            .withType(CryptaGoalsSuggestType.SEGMENT)
            .withSegments(listOf())

        assertThatThrownBy { cryptaSuggestService.getRetargetingGoals(listOf(interest)) }
            .isInstanceOf(InvalidCryptaSegmentException::class.java)
    }

    @Test
    fun shouldReturnSuggestItemsFromGoals() {
        val goalInterest = Goal()
            .withId(CRYPTA_INTERESTS_UPPER_BOUND - 1)
            .withName("cats")
            .withKeyword("1")
            .withKeywordValue("1") as Goal
        val goalLalHost = Goal()
            .withId(LAL_SEGMENT_UPPER_BOUND - 1)
            .withCaText("yandex.ru") as Goal
        val goalLal = Goal().withId(LAL_SEGMENT_UPPER_BOUND - 2) as Goal
        val goalUnsupported = Goal()

        val expectedInterest = CryptaGoalsSuggestItem()
            .withText(goalInterest.name)
            .withType(CryptaGoalsSuggestType.SEGMENT)
            .withSegments(listOf(CryptaGoalsSuggestSegment().withKeywordId(1L).withSegmentId(1L)))

        val expectedHost = CryptaGoalsSuggestItem()
            .withText(goalLalHost.caText)
            .withType(CryptaGoalsSuggestType.HOST)
            .withSegments(listOf())

        val result =
            cryptaSuggestService.getCryptaSuggestFromGoals(listOf(goalInterest, goalLal, goalLalHost, goalUnsupported))

        assertThat(result)
            .isNotNull
            .isNotEmpty
            .containsExactly(expectedInterest, expectedHost)
    }

    @Test
    fun shouldReturnSuggestItemsFromFoals_andEnhanceDataForInternalAd() {
        val goalInterest = Goal()
            .withId(CRYPTA_INTERNAL_UPPER_BOUND - 1)
            .withName("cats") as Goal
        val goalInDB = Goal()
            .withId(CRYPTA_INTERNAL_UPPER_BOUND - 1)
            .withName("cats")
            .withKeyword("1")
            .withKeywordValue("2") as Goal

        val expectedInterest = CryptaGoalsSuggestItem()
            .withText(goalInterest.name)
            .withType(CryptaGoalsSuggestType.SEGMENT)
            .withSegments(listOf(CryptaGoalsSuggestSegment().withKeywordId(1L).withSegmentId(2L)))

        `when`(cryptaSegmentRepository.getByIdsForCa(eq(listOf(goalInterest.id))))
            .thenReturn(mapOf(goalInterest.id to goalInDB))

        val result = cryptaSuggestService.getCryptaSuggestFromGoals(listOf(goalInterest))

        assertThat(result)
            .isNotNull
            .isNotEmpty
            .containsExactly(expectedInterest)
    }

    @Test
    fun shouldReturnRetargetingGoalsSuggest() {
        val inputText = "Cat"
        val cryptaItemCats = generateCryptaResponseItem()
        val cryptaItemHost = TItem.newBuilder()
            .setHostId(Goal.HOST_LOWER_BOUND)
            .setText("cat.ru")
            .setType(CryptaGoalsSuggestType.HOST.name)
            .build()
        val notExistingCryptaItem = TItem.newBuilder()
            .setText("new cats")
            .setType(CryptaGoalsSuggestType.SEGMENT.name)
            .setDescription("description")
            .addExports(
                TExport.newBuilder()
                    .setKeywordId(2L)
                    .setSegmentId(2L)
                    .build()
            ).build()

        val directGoalForCats = Goal()
            .withId(Goal.CRYPTA_INTERESTS_CA_LOWER_BOUND)
            .withKeyword("1")
            .withKeywordValue("1")
                as Goal
        val expectedGoalHost = Goal()
            .withId(cryptaItemHost.hostId)
            .withName(cryptaItemHost.text)
                as Goal

        `when`(cryptaClient.getSuggestRules(eq(inputText), any(), any()))
            .thenReturn(listOf(cryptaItemCats, cryptaItemHost, notExistingCryptaItem))
        `when`(cryptaSegmentRepository.getAll(CryptaGoalScope.PERFORMANCE))
            .thenReturn(mapOf(1L to directGoalForCats))

        val result = cryptaSuggestService.getRetargetingGoalsSuggest(inputText)

        val beanDiffer = beanDiffer(listOf(directGoalForCats, expectedGoalHost))
            .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())

        assertThat(result)
            //.hasSameElementsAs(mutableListOf(directGoalForCats, expectedGoalHost))
            .`is`(matchedBy(beanDiffer))
    }

    @Test
    fun `test empty list as argument for getGoalsByHostIds`() {
        `when`(cryptaClient.getHostsByIds(any())).thenReturn(emptyList())
        assertThat(cryptaSuggestService.getGoalsByHostIds(emptyList())).isEqualTo(emptyMap<Long, Goal>())
    }

    @Test
    fun `test presented ids as argument for getGoalsByHostIds`() {
        `when`(cryptaClient.getHostsByIds(any())).thenReturn(PRESENTED_HOST_IDS.map { hostIdToTItem(it) })
        assertThat(cryptaSuggestService.getGoalsByHostIds(PRESENTED_HOST_IDS).map { it.value.id  }).isEqualTo(PRESENTED_HOST_IDS)
    }

    @Test
    fun `test presented ids with duplicates as argument for getGoalsByHostIds`() {
        `when`(cryptaClient.getHostsByIds(any())).thenReturn(PRESENTED_HOST_IDS.map { hostIdToTItem(it) })
        assertThat(cryptaSuggestService.getGoalsByHostIds(
            listOf(PRESENTED_HOST_IDS, PRESENTED_HOST_IDS).flatten()
        ).map { it.value.id  }).isEqualTo(PRESENTED_HOST_IDS)
    }

    @Test
    fun `test getGoalsByIds without duplicates`() {

        val goalMap = setupTestEnvironmentForGetModifiedGoals()

        val presentedGoalIds = listOf(
            randomElementFrom(goalMap, goalType = GoalType.HOST),
            randomElementFrom(goalMap, goalType = GoalType.GOAL)
        )

        val input = presentedGoalIds

        val actual = cryptaSuggestService.getGoalsByIds(input)
        val expected = presentedGoalIds
            .map { toGoal(it) }
            .associateBy { it.id }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test getGoalsByIds add only presented goals`() {

        val goalMap = setupTestEnvironmentForGetModifiedGoals()

        val presentedGoalIds = listOf(
            randomElementFrom(goalMap, goalType = GoalType.HOST),
            randomElementFrom(goalMap, goalType = GoalType.GOAL)
        )

        val notPresentedGoalIds = listOf(
            randomElementNotFrom(goalMap, goalType = GoalType.HOST),
            randomElementNotFrom(goalMap, goalType = GoalType.GOAL)
        )

        val input = listOf(
            presentedGoalIds,
            notPresentedGoalIds
        ).flatten()

        val actual = cryptaSuggestService.getGoalsByIds(input)
        val expected = presentedGoalIds
            .map { toGoal(it) }
            .associateBy { it.id }

        assertThat(actual).isEqualTo(expected)
    }

    private fun setupTestEnvironmentForGetModifiedGoals(): Map<GoalType, Set<Long>> {
        val hosts: List<Long> = (0..3).map { randomGoalId(GoalType.HOST) }
        val other: List<Long> = (0..3).map { randomGoalId(GoalType.GOAL) }


        `when`(cryptaClient.getHostsByIds(any())).thenAnswer { inv ->
            val given = inv.getArgument<List<Long>>(0)
            hosts.intersect(given.filter { computeType(it) == GoalType.HOST }.toSet())
                .map { hostIdToTItem(it) }
                .toList()
        }

        `when`(cryptaSegmentRepository.getByIdsForCa(any())).thenAnswer { inv ->
            val given = inv.getArgument<Collection<Long>>(0)
            other.intersect(given.filter { computeType(it) != GoalType.HOST }.toSet())
                .map { toGoal(it) }
                .associateBy { it.id }
        }

        return mapOf(
            GoalType.HOST to hosts.toSet(),
            GoalType.GOAL to other.toSet()
        )
    }



    object HostTestSupporter {
        val PRESENTED_HOST_IDS = listOf(1L)

        fun hostIdToTItem(id: Long): TItem = TItem.newBuilder()
            .setText(" host ")
            .setHostId(id)
            .build()

        fun toGoal(id: Long) : Goal {
            return when (computeType(id)) {
                GoalType.HOST -> CryptaSuggestConverter.convertHostToGoal(hostIdToTItem(id))
                else -> Goal().withId(id).withName("not host$id") as Goal
            }
        }

        fun randomGoalId(goalType: GoalType) : Long {
            return when (goalType) {
                GoalType.HOST -> HOST_LOWER_BOUND +
                    RandomNumberUtils.nextPositiveLong(HOST_UPPER_BOUND - HOST_LOWER_BOUND)
                else -> HOST_UPPER_BOUND + RandomNumberUtils.nextPositiveLong(Long.MAX_VALUE - HOST_UPPER_BOUND)
            }
        }

        fun randomElementFrom(goalIdsByType: Map<GoalType, Set<Long>>, goalType: GoalType): Long {
            return randomElementsFrom(goalIdsByType, goalType, 1).random()
        }

        private fun randomElementsFrom(goalIdsByType: Map<GoalType, Set<Long>>, goalType: GoalType, limit: Int): Set<Long> {
            return (1..limit).map { goalIdsByType[goalType]!!.random() }.toSet()
        }

        fun randomElementNotFrom(goalIdsByType: Map<GoalType, Set<Long>>, goalType: GoalType): Long {
            var id : Long
            do {
                id = randomGoalId(goalType)
            } while (goalIdsByType[goalType]!!.contains(id))
            return id
        }

    }

    private fun generateCryptaResponseItem() = TItem.newBuilder()
        .setText("cats")
        .setType("segment")
        .setDescription("description")
        .addExports(
            TExport.newBuilder()
                .setKeywordId(1L)
                .setSegmentId(1L)
                .build()
        ).build()
}
