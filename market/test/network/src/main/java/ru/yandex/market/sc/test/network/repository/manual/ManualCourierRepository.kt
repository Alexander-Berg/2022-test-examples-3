package ru.yandex.market.sc.test.network.repository.manual

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.yandex.market.sc.test.network.api.SortingCenterManualService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManualCourierRepository @Inject constructor(
    private val sortingCenterManualService: SortingCenterManualService,
    private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun getEncryptedCourierQrCode(
        courierUid: Long,
        randomNumber: Long,
        shipmentDate: String
    ): String = withContext(ioDispatcher) {
        sortingCenterManualService.getEncryptedCourierQrCode(
            courierUid = courierUid,
            randomNumber = randomNumber,
            shipmentDate = shipmentDate
        )
    }
}