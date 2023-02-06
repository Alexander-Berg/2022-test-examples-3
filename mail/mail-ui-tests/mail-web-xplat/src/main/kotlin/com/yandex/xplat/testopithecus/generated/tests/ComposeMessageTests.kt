// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM tests/compose-message-tests.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.common.substr
import com.yandex.xplat.testopithecus.common.MBTPlatform
import com.yandex.xplat.testopithecus.common.TestPlan
import com.yandex.xplat.testopithecus.common.TestSettings
import com.yandex.xplat.testopithecus.common.UserAccount

public open class SendMessageWithBody(): RegularYandexMailTestBase("should receive message with body") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.ignoreOn(MBTPlatform.Android).ignoreOn(MBTPlatform.IOS)
    }

    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
        mailbox.nextMessage("subj")
    }

    open override fun regularScenario(account: UserAccount): TestPlan {
        return (this.yandexLogin(account).then(OpenComposeAction()).then(AddToAction(account.login)).thenChain(mutableListOf(SendPreparedAction(), RefreshMessageListAction())).then(OpenMessageAction(0)))
    }

}

public open class SendMessageWithToAddedFromSuggestTest(): RegularYandexMailTestBase("should receive message with to added from suggest") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.ignoreOn(MBTPlatform.Android).ignoreOn(MBTPlatform.IOS)
    }

    open override fun prepareAccount(builder: MailboxBuilder): Unit {
        builder.nextMessage("subj")
    }

    open override fun regularScenario(account: UserAccount): TestPlan {
        return (this.yandexLogin(account).then(OpenComposeAction()).then(AddToFromSuggestAction(account.login.substr(0, 6))).thenChain(mutableListOf(SendPreparedAction(), RefreshMessageListAction())).then(OpenMessageAction(0)))
    }

}
