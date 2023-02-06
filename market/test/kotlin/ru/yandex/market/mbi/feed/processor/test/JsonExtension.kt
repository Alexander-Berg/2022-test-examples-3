package ru.yandex.market.mbi.feed.processor.test

import org.assertj.core.api.Assertions
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import ru.yandex.market.common.test.util.JsonTestUtil
import ru.yandex.market.common.test.util.StringTestUtil

/**
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */

inline fun <reified T> ResponseEntity<String>.isEqualTo(path: String) {
    Assertions.assertThat(statusCode).isEqualTo(HttpStatus.OK)
    val expected = getString<T>(path)
    JsonTestUtil.assertEquals(expected, body)
}

inline fun <reified T> getString(path: String, vars: Map<String, String>? = null): String {
    var res = StringTestUtil.getString(T::class.java, path)
    vars?.forEach { (k, v) ->
        res = res
            .replace("$$k", v)
            .replace("{$k}", v)
    }
    return res
}
