package ru.yandex.market.wms.constraints.dao

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.wms.common.spring.dao.entity.SkuId
import ru.yandex.market.wms.constraints.config.ConstraintsIntegrationTest
import ru.yandex.market.wms.constraints.core.domain.ConstraintsIssueStatus.NEW
import ru.yandex.market.wms.constraints.core.domain.RuleRestrictionType.DIMENSION
import ru.yandex.market.wms.constraints.dao.entity.ConstraintsIssue
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ConstraintsIssueDaoTest : ConstraintsIntegrationTest() {

    @Autowired
    private lateinit var underTest: ConstraintsIssueDao

    @Test
    @DatabaseSetup("/dao/issues/db.xml")
    fun `should parse input skuid list correctly`() {
        val skuIds = listOf(
            SkuId("465852", "ROV000897823"),
            SkuId("123456", "ROV000100011"),
        )
        val issues = underTest.getActiveIssuesBySku(skuIds)
        assertThat(issues.size).isEqualTo(2)
        assertThat(issues).containsExactlyInAnyOrder(
            ConstraintsIssue(
                id = 1,
                sku = "ROV000897823",
                storerKey = "465852",
                loc = "aLoc1",
                type = DIMENSION,
                status = NEW,
                value = "",
                addDate = LocalDateTime.parse("2021-01-01T15:00:00", DateTimeFormatter.ISO_DATE_TIME),
                addWho = "TEST"
            ),
            ConstraintsIssue(
                id = 2,
                sku = "ROV000100011",
                storerKey = "123456",
                loc = "aLoc2",
                type = DIMENSION,
                status = NEW,
                value = "",
                addDate = LocalDateTime.parse("2021-01-01T15:00:00", DateTimeFormatter.ISO_DATE_TIME),
                addWho = "TEST"
            ),
        )
    }
}
