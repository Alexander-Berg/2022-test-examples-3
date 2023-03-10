// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/components/accounts-list-component.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.common.YSArray
import com.yandex.xplat.testopithecus.common.*

public open class AccountsListComponent: MBTComponent {
    open override fun getComponentType(): MBTComponentType {
        return AccountsListComponent.type
    }

    open override fun assertMatches(model: App, application: App): Unit {
    }

    open override fun tostring(): String {
        return this.getComponentType()
    }

    companion object {
        @JvmStatic val type: String = "AccountSwitcher"
    }
}

public open class AllAccountsManagerActions(private var accounts: YSArray<UserAccount>): MBTComponentActions {
    open override fun getActions(model: App): YSArray<MBTAction> {
        val actions: YSArray<MBTAction> = mutableListOf()
        AccountsListFeature.`get`.performIfSupported(model, __LBL__AccountsListComponent_1@ {
            mailboxModel ->
            for (i in (0 until this.accounts.size step 1)) {
                actions.add(ChoseAccountFromAccountsListAction(this.accounts[i]))
            }
        })
        return actions
    }

}

