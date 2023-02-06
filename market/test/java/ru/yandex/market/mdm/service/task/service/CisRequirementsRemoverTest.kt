package ru.yandex.market.mdm.service.task.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.given
import com.nhaarman.mockitokotlin2.verify
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI

@RunWith(MockitoJUnitRunner::class)
class CisRequirementsRemoverTest {

    @Mock
    lateinit var checkouterAPI: CheckouterAPI

    lateinit var cisRequirementsRemover: CisRequirementsRemover

    @Before
    fun init() {
        cisRequirementsRemover =
            CisRequirementsRemover(checkouterAPI)
    }

    @Test
    fun `should call checkouter and return answer`() {
        // given
        val request = CisRequirementsRemover.RemoveCisRequirementsRequest(listOf(1), true)
        val checkouterResponse = CheckouterRemovingCisRequirementsResponse()
        checkouterResponse.addEditedOrderIds(setOf(15))
        checkouterResponse.addOrderIdsWithError(setOf(25))
        given(checkouterAPI.removeCisRequirement(any())).willReturn(checkouterResponse)

        // when
        val response = cisRequirementsRemover.removeCisRequirements(request)

        // then
        response.failedOrderIds shouldContainExactly listOf(25)
        response.successfulOrderIds shouldContainExactly listOf(15)
        response.state shouldBe CisRequirementsRemover.TaskState.SEMI_FAILED

        // and
        val requestCaptor = argumentCaptor<CheckouterRemovingCisRequirementsRequest>()
        verify(checkouterAPI).removeCisRequirement(requestCaptor.capture())
        requestCaptor.firstValue.mskuValues shouldContainExactly setOf(1)
        requestCaptor.firstValue.isCheckOnly shouldBe true
    }
}
