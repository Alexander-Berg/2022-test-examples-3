package ru.yandex.market.wms.consolidation.modules.consolidation.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT
import com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.internal.verification.VerificationModeFactory.atLeast
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.MethodArgumentNotValidException
import ru.yandex.market.wms.common.model.enums.NSqlConfigKey
import ru.yandex.market.wms.common.service.DbConfigService
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.exception.InvalidContainerIdException
import ru.yandex.market.wms.common.spring.exception.PickDetailUnprocessableTitledException
import ru.yandex.market.wms.common.spring.service.NamedCounterService
import ru.yandex.market.wms.common.spring.solomon.SolomonPushClient
import ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent
import ru.yandex.market.wms.consolidation.core.async.MoveToLostProducer
import ru.yandex.market.wms.consolidation.modules.consolidation.dao.ConsolidationDao
import ru.yandex.market.wms.consolidation.modules.consolidation.exception.ContainerInAnotherLocException
import ru.yandex.market.wms.consolidation.modules.consolidation.exception.ContainerIsEmptyException
import ru.yandex.market.wms.consolidation.modules.consolidation.exception.ContainerNotFromWaveException
import ru.yandex.market.wms.consolidation.modules.consolidation.exception.LineAndPutwallInDiffAreasException
import ru.yandex.market.wms.consolidation.modules.consolidation.exception.NotEnoughGoodsException
import ru.yandex.market.wms.consolidation.modules.consolidation.exception.OrderCancelledException
import ru.yandex.market.wms.consolidation.modules.consolidation.service.ConsolidationWaveService
import ru.yandex.market.wms.consolidation.modules.consolidation.service.PutWallActivityHolder
import ru.yandex.market.wms.transportation.client.TransportationClient
import java.time.Clock

class ConsolidationControllerTest : IntegrationTest() {

    @SpyBean
    @Autowired
    private lateinit var namedCounterService: NamedCounterService

    @SpyBean
    @Autowired
    private lateinit var solomonPushClient: SolomonPushClient

    @Autowired
    private lateinit var dbConfigService: DbConfigService

    @Autowired
    private lateinit var clock: Clock

    @Autowired
    private lateinit var waveService: ConsolidationWaveService

    @SpyBean
    @Autowired
    private lateinit var consolidationDao: ConsolidationDao

    @Autowired
    private lateinit var putWallActivityHolder: PutWallActivityHolder

    @SpyBean
    @Autowired
    private lateinit var moveToLostProducer: MoveToLostProducer

    @MockBean
    @Autowired
    private lateinit var transportationClient: TransportationClient

    @AfterEach
    fun after() {
        Mockito.reset<Any>(dbConfigService)
    }

