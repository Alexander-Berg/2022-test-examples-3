package ru.yandex.market.wms.core.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import ru.yandex.market.wms.common.spring.dao.entity.LotLocId
import ru.yandex.market.wms.common.spring.dao.implementation.IdDao
import ru.yandex.market.wms.common.spring.dao.implementation.LotIdDetailDao
import ru.yandex.market.wms.common.spring.dao.implementation.LotLocIdDao
import ru.yandex.market.wms.common.spring.dao.implementation.SerialInventoryDao
import ru.yandex.market.wms.common.spring.service.SerialInventoryService
import ru.yandex.market.wms.common.spring.service.balance.BalanceService
import ru.yandex.market.wms.core.base.request.ValidateIdRequest
import ru.yandex.market.wms.core.configuration.authentication.UserProvider
import ru.yandex.market.wms.core.dao.CoreSerialInventoryDao
import ru.yandex.market.wms.core.dao.LocDao
import java.math.BigDecimal

class IdServiceTest {
    @InjectMocks
    lateinit var service: IdService

    @Mock
    lateinit var balancesService: BalanceService
    @Mock
    lateinit var securityUserProvider: ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider
    @Mock
    lateinit var lotLocIdDao: LotLocIdDao
    @Mock
    lateinit var idDao: IdDao
    @Mock
    lateinit var coreSerialInventoryDao: CoreSerialInventoryDao
    @Mock
    lateinit var serialInventoryDao: SerialInventoryDao
    @Mock
    lateinit var serialInventoryService: SerialInventoryService
    @Mock
    lateinit var userProvider: UserProvider
    @Mock
    lateinit var lotIdDetailDao: LotIdDetailDao
    @Mock
    lateinit var coreSerialInventoryService: CoreSerialInventoryService
    @Mock
    lateinit var nestingService: NestingService
    @Mock
    lateinit var locDao: LocDao

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        Mockito.`when`(idDao.isExists(anyString())).thenReturn(true)
        Mockito.`when`(coreSerialInventoryDao.countById(anyString())).thenReturn(BigDecimal.ONE)
        Mockito.`when`(lotLocIdDao.findById(anyString())).thenReturn(listOf(
                LotLocId.builder()
                        .qtyAllocated(BigDecimal.ZERO)
                        .qtyPicked(BigDecimal.ZERO)
                        .build()
        ))
        Mockito.`when`(lotIdDetailDao.findSerialNumbersWithOutbounds(anyList())).thenReturn(listOf())
    }

    @Test
    fun `check only existing`() {
        service.validateId(ValidateIdRequest("some ID"))

        Mockito.verify(idDao, Mockito.times(1)).isExists("some ID")
    }

    @Test
    fun `check existing and qtyPicked + qtyAllocated above 0`() {
        service.validateId(ValidateIdRequest("some ID", checkQty = true))

        Mockito.verify(idDao, Mockito.times(1)).isExists("some ID")
        Mockito.verify(lotLocIdDao, Mockito.times(1)).findById("some ID")
    }

    @Test
    fun `check existing and emptiness`() {
        service.validateId(ValidateIdRequest("some ID", checkIfEmpty = true))

        Mockito.verify(idDao, Mockito.times(1)).isExists("some ID")
        Mockito.verify(coreSerialInventoryDao, Mockito.times(1)).countById("some ID")
    }

    @Test
    fun `all checks`() {
        service.validateId(ValidateIdRequest("some ID", checkQty = true, checkIfEmpty = true))

        Mockito.verify(idDao, Mockito.times(1)).isExists("some ID")
        Mockito.verify(lotLocIdDao, Mockito.times(1)).findById("some ID")
        Mockito.verify(coreSerialInventoryDao, Mockito.times(1)).countById("some ID")
    }
}
