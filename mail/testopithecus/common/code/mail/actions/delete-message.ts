import { Int32 } from '../../../ys/ys'
import { BaseSimpleAction } from '../../mbt/base-simple-action'
import { Feature, MBTActionType, MBTComponent } from '../../mbt/mbt-abstractions'
import { MaillistComponent } from '../components/maillist-component'
import { MessageComponent } from '../components/message-component'
import { Testopithecus } from '../logging/events/testopithecus'
import { TestopithecusEvent } from '../logging/testopithecus-event'
import {
  DeleteMessage,
  DeleteMessageFeature,
  MessageNavigator,
  MessageNavigatorFeature,
} from '../mail-features'

export class DeleteMessageAction extends BaseSimpleAction<DeleteMessage, MaillistComponent> {
  public static readonly type: MBTActionType = 'DeleteMessage'

  constructor(private order: Int32) {
    super(DeleteMessageAction.type)
  }

  public requiredFeature(): Feature<DeleteMessage> {
    return DeleteMessageFeature.get
  }

  public events(): TestopithecusEvent[] {
    return [Testopithecus.stubEvent()]
  }

  public performImpl(modelOrApplication: DeleteMessage, currentComponent: MaillistComponent): MBTComponent {
    modelOrApplication.deleteMessage(this.order)
    return currentComponent
  }

  public tostring(): string {
    return `DeleteMessage(${this.order})`
  }
}

export class DeleteCurrentMessage extends BaseSimpleAction<MessageNavigator, MessageComponent> {
  public static readonly type: MBTActionType = 'DeleteCurrentMessage'

  constructor() {
    super(DeleteCurrentMessage.type)
  }

  public requiredFeature(): Feature<MessageNavigator> {
    return MessageNavigatorFeature.get
  }

  public events(): TestopithecusEvent[] {
    return [Testopithecus.messageViewEvents.deleteMessage()]
  }

  public performImpl(modelOrApplication: MessageNavigator, currentComponent: MessageComponent): MBTComponent {
    modelOrApplication.deleteCurrentMessage()
    return new MaillistComponent()
  }
}
