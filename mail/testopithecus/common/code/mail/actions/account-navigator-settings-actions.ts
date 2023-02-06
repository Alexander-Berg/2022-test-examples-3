import { Int32 } from '../../../ys/ys'
import { App, FeatureID, MBTAction, MBTActionType, MBTComponent, MBTHistory } from '../../mbt/mbt-abstractions'
import { AccountSettingsComponent } from '../components/account-settings-component'
import { SettingsComponent } from '../components/settings-component'
import { Testopithecus } from '../logging/events/testopithecus'
import { TestopithecusEvent } from '../logging/testopithecus-event'
import { AccountSettingsNavigatorFeature } from '../mail-features'

export class OpenAccountSettingsAction implements MBTAction {
  public static readonly type: MBTActionType = 'OpenAccountSettings'

  constructor(public accountIndex: Int32) {
  }

  public canBePerformed(model: App): boolean {
    return true
  }

  public events(): TestopithecusEvent[] {
    return [Testopithecus.stubEvent()]
  }

  public getActionType(): string {
    return OpenAccountSettingsAction.type
  }

  public perform(model: App, application: App, history: MBTHistory): MBTComponent {
    const modelImpl = AccountSettingsNavigatorFeature.get.forceCast(model)
    const appImpl = AccountSettingsNavigatorFeature.get.forceCast(application)
    modelImpl.openAccountSettings(this.accountIndex)
    appImpl.openAccountSettings(this.accountIndex)
    return new AccountSettingsComponent()
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return AccountSettingsNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public tostring(): string {
    return 'OpenAccountSettings'
  }
}

export class CloseAccountSettingsAction implements MBTAction {
  public static readonly type: MBTActionType = 'CloseAccountSettings'

  public canBePerformed(model: App): boolean {
    return true
  }

  public events(): TestopithecusEvent[] {
    return [Testopithecus.stubEvent()]
  }

  public getActionType(): string {
    return CloseAccountSettingsAction.type
  }

  public perform(model: App, application: App, history: MBTHistory): MBTComponent {
    const modelImpl = AccountSettingsNavigatorFeature.get.forceCast(model)
    const appImpl = AccountSettingsNavigatorFeature.get.forceCast(application)
    modelImpl.closeAccountSettings()
    appImpl.closeAccountSettings()
    return new SettingsComponent()
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return AccountSettingsNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public tostring(): string {
    return 'CloseAccountSettings'
  }
}
