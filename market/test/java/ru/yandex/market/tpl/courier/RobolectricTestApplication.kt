package ru.yandex.market.tpl.courier

import android.app.Application
import org.robolectric.TestLifecycleApplication
import java.lang.reflect.Method

class RobolectricTestApplication : Application(), TestLifecycleApplication {

    val component: RobolectricTestComponent by lazy { DaggerRobolectricTestComponent.create() }

    override fun beforeTest(method: Method?) {
        // no-op
    }

    override fun prepareTest(test: Any?) {
        // no-op
    }

    override fun afterTest(method: Method?) {
        // no-op
    }
}