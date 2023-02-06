import { Int32 } from '../../../ys/ys'
import { App, MBTAction, MBTComponent } from '../../mbt/mbt-abstractions'
import { MBTComponentActions } from '../../mbt/walk/behaviour/user-behaviour'
import { OpenAccountSettingsAction } from '../actions/account-navigator-settings-actions'
import { OpenSettingsAction } from '../actions/settings-navigator-actions'
import { MultiAccountFeature, YandexLoginFeature } from '../mail-features'

export class AccountSettingsComponent implements MBTComponent {
  public static readonly type: string = 'AccountSettings'

  public assertMatches(model: App, application: App): void {
  }

  public getComponentType(): string {
    return AccountSettingsComponent.type
  }

  public tostring(): string {
    return this.getComponentType()
  }
}

export class AllAccountSettingsActions implements MBTComponentActions {
  public constructor(private accountIndexes: Int32[]) {
  }

  public getActions(model: App): MBTAction[] {
    const actions: MBTAction[] = []
    actions.push(new OpenSettingsAction())
    MultiAccountFeature.get.performIfSupported(model, (mailboxModel) => {
      for (const index of this.accountIndexes) {
        actions.push(new OpenAccountSettingsAction(index))
      }
    })
    return actions
  }
}
