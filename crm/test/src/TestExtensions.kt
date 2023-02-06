package ru.yandex.crm.library.kotlin.test

inline fun assert(action: () -> Boolean) {
    assert(action.invoke())
}

inline fun <reified T> T?.isNotNull(): Boolean {
    return this != null
}

inline fun <reified T> T?.isNull(): Boolean {
    return this == null
}
