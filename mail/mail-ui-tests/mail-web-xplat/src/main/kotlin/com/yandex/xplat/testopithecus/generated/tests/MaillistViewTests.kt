// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM tests/maillist-view-tests.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.testopithecus.common.MBTPlatform
import com.yandex.xplat.testopithecus.common.TestPlan
import com.yandex.xplat.testopithecus.common.TestSettings
import com.yandex.xplat.testopithecus.common.UserAccount

public open class ThreadModeTurnOffAndThenOnTest(): RegularYandexMailTestBase("MessageListView. Изменение настройки тредного режима") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.androidCase(6839).iosCase(6793)
    }

    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
        mailbox.nextMessage("subj1").nextMessage("subj2").nextMessage("subj2").nextMessage("subj2").nextMessage("subj1").nextMessage("subj3")
    }

    open override fun regularScenario(account: UserAccount): TestPlan {
        return this.yandexLogin(account).then(OpenFolderListAction()).then(OpenSettingsAction()).then(OpenAccountSettingsAction(0)).then(SwitchOffThreadingAction()).then(CloseAccountSettingsAction()).then(CloseRootSettings()).then(OpenSettingsAction()).then(OpenAccountSettingsAction(0)).then(SwitchOnThreadingAction()).then(CloseAccountSettingsAction()).then(CloseRootSettings()).then(GoToFolderAction(DefaultFolderName.inbox))
    }

}

public open class MailListViewInCompactMode(): RegularYandexMailTestBase("MessageListView. Отображение списка писем папки Входящие в компактном режиме") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.iosCase(641).androidCase(6428).ignoreOn(MBTPlatform.Android)
    }

    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
        mailbox.nextMessage("subj1").nextMessage("subj2").nextMessage("subj3").nextMessage("subj4").nextMessage("subj5")
    }

    open override fun regularScenario(account: UserAccount): TestPlan {
        return this.yandexLogin(account).then(OpenFolderListAction()).then(OpenSettingsAction()).then(OpenGeneralSettingsAction()).then(TurnOnCompactMode()).then(CloseGeneralSettingsAction()).then(CloseRootSettings()).then(GoToFolderAction(DefaultFolderName.inbox))
    }

}

public open class MarkAsImportantMessageInTabSubscriptionTest(): RegularYandexMailTestBase("MessageListView. Отметить письмо важным в табе Рассылки") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.androidCase(551)
    }

    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
        mailbox.turnOnTab().switchFolder(TabBackendName.inbox).nextMessage("subj1").nextMessage("subj2")
    }

    open override fun regularScenario(account: UserAccount): TestPlan {
        return this.yandexLogin(account).then(MoveToFolderAction(0, DefaultFolderName.mailingLists)).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.mailingLists)).then(MarkAsImportant(0))
    }

}

public open class TabNotificationInTheMiddleMailList(): RegularYandexMailTestBase("MessageListView. Плашка таба находится в середине списка писем") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.androidCase(8825)
    }

    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
        mailbox.turnOnTab().switchFolder(TabBackendName.inbox).nextMessage("subj1").nextMessage("subj2").nextMessage("subj3").nextMessage("subj4")
    }

    open override fun regularScenario(account: UserAccount): TestPlan {
        return this.yandexLogin(account).then(MoveToFolderAction(1, DefaultFolderName.socialNetworks))
    }

}

public open class TabNotificationInTheFirstInMailList(): RegularYandexMailTestBase("MessageListView. Плашка таба находится в начале списка писем") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.androidCase(8821)
    }

    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
        mailbox.turnOnTab().switchFolder(TabBackendName.inbox).nextMessage("subj1").nextMessage("subj2").nextMessage("subj3").nextMessage("subj4")
    }

    open override fun regularScenario(account: UserAccount): TestPlan {
        return this.yandexLogin(account).then(MarkAsRead(0)).then(MoveToFolderAction(0, DefaultFolderName.mailingLists))
    }

}

public open class TabNotificationInEmptyMessageList(): RegularYandexMailTestBase("MessageListView. Плашка таба находится в пустом списке писем") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.androidCase(8828)
    }

    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
        mailbox.turnOnTab().switchFolder(TabBackendName.inbox).nextMessage("subj1")
    }

    open override fun regularScenario(account: UserAccount): TestPlan {
        return this.yandexLogin(account).then(MoveToFolderAction(0, DefaultFolderName.mailingLists))
    }

}
