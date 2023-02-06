package ru.yandex.direct.logicprocessor.processors.mysql2grut.steps.repository

import org.springframework.stereotype.Repository
import ru.yandex.direct.dbschema.ppc.Tables.HIERARCHICAL_MULTIPLIERS
import ru.yandex.direct.dbutil.wrapper.DslContextProvider

@Repository
class BidModifierTestRepository(private val dslContextProvider: DslContextProvider) {

    fun updateHierarchicalMultiplierPercent(shard: Int, id: Long, percent: Long) {
        dslContextProvider.ppc(shard)
            .update(HIERARCHICAL_MULTIPLIERS).set(HIERARCHICAL_MULTIPLIERS.MULTIPLIER_PCT, percent)
            .where(HIERARCHICAL_MULTIPLIERS.HIERARCHICAL_MULTIPLIER_ID.eq(id))
            .execute()
    }

    fun updateHierarchicalMultiplierEnabled(shard: Int, id: Long, enabled: Boolean) {
        val enabledInt = when(enabled) {
            true -> 1L
            false -> 0L
        }
        dslContextProvider.ppc(shard)
            .update(HIERARCHICAL_MULTIPLIERS).set(HIERARCHICAL_MULTIPLIERS.IS_ENABLED, enabledInt)
            .where(HIERARCHICAL_MULTIPLIERS.HIERARCHICAL_MULTIPLIER_ID.eq(id))
            .execute()
    }
}
