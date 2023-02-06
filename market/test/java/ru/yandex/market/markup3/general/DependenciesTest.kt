package ru.yandex.market.markup3.general

import org.junit.Test

class DependenciesTest {
    @Test
    fun `test package dependencies`() {
        /**
         * Это пока просто набросок, который ничего не делает - как сделать контроль использования пакетов друг-другом.
         * Это один из вариантов следить за архитектурой, чтобы не было перемешано, что где.
         */
        checkDependencies("ru.yandex.market.markup3") {
            "core" uses "utils"
            "core".packages {
                "repositories" uses "dto"
                "services" uses listOf("dto", "messages")
            }
            "yang" uses listOf("core", "utils")
            "config" uses NO_RESTRICTION
        }
    }

    class DependencyDsl {
        fun String.packages(dsl: DependencyDsl.() -> Unit) {}

        infix fun String.uses(pckg: String) {}
        infix fun String.uses(pckgs: List<String>) {}

        val NO_RESTRICTION = "no restriction :)"
    }

    fun checkDependencies(basePackage: String, init: DependencyDsl.() -> Unit) {
    }
}
