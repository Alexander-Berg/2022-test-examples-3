package ru.yandex.market.mbi.feed.processor.parsing.mapper

import Market.DataCamp.API.UpdateTask
import org.assertj.core.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import ru.yandex.market.core.indexer.model.ReturnCode

/**
 * Тесты для PartnerParsingFeedEventExtensions.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
internal class PartnerParsingFeedEventExtensionsTest {

    @ParameterizedTest
    @CsvSource(
        value = [
            "OK,OK",
            "WARN,WARNING",
            "ERROR,WARNING",
            "CRIT,ERROR",
            "FATAL,FATAL",
        ]
    )
    fun `convert ParsingStatus from idx to inner model`(idxModel: UpdateTask.ParsingStatus, innerModel: ReturnCode) {
        Assertions.assertThat(idxModel.toReturnCode())
            .isEqualTo(innerModel)
    }
}
