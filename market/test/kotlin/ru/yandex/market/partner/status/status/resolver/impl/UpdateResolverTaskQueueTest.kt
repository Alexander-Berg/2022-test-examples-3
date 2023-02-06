package ru.yandex.market.partner.status.status.resolver.impl

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.partner.status.AbstractFunctionalTest
import ru.yandex.market.partner.status.status.resolver.UpdateResolverService
import ru.yandex.market.partner.status.status.resolver.UpdateResolverTaskQueue
import ru.yandex.market.partner.status.status.resolver.model.ProgramResolverType

/**
 * Тесты для [UpdateResolverTaskQueue].
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class UpdateResolverTaskQueueTest : AbstractFunctionalTest() {

    @Autowired
    private lateinit var updateResolverServices: List<UpdateResolverService>

    private lateinit var queue: UpdateResolverTaskQueue

    @BeforeEach
    fun init() {
        queue = UpdateResolverTaskQueueImpl(updateResolverServices, 3)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "-1",
            "3"
        ]
    )
    fun `invalid chunk, add`(chunk: Int) {
        Assertions.assertThatThrownBy { queue.add(ProgramResolverType.FBS_SORTING_CENTER, chunk, listOf(10L)) }
            .hasMessageStartingWith("Invalid chunk")
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "-1",
            "3"
        ]
    )
    fun `invalid chunk, take`(chunk: Int) {
        Assertions.assertThatThrownBy { runBlocking { queue.take(chunk) } }
            .hasMessageStartingWith("Invalid chunk")
    }

    @Test
    fun `add then take`() {
        queue.add(ProgramResolverType.FBS_SORTING_CENTER, 1, listOf(1L, 2L))
        queue.add(ProgramResolverType.FBS_SORTING_CENTER, 2, listOf(3L))
        val actualPartnerIds = runBlocking {
            val availableTasks = queue.take(1)
            availableTasks.getByType(ProgramResolverType.FBS_SORTING_CENTER).toList()
        }

        Assertions.assertThat(actualPartnerIds)
            .containsExactlyInAnyOrder(1L, 2L)
    }

    @Test
    fun `double add into single chunk will not block the thread`() {
        queue.add(ProgramResolverType.FBS_SORTING_CENTER, 1, listOf(1L, 2L))
        queue.add(ProgramResolverType.FBS_SORTING_CENTER, 1, listOf(3L))
        val actualPartnerIds = runBlocking {
            val availableTasks = queue.take(1)
            availableTasks.getByType(ProgramResolverType.FBS_SORTING_CENTER).toList()
        }

        Assertions.assertThat(actualPartnerIds)
            .containsExactlyInAnyOrder(1L, 2L, 3L)
    }
}
