package ru.yandex.direct.core.testing.steps;

import com.nhaarman.mockitokotlin2.doReturn
import org.mockito.ArgumentMatchers
import ru.yandex.direct.balance.client.BalanceClient
import ru.yandex.direct.balance.client.model.response.ClientPassportInfo
import ru.yandex.direct.core.entity.client.model.AgencyNds
import ru.yandex.direct.core.entity.client.model.Client
import ru.yandex.direct.core.entity.client.model.ClientLimits
import ru.yandex.direct.core.entity.client.model.ClientLimitsBase
import ru.yandex.direct.core.entity.client.model.ClientNds
import ru.yandex.direct.core.entity.client.repository.ClientLimitsRepository
import ru.yandex.direct.core.entity.client.repository.ClientOptionsRepository
import ru.yandex.direct.core.entity.client.repository.ClientRepository
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.testing.data.TestClients.defaultClient
import ru.yandex.direct.core.testing.data.TestClients.defaultInternalClient
import ru.yandex.direct.core.testing.data.TestUsers
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.repository.TestAgencyNdsRepository
import ru.yandex.direct.core.testing.repository.TestClientLimitsRepository
import ru.yandex.direct.core.testing.repository.TestClientNdsRepository
import ru.yandex.direct.core.testing.repository.TestClientRepository
import ru.yandex.direct.core.testing.stub.BalanceClientStub
import ru.yandex.direct.core.testing.stub.PassportClientStub
import ru.yandex.direct.currency.Percent
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.model.UidAndClientId
import ru.yandex.direct.dbutil.sharding.ShardKey
import ru.yandex.direct.dbutil.sharding.ShardSupport
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.model.ModelProperty
import ru.yandex.direct.rbac.RbacClientsRelations
import ru.yandex.direct.rbac.RbacRole
import ru.yandex.direct.utils.FunctionalUtils.mapList
import java.math.BigDecimal
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicLong

