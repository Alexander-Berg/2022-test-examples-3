package ru.yandex.direct.oneshot.oneshots.fill_bids_phraseid_associate


import com.nhaarman.mockitokotlin2.mock
import java.time.LocalDateTime
import java.util.function.Consumer
import junitparams.JUnitParamsRunner
import kotlin.math.min
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.util.RepositoryUtils
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.KeywordInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbschema.ppc.Tables.BIDS_PHRASEID_ASSOCIATE
import ru.yandex.direct.dbutil.sharding.ShardHelper
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.oneshot.configuration.OneshotTest
import ru.yandex.direct.oneshot.oneshots.fill_bids_phraseid_associate.repository.InputTableRow
import ru.yandex.direct.oneshot.oneshots.fill_bids_phraseid_associate.repository.OneshotFillBidsPhraseIdAssociateRepository
import ru.yandex.direct.oneshot.oneshots.fill_bids_phraseid_associate.repository.OneshotFillBidsPhraseIdAssociateRepository.Companion.LOG_TIME_FORMATTER
import ru.yandex.direct.test.utils.TestUtils.assumeThat
import ru.yandex.direct.ytwrapper.client.YtProvider
import ru.yandex.direct.ytwrapper.model.YtCluster
import ru.yandex.direct.ytwrapper.model.YtOperator

