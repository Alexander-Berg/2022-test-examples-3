// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM event-logs/log-generated-test.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.testopithecus.common.TestPlan
import com.yandex.xplat.testopithecus.common.UserAccount

public open class LogGeneratedTest(var plan: TestPlan): RegularYandexMailTestBase("Test was generated from logs") {
    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
        for (i in (0 until 15 step 1)) {
            mailbox.nextMessage("subj${i}")
        }
    }

    open override fun regularScenario(_account: UserAccount): TestPlan {
        return this.plan
    }

}
