import { int64 } from '../../../ys/ys'
import { BaseSimpleAction } from '../../mbt/base-simple-action'
import {
  App, Feature,
  FeatureID,
  MBTAction, MBTActionType,
  MBTComponent,
  MBTHistory,
} from '../../mbt/mbt-abstractions'
import { ComposeComponent } from '../components/compose-component'
import { MaillistComponent } from '../components/maillist-component'
import { MessageComponent } from '../components/message-component'
import { Testopithecus } from '../logging/events/testopithecus'
import { TestopithecusEvent } from '../logging/testopithecus-event'
import {
  ComposeMessage,
  ComposeMessageFeature,
  GroupModeFeature,
  MessageNavigator,
  MessageNavigatorFeature,
} from '../mail-features'

export class OpenMessage implements MBTAction {
  public static readonly type: MBTActionType = 'OpenMessage'

  constructor(private order: number) {
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return MessageNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): boolean {
    const groupModeModel = GroupModeFeature.get.castIfSupported(model)
    return groupModeModel === null || !groupModeModel!.isInGroupMode()
  }

  public perform(model: App, application: App, history: MBTHistory): MBTComponent {
    MessageNavigatorFeature.get.forceCast(model).openMessage(this.order)
    MessageNavigatorFeature.get.forceCast(application).openMessage(this.order)
    return new MessageComponent()
  }

  public events(): TestopithecusEvent[] {
    return [Testopithecus.messageListEvents.openMessage(this.order, int64(-1))]
  }

  public tostring(): string {
    return `OpenMessage(${this.order})`
  }

  public getActionType(): MBTActionType {
    return OpenMessage.type
  }
}

export class BackToMaillist extends BaseSimpleAction<MessageNavigator, MBTComponent> {
  public static readonly type: MBTActionType = 'BackToMaillist'

  constructor() {
    super(BackToMaillist.type)
  }

  public requiredFeature(): Feature<MessageNavigator> {
    return MessageNavigatorFeature.get
  }

  public performImpl(modelOrApplication: MessageNavigator, currentComponent: MBTComponent): MBTComponent {
    modelOrApplication.closeMessage()
    return new MaillistComponent()
  }

  public events(): TestopithecusEvent[] {
    return [Testopithecus.messageViewEvents.backToMailList()]
  }
}

export class GoToMessageReply extends BaseSimpleAction<ComposeMessage, MessageComponent> {
  public static readonly type: MBTActionType = 'GoToMessageReply'

  constructor() {
    super(GoToMessageReply.type)
  }

  public requiredFeature(): Feature<ComposeMessage> {
    return ComposeMessageFeature.get
  }

  public performImpl(modelOrApplication: ComposeMessage, currentComponent: MessageComponent): MBTComponent {
    modelOrApplication.goToMessageReply()
    return new ComposeComponent()
  }

  public events(): TestopithecusEvent[] {
    return [Testopithecus.messageViewEvents.reply(0)]
  }
}
