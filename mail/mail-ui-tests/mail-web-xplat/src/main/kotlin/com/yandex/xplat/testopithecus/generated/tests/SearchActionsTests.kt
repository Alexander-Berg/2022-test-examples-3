// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM tests/search-actions-tests.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.testopithecus.common.MBTPlatform
import com.yandex.xplat.testopithecus.common.TestPlan
import com.yandex.xplat.testopithecus.common.TestSettings
import com.yandex.xplat.testopithecus.common.UserAccount

public open class SearchAndMoveToSpamMessage(): RegularYandexMailTestBase("Search. Short swipe menu Пометить спамом") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.androidCase(7379)
    }

    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
        mailbox.nextMessage("subj1")
    }

    open override fun regularScenario(account: UserAccount): TestPlan {
        return this.yandexLogin(account).then(OpenSearchAction()).then(SearchAllMessagesAction()).then(MarkAsSpamFromContextMenuAction(0)).then(CloseSearchAction()).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.spam))
    }

}

public open class SearchAndDeleteMessageFromUserFolder(): RegularYandexMailTestBase("Search. Swipe to delete из результатов поиска по всем папкам") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.androidCase(7360)
    }

    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
        mailbox.createFolder("UserFolder").switchFolder("UserFolder").nextMessage("subj1")
    }

    open override fun regularScenario(account: UserAccount): TestPlan {
        return this.yandexLogin(account).then(OpenSearchAction()).then(SearchAllMessagesAction()).then(DeleteMessageByLongSwipeAction(0)).then(CloseSearchAction()).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.trash))
    }

}

public open class SearchAndDeleteMessageFromSentFolder(): RegularYandexMailTestBase("Search. Swipe to delete из результатов поиска по Отправленным") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.androidCase(7360)
    }

    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
        mailbox.switchFolder(DefaultFolderName.sent).nextMessage("subj1")
    }

    open override fun regularScenario(account: UserAccount): TestPlan {
        return this.yandexLogin(account).then(OpenSearchAction()).then(SearchAllMessagesAction()).then(DeleteMessageByLongSwipeAction(0)).then(CloseSearchAction()).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.trash))
    }

}

public open class SearchAndDeleteMessageShortSwipeFromSubFolder(): RegularYandexMailTestBase("Search. Short swipe. Удаление по кнопке") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.androidCase(7361)
    }

    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
        mailbox.createFolder("subfolder", mutableListOf("folder")).switchFolder("subfolder", mutableListOf("folder")).nextMessage("subj1")
    }

    open override fun regularScenario(account: UserAccount): TestPlan {
        return this.yandexLogin(account).then(OpenSearchAction()).then(SearchAllMessagesAction()).then(DeleteMessageByShortSwipeAction(0)).then(CloseSearchAction()).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.trash))
    }

}

public open class SearchAndDeleteMessageShortSwipeFromTemplates(): RegularYandexMailTestBase("Search. Short swipe. Удаление по кнопке из папки Шаблоны") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.androidCase(7361)
    }

    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
        mailbox.switchFolder(DefaultFolderName.template).nextMessage("subj1")
    }

    open override fun regularScenario(account: UserAccount): TestPlan {
        return this.yandexLogin(account).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.template)).then(OpenSearchAction()).then(SearchAllMessagesAction()).then(DeleteMessageByShortSwipeAction(0)).then(CloseSearchAction()).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.trash))
    }

}

public open class SearchAndGroupDeleteMessageTestFromTemplates(): RegularYandexMailTestBase("Search. Group operation. Удаление письма из папки Шаблоны") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.androidCase(7362)
    }

    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
        mailbox.switchFolder(DefaultFolderName.template).nextMessage("subj1")
    }

    open override fun regularScenario(account: UserAccount): TestPlan {
        return this.yandexLogin(account).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.template)).then(OpenSearchAction()).then(SearchAllMessagesAction()).then(InitialSelectMessage(0)).then(DeleteSelectedMessages()).then(CloseSearchAction()).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.trash))
    }

}

public open class SearchAndGroupDeleteMessageTestFromDraft(): RegularYandexMailTestBase("Search. Group operation. Удаление письма из папки Черновики") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.androidCase(7362)
    }

    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
        mailbox.switchFolder(DefaultFolderName.draft).nextMessage("subj1")
    }

    open override fun regularScenario(account: UserAccount): TestPlan {
        return this.yandexLogin(account).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.draft)).then(OpenSearchAction()).then(SearchAllMessagesAction()).then(InitialSelectMessage(0)).then(DeleteSelectedMessages()).then(CloseSearchAction()).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.trash))
    }

}

