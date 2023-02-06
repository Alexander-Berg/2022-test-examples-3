import { Int32, Nullable, range } from '../../ys/ys'
import { copyArray } from '../utils/utils'
import { UserAccount } from './user-pool'

export class AccountsManager {
  constructor(public readonly accounts: UserAccount[],
              public indexesOfLoggedInAccounts: Int32[] = [],
              public currentAccount: Nullable<Int32> = null) {
  }

  public logInToAccount(account: UserAccount): void {
    if (this.isAccountLoggedIn(account.login)) {
      this.switchToAccount(account.login)
    }

    for (const i of range(0, this.accounts.length)) {
      if (this.accounts[i].login === account.login &&
        this.accounts[i].password === account.password) {
        this.indexesOfLoggedInAccounts.push(i)
        this.currentAccount = i
        return
      }
    }

    throw new Error(`Account (login=${account.login};password=${account.password}) hasn't been downloaded yet`)
  }

  public switchToAccount(login: string): void {
    if (!this.isAccountLoggedIn(login)) {
      throw new Error(`Account for (login=${login}) hasn't been logged in yet`)
    }

    for (const i of range(0, this.accounts.length)) {
      if (this.accounts[i].login === login) {
        this.currentAccount = i
        return
      }
    }
    throw new Error(`Account for (login=${login}) hasn't been logged in yet`)
  }

  public isLoggedIn(): boolean {
    return this.currentAccount !== null
  }

  public isAccountLoggedIn(login: string): boolean {
    return this.indexesOfLoggedInAccounts
      .filter((i) => this.accounts[i].login === login).length > 0
  }

  public getLoggedInAccounts(): UserAccount[] {
    const accountsWhichAreLoggedIn: UserAccount[] = []
    this.indexesOfLoggedInAccounts.forEach((i) => accountsWhichAreLoggedIn.push(this.accounts[i]))
    return accountsWhichAreLoggedIn
  }

  public copy(): AccountsManager {
    return new AccountsManager(this.accounts, copyArray(this.indexesOfLoggedInAccounts), this.currentAccount)
  }
}
