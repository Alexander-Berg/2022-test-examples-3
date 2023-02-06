import { App, FeatureID, MBTAction, MBTActionType, MBTComponent, MBTHistory } from '../../mbt/mbt-abstractions'
import { SettingsComponent } from '../components/settings-component'
import { Testopithecus } from '../logging/events/testopithecus'
import { TestopithecusEvent } from '../logging/testopithecus-event'
import { SettingsNavigatorFeature } from '../mail-features'

export class OpenSettingsAction implements MBTAction {
  public static readonly type: MBTActionType = 'OpenSettings'

  public canBePerformed(model: App): boolean {
    return true
  }

  public events(): TestopithecusEvent[] {
    return [Testopithecus.stubEvent()]
  }

  public perform(model: App, application: App, history: MBTHistory): MBTComponent {
    const modelImpl = SettingsNavigatorFeature.get.forceCast(model)
    const appImpl = SettingsNavigatorFeature.get.forceCast(application)
    modelImpl.openSettings()
    appImpl.openSettings()
    return new SettingsComponent()
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return SettingsNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public tostring(): string {
    return 'OpenSettings'
  }

  public getActionType(): string {
    return OpenSettingsAction.type
  }
}
