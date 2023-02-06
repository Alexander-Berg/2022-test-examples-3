package ru.yandex.market.mapi.mock

import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import ru.yandex.market.common.retrofit.CommonRetrofitHttpExecutionException
import ru.yandex.market.mapi.client.fapi.FapiClient
import ru.yandex.market.mapi.core.ResolverClientResponseMock
import ru.yandex.market.mapi.core.model.screen.ResourceResolver
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeoutException
import java.util.function.Predicate

/**
 * @author Ilya Kislitsyn / ilyakis@ / 24.01.2022
 */
@Service
class FapiMocker {
    @Autowired
    private lateinit var fapiClient: FapiClient

    fun mockFapiResponse(
        file: String,
        name: String? = null,
        checker: Predicate<ResourceResolver>? = null,
        debugInfo: String? = null
    ) {
        whenever(fapiClient.callResolver(callMatcher(name, checker = checker), any())).then { call ->
            val resolver = call.arguments[0] as ResourceResolver
            CompletableFuture.completedFuture(
                ResolverClientResponseMock(file, "${resolver.resolver!!}${resolver.version.uppercase()}", debugInfo)
            )
        }
    }

    fun mockFapiBatchResponse(file: String, name: String) {
        whenever(fapiClient.callResolverBatch(eq(name), anyList(), anyBoolean())).then {
            CompletableFuture.completedFuture(
                ResolverClientResponseMock(file, name)
            )
        }
    }

    fun mockFapiErrorResponse(name: String? = null) {
        whenever(fapiClient.callResolver(callMatcher(name), any())).then {
            CompletableFuture.failedFuture<Void>(
                RuntimeException("Fapi runtime error")
            )
        }
    }

    fun mockFapiTimeoutFuture(name: String? = null) {
        whenever(fapiClient.callResolver(callMatcher(name), any())).then {
            val futureMock = mock<CompletableFuture<Void>>()
            whenever(futureMock.get(anyLong(), any())).thenThrow(
                TimeoutException("Fapi timeout error")
            )
            whenever(futureMock.thenAccept(any())).then {
                // do nothing
                futureMock
            }
            futureMock
        }
    }

    fun mockFapiTimeoutResponse(name: String? = null) {
        whenever(fapiClient.callResolver(callMatcher(name), any())).then {
            CompletableFuture.failedFuture<Void>(
                CommonRetrofitHttpExecutionException(
                    "somepackage.TimeoutException: Fapi client timeout error",
                    -1,
                    TimeoutException("read cause")
                )
            )
        }
    }

    fun verifyCall(times: Int, name: String? = null, checker: (Int, ResourceResolver) -> Unit = { _, _ -> }) {
        val capture = mutableListOf<ResourceResolver>()
        verify(fapiClient, times(times)).callResolver(callMatcher(name, capture), any())

        capture.forEachIndexed(checker)
    }

    fun verifyNoMoreInteractions() {
        verifyNoMoreInteractions(fapiClient)
    }

    private fun callMatcher(
        name: String?,
        capture: MutableList<ResourceResolver>? = null,
        checker: Predicate<ResourceResolver>? = null,
    ): ResourceResolver {
        return argThat { resolver ->
            if (resolver == null) {
                return@argThat false
            }
            if (name != null && resolver.resolver != name) {
                return@argThat false
            }
            if (checker != null) {
                return@argThat checker.test(resolver)
            }

            capture?.add(resolver)
            return@argThat true

        }
    }
}
