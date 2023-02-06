package ru.yandex.market.sc.test.network.domain.courier

import kotlinx.coroutines.runBlocking
import ru.yandex.market.sc.test.network.repository.manual.ManualCourierRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestCourierUseCases @Inject constructor(
    private val manualCourierRepository: ManualCourierRepository,
) {
    fun createCourierCipherId(courierId: Long): String = runBlocking {
        manualCourierRepository.getEncryptedCourierQrCode(
            courierUid = courierId,
            randomNumber = 234,
            shipmentDate = "2022-01-10"
        )
    }
}
