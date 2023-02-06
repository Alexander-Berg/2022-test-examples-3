package ru.yandex.market.partner.status.mbi.replication

import org.junit.jupiter.api.Test
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.partner.status.AbstractFunctionalTest
import ru.yandex.mj.generated.client.wizard_client.model.ReplicationStatus
import ru.yandex.mj.generated.client.wizard_client.model.ReplicationStatusRequest
import ru.yandex.mj.generated.client.wizard_client.model.ReplicationType

/**
 * Тесты для [PartnerReplicationStatusController].
 *
 */
class PartnerReplicationStatusControllerTest : AbstractFunctionalTest(){

    @Test
    @DbUnitDataSet(after = ["createReplication.status.after.csv"])
    fun `create partner replication status`() {
        val partnerId = 999L
        val createRequest = ReplicationStatusRequest()
            .replicationType(ReplicationType.FBS_TO_FBS_REPLICATION)
            .replicationStatus(ReplicationStatus.IN_PROGRESS)
        partnerReplicationStatusApiClient.updateReplicationStatus(partnerId, createRequest).scheduleVoid().join()
    }

    @Test
    @DbUnitDataSet(after = ["updateReplication.status.after.csv"])
    fun `create and update partner replication status`() {
        val partnerId = 999L
        val createRequest = ReplicationStatusRequest()
            .replicationType(ReplicationType.FBS_TO_FBS_REPLICATION)
            .replicationStatus(ReplicationStatus.IN_PROGRESS)
        partnerReplicationStatusApiClient.updateReplicationStatus(partnerId, createRequest).scheduleVoid().join()

        val updateRequest = ReplicationStatusRequest()
            .replicationType(ReplicationType.FBS_TO_FBS_REPLICATION)
            .replicationStatus(ReplicationStatus.DONE)
        partnerReplicationStatusApiClient.updateReplicationStatus(partnerId, updateRequest).scheduleVoid().join()
    }
}
