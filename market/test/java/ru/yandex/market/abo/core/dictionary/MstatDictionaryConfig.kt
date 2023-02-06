package ru.yandex.market.abo.core.dictionary

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 06.09.2021
 */
data class MstatDictionaryConfig(
    val dbEntityName: String,
    val columns: Set<String>
)
