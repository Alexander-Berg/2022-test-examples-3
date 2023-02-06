import { App, MBTAction, MBTComponent } from '../../mbt/mbt-abstractions'
import { MBTComponentActions } from '../../mbt/walk/behaviour/user-behaviour'
import { BackToMaillist } from '../actions/open-message'
import { MessageNavigatorFeature } from '../mail-features'
import { FullMessage } from '../model/mail-model'

export class MessageComponent implements MBTComponent {
  public static readonly type: string = 'MessageComponent'

  public getComponentType(): string {
    return MessageComponent.type
  }

  public assertMatches(model: App, application: App): void {
    const messageNavigatorModel = MessageNavigatorFeature.get.castIfSupported(model)
    const messageNavigatorApp = MessageNavigatorFeature.get.castIfSupported(application)
    if (messageNavigatorModel !== null && messageNavigatorApp !== null) {
      const openedMessageInModel = messageNavigatorModel.getOpenedMessage()
      const openedMessageInApp = messageNavigatorModel.getOpenedMessage()
      if (!FullMessage.matches(openedMessageInModel, openedMessageInApp)) {
        throw new Error(`Opened message are different. Expected: "${openedMessageInModel}". Got: "${openedMessageInApp}"`)
      }
    }
  }

  public tostring(): string {
    return this.getComponentType()
  }
}

export class AllMessageActions implements MBTComponentActions {
  public getActions(model: App): MBTAction[] {
    const actions: MBTAction[] = []
    actions.push(new BackToMaillist())
    // actions.push(new ReplyMessageAction());
    return actions
  }
}
