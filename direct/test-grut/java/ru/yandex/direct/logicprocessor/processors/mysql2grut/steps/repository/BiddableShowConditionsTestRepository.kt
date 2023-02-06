package ru.yandex.direct.logicprocessor.processors.mysql2grut.steps.repository

import org.springframework.stereotype.Repository
import ru.yandex.direct.core.mysql2grut.repository.BiddableShowConditionsRepository
import ru.yandex.direct.dbschema.ppc.Tables.BIDS
import ru.yandex.direct.dbschema.ppc.Tables.BIDS_BASE
import ru.yandex.direct.dbschema.ppc.Tables.BIDS_DYNAMIC
import ru.yandex.direct.dbschema.ppc.Tables.BIDS_PERFORMANCE
import ru.yandex.direct.dbschema.ppc.Tables.BIDS_RETARGETING
import ru.yandex.direct.dbutil.wrapper.DslContextProvider

@Repository
class BiddableShowConditionsTestRepository(private val dslContextProvider: DslContextProvider) : BiddableShowConditionsRepository(dslContextProvider) {

   fun deleteKeyword(shard: Int, id: Long) {
       dslContextProvider.ppc(shard).deleteFrom(BIDS).where(BIDS.ID.equal(id)).execute()
   }

    fun deleteBidsBase(shard: Int, id: Long) {
        dslContextProvider.ppc(shard).deleteFrom(BIDS_BASE).where(BIDS_BASE.BID_ID.equal(id)).execute()
    }

    fun deleteDynamicBid(shard: Int, id: Long) {
        dslContextProvider.ppc(shard).deleteFrom(BIDS_DYNAMIC).where(BIDS_DYNAMIC.DYN_ID.equal(id)).execute()
    }

    fun deletePerformanceBid(shard: Int, id: Long) {
        dslContextProvider.ppc(shard).deleteFrom(BIDS_PERFORMANCE).where(BIDS_PERFORMANCE.PERF_FILTER_ID.equal(id)).execute()
    }

    fun deleteRetargetingBid(shard: Int, id: Long) {
        dslContextProvider.ppc(shard).deleteFrom(BIDS_RETARGETING).where(BIDS_RETARGETING.RET_ID.equal(id)).execute()
    }
}
