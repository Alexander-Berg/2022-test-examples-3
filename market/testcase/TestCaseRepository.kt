package ru.yandex.market.tpl.e2e.data.feature.testcase

import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.yandex.market.tpl.e2e.data.remote.TestPalmApi

class TestCaseRepository(private val moshi: Moshi, private val testPalmApi: TestPalmApi) {
    private val idRegex = Regex("^(?<projectId>.+)-(?<testCaseId>\\d+)\$")

    suspend fun getTestCase(token: String, id: String): TestCaseDto {
        return withContext(Dispatchers.IO) {
            val (projectId, testCaseId) = requireNotNull(idRegex.matchEntire(id)) {
                "Id тест кейса $id не соответствует формату projectId-testCaseId"
            }.destructured

            testPalmApi.getTestCases(
                token = "OAuth $token",
                projectId = projectId,
                filterExpression = serializeFilterExpression(
                    FilterExpressionDto(
                        type = "EQ",
                        key = "id",
                        value = testCaseId,
                    )
                ),
            ).first()
        }
    }

    private val filterExpressionAdapter by lazy { moshi.adapter(FilterExpressionDto::class.java) }

    private fun serializeFilterExpression(filterExpressionDto: FilterExpressionDto): String {
        return filterExpressionAdapter.toJson(filterExpressionDto)
    }
}