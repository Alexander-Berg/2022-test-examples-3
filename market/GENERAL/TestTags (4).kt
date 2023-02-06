package ru.yandex.market.sc.feature.accept_returns.presenter.scan_courier

enum class AcceptReturnsScanCourier {
    SelectCourierBtn;

    val tag: String
        get(): String = this.toString()
}