package ru.yandex.market.tpl.courier

import ru.yandex.market.tpl.courier.presentation.feature.app.ProcessDelegate
import ru.yandex.market.tpl.courier.presentation.feature.app.TestMainProcessDelegate

class TestApplication : MainApplication() {
    override val mainProcessDelegate: ProcessDelegate get() = TestMainProcessDelegate(this)
}