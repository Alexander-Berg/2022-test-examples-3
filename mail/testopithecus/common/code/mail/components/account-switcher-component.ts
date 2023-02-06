import { App, MBTAction, MBTComponent, MBTComponentType } from '../../mbt/mbt-abstractions'
import { MBTComponentActions } from '../../mbt/walk/behaviour/user-behaviour'
import { UserAccount } from '../../users/user-pool'
import { AddNewAccountAction, SwitchAccountAction } from '../actions/multi-account-actions'
import { MultiAccountFeature } from '../mail-features'

export class AccountSwitcherComponent implements MBTComponent {
  public static readonly type: string = 'AccountSwitcher'

  public getComponentType(): MBTComponentType {
    return AccountSwitcherComponent.type
  }

  public assertMatches(model: App, application: App): void {
    // TODO
  }

  public tostring(): string {
    return this.getComponentType()
  }
}

export class AllAccountSwitcherActions implements MBTComponentActions {
  public constructor(private accounts: UserAccount[]) {}

  public getActions(model: App): MBTAction[] {
    const actions: MBTAction[] = []
    MultiAccountFeature.get.performIfSupported(model, (mailboxModel) => {
      for (const acc of this.accounts) {
        actions.push(new SwitchAccountAction(acc.login))
      }
      actions.push(new AddNewAccountAction())
    })
    return actions
  }
}
