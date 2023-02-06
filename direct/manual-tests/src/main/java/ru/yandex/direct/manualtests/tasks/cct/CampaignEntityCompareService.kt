package ru.yandex.direct.manualtests.tasks.cct

import org.assertj.core.api.recursive.comparison.ComparisonDifference
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import org.assertj.core.api.recursive.comparison.RecursiveComparisonDifferenceCalculator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.yandex.direct.core.copyentity.EntityContext
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign
import ru.yandex.direct.core.entity.retargeting.model.CampMetrikaGoal
import ru.yandex.direct.model.Entity
import ru.yandex.direct.tracing.Trace

@Service
class CampaignEntityCompareService {

    private val logger = LoggerFactory.getLogger(CampaignEntityCompareService::class.java)

    fun compareCampaigns(expected: EntityContext, actual: EntityContext): GraphDiff {
        logger.info("Comparing contexts")
        val graphDiff = GraphDiff()

        graphDiff.expectedIds += expected.getEntities(BaseCampaign::class.java).keys as Set<Long>
        graphDiff.actualIds += actual.getEntities(BaseCampaign::class.java).keys as Set<Long>

        Trace.current().profile("compare_context").use {
            CampaignCompareStrategies.STRATEGY_BY_ENTITY_CLASS.forEach { (entityClass, configuration) ->
                val expectedEntities: Map<*, Entity<*>> = expected.getEntitiesUntyped(entityClass)
                val actualEntities: Map<*, Entity<*>> = actual.getEntitiesUntyped(entityClass)

                val diff: List<ObjectDiff> = matchObjects(
                    configuration,
                    expectedEntities.values.toList(),
                    actualEntities.values.toList(),
                )

                if (diff.isNotEmpty()) {
                    graphDiff.forEntity(entityClass).addAll(diff)
                }
            }
        }

        return graphDiff
    }

    /**
     * Принимает два списка объектов. Попарно сравнивает все объекты, а затем использует максимальное паросочетание
     * минимального веса для поиска наиболее вероятных совпадений. В качестве веса используется количество
     * несовпадающий полей у объектов.
     */
    private fun matchObjects(
        configuration: RecursiveComparisonConfiguration,
        expectedObjects: List<Entity<*>>,
        actualObjects: List<Entity<*>>,
    ): List<ObjectDiff> {
        val calculator = RecursiveComparisonDifferenceCalculator()

        val pairwiseDiff: MutableMap<Pair<Int, Int>, List<ComparisonDifference>> = mutableMapOf()

        expectedObjects.forEachIndexed { i, leftObject ->
            actualObjects.forEachIndexed { j, rightObject ->
                if (leftObject::class.java == rightObject::class.java) {
                    pairwiseDiff[i to j] = calculator.determineDifferences(rightObject, leftObject, configuration)
                }
            }
        }

        val matching: List<Pair<Int, Int>> = MinimalWeightedMatching.minimalMatching(
            expectedObjects.size,
            actualObjects.size,
            Array(expectedObjects.size) { i ->
                IntArray(actualObjects.size) { j ->
                    pairwiseDiff[i to j]
                        ?.let { diff -> diff.size * diff.size }
                        ?: MinimalWeightedMatching.INF
                }
            },
        )

        val leftMissingObjects: List<ObjectDiff> = expectedObjects.indices
            .minus(matching.map { (l, _) -> l }.toSet())
            .map { index -> MissingObject(expectedObjects[index].id, null) }

        val rightMissingObjects: List<ObjectDiff> = actualObjects.indices
            .minus(matching.map { (_, r) -> r }.toSet())
            .map { index -> MissingObject(null, actualObjects[index].id) }

        val diffItems: List<ObjectDiff> = matching.map { (l, r) ->
            FieldsDiff(
                expectedObjects[l].id,
                actualObjects[r].id,
                pairwiseDiff[l to r]!!,
            )
        }

        return leftMissingObjects + rightMissingObjects + diffItems
    }

    fun buildDiff(graphDiff: GraphDiff?): String {
        if (graphDiff == null) {
            return ""
        }

        var result = """
            Expected campaigns: ${graphDiff.expectedIds.joinToString()}
            Actual campaigns: ${graphDiff.actualIds.joinToString()}
        """.trimIndent()

        result += "\n"
        graphDiff.diff.forEach { (entityClass, entityDiff) ->
            result += "Entity: ${entityClass.simpleName}\n"
            entityDiff.forEach { objectDiff ->
                when (objectDiff) {
                    is MissingObject -> {
                        result += if (objectDiff.leftId != null) {
                            "  Id: Expected - ${objectDiff.leftId}, Actual - Missing\n"
                        } else {
                            "  Id: Expected - Missing, Actual - ${objectDiff.rightId}\n"
                        }
                    }
                    is FieldsDiff -> {
                        if (objectDiff.diff.isNotEmpty()) {
                            result += "  Id: " +
                                    "Expected - ${idToString(objectDiff.leftId)}, " +
                                    "Actual - ${idToString(objectDiff.rightId)}\n"
                            objectDiff.diff.forEach { diffItem ->
                                result += diffItem.multiLineDescription().prependIndent("    ") + "\n"
                            }
                        }
                    }
                }
            }
        }
        return result
    }

    private fun idToString(id: Any): String = when (id) {
        is CampMetrikaGoal -> id.goalId.toString()
        else -> id.toString()
    }
}
