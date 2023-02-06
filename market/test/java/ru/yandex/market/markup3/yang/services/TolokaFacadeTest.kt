package ru.yandex.market.markup3.yang.services

import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.util.ReflectionTestUtils
import ru.yandex.market.markup3.core.TolokaSource
import ru.yandex.market.markup3.core.TolokaTask
import ru.yandex.market.markup3.testutils.BaseAppTest
import ru.yandex.toloka.client.v1.SearchResult
import ru.yandex.toloka.client.v1.aggregatedsolutions.AggregatedSolution
import ru.yandex.toloka.client.v1.aggregatedsolutions.AggregatedSolutionSearchRequest
import ru.yandex.toloka.client.v1.pool.Pool
import ru.yandex.toloka.client.v1.task.TaskCreateRequestParameters
import java.lang.Integer.max
import java.math.BigDecimal
import java.util.Date

class TolokaFacadeTest : BaseAppTest() {
    @Autowired
    private lateinit var tolokaFacadeFactory: TolokaFacadeFactory

    @Test
    fun `test find all`() {
        val tolokaFacade = tolokaFacadeFactory.get(TolokaSource.YANG)
        val allSolutions = mutableListOf<AggregatedSolution>()
        for (i in 1..10000) {
            val solution = AggregatedSolution()
            ReflectionTestUtils.setField(solution, "taskId", i.toString())
            allSolutions.add(solution)
        }

        fun testSearch(request: AggregatedSolutionSearchRequest): SearchResult<AggregatedSolution> {
            val searchResult = SearchResult<AggregatedSolution>()

            val params = request.queryParameters
            val limit = params["limit"].toString().toInt()
            val sort = params["sort"]
            sort shouldBe "task_id" //should be positive if we take gt
            val from = params["task_id_gt"]?.toString()?.toInt() ?: 0
            val to = max(from + limit, allSolutions.size)
            val hasMore = to < allSolutions.size
            ReflectionTestUtils.setField(searchResult, "items", allSolutions.subList(from, to))
            ReflectionTestUtils.setField(searchResult, "hasMore", hasMore)

            return searchResult
        }

        val findAll = tolokaFacade.findAll<AggregatedSolutionSearchRequest, AggregatedSolution>(
            limit = TolokaFacade.LARGE_LIMIT,
            requestCustomizer = { limit, lastVal ->
                val searchReqBuilder = AggregatedSolutionSearchRequest.make()
                searchReqBuilder.limit(limit)
                searchReqBuilder.sort().byTaskId().asc()

                lastVal?.let { searchReqBuilder.range().byTaskId(lastVal.taskId).gt() }
                searchReqBuilder.done()
            },
            search = { request -> testSearch(request) }
        )

        findAll shouldHaveSize allSolutions.size
        findAll.map { it.taskId.toInt() }.sorted() shouldContainInOrder (1..10000).toList()
    }

    @Test
    fun `test batched createTasks`() {
        val tolokaFacade = tolokaFacadeFactory.get(TolokaSource.TOLOKA)
        val pool = tolokaFacade.createPool(
            Pool(
                "prj", "pr_name", true, Date(),
                BigDecimal.ONE, 1, true, null
            )
        )
        val request = generateSequence(0) { if (it >= 1111) null else it + 1 }
            .map { index ->
                TolokaTask(pool.id, mapOf("index" to index))
            }
            .toList()
        val params = TaskCreateRequestParameters().apply {
            allowDefaults = true
            openPool = true
        }

        val result = tolokaFacade.createTasks(request, params)

        result.forEach { (index, task) ->
            val rqTask = request[index]
            rqTask.inputValues shouldBe task.inputValues
        }
    }
}
