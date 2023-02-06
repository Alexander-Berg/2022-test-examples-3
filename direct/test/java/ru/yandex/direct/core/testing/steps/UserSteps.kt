package ru.yandex.direct.core.testing.steps;

import com.nhaarman.mockitokotlin2.doReturn
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.hasProperty
import org.mockito.ArgumentMatchers
import org.mockito.hamcrest.MockitoHamcrest.argThat
import ru.yandex.direct.balance.client.BalanceClient
import ru.yandex.direct.balance.client.model.request.FindClientRequest
import ru.yandex.direct.balance.client.model.request.ListPaymentMethodsSimpleRequest
import ru.yandex.direct.balance.client.model.response.ClientPassportInfo
import ru.yandex.direct.balance.client.model.response.FindClientResponseItem
import ru.yandex.direct.balance.client.model.response.ListPaymentMethodsSimpleResponseItem
import ru.yandex.direct.core.entity.client.model.Client
import ru.yandex.direct.core.entity.user.model.AgencyLimRep
import ru.yandex.direct.core.entity.user.model.BlackboxUser
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.entity.user.repository.UserRepository
import ru.yandex.direct.core.entity.user.service.BlackboxUserService
import ru.yandex.direct.core.testing.data.TestClients
import ru.yandex.direct.core.testing.data.TestUsers.generateNewBlackboxUser
import ru.yandex.direct.core.testing.data.TestUsers.generateNewUser
import ru.yandex.direct.core.testing.info.BlackboxUserInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.repository.TestAgencyRepository
import ru.yandex.direct.core.testing.stub.PassportClientStub
import ru.yandex.direct.i18n.Language
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.model.ModelProperty
import ru.yandex.direct.rbac.RbacRepType
import ru.yandex.direct.rbac.RbacRole
import java.util.Collections.singletonMap

