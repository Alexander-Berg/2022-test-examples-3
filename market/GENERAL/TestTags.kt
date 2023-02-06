package ru.yandex.market.sc.core.ui

enum class CommonUI {
    SuccessText,
    ErrorText;

    val tag: String
        get(): String = this.toString()
}
