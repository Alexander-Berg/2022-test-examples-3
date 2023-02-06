package ru.yandex.market.abo.tms.ticket

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import ru.yandex.EmptyTest
import ru.yandex.market.abo.core.regiongroup.RegionGroupManager
import ru.yandex.market.abo.core.regiongroup.model.AboRegionGroup
import ru.yandex.market.abo.core.regiongroup.model.AboRegionGroupStatus
import ru.yandex.market.abo.core.regiongroup.service.RegionGroupRepo
import ru.yandex.market.abo.core.regiongroup.service.RegionGroupService
import ru.yandex.market.abo.core.screenshot.ScreenshotService
import ru.yandex.market.abo.core.shop.ShopInfoService
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketManager
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketService
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.RegionGroupDto

class RegionGroupProcessorTest @Autowired constructor(
    recheckTicketService: RecheckTicketService,
    recheckTicketManager: RecheckTicketManager,
    regionGroupService: RegionGroupService,
    val regionGroupRepo: RegionGroupRepo,
    transactionTemplate: TransactionTemplate,
) : EmptyTest() {

    private val shopInfoService: ShopInfoService = mock()
    private val screenshotService: ScreenshotService = mock()
    private val regionGroupManager: RegionGroupManager = mock()

    private val regionGroupProcessor = RegionGroupProcessor(
        recheckTicketManager,
        recheckTicketService,
        regionGroupManager,
        regionGroupService,
        shopInfoService, screenshotService, transactionTemplate
    )

    val REG_GROUP_ID = 1L

    @BeforeEach
    fun prepare() {
        regionGroupRepo.save(AboRegionGroup(REG_GROUP_ID, 2L, "name", true, null)
            .apply {
                regionGroupStatus = AboRegionGroupStatus.IN_PROGRESS
            })
    }

    @Test
    fun cancelledDisableRegionGroupsNotInTarifficator() {
        doReturn(listOf<RegionGroupDto>()).whenever(regionGroupManager).getActiveRegionGroups(anyLong(), anyList())

        regionGroupProcessor.cancelledDisableRegionGroups()
        assertTrue(regionGroupRepo.findAllByStatus(AboRegionGroupStatus.CANCELLED).isNotEmpty())
    }

    @Test
    fun cancelledDisableRegionGroupsSameInTarifficator() {
        var tarifficatorRegionGroup: RegionGroupDto = mock()
        whenever(tarifficatorRegionGroup.id).thenReturn(REG_GROUP_ID)
        doReturn(listOf(tarifficatorRegionGroup)).whenever(regionGroupManager).getActiveRegionGroups(anyLong(), anyList())

        regionGroupProcessor.cancelledDisableRegionGroups()
        assertTrue(regionGroupRepo.findAllByStatus(AboRegionGroupStatus.CANCELLED).isEmpty())
    }
}
