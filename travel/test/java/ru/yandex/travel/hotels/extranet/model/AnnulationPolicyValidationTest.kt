package ru.yandex.travel.hotels.extranet.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.yandex.travel.hotels.extranet.entities.AnnulationPenaltyType
import ru.yandex.travel.hotels.extranet.entities.AnnulationPolicy
import ru.yandex.travel.hotels.extranet.entities.AnnulationRule
import ru.yandex.travel.hotels.extranet.entities.AnnulationRuleStart
import ru.yandex.travel.hotels.extranet.entities.Organization
import java.math.BigDecimal
import javax.validation.Validation
import javax.validation.Validator

class AnnulationPolicyValidationTest {
    lateinit var validator: Validator

    @BeforeEach
    fun setUp() {
        validator = Validation.buildDefaultValidatorFactory().validator
    }

    @Test
    fun testInvalidAsNoRules() {
        val policy = AnnulationPolicy(Organization("test"), "foo", emptyList())
        val violations = validator.validate(policy)
        assertThat(violations).hasSize(1).extracting("message").containsExactly("Не указаны правила аннуляции")
    }

    @Test
    fun testInvalidAsDifferentPenaltyTypes() {
        val policy = AnnulationPolicy(
            Organization("test"), "foo",
            listOf(
                AnnulationRule(
                    penaltyNominal = BigDecimal.valueOf(10),
                    penaltyType = AnnulationPenaltyType.PERCENTAGE
                ),
                AnnulationRule(
                    start = AnnulationRuleStart(1, AnnulationRuleStart.Unit.DAYS),
                    penaltyNominal = BigDecimal.valueOf(1),
                    penaltyType = AnnulationPenaltyType.NIGHTS
                )
            )
        )
        val violations = validator.validate(policy)
        assertThat(violations).hasSize(1).extracting("message")
            .containsExactly("Разные типы штрафов в правилах одной политики")
    }

    @Test
    fun testInvalidAsNoAmountForPercentage() {
        val policy = AnnulationPolicy(
            Organization("test"), "foo",
            listOf(
                AnnulationRule(penaltyType = AnnulationPenaltyType.PERCENTAGE),
            )
        )
        val violations = validator.validate(policy)
        assertThat(violations).hasSize(1).extracting("message").containsExactly("Не указан размер штрафа")
    }

    @Test
    fun testInvalidAsIncorrectAmountForPercentage() {
        val policy = AnnulationPolicy(
            Organization("test"), "foo",
            listOf(
                AnnulationRule(
                    start = AnnulationRuleStart(0, AnnulationRuleStart.Unit.DAYS),
                    penaltyNominal = BigDecimal.valueOf(105),
                    penaltyType = AnnulationPenaltyType.PERCENTAGE
                ),
                AnnulationRule(
                    penaltyNominal = BigDecimal.valueOf(-10),
                    penaltyType = AnnulationPenaltyType.PERCENTAGE
                ),
                AnnulationRule(
                    start = AnnulationRuleStart(10, AnnulationRuleStart.Unit.DAYS),
                    penaltyNominal = BigDecimal.valueOf(15.4),
                    penaltyType = AnnulationPenaltyType.PERCENTAGE
                ),
            )
        )
        val violations = validator.validate(policy)
        assertThat(violations).hasSize(2).extracting("message").containsOnly("Некорректный размер штрафа")
    }

    @Test
    fun testInvalidAsNoAmountForNights() {
        val policy = AnnulationPolicy(
            Organization("test"), "foo",
            listOf(
                AnnulationRule(penaltyType = AnnulationPenaltyType.NIGHTS),
            )
        )
        val violations = validator.validate(policy)
        assertThat(violations).hasSize(1).extracting("message").containsExactly("Не указан размер штрафа")
    }

    @Test
    fun testInvalidAsIncorrectAmountForNights() {
        val policy = AnnulationPolicy(
            Organization("test"), "foo",
            listOf(
                AnnulationRule(
                    penaltyNominal = BigDecimal.valueOf(-2),
                    penaltyType = AnnulationPenaltyType.NIGHTS
                ),
                AnnulationRule(
                    start = AnnulationRuleStart(1, AnnulationRuleStart.Unit.DAYS),
                    penaltyNominal = BigDecimal.valueOf(5.4),
                    penaltyType = AnnulationPenaltyType.NIGHTS
                ),
            )
        )
        val violations = validator.validate(policy)
        assertThat(violations).hasSize(2).extracting("message")
            .containsOnly("Некорректное количество ночей", "Нецелое количество ночей")
    }

