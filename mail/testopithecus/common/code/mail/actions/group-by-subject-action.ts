import {
  App,
  FeatureID,
  MBTAction,
  MBTActionType,
  MBTComponent,
  MBTComponentType,
  MBTHistory,
} from '../../mbt/mbt-abstractions'
import { Testopithecus } from '../logging/events/testopithecus'
import { TestopithecusEvent } from '../logging/testopithecus-event'
import { GroupBySubjectFeature } from '../mail-features'

export abstract class BaseGroupBySubjectAction implements MBTAction {
  constructor(private type: MBTActionType) {
  }

  public abstract canBePerformed(model: App): boolean

  public events(): TestopithecusEvent[] {
    return [Testopithecus.stubEvent()]
  }

  public perform(model: App, application: App, history: MBTHistory): MBTComponent {
    this.performImpl(model)
    this.performImpl(application)
    return history.currentComponent
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return GroupBySubjectFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public abstract performImpl(modelOrApplication: App): void

  public getActionType(): MBTComponentType {
    return this.type
  }

  public tostring(): string {
    return this.getActionType()
  }
}

export class SwitchOnThreadingAction extends BaseGroupBySubjectAction {
  public static readonly type: MBTActionType = 'SwitchOnThreading'

  constructor() {
    super(SwitchOnThreadingAction.type)
  }

  public canBePerformed(model: App): boolean {
    const groupBySybjectModel = GroupBySubjectFeature.get.forceCast(model)
    return !groupBySybjectModel.getThreadingSetting()
  }

  public performImpl(modelOrApplication: App): void {
    const modelOrAppImpl = GroupBySubjectFeature.get.forceCast(modelOrApplication)
    modelOrAppImpl.toggleThreadingSetting()
  }
}

export class SwitchOffThreadingAction extends BaseGroupBySubjectAction {
  public static readonly type: MBTActionType = 'SwitchOffThreading'

  constructor() {
    super(SwitchOffThreadingAction.type)
  }

  public canBePerformed(model: App): boolean {
    const groupBySybjectModel = GroupBySubjectFeature.get.forceCast(model)
    return groupBySybjectModel.getThreadingSetting()
  }

  public performImpl(modelOrApplication: App): void {
    const modelOrAppImpl = GroupBySubjectFeature.get.forceCast(modelOrApplication)
    modelOrAppImpl.toggleThreadingSetting()
  }
}

export class GetThreadingSetting {
  public canBePerformed(model: App): boolean {
    return true
  }

  public performImpl(modelOrApplication: App): void {
    const modelOrAppImpl = GroupBySubjectFeature.get.forceCast(modelOrApplication)
    modelOrAppImpl.getThreadingSetting()
  }

  public tostring(): string {
    return 'GetThreadingSetting'
  }
}
