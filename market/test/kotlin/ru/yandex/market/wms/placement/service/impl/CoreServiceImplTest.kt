package ru.yandex.market.wms.placement.service.impl


import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils
import ru.yandex.market.wms.common.model.enums.ItrnSourceType
import ru.yandex.market.wms.common.model.enums.LocationType
import ru.yandex.market.wms.common.spring.dao.entity.Loc
import ru.yandex.market.wms.common.spring.dao.entity.SerialInventory
import ru.yandex.market.wms.common.spring.dao.implementation.IdDao
import ru.yandex.market.wms.common.spring.dao.implementation.LocDAO
import ru.yandex.market.wms.common.spring.dao.implementation.LotIdDetailDao
import ru.yandex.market.wms.common.spring.dao.implementation.LotLocIdDao
import ru.yandex.market.wms.common.spring.dao.implementation.SerialInventoryDao
import ru.yandex.market.wms.common.spring.service.SerialInventoryService
import ru.yandex.market.wms.common.spring.service.UserService
import ru.yandex.market.wms.common.spring.service.balance.BalanceService
import ru.yandex.market.wms.core.client.CoreClient

internal class CoreServiceImplTest {
    @InjectMocks
    lateinit var service: CoreServiceImpl

    @Mock
    lateinit var idDao: IdDao
    @Mock
    lateinit var locDAO: LocDAO
    @Mock
    lateinit var lotLocIdDao: LotLocIdDao
    @Mock
    lateinit var balanceService: BalanceService
    @Mock
    lateinit var userService: UserService
    @Mock
    lateinit var serialInventoryDao: SerialInventoryDao
    @Mock
    lateinit var serialInventoryService: SerialInventoryService
    @Mock
    lateinit var userProvider: ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider
    @Mock
    lateinit var lotIdDetailDao: LotIdDetailDao
    @Mock
    lateinit var coreClient: CoreClient

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun moveSerialInventoriesInContainerList() {
        val ids = listOf("123", "234", "345", "456", "567", "678", "789")

        val serialsQtyOnId = 10;
        val serials = ids.map { generateSerialsForId(it,  serialsQtyOnId) }.flatten()
        val moveSerialsBatchSize = 25
        val serialsQtyTotal = ids.size * serialsQtyOnId

        ReflectionTestUtils.setField(service, "moveSerialsBatchSize", moveSerialsBatchSize)
        whenever(serialInventoryDao.getByIds(ids)).thenReturn(serials)
        whenever(userProvider.user).thenReturn("user")

        service.moveSerialInventoriesInContainerList(ids, "1-01", 111, ItrnSourceType.PLACEMENT_TAKE)

        verify(serialInventoryService, times(serialsQtyTotal))
                .createMovingItem(any(), anyBoolean())

        verify(serialInventoryService, times(4))
                .moveToLocWithSameId(anyList(), anyString(), any(), anyString())
    }


    private fun generateSerialsForId(id: String, qty: Int): List<SerialInventory> {
        val serialList = mutableListOf<SerialInventory>()
        repeat(qty) { i ->
            val serial = SerialInventory.builder()
                    .serialNumber(id + i)
                    .id(id)
                    .build()

            serialList.add(serial)
        }

        return serialList
    }

    @Test
    fun getPlacementBufLocsTest() {
        whenever(locDAO.findByType(LocationType.PLACEMENT_BUF)).thenReturn(listOf(
            Loc.builder()
                .loc("OUTBUF1")
                .putawayzone("1")
                .build(),
            Loc.builder()
                .loc("OUTBUF2")
                .putawayzone("2")
                .build()
        ))

        whenever(locDAO.findInputBuffLocs(LocationType.ST_IN_BUF, listOf("1", "2"))).thenReturn(listOf(
            Loc.builder()
                .loc("INFKBUF1")
                .putawayzone("1")
                .build()
        ))

        val res = service.getPlacementBufLocs()
        Assertions.assertEquals(3, res.size)
        Assertions.assertTrue(res.containsAll(listOf("OUTBUF1", "OUTBUF2", "INFKBUF1")))
    }
}