    @Test
    fun testInvalidAsNoAmountForAbsolute() {
        val policy = AnnulationPolicy(
            Organization("test"), "foo",
            listOf(
                AnnulationRule(penaltyType = AnnulationPenaltyType.FIXED),
            )
        )
        val violations = validator.validate(policy)
        assertThat(violations).hasSize(1).extracting("message").containsExactly("Не указан размер штрафа")
    }

    @Test
    fun testInvalidAsIncorrectSequence() {
        val policy = AnnulationPolicy(
            Organization("test"), "foo",
            listOf(
                AnnulationRule(
                    penaltyType = AnnulationPenaltyType.NIGHTS,
                    penaltyNominal = BigDecimal.valueOf(2)
                ),
                AnnulationRule(
                    start = AnnulationRuleStart(20, AnnulationRuleStart.Unit.HOURS),
                    penaltyType = AnnulationPenaltyType.NIGHTS,
                    penaltyNominal = BigDecimal.valueOf(1)
                ),
                AnnulationRule(
                    start = AnnulationRuleStart(10, AnnulationRuleStart.Unit.DAYS),
                    penaltyType = AnnulationPenaltyType.NIGHTS,
                    penaltyNominal = BigDecimal.valueOf(3)
                ),
            )
        )
        val violations = validator.validate(policy)
        assertThat(violations).hasSize(1).extracting("message")
            .containsExactly("Неравномерная последовательность штрафов")
    }

    @Test
    fun testInvalidAsSameTimeRules() {
        val policy = AnnulationPolicy(
            Organization("test"), "foo",
            listOf(
                AnnulationRule(
                    penaltyType = AnnulationPenaltyType.NIGHTS,
                    penaltyNominal = BigDecimal.valueOf(2)
                ),
                AnnulationRule(
                    penaltyType = AnnulationPenaltyType.NIGHTS,
                    penaltyNominal = BigDecimal.valueOf(1)
                )
            )
        )
        val violations = validator.validate(policy)
        assertThat(violations).hasSize(1).extracting("message")
            .containsExactly("Одинаковый момент начала у разных правил")
    }

    @Test
    fun testAllValidSorted() {
        val policy = AnnulationPolicy(
            Organization("test"), "foo",
            listOf(
                AnnulationRule(
                    penaltyType = AnnulationPenaltyType.NONE,
                ),
                AnnulationRule(
                    start = AnnulationRuleStart(10, AnnulationRuleStart.Unit.DAYS),
                    penaltyType = AnnulationPenaltyType.NIGHTS,
                    penaltyNominal = BigDecimal.valueOf(1)
                ),
                AnnulationRule(
                    start = AnnulationRuleStart(24, AnnulationRuleStart.Unit.HOURS),
                    penaltyType = AnnulationPenaltyType.NIGHTS,
                    penaltyNominal = BigDecimal.valueOf(2)
                ),
                AnnulationRule(
                    start = AnnulationRuleStart(2, AnnulationRuleStart.Unit.HOURS),
                    penaltyType = AnnulationPenaltyType.FULL,
                ),
            )
        )
        val violations = validator.validate(policy)
        assertThat(violations).isEmpty()
    }

    @Test
    fun testAllValidUnsorted() {
        val policy = AnnulationPolicy(
            Organization("test"), "foo",
            listOf(
                AnnulationRule(
                    start = AnnulationRuleStart(24, AnnulationRuleStart.Unit.HOURS),
                    penaltyType = AnnulationPenaltyType.PERCENTAGE,
                    penaltyNominal = BigDecimal.valueOf(50)
                ),
                AnnulationRule(
                    penaltyType = AnnulationPenaltyType.NONE,
                ),
                AnnulationRule(
                    start = AnnulationRuleStart(2, AnnulationRuleStart.Unit.HOURS),
                    penaltyType = AnnulationPenaltyType.FULL,
                ),
                AnnulationRule(
                    start = AnnulationRuleStart(10, AnnulationRuleStart.Unit.DAYS),
                    penaltyType = AnnulationPenaltyType.PERCENTAGE,
                    penaltyNominal = BigDecimal.valueOf(10)
                ),
            )
        )
        val violations = validator.validate(policy)
        assertThat(violations).isEmpty()
    }
}
