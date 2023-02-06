package ru.yandex.market.sc.test.network.arch.ext

import okhttp3.ResponseBody

fun ResponseBody.safeString(): String = source().buffer.clone().readUtf8()