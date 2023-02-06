@file:JvmName("RetrofitTestUtil")

package ru.yandex.market.abo.util

import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import retrofit2.Call
import retrofit2.Response

fun <T> mockRetrofitCall(body: T): Call<T> {
    val call: Call<T> = mock()
    whenever(call.execute()).thenReturn(Response.success(body))
    return call
}
