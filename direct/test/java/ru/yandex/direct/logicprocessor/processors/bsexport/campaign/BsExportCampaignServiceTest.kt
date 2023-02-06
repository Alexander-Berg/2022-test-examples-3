package ru.yandex.direct.logicprocessor.processors.bsexport.campaign

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.yandex.direct.bstransport.yt.repository.campaign.CampaignDeleteYtRepository
import ru.yandex.direct.bstransport.yt.repository.campaign.CampaignYtRepository
import ru.yandex.direct.bstransport.yt.utils.CaesarIterIdGenerator
import ru.yandex.direct.common.log.service.LogBsExportEssService
import ru.yandex.direct.core.entity.bs.common.service.BsOrderIdCalculator
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign
import ru.yandex.direct.ess.logicobjects.bsexport.campaing.BsExportCampaignObject
import ru.yandex.direct.ess.logicobjects.bsexport.campaing.CampaignAdditionalInfo
import ru.yandex.direct.ess.logicobjects.bsexport.campaing.CampaignResourceType
import ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler.CampaignAllowedOnAdultContentHandler
import ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler.CampaignDeleteHandler
import ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler.CampaignMinusPhrasesHandler
import ru.yandex.direct.logicprocessor.processors.bsexport.campaign.utils.BsExportCampaignOrderIdLoader
import ru.yandex.direct.logicprocessor.processors.bsexport.utils.SupportedCampaignsService

class BsExportCampaignServiceTest {
    private val logBsExportEssService = mock<LogBsExportEssService>()
    private val caesarIterIdGenerator = mock<CaesarIterIdGenerator> {
        on(it.generateCaesarIterId()) doReturn 123
    }
    private val ytRepository = mock<CampaignYtRepository>()
    private val ytDeleteRepository = mock<CampaignDeleteYtRepository>()
    private val allowedOnAdultContentHandler = spy(CampaignAllowedOnAdultContentHandler())
    private val minusPhrasesHandler = spy(CampaignMinusPhrasesHandler())
    private val deleteHandler = mock<CampaignDeleteHandler> { mock ->
        on(mock.getDeletedCampaignIds(any(), any())).then {
            it.getArgument<Collection<BsExportCampaignObject>>(1)
                .mapTo(hashSetOf(), BsExportCampaignObject::getCid)
        }
    }
    private val bsOrderIdCalculator = mock<BsOrderIdCalculator> { mock ->
        on(mock.calculateOrderId(any())).thenCallRealMethod()
        on(mock.calculateOrderIdIfNotExist(any(), any())).then {
            it.getArgument<Collection<Long>>(1)
                .associateWith { cid -> cid * 10 }
        }
    }
    private val campaignOrderIdLoader = spy(BsExportCampaignOrderIdLoader(bsOrderIdCalculator))
    private val supportedCampaignsLoader = mock<SupportedCampaignsService> { repository ->
        on(repository.getCampaignsByIdTyped(any(), any()))
            .then {
                val cids = it.getArgument<Collection<Long>>(1)
                cids.associateWith { cid ->
                    DynamicCampaign()
                        .withId(cid)
                        .withIsAllowedOnAdultContent(true)
                        .withMinusKeywords(listOf("а"))
                }
            }
    }
    private val service = BsExportCampaignService(
        logBsExportEssService,
        caesarIterIdGenerator,
        ytRepository,
        ytDeleteRepository,
        Clock.fixed(Instant.ofEpochSecond(1586345827L), ZoneOffset.UTC), // 2020-04-08,
        resourceHandlers = listOf(allowedOnAdultContentHandler, minusPhrasesHandler),
        deleteHandler,
        campaignOrderIdLoader,
        supportedCampaignsLoader,
    )

    @Test
    fun `object with unknown or null type is not processed`() {
        val unknownObject = createLogicObject(cid = 13, type = CampaignResourceType.UNKNOWN)
        val nullTypeObject = createLogicObject(cid = 13, type = null)
        service.processCampaigns(1, listOf(unknownObject, nullTypeObject))
        verify(allowedOnAdultContentHandler, never()).getCampaignsIdsToLoad(any(), any())
        verify(minusPhrasesHandler, never()).getCampaignsIdsToLoad(any(), any())
        verify(deleteHandler, never()).getDeletedCampaignIds(any(), any())
        verify(ytRepository, never()).modify(any())
        verify(ytDeleteRepository, never()).modify(any())
    }

