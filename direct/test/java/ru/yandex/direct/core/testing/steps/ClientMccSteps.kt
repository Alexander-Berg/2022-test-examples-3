package ru.yandex.direct.core.testing.steps

import org.springframework.stereotype.Component
import ru.yandex.direct.core.entity.client.mcc.ClientMccService
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.rbac.RbacClientsRelations

@Component
class ClientMccSteps(
    private val rbacClientsRelations: RbacClientsRelations,
    private val clientMccService: ClientMccService,
) {
    fun createClientMccLink(controlClientId: ClientId, managedClientId: ClientId) {
        rbacClientsRelations.addClientMccRelation(controlClientId, managedClientId)
    }

    fun addMccRequest(controlClientId: ClientId, managedClientId: ClientId) {
        return clientMccService.addRequest(controlClientId, managedClientId)
    }
}
