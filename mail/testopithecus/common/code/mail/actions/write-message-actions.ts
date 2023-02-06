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
import { requireNonNull } from '../../utils/utils'
import { ComposeComponent } from '../components/compose-component'
import { MaillistComponent } from '../components/maillist-component'
import { Testopithecus } from '../logging/events/testopithecus'
import { TestopithecusEvent } from '../logging/testopithecus-event'
import { ComposeMessageFeature, WriteMessage, WriteMessageFeature } from '../mail-features'

export abstract class WriteMessageBaseAction extends BaseSimpleAction<WriteMessage, MBTComponent> {
  constructor(type: MBTActionType) {
    super(type)
  }

  public requiredFeature(): Feature<WriteMessage> {
    return WriteMessageFeature.get
  }
}

export class SendMessageAction extends WriteMessageBaseAction {
  public static readonly type: MBTActionType = 'SendMessage'

  constructor(private to: string, private subject: string) {
    super(SendMessageAction.type)
  }

  public performImpl(modelOrApplication: WriteMessage, currentComponent: MBTComponent): MBTComponent {
    modelOrApplication.sendMessage(this.to, this.subject)
    return new MaillistComponent()
  }

  public events(): TestopithecusEvent[] {
    return [Testopithecus.composeEvents.sendMessage()]
  }

  public tostring(): string {
    return `SendMessage(to=${this.to}, subject=${this.subject})`
  }
}

export class OpenComposeAction extends WriteMessageBaseAction {
  public static readonly type: MBTActionType = 'OpenCompose'

  constructor() {
    super(OpenComposeAction.type)
  }

  public performImpl(modelOrApplication: WriteMessage, currentComponent: MBTComponent): MBTComponent {
    modelOrApplication.openCompose()
    return new ComposeComponent()
  }

  public tostring(): string {
    return `OpenCompose`
  }

  public events(): TestopithecusEvent[] {
    return [Testopithecus.messageListEvents.writeNewMessage()]
  }
}

export class ReplyMessageAction implements MBTAction {
  public static readonly type: MBTActionType = 'ReplyMessage'

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return WriteMessageFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): boolean {
    return true
  }

  public perform(model: App, application: App, history: MBTHistory): MBTComponent {
    WriteMessageFeature.get.forceCast(model).replyMessage()
    WriteMessageFeature.get.forceCast(application).replyMessage()
    const mailListOrMessageView = history.previousDifferentComponent
    if (mailListOrMessageView !== null) {
      return mailListOrMessageView!
    }
    return new MaillistComponent()
  }

  public events(): TestopithecusEvent[] {
    return [Testopithecus.composeEvents.sendMessage()]
  }

  public tostring(): string {
    return `ReplyMessageAction`
  }

  public getActionType(): MBTActionType {
    return ReplyMessageAction.type
  }
}

export class SendPreparedAction implements MBTAction {
  public static readonly type: MBTActionType = 'SendPrepared'

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return ComposeMessageFeature.get.included(modelFeatures) &&
      WriteMessageFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): boolean {
    return ComposeMessageFeature.get.forceCast(model).getTo().size > 0
  }

  public perform(model: App, application: App, history: MBTHistory): MBTComponent {
    WriteMessageFeature.get.forceCast(model).sendPrepared()
    WriteMessageFeature.get.forceCast(application).sendPrepared()
    return requireNonNull(history.previousDifferentComponent, 'No previous screen!')
  }

  public events(): TestopithecusEvent[] {
    return [Testopithecus.composeEvents.sendMessage()]
  }

  public tostring(): string {
    return `SendPreparedAction`
  }

  public getActionType(): MBTActionType {
    return SendMessageAction.type
  }
}
