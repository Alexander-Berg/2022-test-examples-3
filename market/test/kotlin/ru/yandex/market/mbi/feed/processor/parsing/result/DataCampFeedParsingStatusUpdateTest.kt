package ru.yandex.market.mbi.feed.processor.parsing.result

import org.assertj.core.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import ru.yandex.market.core.indexer.model.ReturnCode
import ru.yandex.market.mbi.feed.processor.model.FeedProcessingStatus
import ru.yandex.market.mbi.feed.processor.model.FeedType
import ru.yandex.market.mbi.feed.processor.parsing.status.DataCampFeedParsingError
import ru.yandex.market.mbi.feed.processor.parsing.status.DataCampFeedParsingErrorArg
import ru.yandex.market.mbi.feed.processor.parsing.status.DataCampFeedParsingErrorArgsList
import ru.yandex.market.mbi.feed.processor.parsing.status.DataCampFeedParsingFeedIdentifiers
import ru.yandex.market.mbi.feed.processor.parsing.status.DataCampFeedParsingInfo
import ru.yandex.market.mbi.feed.processor.parsing.status.DataCampFeedParsingReturnCode
import ru.yandex.market.mbi.feed.processor.parsing.status.DataCampFeedParsingStatus
import ru.yandex.market.mbi.feed.processor.parsing.status.DataCampFeedParsingStatusIdentifiers
import ru.yandex.market.mbi.feed.processor.parsing.status.DataCampFeedParsingTime
import ru.yandex.market.mbi.feed.processor.parsing.yt.model.DatacampParsingHistoryRecord
import ru.yandex.market.mbi.feed.processor.parsing.yt.model.DatacampParsingHistoryRecord.OfferStatistics
import ru.yandex.market.mbi.feed.processor.parsing.yt.model.FeedParsingResult
import ru.yandex.market.mbi.feed.processor.parsing.yt.model.ParseLogExampleDetail
import ru.yandex.market.mbi.feed.processor.parsing.yt.model.ParseLogExampleLevel
import ru.yandex.market.mbi.feed.processor.parsing.yt.model.ParseLogExampleRecord
import ru.yandex.market.mbi.feed.processor.parsing.yt.model.ParseLogRecord
import ru.yandex.market.mbi.feed.processor.parsing.yt.model.ParsingType
import ru.yandex.market.mbi.feed.processor.test.toInstant

