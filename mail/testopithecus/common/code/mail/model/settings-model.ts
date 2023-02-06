import { Int32, Nullable } from '../../../ys/ys'
import { AccountsManager } from '../../users/accounts-manager'
import { requireNonNull } from '../../utils/utils'
import { AccountSettingsNavigator, GroupBySubject, Settings, SettingsNavigator } from '../mail-features'
import { AccountSettings } from './mail-model'

export class SettingsModel implements Settings, GroupBySubject, SettingsNavigator, AccountSettingsNavigator {
  public constructor(public readonly accountsSettings: AccountSettings[],
                     public readonly accountsManager: AccountsManager,
                     private openedAccount: Nullable<Int32> = null) {
  }

  public clearCache(): void {
  }

  public getThreadingSetting(): boolean {
    return this.accountsSettings[this.demandRequiredAccountIndex()].groupBySubject
  }

  public toggleThreadingSetting(): void {
    const currentAccountSettings: AccountSettings = this.accountsSettings[this.demandRequiredAccountIndex()]
    currentAccountSettings.groupBySubject = !currentAccountSettings.groupBySubject
  }

  public openSettings(): void {
  }

  public openAccountSettings(accountIndex: Int32): void {
    this.openedAccount = this.accountsManager.indexesOfLoggedInAccounts[accountIndex]
  }

  public closeAccountSettings(): void {
    this.openedAccount = null
  }

  private demandRequiredAccountIndex(): Int32 {
    return requireNonNull(this.openedAccount, 'Необходимо зайти в настройки аккаунта')
  }
}
