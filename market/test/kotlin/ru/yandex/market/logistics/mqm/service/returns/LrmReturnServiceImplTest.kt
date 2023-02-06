package ru.yandex.market.logistics.mqm.service.returns

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import ru.yandex.market.logistics.mqm.repository.LrmReturnRepository

internal class LrmReturnServiceImplTest {

    @Test
    fun findByExternalOrderIdWithTwoReturns() {
        val a = mock<LrmReturnRepository>()
        whenever(a.findByOrderExternalId(any())).thenReturn(listOf(mock(), mock()))
        val lrmReturnServiceImpl = LrmReturnServiceImpl(a)
        val findByExternalOrderId = lrmReturnServiceImpl.findByExternalOrderId("1")
        assertSoftly {
            findByExternalOrderId shouldNotBe null
        }
    }

    @Test
    fun findForEmptyList(){
        val a = mock<LrmReturnRepository>()
        whenever(a.findByOrderExternalId(any())).thenReturn(listOf())
        val lrmReturnServiceImpl = LrmReturnServiceImpl(a)
        assertSoftly {
            lrmReturnServiceImpl.findByExternalOrderId("1") shouldBe null
        }
    }
}