public open class SearchAndDeleteMessageShortSwipeMenuFromArchive(): RegularYandexMailTestBase("Search. Short swipe menu. Удаление по кнопке из папки Архив") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.androidCase(7363)
    }

    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
        mailbox.switchFolder(DefaultFolderName.archive).nextMessage("subj1")
    }

    open override fun regularScenario(account: UserAccount): TestPlan {
        return this.yandexLogin(account).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.archive)).then(OpenSearchAction()).then(SearchAllMessagesAction()).then(DeleteMessageFromContextMenuAction(0)).then(CloseSearchAction()).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.trash))
    }

}

public open class SearchAndArchiveMessageShortSwipeTest(): RegularYandexMailTestBase("Search. Swipe to archive из результатов поиска по пользовательской подпапке") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.androidCase(7364)
    }

    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
        mailbox.createFolder("subfolder", mutableListOf("folder")).switchFolder("subfolder", mutableListOf("folder")).nextMessage("subj1")
    }

    open override fun regularScenario(account: UserAccount): TestPlan {
        return this.yandexLogin(account).then(OpenFolderListAction()).then(OpenSettingsAction()).then(OpenGeneralSettingsAction()).then(SetActionOnSwipe(ActionOnSwipe.archive)).then(CloseGeneralSettingsAction()).then(CloseRootSettings()).then(GoToFolderAction("subfolder", mutableListOf("folder"))).then(OpenSearchAction()).then(SearchAllMessagesAction()).then(ArchiveMessageByShortSwipeAction(0)).then(CloseSearchAction()).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.archive))
    }

}

public open class SearchAndArchiveMessageShortSwipeFromSent(): RegularYandexMailTestBase("Search. Swipe to archive из результатов поиска по Отправленным") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.androidCase(7364)
    }

    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
        mailbox.switchFolder(DefaultFolderName.sent).nextMessage("subj1")
    }

    open override fun regularScenario(account: UserAccount): TestPlan {
        return this.yandexLogin(account).then(OpenFolderListAction()).then(OpenSettingsAction()).then(OpenGeneralSettingsAction()).then(SetActionOnSwipe(ActionOnSwipe.archive)).then(CloseGeneralSettingsAction()).then(CloseRootSettings()).then(GoToFolderAction(DefaultFolderName.sent)).then(OpenSearchAction()).then(SearchAllMessagesAction()).then(ArchiveMessageByShortSwipeAction(0)).then(CloseSearchAction()).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.archive))
    }

}

public open class SearchAndMarkImportantMessageShortSwipe(): RegularYandexMailTestBase("Search. Short swipe menu. Пометка письма Важным") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.androidCase(7371).ignoreOn(MBTPlatform.IOS)
    }

    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
        mailbox.switchFolder(DefaultFolderName.draft).nextMessage("subj1")
    }

    open override fun regularScenario(account: UserAccount): TestPlan {
        return this.yandexLogin(account).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.draft)).then(OpenSearchAction()).then(SearchAllMessagesAction()).then(MarkAsImportantFromContextMenuAction(0)).then(CloseSearchAction()).then(OpenFolderListAction()).then(GoToFilterImportantAction())
    }

}

public open class SearchAndMarkMessageRead(): RegularYandexMailTestBase("Search. Group operation. Пометка письма из папки Архив прочитанным") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.androidCase(7356)
    }

    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
        mailbox.switchFolder(DefaultFolderName.archive).nextMessage("subj1")
    }

    open override fun regularScenario(account: UserAccount): TestPlan {
        return this.yandexLogin(account).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.archive)).then(OpenSearchAction()).then(SearchAllMessagesAction()).then(InitialSelectMessage(0)).then(MarkAsReadSelectedMessages())
    }

}

public open class SearchAndMarkMessageReadFromUserFolder(): RegularYandexMailTestBase("Search. Group operation. Пометка письма из user subfolder прочитанным") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.androidCase(7356)
    }

    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
        mailbox.createFolder("subfolder", mutableListOf("folder")).switchFolder("subfolder", mutableListOf("folder")).nextMessage("subj1")
    }

    open override fun regularScenario(account: UserAccount): TestPlan {
        return this.yandexLogin(account).then(OpenFolderListAction()).then(GoToFolderAction("subfolder", mutableListOf("folder"))).then(OpenSearchAction()).then(SearchAllMessagesAction()).then(InitialSelectMessage(0)).then(MarkAsReadSelectedMessages())
    }

}

