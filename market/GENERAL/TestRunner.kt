package ru.yandex.market.tpl.courier

import android.app.Application
import android.content.Context
import android.os.Bundle
import io.qameta.allure.android.runners.AllureAndroidJUnitRunner
import ru.yandex.market.tpl.courier.arch.logs.d
import ru.yandex.market.tpl.courier.utils.FailedTestsListener


@Suppress("unused")
class TestRunner : AllureAndroidJUnitRunner() {

    override fun onCreate(arguments: Bundle) {
        arguments.stringArguments.forEach(System::setProperty)
        arguments?.putCharSequence("listener", arguments.getCharSequence("listener")
            ?.let {
                "$it,${FailedTestsListener::class.java.name}"
            }
            ?: FailedTestsListener::class.java.name)
        d("TestRunner received ${arguments?.size() ?: 0} arguments")

        arguments?.keySet()?.forEachIndexed { index, key ->
            d("$index $key : ${arguments[key]}")
        }
        super.onCreate(arguments)
    }

    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application {
        return super.newApplication(cl, TestApplication::class.java.name, context)
    }

    private val Bundle.stringArguments: Map<String, String>
        get() {
            return keySet()
                .mapNotNull { key ->
                    val value = getString(key) ?: return@mapNotNull null
                    key to value
                }
                .toMap()
        }
}