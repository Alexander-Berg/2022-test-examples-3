import { BaseSimpleAction } from '../../mbt/base-simple-action'
import {
  Feature, MBTActionType,
  MBTComponent,
} from '../../mbt/mbt-abstractions'
import { Testopithecus } from '../logging/events/testopithecus'
import { TestopithecusEvent } from '../logging/testopithecus-event'
import { Settings, SettingsFeature } from '../mail-features'

export class ClearCache extends BaseSimpleAction<Settings, MBTComponent> {
  public static readonly type: MBTActionType = 'ClearCache'

  constructor() {
    super(ClearCache.type)
  }

  public requiredFeature(): Feature<Settings> {
    return SettingsFeature.get
  }

  public performImpl(modelOrApplication: Settings, currentComponent: MBTComponent): MBTComponent {
    modelOrApplication.clearCache()
    return currentComponent
  }

  public events(): TestopithecusEvent[] {
    return [Testopithecus.stubEvent()]
  }

  public tostring(): string {
    return this.getActionType()
  }
}