public open class SearchAndMarkMessageUnreadFromUserFolder(): RegularYandexMailTestBase("Search. Group operation. Пометка письма из user subfolder непрочитанным") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.androidCase(7357)
    }

    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
        mailbox.createFolder("subfolder", mutableListOf("folder")).switchFolder("subfolder", mutableListOf("folder")).nextMessage("subj1")
    }

    open override fun regularScenario(account: UserAccount): TestPlan {
        return this.yandexLogin(account).then(OpenFolderListAction()).then(GoToFolderAction("subfolder", mutableListOf("folder"))).then(MarkAsRead(0)).then(OpenSearchAction()).then(SearchAllMessagesAction()).then(InitialSelectMessage(0)).then(MarkAsUnreadSelectedMessages())
    }

}

public open class SearchAndMoveMessageFromUserFolder(): RegularYandexMailTestBase("Search. Short swipe menu. Перемещение писем из user subfolder") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.androidCase(7374)
    }

    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
        mailbox.createFolder("subfolder", mutableListOf("folder")).switchFolder("subfolder", mutableListOf("folder")).nextMessage("subj1")
    }

    open override fun regularScenario(account: UserAccount): TestPlan {
        return this.yandexLogin(account).then(OpenFolderListAction()).then(GoToFolderAction("subfolder", mutableListOf("folder"))).then(OpenSearchAction()).then(SearchAllMessagesAction()).then(MoveToFolderFromContextMenuAction(0, DefaultFolderName.inbox)).then(CloseSearchAction()).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.inbox))
    }

}

public open class SearchAndAddLabelMessageFromUserFolder(): RegularYandexMailTestBase("Search. Short swipe menu. Добавление пользовательской метки для письма из user subfolder") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.androidCase(7377)
    }

    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
        mailbox.createFolder("subfolder", mutableListOf("folder")).switchFolder("subfolder", mutableListOf("folder")).nextMessage("subj1").createLabel("label1")
    }

    open override fun regularScenario(account: UserAccount): TestPlan {
        return this.yandexLogin(account).then(OpenSearchAction()).then(SearchAllMessagesAction()).then(ApplyLabelsFromContextMenuAction(0, mutableListOf("label1"))).then(CloseSearchAction()).then(OpenFolderListAction()).then(GoToLabelAction("label1"))
    }

}

public open class SearchAndAddLabelMessageFromArchive(): RegularYandexMailTestBase("Search. Short swipe menu. Добавление пользовательской метки для письма из Архива") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.androidCase(7377)
    }

    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
        mailbox.switchFolder(DefaultFolderName.archive).nextMessage("subj1").createLabel("label1")
    }

    open override fun regularScenario(account: UserAccount): TestPlan {
        return this.yandexLogin(account).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.archive)).then(OpenSearchAction()).then(SearchAllMessagesAction()).then(ApplyLabelsFromContextMenuAction(0, mutableListOf("label1"))).then(CloseSearchAction()).then(OpenFolderListAction()).then(GoToLabelAction("label1"))
    }

}

public open class SearchAndAddLabelMessage(): RegularYandexMailTestBase("Search. Поставить пользовательскую метку на письмо по селекту") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.androidCase(6090)
    }

    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
        mailbox.nextMessage("subj1").createLabel("label1")
    }

    open override fun regularScenario(account: UserAccount): TestPlan {
        return this.yandexLogin(account).then(OpenSearchAction()).then(SearchAllMessagesAction()).then(InitialSelectMessage(0)).then(ApplyLabelsToSelectedMessagesAction(mutableListOf("label1"))).then(CloseSearchAction()).then(OpenFolderListAction()).then(GoToLabelAction("label1"))
    }

}

public open class SearchAndMarkMessageReadBySwipeFromUserFolder(): RegularYandexMailTestBase("Search. Пометить письмо прочитанным по свайпу") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.androidCase(7354)
    }

    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
        mailbox.createFolder("folder").switchFolder("folder").nextMessage("subj1")
    }

    open override fun regularScenario(account: UserAccount): TestPlan {
        return this.yandexLogin(account).then(OpenFolderListAction()).then(GoToFolderAction("folder")).then(OpenSearchAction()).then(SearchAllMessagesAction()).then(MarkAsRead(0))
    }

}

public open class SearchAndMarkMessageUnreadBySwipeFromSent(): RegularYandexMailTestBase("Search. Пометить письмо непрочитанным по свайпу") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.androidCase(7355)
    }

    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
        mailbox.switchFolder(DefaultFolderName.sent).nextMessage("subj1")
    }

    open override fun regularScenario(account: UserAccount): TestPlan {
        return this.yandexLogin(account).then(OpenFolderListAction()).then(GoToFolderAction(DefaultFolderName.sent)).then(MarkAsRead(0)).then(OpenSearchAction()).then(SearchAllMessagesAction()).then(MarkAsUnread(0))
    }

}
