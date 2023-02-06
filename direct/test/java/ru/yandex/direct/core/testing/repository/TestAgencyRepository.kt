package ru.yandex.direct.core.testing.repository

import one.util.streamex.StreamEx
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import ru.yandex.direct.core.entity.user.model.AgencyLimRep
import ru.yandex.direct.dbschema.ppc.Tables.AGENCY_LIM_REP_CLIENTS
import ru.yandex.direct.dbschema.ppc.Tables.USERS_AGENCY
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.rbac.RbacAgencyLimRepType.toSource
import javax.annotation.ParametersAreNonnullByDefault

@Repository
@ParametersAreNonnullByDefault
class TestAgencyRepository {
    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    fun linkLimRepToClient(shard: Int, limRepUids: Collection<Long>, clientId: Long) {
        val insertQuery = dslContextProvider.ppc(shard).insertInto(AGENCY_LIM_REP_CLIENTS)
                .columns(AGENCY_LIM_REP_CLIENTS.AGENCY_UID, AGENCY_LIM_REP_CLIENTS.CLIENT_ID)

        StreamEx.of(limRepUids).distinct().forEach { limRepUid ->
            insertQuery.values(limRepUid, clientId)
        }

        insertQuery.execute()
    }

    fun unlinkLimRepToClient(shard: Int, clientId: Long) {
        dslContextProvider.ppc(shard).deleteFrom(AGENCY_LIM_REP_CLIENTS)
                .where(AGENCY_LIM_REP_CLIENTS.CLIENT_ID.eq(clientId))
                .execute()
    }

    fun addUsersAgency(shard: Int, agencyLimRep: AgencyLimRep) {
        dslContextProvider.ppc(shard).insertInto(USERS_AGENCY)
            .columns(USERS_AGENCY.UID, USERS_AGENCY.GROUP_ID, USERS_AGENCY.LIM_REP_TYPE)
            .values(agencyLimRep.uid, agencyLimRep.groupId, toSource(agencyLimRep.repType))
            .execute()
    }
}
