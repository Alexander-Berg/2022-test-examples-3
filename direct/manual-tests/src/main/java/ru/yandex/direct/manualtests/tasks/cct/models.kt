package ru.yandex.direct.manualtests.tasks.cct

import org.assertj.core.api.recursive.comparison.ComparisonDifference

data class GraphDiff(
    val expectedIds: MutableSet<Long> = mutableSetOf(),
    val actualIds: MutableSet<Long> = mutableSetOf(),
    val diff: MutableMap<Class<Any>, MutableList<ObjectDiff>> = mutableMapOf(),
) {
    fun forEntity(entityClass: Class<*>): MutableList<ObjectDiff> {
        @Suppress("UNCHECKED_CAST")
        return diff.computeIfAbsent(entityClass as Class<Any>) { mutableListOf() }
    }

    fun merge(other: GraphDiff) {
        expectedIds.addAll(other.expectedIds)
        actualIds.addAll(other.actualIds)
        other.diff.forEach { (entityClass, otherEntityDiff) ->
            forEntity(entityClass).addAll(otherEntityDiff)
        }
    }
}

interface ObjectDiff

data class MissingObject(
    val leftId: Any?,
    val rightId: Any?,
) : ObjectDiff

data class FieldsDiff(
    val leftId: Any,
    val rightId: Any,
    val diff: List<ComparisonDifference>,
) : ObjectDiff
