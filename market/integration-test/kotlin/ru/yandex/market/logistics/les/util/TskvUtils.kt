package ru.yandex.market.logistics.les.util

private const val ENTITY_TYPES = "entity_types"
private const val ENTITY_VALUES = "entity_values"
private const val EXTRA_VALUES = "extra_values"
private const val EXTRA_KEYS = "extra_keys"
private const val CODE_KEY = "code"
private const val PAYLOAD_KEY = "payload"
private const val LEVEL_KEY = "level"


fun parseTskRow(logRow: String): TskvLogRow {
    val logHeaders = tskvLogToMap(logRow)
    return TskvLogRow(
        code = logHeaders.getOrElse(CODE_KEY) { null },
        payload = logHeaders.getOrElse(PAYLOAD_KEY) { null },
        entities = tskvGetEntity(logHeaders),
        extra = tskvGetExtra(logHeaders),
    )
}

data class TskvLogRow(
    val code: String?,
    val payload: String?,
    val entities: List<Pair<String, String>> = listOf(),
    val extra: List<Pair<String, String>> = listOf(),
)

private fun tskvLogToMap(log: String) = Regex("""([a-z_]+)=([^\t]*)\t?|$""")
    .findAll(log)
    .iterator()
    .asSequence()
    .associate { it.destructured.component1() to it.destructured.component2() }


fun tskvGetEntity(logHeader: Map<String, String>): List<Pair<String, String>> =
    logHeader.map {
        it.key to it.value.split(",").map { element -> element.trimEnd().trimStart() }
    }.toMap()
        .let {
            it.getOrDefault(ENTITY_TYPES, emptyList()).zip(
                it.getOrDefault(ENTITY_VALUES, emptyList())
            )
        }

fun tskvGetExtra(logHeader: Map<String, String>): List<Pair<String, String>> =
    logHeader.map {
        it.key to it.value.split(",").map { element -> element.trimEnd().trimStart() }
    }.toMap()
        .let {
            it.getOrDefault(EXTRA_KEYS, emptyList()).zip(
                it.getOrDefault(EXTRA_VALUES, emptyList())
            )
        }