@OneshotTest
@RunWith(JUnitParamsRunner::class)
class FillBidsPhraseIdAssociateOneshotTest {

    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()
        private const val CHUNK_SIZE = 2L
        private const val LOG_TIME_1 = "2021-05-17 08:31:24"
        private const val LOG_TIME_2 = "2021-04-18 07:24:49"
    }

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    @Autowired
    private lateinit var shardHelper: ShardHelper

    private var ytProvider = mock<YtProvider>()
    private var ytOperator = mock<YtOperator>()

    private lateinit var oneshot: FillBidsPhraseIdAssociateOneshot
    private lateinit var oneshotFillBidsPhraseIdAssociateRepository: OneshotFillBidsPhraseIdAssociateRepository

    private lateinit var clientInfo1: ClientInfo
    private lateinit var clientInfo2: ClientInfo
    private lateinit var adGroupInfo1: AdGroupInfo
    private lateinit var adGroupInfo2: AdGroupInfo
    private lateinit var keywordInfo1: KeywordInfo
    private lateinit var keywordInfo2: KeywordInfo

    @Before
    fun before() {
        `when`(ytProvider.getOperator(any(YtCluster::class.java))).thenReturn(ytOperator)

        oneshotFillBidsPhraseIdAssociateRepository = OneshotFillBidsPhraseIdAssociateRepository(ytProvider, dslContextProvider)

        oneshot = FillBidsPhraseIdAssociateOneshot(
            ytProvider, oneshotFillBidsPhraseIdAssociateRepository, ppcPropertiesSupport, shardHelper, CHUNK_SIZE)

        clientInfo1 = steps.clientSteps().createDefaultClient()
        clientInfo2 = steps.clientSteps().createDefaultClientAnotherShard()

        adGroupInfo1 = steps.adGroupSteps().createDefaultAdGroup(clientInfo1)
        adGroupInfo2 = steps.adGroupSteps().createDefaultAdGroup(clientInfo2)

        keywordInfo1 = steps.keywordSteps().createKeywordWithText("keyword1", adGroupInfo1)
        keywordInfo2 = steps.keywordSteps().createKeywordWithText("keyword2", adGroupInfo2)

        MockitoAnnotations.openMocks(this)
    }

    /**
     * В yt таблице 2 записи для 2ух клиентов на разных шардах. Запускаем ваншот с фильтром по этим двум шардам
     * -> все записи добавились в bids_phraseid_associate
     */
    @Test
    fun `2 rows in yt table from 2 clients in different shards, filter by this two shard`() {
        val inputTableRow1 = getInputTableRow(keywordInfo1, LOG_TIME_1)
        val inputTableRow2 = getInputTableRow(keywordInfo2, LOG_TIME_2)
        mockReadTableByRowRange(listOf(inputTableRow1, inputTableRow2))

        val filterByShards = mutableListOf<Int>(adGroupInfo1.shard, adGroupInfo2.shard)
        val inputData = createInputData(filterByShards)
        executeOneshot(inputData)

        val actualBidsPhraseIdAssociates1 = getBidsPhraseIdAssociates(adGroupInfo1.shard, adGroupInfo1.campaignId)
        val actualBidsPhraseIdAssociates2 = getBidsPhraseIdAssociates(adGroupInfo2.shard, adGroupInfo2.campaignId)

        val expectActualBidsPhraseIdAssociates1 = getExpectedBidsPhraseIdAssociate(keywordInfo1, LOG_TIME_1)
        val expectActualBidsPhraseIdAssociates2 = getExpectedBidsPhraseIdAssociate(keywordInfo2, LOG_TIME_2)

        val soft = SoftAssertions()
        soft.assertThat(actualBidsPhraseIdAssociates1)
            .`as`("История изменения PhraseID для keyword 1")
            .hasSize(1)
            .containsOnly(expectActualBidsPhraseIdAssociates1)
        soft.assertThat(actualBidsPhraseIdAssociates2)
            .`as`("История изменения PhraseID для keyword 2")
            .hasSize(1)
            .containsOnly(expectActualBidsPhraseIdAssociates2)
        soft.assertAll()
    }

    /**
     * В yt таблице 2 записи для 2ух клиентов на разных шардах. Запускаем ваншот с фильтром по одному из них
     * -> добавилась только запись по переданному шарду в bids_phraseid_associate
     */
    @Test
    fun `2 rows in yt table from 2 clients in different shards, filter by one shard`() {
        val inputTableRow1 = getInputTableRow(keywordInfo1, LOG_TIME_1)
        val inputTableRow2 = getInputTableRow(keywordInfo2, LOG_TIME_2)

        mockReadTableByRowRange(listOf(inputTableRow1, inputTableRow2))

        val filterByShards = mutableListOf<Int>(adGroupInfo2.shard)
        val inputData = createInputData(filterByShards)
        executeOneshot(inputData)

        val actualBidsPhraseIdAssociates1 = getBidsPhraseIdAssociates(adGroupInfo1.shard, adGroupInfo1.campaignId)
        val actualBidsPhraseIdAssociates2 = getBidsPhraseIdAssociates(adGroupInfo2.shard, adGroupInfo2.campaignId)

        val expectActualBidsPhraseIdAssociates2 = getExpectedBidsPhraseIdAssociate(keywordInfo2, LOG_TIME_2)

        val soft = SoftAssertions()
        soft.assertThat(actualBidsPhraseIdAssociates1)
            .`as`("История изменения PhraseID для keyword 1")
            .isEmpty()
        soft.assertThat(actualBidsPhraseIdAssociates2)
            .`as`("История изменения PhraseID для keyword 2")
            .hasSize(1)
            .containsOnly(expectActualBidsPhraseIdAssociates2)
        soft.assertAll()
    }

    /**
     * В yt таблице 2 записи для 2ух клиентов на разных шардах. Запускаем ваншот без фильтра по шардам
     * -> все записи добавились в bids_phraseid_associate
     */
    @Test
    fun `2 rows in yt table from 2 clients in different shards, without filter by shard`() {
        val inputTableRow1 = getInputTableRow(keywordInfo1, LOG_TIME_1)
        val inputTableRow2 = getInputTableRow(keywordInfo2, LOG_TIME_2)

        mockReadTableByRowRange(listOf(inputTableRow1, inputTableRow2))

        val inputData = createInputData(null)
        executeOneshot(inputData)

        val actualBidsPhraseIdAssociates1 = getBidsPhraseIdAssociates(adGroupInfo1.shard, adGroupInfo1.campaignId)
        val actualBidsPhraseIdAssociates2 = getBidsPhraseIdAssociates(adGroupInfo2.shard, adGroupInfo2.campaignId)

        val expectActualBidsPhraseIdAssociates1 = getExpectedBidsPhraseIdAssociate(keywordInfo1, LOG_TIME_1)
        val expectActualBidsPhraseIdAssociates2 = getExpectedBidsPhraseIdAssociate(keywordInfo2, LOG_TIME_2)

        val soft = SoftAssertions()
        soft.assertThat(actualBidsPhraseIdAssociates1)
            .`as`("История изменения PhraseID для keyword 1")
            .hasSize(1)
            .containsOnly(expectActualBidsPhraseIdAssociates1)
        soft.assertThat(actualBidsPhraseIdAssociates2)
            .`as`("История изменения PhraseID для keyword 2")
            .hasSize(1)
            .containsOnly(expectActualBidsPhraseIdAssociates2)
        soft.assertAll()
    }

    /**
     * В yt таблице 2 записи для 2ух клиентов на разных шардах. Запускаем ваншот с пустым фильтром по шардам
     * -> все записи добавились в bids_phraseid_associate
     */
    @Test
    fun `2 rows in yt table from 2 clients in different shards, with empty filter by shard`() {
        val inputTableRow1 = getInputTableRow(keywordInfo1, LOG_TIME_1)
        val inputTableRow2 = getInputTableRow(keywordInfo2, LOG_TIME_2)

        mockReadTableByRowRange(listOf(inputTableRow1, inputTableRow2))

        val inputData = createInputData(emptyList())
        executeOneshot(inputData)

        val actualBidsPhraseIdAssociates1 = getBidsPhraseIdAssociates(adGroupInfo1.shard, adGroupInfo1.campaignId)
        val actualBidsPhraseIdAssociates2 = getBidsPhraseIdAssociates(adGroupInfo2.shard, adGroupInfo2.campaignId)

        val expectActualBidsPhraseIdAssociates1 = getExpectedBidsPhraseIdAssociate(keywordInfo1, LOG_TIME_1)
        val expectActualBidsPhraseIdAssociates2 = getExpectedBidsPhraseIdAssociate(keywordInfo2, LOG_TIME_2)

        val soft = SoftAssertions()
        soft.assertThat(actualBidsPhraseIdAssociates1)
            .`as`("История изменения PhraseID для keyword 1")
            .hasSize(1)
            .containsOnly(expectActualBidsPhraseIdAssociates1)
        soft.assertThat(actualBidsPhraseIdAssociates2)
            .`as`("История изменения PhraseID для keyword 2")
            .hasSize(1)
            .containsOnly(expectActualBidsPhraseIdAssociates2)
        soft.assertAll()
    }

    /**
     * В yt таблице 2 одинаковые записи -> в bids_phraseid_associate добавилась только одна
     */
    @Test
    fun `2 duplicate rows in yt table`() {
        val inputTableRow1 = getInputTableRow(keywordInfo1, LOG_TIME_1)
        val inputTableRow2 = getInputTableRow(keywordInfo1, LOG_TIME_1)

        mockReadTableByRowRange(listOf(inputTableRow1, inputTableRow2))

        val inputData = createInputData(null)
        executeOneshot(inputData)

        val actualBidsPhraseIdAssociates = getBidsPhraseIdAssociates(adGroupInfo1.shard, adGroupInfo1.campaignId)

        val expectActualBidsPhraseIdAssociates1 = getExpectedBidsPhraseIdAssociate(keywordInfo1, LOG_TIME_1)

        val soft = SoftAssertions()
        soft.assertThat(actualBidsPhraseIdAssociates)
            .`as`("История изменения PhraseID для keyword без дублей")
            .hasSize(1)
            .containsOnly(expectActualBidsPhraseIdAssociates1)
        soft.assertAll()
    }

    /**
     * В yt таблице одина запись, которая уже есть в mysql -> в bids_phraseid_associate ничего не добавляется
     */
    @Test
    fun `1 row in yt table that already exist in mysql`() {
        val expectActualBidsPhraseIdAssociates = getExpectedBidsPhraseIdAssociate(keywordInfo1, LOG_TIME_1)

        oneshotFillBidsPhraseIdAssociateRepository
            .insertBidsPhraseIdAssociates(adGroupInfo1.shard, listOf(expectActualBidsPhraseIdAssociates))

        var actualBidsPhraseIdAssociates = getBidsPhraseIdAssociates(adGroupInfo1.shard, adGroupInfo1.campaignId)
        assumeThat{sa -> sa.assertThat(actualBidsPhraseIdAssociates)
            .`as`("История изменения PhraseID для keyword")
            .hasSize(1)
            .containsOnly(expectActualBidsPhraseIdAssociates)
        }

        val inputTableRow = getInputTableRow(keywordInfo1, LOG_TIME_1)
        mockReadTableByRowRange(listOf(inputTableRow))

        val inputData = createInputData(null)
        executeOneshot(inputData)

        actualBidsPhraseIdAssociates = getBidsPhraseIdAssociates(adGroupInfo1.shard, adGroupInfo1.campaignId)

        val soft = SoftAssertions()
        soft.assertThat(actualBidsPhraseIdAssociates)
            .`as`("История изменения PhraseID для keyword без дублей")
            .hasSize(1)
            .containsOnly(expectActualBidsPhraseIdAssociates)
        soft.assertAll()
    }

    /**
     * В yt таблице несколько записей, в количестве большем размера чанка
     * -> все записи добавились в bids_phraseid_associate
     */
    @Test
    fun `multiple rows in yt table, more than chunk size`() {
        val logTime3 = "2021-01-01 00:00:00"
        val logTime4 = "2021-02-18 02:24:49"
        val logTime5 = "2021-03-29 23:59:59"

        val keywordInfo2 = steps.keywordSteps().createKeywordWithText("keyword2", adGroupInfo1)
        val keywordInfo3 = steps.keywordSteps().createKeywordWithText("keyword3", adGroupInfo1)
        val keywordInfo4 = steps.keywordSteps().createKeywordWithText("keyword4", adGroupInfo1)
        val keywordInfo5 = steps.keywordSteps().createKeywordWithText("keyword5", adGroupInfo1)

        val inputTableRow1 = getInputTableRow(keywordInfo1, LOG_TIME_1)
        val inputTableRow2 = getInputTableRow(keywordInfo2, LOG_TIME_2)
        val inputTableRow3 = getInputTableRow(keywordInfo3, logTime3)
        val inputTableRow4 = getInputTableRow(keywordInfo4, logTime4)
        val inputTableRow5 = getInputTableRow(keywordInfo5, logTime5)
        mockReadTableByRowRange(listOf(inputTableRow1, inputTableRow2, inputTableRow3, inputTableRow4, inputTableRow5))

        val inputData = createInputData(null)
        executeOneshot(inputData)

        val actualBidsPhraseIdAssociates = getBidsPhraseIdAssociates(adGroupInfo1.shard, adGroupInfo1.campaignId).toSet()

        val expectActualBidsPhraseIdAssociates = setOf(
            getExpectedBidsPhraseIdAssociate(keywordInfo1, LOG_TIME_1),
            getExpectedBidsPhraseIdAssociate(keywordInfo2, LOG_TIME_2),
            getExpectedBidsPhraseIdAssociate(keywordInfo3, logTime3),
            getExpectedBidsPhraseIdAssociate(keywordInfo4, logTime4),
            getExpectedBidsPhraseIdAssociate(keywordInfo5, logTime5),
        )

        val soft = SoftAssertions()
        soft.assertThat(actualBidsPhraseIdAssociates)
            .`as`("История изменения PhraseID для keyword")
            .hasSize(expectActualBidsPhraseIdAssociates.size)
            .containsAll(expectActualBidsPhraseIdAssociates)
        soft.assertAll()
    }

    private fun createInputData(
        shards: List<Int>?
    ): InputData = InputData(
        ytCluster = YtCluster.HAHN,
        tablePath = "",
        shards = shards
    )

    private fun getInputTableRow(
        keywordInfo: KeywordInfo,
        logTime: String,
    ): InputTableRow {
        val inputTableRow = InputTableRow()
        inputTableRow.setValue(InputTableRow.CID, keywordInfo.campaignId as java.lang.Long)
        inputTableRow.setValue(InputTableRow.PID, keywordInfo.adGroupId as java.lang.Long)
        inputTableRow.setValue(InputTableRow.BIDS_ID, keywordInfo.keyword.id as java.lang.Long)
        inputTableRow.setValue(InputTableRow.PHRASE_ID, keywordInfo.keyword.phraseBsId.toString())
        inputTableRow.setValue(InputTableRow.LOG_TIME, logTime)
        return inputTableRow
    }

    private fun getExpectedBidsPhraseIdAssociate(
        keywordInfo: KeywordInfo,
        togTime: String,
    ) = BidsPhraseIdAssociate(
        campaignId = keywordInfo.campaignId,
        adGroupId = keywordInfo.adGroupId,
        keywordId = keywordInfo.keyword.id,
        bsPhraseID = keywordInfo.keyword.phraseBsId,
        logTime = LocalDateTime.parse(togTime, LOG_TIME_FORMATTER),
    )

    private fun mockReadTableByRowRange(rows: List<InputTableRow>) {
        Mockito.doAnswer { invocation: InvocationOnMock ->
            val from = invocation.getArgument<Any>(3) as Long

            val consumer = invocation.getArgument<Any>(1) as Consumer<InputTableRow>
            for (i in from until min(rows.size.toLong(), from + CHUNK_SIZE)) {
                consumer.accept(rows[i.toInt()])
            }
            null
        }.`when`(ytOperator)
            .readTableByRowRange(any(), any(), any(), ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong())
    }

    /**
     * Эмулация выполнения oneshot'a
     */
    private fun executeOneshot(inputData: InputData) {
        var state: State? = null
        var maxIterationsCount = 20
        while (maxIterationsCount-- > 0) {
            state = oneshot.execute(inputData, state)
            if (state == null) {
                return
            }
        }
    }

    private fun getBidsPhraseIdAssociates(
        shard: Int,
        campaignId: Long
    ): List<BidsPhraseIdAssociate> = dslContextProvider.ppc(shard)
        .select(
            BIDS_PHRASEID_ASSOCIATE.CID,
            BIDS_PHRASEID_ASSOCIATE.PID,
            BIDS_PHRASEID_ASSOCIATE.BIDS_ID,
            BIDS_PHRASEID_ASSOCIATE.PHRASE_ID,
            BIDS_PHRASEID_ASSOCIATE.LOGTIME,
        )
        .from(BIDS_PHRASEID_ASSOCIATE)
        .where(BIDS_PHRASEID_ASSOCIATE.CID.eq(campaignId))
        .fetch { record ->
            BidsPhraseIdAssociate(
                campaignId = record[BIDS_PHRASEID_ASSOCIATE.CID],
                adGroupId = record[BIDS_PHRASEID_ASSOCIATE.PID],
                keywordId = record[BIDS_PHRASEID_ASSOCIATE.BIDS_ID],
                bsPhraseID = RepositoryUtils.bigIntegerFromULong(record[BIDS_PHRASEID_ASSOCIATE.PHRASE_ID]),
                logTime = record[BIDS_PHRASEID_ASSOCIATE.LOGTIME],
            )
        }
}
