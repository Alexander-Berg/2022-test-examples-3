// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM tests/abstract-mail-tests.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.testopithecus.common.AccountType2
import com.yandex.xplat.testopithecus.common.RegularYandexTestBase
import com.yandex.xplat.testopithecus.common.TestPlan
import com.yandex.xplat.testopithecus.common.UserAccount

public abstract class RegularYandexMailTestBase protected constructor(description: String): RegularYandexTestBase<MailboxBuilder>(description) {
    abstract override fun regularScenario(account: UserAccount): TestPlan
    abstract override fun prepareAccount(preparer: MailboxBuilder): Unit
    protected open fun yandexLogin(account: UserAccount): TestPlan {
        return this.login(account, AccountType2.Yandex)
    }

    protected open fun login(account: UserAccount, accountType: AccountType2): TestPlan {
        return TestPlan.empty().then(loginAction(account, accountType))
    }

}