    @Test
    @DatabaseSetup("/line-for-putwall/before.xml")
    @ExpectedDatabase("/line-for-putwall/after.xml", assertionMode = NON_STRICT)
    fun getLineForPutWall() {
        mockMvc.perform(get("/cons/line-for-putwall/SORT-01"))
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("line-for-putwall/response.json"), false))
    }

    @Test
    @DatabaseSetup("/line-for-putwall/before.xml")
    @ExpectedDatabase("/line-for-putwall/after.xml", assertionMode = NON_STRICT)
    fun getLineForPutWall_withCache() {
        waveService.refillTaskCache()
        mockMvc.perform(get("/cons/line-for-putwall/SORT-01"))
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("line-for-putwall/response.json"), false))
    }

    @Test
    @DatabaseSetup("/line-for-putwall/before-with-status.xml")
    @ExpectedDatabase("/line-for-putwall/after.xml", assertionMode = NON_STRICT)
    fun getLineForPutWall_withStatus() {
        mockMvc.perform(get("/cons/line-for-putwall/SORT-01"))
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("line-for-putwall/response.json"), false))
    }

    @Test
    @DatabaseSetup("/line-for-putwall/empty.xml")
    fun getLineForPutWall_SortStationNotExist() {
        mockMvc.perform(get("/cons/line-for-putwall/SORT-01"))
            .andExpect(status().is4xxClientError)
    }

    @Test
    @DatabaseSetup("/line-for-putwall/before-already-being-sorted.xml")
    fun getLineForPutWall_alreadyBeingSorted_nothing() {
        mockMvc.perform(get("/cons/line-for-putwall/SORT-01"))
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("line-for-putwall/response.json"), false))
    }

    @Test
    @DatabaseSetup("/line-for-putwall/before-noContent.xml")
    fun getLineForPutWall_noContent() {
        mockMvc.perform(get("/cons/line-for-putwall/SORT-01"))
            .andExpect(status().isNoContent)
            .andReturn()
    }

    @Test
    @DatabaseSetup("/line-for-putwall/before-nonWave-balance.xml")
    fun getLineForPutWall_NonWaveBalances() {
        mockMvc.perform(get("/cons/line-for-putwall/SORT-01"))
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("line-for-putwall/response.json"), false))
    }

    @Test
    @DatabaseSetup("/line-for-putwall-percentage/before/before-1.xml")
    @ExpectedDatabase("/line-for-putwall-percentage/after/after-1.xml", assertionMode = NON_STRICT_UNORDERED)
    fun getLineForPutWall_Percentage_100() {
        Mockito.doAnswer {
            when (it.getArgument(0) as String) {
                "WAVE001" -> 0.0
                "WAVE002" -> 100.0
                "WAVE003" -> 99.0
                else -> 0.0
            }
        }.`when`(consolidationDao).percentageOfWaveNearPutWall(anyString())

        mockMvc.perform(get("/cons/line-for-putwall/SORT-01"))
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("line-for-putwall-percentage/after/response-1.json"), false))

    }

    @Test
    @DatabaseSetup("/line-for-putwall-percentage/before/before-2.xml")
    @ExpectedDatabase("/line-for-putwall-percentage/after/after-2.xml", assertionMode = NON_STRICT_UNORDERED)
    fun getLineForPutWall_Percentage_55_58() {
        Mockito.doAnswer {
            when (it.getArgument(0) as String) {
                "WAVE001" -> 55.0
                "WAVE002" -> 58.0
                "WAVE003" -> 10.0
                else -> 0.0
            }
        }.`when`(consolidationDao).percentageOfWaveNearPutWall(anyString())

        mockMvc.perform(get("/cons/line-for-putwall/SORT-01"))
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("line-for-putwall-percentage/after/response-2-1.json"), false))

        dbConfigService.updateConfigByValue("YM_PARTIAL_CONS_NON_CONVEYOR", "58", clock)

        mockMvc.perform(get("/cons/line-for-putwall/SORT-01"))
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("line-for-putwall-percentage/after/response-2-2.json"), false))

    }

    @Test
    @DatabaseSetup("/line-for-putwall-percentage/before/before-3.xml")
    @ExpectedDatabase("/line-for-putwall-percentage/after/after-3.xml", assertionMode = NON_STRICT_UNORDERED)
    fun getLineForPutWall_Percentage_80_conveyor() {
        Mockito.doAnswer {
            when (it.getArgument(0) as String) {
                "WAVE001" -> 55.0
                "WAVE002" -> 58.0
                "WAVE003" -> 10.0
                else -> 0.0
            }
        }.`when`(consolidationDao).percentageOfWaveNearPutWall(anyString())

        mockMvc.perform(get("/cons/line-for-putwall/SORT-01"))
            .andExpect(status().isNoContent)

        dbConfigService.updateConfigByValue("YM_PARTIAL_CONS_NON_CONVEYOR", "50", clock)

        mockMvc.perform(get("/cons/line-for-putwall/SORT-01"))
            .andExpect(status().isNoContent)

        dbConfigService.updateConfigByValue("YM_PARTIAL_CONS_CONVEYOR", "50", clock)

        mockMvc.perform(get("/cons/line-for-putwall/SORT-01"))
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("line-for-putwall-percentage/after/response-3.json"), false))

    }

    @Test
    @DatabaseSetup("/get-container-info/post/before.xml")
    fun getContainerInfo_wrongArea() {
        mockMvc.perform(
            post("/cons/get-container-info")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("get-container-info/post/request-wrong-area.json"))
        )
            .andExpect(status().is4xxClientError)
            .andExpect { result -> assertTrue(result.resolvedException is LineAndPutwallInDiffAreasException) }
            .andReturn()
    }

    @Test
    @DatabaseSetup("/get-container-info/post/before.xml")
    fun getContainerInfo_emptyId() {
        mockMvc.perform(
            post("/cons/get-container-info")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("get-container-info/post/request-with-empty-id.json"))
        )
            .andExpect(status().is4xxClientError)
            .andExpect { result -> assertEquals(ContainerIsEmptyException::class, result.resolvedException::class) }
            .andReturn()
    }

    @Test
    @DatabaseSetup("/get-container-info/post/before.xml")
    fun getContainerInfo_wrongIdLocation() {
        mockMvc.perform(
            post("/cons/get-container-info")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("get-container-info/post/request-wrong-id-location.json"))
        )
            .andExpect(status().is4xxClientError)
            .andExpect { result -> assertEquals(ContainerInAnotherLocException::class, result.resolvedException::class) }
            .andReturn()
    }

    @Test
    @DatabaseSetup("/get-container-info/post/before.xml")
    fun getContainerInfo_skuIsNotEnough() {
        mockMvc.perform(
            post("/cons/get-container-info")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("get-container-info/post/request-sku-with-empty-balance.json"))
        )
            .andExpect(status().is4xxClientError)
            .andExpect { result -> assertEquals(NotEnoughGoodsException::class, result.resolvedException::class) }
            .andReturn()
    }

    @Test
    @DatabaseSetup(
        "/get-container-info/post/before.xml",
        "/get-container-info/lt_wavemgmt_batchpicking_old_off.xml"
    )
    @ExpectedDatabase("/get-container-info/post/after.xml", assertionMode = NON_STRICT)
    fun getContainerInfoWithPreconsolidation() {
        mockMvc.perform(
            post("/cons/get-container-info")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("get-container-info/post/request.json"))
        )
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("get-container-info/post/response.json"), true))
            .andReturn()
    }

    @Test
    @DatabaseSetup("/get-container-info/post/before-tote.xml")
    @ExpectedDatabase("/get-container-info/post/after-tote.xml", assertionMode = NON_STRICT)
    fun getContainerInfoUpdatesToteStatusAndLoc() {
        mockMvc.perform(
            post("/cons/get-container-info")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("get-container-info/post/request-tote.json"))
        )
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("get-container-info/post/response-tote.json"), true))
            .andReturn()
    }

    @Test
    @DatabaseSetup(
        "/get-container-info/post/before-2.xml",
        "/get-container-info/lt_wavemgmt_batchpicking_old_off.xml"
    )
    fun getContainerInfoWithPreconsolidation_OldUI() {
        mockMvc.perform(
            post("/cons/get-container-info")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("get-container-info/post/request.json"))
        )
            .andExpect(status().is4xxClientError)
            .andReturn()
    }

    @Test
    @DatabaseSetup(
        "/get-container-info/post/before.xml",
        "/get-container-info/lt_wavemgmt_batchpicking_old_on.xml"
    )
    @ExpectedDatabase("/get-container-info/post/before.xml", assertionMode = NON_STRICT_UNORDERED)
    fun getContainerInfoWithPreconsolidationWrongConfigValue() {
        mockMvc.perform(
            post("/cons/get-container-info")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("get-container-info/post/request.json"))
        )
            .andExpect(status().is4xxClientError)
            .andExpect(
                content().json(
                    getFileContent("get-container-info/post/response-wrong-config.json"),
                    true
                )
            )
            .andReturn()
    }

    @Test
    @DatabaseSetup(
        "/get-container-info/get/before.xml",
        "/get-container-info/lt_wavemgmt_batchpicking_old_on.xml"
    )
    fun getContainerInfoWithoutPreconsolidation() {
        mockMvc.perform(get("/cons/get-container-info/CART0001"))
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("get-container-info/get/response.json"), true))
            .andReturn()
    }

    @Test
    @DatabaseSetup(
        "/get-container-info/get/before.xml",
        "/get-container-info/lt_wavemgmt_batchpicking_old_off.xml"
    )
    fun getContainerInfoWithoutPreconsolidationWrongConfigValue() {
        mockMvc.perform(get("/cons/get-container-info/CART0001"))
            .andExpect(status().is4xxClientError)
            .andExpect(
                content().json(
                    getFileContent("get-container-info/get/response-wrong-config.json"),
                    true
                )
            )
            .andReturn()
    }

    @Test
    @DatabaseSetup("/get-container-info/post/before.xml")
    fun getContainerInfo_waveNotReady() {
        mockMvc.perform(
            post("/cons/get-container-info")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("get-container-info/post/request-wave-not-ready.json"))
        )
            .andExpect(status().is4xxClientError)
            .andExpect { result -> assertEquals(ContainerNotFromWaveException::class, result.resolvedException::class) }
            .andReturn()
    }

    @Test
    @DatabaseSetup("/get-container-info/post/before-nonWave.xml")
    fun getContainerInfo_NonWaveBalance() {
        mockMvc.perform(
            post("/cons/get-container-info")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("get-container-info/post/request-non-wave.json"))
        )
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("get-container-info/post/response-non-wave.json"), true))
            .andReturn()
    }

    @Test
    fun scanUit_Blank_BadRequest() {
        mockMvc.perform(
            post("/cons/scan-uit")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("scan-uit/request-0.json"))
        )
            .andExpect(status().is4xxClientError)
            .andExpect { assertTrue(it.resolvedException is MethodArgumentNotValidException) }
    }

    @Test
    @DatabaseSetup("/scan-uit/scan-uit.xml")
    fun scanUit_SerialNotFound() {
        mockMvc.perform(
            post("/cons/scan-uit")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("scan-uit/request-2.json"))
        )
            .andExpect(status().is4xxClientError)
    }

    @Test
    @DatabaseSetup("/scan-uit/scan-uit.xml")
    fun scanUit_ContainerNotEquals() {
        mockMvc.perform(
            post("/cons/scan-uit")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("scan-uit/request-3.json"))
        )
            .andExpect(status().is4xxClientError)
    }

    @Test
    @DatabaseSetup("/scan-uit/scan-uit.xml")
    fun scanUit_HappyPath() {
        mockMvc.perform(
            post("/cons/scan-uit")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("scan-uit/request-1.json"))
        )
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("scan-uit/response.json"), true))
    }

    @Test
    @DatabaseSetup("/scan-uit/scan-uit-order-not-in-putwall.xml")
    fun scanUit_NoOrderInPutwall_HappyPath() {
        mockMvc.perform(
            post("/cons/scan-uit")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("scan-uit/request-1.json"))
        )
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("scan-uit/response-neworderkey.json"), true))
    }

    @Test
    @DatabaseSetup("/scan-uit/scan-uit.xml")
    fun scanUit_NullContainerId() {
        mockMvc.perform(
            post("/cons/scan-uit")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("scan-uit/request-4.json"))
        )
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("scan-uit/response.json"), true))
    }

    @Test
    @DatabaseSetup("/scan-uit/scan-uit-cancelled-order.xml")
    fun scanUit_CancelledOrder() {
        mockMvc.perform(
            post("/cons/scan-uit")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("scan-uit/request-1.json"))
        )
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("scan-uit/response-cancelled.json"), true))
    }

    @Test
    @DatabaseSetup("/scan-uit/scan-uit.xml")
    fun scanUit_HappyPath_emptyCellsHint() {
        enableEmptyCellsHintOptional()
        mockMvc.perform(
            post("/cons/scan-uit")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("scan-uit/request-1.json"))
        )
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("scan-uit/response-emptycellshint.json"), true))
    }

    @Test
    @DatabaseSetup("/scan-uit/scan-uit.xml")
        /**
         * Нет советований ячеек, т.к. заказ уже есть в путволе. Настойчивость советования неактуальна.
         */
    fun scanUit_HappyPass_emptyCellsHintInsist() {
        enableEmptyCellsHintInsist()
        mockMvc.perform(
            post("/cons/scan-uit")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("scan-uit/request-1.json"))
        )
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("scan-uit/response-emptycellshint.json"), true))
    }

    @Test
    @DatabaseSetup("/scan-uit/scan-uit-order-not-in-putwall.xml")
    fun scanUit_NoOrderInPutwall_emptyCellsHint() {
        enableEmptyCellsHintOptional()
        mockMvc.perform(
            post("/cons/scan-uit")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("scan-uit/request-1.json"))
        )
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("scan-uit/response-emptycellshint-neworderkey.json"), true))
    }

    @Test
    @DatabaseSetup("/scan-uit/scan-uit-order-not-in-putwall.xml")
    fun scanUit_NoOrderInPutwall_emptyCellsHintInsist() {
        enableEmptyCellsHintInsist()
        mockMvc.perform(
            post("/cons/scan-uit")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("scan-uit/request-1.json"))
        )
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("scan-uit/response-emptycellshint-neworderkey-insist.json"), true))
    }

    @Test
    @DatabaseSetup("/scan-uit/scan-uit.xml")
    fun scanUit_NullContainerId_emptyCellsHint() {
        enableEmptyCellsHintOptional()
        mockMvc.perform(
            post("/cons/scan-uit")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("scan-uit/request-4.json"))
        )
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("scan-uit/response-emptycellshint.json"), true))
    }

    @Test
    @DatabaseSetup("/scan-uit/scan-uit-cancelled-order.xml")
    fun scanUit_CancelledOrder_emptyCellsHint() {
        enableEmptyCellsHintOptional()
        mockMvc.perform(
            post("/cons/scan-uit")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("scan-uit/request-1.json"))
        )
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("scan-uit/response-emptycellshint-cancelled.json"), true))
    }

    @Test
    @DatabaseSetup("/scan-uit/scan-uit-nonsort.xml")
    fun scanUit_NullContainerId_NONSORT() {
        mockMvc.perform(
            post("/cons/scan-uit")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("scan-uit/request-4.json"))
        )
            .andExpect(status().is4xxClientError)
            .andExpect { assertTrue(it.resolvedException is PickDetailUnprocessableTitledException) }
    }

    @Test
    @DatabaseSetup("/scan-uit/scan-uit-wave-not-sorting.xml")
    fun scanUit_NullContainerId_waveNotSorting() {
        mockMvc.perform(
            post("/cons/scan-uit")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("scan-uit/request-4.json"))
        )
            .andExpect(status().is4xxClientError)
    }

    @Test
    @DatabaseSetup("/scan-uit/scan-uit-sku-not-found.xml")
    fun scanUit_SkuNotFound() {
        mockMvc.perform(
            post("/cons/scan-uit")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("scan-uit/request-1.json"))
        )
            .andExpect(status().is4xxClientError)
    }

    @Test
    @DatabaseSetup("/scan-uit/scan-uit-pickdetail-not-found.xml")
    fun scanUit_PickDetailNotFound() {
        mockMvc.perform(
            post("/cons/scan-uit")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("scan-uit/request-1.json"))
        )
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("scan-uit/response-uit-nomad-to-cancelled.json"), true))
    }

    @Test
    @DatabaseSetup("/get-container-items/before.xml")
    fun getContainerItems() {
        mockMvc.perform(get("/cons/get-container-items/CART0001"))
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("get-container-items/response.json"), true))
    }

    @Test
    @DatabaseSetup("/get-container-items/before.xml")
    fun getContainerItems_EmptyContainer() {
        mockMvc.perform(get("/cons/get-container-items/CART8800"))
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("get-container-items/response-empty.json"), true))
    }

    @Test
    fun scanCell_Blank_BadRequest() {
        mockMvc.perform(
            post("/cons/scan-putwall-cell")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("scan-putwall-cell/before/request-0.json"))
        )
            .andExpect(status().is4xxClientError)
            .andExpect { assertTrue(it.resolvedException is MethodArgumentNotValidException) }
    }

    @Test
    @DatabaseSetup("/scan-putwall-cell/before/scan-cell.xml")
    @ExpectedDatabase("/scan-putwall-cell/after/scan-cell.xml", assertionMode = NON_STRICT_UNORDERED)
    fun scanCell() {
        mockMvc.perform(
            post("/cons/scan-putwall-cell")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("scan-putwall-cell/before/request-1.json"))
        )
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("scan-putwall-cell/after/response-1.json"), false))
    }

    @Test
    @DatabaseSetup("/scan-putwall-cell/before/scan-cell.xml")
    fun scanCell_alreadySorting() {
        mockMvc.perform(
            post("/cons/scan-putwall-cell")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("scan-putwall-cell/before/request-4.json"))
        )
            .andExpect(status().is4xxClientError)
    }

    @Test
    @DatabaseSetup("/scan-putwall-cell/before/scan-cell-empty.xml")
    @ExpectedDatabase("/scan-putwall-cell/after/scan-cell-empty.xml", assertionMode = NON_STRICT_UNORDERED)
    fun scanCell_Empty() {
        Mockito.doReturn("IDGENERATED").`when`(namedCounterService).nextCaseId
        mockMvc.perform(
            post("/cons/scan-putwall-cell")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("scan-putwall-cell/before/request-2.json"))
        )
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("scan-putwall-cell/after/response-1.json"), false))
    }

    @Test
    @DatabaseSetup("/scan-putwall-cell/before/scan-cell-delete-batch.xml")
    @ExpectedDatabase("/scan-putwall-cell/after/scan-cell-delete-batch.xml", assertionMode = NON_STRICT_UNORDERED)
    fun scanCell_DeleteBatch() {
        Mockito.doReturn("IDGENERATED").`when`(namedCounterService).nextCaseId
        mockMvc.perform(
            post("/cons/scan-putwall-cell")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("scan-putwall-cell/before/request-3.json"))
        )
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("scan-putwall-cell/after/response-3.json"), false))
    }

    @Test
    @DatabaseSetup("/scan-putwall-cell/before/scan-cell-cancelled-order.xml")
    fun scanCell_CancelledOrder() {
        mockMvc.perform(
            post("/cons/scan-putwall-cell")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("scan-putwall-cell/before/request-3.json"))
        )
            .andExpect(status().is4xxClientError)
            .andExpect { assertTrue(it.resolvedException is OrderCancelledException) }
            .andExpect(content().json(getFileContent("scan-putwall-cell/after/response-4.json"), false))
    }

    @Test
    @DatabaseSetup("/move-cancelled-item/before.xml")
    @ExpectedDatabase("/move-cancelled-item/after.xml", assertionMode = NON_STRICT_UNORDERED)
    fun scanCancelledOrder() {
        mockMvc.perform(
            post("/cons/move-cancelled-item")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("move-cancelled-item/request.json"))
        )
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("move-cancelled-item/response.json"), true))
    }

    @Test
    fun scanCancelledOrder_Blank_BadRequest() {
        mockMvc.perform(
            post("/cons/move-cancelled-item")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("move-cancelled-item/request-0.json"))
        )
            .andExpect(status().is4xxClientError)
            .andExpect { assertTrue(it.resolvedException is MethodArgumentNotValidException) }
    }

    @Test
    @DatabaseSetup("/move-cancelled-item/before.xml")
    @ExpectedDatabase("/move-cancelled-item/before.xml", assertionMode = NON_STRICT_UNORDERED)
    fun scanCancelledOrder_InvalidId_BadRequest() {
        mockMvc.perform(
            post("/cons/move-cancelled-item")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("move-cancelled-item/request-1.json"))
        )
            .andExpect(status().is4xxClientError)
            .andExpect { assertTrue(it.resolvedException is InvalidContainerIdException) }
    }

    @Test
    @DatabaseSetup("/close-container/before/before-1.xml")
    @ExpectedDatabase("/close-container/after/after-1.xml", assertionMode = NON_STRICT_UNORDERED)
    fun closeContainer() {
        mockMvc.perform(
            post("/cons/close-container")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("close-container/request.json"))
        )
            .andExpect(status().is2xxSuccessful)
    }

    @Test
    @DatabaseSetup(value = ["/close-container/before/before-1.xml", "/close-container/before/enable-shorts.xml"])
    @ExpectedDatabase("/close-container/after/after-1.xml", assertionMode = NON_STRICT_UNORDERED)
    fun closeContainer_withShortParam() {
        mockMvc.perform(
            post("/cons/close-container")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("close-container/request.json"))
        )
            .andExpect(status().is2xxSuccessful)

        Mockito.verify(moveToLostProducer, atLeast(1)).produce("SORT-01", "CART002", "TEST")
    }

    @Test
    fun closeContainer_Blank_BadRequest() {
        mockMvc.perform(
            post("/cons/close-container")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("close-container/request-0.json"))
        )
            .andExpect(status().is4xxClientError)
            .andExpect { assertTrue(it.resolvedException is MethodArgumentNotValidException) }
    }

    /**
     * Есть три заказа. Они отличаются по времени отгрузки (поле SHIPMENTDATETIME)
     * Два заказа отгружаются 2021-09-04 13:00:00.000, другой заказ отгружается на день позже - 2021-09-05 13:00:00.000
     * Волны должны приоритизироваться по времени отгрузки, поэтому должна
     * быть выбрана волна с более ранним временем отгрузки
     * Время отгрузки волны = время отгрузки самого раннего заказа из этой волны
     */
    @Test
    @DatabaseSetup("/line-for-putwall/shipmentdatetime/before.xml")
    @ExpectedDatabase("/line-for-putwall/after.xml", assertionMode = NON_STRICT)
    fun getLineForPutWallUsingShipmentDateTime() {
        mockMvc.perform(get("/cons/line-for-putwall/SORT-01"))
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("line-for-putwall/response.json"), false))
    }

    /**
     * Если SHIPMENTDATETIME == null, ничего не должно падать
     * У первых двух заказов поле SHIPMENTDATETIME отсутствует
     */
    @Test
    @DatabaseSetup("/line-for-putwall/shipmentdatetime/before-without-shipmentdatetime.xml")
    @ExpectedDatabase("/line-for-putwall/after.xml", assertionMode = NON_STRICT)
    fun getLineForPutWallWithoutShipmentDateTime() {
        mockMvc.perform(get("/cons/line-for-putwall/SORT-01"))
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("line-for-putwall/response.json"), false))
    }

    @Test
    @DatabaseSetup("/another-tasks/before/before-1.xml")
    fun getAnotherTasks_cachingON() {
        waveService.refillTaskCache()
        mockMvc.perform(get("/cons/another-tasks/SORT-01"))
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("another-tasks/after/response.json"), false))
    }

    @Test
    @DatabaseSetup("/another-tasks/before/before-2.xml")
    fun getAnotherTasks_cachingOFF() {
        putWallActivityHolder.markActivity("SORT-04")
        mockMvc.perform(get("/cons/another-tasks/SORT-01"))
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("another-tasks/after/response.json"), false))
    }

    @Test
    @DatabaseSetup("/scan-putwall-cell/before/scan-cell-last-item.xml")
    @ExpectedDatabase("/scan-putwall-cell/after/scan-cell-last-item.xml", assertionMode = NON_STRICT_UNORDERED)
    fun scanLastItemMakesToteEmptyAndStartsETReplenishment() {
        mockMvc.perform(
            post("/cons/scan-putwall-cell")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("scan-putwall-cell/before/request-5.json"))
        )
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("scan-putwall-cell/after/response-5.json"), false))
            .andReturn()

        val containerId = "TM12345"

        Mockito.verify(transportationClient, Mockito.times(1)).makeToteEmpty(containerId, null)
    }

    private fun enableEmptyCellsHintOptional() {
        Mockito.doAnswer {
            when (it.getArgument(0) as String) {
                NSqlConfigKey.CONS_EMPTY_CELL_HINT_ON -> true
                NSqlConfigKey.CONS_EMPTY_CELL_HINT_INSIST -> false
                else -> false
            }
        }.`when`(dbConfigService).getConfigAsBoolean(anyString(), anyBoolean())
    }

    private fun enableEmptyCellsHintInsist() {
        Mockito.doAnswer {
            when (it.getArgument(0) as String) {
                NSqlConfigKey.CONS_EMPTY_CELL_HINT_ON -> true
                NSqlConfigKey.CONS_EMPTY_CELL_HINT_INSIST -> true
                else -> false
            }
        }.`when`(dbConfigService).getConfigAsBoolean(anyString(), anyBoolean())
    }
}
