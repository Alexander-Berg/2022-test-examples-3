package ru.yandex.direct.core.entity.adgroup.service.validation.types

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType
import ru.yandex.direct.core.entity.adgroup.service.AdGroupCpmPriceUtils
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage
import ru.yandex.direct.core.entity.pricepackage.model.PriceRetargetingCondition
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.retargeting.model.Rule
import ru.yandex.direct.core.entity.retargeting.model.RuleType
import ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects
import ru.yandex.direct.core.testing.data.TestPricePackages
import ru.yandex.direct.core.testing.data.TestPricePackages.BOOKS_GOAL_ID
import ru.yandex.direct.core.testing.data.TestPricePackages.MALE_CRYPTA_GOAL_ID
import ru.yandex.direct.core.testing.data.TestPricePackages.SCIENCE_GOAL_ID
import ru.yandex.direct.core.testing.data.TestPricePackages.SPORT_GOAL_ID
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith
import ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.CommonDefects.invalidValue
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path
import ru.yandex.direct.validation.result.ValidationResult

class PriceSalesAdGroupContentCategoriesValidatorTest {
    private fun validate(goals: List<Long?>,
                         pricePackage: PricePackage = pricePackage(),
                         defAdGroupContentCategoriesRules: List<Rule> = emptyList()
    ): ValidationResult<List<Rule>, Defect<*>> {
        val categoriesValidationData = PriceSalesAdGroupContentCategoriesValidationData(pricePackage,
            AdGroupCpmPriceUtils.PRIORITY_SPECIFIC, defAdGroupContentCategoriesRules)
        val validator = PriceSalesAdGroupContentCategoriesValidator(categoriesValidationData)
        val rule = Rule().withType(RuleType.OR).withGoals(goals.map { Goal().withId(it) as Goal })
        return validator.apply(listOf(rule))
    }

    @Test
    fun `Валидные категории видеогруппы`() {
        val result = validate(listOf(SCIENCE_GOAL_ID, SPORT_GOAL_ID, MALE_CRYPTA_GOAL_ID))
        assertThat(result).`is`(matchedBy(hasNoDefectsDefinitions<Any>()))
    }

    @Test
    fun forbiddenGoal() {
        //Категория Образование 4294968334 запрещена пакетом
        val result = validate(listOf(SCIENCE_GOAL_ID, 4294968334L))
        assertThat(result).`is`(matchedBy(hasDefectDefinitionWith<Any>(
            validationError(path(index(0), field(Rule.GOALS)), invalidValue()))))
    }

    @Test
    fun `Отсутствует обязательная категория`() {
        val result = validate(listOf(SPORT_GOAL_ID))
        assertThat(result).`is`(matchedBy(hasDefectDefinitionWith<Any>(
            validationError(path(index(0), field(Rule.GOALS)), invalidValue()))))
    }

    @Test
    fun socdemIgnore() {
        var pack = pricePackage()
        pack.targetingsFixed
            .withCryptaSegments(listOf(SCIENCE_GOAL_ID, MALE_CRYPTA_GOAL_ID))
        val result = validate(listOf(SCIENCE_GOAL_ID, SPORT_GOAL_ID), pack)
        assertThat(result).`is`(matchedBy(hasNoDefectsDefinitions<Any>()))
    }

    @Test
    fun specificAdGroup_DefaultEmpty_Ok() {
        //тесты на валидацию специфичной группы об дефолтную. Специфичная сужает
        // когда дефолтная пустая, а специфичная нет
        val result = validate(
            listOf(BOOKS_GOAL_ID),
            yndxFixPricePackage()
        )
        assertThat(result).`is`(matchedBy(hasNoDefectsDefinitions<Any>()))
    }

    @Test
    fun specificAdGroup_Goals_Ok() {
        // тесты на валидацию специфичной группы об дефолтную. Специфичная сужает
        // когда в специфичной часть категорий
        val result = validate(
            listOf(SPORT_GOAL_ID),
            pricePackage = yndxFixPricePackage(),
            defAdGroupContentCategoriesRules = scienceSportRules()
        )
        assertThat(result).`is`(matchedBy(hasNoDefectsDefinitions<Any>()))
    }

    @Test
    fun specificAdGroup_Goals_Error() {
        //тесты на валидацию специфичной группы об дефолтную. Специфичная шире
        val result = validate(
            listOf(SCIENCE_GOAL_ID, BOOKS_GOAL_ID),
            pricePackage = yndxFixPricePackage(),
            defAdGroupContentCategoriesRules = scienceSportRules()
        )
        assertThat(result).`is`(matchedBy(hasDefectDefinitionWith<Any>(
            validationError(path(index(0), field(Rule.GOALS)),
                RetargetingDefects.retargetingConditionIsInvalidByDefaultAdGroup()))))
    }

    @Test
    fun specificAdGroupEmpty_Error() {
        //тесты на валидацию специфичной группы об дефолтную. Специфичная пустая, значит шире
        val result = validate(
            listOf(),
            pricePackage = yndxFixPricePackage(),
            defAdGroupContentCategoriesRules = scienceSportRules()
        )
        assertThat(result).`is`(matchedBy(hasDefectDefinitionWith<Any>(
            validationError(path(index(0), field(Rule.GOALS)),
                RetargetingDefects.retargetingConditionIsInvalidByDefaultAdGroup()))))
    }

    //пакет видео. Разрешено Спорт, обязательна наука
    private fun pricePackage(): PricePackage {
        val pricePackage = TestPricePackages.approvedPricePackage()
            .withAvailableAdGroupTypes(setOf(AdGroupType.CPM_VIDEO))
            .withIsFrontpage(false);
        pricePackage.targetingsFixed
            .withCryptaSegments(listOf(SCIENCE_GOAL_ID))
        pricePackage.targetingsCustom
            .withRetargetingCondition(PriceRetargetingCondition()
                .withAllowAudienceSegments(true)
                .withAllowMetrikaSegments(false)
                .withLowerCryptaTypesCount(1)
                .withUpperCryptaTypesCount(3)
                .withCryptaSegments(listOf(SPORT_GOAL_ID)))
        return pricePackage
    }

    private fun yndxFixPricePackage(): PricePackage {
        return pricePackage()
            .withAvailableAdGroupTypes(setOf(AdGroupType.CPM_YNDX_FRONTPAGE))
    }

    private fun scienceSportRules(): List<Rule> {
        val rule = Rule()
            .withType(RuleType.OR)
            .withGoals(listOf(SCIENCE_GOAL_ID, SPORT_GOAL_ID).map { Goal().withId(it) as Goal })
        return (listOf(rule))
    }
}
