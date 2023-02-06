package ru.yandex.market.mapi.engine.pumpkin

import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mapi.core.pumpkin.PumpkinConstants
import ru.yandex.market.mapi.core.util.asResource
import ru.yandex.market.mapi.core.util.assertJson
import ru.yandex.market.mapi.db.PumpkinRepository
import ru.yandex.market.mapi.engine.AbstractEngineTest
import kotlin.test.Test
import kotlin.test.assertNull

/**
 * @author Ilya Kislitsyn / ilyakis@ / 10.06.2022
 */
class PumpkinStorageTest : AbstractEngineTest() {

    @Autowired
    private lateinit var pumpkinStorage: PumpkinStorage

    @Autowired
    private lateinit var pumpkinRepository: PumpkinRepository

    @Test
    fun testRefresh() {
        // set pumpkin to repo
        whenever(pumpkinRepository.getAll()).thenReturn(
            mapOf(
                PumpkinConstants.PUMPKIN_MAIN to "/engine/pumpkin/noResponse.json".asResource(),
                "other" to "/engine/pumpkin/sectionWithConditionsPumpkin.json".asResource(),
            )
        )

        assertNull(pumpkinStorage.get(PumpkinConstants.PUMPKIN_MAIN))

        // refresh storage
        pumpkinStorage.refreshCache()
        assertJson(
            pumpkinStorage.get(PumpkinConstants.PUMPKIN_MAIN)!!,
            "/engine/pumpkin/noResponse.json"
        )
        assertJson(
            pumpkinStorage.get("other")!!,
            "/engine/pumpkin/sectionWithConditionsPumpkin.json"
        )

        // change contents
        whenever(pumpkinRepository.getAll()).thenReturn(
            mapOf(
                "other" to "/engine/pumpkin/noResponse.json".asResource(),
            )
        )

        // not refreshed yet
        assertJson(
            pumpkinStorage.get(PumpkinConstants.PUMPKIN_MAIN)!!,
            "/engine/pumpkin/noResponse.json"
        )

        // now refreshed
        pumpkinStorage.refreshCache()
        assertNull(pumpkinStorage.get(PumpkinConstants.PUMPKIN_MAIN))
        assertJson(
            pumpkinStorage.get("other")!!,
            "/engine/pumpkin/noResponse.json"
        )
    }
}