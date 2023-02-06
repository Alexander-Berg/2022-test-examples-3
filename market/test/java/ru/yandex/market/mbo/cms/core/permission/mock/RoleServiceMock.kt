package ru.yandex.market.mbo.cms.core.permission.mock

import ru.yandex.market.mbo.cms.core.json.idm.requests.RoleRequest
import ru.yandex.market.mbo.cms.core.json.idm.response.AllRolesResponse
import ru.yandex.market.mbo.cms.core.json.idm.response.DefaultIdmResponse
import ru.yandex.market.mbo.cms.core.json.idm.response.InfoResponse
import ru.yandex.market.mbo.cms.core.service.permission.RoleService

class RoleServiceMock: RoleService {
    override fun getRolesTree(): InfoResponse {
        throw NotImplementedError("Not yet implemented")
    }

    override fun addRole(roleRequest: RoleRequest): DefaultIdmResponse {
        throw NotImplementedError("Not yet implemented")
    }

    override fun removeRole(roleRequest: RoleRequest): DefaultIdmResponse {
        throw NotImplementedError("Not yet implemented")
    }

    override fun getRoles(pageNumber: Int, pageSize: Int): AllRolesResponse {
        throw NotImplementedError("Not yet implemented")
    }

    override fun getUserAndGroupRoles(userId: Long?, serviceId: Int?): List<Pair<String, String>> {
        return listOf(
                Pair("project1", "role1"),
                Pair("project2", "role2"),
                Pair("project3", "role3"),
        )
    }
}
