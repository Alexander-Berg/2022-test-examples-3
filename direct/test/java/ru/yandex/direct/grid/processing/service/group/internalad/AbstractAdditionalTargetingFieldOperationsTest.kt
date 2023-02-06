package ru.yandex.direct.grid.processing.service.group.internalad

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargeting
import ru.yandex.direct.core.testing.data.TestGroups.activeInternalAdGroup

private const val CID = 22L

@RunWith(JUnitParamsRunner::class)
abstract class AbstractAdditionalTargetingFieldOperationsTest<VAL>(
    private val fieldOperations: InternalAdGroupTargetingOperations<VAL>
) {

    abstract fun extract_targeting_params(): List<List<Any>>

    @Test
    @TestCaseName("{0}")
    @Parameters(method = "extract_targeting_params")
    fun extract_targeting(
        description: String,
        originalTargetings: List<AdGroupAdditionalTargeting>,
        expectedValues: List<AdditionalTargetingValue<VAL>>
    ) {
        val obj = internalAdGroupWithTargetings(originalTargetings)
        assertThat(fieldOperations.extract(obj))
            .containsExactlyInAnyOrderElementsOf(expectedValues)
    }

    abstract fun remove_targeting_params(): List<List<Any>>

    @Test
    @TestCaseName("{0}")
    @Parameters(method = "remove_targeting_params")
    fun remove_targeting(
        description: String,
        originalTargetings: List<AdGroupAdditionalTargeting>,
        removedValues: List<AdditionalTargetingValue<VAL>>,
        expectedTargetings: List<AdGroupAdditionalTargeting>
    ) {
        val obj = internalAdGroupWithTargetings(originalTargetings)
        removedValues.forEach { value -> fieldOperations.remove(obj, value) }
        assertThat(obj.targetings.toList())
            .containsExactlyInAnyOrderElementsOf(expectedTargetings)
    }

    abstract fun add_targeting_params(): List<List<Any>>

    @Test
    @TestCaseName("{0}")
    @Parameters(method = "add_targeting_params")
    fun add_targeting(
        description: String,
        originalTargetings: List<AdGroupAdditionalTargeting>,
        addedValues: List<AdditionalTargetingValue<VAL>>,
        expectedTargetings: List<AdGroupAdditionalTargeting>
    ) {
        val obj = internalAdGroupWithTargetings(originalTargetings)
        addedValues.forEach { value -> fieldOperations.add(obj, value) }
        assertThat(obj.targetings.toList())
            .containsExactlyInAnyOrderElementsOf(expectedTargetings)
    }

    private fun internalAdGroupWithTargetings(targetings: List<AdGroupAdditionalTargeting>) =
        InternalAdGroupWithTargeting(
            activeInternalAdGroup(CID), TargetingIndex.fromTargetingCollection(
                targetings
            )
        )
}
