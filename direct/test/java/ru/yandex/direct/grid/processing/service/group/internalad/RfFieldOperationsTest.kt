package ru.yandex.direct.grid.processing.service.group.internalad

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.direct.core.entity.adgroup.model.InternalAdGroup
import ru.yandex.direct.core.testing.data.TestGroups.activeInternalAdGroup
import ru.yandex.direct.test.utils.randomPositiveInt

private const val CID = 22L

class RfFieldOperationsTest {
    private val fieldOperations = SimpleInternalAdGroupFieldOperations(InternalAdGroup.RF)

    @Test
    fun extract_field() {
        val rf = randomPositiveInt()
        val obj = internalAdGroupWithRf(rf)
        assertThat(fieldOperations.extract(obj))
            .isEqualTo(listOf(rf))
    }

    @Test
    fun remove_field() {
        val rf = randomPositiveInt()
        val obj = internalAdGroupWithRf(rf)
        fieldOperations.remove(obj, rf)
        assertThat(obj.adGroup)
            .hasFieldOrPropertyWithValue("rf", null)
    }

    @Test
    fun add_field() {
        val rf = randomPositiveInt()
        val obj = internalAdGroupWithRf(null)
        fieldOperations.add(obj, rf)
        assertThat(obj.adGroup)
            .hasFieldOrPropertyWithValue("rf", rf)
    }

    private fun internalAdGroupWithRf(rf: Int?) = InternalAdGroupWithTargeting(
        activeInternalAdGroup(CID)
            .withRf(rf),
        TargetingIndex.fromTargetingCollection(emptyList())
    )
}
