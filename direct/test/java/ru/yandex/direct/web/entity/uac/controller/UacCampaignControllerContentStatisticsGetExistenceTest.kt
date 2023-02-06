package ru.yandex.direct.web.entity.uac.controller

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.uac.model.CampaignContentStatus
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaignContent
import ru.yandex.direct.model.KtModelChanges
import ru.yandex.direct.test.utils.TestUtils.assumeThat
import ru.yandex.direct.utils.DateTimeUtils
import ru.yandex.direct.web.configuration.DirectWebTest
import java.time.LocalDateTime

@DirectWebTest
@RunWith(JUnitParamsRunner::class)
class UacCampaignControllerContentStatisticsGetExistenceTest : UacCampaignControllerContentStatisticsBaseTest() {

    companion object {
        private val SEARCH_FROM = LocalDateTime.now().minusDays(3).withHour(1)
        private val SEARCH_TO = LocalDateTime.now().minusDays(1).withHour(1)
    }

    fun assetExistenceParameters() = arrayOf(
        arrayOf("ассет был создан до from и удален до from", -1L, -1L, false),
        arrayOf("ассет был создан до from и удален в день from", -1L, 0L, true),
        arrayOf("ассет был создан до from и удален в день to", -1L, 2L, true),
        arrayOf("ассет был создан до from и удален после to", -1L, 3L, true),
        arrayOf("ассет был создан до from", -1L, null, true),
        arrayOf("ассет был создан в день from и удален в день from", 0L, 0L, true),
        arrayOf("ассет был создан в день from и удален в день to", 0L, 2L, true),
        arrayOf("ассет был создан в день from и удален после to", 0L, 3L, true),
        arrayOf("ассет был создан в день from", 0L, null, true),
        arrayOf("ассет был создан в день to и удален в день to", 2L, 2L, true),
        arrayOf("ассет был создан в день to и удален после to", 2L, 3L, true),
        arrayOf("ассет был создан в день to", 2L, null, true),
        arrayOf("ассет был создан после to и удален после to", 3L, 3L, false),
        arrayOf("ассет был создан после to", 3L, null, false),
    )

    /**
     * Проверяем показ ассета в зависимости от дат создания/удаления ассета и периода показа статистики
     */
    @Test
    @TestCaseName("{0} -> {3}")
    @Parameters(method = "assetExistenceParameters")
    fun `check assets existence`(
        description: String,
        daysOffsetOfCreatedAt: Long,
        daysOffsetOfRemovedAt: Long?,
        expectExistence: Boolean,
    ) {
        val createdAt = SEARCH_FROM.plusDays(daysOffsetOfCreatedAt)
        val removedAt = if (daysOffsetOfRemovedAt != null) SEARCH_FROM.plusDays(daysOffsetOfRemovedAt) else null

        val from = SEARCH_FROM.toLocalDate().atStartOfDay().atZone(DateTimeUtils.MSK).toEpochSecond()
        val to =
            SEARCH_TO.plusDays(1).toLocalDate().atStartOfDay().minusSeconds(1).atZone(DateTimeUtils.MSK).toEpochSecond()

        assumeThat { sa ->
            sa.assertThat(to - from)
                .`as`("Поиск статистики за 3 дня")
                // Если период сменился - нужно так же сменить входные данные в параметрах этого теста
                .isEqualTo(259199)
        };

        setCreateAtToCampaign(LocalDateTime.now().minusDays(20))

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
        val actualAssetIdToToShow = sendAndGetResultAssetIdToExistence(from, to)

        val expectedAssetIdToPartialExistence = if (expectExistence) listOf(deletedTextAsset.id) else emptyList()

        Assertions.assertThat(actualAssetIdToToShow)
            .`as`("Показ ассета")
            .isEqualTo(expectedAssetIdToPartialExistence)
    }

    private fun sendAndGetResultAssetIdToExistence(
        from: Long,
        to: Long,
    ): List<String> {
        val uacContentStatisticsResponse = sendAndGetStatisticResult(from, to)
        return uacContentStatisticsResponse.results
            .map { it.id }
    }
}
