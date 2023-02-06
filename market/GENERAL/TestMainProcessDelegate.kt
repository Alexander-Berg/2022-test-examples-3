package ru.yandex.market.tpl.courier.presentation.feature.app

import ru.yandex.market.tpl.courier.ApplicationComponent
import ru.yandex.market.tpl.courier.DaggerTestComponent
import ru.yandex.market.tpl.courier.MainApplication

class TestMainProcessDelegate(application: MainApplication) : MainProcessDelegate(application) {
    override val daggerComponent: ApplicationComponent by lazy {
        DaggerTestComponent.factory().create(context = application, reactApplication = application)
    }
}
