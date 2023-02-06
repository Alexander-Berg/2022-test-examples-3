package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.model.UidAndClientId

class UserInfo(
        var user: User? = null,
        var clientInfo: ClientInfo? = null
) {

    val shard : Int get() = clientInfo!!.shard

    val clientId: ClientId get() = clientInfo!!.clientId!!
    val chiefUid: Long get() = user!!.chiefUid!!

    val uid : Long get() = user!!.uid
    val login : String get() = user!!.login

    fun withUser(user: User) = apply {
        this.user = user
    }

    fun withClientInfo(clientInfo: ClientInfo) = apply {
        this.clientInfo = clientInfo
    }

    fun getUidAndClientId(): UidAndClientId {
        return UidAndClientId.of(uid, clientId)
    }
}
