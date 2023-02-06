package ru.yandex.market.logistics.mqm.utils

private const val ENTITY_TYPES = "entity_types"
private const val ENTITY_VALUES = "entity_values"
private const val EXTRA_VALUES = "extra_values"
private const val EXTRA_KEYS = "extra_keys"
private const val CODE_KEY = "code"
private const val PAYLOAD_KEY = "payload"
private const val LEVEL_KEY = "level"

private fun tskvLogToMap(
    log: String,
) = Regex("""([a-z_]+)=([^\t]*)\t?|$""")
    .findAll(log)
    .iterator()
    .asSequence()
    .associate { it.destructured.component1() to it.destructured.component2() }

fun tskvGetExtra(log: String) =
    tskvLogToMap(log).map {
        it.key to
            it.value.split(",").map { element -> element.trimEnd().trimStart() }
    }.toMap()
        .let {
            it.getOrDefault(EXTRA_KEYS, emptyList()).zip(
                it.getOrDefault(EXTRA_VALUES, emptyList())
            )
        }

fun tskvGetEntity(log: String) =
    tskvLogToMap(log).map {
        it.key to
            it.value.split(",").map { element -> element.trimEnd().trimStart() }
    }.toMap()
        .let {
            it.getOrDefault(ENTITY_TYPES, emptyList()).zip(
                it.getOrDefault(ENTITY_VALUES, emptyList())
            )
        }

fun tskvGetEntities(log: String): List<Pair<String, String>> {
    return tskvLogToMap(log).map {
        it.key to it.value.split(",").map { element -> element.trimEnd().trimStart() }
    }
        .find { pair -> pair.first == ENTITY_VALUES }!!
        .second
        .map { value -> value.split(":")[0] to value.split(":")[1] }
}

fun tskvGetByKey(log: String, key: String) =
    Pair(key, tskvLogToMap(log)[key])

fun tskvGetCode(log: String) = tskvGetByKey(log, CODE_KEY).second

fun tskvGetPayload(log: String) = tskvGetByKey(log, PAYLOAD_KEY).second

fun tskvGetLevel(log: String) = tskvGetByKey(log, LEVEL_KEY).second
