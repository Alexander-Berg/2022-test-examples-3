package ru.yandex.market.wms.constraints.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test

import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.wms.constraints.config.ConstraintsIntegrationTest
import ru.yandex.market.wms.constraints.core.domain.ConstraintsIssueStatus
import ru.yandex.market.wms.constraints.core.domain.RuleRestrictionType
import ru.yandex.market.wms.constraints.core.dto.SkuIdDto

internal class ConstraintsIssueServiceTest : ConstraintsIntegrationTest() {
    @Autowired
    private lateinit var constraintsIssueService: ConstraintsIssueService

    @Test
    @DatabaseSetup("/service/processor/before.xml")
    @ExpectedDatabase(
        value = "/service/processor/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateIssueStatus_forEmptySkuList() {
        constraintsIssueService.updateStatusBySkuIds(
            listOf(),
            RuleRestrictionType.DIMENSION,
            ConstraintsIssueStatus.FIXED,
            setOf()
        )
    }

    @Test
    @DatabaseSetup("/service/processor/before.xml")
    @ExpectedDatabase(
        value = "/service/processor/without-specific-from-status/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateIssueStatus_withEmptyFromStatus() {
        constraintsIssueService.updateStatusBySkuIds(
            listOf(
                SkuIdDto("465852", "ROV001"),
                SkuIdDto("465852", "ROV002"),
                SkuIdDto("465852", "ROV003"),
                SkuIdDto("465852", "ROV004"),
                SkuIdDto("465852", "ROV005"),
                SkuIdDto("465852", "ROV006")
            ),
            RuleRestrictionType.DIMENSION,
            ConstraintsIssueStatus.FIXED,
            setOf()
        )
    }

    @Test
    @DatabaseSetup("/service/processor/before.xml")
    @ExpectedDatabase(
        value = "/service/processor/with-specific-from-status/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateIssueStatus_withSpecificFromStatus() {
        constraintsIssueService.updateStatusBySkuIds(
            listOf(
                SkuIdDto("465852", "ROV001"),
                SkuIdDto("465852", "ROV002"),
                SkuIdDto("465852", "ROV003")
            ),
            RuleRestrictionType.DIMENSION,
            ConstraintsIssueStatus.FIXED,
            setOf(ConstraintsIssueStatus.NEW)
        )
    }

    @Test
    @DatabaseSetup("/service/processor/before.xml")
    @ExpectedDatabase(
        value = "/service/processor/with-several-from-status/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateIssueStatus_withSeveralFromStatus() {
        constraintsIssueService.updateStatusBySkuIds(
            listOf(
                SkuIdDto("465852", "ROV001"),
                SkuIdDto("465852", "ROV002"),
                SkuIdDto("465852", "ROV003")
            ),
            RuleRestrictionType.DIMENSION,
            ConstraintsIssueStatus.FIXED,
            setOf(ConstraintsIssueStatus.NEW, ConstraintsIssueStatus.PROCESSED)
        )
    }
}