    @Test
    fun `object with DELETE type is processed only by deletion handler`() {
        val deleteObject = createLogicObject(cid = 13, type = CampaignResourceType.CAMPAIGN_DELETE)
        service.processCampaigns(1, listOf(deleteObject))
        verify(deleteHandler).getDeletedCampaignIds(any(), any())
        verify(minusPhrasesHandler, never()).getCampaignsIdsToLoad(any(), any())
        verify(minusPhrasesHandler, never()).handle(any(), any())
        verify(ytRepository, never()).modify(any())
        verify(ytDeleteRepository).modify(any())
    }

    @Test
    fun `object with ALL type is processed by all handlers`() {
        val allObject = createLogicObject(cid = 13, type = CampaignResourceType.ALL)
        service.processCampaigns(1, listOf(allObject))
        verify(minusPhrasesHandler).getCampaignsIdsToLoad(any(), any())
        verify(minusPhrasesHandler).handle(any(), any())
        verify(allowedOnAdultContentHandler).getCampaignsIdsToLoad(any(), any())
        verify(allowedOnAdultContentHandler).handle(any(), any())
        verify(ytRepository).modify(check {
            assertThat(it).hasSize(1)
            assertThat(it.first().hasMinusPhrases())
            assertThat(it.first().hasAllowedOnAdultContent())
        })
        verify(ytDeleteRepository, never()).modify(any())
    }

    @Test
    fun `duplicated campaign is processed only once`() {
        val objects = listOf(
            createLogicObject(cid = 13, type = CampaignResourceType.CAMPAIGN_MINUS_PHRASES),
            createLogicObject(cid = 13, type = CampaignResourceType.ALL)
        )
        service.processCampaigns(1, objects)
        verify(minusPhrasesHandler).handle(any(), check { assertThat(it).hasSize(1) })
    }

    @Test
    fun `non-existing campaign is not processed`() {
        whenever(supportedCampaignsLoader.getCampaignsByIdTyped(any(), any())) doReturn mapOf()
        val nonExistingObject = createLogicObject(cid = 13, type = CampaignResourceType.CAMPAIGN_MINUS_PHRASES)
        service.processCampaigns(1, listOf(nonExistingObject))
        verify(minusPhrasesHandler).getCampaignsIdsToLoad(any(), any())
        verify(minusPhrasesHandler, never()).handle(any(), any())
        verify(ytRepository, never()).modify(any())
    }

    @Test
    fun `required proto fields are filled`() {
        val logicObject = createLogicObject(cid = 13, type = CampaignResourceType.ALL)
        service.processCampaigns(1, listOf(logicObject))
        verify(campaignOrderIdLoader).getOrderIdForExistingCampaigns(
            any(),
            requestedCampaigns = check { assertThat(it).containsExactly(13) },
            knownCampaignsObjects = check { assertThat(it).containsExactly(logicObject) },
        )
        verify(ytRepository).modify(check {
            val proto = it.first()
            assertThat(proto.hasExportId())
            assertThat(proto.hasOrderId())
            assertThat(proto.hasUpdateTime())
            assertThat(proto.hasIterId())
        })
    }

    @Test
    fun `campaign being processed was deleted`() {
        doReturn(mapOf<Long, Long>())
            .whenever(campaignOrderIdLoader)
            .getOrderIdForExistingCampaigns(any(), any(), any())
        val logicObject = createLogicObject(cid = 13, type = CampaignResourceType.ALL)
        service.processCampaigns(1, listOf(logicObject))
        verify(minusPhrasesHandler).getCampaignsIdsToLoad(any(), any())
        verify(minusPhrasesHandler, never()).handle(any(), any())
        verify(ytRepository, never()).modify(any())
    }
}

private fun createLogicObject(
    cid: Long? = null,
    orderId: Long? = null,
    type: CampaignResourceType? = null,
    additionalInfo: CampaignAdditionalInfo? = null,
): BsExportCampaignObject = BsExportCampaignObject.Builder().run {
    setCid(cid)
    setOrderId(orderId)
    setAdditionalInfo(additionalInfo)
    setCampaignResourceType(type)
    build()
}
