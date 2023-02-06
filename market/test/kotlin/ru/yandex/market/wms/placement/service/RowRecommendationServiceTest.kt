package ru.yandex.market.wms.placement.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import ru.yandex.market.wms.common.model.enums.NSqlConfigKey
import ru.yandex.market.wms.common.service.DbConfigService
import ru.yandex.market.wms.common.spring.dao.entity.SkuId
import ru.yandex.market.wms.common.spring.enums.ContainerIdType
import ru.yandex.market.wms.constraints.client.ConstraintsClient
import ru.yandex.market.wms.constraints.core.request.SkuInfo
import ru.yandex.market.wms.constraints.core.response.GetRestrictedRowsResponse
import ru.yandex.market.wms.constraints.core.response.RestrictedRow
import ru.yandex.market.wms.core.base.dto.RowFullness
import ru.yandex.market.wms.core.base.dto.SerialInventoryDto
import ru.yandex.market.wms.core.base.response.GetIdTypesResponse
import ru.yandex.market.wms.core.base.response.GetRowsFullnessResponse
import ru.yandex.market.wms.core.base.response.GetSerialInventoriesByIdResponse
import ru.yandex.market.wms.core.base.response.IdWithType
import ru.yandex.market.wms.core.client.CoreClient
import ru.yandex.market.wms.placement.dao.RecommendedRowDao
import ru.yandex.market.wms.placement.dao.model.RecommendedRow
import ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider
import java.math.BigDecimal

