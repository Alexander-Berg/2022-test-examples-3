// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM tests/login-tests.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.common.YSArray
import com.yandex.xplat.testopithecus.common.*

public open class YandexLoginTest(): MBTTest<MailboxBuilder>("should login 3 yandex accounts") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.iosCase(455)
    }

    open override fun requiredAccounts(): YSArray<AccountType2> {
        return mutableListOf(AccountType2.Yandex, AccountType2.Yandex, AccountType2.Yandex)
    }

    open override fun prepareAccounts(mailboxes: YSArray<MailboxBuilder>): Unit {
        mailboxes[0].nextMessage("firstAccountMsg")
        mailboxes[1].nextMessage("secondAccountMsg")
        mailboxes[2].nextMessage("thirdAccountMsg")
    }

    open override fun scenario(accounts: YSArray<UserAccount>, _model: AppModel?, _supportedFeatures: YSArray<FeatureID>): TestPlan {
        return TestPlan.empty().then(YandexLoginAction(accounts[0])).then(GoToAccountSwitcherAction()).then(AddNewAccountAction()).then(YandexLoginAction(accounts[1])).then(GoToAccountSwitcherAction()).then(AddNewAccountAction()).then(YandexLoginAction(accounts[2]))
    }

}

public open class SwitchAccountTest(): MBTTest<MailboxBuilder>("should switch between 2 yandex accounts") {
    open override fun requiredAccounts(): YSArray<AccountType2> {
        return mutableListOf(AccountType2.Yandex, AccountType2.Yandex)
    }

    open override fun prepareAccounts(mailboxes: YSArray<MailboxBuilder>): Unit {
        mailboxes[0].nextMessage("firstAccountMsg")
        mailboxes[1].nextMessage("secondAccountMsg")
    }

    open override fun scenario(accounts: YSArray<UserAccount>, _model: AppModel?, _supportedFeatures: YSArray<FeatureID>): TestPlan {
        return TestPlan.empty().then(YandexLoginAction(accounts[0])).then(GoToAccountSwitcherAction()).then(AddNewAccountAction()).then(YandexLoginAction(accounts[1])).then(GoToAccountSwitcherAction()).then(SwitchAccountAction(accounts[0]))
    }

}

public open class OuterMailLoginTest(): MBTTest<MailboxBuilder>("?????????????? ???????????????????????? ?????????? mail.ru ??????????????") {
    open override fun requiredAccounts(): YSArray<AccountType2> {
        return mutableListOf(AccountType2.Mail)
    }

    open override fun prepareAccounts(mailboxes: YSArray<MailboxBuilder>): Unit {
        mailboxes[0].nextMessage("pizza")
    }

    open override fun scenario(accounts: YSArray<UserAccount>, model: AppModel?, supportedFeatures: YSArray<FeatureID>): TestPlan {
        return TestPlan.empty().then(MailRuLoginAction(accounts[0]))
    }

}

public open class GenericIMAPOtherLoginTest(): MBTTest<MailboxBuilder>("Account manager. ???????????????????????? ?????????? ?????????????? ?????????? GenericIMAP") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.iosCase(7)
    }

    open override fun requiredAccounts(): YSArray<AccountType2> {
        return mutableListOf(AccountType2.Other)
    }

    open override fun prepareAccounts(mailboxes: YSArray<MailboxBuilder>): Unit {
        mailboxes[0].nextMessage("subj0")
    }

    open override fun scenario(accounts: YSArray<UserAccount>, _model: AppModel?, _supportedFeatures: YSArray<FeatureID>): TestPlan {
        return TestPlan.empty().then(CustomMailServiceLoginAction(accounts[0]))
    }

}

public open class GenericIMAPYandexLoginTest(): MBTTest<MailboxBuilder>("Account manager. ?????????????? ?? ???????????????? ?????????????????????? Yandex ???? ?????????? GenericIMAP") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.iosCase(433)
    }

    open override fun requiredAccounts(): YSArray<AccountType2> {
        return mutableListOf(AccountType2.Yandex)
    }

    open override fun prepareAccounts(mailboxes: YSArray<MailboxBuilder>): Unit {
        mailboxes[0].nextMessage("subj0")
    }

    open override fun scenario(accounts: YSArray<UserAccount>, _model: AppModel?, _supportedFeatures: YSArray<FeatureID>): TestPlan {
        return TestPlan.empty().then(CustomMailServiceLoginAction(accounts[0]))
    }

}

public open class ChoseAccountFromAccountsListTest(): MBTTest<MailboxBuilder>("Account manager. ?????????? ???????????????? ???? ???????????????? ??????????????????") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.iosCase(469)
    }

    open override fun requiredAccounts(): YSArray<AccountType2> {
        return mutableListOf(AccountType2.Yandex, AccountType2.Yandex)
    }

    open override fun prepareAccounts(mailboxes: YSArray<MailboxBuilder>): Unit {
        mailboxes[0].nextMessage("firstAccountMsg")
        mailboxes[1].nextMessage("secondAccountMsg")
    }

    open override fun scenario(accounts: YSArray<UserAccount>, _model: AppModel?, _supportedFeatures: YSArray<FeatureID>): TestPlan {
        return TestPlan.empty().then(YandexLoginAction(accounts[0])).then(GoToAccountSwitcherAction()).then(AddNewAccountAction()).then(YandexLoginAction(accounts[1])).then(GoToAccountSwitcherAction()).then(LogoutFromAccountAction(accounts[1])).then(LogoutFromAccountAction(accounts[0])).then(ChoseAccountFromAccountsListAction(accounts[0]))
    }

}

public open class LogoutWorkingTest(): MBTTest<MailboxBuilder>("Account manager. ???????????????? ???????????? ???????????????????? ?????????? ??????????????") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.iosCase(462)
    }

    open override fun requiredAccounts(): YSArray<AccountType2> {
        return mutableListOf(AccountType2.Yandex, AccountType2.Yandex)
    }

    open override fun prepareAccounts(mailboxes: YSArray<MailboxBuilder>): Unit {
        mailboxes[0].nextMessage("firstAccountMsg")
        mailboxes[1].nextMessage("secondAccountMsg")
    }

    open override fun scenario(accounts: YSArray<UserAccount>, _model: AppModel?, _supportedFeatures: YSArray<FeatureID>): TestPlan {
        return TestPlan.empty().then(YandexLoginAction(accounts[0])).then(GoToAccountSwitcherAction()).then(AddNewAccountAction()).then(YandexLoginAction(accounts[1])).then(RevokeTokenForAccount(accounts[1])).then(RefreshMessageListAction())
    }

}

