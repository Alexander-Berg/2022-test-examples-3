package ru.yandex.market.contentmapping.app

class TestParamNames {
    fun paramName(someParamName: String) = if (someParamName != "") javaClass.getMethod("paramName", String::class.java).parameters[0].name else ""

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            println("This shouldn't be 'arg0': " + TestParamNames().paramName("!"))
        }
    }
}
