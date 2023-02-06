package ru.yandex.direct.grid.processing.service.goal

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.direct.common.TranslationService
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.grid.processing.model.goal.GdGoal
import ru.yandex.direct.grid.processing.model.goal.GdGoalTruncated
import ru.yandex.direct.grid.processing.model.goal.GdGoalType
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy

@RunWith(MockitoJUnitRunner::class)
class CryptaGoalsConverterTest {

    @Mock
    private lateinit var translationService: TranslationService

    @InjectMocks
    private lateinit var cryptaGoalsConverter: CryptaGoalsConverter

    @Test
    fun shouldTranslateGoals() {
        val name = "name"
        val description = "description"
        val tankerNameKey = "tankerNameKey"
        val tankerDescriptionKey = "tankerDescriptionKey"
        val host = "ya.ru"
        val goals = listOf(
            Goal()
                .withId(Goal.CRYPTA_INTERESTS_CA_LOWER_BOUND)
                .withTankerNameKey(tankerNameKey)
                .withTankerDescriptionKey(tankerDescriptionKey)
                    as Goal,
            Goal()
                .withId(Goal.HOST_LOWER_BOUND)
                .withName(host)
                    as Goal
        )

        val expectedGoals = listOf(
            GdGoal()
                .withId(Goal.CRYPTA_INTERESTS_CA_LOWER_BOUND)
                .withType(GdGoalType.INTERESTS)
                .withName(name)
                .withDescription(description)
                    as GdGoalTruncated,
            GdGoal()
                .withId(Goal.HOST_LOWER_BOUND)
                .withType(GdGoalType.HOST)
                .withName(host)
        )

        `when`(translationService.translate(anyString(), eq(tankerNameKey)))
            .thenReturn(name)
        `when`(translationService.translate(anyString(), eq(tankerDescriptionKey)))
            .thenReturn(description)

        val result = cryptaGoalsConverter.convertToTranslatedGdGoal(goals)

        val beanDiffer = beanDiffer(expectedGoals)
        assertThat(result)
            .`is`(matchedBy(beanDiffer))
    }

    @Test
    fun shouldSkipGoalsIfTranslationNotFound() {
        val host = "ya.ru"
        val goals = listOf(
            Goal()
                .withId(Goal.CRYPTA_INTERESTS_CA_LOWER_BOUND)
                .withTankerNameKey("name")
                .withTankerDescriptionKey("description")
                    as Goal,
            Goal()
                .withId(Goal.HOST_LOWER_BOUND)
                .withName(host)
                    as Goal
        )

        `when`(translationService.translate(anyString(), anyString()))
            .thenReturn(null)
        `when`(translationService.translate(anyString(), anyString()))
            .thenReturn(null)

        val result = cryptaGoalsConverter.convertToTranslatedGdGoal(goals)

        assertThat(result)
            .hasSize(1)
            .singleElement()
            .hasFieldOrPropertyWithValue("id", Goal.HOST_LOWER_BOUND)
            .hasFieldOrPropertyWithValue("type", GdGoalType.HOST)
            .hasFieldOrPropertyWithValue("name", host)
    }

    @Test
    fun shouldAddAudienceTypeForInterests() {
        val name = "name"
        val description = "description"
        val audienceTypeName = "organization visits"
        val tankerNameKey = "tankerNameKey"
        val tankerDescriptionKey = "tankerDescriptionKey"
        val tankerAudienceTypeKey = "tankerAudienceTypeKey"

        val goals = listOf(
            Goal()
                .withId(Goal.CRYPTA_INTERESTS_CA_LOWER_BOUND)
                .withTankerNameKey(tankerNameKey)
                .withTankerDescriptionKey(tankerDescriptionKey)
                .withTankerAudienceTypeKey(tankerAudienceTypeKey)
                    as Goal,
        )

        val expectedGoals = listOf(
            GdGoal()
                .withId(Goal.CRYPTA_INTERESTS_CA_LOWER_BOUND)
                .withType(GdGoalType.INTERESTS)
                .withName(name)
                .withDescription(description)
                .withAudienceTypeName(audienceTypeName)
                    as GdGoalTruncated,
        )

        `when`(translationService.translate(anyString(), eq(tankerNameKey)))
            .thenReturn(name)
        `when`(translationService.translate(anyString(), eq(tankerDescriptionKey)))
            .thenReturn(description)
        `when`(translationService.translate(anyString(), eq(tankerAudienceTypeKey)))
            .thenReturn(audienceTypeName)

        val result = cryptaGoalsConverter.convertToTranslatedGdGoal(goals)

        val beanDiffer = beanDiffer(expectedGoals)
        assertThat(result)
            .`is`(matchedBy(beanDiffer))
    }
}
