package ru.yandex.direct.web.entity.uac.campaign.service

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import ru.yandex.direct.core.entity.campaign.model.CampaignSource
import ru.yandex.direct.web.entity.campaign.service.CampaignGeneratedFlagService
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbDirectCampaign
import ru.yandex.direct.core.entity.uac.model.DirectCampaignStatus
import java.time.LocalDateTime

class WebCampaignServiceTest {
    private var campaignGeneratedFlagService: CampaignGeneratedFlagService? = null

    @Before
    fun before() {
        val uacYdbDirectCampaignRepository = FakeDirectCampaignRepository(
            mapOf(Pair(
                1L,
                UacYdbDirectCampaign("1", 1L, DirectCampaignStatus.DRAFT, LocalDateTime.now(), null)
            ))
        )
        val fakeUacCampaignService = FakeUacCampaignService(mapOf())

        campaignGeneratedFlagService = CampaignGeneratedFlagService(uacYdbDirectCampaignRepository, fakeUacCampaignService)
    }


    @Test
    fun isFalseForAllCantBeGeneratedSources() {
        for (source in CampaignSource.values()) {
            val result = campaignGeneratedFlagService!!.isGenerated(source, 1L)
            if (!result && !CANT_BE_GENERATED_SOURCES.contains(source)) {
                Assert.fail("CampaignSource $source may be generated?")
            }
        }
    }

    companion object {
        private val CANT_BE_GENERATED_SOURCES = setOf(
            CampaignSource.DIRECT,
            CampaignSource.USLUGI,
            CampaignSource.EDA,
            CampaignSource.GEO,
            CampaignSource.DC,
            CampaignSource.API,
            CampaignSource.XLS,
            CampaignSource.ZEN
        )
    }
}
