package ru.yandex.market.contentmapping.config.utils

/**
 * Помогает запустить тесты с полным спринговым конфигом, но часть из них заменить на Spy-и.
 * Таким образом можно в отдельно взятом тесте менять поведение отдельных бинов, не пересобирая контекст.
 *
 * В продакшен коде идёт просто заглушка, которая возвращает сервис as-is, и не требует доп. зависимостей.
 */
interface TestServiceWrapper {
    fun <T> spyInTests(service: T): T

    class NoWrap: TestServiceWrapper {
        override fun <T> spyInTests(service: T): T = service
    }
}