class ClientSteps(
    private val baseUserSteps: BaseUserSteps,
    private val clientRepository: ClientRepository,
    private val testClientRepository: TestClientRepository,
    private val passportClientStub: PassportClientStub?,
    private val shardSupport: ShardSupport,
    private val testClientLimitsRepository: TestClientLimitsRepository,
    private val clientLimitsRepository: ClientLimitsRepository,
    private val testClientNdsRepository: TestClientNdsRepository,
    private val testAgencyNdsRepository: TestAgencyNdsRepository,
    private val clientOptionsRepository: ClientOptionsRepository,
    private val rbacClientsRelations: RbacClientsRelations,
    private val balanceClient: BalanceClient
) {

    private val balanceTidMap: ConcurrentMap<ClientId, AtomicLong> = ConcurrentHashMap()

    companion object {
        const val DEFAULT_SHARD = 1

        // Можно использовать для тестов, когда хочется быть уверенным,
        // что какие-то сущности живут на разных шардах и при этом всё работает
        const val ANOTHER_SHARD = 2

        @JvmField
        val DEFAULT_NDS: Percent = Percent.fromPercent(BigDecimal.valueOf(20))!!
    }

    fun createDefaultClient(): ClientInfo {
        return createClient(ClientInfo())
    }

    //дубль createDefaultClient на время мержа новых степов
    //использовать createDefaultClient
    @Deprecated("use createDefaultClient")
    fun createDefaultClientAndUser(): ClientInfo {
        return createDefaultClient()
    }

    //дубль createDefaultClientWithRole на время мержа новых степов
    //использовать createDefaultClientWithRole
    @Deprecated("use createDefaultClientWithRole")
    fun createDefaultClientAndUserWithRole(role: RbacRole): ClientInfo {
        return createDefaultClientWithRole(role)
    }

    fun createDefaultClientAnotherShard(): ClientInfo {
        return createClient(ClientInfo(shard = ANOTHER_SHARD))
    }

    fun createDefaultClientUnderAgency(agencyClientInfo: ClientInfo): ClientInfo {
        return createClientUnderAgency(agencyClientInfo, ClientInfo())
    }

    fun createDefaultClientUnderManager(managerClientInfo: ClientInfo): ClientInfo {
        return createClientUnderManager(managerClientInfo, ClientInfo())
    }

    fun createDefaultClientWithRole(role: RbacRole): ClientInfo {
        return createClient(ClientInfo(client = defaultClient(role)))
    }

    fun createDefaultClientWithRoleInAnotherShard(role: RbacRole): ClientInfo {
        return createClient(ClientInfo(client = defaultClient(role), shard = ANOTHER_SHARD))
    }

    fun createDefaultClient(user: User): ClientInfo {
        return createClient(
            ClientInfo()
                .withClient(
                    defaultClient(user.role)
                        .withSubRole(user.subRole)
                        .withAgencyClientId(user.agencyClientId)
                        .withAgencyUserId(user.agencyUserId)
                        .withPrimaryManagerUid(user.managerUserId)
                )
                .withChiefUserInfo(UserInfo().withUser(user))
        )
    }

    fun createDefaultAgency(): ClientInfo {
        return createDefaultClientWithRole(RbacRole.AGENCY);
    }

    fun createDefaultInternalClient(): ClientInfo {
        return createClient(defaultInternalClient())
    }

    fun createClient(client: Client): ClientInfo {
        return createClient(ClientInfo(client = client))
    }

    fun createClient(clientInfo: ClientInfo): ClientInfo {
        return createClient(clientInfo, true)
    }

    fun createClientUnderAgency(agencyClientInfo: ClientInfo): ClientInfo {
        return createClientUnderAgency(agencyClientInfo, ClientInfo());
    }

    fun createClientUnderAgency(agencyClientInfo: ClientInfo, clientInfo: ClientInfo): ClientInfo {
        check(agencyClientInfo.client?.role == RbacRole.AGENCY) { "agencyClientInfo must be with agency role" }
        clientInfo.client = clientInfo.client ?: defaultClient()
        val client = clientInfo.client!!

        client.agencyClientId = agencyClientInfo.clientId!!.asLong()
        client.agencyUserId = agencyClientInfo.uid
        return createClient(clientInfo)
    }

    fun createClientUnderAgency(agencyUserInfo: UserInfo, clientInfo: ClientInfo): ClientInfo {
        check(agencyUserInfo.clientInfo?.client?.role == RbacRole.AGENCY) { "agencyClientInfo must be with agency role" }
        clientInfo.client = clientInfo.client ?: defaultClient()
        val client = clientInfo.client!!

        client.agencyClientId = agencyUserInfo.clientId.asLong()
        client.agencyUserId = agencyUserInfo.uid
        return createClient(clientInfo)
    }

    fun createClientUnderManager(managerClientInfo: ClientInfo, clientInfo: ClientInfo): ClientInfo {
        check(managerClientInfo.client?.role == RbacRole.MANAGER) { "managerClientInfo must be with manager role" }
        createClient(clientInfo, true)
        val client = clientInfo.client!!
        client.primaryManagerUid = managerClientInfo.uid

        if (client.primaryManagerUid != null) {
            if (client.role == RbacRole.CLIENT) {
                testClientRepository.bindManagerToClient(
                    clientInfo.shard, clientInfo.clientId,
                    client.primaryManagerUid
                )
            } else if (client.role == RbacRole.AGENCY) {
                testClientRepository.bindManagerToAgency(
                    clientInfo.shard, clientInfo.clientId,
                    managerClientInfo.chiefUserInfo!!.user
                )
            }
        }
        return clientInfo
    }

    fun createClient(clientInfo: ClientInfo, isUserNeeded: Boolean): ClientInfo {
        checkClientInfoState(clientInfo)

        //Генерируем uid, если передан null
        var uid = clientInfo.chiefUserInfo?.user?.uid ?: 0L
        if (uid == 0L) {
            checkNotNull(passportClientStub) { "Unsupported with this configuration" }
            uid = passportClientStub.generateNewUserUid()
        }

        val client = clientInfo.client ?: defaultClient(uid)
        if (clientInfo.client == null) {
            clientInfo.client = client
        }

        if (client.chiefUid == null) {
            client.chiefUid = uid
        }

        if (client.clientId == null) {
            createClient(client, uid, clientInfo.shard)
        }

        if (isUserNeeded && clientInfo.chiefUserInfo?.user?.uid == null) {
            clientInfo.chiefUserInfo = clientInfo.chiefUserInfo ?: UserInfo()
            val chiefUserInfo = clientInfo.chiefUserInfo!!
            chiefUserInfo.user = chiefUserInfo.user ?: TestUsers.generateNewUser()
            chiefUserInfo.user!!.uid = clientInfo.client!!.chiefUid
            chiefUserInfo.clientInfo = clientInfo
            baseUserSteps.createUser(chiefUserInfo)
        }

        if (clientInfo.clientLimits == null) {
            val clientLimits = ClientLimits()
            clientLimits.clientId = clientInfo.clientId
            clientInfo.clientLimits = clientLimits
            testClientLimitsRepository.addClientLimits(clientInfo.shard, clientLimits)
        }
        initClientNds(clientInfo.shard, client.clientId)

        return clientInfo
    }

    fun updateClientLimits(clientInfo: ClientInfo) {
        testClientLimitsRepository.addClientLimits(clientInfo.shard, clientInfo.clientLimits)
    }

    fun generateNewClientId(): ClientId {
        checkNotNull(balanceClient) { "Unsupported with this configuration" }
        return ClientId.fromLong((balanceClient as BalanceClientStub).createClient())
    }

    fun generateNewUidAndClientId(): UidAndClientId {
        checkNotNull(passportClientStub) { "Unsupported with this configuration" }
        return UidAndClientId.of(passportClientStub.generateNewUserUid(), generateNewClientId())
    }

    fun setOverdraftOptions(
        shard: Int, clientId: ClientId, overdraftLimit: BigDecimal?, debt: BigDecimal?,
        balanceBanned: Boolean?
    ) {
        val balanceTid = getNextBalanceTid(clientId)
        clientOptionsRepository
            .insertOrUpdateBalanceClientOptions(
                shard, clientId, balanceTid, overdraftLimit, debt, null,
                balanceBanned, false, null, null
            )
    }

    fun getNextBalanceTid(clientId: ClientId): Long {
        balanceTidMap.putIfAbsent(clientId, AtomicLong(0))
        return balanceTidMap[clientId]!!.incrementAndGet()
    }

    fun setFakeConnectOrgId(clientInfo: ClientInfo) {
        val fakeConnectOrgId = 10000L + clientInfo.clientId!!.asLong()
        setClientProperty(clientInfo, Client.CONNECT_ORG_ID, fakeConnectOrgId)
        clientInfo.client!!.connectOrgId = fakeConnectOrgId
    }

    fun <V> setClientProperty(clientInfo: ClientInfo, property: ModelProperty<in Client, V>, value: V) {
        val client = clientInfo.client
        require(Client.allModelProperties().contains(property))
        { "Model " + property.getModelClass().name + " doesn't contain property " + property.name() }
        val appliedChanges = ModelChanges(client!!.id, Client::class.java)
            .process(value, property)
            .applyTo(client)
        clientRepository.update(clientInfo.shard, listOf(appliedChanges))
    }

    fun <V> setClientLimit(clientInfo: ClientInfo, property: ModelProperty<ClientLimitsBase, V>, value: V) {
        val clientIds = setOf(clientInfo.clientId!!.asLong())
        val clientLimits = clientLimitsRepository.fetchByClientIds(clientInfo.shard, clientIds).firstOrNull()!!
        property.set(clientLimits, value);
        testClientLimitsRepository.addClientLimits(clientInfo.shard, clientLimits)
    }

    /**
     * Безопасно мокает клиента (организацию) в клиенте (программном) к Балансу.
     * Ответ отдаётся только при совпадении ClientId в запросе с заданным клиентом.
     * <p>
     * Такое название для единообразия с аналогичным методом в UserSteps и в перспективе возможно пополнение списка
     * внешних клиентов.
     */
    fun mockClientInExternalClients(client: Client, representatives: Collection<User>) {
        val clientId = client.clientId
        val clientPassportInfoList = mapList(representatives) { user ->
            ClientPassportInfo().withUid(user.uid)
                .withClientId(clientId)
                .withLogin(user.login)
                .withName(user.fio)
                .withIsMain(if (user.uid == client.chiefUid) 1 else 0)
        }
        doReturn(clientPassportInfoList)
            .`when`(balanceClient)?.getClientRepresentativePassports(
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.eq<Long>(clientId)
            )
    }

    fun addCommonMetrikaCounters(clientInfo: ClientInfo, counters: List<Long>?) {
        testClientRepository.addCommonMetrikaCounters(clientInfo.shard, clientInfo.clientId, counters)
    }

    fun initAgencyNds(shard: Int, clientId: Long?, percent: Percent) {
        testAgencyNdsRepository.addAgencyNds(
            shard,
            AgencyNds()
                .withClientId(clientId)
                .withNds(percent)
                .withDateFrom(LocalDate.now().minusYears(1))
                .withDateTo(LocalDate.now().plusYears(1))
        )
    }

    fun deleteClientNds(shard: Int, clientId: Long) {
        testClientNdsRepository.deleteClientNds(shard, clientId)
    }

    private fun createClient(client: Client, uid: Long, shard: Int): ClientId {
        val clientId = generateNewClientId()
        client.id = clientId.asLong()

        testClientRepository.addClient(shard, client)
        shardSupport.saveValue(ShardKey.UID, uid, ShardKey.CLIENT_ID, clientId)

        return clientId
    }

    private fun initClientNds(shard: Int, clientId: Long?) {
        testClientNdsRepository.addClientNds(
            shard,
            ClientNds()
                .withClientId(clientId)
                .withNds(DEFAULT_NDS)
                .withDateFrom(LocalDate.now().minusYears(1))
                .withDateTo(LocalDate.now().plusYears(1))
        )
    }

    private fun checkClientInfoState(clientInfo: ClientInfo) {
        val user = clientInfo.chiefUserInfo?.user

        check(user?.uid == null && user?.login == null || user.uid != null && user.login != null) {
            "Uid and Login must be set together"
        }
    }

    fun addClientToMcc(controlClientInfo: ClientInfo, managedClientInfo: ClientInfo) {
        rbacClientsRelations.addClientMccRelation(controlClientInfo.clientId!!, managedClientInfo.clientId!!)
    }
}
