package ru.yandex.direct.web.entity.uac.controller

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.uac.model.CampaignContentStatus
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaignContent
import ru.yandex.direct.model.KtModelChanges
import ru.yandex.direct.test.utils.TestUtils.assumeThat
import ru.yandex.direct.web.configuration.DirectWebTest
import java.time.LocalDateTime.now

@DirectWebTest
@RunWith(JUnitParamsRunner::class)
class UacCampaignControllerContentStatisticsGetPartialExistenceTest : UacCampaignControllerContentStatisticsBaseTest() {

    companion object {
        private val SEARCH_FROM = now().minusDays(3)
        private val SEARCH_TO = now().minusDays(1)
    }

    fun assetPartialExistenceParameters() = arrayOf(
        arrayOf("ассет был создан и удален в этот день", 0L, 0L),
        arrayOf("ассет был создан до from и удален в этот день", -2L, 0L),
        arrayOf("ассет был создан в этот день и удален после to", 0L, 2L),
        arrayOf("ассет был создан в этот день", 0L, null),
        arrayOf("ассет был создан до from", -2L, null),
    )

    /**
     * Проверяем флаг частичного показа ассета при запросе статистики за один день
     */
    @Test
    @TestCaseName("{0} -> false")
    @Parameters(method = "assetPartialExistenceParameters")
    fun `check assets partial existence in one day`(
        description: String,
        daysOffsetOfCreatedAt: Long,
        daysOffsetOfRemovedAt: Long?,
    ) {
        val createdAt = SEARCH_FROM.plusDays(daysOffsetOfCreatedAt)
        val removedAt = if (daysOffsetOfRemovedAt != null) SEARCH_FROM.plusDays(daysOffsetOfRemovedAt) else null

        val from = UacYdbUtils.toEpochSecond(SEARCH_FROM.toLocalDate().atStartOfDay())
        val to = UacYdbUtils.toEpochSecond(SEARCH_FROM.plusDays(1).toLocalDate().atStartOfDay().minusSeconds(1))

        setCreateAtToCampaign(now().minusDays(20))

        val assetModelChanges = KtModelChanges<String, UacYdbCampaignContent>(deletedTextAsset.id)
        assetModelChanges.process(UacYdbCampaignContent::createdAt, createdAt)
        if (removedAt != null) {
            assetModelChanges.process(UacYdbCampaignContent::removedAt, removedAt)
            assetModelChanges.process(UacYdbCampaignContent::status, CampaignContentStatus.DELETED)
        } else {
            assetModelChanges.process(UacYdbCampaignContent::removedAt, null)
            assetModelChanges.process(UacYdbCampaignContent::status, CampaignContentStatus.CREATED)
        }
        uacYdbCampaignContentRepository.update(assetModelChanges)

        getMockedDataFromStatTable(from, to, emptyList())
        val actualAssetIdToPartialExistence = sendAndGetResultAssetIdToPartialExistence(from, to)

        val expectedAssetIdToPartialExistence = mapOf(
            deletedTextAsset.id to false,
        )

        Assertions.assertThat(actualAssetIdToPartialExistence)
            .`as`("Флаг частичного показа статистики ассета")
            .isEqualTo(expectedAssetIdToPartialExistence)
    }

    fun assetPartialExistenceNotInOneDayParameters() = arrayOf(
        arrayOf("ассет был создан до from и удален в день from", -1L, 0L, true),
        arrayOf("ассет был создан до from и удален в день между from и to", -1L, 1L, true),
        arrayOf("ассет был создан до from и удален в день to", -1L, 2L, false),
        arrayOf("ассет был создан до from и удален после to", -1L, 3L, false),
        arrayOf("ассет был создан в день from и удален в день from", 0L, 0L, true),
        arrayOf("ассет был создан в день from и удален в день между from и to", 0L, 1L, true),
        arrayOf("ассет был создан в день from и удален в день to", 0L, 2L, false),
        arrayOf("ассет был создан в день from и удален после to", 0L, 3L, false),
        arrayOf("ассет был создан в день между from и to и удален в этот же день", 1L, 1L, true),
        arrayOf("ассет был создан в день между from и to и удален в день to", 1L, 2L, true),
        arrayOf("ассет был создан в день между from и to и удален после to", 1L, 3L, true),
        arrayOf("ассет был создан в день to и удален в день to", 2L, 2L, true),
        arrayOf("ассет был создан в день to и удален после to", 2L, 3L, true),
        arrayOf("ассет был создан до from", -1L, null, false),
        arrayOf("ассет был создан в день from", 0L, null, false),
        arrayOf("ассет был создан в день между from и to", 1L, null, true),
        arrayOf("ассет был создан в день to", 2L, null, true),
    )

    /**
     * Проверяем флаг частичного показа ассета при запросе статистики за несколько дней
     */
    @Test
    @TestCaseName("{0} -> {3}")
    @Parameters(method = "assetPartialExistenceNotInOneDayParameters")
    fun `check assets partial existence not in one day`(
        description: String,
        daysOffsetOfCreatedAt: Long,
        daysOffsetOfRemovedAt: Long?,
        expectPartialExistence: Boolean,
    ) {
        val createdAt = SEARCH_FROM.plusDays(daysOffsetOfCreatedAt)
        val removedAt = if (daysOffsetOfRemovedAt != null) SEARCH_FROM.plusDays(daysOffsetOfRemovedAt) else null

        val from = UacYdbUtils.toEpochSecond(SEARCH_FROM.toLocalDate().atStartOfDay())
        val to = UacYdbUtils.toEpochSecond(SEARCH_TO.plusDays(1).toLocalDate().atStartOfDay().minusSeconds(1))

        assumeThat { sa ->
            sa.assertThat(to - from)
                .`as`("Поиск статистики за 3 дня")
                // Если период сменился - нужно так же сменить входные данные в параметрах этого теста
                .isEqualTo(259199)
        }

        setCreateAtToCampaign(now().minusDays(20))

        val assetModelChanges = KtModelChanges<String, UacYdbCampaignContent>(deletedTextAsset.id)
        assetModelChanges.process(UacYdbCampaignContent::createdAt, createdAt)
        if (removedAt != null) {
            assetModelChanges.process(UacYdbCampaignContent::removedAt, removedAt)
            assetModelChanges.process(UacYdbCampaignContent::status, CampaignContentStatus.DELETED)
        } else {
            assetModelChanges.process(UacYdbCampaignContent::removedAt, null)
            assetModelChanges.process(UacYdbCampaignContent::status, CampaignContentStatus.CREATED)
        }
        uacYdbCampaignContentRepository.update(assetModelChanges)

        getMockedDataFromStatTable(from, to, emptyList())
        val actualAssetIdToPartialExistence = sendAndGetResultAssetIdToPartialExistence(from, to)

        val expectedAssetIdToPartialExistence = mapOf(
            deletedTextAsset.id to expectPartialExistence,
        )

        Assertions.assertThat(actualAssetIdToPartialExistence)
            .`as`("Флаг частичного показа статистики ассета")
            .isEqualTo(expectedAssetIdToPartialExistence)
    }

    private fun sendAndGetResultAssetIdToPartialExistence(
        from: Long,
        to: Long,
    ): Map<String, Boolean?> {
        val uacContentStatisticsResponse = sendAndGetStatisticResult(from, to)
        return uacContentStatisticsResponse.results
            .associateBy({ it.id }, { it.partialExistence })
    }
}
