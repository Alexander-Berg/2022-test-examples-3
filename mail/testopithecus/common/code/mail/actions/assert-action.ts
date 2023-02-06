import { App, FeatureID, MBTAction, MBTActionType, MBTComponent, MBTHistory } from '../../mbt/mbt-abstractions'
import { TestopithecusEvent } from '../logging/testopithecus-event'

export class AssertAction implements MBTAction {
  public static type: MBTActionType = 'Assert'

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return true
  }

  public canBePerformed(model: App): boolean {
    return true
  }

  public perform(model: App, application: App, history: MBTHistory): MBTComponent {
    const currentComponent = history.currentComponent;
    currentComponent.assertMatches(model, application);
    return currentComponent
  }

  public getActionType(): MBTActionType {
    return AssertAction.type
  }

  public events(): TestopithecusEvent[] {
    return []
  }

  public tostring(): string {
    return this.getActionType()
  }
}
