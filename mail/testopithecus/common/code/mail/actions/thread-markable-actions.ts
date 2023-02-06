import { Int32 } from '../../../ys/ys'
import { App, FeatureID, MBTAction, MBTActionType, MBTComponent, MBTHistory } from '../../mbt/mbt-abstractions'
import { Testopithecus } from '../logging/events/testopithecus'
import { TestopithecusEvent } from '../logging/testopithecus-event'
import {
  ExpandableThreads,
  ExpandableThreadsFeature,
  ExpandableThreadsModelFeature,
  ReadOnlyExpandableThreads,
} from '../mail-features'

export abstract class AbstractExpandableThreadsAction implements MBTAction {
  constructor(protected threadOrder: Int32, private type: MBTActionType) {
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return ExpandableThreadsModelFeature.get.included(modelFeatures) && ExpandableThreadsFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): boolean {
    return this.canBePerformedImpl(ExpandableThreadsModelFeature.get.forceCast(model))
  }

  public perform(model: App, application: App, history: MBTHistory): MBTComponent {
    this.performImpl(ExpandableThreadsFeature.get.forceCast(model))
    this.performImpl(ExpandableThreadsFeature.get.forceCast(application))
    return history.currentComponent
  }

  public events(): TestopithecusEvent[] {
    return [Testopithecus.stubEvent()]
  }

  public abstract canBePerformedImpl(model: ReadOnlyExpandableThreads): boolean

  public abstract performImpl(modelOrApplication: ExpandableThreads): void

  public abstract tostring(): string

  public getActionType(): MBTActionType {
    return this.type
  }
}

export class ExpandThreadAction extends AbstractExpandableThreadsAction {
  public static readonly type: MBTActionType = 'ExpandThread'

  constructor(threadOrder: number) {
    super(threadOrder, ExpandThreadAction.type)
  }

  public canBePerformedImpl(model: ReadOnlyExpandableThreads): boolean {
    return !model.isExpanded(this.threadOrder) && model.getMessagesInThread(this.threadOrder).length > 1
  }

  public performImpl(modelOrApplication: ExpandableThreads): void {
    modelOrApplication.expandThread(this.threadOrder)
  }

  public tostring(): string {
    return `ExpandThreadAction(${this.threadOrder})`
  }
}

export class CollapseThreadAction extends AbstractExpandableThreadsAction {
  public static readonly type: MBTActionType = 'CollapseThread'

  constructor(threadOrder: number) {
    super(threadOrder, CollapseThreadAction.type)
  }

  public canBePerformedImpl(model: ReadOnlyExpandableThreads): boolean {
    return model.isExpanded(this.threadOrder)
  }

  public performImpl(modelOrApplication: ExpandableThreads): void {
    modelOrApplication.collapseThread(this.threadOrder)
  }

  public tostring(): string {
    return `CollapseThreadAction(${this.threadOrder})`
  }
}

export class MarkAsReadExpandedAction extends AbstractExpandableThreadsAction {
  public static readonly type: MBTActionType = 'MarkAsReadExpanded'

  constructor(threadOrder: Int32, private messageOrder: Int32) {
    super(threadOrder, MarkAsReadExpandedAction.type)
  }

  public canBePerformedImpl(model: ReadOnlyExpandableThreads): boolean {
    return model.isExpanded(this.threadOrder) && !model.isRead(this.threadOrder, this.messageOrder)
  }

  public performImpl(modelOrApplication: ExpandableThreads): void {
    modelOrApplication.markThreadMessageAsRead(this.threadOrder, this.messageOrder)
  }

  public tostring(): string {
    return `MarkAsReadExpandedAction(${this.threadOrder}, ${this.messageOrder})`
  }
}

export class MarkAsUnreadExpandedAction extends AbstractExpandableThreadsAction {
  public static readonly type: MBTActionType = 'MarkAsUnreadExpanded'

  constructor(threadOrder: Int32, private messageOrder: Int32) {
    super(threadOrder, MarkAsUnreadExpandedAction.type)
  }

  public canBePerformedImpl(model: ReadOnlyExpandableThreads): boolean {
    return model.isExpanded(this.threadOrder) && model.isRead(this.threadOrder, this.messageOrder)
  }

  public performImpl(modelOrApplication: ExpandableThreads): void {
    modelOrApplication.markThreadMessageAsUnRead(this.threadOrder, this.messageOrder)
  }

  public tostring(): string {
    return `MarkAsUnreadExpandedAction(${this.threadOrder}, ${this.messageOrder})`
  }
}
