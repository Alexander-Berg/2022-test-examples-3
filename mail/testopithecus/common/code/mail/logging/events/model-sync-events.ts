import { MessageDTO } from '../objects/message';
import { TestopithecusEvent } from '../testopithecus-event';
import { ValueMapBuilder } from '../value-map-builder';
import { EventNames } from './event-names';

export class ModelSyncEvents {

  public updateMessageList(messages: MessageDTO[]): TestopithecusEvent {
    return new TestopithecusEvent(
      EventNames.MODEL_SYNC_MESSAGE_LIST,
      ValueMapBuilder.modelSyncEvent().addMessages(messages),
    );
  }

}
