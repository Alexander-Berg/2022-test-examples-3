package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.client.model.Client
import ru.yandex.direct.core.entity.client.model.ClientLimits
import ru.yandex.direct.core.testing.steps.ClientSteps.Companion.DEFAULT_SHARD
import ru.yandex.direct.dbutil.model.ClientId

class ClientInfo(
        var shard: Int = DEFAULT_SHARD,

        var client: Client? = null,
        var clientLimits: ClientLimits? = null,

        var chiefUserInfo: UserInfo? = null
) {

    val uid : Long get() = chiefUserInfo!!.uid
    val login : String get() = chiefUserInfo!!.login

    val clientId: ClientId? get() = ClientId.fromNullableLong(client?.id)

    fun withShard(shard: Int) = apply { this.shard = shard }

    fun withClient(client: Client) = apply { this.client = client }

    fun withClientLimits(clientLimits: ClientLimits) = apply { this.clientLimits = clientLimits }

    fun withChiefUserInfo(chiefUserInfo: UserInfo) = apply { this.chiefUserInfo = chiefUserInfo }
}
