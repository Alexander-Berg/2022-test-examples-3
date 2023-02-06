package ru.yandex.market.wms.core.controller.planned

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.util.LinkedMultiValueMap
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.core.base.dto.IndirectActivityPlanDto
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.stream.Stream

class GetPlannedIndirectActivityControllerTest : IntegrationTest() {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    // Я знаю, мы в вмс любим тесты, тут их 26 за раз
    @ParameterizedTest
    @MethodSource
    @DatabaseSetup("/controller/planned-indirect-activities/db/plannedActivityInit.xml")
    fun getPlannedTest(
        users: Set<String> = emptySet(),
        activities: Set<String> = emptySet(),
        startTime: LocalDateTime? = null,
        expected: Set<IndirectActivityPlanDto>
    ) {
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .get("/indirect-activities/planned")
                    .queryParams(
                        LinkedMultiValueMap<String, String>().apply {
                            users.forEach { add("users", it) }
                            activities.forEach { add("activities", it) }
                            startTime?.let { add("startTime", it.format(DateTimeFormatter.ISO_DATE_TIME)) }
                        }
                    )
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.content().json(objectMapper.writeValueAsString(expected)))
    }

    companion object {

        @JvmStatic
        fun getPlannedTest() = Stream.of(
            // Без фильтров
            Arguments.of(
                emptySet<String>(),
                emptySet<String>(),
                null,
                setOf(firstPlanDto(), secondPlanDto())
            ),
            // Фильтр по юзеру 1
            Arguments.of(
                setOf(firstPlanDto().username),
                emptySet<String>(),
                null,
                setOf(firstPlanDto())
            ),
            // Фильтр по юзеру 2
            Arguments.of(
                setOf(secondPlanDto().username),
                emptySet<String>(),
                null,
                setOf(secondPlanDto())
            ),
            // Фильтр по двум юзерам
            Arguments.of(
                setOf(firstPlanDto().username, secondPlanDto().username),
                emptySet<String>(),
                null,
                setOf(firstPlanDto(), secondPlanDto())
            ),
            // Фильтр по активности 1
            Arguments.of(
                emptySet<String>(),
                setOf(firstPlanDto().activity),
                null,
                setOf(firstPlanDto())
            ),
            // Фильтр по активности 2
            Arguments.of(
                emptySet<String>(),
                setOf(secondPlanDto().activity),
                null,
                setOf(secondPlanDto())
            ),
            // Фильтр по двум активностям
            Arguments.of(
                emptySet<String>(),
                setOf(firstPlanDto().activity, secondPlanDto().activity),
                null,
                setOf(firstPlanDto(), secondPlanDto())
            ),
            // Фильтр по юзеру 1 и активности 1
            Arguments.of(
                setOf(firstPlanDto().username),
                setOf(firstPlanDto().activity),
                null,
                setOf(firstPlanDto())
            ),
            // Фильтр по юзеру 2 и активности 2
            Arguments.of(
                setOf(secondPlanDto().username),
                setOf(secondPlanDto().activity),
                null,
                setOf(secondPlanDto())
            ),
            // Фильтр по юзеру 1 и активности 2
            Arguments.of(
                setOf(firstPlanDto().username),
                setOf(secondPlanDto().activity),
                null,
                emptySet<Any>()
            ),
            // Фильтр по юзеру 2 и активности 1
            Arguments.of(
                setOf(secondPlanDto().username),
                setOf(firstPlanDto().activity),
                null,
                emptySet<Any>()
            ),
            // Фильтр по startTime < минимального
            Arguments.of(
                emptySet<String>(),
                emptySet<String>(),
                LocalDateTime.of(2021, 3, 30, 22, 26, 54),
                setOf(firstPlanDto(), secondPlanDto())
            ),
            // Фильтр по startTime < 2, но больше 1
            Arguments.of(
                emptySet<String>(),
                emptySet<String>(),
                LocalDateTime.of(2022, 4, 2, 22, 26, 54),
                setOf(secondPlanDto())
            ),
            // Фильтр по startTime > максимального
            Arguments.of(
                emptySet<String>(),
                emptySet<String>(),
                LocalDateTime.of(2022, 4, 4, 22, 26, 54),
                emptySet<Any>()
            ),
            // Фильтр по юзеру 1 и startTime < минимального
            Arguments.of(
                setOf(firstPlanDto().username),
                emptySet<String>(),
                LocalDateTime.of(2021, 3, 30, 22, 26, 54),
                setOf(firstPlanDto())
            ),
            // Фильтр по юзеру 2 и startTime < минимального
            Arguments.of(
                setOf(secondPlanDto().username),
                emptySet<String>(),
                LocalDateTime.of(2021, 3, 30, 22, 26, 54),
                setOf(secondPlanDto())
            ),
            // Фильтр по активности 1 и startTime < минимального
            Arguments.of(
                emptySet<String>(),
                setOf(firstPlanDto().activity),
                LocalDateTime.of(2021, 3, 30, 22, 26, 54),
                setOf(firstPlanDto())
            ),
            // Фильтр по активности 2 и startTime < минимального
            Arguments.of(
                emptySet<String>(),
                setOf(secondPlanDto().activity),
                LocalDateTime.of(2021, 3, 30, 22, 26, 54),
                setOf(secondPlanDto())
            ),
            // Фильтр по startTime < 2, но больше 1 и по юзеру 1
            Arguments.of(
                setOf(firstPlanDto().username),
                emptySet<String>(),
                LocalDateTime.of(2022, 4, 2, 22, 26, 54),
                emptySet<Any>()
            ),
            // Фильтр по startTime < 2, но больше 1 и по юзеру 2
            Arguments.of(
                setOf(secondPlanDto().username),
                emptySet<String>(),
                LocalDateTime.of(2022, 4, 2, 22, 26, 54),
                setOf(secondPlanDto())
            ),
            // Фильтр по активности 1 и startTime < 2, но больше 1
            Arguments.of(
                emptySet<String>(),
                setOf(firstPlanDto().activity),
                LocalDateTime.of(2022, 4, 2, 22, 26, 54),
                emptySet<Any>()
            ),
            // Фильтр по активности 2 и startTime < 2, но больше 1
            Arguments.of(
                emptySet<String>(),
                setOf(secondPlanDto().activity),
                LocalDateTime.of(2022, 4, 2, 22, 26, 54),
                setOf(secondPlanDto())
            ),
            // Фильтр по startTime > максимального и юзеру 1
            Arguments.of(
                setOf(firstPlanDto().username),
                emptySet<String>(),
                LocalDateTime.of(2022, 4, 4, 22, 26, 54),
                emptySet<Any>()
            ),
            // Фильтр по startTime > максимального и юзеру 2
            Arguments.of(
                setOf(secondPlanDto().username),
                emptySet<String>(),
                LocalDateTime.of(2022, 4, 4, 22, 26, 54),
                emptySet<Any>()
            ),
            // Фильтр по startTime > максимального и активности 1
            Arguments.of(
                emptySet<String>(),
                setOf(firstPlanDto().activity),
                LocalDateTime.of(2022, 4, 4, 22, 26, 54),
                emptySet<Any>()
            ),
            // Фильтр по startTime > максимального и активности 2
            Arguments.of(
                emptySet<String>(),
                setOf(secondPlanDto().activity),
                LocalDateTime.of(2022, 4, 4, 22, 26, 54),
                emptySet<Any>()
            ),
        )

        private fun firstPlanDto() = IndirectActivityPlanDto(
            id = 1,
            username = "testUser",
            activity = "testActivity",
            startTime = LocalDateTime.of(2022, 4, 2, 0, 30, 7)
        )

        private fun secondPlanDto() = IndirectActivityPlanDto(
            id = 2,
            username = "testUser2",
            activity = "testActivity2",
            startTime = LocalDateTime.of(2022, 4, 4, 1, 26, 54)
        )
    }
}
