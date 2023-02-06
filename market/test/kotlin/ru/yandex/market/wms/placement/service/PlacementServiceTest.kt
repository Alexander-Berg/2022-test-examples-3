package ru.yandex.market.wms.placement.service

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import ru.yandex.market.wms.common.spring.dao.entity.Loc
import ru.yandex.market.wms.common.spring.dao.entity.SerialInventory
import ru.yandex.market.wms.common.spring.dao.implementation.LocDAO
import ru.yandex.market.wms.common.spring.dao.implementation.PalletDao
import ru.yandex.market.wms.common.spring.dao.implementation.SerialInventoryDao
import ru.yandex.market.wms.common.spring.exception.NotFoundException
import ru.yandex.market.wms.common.spring.service.balance.BalanceService
import ru.yandex.market.wms.common.spring.service.balance.MovingCause
import ru.yandex.market.wms.placement.service.impl.PlacementServiceImpl
import java.util.Optional

class PlacementServiceTest {
    @InjectMocks
    lateinit var service: PlacementServiceImpl

    @Mock
    lateinit var balanceService: BalanceService
    @Mock
    lateinit var palletDao: PalletDao
    @Mock
    lateinit var locDao: LocDAO
    @Mock
    lateinit var securityDataProvider: ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider
    @Mock
    lateinit var serialInventoryDao: SerialInventoryDao

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun palletNotFoundTest() {
        whenever(locDao.findById(ArgumentMatchers.anyString())).thenReturn(Optional.of(Loc.builder().build()))
        whenever(serialInventoryDao.getAnyByUnitId(ArgumentMatchers.anyString())).thenReturn(Optional.empty())
        whenever(palletDao.existsPallet(ArgumentMatchers.anyString())).thenReturn(false)

        Assertions.assertThrows(NotFoundException::class.java) {
            service.placePackaging("123", "qwerty")
        }

        verifyNoInteractions(balanceService)
    }

    @Test
    fun locationNotFoundTest() {
        whenever(palletDao.existsPallet(ArgumentMatchers.anyString())).thenReturn(true)
        whenever(locDao.findById(ArgumentMatchers.anyString())).thenReturn(Optional.empty())

        Assertions.assertThrows(NotFoundException::class.java) {
            service.placePackaging("123", "qwerty")
        }

        verifyNoInteractions(balanceService)
    }

    @Test
    fun successStoryTest_whenLocationAndPalletFound() {
        whenever(locDao.findById(ArgumentMatchers.anyString())).thenReturn(Optional.of(Loc.builder().build()))
        whenever(palletDao.existsPallet(ArgumentMatchers.anyString())).thenReturn(true)
        whenever(securityDataProvider.user).thenReturn("some user")

        val palletId = "123"
        val toLoc = "qwerty"

        service.placePackaging(palletId, toLoc)

        val movingCause = MovingCause.builder()
                .key(palletId)
                .type("TO")
                .initiator(securityDataProvider.user)
                .build()

        verify(balanceService, times(1))
                .moveContainer(palletId, toLoc, movingCause)
    }

    @Test
    fun successStoryTest_whenLocationAndSerialInventoryFound() {
        whenever(locDao.findById(ArgumentMatchers.anyString())).thenReturn(Optional.of(Loc.builder().build()))
        whenever(palletDao.existsPallet(ArgumentMatchers.anyString())).thenReturn(false)
        whenever(serialInventoryDao.getAnyByUnitId(ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(SerialInventory.builder().build()))
        whenever(securityDataProvider.user).thenReturn("some user")

        val palletId = "123"
        val toLoc = "qwerty"

        service.placePackaging(palletId, toLoc)

        val movingCause = MovingCause.builder()
                .key(palletId)
                .type("TO")
                .initiator(securityDataProvider.user)
                .build()

        verify(balanceService, times(1))
                .moveContainer(palletId, toLoc, movingCause)
    }

    @Test
    fun successStoryTest_whenLocationAndPalletAndSerialInventoryFound() {
        whenever(locDao.findById(ArgumentMatchers.anyString())).thenReturn(Optional.of(Loc.builder().build()))
        whenever(palletDao.existsPallet(ArgumentMatchers.anyString())).thenReturn(true)
        whenever(serialInventoryDao.getAnyByUnitId(ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(SerialInventory.builder().build()))
        whenever(securityDataProvider.user).thenReturn("some user")

        val palletId = "123"
        val toLoc = "qwerty"

        service.placePackaging(palletId, toLoc)

        val movingCause = MovingCause.builder()
                .key(palletId)
                .type("TO")
                .initiator(securityDataProvider.user)
                .build()

        verify(balanceService, times(1))
                .moveContainer(palletId, toLoc, movingCause)
    }
}