class UserSteps(
    private val baseUserSteps: BaseUserSteps,
    private val userRepository: UserRepository,
    private val clientSteps: ClientSteps,
    private val passportClientStub: PassportClientStub?,
    private val blackboxUserService: BlackboxUserService,
    private val balanceClient: BalanceClient,
    private val agencyRepository: TestAgencyRepository
) {

    /**
     * Создать пользователя с произвольным логином
     */
    fun createDefaultUser(): UserInfo {
        return createUser(generateNewUser())
    }

    fun createDefaultBlackboxUser(): BlackboxUserInfo {
        return createBlackboxUser(generateNewBlackboxUser())
    }

    fun createDefaultUserWithRole(rbacRole: RbacRole): UserInfo {
        return createUser(UserInfo(generateNewUser(), ClientInfo(client = TestClients.defaultClient(rbacRole))))
    }

    fun createUser(clientInfo: ClientInfo, user: User, repType: RbacRepType): UserInfo {
        return createUser(UserInfo()
            .withUser(user
                .withRepType(repType)
                .withClientId(clientInfo.clientId))
            .withClientInfo(clientInfo))
    }

    fun createUser(clientInfo: ClientInfo, repType: RbacRepType): UserInfo {
        val user = generateNewUser()
            .withRepType(repType)
        return createUser(UserInfo()
            .withUser(user
                .withClientId(clientInfo.clientId))
            .withClientInfo(clientInfo))
    }

    fun createUser(user: User): UserInfo {
        return createUser(UserInfo(user))
    }

    fun createBlackboxUser(user: BlackboxUser): BlackboxUserInfo {
        return createBlackboxUser(BlackboxUserInfo(user))
    }

    fun createRepresentative(clientInfo: ClientInfo): UserInfo {
        return createRepresentative(clientInfo, RbacRepType.MAIN)
    }

    fun createRepresentativeWithFio(clientInfo: ClientInfo, fio: String): UserInfo {
        return createRepresentativeWithFio(clientInfo, RbacRepType.MAIN, fio)
    }

    fun createReadonlyRepresentative(clientInfo: ClientInfo): UserInfo {
        return createRepresentative(clientInfo, RbacRepType.READONLY)
    }

    fun createReadonlyRepresentativeWithFio(clientInfo: ClientInfo, fio: String): UserInfo {
        return createRepresentativeWithFio(clientInfo, RbacRepType.READONLY, fio)
    }

    fun createAgencyLimRep(clientInfo: ClientInfo): UserInfo {
        return createRepresentative(clientInfo, RbacRepType.LIMITED)
    }

    fun createAgencyLimRep(clientInfo: ClientInfo, agencyLimRep: AgencyLimRep): UserInfo {
        val agencyLimRepUserInfo = createRepresentative(clientInfo, RbacRepType.LIMITED)
        agencyLimRep.uid = agencyLimRepUserInfo.uid
        agencyRepository.addUsersAgency(agencyLimRepUserInfo.shard, agencyLimRep)
        return agencyLimRepUserInfo
    }

    fun createRepresentative(clientInfo: ClientInfo, repType: RbacRepType): UserInfo {
        return createUser(UserInfo(generateNewUser()
            .withClientId(clientInfo.clientId)
            .withRepType(repType)
            .withIsReadonlyRep(RbacRepType.READONLY == repType), clientInfo))
    }

    fun createRepresentativeWithFio(clientInfo: ClientInfo, repType: RbacRepType, fio: String): UserInfo {
        return createUser(UserInfo(generateNewUser()
            .withClientId(clientInfo.clientId)
            .withRepType(repType)
            .withFio(fio), clientInfo))
    }

    fun createDeletedUser(clientInfo: ClientInfo) {
        val user = createUserInBlackboxStub()
        setDeletedUsers(clientInfo, listOf(user))
    }

    fun setDeletedUsers(clientInfo: ClientInfo, deletedReps: Collection<User>) {
        val deletedRepsString = "[" + deletedReps.joinToString(",") {
            String.format(
                "{\"uid\":\"%1\$d\",\"login\":\"%2\$s\",\"email\":\"%3\$s\",\"phone\":\"%4\$s\",\"fio\":\"%5\$s\"}",
                it.uid, it.login, it.email, it.phone ?: "123456", it.fio
            )
        } + "]"
        clientSteps.setClientProperty(clientInfo, Client.DELETED_REPS, deletedRepsString)
    }

    fun createUser(userInfo: UserInfo): UserInfo {
        val user: User = userInfo.user ?: generateNewUser()
        if (userInfo.user == null) {
            userInfo.user = user
        }

        if (userInfo.clientInfo?.client?.clientId == null) {
            val clientInfo = clientSteps.createClient(userInfo.clientInfo
                ?: ClientInfo().withChiefUserInfo(userInfo), false)
            userInfo.clientInfo = clientInfo
            clientInfo.chiefUserInfo = userInfo

            //uid генерируется в createClient
            user.uid = clientInfo.client!!.chiefUid
            checkNotNull(passportClientStub) { "Unsupported with this configuration" }
            user.login = passportClientStub.getLoginByUid(user.uid)
        }
        baseUserSteps.createUser(userInfo)

        return userInfo
    }

    fun createBlackboxUser(userInfo: BlackboxUserInfo): BlackboxUserInfo {
        val user: BlackboxUser = userInfo.user ?: generateNewBlackboxUser()
        if (userInfo.user == null) {
            userInfo.user = user
        }

        return userInfo
    }

    fun generateNewUserUid(): Long {
        checkNotNull(passportClientStub) { "Unsupported with this configuration" }
        return passportClientStub.generateNewUserUid()
    }

    //Если пользователь с таким логином сущейтсвует, то возвращается uid из базы
    fun generateNewUserUidSafely(login: String): Long {
        checkNotNull(passportClientStub) { "Unsupported with this configuration" }
        val uid = passportClientStub.getUidByLogin(login)
        return uid ?: passportClientStub.generateNewUserUid(login)
    }

    fun getUserLogin(uid: Long): String {
        checkNotNull(passportClientStub) { "Unsupported with this configuration" }
        return passportClientStub.getLoginByUid(uid)
    }

    /**
     * Создаёт пользователя в имитации BlackBox для тестов. В базу данных пользователь НЕ записывается,
     * но может быть записан, т.к. Uid и логин созданного пользователя не будут пересекаться с другими
     * uid-ами и логинами создаваемыми в тестах пользователей.
     * У пользователя заполнены только те поля, которые можно заполнить по даннным из BlackBox!
     */
    fun createUserInBlackboxStub(): User {
        checkNotNull(passportClientStub) { "Unsupported with this configuration" }
        val uid = passportClientStub.generateNewUserUid()
        val login = passportClientStub.getLoginByUid(uid)
        return User()
            .withUid(uid)
            .withLogin(login)
            .withEmail("$login@yandex.ru")
            .withFio("Ivanov Ivan")
            .withLang(Language.RU)
    }

    fun createBlackboxUserInBlackboxStub(): BlackboxUser {
        checkNotNull(passportClientStub) { "Unsupported with this configuration" }
        val uid = passportClientStub.generateNewUserUid()
        val login = passportClientStub.getLoginByUid(uid)

        return BlackboxUser(uid, login, "0/0", "$login@yandex.ru", "Ivanov Ivan", Language.RU)
    }

    /**
     * Безопасно мокает пользователя в клиентах к Черному ящику и Балансу.
     * Пользователь отдаётся только при совпадении uid'а в запросе с заданным пользователем.
     */
    fun mockUserInExternalClients(user: User) {
        val uid = user.uid
        val blackboxUser = BlackboxUser(user.uid, user.login, "0/0", user.email, user.fio, user.lang)

        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "UNCHECKED_CAST")
        doReturn(singletonMap(uid, blackboxUser))
            .`when`(blackboxUserService)
            .getUsersInfo(argThat(contains(uid)) as Collection<Long?>?)

        doReturn(ListPaymentMethodsSimpleResponseItem().withPaymentMethods(emptyMap()))
            .`when`(balanceClient).listPaymentMethodsSimple(
                argThat(
                    hasProperty<ListPaymentMethodsSimpleRequest>(
                        "uid",
                        equalTo(uid.toString())
                    )
                )
            )
        val findClientResponseItems = user.clientId
            .let { v -> if (v != null) listOf(FindClientResponseItem().withClientId(v.asLong())) else emptyList() }
        doReturn(findClientResponseItems).`when`(balanceClient)
            .findClient(argThat(hasProperty<FindClientRequest>("uid", equalTo(uid.toString()))))

        val passportByUid = ClientPassportInfo()
            .withUid(uid)
            .withClientId(user.clientId?.asLong())
            .withLogin(user.login)
            .withName(user.fio)
            .withIsMain(if (user.repType == RbacRepType.CHIEF) 1 else 0)

        doReturn(passportByUid).`when`(balanceClient)
            .getPassportByUid(ArgumentMatchers.anyLong(), ArgumentMatchers.eq(uid))

        val balanceManagers = if (user.role == RbacRole.MANAGER) setOf(uid) else emptySet()
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "UNCHECKED_CAST")
        doReturn(balanceManagers).`when`(balanceClient)
            .massCheckManagersExist(argThat(contains(uid)) as Collection<Long?>?)
    }

    fun <V> setUserProperty(userInfo: UserInfo, property: ModelProperty<in User?, V>, value: V) {
        val user = userInfo.user!!

        require(User.allModelProperties().contains(property))
        { "Model " + property.getModelClass().name + " doesn't contain property " + property.name() }

        val appliedChanges = ModelChanges(user.id, User::class.java)
            .process(value, property)
            .applyTo(user)
        userRepository.update(userInfo.shard, setOf(appliedChanges))
    }
}
