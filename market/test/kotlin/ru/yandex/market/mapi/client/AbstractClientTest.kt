package ru.yandex.market.mapi.client

import io.netty.handler.codec.http.DefaultHttpHeaders
import okhttp3.Protocol
import okhttp3.ResponseBody
import okio.Buffer
import org.asynchttpclient.AsyncHttpClient
import org.asynchttpclient.Param
import org.asynchttpclient.Request
import org.junit.jupiter.api.BeforeEach
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatcher
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import ru.yandex.market.common.retrofit.ahc.OkhttpResponseAsyncHandler
import ru.yandex.market.mapi.core.AbstractNonSpringTest
import ru.yandex.market.mapi.core.util.asResource
import ru.yandex.market.mapi.core.util.assertJson
import ru.yandex.market.mapi.core.util.fillMultimap
import ru.yandex.market.mapi.core.util.multimapSetOf
import ru.yandex.market.mapi.core.util.spring.MapiContextBuilder
import ru.yandex.market.request.trace.Module
import ru.yandex.market.request.trace.RequestContextHolder
import ru.yandex.market.request.trace.RequestTraceUtil
import ru.yandex.passport.tvmauth.TvmClient
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ScheduledExecutorService
import java.util.function.Supplier
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @author Ilya Kislitsyn / ilyakis@ / 27.01.2022
 */
abstract class AbstractClientTest : AbstractNonSpringTest() {
    protected val log = LoggerFactory.getLogger(javaClass)

    private val asyncHttpClient = mock<AsyncHttpClient>()
    protected val tvmClient = mock<TvmClient>()
    private val mapiContextBuilder = MapiContextBuilder("junit")
    private val retrofitHelper = AsyncClientHelper(tvmClient, mapiContextBuilder)

    private val retryPool: ScheduledExecutorService = mock()

    protected fun mockedRetrofit(module: Module): MapiRetrofitService {
        return retrofitHelper.buildRetrofit("http://someurl-${module.name}", module,
            httpClientModifier = { _ -> asyncHttpClient })
    }

    protected fun mockedRetrofitWithRetry(module: Module): MapiRetrofitService {
        return retrofitHelper.buildRetrofit(
            "http://someurl-${module.name}", module,
            httpClientModifier = { _ -> asyncHttpClient },
            retryPool = retryPool
        )
    }

    @BeforeEach
    override fun prepareTests() {
        super.prepareTests()
        reset(tvmClient)

        whenever(retryPool.schedule(any(), any(), any())).then { invocation ->
            // retry will erase context, because it assumes to run in threadpool :(
            val context = RequestContextHolder.getContext()
            invocation.getArgument<Runnable>(0).run()
            RequestContextHolder.setContext(context)
        }
    }

    fun assertResponse(supplier: Supplier<*>, expected: String) {
        assertResponse(supplier.get(), expected)
    }

    fun assertResponse(future: CompletableFuture<*>, expected: String) {
        val response = future.get()
        val actualResponse = if (response is Supplier<*>) response.get() else response

        assertResponse(actualResponse, expected)
    }

    fun assertResponse(actualResponse: Any, expected: String) {
        assertJson(actualResponse, expected, name = "Actual json")
    }

    fun mockClientResponse(
        file: String? = null,
        status: HttpStatus = HttpStatus.OK,
        mediaType: MediaType = MediaType.APPLICATION_JSON,
        headers: Map<String, String> = emptyMap(),
        matcher: ArgumentMatcher<Request>? = null,
        withException: (() -> Exception)? = null
    ) {
        val requestMatcher = if (matcher != null) ArgumentMatchers.argThat(matcher) else any<Request>()

        whenever(
            asyncHttpClient.executeRequest(
                requestMatcher,
                any<OkhttpResponseAsyncHandler>()
            )
        ).thenAnswer mock_answer@{ call ->
            val handler = call.getArgument(1, OkhttpResponseAsyncHandler::class.java)

            if (withException != null) {
                handler.onThrowable(withException.invoke())
                return@mock_answer null
            }

            // prepare response mock
            val responseHeadersMock = DefaultHttpHeaders()
            headers.forEach { (key, value) -> responseHeadersMock.add(key, value) }

            val responseBody = file?.asResource() ?: "{}"
            val response = okhttp3.Response.Builder()
                //doesn't matter for tests
                .request(okhttp3.Request.Builder().url("http://localhost").build())
                .protocol(Protocol.HTTP_1_1)
                .code(status.value())
                .message(status.reasonPhrase)
                .body(
                    ResponseBody.create(
                        okhttp3.MediaType.parse(mediaType.toString()),
                        responseBody.toByteArray(Charsets.UTF_8)
                    )
                )

            headers.forEach { (key, value) -> response.addHeader(key, value) }

            // send response to client future
            val okResponse = handler.onCompleted(response.build()) as okhttp3.Response

            // this is important, allows to use @Streaming in clients to save some bytes moving
            assertTrue("async-http-client response is not a buffer any more") {
                okResponse.body()?.source() is Buffer
            }

            // asynchttpclient's future response is not used by retrofit
            return@mock_answer null
        }
    }

    fun verifyClientCall(
        times: Int = 1,
        verifier: (Int, Request) -> Unit = { _, _ -> }
    ) {
        val captor = ArgumentCaptor.forClass(Request::class.java)

        verify(
            asyncHttpClient, times(times)
        ).executeRequest(
            captor.capture(),
            any<OkhttpResponseAsyncHandler>()
        )

        captor.allValues.forEachIndexed(verifier)
    }

    fun Collection<Param>.toQueryMap(): Map<String, String> {
        return this.associate { it.name to it.value }
    }

    fun Request.assertRequest(method: HttpMethod, path: String) {
        assertEquals(method.toString(), this.method)
        assertEquals(path, this.uri.path)
    }

    fun Request.assertQuery(vararg pairs: Pair<String, Any>) {
        assertEquals(mapOf(*pairs), this.queryParams.toQueryMap())
    }

    /**
     * Case-insensitive (key) headers check.
     * Strict (default) - check full headers map.
     * Non-strict - check value of passed headers
     */
    fun Request.assertHeaders(vararg pairs: Pair<String, Any>, strict: Boolean = true) {
        // calculate in case-insensitive
        val expected = multimapSetOf(*pairs, lowercase = true)

        // default headers to pass in every call
        expected.fillMultimap(
            RequestTraceUtil.REQUEST_ID_HEADER to "req_id/1",
            "sec_header" to "sec_value",
            "sec_header2" to "sec_value2",
            lowercase = true
        )

        val headerPairs = this.headers.entries()
            .map { it.key.lowercase() to it.value }
            .filter { strict || expected.containsKey(it.first) }
            .toTypedArray()

        assertEquals(expected, multimapSetOf(*headerPairs))
    }
}
