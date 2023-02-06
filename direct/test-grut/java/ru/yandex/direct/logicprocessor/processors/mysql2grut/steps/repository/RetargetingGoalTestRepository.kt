package ru.yandex.direct.logicprocessor.processors.mysql2grut.steps.repository

import org.springframework.stereotype.Repository
import ru.yandex.direct.core.mysql2grut.repository.RetargetingGoalRepository
import ru.yandex.direct.dbschema.ppc.Tables.RETARGETING_GOALS
import ru.yandex.direct.dbutil.wrapper.DslContextProvider

@Repository
class RetargetingGoalTestRepository(private val dslContextProvider: DslContextProvider) :
    RetargetingGoalRepository(dslContextProvider) {

    fun updateIsAccessible(shard: Int, retCondId: Long, goalId: Long, isAccessible: Boolean) {
        val intIsAccessible = when (isAccessible) {
            true -> 1L
            false -> 0L
        }

        dslContextProvider.ppc(shard).update(RETARGETING_GOALS)
            .set(RETARGETING_GOALS.IS_ACCESSIBLE, intIsAccessible)
            .where(RETARGETING_GOALS.RET_COND_ID.eq(retCondId))
            .and(RETARGETING_GOALS.GOAL_ID.eq(goalId))
            .execute()
    }
}
