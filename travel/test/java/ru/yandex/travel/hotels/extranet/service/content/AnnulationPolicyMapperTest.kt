package ru.yandex.travel.hotels.extranet.service.content

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.yandex.travel.hotels.extranet.dto.AnnulationPolicyMoment
import ru.yandex.travel.hotels.extranet.dto.AnnulationRuleDTO
import ru.yandex.travel.hotels.extranet.dto.PenaltyType
import ru.yandex.travel.hotels.extranet.dto.TimeUnit
import ru.yandex.travel.hotels.extranet.entities.AnnulationPenaltyType
import ru.yandex.travel.hotels.extranet.entities.AnnulationPolicy
import ru.yandex.travel.hotels.extranet.entities.AnnulationRule
import ru.yandex.travel.hotels.extranet.entities.AnnulationRuleStart
import ru.yandex.travel.hotels.extranet.entities.Organization
import ru.yandex.travel.hotels.extranet.service.content.annulation.AnnulationPolicyMapper
import java.math.BigDecimal

class AnnulationPolicyMapperTest {
    private val annulationPolicyMapper = AnnulationPolicyMapper()

    @Test
    fun testMapWithOnlyEmptyRule() {
        val policy = AnnulationPolicy(
            Organization("test"),
            "test policy",
            listOf(
                AnnulationRule(penaltyType = AnnulationPenaltyType.FULL)
            )
        )
        val dto = annulationPolicyMapper.mapToDto(policy)
        assertThat(dto.id).isEqualTo(0)
        assertThat(dto.name).isEqualTo("test policy")
        assertThat(dto.rulesList).hasSize(1)
        assertThat(dto.getRules(0).hasStartsAt()).isFalse
        assertThat(dto.getRules(0).hasEndsAt()).isFalse
        assertThat(dto.getRules(0).hasPenaltyAmountFloat()).isFalse
        assertThat(dto.getRules(0).hasPenaltyAmountInt()).isFalse
        assertThat(dto.getRules(0).penaltyType).isEqualTo(PenaltyType.PENALTY_TYPE_FULL)
    }

    @Test
    fun testMapWithOnlyNightsRule() {
        val policy = AnnulationPolicy(
            Organization("test"),
            "test policy",
            listOf(
                AnnulationRule(
                    penaltyType = AnnulationPenaltyType.NIGHTS,
                    penaltyNominal = BigDecimal.valueOf(1)
                )
            )
        )
        val dto = annulationPolicyMapper.mapToDto(policy)
        assertThat(dto.id).isEqualTo(0)
        assertThat(dto.name).isEqualTo("test policy")
        assertThat(dto.rulesList).hasSize(1)
        assertThat(dto.getRules(0).hasStartsAt()).isFalse
        assertThat(dto.getRules(0).hasEndsAt()).isFalse
        assertThat(dto.getRules(0).hasPenaltyAmountFloat()).isFalse
        assertThat(dto.getRules(0).hasPenaltyAmountInt()).isTrue
        assertThat(dto.getRules(0).penaltyType).isEqualTo(PenaltyType.PENALTY_TYPE_NIGHTS)
        assertThat(dto.getRules(0).penaltyAmountInt).isEqualTo(1)
    }

    @Test
    fun testCorrectEndsAt() {
        val policy = AnnulationPolicy(
            Organization("test"),
            "test policy",
            listOf(
                AnnulationRule(
                    AnnulationPenaltyType.FULL,
                    start = AnnulationRuleStart(2, AnnulationRuleStart.Unit.HOURS)
                ),
                AnnulationRule(
                    penaltyType = AnnulationPenaltyType.NIGHTS,
                    penaltyNominal = BigDecimal.valueOf(2),
                    start = AnnulationRuleStart(5, AnnulationRuleStart.Unit.DAYS)
                ),
                AnnulationRule(
                    penaltyType = AnnulationPenaltyType.NIGHTS,
                    penaltyNominal = BigDecimal.valueOf(1),
                    start = AnnulationRuleStart(10, AnnulationRuleStart.Unit.DAYS)
                ),
                AnnulationRule(
                    AnnulationPenaltyType.NONE
                )
            )
        )
        val dto = annulationPolicyMapper.mapToDto(policy)
        assertThat(dto.rulesList).hasSize(4)
        for (i in 0..2) {
            assertThat(dto.getRules(i).endsAt).isEqualTo(dto.getRules(i + 1).startsAt)
        }
        assertThat(dto.getRules(3).hasEndsAt()).isFalse

        assertThat(dto.getRules(0).penaltyType).isEqualTo(PenaltyType.PENALTY_TYPE_NONE)
        assertThat(dto.getRules(0).hasPenaltyAmountInt()).isFalse
        assertThat(dto.getRules(1).penaltyType).isEqualTo(PenaltyType.PENALTY_TYPE_NIGHTS)
        assertThat(dto.getRules(1).hasPenaltyAmountInt()).isTrue
        assertThat(dto.getRules(1).penaltyAmountInt).isEqualTo(1)
        assertThat(dto.getRules(2).penaltyType).isEqualTo(PenaltyType.PENALTY_TYPE_NIGHTS)
        assertThat(dto.getRules(2).hasPenaltyAmountInt()).isTrue
        assertThat(dto.getRules(2).penaltyAmountInt).isEqualTo(2)
        assertThat(dto.getRules(3).penaltyType).isEqualTo(PenaltyType.PENALTY_TYPE_FULL)
        assertThat(dto.getRules(3).hasPenaltyAmountInt()).isFalse
    }

    @Test
    fun testFloatingPointValues() {
        val policy = AnnulationPolicy(
            Organization("test"),
            "test policy",
            listOf(
                AnnulationRule(
                    penaltyType = AnnulationPenaltyType.PERCENTAGE,
                    penaltyNominal = BigDecimal.valueOf(10)
                ),
                AnnulationRule(
                    penaltyType = AnnulationPenaltyType.PERCENTAGE,
                    penaltyNominal = BigDecimal.valueOf(33.33)
                )
            )
        )
        val dto = annulationPolicyMapper.mapToDto(policy)
        assertThat(dto.rulesList).hasSize(2)
        assertThat(dto.getRules(0).hasPenaltyAmountFloat()).isTrue
        assertThat(dto.getRules(0).hasPenaltyAmountInt()).isFalse
        assertThat(dto.getRules(1).hasPenaltyAmountFloat()).isTrue
        assertThat(dto.getRules(1).hasPenaltyAmountInt()).isFalse
    }

    @Test
    fun testMapRuleFromDto() {
        val dto = AnnulationRuleDTO.newBuilder()
            .setPenaltyType(PenaltyType.PENALTY_TYPE_NIGHTS)
            .setPenaltyAmountInt(1)
            .setStartsAt(
                AnnulationPolicyMoment.newBuilder()
                    .setAmount(10)
                    .setUnits(TimeUnit.TIME_UNIT_DAY)
                    .build()
            )
            .build()
        val rule = annulationPolicyMapper.mapRuleFromDto(dto)
        assertThat(rule.penaltyNominal).isEqualByComparingTo("1")
        assertThat(rule.penaltyType).isEqualTo(AnnulationPenaltyType.NIGHTS)
        assertThat(rule.start).isNotNull.matches { it?.amount == 10 && it.unit == AnnulationRuleStart.Unit.DAYS }
    }
}
