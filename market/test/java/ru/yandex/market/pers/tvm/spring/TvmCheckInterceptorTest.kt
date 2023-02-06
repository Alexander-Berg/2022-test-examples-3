package ru.yandex.market.pers.tvm.spring

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.pers.test.common.AbstractPersWebTest
import ru.yandex.market.pers.test.common.PersTestMocksHolder
import ru.yandex.market.pers.tvm.TvmChecker

/**
 * @author Ilya Kislitsyn / ilyakis@ / 18.03.2021
 */
@Import(TvmCheckerConfiguration::class)
class TvmCheckInterceptorTest : AbstractPersWebTest() {
    companion object {
        private const val TICKET_OK = "ticket"
        private const val TICKET_ERR = "ticket_err"
        val OK = MockMvcResultMatchers.status().is2xxSuccessful
        val ERR_4XX = MockMvcResultMatchers.status().is4xxClientError
        val ERR_5XX = MockMvcResultMatchers.status().is5xxServerError
    }

    @Autowired
    lateinit var tvmChecker: TvmChecker

    @Autowired
    lateinit var tvmCheckMvc: TvmCheckMvcMock

    @BeforeEach
    fun resetMocks() {
        PersTestMocksHolder.resetMocks()
    }

    private fun mockCheckerError() {
        Mockito.doAnswer {
            val ticket = it.getArgument<String>(0)
            val trust = it.getArgument<List<String>>(1)
            if (trust.isEmpty()) {
                if (ticket == null || ticket.equals(TICKET_ERR)) {
                    throw IllegalArgumentException("tvm check failed")
                }
                return@doAnswer null
            }

            // handle non-empty trust list - expect ticket value to be one of trusted names
            if (!trust.contains(ticket)) {
                throw IllegalArgumentException("tvm not trusted")
            }

            return@doAnswer null
        }
            .`when`(tvmChecker).checkTvm(any(), anyList())
    }

    @Test
    fun testCall() {
        mockCheckerError()
        assertTrue(tvmCheckMvc.methodWithTvm(TICKET_OK, OK).contains("method is executed"))
        assertTrue(tvmCheckMvc.methodWithTvm(TICKET_ERR, ERR_4XX).contains("tvm check failed"))
        assertTrue(tvmCheckMvc.methodWithTvm(null, ERR_4XX).contains("tvm check failed"))
        assertTrue(tvmCheckMvc.methodWithTvmDisabled(TICKET_OK, OK).contains("method executed well"))
        assertTrue(tvmCheckMvc.methodWithTvmDisabled(TICKET_ERR, OK).contains("method executed well"))
        assertTrue(tvmCheckMvc.methodWithTvmDisabled(null, OK).contains("method executed well"))
        assertTrue(tvmCheckMvc.methodWithTvmDisabled2(TICKET_OK, OK).contains("method executed well"))
        assertTrue(tvmCheckMvc.methodWithTvmDisabled2(TICKET_ERR, OK).contains("method executed well"))
        assertTrue(tvmCheckMvc.methodWithTvmDisabled2(null, OK).contains("method executed well"))
        assertTrue(tvmCheckMvc.methodWithTvmError(TICKET_OK, ERR_5XX).contains("ruined"))
        assertTrue(tvmCheckMvc.methodWithTvmError(TICKET_ERR, ERR_4XX).contains("tvm check failed"))
        assertTrue(tvmCheckMvc.methodWithTvmError(null, ERR_4XX).contains("tvm check failed"))

        //different rules for
        assertTrue(tvmCheckMvc.methodWithTvmTrusted(TICKET_OK, ERR_4XX).contains("tvm not trusted"))
        assertTrue(tvmCheckMvc.methodWithTvmTrusted("trusted_client", OK).contains("restricted well"))
        assertTrue(tvmCheckMvc.methodWithTvmTrusted("secure_client", ERR_4XX).contains("tvm not trusted"))
        assertTrue(tvmCheckMvc.methodWithTvmTrusted2("secure_client", OK).contains("restricted 2 well"))
        assertTrue(tvmCheckMvc.methodWithTvmTrusted2("trusted_client", ERR_4XX).contains("tvm not trusted"))
    }

    @Test
    fun testWithNullTvmAcceptable() {
        Mockito.doNothing().`when`(tvmChecker).checkTvm(isNull())
        assertTrue(tvmCheckMvc.methodWithTvm(null, ERR_4XX).contains("method is executed"))
        assertTrue(tvmCheckMvc.methodWithTvmDisabled(null, OK).contains("method executed well"))
        assertTrue(tvmCheckMvc.methodWithTvmDisabled2(null, OK).contains("method executed well"))
        assertTrue(tvmCheckMvc.methodWithTvmError(null, ERR_5XX).contains("ruined"))
        assertTrue(
            (tvmCheckMvc.methodWithTvmHeader(null, ERR_4XX) ?: "")
                .contains("TVM is required to call properly")
        )
    }
}