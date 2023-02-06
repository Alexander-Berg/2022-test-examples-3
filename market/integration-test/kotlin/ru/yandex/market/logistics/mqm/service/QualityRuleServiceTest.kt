package ru.yandex.market.logistics.mqm.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest

@DisplayName("Тест сервиса работы с правилами обработки план-фактов")
class QualityRuleServiceTest : AbstractContextualTest() {
    @Autowired
    private lateinit var qualityRuleService: QualityRuleService

    @Test
    @DisplayName("Валидное содержание json (без агрегации)")
    @DatabaseSetup("/service/quality_rule_service/valid_single.xml")
    fun validSituationTest() {
        qualityRuleService.findActiveSingleEventRules()
            .shouldNotBeEmpty()
            .forAll {
                it.rule shouldNotBe null
            }
    }

    @Test
    @DisplayName("Невалидное содержание json (без егрегации)")
    @DatabaseSetup("/service/quality_rule_service/non_valid_single.xml")
    fun nonValidSituationTest() {
        qualityRuleService.findActiveSingleEventRules().shouldBeEmpty()
    }

    @Test
    @DisplayName("Валидное содержание json (с агрегацией)")
    @DatabaseSetup("/service/quality_rule_service/valid_aggregation.xml")
    fun validSituationInAggregationTest() {
        qualityRuleService.findActiveRulesWithAggregation()
            .shouldNotBeEmpty()
            .forAll {
                it.rule shouldNotBe null
            }
    }

    @Test
    @DisplayName("Валидное содержание json (с агрегацией)")
    @DatabaseSetup("/service/quality_rule_service/non_valid_aggregation.xml")
    fun nonValidSituationInAggregationTest() {
        qualityRuleService.findActiveSingleEventRules().shouldBeEmpty()
    }

}
