package ru.yandex.direct.core.testing.steps

import org.springframework.stereotype.Component
import ru.yandex.direct.core.entity.client.repository.ClientOptionsRepository
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.entity.user.repository.UserRepository
import ru.yandex.direct.core.testing.data.TestUsers
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.repository.TestUserRepository
import ru.yandex.direct.core.testing.stub.PassportClientStub
import ru.yandex.direct.rbac.RbacRole

@Component
class BaseUserSteps(
    private val testUserRepository: TestUserRepository,
    private val userRepository: UserRepository,
    private val passportClientStub: PassportClientStub?,
    private val clientOptionsRepository: ClientOptionsRepository
) {

    //создание user-а вынесено в отдельные степы, чтобы не было кросс зависимости между clientsteps и usersteps
    fun createUser(userInfo: UserInfo) {
        val clientInfo = userInfo.clientInfo!!
        val role = clientInfo.client!!.role
        val user = userInfo.user ?: TestUsers.generateNewUser()

        //записываем роль клиента в user-а
        user.role = role
        user.subRole = clientInfo.client!!.subRole

        if (user.clientId == null) {
            user.clientId = clientInfo.clientId
        }

        if (user.chiefUid == null) {
            user.chiefUid = clientInfo.client!!.chiefUid
        }

        //для внутренних ролей выставляем логин
        if (role.isInternal || role.anyOf(RbacRole.LIMITED_SUPPORT)) {
            user.domainLogin = TestUsers.generateRandomLogin()
        }

        if (user.uid == null) {
            user.uid = createUserInternal(user)
        } else if (!testUserRepository.userExists(user.uid)) {
            user.uid = createUserInternal(user.uid, user)
        }

        if (role == RbacRole.MANAGER) {
            userRepository.addManagerInHierarchy(clientInfo.shard, clientInfo.clientId, user.uid)
        }

        // часть данных из User хранятся в client_options, нужно их проставить
        if (user.canManagePricePackages != null) {
            clientOptionsRepository
                .updateCanManagePricePackage(clientInfo.shard, clientInfo.clientId!!, user.canManagePricePackages)
        }
        if (user.canApprovePricePackages != null) {
            clientOptionsRepository
                .updateCanApprovePricePackage(clientInfo.shard, clientInfo.clientId!!, user.canApprovePricePackages)
        }
    }

    private fun createUserInternal(user: User): Long {
        checkNotNull(passportClientStub) { "Unsupported with this configuration" }
        val uid = passportClientStub.generateNewUserUid()
        return createUserInternal(uid, user)
    }

    private fun createUserInternal(uid: Long, user: User): Long {
        checkNotNull(passportClientStub) { "Unsupported with this configuration" }
        user.uid = uid
        if (user.login == null) {
            user.login = passportClientStub.getLoginByUid(user.uid)
        }
        testUserRepository.addUser(user)
        return uid
    }
}
