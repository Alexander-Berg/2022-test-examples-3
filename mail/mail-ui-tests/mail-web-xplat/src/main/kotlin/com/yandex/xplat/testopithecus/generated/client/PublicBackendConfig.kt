// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM client/public-backend-config.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.testopithecus.common.AccountType2
import com.yandex.xplat.testopithecus.common.OAuthApplicationCredentialsRegistry
import com.yandex.xplat.testopithecus.common.OAuthCredentials

public open class PublicBackendConfig {
    companion object {
        @JvmStatic var mailBaseUrl: String = "https://mail.yandex.ru/api/mobile"
        @JvmStatic var mailYandexTeamBaseUrl: String = "https://mail.yandex-team.ru/api/mobile"
        @JvmStatic var xenoBaseUrl: String = "https://xeno.mail.yandex.net/api/mobile"
        @JvmStatic var mailApplicationCredentials: OAuthApplicationCredentialsRegistry = OAuthApplicationCredentialsRegistry().register(AccountType2.Yandex, OAuthCredentials("e7618c5efed842be839cc9a580be94aa", "81a97a4e05094a4c96e9f5fa0b21f794")).register(AccountType2.YandexTeam, OAuthCredentials("a517719ccf0c4aebade6cdc90a5aefe2", "71b77d7aa4d54aa09dd68526cc97bb98"))
        @JvmStatic
        open fun baseUrl(accountType: AccountType2): String {
            when (accountType) {
                AccountType2.Yandex -> {
                    return PublicBackendConfig.mailBaseUrl
                }
                AccountType2.YandexTeam -> {
                    return PublicBackendConfig.mailYandexTeamBaseUrl
                }
                else -> {
                    return PublicBackendConfig.xenoBaseUrl
                }
            }
        }

    }
}

