package ru.yandex.direct.core.testing.steps.uac

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import ru.yandex.direct.core.entity.uac.model.AccountFeatures
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAccountRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAccount
import ru.yandex.direct.core.testing.info.ClientInfo
import java.time.LocalDateTime

@Lazy
@Component
class UacAccountSteps {
    @Autowired
    private lateinit var uacYdbAccountRepository: UacYdbAccountRepository

    fun createAccount(clientInfo: ClientInfo, id: String = UacYdbUtils.generateUniqueRandomId()): UacYdbAccount {
        val account = UacYdbAccount(
            id = id,
            uid = clientInfo.client!!.chiefUid,
            features = AccountFeatures(),
            createdAt = LocalDateTime.now().withNano(0),
            directClientId = clientInfo.clientId!!.asLong(),
        )
        uacYdbAccountRepository.saveAccount(account)
        return account
    }

}
