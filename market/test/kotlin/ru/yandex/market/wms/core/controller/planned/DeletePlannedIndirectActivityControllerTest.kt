package ru.yandex.market.wms.core.controller.planned

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.util.LinkedMultiValueMap
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.core.dao.IndirectActivityPlanDao
import java.util.stream.Stream

class DeletePlannedIndirectActivityControllerTest : IntegrationTest() {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @SpyBean
    @Autowired
    private lateinit var indirectActivityPlanDao: IndirectActivityPlanDao

    @ParameterizedTest
    @MethodSource
    @DatabaseSetup("/controller/planned-indirect-activities/db/plannedActivityInit.xml")
    fun delete(ids: Set<Int>, expectedIds: Set<Int>, expectedResponseCode: Int) {
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .delete("/indirect-activities/planned")
                    .queryParams(
                        LinkedMultiValueMap<String, String>().apply {
                            ids.forEach { add("ids", it.toString()) }
                        }
                    )
            )
            .andExpect(MockMvcResultMatchers.status().`is`(expectedResponseCode))
            .apply {
                if (expectedResponseCode == 200) {
                    andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                    andExpect(MockMvcResultMatchers.content().json(objectMapper.writeValueAsString(expectedIds)))
                }
            }

        Assertions.assertIterableEquals(
            ALL_IDS - expectedIds,
            indirectActivityPlanDao.get(emptySet(), emptySet(), emptySet()).map { it.id }
        )
    }

    companion object {

        private val ALL_IDS = setOf(1, 2)

        @JvmStatic
        fun delete() = Stream.of(
            // Стерли все
            argument(
                setOf(1, 2),
                setOf(1, 2)
            ),
            // Стерли первый
            argument(
                setOf(1),
                setOf(1)
            ),
            // Стерли второй
            argument(
                setOf(2),
                setOf(2)
            ),
            // Ничего не стерли
            argument(
                emptySet(),
                emptySet(),
                400
            ),
            // Стерли то, чего не было
            argument(
                setOf(3),
                emptySet()
            ),
            // Стерли то, чего не было + первый
            argument(
                setOf(3, 1),
                setOf(1)
            ),
            // Стерли то, чего не было + второй
            argument(
                setOf(2, 3),
                setOf(2)
            ),
            // Стерли то, чего не было + все
            argument(
                setOf(2, 3, 1),
                setOf(1, 2)
            ),
        )

        private fun argument(
            ids: Set<Int>,
            expectedIds: Set<Int>,
            expectedResponseCode: Int = 200
        ) = Arguments.of(
            ids,
            expectedIds,
            expectedResponseCode
        )
    }
}
