package ru.yandex.market.sc.test.network.arch.ext

import okhttp3.RequestBody
import okio.Buffer

fun RequestBody.string(): String = Buffer().also { writeTo(it) }.readUtf8()