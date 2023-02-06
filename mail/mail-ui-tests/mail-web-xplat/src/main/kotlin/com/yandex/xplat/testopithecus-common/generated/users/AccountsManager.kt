// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM users/accounts-manager.ts >>>

package com.yandex.xplat.testopithecus.common

import com.yandex.xplat.common.YSArray
import com.yandex.xplat.common.filter

public open class AccountsManager(val accounts: YSArray<UserAccount>, var indexesOfLoggedInAccounts: YSArray<Int> = mutableListOf(), var currentAccount: Int? = null, var indexesOfRevokedTokenAccounts: YSArray<Int> = mutableListOf()) {
    open fun logInToAccount(account: UserAccount): Unit {
        if (this.isAccountLoggedIn(account.login)) {
            this.switchToAccount(account.login)
        }
        for (i in (0 until this.accounts.size step 1)) {
            if (this.accounts[i].login == account.login && this.accounts[i].password == account.password) {
                this.indexesOfLoggedInAccounts.add(i)
                this.currentAccount = i
                return
            }
        }
        throw Error("Account (login=${account.login};password=${account.password}) hasn't been downloaded yet")
    }

    open fun switchToAccount(login: String): Unit {
        if (!this.isAccountLoggedIn(login)) {
            throw Error("Account for (login=${login}) hasn't been logged in yet")
        }
        for (i in (0 until this.accounts.size step 1)) {
            if (this.accounts[i].login == login) {
                this.currentAccount = i
                return
            }
        }
        throw Error("Account for (login=${login}) hasn't been logged in yet")
    }

    open fun switchToAccountByOrder(loginOrder: Int): Unit {
        this.switchToAccount(this.accounts[loginOrder].login)
    }

    open fun isLoggedIn(): Boolean {
        return this.currentAccount != null
    }

    open fun isAccountLoggedIn(login: String): Boolean {
        return this.indexesOfLoggedInAccounts.filter( {
            i ->
            this.accounts[i].login == login
        }).size > 0
    }

    open fun isAccountWithExpiredToken(login: String): Boolean {
        return this.indexesOfRevokedTokenAccounts.filter( {
            i ->
            this.accounts[i].login == login
        }).size > 0
    }

    open fun getLoggedInAccounts(): YSArray<UserAccount> {
        val accountsWhichAreLoggedIn: YSArray<UserAccount> = mutableListOf()
        this.indexesOfLoggedInAccounts.forEach( {
            i ->
            accountsWhichAreLoggedIn.add(this.accounts[i])
        })
        return accountsWhichAreLoggedIn
    }

    open fun logoutAccount(login: String): Unit {
        if (!this.isAccountLoggedIn(login)) {
            throw Error("Account for (login=${login}) hasn't been logged in yet")
        }
        for (i in (0 until this.indexesOfLoggedInAccounts.size step 1)) {
            if (this.accounts[this.indexesOfLoggedInAccounts[i]].login == login) {
                this.indexesOfLoggedInAccounts = this.indexesOfLoggedInAccounts.filter( {
                    index ->
                    index != this.indexesOfLoggedInAccounts[i]
                })
                if (this.indexesOfLoggedInAccounts.size == 0) {
                    this.currentAccount = null
                } else if (i == 0) {
                    this.currentAccount = 0
                } else {
                    this.currentAccount = i - 1
                }
                return
            }
        }
        throw Error("Account for (login=${login}) hasn't been logged in yet")
    }

    open fun revokeToken(account: UserAccount): Unit {
        if (!this.isAccountLoggedIn(account.login)) {
            throw Error("Account for (login=${account.login}) hasn't been logged in yet")
        }
        for (i in (0 until this.indexesOfLoggedInAccounts.size step 1)) {
            if (this.accounts[this.indexesOfLoggedInAccounts[i]].login == account.login) {
                if (this.indexesOfLoggedInAccounts[i] != this.currentAccount) {
                    this.indexesOfRevokedTokenAccounts.add(this.indexesOfLoggedInAccounts[i])
                } else {
                    this.changeCurrentAccount()
                }
            }
        }
        throw Error("Account for (login=${account.login}) hasn't been logged in yet")
    }

    open fun copy(): AccountsManager {
        return AccountsManager(copyArray(this.accounts), copyArray(this.indexesOfLoggedInAccounts), this.currentAccount, copyArray(this.indexesOfRevokedTokenAccounts))
    }

    private fun changeCurrentAccount(): Unit {
        for (j in (0 until this.indexesOfLoggedInAccounts.size step 1)) {
            if (this.isAccountWithExpiredToken(this.accounts[this.indexesOfLoggedInAccounts[j]].login)) {
                this.currentAccount = this.indexesOfRevokedTokenAccounts[j]
                break
            }
        }
        this.currentAccount = null
    }

}
