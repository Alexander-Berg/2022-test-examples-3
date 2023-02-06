package ru.yandex.market.tpl.e2e.domain.feature.testcase

import ru.yandex.market.tpl.e2e.data.feature.testcase.TestCaseDto
import ru.yandex.market.tpl.e2e.data.feature.testcase.TestCaseRepository
import ru.yandex.market.tpl.e2e.domain.feature.remote.GetTestPalmTokenUseCase

class GetTestCaseUseCase(
    private val getTestPalmTokenUseCase: GetTestPalmTokenUseCase,
    private val testCaseRepository: TestCaseRepository,
) {
    suspend fun getTestCase(id: String): TestCaseDto {
        return testCaseRepository.getTestCase(getTestPalmTokenUseCase.getToken(), id)
    }
}