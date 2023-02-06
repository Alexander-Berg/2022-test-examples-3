package ru.yandex.market.mapi.db

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class PumpkinRepositoryTest : AbstractDbTest() {

    @Autowired
    private lateinit var repository: PumpkinRepository

    @Test
    fun testUpsertValue() {
        Assertions.assertNull(
            repository.getValue("testName1")
        )

        repository.upsertValue("testName1", "testValue1")

        Assertions.assertEquals(
            "testValue1",
            repository.getValue("testName1")
        )

        repository.upsertValue("testName1", "testValue2")

        Assertions.assertEquals(
            "testValue2",
            repository.getValue("testName1")
        )
    }

    @Test
    fun getAllValues() {
        assertEquals(0, repository.getAll()?.size)

        repository.upsertValue("testName1", "testValue1")
        repository.upsertValue("testName2", "testValue2")

        val actualValues = repository.getAll()

        assertEquals(2, actualValues?.size)

        Assertions.assertEquals(
            mapOf(
                "testName1" to "testValue1",
                "testName2" to "testValue2"
            ),
            actualValues
        )
    }
}
