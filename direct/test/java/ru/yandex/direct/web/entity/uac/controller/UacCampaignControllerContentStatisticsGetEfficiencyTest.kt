package ru.yandex.direct.web.entity.uac.controller

import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.model.campaign_content.AssetStat
import ru.yandex.direct.core.entity.uac.model.campaign_content.ContentEfficiency
import ru.yandex.direct.core.entity.uac.model.campaign_content.ContentEfficiency.COLLECTING
import ru.yandex.direct.core.entity.uac.model.campaign_content.ContentEfficiency.FIVE
import ru.yandex.direct.core.entity.uac.model.campaign_content.ContentEfficiency.FOUR
import ru.yandex.direct.core.entity.uac.model.campaign_content.ContentEfficiency.ONE
import ru.yandex.direct.core.entity.uac.model.campaign_content.ContentEfficiency.THREE
import ru.yandex.direct.core.entity.uac.model.campaign_content.ContentEfficiency.TWO
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaignContent
import ru.yandex.direct.model.KtModelChanges
import ru.yandex.direct.web.configuration.DirectWebTest

@DirectWebTest
@RunWith(JUnitParamsRunner::class)
class UacCampaignControllerContentStatisticsGetEfficiencyTest : UacCampaignControllerContentStatisticsBaseTest() {

    // all cost = 20, avg cost = 10, thresholds = [0,4], (4-7], (7-10], (10-15], (15-)
    fun assetEfficiencyParameters() = arrayOf(
        arrayOf(null, null, ONE, ONE),
        arrayOf(BigDecimal.valueOf(20), null, FIVE, ONE),
        arrayOf(BigDecimal.valueOf(15), BigDecimal.valueOf(5), FOUR, TWO),
        arrayOf(BigDecimal.valueOf(10), BigDecimal.valueOf(10), THREE, THREE),
        arrayOf(BigDecimal.valueOf(7), BigDecimal.valueOf(13), TWO, FOUR),
        arrayOf(null, BigDecimal.valueOf(20), ONE, FIVE),
    )

    /**
     * Проверяем расчет эффективности для разных значений clicksCost у ассетов одного типа
     */
    @Test
    @TestCaseName("Cost of asset1 {0} and asset2 {1}. Expect efficiency1 {2} and efficiency2 {3}")
    @Parameters(method = "assetEfficiencyParameters")
    fun `check assets efficiency`(
        clicksCost1: BigDecimal?,
        clicksCost2: BigDecimal?,
        expectEfficiency1: ContentEfficiency,
        expectEfficiency2: ContentEfficiency,
    ) {

        val createAt = now().minusDays(7)

        val from = UacYdbUtils.toEpochSecond(createAt.toLocalDate().atStartOfDay())
        val to = UacYdbUtils.toEpochSecond(now().plusDays(1).toLocalDate().atStartOfDay().minusSeconds(1))
        val createAtSeconds = UacYdbUtils.toEpochSecond(createAt)

        setCreateAtToCampaignAndAssets(createAt)

        val assetStats = mutableListOf<AssetStat>()
        if (clicksCost1 != null) {
            assetStats.add(defaultAssetStat(textAsset.id, clicksCost = clicksCost1))
        }
        if (clicksCost2 != null) {
            assetStats.add(defaultAssetStat(deletedTextAsset.id, clicksCost = clicksCost2))
        }

        getMockedDataFromStatTable(createAtSeconds, to, assetStats)
        val actualAssetIdToEfficiency = sendAndGetResultAssetIdToEfficiency(from, to)

        val expectedAssetIdToEfficiency = mapOf(
            titleAsset.id to ONE,
            textAsset.id to expectEfficiency1,
            deletedTextAsset.id to expectEfficiency2,
        )

        assertThat(actualAssetIdToEfficiency)
            .`as`("Эффективность ассетов")
            .isEqualTo(expectedAssetIdToEfficiency)
    }

    /**
     * Проверяем расчет эффективности для разных значений clicksCost, когда есть еще данные по другому типу ассетов
     */
    @Test
    @TestCaseName("Cost of asset1 {0} and asset2 {1}. Expect efficiency: {2}, {3} and THREE")
    @Parameters(method = "assetEfficiencyParameters")
    fun `check assets efficiency with different types`(
        textClicksCost1: BigDecimal?,
        textClicksCost2: BigDecimal?,
        expectTextEfficiency1: ContentEfficiency,
        expectTextEfficiency2: ContentEfficiency,
    ) {
        val createAt = now().minusDays(7)

        val from = UacYdbUtils.toEpochSecond(createAt.toLocalDate().atStartOfDay())
        val to = UacYdbUtils.toEpochSecond(now().plusDays(1).toLocalDate().atStartOfDay().minusSeconds(1))
        val createAtSeconds = UacYdbUtils.toEpochSecond(createAt)

        setCreateAtToCampaignAndAssets(createAt)

        val assetStats = mutableListOf<AssetStat>()
        if (textClicksCost1 != null) {
            assetStats.add(defaultAssetStat(textAsset.id, clicksCost = textClicksCost1))
        }
        if (textClicksCost2 != null) {
            assetStats.add(defaultAssetStat(deletedTextAsset.id, clicksCost = textClicksCost2))
        }
        assetStats.add(defaultAssetStat(titleAsset.id, MediaType.TITLE, BigDecimal.valueOf(100L)))

        getMockedDataFromStatTable(createAtSeconds, to, assetStats)

        val actualAssetIdToEfficiency = sendAndGetResultAssetIdToEfficiency(from, to)

        val expectedAssetIdToEfficiency = mapOf(
            textAsset.id to expectTextEfficiency1,
            deletedTextAsset.id to expectTextEfficiency2,
            titleAsset.id to THREE,
        )

        assertThat(actualAssetIdToEfficiency)
            .`as`("Эффективность ассетов")
            .isEqualTo(expectedAssetIdToEfficiency)
    }

