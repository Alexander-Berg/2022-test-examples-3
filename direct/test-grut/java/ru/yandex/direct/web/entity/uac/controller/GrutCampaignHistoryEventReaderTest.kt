package ru.yandex.direct.web.entity.uac.controller

import java.time.LocalDateTime
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.entity.uac.service.GrutUacCampaignService
import ru.yandex.direct.core.entity.uac.service.GrutUacCampaignUpdateService
import ru.yandex.direct.core.grut.model.GrutHistoryEventEntry
import ru.yandex.direct.core.grut.model.GrutHistoryEventType
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.model.KtModelChanges
import ru.yandex.direct.regions.Region
import ru.yandex.direct.web.configuration.GrutDirectWebTest
import ru.yandex.direct.web.entity.uac.model.history.GrutHistoryOutputCategory
import ru.yandex.direct.web.entity.uac.model.history.event.GrutCampaignHistoryEvent
import ru.yandex.direct.web.entity.uac.service.history.GrutCampaignHistoryEventReader
import ru.yandex.direct.web.entity.uac.service.history.NotDeletedGrutHistoryPeekingIterator

@GrutDirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class GrutCampaignHistoryEventReaderTest {

    @Autowired
    private lateinit var grutCampaignHistoryEventReader: GrutCampaignHistoryEventReader

    @Autowired
    private lateinit var grutSteps: GrutSteps

    @Autowired
    private lateinit var uacCampaignService: GrutUacCampaignService

    @Autowired
    private lateinit var uacUpdateCampaignService: GrutUacCampaignUpdateService

    @Autowired
    private lateinit var steps: Steps

    private val changeList = mutableListOf<GrutHistoryEventEntry<UacYdbCampaign>>()
    private lateinit var clientInfo: ClientInfo

    companion object {
        private const val NEW_URL: String = "newUrl"
        private val REGIONS = listOf(Region.MOSCOW_REGION_ID, Region.SAINT_PETERSBURG_REGION_ID)
    }

    @Before
    fun init() {
        clientInfo = steps.clientSteps().createDefaultClient()
        grutSteps.createClient(clientInfo)
    }

    @Test
    fun grutCampaignHistoryEventReaderTest_generalFlow() {
        val changeList = getCampaignHistories()

        val result = grutCampaignHistoryEventReader.getTypedHistoryEvents(changeList)
        SoftAssertions().apply {
            assertThat(result.size)
                .`as`("Количество изменений")
                .isEqualTo(3)
            assertThat(result.map { it.timestamp })
                .`as`("Проверка времени возникновения изменений")
                .containsExactly(
                    UacYdbUtils.toEpochSecond(changeList[1].value.updatedAt),
                    UacYdbUtils.toEpochSecond(changeList[1].value.updatedAt),
                    UacYdbUtils.toEpochSecond(changeList[2].value.updatedAt)
                )
            for (i in 0..1) {
                if (result[i].grutHistoryEvent.getType() == GrutHistoryOutputCategory.CAMPAIGN_ADULT_CONTENT) {
                    assertThat(result[i].grutHistoryEvent)
                        .`as`("Проверка содержимого изменений (изменение показа взрослого контента)")
                        .isEqualTo(GrutCampaignHistoryEvent(
                            changeList[0].value.adultContentEnabled,
                            true,
                            GrutHistoryOutputCategory.CAMPAIGN_ADULT_CONTENT,
                            changeList[0].value.account,
                            changeList[0].value.id))
                } else {
                    assertThat(result[i].grutHistoryEvent)
                        .`as`("Проверка содержимого изменений (изменение трекинговой ссылки)")
                        .isEqualTo(GrutCampaignHistoryEvent(
                            changeList[0].value.trackingUrl,
                            NEW_URL,
                            GrutHistoryOutputCategory.CAMPAIGN_TRACKING_URL,
                            changeList[0].value.account,
                            changeList[0].value.id))
                }
            }

            assertThat(result[2].grutHistoryEvent)
                .`as`("Проверка содержимого(изменение регионов)")
                .isEqualTo(GrutCampaignHistoryEvent(
                    changeList[1].value.regions ?: emptyList(),
                    REGIONS,
                    GrutHistoryOutputCategory.CAMPAIGN_REGIONS,
                    changeList[1].value.account,
                    changeList[1].value.id))
        }.assertAll()
    }

    @Test
    fun grutHistoryIteratorTest_next() {
        val changes = getCampaignHistories()
        val iterator = NotDeletedGrutHistoryPeekingIterator(changes.iterator())
        for (i in 0..2) {
            val next = iterator.next()
            SoftAssertions().apply {
                assertThat(next)
                    .`as`("Assert iterator content on read")
                    .isNotNull
                    .isEqualTo(changes[i])
            }
        }
    }

    @Test
    fun grutHistoryIteratorTest_peek() {
        val changes = getCampaignHistories()
        val iterator = NotDeletedGrutHistoryPeekingIterator(changes.iterator())
        for (i in 0..2) {
            val peek = iterator.peek()
            val next = iterator.peek()

            SoftAssertions().apply {
                assertThat(peek)
                    .`as`("Assert iterator content on read")
                    .isNotNull
                    .isEqualTo(changes[i])
                    .isEqualTo(next)
            }
        }
    }

    @Test
    fun grutHistoryIteratorTest_removal() {
        val changes = getCampaignHistories()
        val iterator = NotDeletedGrutHistoryPeekingIterator(changes.iterator())
        assertThrows<UnsupportedOperationException> {
            iterator.remove()
        }
    }

    @Test
    fun grutHistoryIteratorTest_reachOverLimitOnNext() {
        val changes = emptyList<GrutHistoryEventEntry<UacYdbCampaign>>()
        val iterator = NotDeletedGrutHistoryPeekingIterator(changes.iterator())
        assertThrows<NoSuchElementException> {
            iterator.next()
        }
    }

    @Test
    fun grutHistoryIteratorTest_reachOverLimitOnPeek() {
        val changes = emptyList<GrutHistoryEventEntry<UacYdbCampaign>>()
        val iterator = NotDeletedGrutHistoryPeekingIterator(changes.iterator())
        assertThrows<NoSuchElementException> {
            iterator.peek()
        }
    }

    private fun updateCampaign(campaign: UacYdbCampaign, changes: KtModelChanges<String, UacYdbCampaign>): UacYdbCampaign {
        uacUpdateCampaignService.updateCampaignFromModelChanges(campaign, changes)
        return uacCampaignService.getCampaignById(campaign.id)!!
    }

    private fun timestamp(time: LocalDateTime): Long {
        return UacYdbUtils.toEpochSecond(time)
    }

    private fun getCampaignHistories(): List<GrutHistoryEventEntry<UacYdbCampaign>> {
        val campaignId = grutSteps.createMobileAppCampaign(clientInfo)
        val campaign = uacCampaignService.getCampaignById(campaignId.toString())

        val adjustmentChanges = KtModelChanges<String, UacYdbCampaign>(campaign!!.id)
        adjustmentChanges.process(UacYdbCampaign::trackingUrl, NEW_URL)
        adjustmentChanges.process(UacYdbCampaign::adultContentEnabled, true)

        val updatedCampaign = updateCampaign(campaign, adjustmentChanges)

        val regionChanges = KtModelChanges<String, UacYdbCampaign>(updatedCampaign.id)
        regionChanges.process(UacYdbCampaign::regions, REGIONS)
        val updatedCampaign2 = updateCampaign(updatedCampaign, regionChanges)
        changeList.apply {
            add(GrutHistoryEventEntry(campaign, timestamp(campaign.createdAt), GrutHistoryEventType.ET_OBJECT_CREATED))
            add(GrutHistoryEventEntry(updatedCampaign, timestamp(updatedCampaign.updatedAt), GrutHistoryEventType.ET_OBJECT_UPDATED))
            add(GrutHistoryEventEntry(updatedCampaign2, timestamp(updatedCampaign2.updatedAt), GrutHistoryEventType.ET_OBJECT_UPDATED))
            add(GrutHistoryEventEntry(updatedCampaign2, timestamp(LocalDateTime.now()), GrutHistoryEventType.ET_OBJECT_REMOVED))
        }

        return changeList
    }
}
