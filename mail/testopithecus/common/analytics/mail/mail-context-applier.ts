import { EventNames } from '../../code/mail/logging/events/event-names';
import { MessageDTO } from '../../code/mail/logging/objects/message';
import { TestopithecusEvent } from '../../code/mail/logging/testopithecus-event';
import { Int32, Int64, undefinedToNull } from '../../ys/ys';
import { ContextApplier } from '../context-applier';
import { MailContext } from './mail-context';

export class MailContextApplier implements ContextApplier<MailContext> {

  public init(): MailContext {
    return new MailContext();
  }

  public apply(event: TestopithecusEvent, context: MailContext): MailContext {
    switch (event.name) {
      case EventNames.MODEL_SYNC_MESSAGE_LIST:
        const messages = event.getAttributes().get('messages') as Array<Map<string, any>>
        for (const message of messages) {
          const messageDTO = MessageDTO.fromMap(message)
          context.messages.set(messageDTO.mid, messageDTO)
        }
        return context
      case EventNames.PUSH_MESSAGES_RECEIVED_SHOWN:
        const mids: Int64[] = undefinedToNull(event.getAttributes().get('mids'))
        const repliesNumbers: Int32[] = undefinedToNull(event.getAttributes().get('repliesNumbers'))
        if (repliesNumbers !== null && mids !== null) {
          for (let i = 0; i < mids.length; i++) {
            context.pushes.set(mids[i], repliesNumbers[i])
          }
        }
        return context
      case EventNames.PUSH_SINGLE_MESSAGE_CLICKED:
        context.currentMessageId = event.getAttributes().get('mid')
        return context
      case EventNames.LIST_MESSAGE_OPEN:
        context.currentMessageId = event.getAttributes().get('mid')
        return context
      case EventNames.LIST_MESSAGE_OPEN_ACTIONS:
        context.currentMessageId = event.getAttributes().get('mid')
        return context
      case EventNames.COMPOSE_BACK:
        context.currentMessageId = null
        return context
      case EventNames.MESSAGE_VIEW_BACK:
        context.currentMessageId = null
        return context
    }
    return context
  }

}
