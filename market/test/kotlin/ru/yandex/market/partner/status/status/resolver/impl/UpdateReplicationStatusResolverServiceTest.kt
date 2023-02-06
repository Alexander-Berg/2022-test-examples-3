package ru.yandex.market.partner.status.status.resolver.impl

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.partner.status.AbstractFunctionalTest
import ru.yandex.market.partner.status.status.resolver.impl.replication.UpdateReplicationStatusResolverService

class UpdateReplicationStatusResolverServiceTest : AbstractFunctionalTest() {
    @Autowired
    lateinit var updateReplicationStatusResolverService: UpdateReplicationStatusResolverService

    @Test
    @DbUnitDataSet(after = ["updateReplicationStatusResolverServiceTest/empty.after.csv"])
    fun `empty partnerIds list`() {
        updateReplicationStatusResolverService.updateForPartners(emptyList())
    }

    @Test
    @DbUnitDataSet(
        before = ["updateReplicationStatusResolverServiceTest/replication_in_progress.before.csv"],
        after = ["updateReplicationStatusResolverServiceTest/replication_in_progress.after.csv"])
    fun `partnerId in replication process`() {
        updateReplicationStatusResolverService.updateForPartners(listOf(999L))
    }
}
