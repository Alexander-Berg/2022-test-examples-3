package ru.yandex.market.abo.core.antifraud.v2.service

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.core.antifraud.v2.model.AntiFraudClchResult
import ru.yandex.market.abo.core.antifraud.v2.model.AntiFraudCloneFeature.UNKNOWN
import ru.yandex.market.abo.core.antifraud.v2.model.AntiFraudGlueContacts

/**
 * @author zilzilok
 */
class AntiFraudClchResultServiceTest @Autowired constructor(
    private val antiFraudClchResultService: AntiFraudClchResultService
) : EmptyTest() {

    @Test
    fun crud() {
        val result1 = createCloneCheckResult(1L, 2L)
        val result2 = createCloneCheckResult(2L, 3L)

        antiFraudClchResultService.save(listOf(result1, result2))
        val savedResults = antiFraudClchResultService.findAllCheckResultsForShops(setOf(1L, 2L))

        assertTrue(savedResults.contains(result1))
        assertTrue(savedResults.contains(result2))
    }

    private fun createCloneCheckResult(shopId: Long, cloneShopId: Long) = AntiFraudClchResult().apply {
        compositeId = AntiFraudClchResult.Key(shopId, cloneShopId)
        cloneFeatures = arrayOf(UNKNOWN)
        distance = 1.0
        glueContacts = AntiFraudGlueContacts().apply {
            sameContacts = arrayOf("123@123.ru", "+777777777")
            sameJurInfo = null
        }
    }
}
