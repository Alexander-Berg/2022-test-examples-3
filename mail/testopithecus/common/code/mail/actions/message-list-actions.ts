import { BaseSimpleAction } from '../../mbt/base-simple-action'
import {
  App,
  Feature,
  FeatureID,
  MBTAction,
  MBTActionType,
  MBTComponent,
  MBTHistory,
} from '../../mbt/mbt-abstractions'
import { AccountSwitcherComponent } from '../components/account-switcher-component'
import { Testopithecus } from '../logging/events/testopithecus'
import { TestopithecusEvent } from '../logging/testopithecus-event'
import { MessageListDisplay, MessageListDisplayFeature} from '../mail-features'

export class RefreshMessageListAction extends BaseSimpleAction<MessageListDisplay, MBTComponent> {
  public static readonly type: MBTActionType = 'RefreshMessageList'

  constructor() {
    super(RefreshMessageListAction.type)
  }

  public requiredFeature(): Feature<MessageListDisplay> {
    return MessageListDisplayFeature.get
  }

  public performImpl(modelOrApplication: MessageListDisplay, currentComponent: MBTComponent): MBTComponent {
    modelOrApplication.refreshMessageList()
    return currentComponent
  }

  public events(): TestopithecusEvent[] {
    return [Testopithecus.messageListEvents.refreshMessageList()]
  }

  public tostring(): string {
    return `RefreshMessageList`
  }
}

export class GoToAccountSwitcherAction implements MBTAction {
  public static readonly type: MBTActionType = 'GoToAccountSwitcher'

  public canBePerformed(model: App): boolean {
    return true
  }

  public events(): TestopithecusEvent[] {
    return [Testopithecus.stubEvent()]
  }

  public perform(model: App, application: App, history: MBTHistory): MBTComponent {
    MessageListDisplayFeature.get.forceCast(model).goToAccountSwitcher()
    MessageListDisplayFeature.get.forceCast(application).goToAccountSwitcher()
    return new AccountSwitcherComponent()
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return MessageListDisplayFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public getActionType(): MBTActionType {
    return GoToAccountSwitcherAction.type
  }

  public tostring(): string {
    return this.getActionType()
  }
}
