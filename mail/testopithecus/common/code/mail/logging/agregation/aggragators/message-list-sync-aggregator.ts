import { Int64, Nullable } from '../../../../../ys/ys';
import { EventNames } from '../../events/event-names';
import { Testopithecus } from '../../events/testopithecus';
import { MessageDTO } from '../../objects/message';
import { TestopithecusEvent } from '../../testopithecus-event';
import { Aggregator } from '../aggregator';

export class MessageListSyncAggregator implements Aggregator {

  private sentMessageIds: Set<Int64> = new Set<Int64>()

  public accept(event: TestopithecusEvent): Nullable<TestopithecusEvent> {
    if (!this.accepts(event)) {
      return null
    }
    const messages = event.getAttributes().get('messages') as Array<Map<string, any>>
    const messagesDTO: MessageDTO[] = []
    for (const message of messages) {
      const messageDTO = MessageDTO.fromMap(message)
      if (!this.sentMessageIds.has(messageDTO.mid)) {
        this.sentMessageIds.add(messageDTO.mid)
        messagesDTO.push(messageDTO)
      }
    }
    return messagesDTO.length === 0 ? null : Testopithecus.modelSyncEvents.updateMessageList(messagesDTO)
  }

  public accepts(event: TestopithecusEvent): boolean {
    return event.name === EventNames.MODEL_SYNC_MESSAGE_LIST;
  }

  public finalize(): Nullable<TestopithecusEvent> {
    return null;
  }

}
