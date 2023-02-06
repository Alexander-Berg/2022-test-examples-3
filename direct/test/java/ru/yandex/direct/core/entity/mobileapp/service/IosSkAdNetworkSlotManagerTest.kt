package ru.yandex.direct.core.entity.mobileapp.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.mobileapp.model.SkAdNetworkSlot
import ru.yandex.direct.core.entity.mobileapp.repository.IosSkAdNetworkSlotRepository
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import java.util.UUID

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class IosSkAdNetworkSlotManagerTest {
    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var mobileAppService: MobileAppService

    @Autowired
    private lateinit var iosSkAdNetworkSlotRepository: IosSkAdNetworkSlotRepository

    private lateinit var grutSkadNetworkSlotService: GrutSkadNetworkSlotService

    private lateinit var slotManager: IosSkAdNetworkSlotManager

    private lateinit var config: SkAdNetworkSlotsConfig

    @Before
    fun before() {
        grutSkadNetworkSlotService = mock(GrutSkadNetworkSlotService::class.java)
        config = SkAdNetworkSlotsConfig(1, 1)
        slotManager = IosSkAdNetworkSlotManager(
            dslContextProvider, mobileAppService, { config }, iosSkAdNetworkSlotRepository, grutSkadNetworkSlotService
        )
    }

    @Test
    fun applicationHasVacantSlot_Free() {
        val bundleId = createBundleId()
        val hasVacantSlot = slotManager.applicationHasVacantSlot(bundleId)
        assertThat(hasVacantSlot).isTrue()
    }

    @Test
    fun applicationHasVacantSlot_NoFree() {
        val bundleId = createBundleId()
        slotManager.allocateCampaignSlot(bundleId, 100001)
        val hasVacantSlot = slotManager.applicationHasVacantSlot(bundleId)
        assertThat(hasVacantSlot).isFalse()
    }

    @Test
    fun allocateCampaignSlot_One() {
        val bundleId = createBundleId()
        val allocateCampaignSlot = slotManager.allocateCampaignSlot(bundleId, 100002)
        assertThat(allocateCampaignSlot).isEqualTo(1)
    }

    @Test
    fun allocateCampaignSlot_Three() {
        config = SkAdNetworkSlotsConfig(3, 1)
        val bundleId = createBundleId()
        val allocateCampaignSlot = mutableSetOf<Int>()
        allocateCampaignSlot.add(slotManager.allocateCampaignSlot(bundleId, 1001)!!)
        allocateCampaignSlot.add(slotManager.allocateCampaignSlot(bundleId, 1002)!!)
        allocateCampaignSlot.add(slotManager.allocateCampaignSlot(bundleId, 1003)!!)

        assertThat(slotManager.allocateCampaignSlot(bundleId, 1004)).isNull()
        assertThat(allocateCampaignSlot).containsExactlyInAnyOrder(1, 2, 3)
    }

    @Test
    fun getAllocatedSlotsByBundleIds_twoAllocatedSlots() {
        val bundleId1 = createBundleId()
        val bundleId2 = createBundleId()
        val campaignId1: Long = 300001
        val campaignId2: Long = 300002
        slotManager.allocateCampaignSlot(bundleId1, campaignId1)
        slotManager.allocateCampaignSlot(bundleId2, campaignId2)
        val slots = slotManager.getAllocatedSlotsByCampaignIds(listOf(campaignId1, campaignId2))
        assertThat(slots).containsExactlyInAnyOrder(
            SkAdNetworkSlot(bundleId1, campaignId1, 1),
            SkAdNetworkSlot(bundleId2, campaignId2, 1),
        )
    }

    private fun createBundleId(): String = "ru.yandex.super.puper." + UUID.randomUUID().toString()
}