    fun assetEfficiencyByActivityParameters() = arrayOf(
        arrayOf(BigDecimal.valueOf(1000), 7L, FIVE, ONE, ONE),
        arrayOf(BigDecimal.valueOf(999), 7L, FIVE, ONE, ONE),
        arrayOf(BigDecimal.valueOf(1000), 6L, THREE, COLLECTING, COLLECTING),
        arrayOf(BigDecimal.valueOf(999), 6L, COLLECTING, COLLECTING, COLLECTING),
    )

    /**
     * Проверяем показ эффективности ассета по его активности
     * Ассет активен если по нему было 1000 показов или прошло 7 дней с даты его создания
     */
    @Test
    @TestCaseName("Shows of first text asset {0} and left {1} days. Expect efficiency: {2}, {3} and {4}")
    @Parameters(method = "assetEfficiencyByActivityParameters")
    fun `check assets efficiency by assets activity`(
        textShows: BigDecimal?,
        daysLeft: Long,
        expectTextEfficiency1: ContentEfficiency,
        expectTextEfficiency2: ContentEfficiency,
        expectTitleEfficiency: ContentEfficiency,
    ) {
        val createAt = now().minusDays(daysLeft)

        val from = UacYdbUtils.toEpochSecond(createAt.toLocalDate().atStartOfDay())
        val to = UacYdbUtils.toEpochSecond(now().plusDays(1).toLocalDate().atStartOfDay().minusSeconds(1))
        val createAtSeconds = UacYdbUtils.toEpochSecond(createAt)

        setCreateAtToCampaignAndAssets(createAt)

        val assetStats = mutableListOf<AssetStat>()
        if (textShows != null) {
            assetStats.add(defaultAssetStat(textAsset.id, shows = textShows))
        }

        getMockedDataFromStatTable(createAtSeconds, to, assetStats)

        val actualAssetIdToEfficiency = sendAndGetResultAssetIdToEfficiency(from, to)

        val expectedAssetIdToEfficiency = mapOf(
            textAsset.id to expectTextEfficiency1,
            deletedTextAsset.id to expectTextEfficiency2,
            titleAsset.id to expectTitleEfficiency,
        )

        assertThat(actualAssetIdToEfficiency)
            .`as`("Эффективность ассетов")
            .isEqualTo(expectedAssetIdToEfficiency)
    }

    private fun sendAndGetResultAssetIdToEfficiency(
        from: Long,
        to: Long,
    ): Map<String, ContentEfficiency> {
        val uacContentStatisticsResponse = sendAndGetStatisticResult(from, to)
        return uacContentStatisticsResponse.results
            .associateBy({ it.id }, { it.efficiency!! })
    }

    private fun setCreateAtToCampaignAndAssets(createAt: LocalDateTime) {
        setCreateAtToCampaign(createAt)
        for (assetId in listOf(titleAsset.id, textAsset.id, deletedTextAsset.id)) {
            val assetModelChanges = KtModelChanges<String, UacYdbCampaignContent>(assetId)
            assetModelChanges.process(UacYdbCampaignContent::createdAt, createAt)
            uacYdbCampaignContentRepository.update(assetModelChanges)
        }
    }

    private fun defaultAssetStat(
        id: String,
        mediaType: MediaType = MediaType.TEXT,
        shows: BigDecimal = BigDecimal.valueOf(1000),
        clicksCost: BigDecimal = BigDecimal.valueOf(20L),
    ) = AssetStat(
        id = id,
        mediaType = mediaType,
        shows = shows,
        clicks = BigDecimal.ONE,
        clicksCost = clicksCost,
        clicksCostCur = BigDecimal.ONE,
        cost = BigDecimal.ONE,
        costCur = BigDecimal.ONE,
        costTaxFree = BigDecimal.ONE,
        conversions = BigDecimal.ONE,
        installs = BigDecimal.ONE,
        postViewConversions = BigDecimal.ONE,
        postViewInstalls = BigDecimal.ONE,
    )
}
