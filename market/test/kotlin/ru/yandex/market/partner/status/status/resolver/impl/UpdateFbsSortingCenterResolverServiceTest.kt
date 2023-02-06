package ru.yandex.market.partner.status.status.resolver.impl

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.partner.status.AbstractFunctionalTest
import ru.yandex.market.partner.status.status.resolver.impl.sortcenter.UpdateFbsSortingCenterResolverService

/**
 * Тесты для [UpdateFbsSortingCenterResolverService].
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class UpdateFbsSortingCenterResolverServiceTest : AbstractFunctionalTest() {

    @Autowired
    private lateinit var updateFbsSortingCenterResolverService: UpdateFbsSortingCenterResolverService

    @Test
    @DbUnitDataSet(after = ["UpdateFbsSortingCenterResolverServiceTest/empty.after.csv"])
    fun `empty partnerIds list`() {
        updateFbsSortingCenterResolverService.updateForPartners(emptyList())
    }

    @Test
    @DbUnitDataSet(
        before = ["UpdateFbsSortingCenterResolverServiceTest/withoutLink.before.csv"],
        after = ["UpdateFbsSortingCenterResolverServiceTest/empty.after.csv"]
    )
    fun `partner with not ready data`() {
        updateFbsSortingCenterResolverService.updateForPartners(listOf(100L))
    }

    @Test
    @DbUnitDataSet(
        before = ["UpdateFbsSortingCenterResolverServiceTest/withoutSegment.before.csv"],
        after = ["UpdateFbsSortingCenterResolverServiceTest/failedStatus.after.csv"]
    )
    fun `failed status`() {
        updateFbsSortingCenterResolverService.updateForPartners(listOf(100L))
    }

    @Test
    @DbUnitDataSet(
        before = [
            "UpdateFbsSortingCenterResolverServiceTest/withoutLink.before.csv",
            "UpdateFbsSortingCenterResolverServiceTest/withoutSegment.before.csv"
        ],
        after = ["UpdateFbsSortingCenterResolverServiceTest/successStatus.after.csv"]
    )
    fun `ok status`() {
        updateFbsSortingCenterResolverService.updateForPartners(listOf(100L))
    }

    @Test
    @DbUnitDataSet(
        before = [
            "UpdateFbsSortingCenterResolverServiceTest/withoutLink.before.csv",
            "UpdateFbsSortingCenterResolverServiceTest/withoutSegment.before.csv",
            "UpdateFbsSortingCenterResolverServiceTest/statusFromFuture.before.csv"
        ],
        after = ["UpdateFbsSortingCenterResolverServiceTest/statusFromFuture.before.csv"]
    )
    fun `do not save outdated status`() {
        updateFbsSortingCenterResolverService.updateForPartners(listOf(100L))
    }
}