/**
 * Тесты на обновление модели [DataCampFeedParsingStatus].
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
internal class DataCampFeedParsingStatusUpdateTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("statusData")
    fun `test update`(
        name: String,
        old: DataCampFeedParsingStatus?,
        parsingResult: DatacampParsingHistoryRecord,
        expected: DataCampFeedParsingStatus
    ) {
        val updatedStatus = old.update(parsingResult)
        Assertions.assertThat(updatedStatus)
            .isEqualTo(expected)
    }

    companion object {

        private val successfulParsing = DatacampParsingHistoryRecord(
            id = DatacampParsingHistoryRecord.Id(
                businessId = 1001,
                partnerId = 1002,
                feedId = 1003,
                feedType = FeedType.ASSORTMENT_FEED,
                updateTime = toInstant(2020, 1, 1),
                parsingTaskId = 2001
            ),
            offerStats = OfferStatistics(
                loadedOffers = 1
            ),
            parsingType = ParsingType.COMPLETE,
            receivedTime = toInstant(2020, 2, 2),
            feedParserStatusCode = FeedParsingResult.SUCCESS,
            parseStatusCode = ReturnCode.OK
        )

        private val errorParsing = successfulParsing.copy(
            parseStatusCode = ReturnCode.ERROR,
            parseLog = ParseLogRecord(
                examples = listOf(
                    ParseLogExampleRecord(
                        level = ParseLogExampleLevel.FATAL,
                        namespace = null,
                        code = "550",
                        details = listOf(
                            ParseLogExampleDetail(name = "code", value = "550")
                        ),
                        text = "Too many offers declined"
                    )
                )
            )
        )

        private val successfulStatus = DataCampFeedParsingStatus(
            id = 0,
            feedIdentifiers = DataCampFeedParsingFeedIdentifiers(
                feedId = 1003,
                feedType = FeedType.ASSORTMENT_FEED,
                partnerId = 1002
            ),
            parsingIdentifiers = DataCampFeedParsingStatusIdentifiers(
                lastParsingId = 2001,
                lastNonFatalParsingId = 2001,
                lastSuccessParsingId = 2001
            ),
            lastNonFatalParsing = DataCampFeedParsingInfo(
                parsingType = ParsingType.COMPLETE,
                returnCode = DataCampFeedParsingReturnCode(
                    code = ReturnCode.OK,
                    number = 1
                ),
                time = DataCampFeedParsingTime(
                    sentTime = toInstant(2020, 1, 1),
                    receivedTime = toInstant(2020, 2, 2)
                ),
                error = null
            ),
            isFatalInLastParsing = false,
            status = FeedProcessingStatus.PROCESSED_WITHOUT_ERRORS
        )

        private val errorStatus = successfulStatus.copy(
            parsingIdentifiers = successfulStatus.parsingIdentifiers.copy(
                lastSuccessParsingId = null
            ),
            lastNonFatalParsing = successfulStatus.lastNonFatalParsing!!.copy(
                returnCode = DataCampFeedParsingReturnCode(
                    code = ReturnCode.ERROR,
                    number = 1
                ),
                error = DataCampFeedParsingError(
                    code = "550",
                    errorArgs = DataCampFeedParsingErrorArgsList(
                        list = listOf(
                            DataCampFeedParsingErrorArg(
                                name = "code",
                                value = "550"
                            )
                        )
                    )
                )
            )
        )

        private val firstFatalStatus = DataCampFeedParsingStatus(
            id = 100,
            feedIdentifiers = DataCampFeedParsingFeedIdentifiers(
                feedId = 1003,
                feedType = FeedType.ASSORTMENT_FEED,
                partnerId = 1002
            ),
            parsingIdentifiers = DataCampFeedParsingStatusIdentifiers(
                lastParsingId = 999,
                lastNonFatalParsingId = null,
                lastSuccessParsingId = null
            ),
            lastNonFatalParsing = null,
            isFatalInLastParsing = true,
            status = null
        )

        private val fatalStatusAfterSuccessful = successfulStatus.copy(
            id = 100,
            parsingIdentifiers = DataCampFeedParsingStatusIdentifiers(
                lastParsingId = 999,
                lastNonFatalParsingId = 998,
                lastSuccessParsingId = 998
            ),
            isFatalInLastParsing = true
        )

        @JvmStatic
        fun statusData() = listOf(
            Arguments.of(
                "Статуса не было до этого. Парсинг успешный",
                null,
                successfulParsing,
                successfulStatus
            ),
            Arguments.of(
                "Статуса не было до этого. Парсинг фатальный",
                null,
                successfulParsing.copy(
                    parseStatusCode = ReturnCode.FATAL,
                    offerStats = OfferStatistics(
                        loadedOffers = 0
                    ),
                ),
                successfulStatus.copy(
                    parsingIdentifiers = DataCampFeedParsingStatusIdentifiers(
                        lastParsingId = 2001,
                        lastSuccessParsingId = null,
                        lastNonFatalParsingId = null
                    ),
                    lastNonFatalParsing = null,
                    isFatalInLastParsing = true,
                    status = FeedProcessingStatus.EXTERNAL_ERROR
                )
            ),
            Arguments.of(
                "Статуса не было до этого. Парсинг с ошибкой",
                null,
                errorParsing,
                errorStatus.copy(
                    status = FeedProcessingStatus.EXTERNAL_ERROR
                )
            ),
            Arguments.of(
                "Самый первый статус был фатальным, пришел еще один фатальный",
                firstFatalStatus,
                successfulParsing.copy(
                    parseStatusCode = ReturnCode.FATAL,
                    offerStats = OfferStatistics(
                        loadedOffers = 0
                    ),
                ),
                firstFatalStatus.copy(
                    parsingIdentifiers = firstFatalStatus.parsingIdentifiers.copy(
                        lastParsingId = 2001
                    ),
                    status = FeedProcessingStatus.EXTERNAL_ERROR
                )
            ),
            Arguments.of(
                "Самый первый статус был фатальным, пришел с ошибкой",
                firstFatalStatus,
                errorParsing,
                errorStatus.copy(
                    id = 100,
                    status = FeedProcessingStatus.EXTERNAL_ERROR
                )
            ),
            Arguments.of(
                "Самый первый статус был фатальным, пришел успешный",
                firstFatalStatus,
                successfulParsing,
                successfulStatus.copy(id = 100)
            ),
            Arguments.of(
                "Предыдущий статус был фатальным, пришел еще один фатальный",
                fatalStatusAfterSuccessful,
                successfulParsing.copy(
                    parseStatusCode = ReturnCode.FATAL,
                    offerStats = OfferStatistics(
                        loadedOffers = 0
                    ),
                ),
                fatalStatusAfterSuccessful.copy(
                    parsingIdentifiers = fatalStatusAfterSuccessful.parsingIdentifiers.copy(
                        lastParsingId = 2001
                    ),
                    status = FeedProcessingStatus.EXTERNAL_ERROR
                )
            ),
            Arguments.of(
                "Предыдущий статус был фатальным, пришел с ошибкой",
                fatalStatusAfterSuccessful,
                errorParsing,
                errorStatus.copy(
                    id = 100,
                    parsingIdentifiers = errorStatus.parsingIdentifiers.copy(
                        lastSuccessParsingId = 998
                    ),
                    status = FeedProcessingStatus.EXTERNAL_ERROR
                )
            ),
            Arguments.of(
                "Предыдущий статус был фатальным, пришел успешный",
                fatalStatusAfterSuccessful,
                successfulParsing,
                successfulStatus.copy(
                    id = 100,
                    lastNonFatalParsing = successfulStatus.lastNonFatalParsing!!.copy(
                        returnCode = DataCampFeedParsingReturnCode(
                            code = ReturnCode.OK,
                            number = 2 // 2, тк перед фатальным парсингом был тоже успешный
                        )
                    )
                )
            ),
            Arguments.of(
                "Предыдущий статус был с ошибкой, пришел предупреждением",
                errorStatus.copy(id = 100),
                successfulParsing.copy(
                    parseStatusCode = ReturnCode.WARNING,
                    parseLog = ParseLogRecord(
                        examples = listOf(
                            ParseLogExampleRecord(
                                level = ParseLogExampleLevel.ERROR,
                                namespace = null,
                                code = "330",
                                details = listOf(
                                    ParseLogExampleDetail(name = "code", value = "330")
                                ),
                                text = "cats warning"
                            )
                        )
                    )
                ),
                successfulStatus.copy(
                    id = 100,
                    lastNonFatalParsing = successfulStatus.lastNonFatalParsing!!.copy(
                        returnCode = DataCampFeedParsingReturnCode(
                            code = ReturnCode.WARNING,
                            number = 1
                        )
                    )
                )
            ),
            Arguments.of(
                "Предыдущий статус был с ошибкой, пришел опять с ошибкой",
                errorStatus.copy(
                    id = 100,
                    parsingIdentifiers = DataCampFeedParsingStatusIdentifiers(
                        lastParsingId = 999,
                        lastNonFatalParsingId = 999,
                        lastSuccessParsingId = 998
                    )
                ),
                errorParsing,
                errorStatus.copy(
                    id = 100,
                    parsingIdentifiers = errorStatus.parsingIdentifiers.copy(
                        lastSuccessParsingId = 998
                    ),
                    lastNonFatalParsing = errorStatus.lastNonFatalParsing!!.copy(
                        returnCode = DataCampFeedParsingReturnCode(
                            code = ReturnCode.ERROR,
                            number = 2
                        )
                    ),
                    status = FeedProcessingStatus.EXTERNAL_ERROR
                )
            ),
            Arguments.of(
                "Предыдущий статус был с ошибкой, пришел успешный",
                errorStatus.copy(
                    id = 100,
                    parsingIdentifiers = DataCampFeedParsingStatusIdentifiers(
                        lastParsingId = 999,
                        lastNonFatalParsingId = 999,
                        lastSuccessParsingId = 998
                    )
                ),
                successfulParsing,
                successfulStatus.copy(id = 100)
            ),
            Arguments.of(
                "Предыдущий статус был успешный, пришел фатальный",
                successfulStatus.copy(
                    id = 100,
                    parsingIdentifiers = DataCampFeedParsingStatusIdentifiers(
                        lastParsingId = 999,
                        lastNonFatalParsingId = 999,
                        lastSuccessParsingId = 999
                    )
                ),
                successfulParsing.copy(
                    parseStatusCode = ReturnCode.FATAL,
                    offerStats = OfferStatistics(
                        loadedOffers = 0
                    ),
                ),
                successfulStatus.copy(
                    id = 100,
                    parsingIdentifiers = DataCampFeedParsingStatusIdentifiers(
                        lastParsingId = 2001,
                        lastNonFatalParsingId = 999,
                        lastSuccessParsingId = 999
                    ),
                    isFatalInLastParsing = true,
                    status = FeedProcessingStatus.EXTERNAL_ERROR
                )
            ),
            Arguments.of(
                "Предыдущий статус был успешный, пришел с ошибкой",
                successfulStatus.copy(
                    id = 100,
                    parsingIdentifiers = DataCampFeedParsingStatusIdentifiers(
                        lastParsingId = 999,
                        lastNonFatalParsingId = 999,
                        lastSuccessParsingId = 999
                    )
                ),
                errorParsing,
                errorStatus.copy(
                    id = 100,
                    parsingIdentifiers = errorStatus.parsingIdentifiers.copy(
                        lastSuccessParsingId = 999
                    ),
                    status = FeedProcessingStatus.EXTERNAL_ERROR
                )
            ),
            Arguments.of(
                "Предыдущий статус был успешный, пришел успешный",
                successfulStatus.copy(
                    id = 100,
                    parsingIdentifiers = DataCampFeedParsingStatusIdentifiers(
                        lastParsingId = 999,
                        lastNonFatalParsingId = 999,
                        lastSuccessParsingId = 999
                    )
                ),
                successfulParsing,
                successfulStatus.copy(
                    id = 100,
                    lastNonFatalParsing = successfulStatus.lastNonFatalParsing!!.copy(
                        returnCode = DataCampFeedParsingReturnCode(
                            code = ReturnCode.OK,
                            number = 2
                        )
                    )
                ),
            )
        )
    }
}