@Suppress("BlockingMethodInNonBlockingContext")
@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RowRecommendationServiceTest {
    @InjectMocks
    lateinit var service: RowRecommendationService

    @Mock
    lateinit var coreClient: CoreClient

    @Mock
    lateinit var constraintsClient: ConstraintsClient

    @Mock
    lateinit var dbConfigService: DbConfigService

    @Mock
    lateinit var rowRecommendedRowDao: RecommendedRowDao

    @Mock
    lateinit var userProvider: SecurityDataProvider

    @BeforeEach
    fun mockDefaults() {
        whenever(dbConfigService.getConfigAsInteger(eq(NSqlConfigKey.YM_PLACEMENT_ROW_RECOM_TIMEOUT_MS), any()))
            .thenReturn(10000)
        whenever(dbConfigService.getConfigAsBoolean(NSqlConfigKey.YM_PLACEMENT_ROW_RECOM_ALL_POSSIBLE))
            .thenReturn(false)

        whenever(constraintsClient.getRestrictedRowsByZoneAndSku(ZONE, listOf()))
            .thenReturn(Mono.just(GetRestrictedRowsResponse(putawayzone = ZONE, rows = listOf())))
        whenever(coreClient.getSerialInventoriesByIdMono(any()))
            .thenReturn(Mono.just(GetSerialInventoriesByIdResponse(emptyList())))
    }

    @Test
    fun `recommendation returns empty list if request to core fails`() {
        val ids = listOf("RCP01")
        whenever(coreClient.getIdTypesMono(any()))
            .thenReturn(Mono.error(RuntimeException("Test exception")))

        val recommendedRows = service.recommend(ids, ZONE)
        assertThat(recommendedRows).isEmpty()

        verify(coreClient).getIdTypesMono(ids)
    }

    @Test
    fun `recommendation returns empty list if request for id types times out`() {
        val ids = listOf("RCP01")
        whenever(dbConfigService.getConfigAsInteger(eq(NSqlConfigKey.YM_PLACEMENT_ROW_RECOM_TIMEOUT_MS), any()))
            .thenReturn(100)
        whenever(coreClient.getIdTypesMono(any()))
            .thenReturn(Mono.fromCallable {
                Thread.sleep(1000L)
                GetIdTypesResponse(ids.map { IdWithType(it, determineType(it)) })
            }.publishOn(Schedulers.single()))

        val recommendedRows = service.recommend(ids, ZONE)
        assertThat(recommendedRows).isEmpty()

        verify(coreClient).getIdTypesMono(ids)
    }

    @Test
    fun `recommendation returns empty list if request for fullness times out`() {
        val ids = listOf("RCP01")
        whenever(dbConfigService.getConfigAsInteger(eq(NSqlConfigKey.YM_PLACEMENT_ROW_RECOM_TIMEOUT_MS), any()))
            .thenReturn(100)

        whenever(coreClient.getIdTypesMono(any()))
            .thenReturn(Mono.just(GetIdTypesResponse(ids.map { IdWithType(it, determineType(it)) })))
        whenever(coreClient.getRowFullnessMono(any(), any()))
            .thenReturn(Mono.fromCallable {
                Thread.sleep(1000L)
                GetRowsFullnessResponse(listOf(rowFullness("A1-01", ContainerIdType.RCP, 0, 10)))
            }.publishOn(Schedulers.single()))

        val recommendedRows = service.recommend(ids, ZONE)
        assertThat(recommendedRows).isEmpty()

        verify(coreClient).getIdTypesMono(ids)
        verify(coreClient).getRowFullnessMono(ZONE, ContainerIdType.RCP)
        verify(coreClient).getSerialInventoriesByIdMono("RCP01")
    }

    @Test
    fun `recommend one row that accommodates all containers of the same type`() {
        val ids = listOf("RCP01", "RCP02", "RCP03")
        val rows = listOf(
            rowFullness("A1-01", ContainerIdType.RCP, 60, 2),
            rowFullness("B1-01", ContainerIdType.RCP, 50, 5),
            rowFullness("C1-01", ContainerIdType.RCP, 90, 1),
        )

        whenever(coreClient.getIdTypesMono(any()))
            .thenReturn(Mono.just(GetIdTypesResponse(ids.map { IdWithType(it, determineType(it)) })))
        whenever(coreClient.getRowFullnessMono(any(), any()))
            .thenReturn(Mono.just(GetRowsFullnessResponse(rows)))

        val recommendedRows = service.recommend(ids, ZONE)
        assertThat(recommendedRows.map { it.row }).containsExactly("B1-01")

        verify(coreClient).getIdTypesMono(ids)
        verify(coreClient).getRowFullnessMono(ZONE, ContainerIdType.RCP)
    }

    @Test
    fun `recommend one row that accommodates all containers of different types`() {
        val ids = listOf("RCP01", "RCP02", "RCP03", "BL01", "PLT01", "PLT02")
        val rcpRows = listOf(
            rowFullness("A1-01", ContainerIdType.RCP, 40, 10),
            rowFullness("B1-01", ContainerIdType.RCP, 60, 15),
            rowFullness("C1-01", ContainerIdType.RCP, 20, 10),
        )
        val blRows = listOf(
            rowFullness("A1-01", ContainerIdType.BL, 10, 0),
            rowFullness("B1-01", ContainerIdType.BL, 60, 15),
        )
        val pltRows = listOf(
            rowFullness("A1-01", ContainerIdType.PLT, 50, 1),
            rowFullness("B1-01", ContainerIdType.PLT, 50, 2),
        )

        whenever(coreClient.getIdTypesMono(any()))
            .thenReturn(Mono.just(GetIdTypesResponse(ids.map { IdWithType(it, determineType(it)) })))
        whenever(coreClient.getRowFullnessMono(any(), eq(ContainerIdType.RCP)))
            .thenReturn(Mono.just(GetRowsFullnessResponse(rcpRows)))
        whenever(coreClient.getRowFullnessMono(any(), eq(ContainerIdType.BL)))
            .thenReturn(Mono.just(GetRowsFullnessResponse(blRows)))
        whenever(coreClient.getRowFullnessMono(any(), eq(ContainerIdType.PLT)))
            .thenReturn(Mono.just(GetRowsFullnessResponse(pltRows)))

        val recommendedRows = service.recommend(ids, ZONE)
        assertThat(recommendedRows.map { it.row }).containsExactly("B1-01")

        verify(coreClient).getIdTypesMono(ids)
        verify(coreClient).getRowFullnessMono(ZONE, ContainerIdType.RCP)
        verify(coreClient).getRowFullnessMono(ZONE, ContainerIdType.BL)
        verify(coreClient).getRowFullnessMono(ZONE, ContainerIdType.PLT)
    }

    @Test
    fun `recommend multiple rows sorted by fullness groups for containers of the same types`() {
        val ids = listOf("RCP01", "RCP02")
        val rows = listOf(
            rowFullness("A1-01", ContainerIdType.RCP, 0, 1),
            rowFullness("B1-01", ContainerIdType.RCP, 50, 1),
            rowFullness("C1-01", ContainerIdType.RCP, 80, 1),
            rowFullness("D1-01", ContainerIdType.RCP, 90, 1),
        )

        whenever(coreClient.getIdTypesMono(any()))
            .thenReturn(Mono.just(GetIdTypesResponse(ids.map { IdWithType(it, determineType(it)) })))
        whenever(coreClient.getRowFullnessMono(any(), any()))
            .thenReturn(Mono.just(GetRowsFullnessResponse(rows)))

        val recommendedRows = service.recommend(ids, ZONE)
        assertThat(sortRowsByPriority(recommendedRows)).containsExactly("B1-01", "A1-01")

        verify(coreClient).getIdTypesMono(ids)
        verify(coreClient).getRowFullnessMono(ZONE, ContainerIdType.RCP)
    }

    @Test
    fun `recommend all possible rows sorted by fullness groups for containers of the same types`() {
        val ids = listOf("RCP01", "RCP02")
        val rows = listOf(
            rowFullness("A1-01", ContainerIdType.RCP, 0, 1),
            rowFullness("B1-01", ContainerIdType.RCP, 50, 1),
            rowFullness("C1-01", ContainerIdType.RCP, 80, 1),
            rowFullness("D1-01", ContainerIdType.RCP, 90, 1),
        )

        whenever(dbConfigService.getConfigAsBoolean(NSqlConfigKey.YM_PLACEMENT_ROW_RECOM_ALL_POSSIBLE))
            .thenReturn(true)
        whenever(coreClient.getIdTypesMono(any()))
            .thenReturn(Mono.just(GetIdTypesResponse(ids.map { IdWithType(it, determineType(it)) })))
        whenever(coreClient.getRowFullnessMono(any(), any()))
            .thenReturn(Mono.just(GetRowsFullnessResponse(rows)))

        val recommendedRows = service.recommend(ids, ZONE)
        assertThat(sortRowsByPriority(recommendedRows)).containsExactly("B1-01", "A1-01", "C1-01", "D1-01")

        verify(coreClient).getIdTypesMono(ids)
        verify(coreClient).getRowFullnessMono(ZONE, ContainerIdType.RCP)
    }

    @Test
    fun `recommend top priority rows by fullness percent for containers of different types`() {
        val ids = listOf("RCP01", "BL01")
        val rcpRows = listOf(
            rowFullness("A1-01", ContainerIdType.RCP, 0, 1),
            rowFullness("B1-01", ContainerIdType.RCP, 60, 1),
            rowFullness("C1-01", ContainerIdType.RCP, 90, 1),
        )
        val blRows = listOf(
            rowFullness("D1-01", ContainerIdType.BL, 50, 1),
            rowFullness("E1-01", ContainerIdType.BL, 60, 1),
            rowFullness("F1-01", ContainerIdType.BL, 74, 1),
        )

        whenever(coreClient.getIdTypesMono(any()))
            .thenReturn(Mono.just(GetIdTypesResponse(ids.map { IdWithType(it, determineType(it)) })))
        whenever(coreClient.getRowFullnessMono(any(), eq(ContainerIdType.RCP)))
            .thenReturn(Mono.just(GetRowsFullnessResponse(rcpRows)))
        whenever(coreClient.getRowFullnessMono(any(), eq(ContainerIdType.BL)))
            .thenReturn(Mono.just(GetRowsFullnessResponse(blRows)))

        val recommendedRows = service.recommend(ids, ZONE)
        assertThat(recommendedRows.map { it.row }).containsExactlyInAnyOrder("B1-01", "F1-01")

        verify(coreClient).getIdTypesMono(ids)
        verify(coreClient).getRowFullnessMono(ZONE, ContainerIdType.RCP)
        verify(coreClient).getRowFullnessMono(ZONE, ContainerIdType.BL)
    }


    @Test
    fun `recommend rows for containers of different types prioritized by empty loc count`() {
        val ids = (1..6).flatMap { i -> listOf("RCP$i", "BL$i") } // 6 RCP и 6 BL
        val rcpRows = listOf(
            rowFullness("A1-01", ContainerIdType.RCP, 50, 3),
            rowFullness("B1-01", ContainerIdType.RCP, 50, 2),
            rowFullness("C1-01", ContainerIdType.RCP, 50, 1),
        )
        val blRows = listOf(
            rowFullness("A1-01", ContainerIdType.BL, 50, 1),
            rowFullness("B1-01", ContainerIdType.BL, 50, 1),
            rowFullness("C1-01", ContainerIdType.BL, 50, 4),
        )

        whenever(coreClient.getIdTypesMono(any()))
            .thenReturn(Mono.just(GetIdTypesResponse(ids.map { IdWithType(it, determineType(it)) })))
        whenever(coreClient.getRowFullnessMono(any(), eq(ContainerIdType.RCP)))
            .thenReturn(Mono.just(GetRowsFullnessResponse(rcpRows)))
        whenever(coreClient.getRowFullnessMono(any(), eq(ContainerIdType.BL)))
            .thenReturn(Mono.just(GetRowsFullnessResponse(blRows)))

        val recommendedRows = service.recommend(ids, ZONE)
        assertThat(sortRowsByPriority(recommendedRows)).containsExactly("C1-01", "A1-01", "B1-01")

        verify(coreClient).getIdTypesMono(ids)
        verify(coreClient).getRowFullnessMono(ZONE, ContainerIdType.RCP)
        verify(coreClient).getRowFullnessMono(ZONE, ContainerIdType.BL)
    }

    @Test
    fun `recommend rows for containers of different types with some restricted rows for one ID`() {
        val ids = listOf("RCP01", "RCP02", "RCP03", "BL01", "BL02")
        val rcpRows = listOf(
            rowFullness("A1-01", ContainerIdType.RCP, 0, 2),
            rowFullness("B1-01", ContainerIdType.RCP, 50, 2),
            rowFullness("C1-01", ContainerIdType.RCP, 80, 1),
            rowFullness("D1-01", ContainerIdType.RCP, 90, 1),
        )
        val blRows = listOf(
            rowFullness("A1-01", ContainerIdType.BL, 20, 3),
            rowFullness("B1-01", ContainerIdType.BL, 50, 2),
            rowFullness("C1-01", ContainerIdType.BL, 80, 2),
            rowFullness("D1-01", ContainerIdType.BL, 90, 1),
        )

        whenever(coreClient.getIdTypesMono(any()))
            .thenReturn(Mono.just(GetIdTypesResponse(ids.map { IdWithType(it, determineType(it)) })))
        whenever(coreClient.getRowFullnessMono(any(), eq(ContainerIdType.RCP)))
            .thenReturn(Mono.just(GetRowsFullnessResponse(rcpRows)))
        whenever(coreClient.getRowFullnessMono(any(), eq(ContainerIdType.BL)))
            .thenReturn(Mono.just(GetRowsFullnessResponse(blRows)))

        val serials = listOf(serialInventoryDto(sku = "1", storerKey = "12345"))
        whenever(coreClient.getSerialInventoriesByIdMono("BL01"))
            .thenReturn(Mono.just(GetSerialInventoriesByIdResponse(serials)))
        whenever(constraintsClient.getRestrictedRowsByZoneAndSku(ZONE, skusBySerials(serials)))
            .thenReturn(Mono.just(GetRestrictedRowsResponse(
                putawayzone = ZONE,
                rows = listOf(
                    RestrictedRow("A1-01", null),
                    RestrictedRow("B1-01", null),
                    RestrictedRow("C1-01", null),
                )))
            )

        val recommendedRows = service.recommend(ids, ZONE)
        assertThat(sortRowsByPriority(recommendedRows)).containsExactly("B1-01", "A1-01", "D1-01")

        verify(coreClient).getIdTypesMono(ids)
        verify(coreClient).getRowFullnessMono(ZONE, ContainerIdType.RCP)
        verify(coreClient).getRowFullnessMono(ZONE, ContainerIdType.BL)
    }

    @Test
    fun `recommend rows for containers of different types with restricted all rows for one ID`() {
        val ids = listOf("RCP01", "RCP02", "RCP03", "BL01", "BL02")
        val rcpRows = listOf(
            rowFullness("A1-01", ContainerIdType.RCP, 0, 2),
            rowFullness("B1-01", ContainerIdType.RCP, 50, 2),
            rowFullness("C1-01", ContainerIdType.RCP, 80, 1),
            rowFullness("D1-01", ContainerIdType.RCP, 90, 1),
        )
        val blRows = listOf(
            rowFullness("A1-01", ContainerIdType.BL, 20, 3),
            rowFullness("B1-01", ContainerIdType.BL, 50, 2),
            rowFullness("C1-01", ContainerIdType.BL, 80, 2),
            rowFullness("D1-01", ContainerIdType.BL, 90, 1),
        )

        whenever(coreClient.getIdTypesMono(any()))
            .thenReturn(Mono.just(GetIdTypesResponse(ids.map { IdWithType(it, determineType(it)) })))
        whenever(coreClient.getRowFullnessMono(any(), eq(ContainerIdType.RCP)))
            .thenReturn(Mono.just(GetRowsFullnessResponse(rcpRows)))
        whenever(coreClient.getRowFullnessMono(any(), eq(ContainerIdType.BL)))
            .thenReturn(Mono.just(GetRowsFullnessResponse(blRows)))

        val serials = listOf(serialInventoryDto(sku = "1", storerKey = "12345"))
        whenever(coreClient.getSerialInventoriesByIdMono("BL01"))
            .thenReturn(Mono.just(GetSerialInventoriesByIdResponse(serials)))
        whenever(constraintsClient.getRestrictedRowsByZoneAndSku(ZONE, skusBySerials(serials)))
            .thenReturn(Mono.just(GetRestrictedRowsResponse(
                putawayzone = ZONE,
                rows = listOf(
                    RestrictedRow("A1-01", null),
                    RestrictedRow("B1-01", null),
                    RestrictedRow("C1-01", null),
                    RestrictedRow("D1-01", null),
                )))
            )

        val recommendedRows = service.recommend(ids, ZONE)
        assertThat(sortRowsByPriority(recommendedRows)).containsExactly("B1-01", "A1-01")

        verify(coreClient).getIdTypesMono(ids)
        verify(coreClient).getRowFullnessMono(ZONE, ContainerIdType.RCP)
        verify(coreClient).getRowFullnessMono(ZONE, ContainerIdType.BL)
    }

    private fun skusBySerials(serialInventories: List<SerialInventoryDto>): List<SkuInfo> {
        val skuToCount = serialInventories.groupingBy{ SkuId(it.storerKey, it.sku) }.eachCount()
        return skuToCount.map { (skuId, count) -> SkuInfo(skuId.sku, skuId.storerKey, count) }
    }

    private fun determineType(id: String): ContainerIdType {
        return when {
            id.startsWith("RCP") -> ContainerIdType.RCP
            id.startsWith("BL") -> ContainerIdType.BL
            id.startsWith("PLT") -> ContainerIdType.PLT
            else -> throw IllegalArgumentException()
        }
    }

    private fun serialInventoryDto(
        serialNumber: String = "", storerKey: String = "", sku: String = "",
        lot: String = "", loc: String = "", id: String = "",
        quantity: BigDecimal = BigDecimal.ONE,
        addWho: String = "", editWho: String = ""
    ): SerialInventoryDto = SerialInventoryDto(serialNumber, storerKey, sku, lot, loc, id, quantity, addWho, editWho)

    private fun rowFullness(row: String, idType: ContainerIdType, fullness: Int, emptyLocCount: Int): RowFullness {
        return RowFullness(row, 1, 1, fullness.toDouble(), emptyLocCount, idType)
    }

    private fun sortRowsByPriority(rows: List<RecommendedRow>): List<String> {
        return rows.sortedBy { it.priority }.map { it.row }
    }

    companion object {
        private const val ZONE = "MEZ-1"
    }
}
