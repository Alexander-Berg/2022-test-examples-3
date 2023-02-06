import { App, FeatureID, MBTAction, MBTActionType, MBTComponent, MBTHistory } from '../../mbt/mbt-abstractions'
import { log } from '../../utils/logger'
import { TestopithecusEvent } from '../logging/testopithecus-event'

export class DebugDumpAction implements MBTAction {
  public static readonly type: MBTActionType = 'DebugDump'

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return true
  }

  public canBePerformed(model: App): boolean {
    return true
  }

  public perform(model: App, application: App, history: MBTHistory): MBTComponent {
    const expected = model.dump()
    const actual = application.dump()
    log(`DEBUG DUMP\nMODEL\n${expected}APPLICATION\n${actual}`)
    return history.currentComponent
  }

  public events(): TestopithecusEvent[] {
    return []
  }

  public tostring(): string {
    return 'DEBUG DUMP'
  }

  public getActionType(): MBTActionType {
    return DebugDumpAction.type
  }
}
