package ru.yandex.market.sc.test.resources

import ru.yandex.market.sc.core.resources.StringManager

class TestStringManager : StringManager {
    override fun getString(resId: Int): String {
        return resId.toString()
    }

    override fun getString(resId: Int, vararg formatArgs: Any?): String {
        return "$resId ${formatArgs.joinToString(", ")}"
    }
}
