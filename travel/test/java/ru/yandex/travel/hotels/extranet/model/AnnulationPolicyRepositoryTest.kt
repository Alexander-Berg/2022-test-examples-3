package ru.yandex.travel.hotels.extranet.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import ru.yandex.travel.hotels.extranet.entities.AnnulationPenaltyType
import ru.yandex.travel.hotels.extranet.entities.AnnulationPolicy
import ru.yandex.travel.hotels.extranet.entities.AnnulationRule
import ru.yandex.travel.hotels.extranet.entities.Organization
import ru.yandex.travel.hotels.extranet.repository.AnnulationPolicyRepository
import ru.yandex.travel.hotels.extranet.repository.OrganizationRepository
import java.math.BigDecimal
import java.util.UUID
import javax.persistence.EntityManager

@DataJpaTest
class AnnulationPolicyRepositoryTest {
    @Autowired
    lateinit var repository: AnnulationPolicyRepository

    @Autowired
    lateinit var orgRepository: OrganizationRepository

    @Autowired
    lateinit var em: EntityManager

    lateinit var ruleRepository: JpaRepository<AnnulationRule, UUID>

    lateinit var organization: Organization

    @BeforeEach
    fun setUp() {
        organization = orgRepository.saveAndFlush(Organization("test"))
        ruleRepository = SimpleJpaRepository<AnnulationRule, UUID>(AnnulationRule::class.java, em)
    }

    @Test
    fun testCreateGetListDelete() {
        var p = AnnulationPolicy(
            organization, "foo",
            listOf(
                AnnulationRule(
                    penaltyNominal = BigDecimal.valueOf(100L),
                    penaltyType = AnnulationPenaltyType.PERCENTAGE
                )
            )
        )
        val id = repository.saveAndFlush(p).id!!
        val res = repository.getOne(id)
        assertThat(res).isNotNull
        assertThat(res.id).isNotNull.isGreaterThan(0)
        assertThat(res.name).isEqualTo("foo")
        assertThat(res.rules).hasSize(1)

        assertThat(repository.findAllByOrganizationIdAndDeletedFalse(organizationId = organization.id)).hasSize(1)
        res.deleted = true
        repository.flush()
        assertThat(repository.findAllByOrganizationIdAndDeletedFalse(organizationId = organization.id)).isEmpty()
    }

    @Test
    fun testOrphanRemoval() {
        var p = AnnulationPolicy(
            organization, "foo",
            listOf(
                AnnulationRule(
                    penaltyNominal = BigDecimal.valueOf(100L),
                    penaltyType = AnnulationPenaltyType.PERCENTAGE
                )
            )
        )
        val res = repository.saveAndFlush(p)

        assertThat(ruleRepository.count()).isEqualTo(1)
        res.rules.clear()
        repository.flush()
        assertThat(ruleRepository.count()).isZero
    }
}
