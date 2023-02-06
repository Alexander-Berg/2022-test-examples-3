import { JSONItem } from '../json-types'
import { TestopithecusEvent } from '../testopithecus-event'
import { ValueMapBuilder } from '../value-map-builder'
import { ComposeEvents } from './compose-events'
import { EventNames } from './event-names'
import { GroupActionsEvents } from './group-actions-events'
import { MessageActionsEvents } from './message-actions-events'
import { MessageEvents } from './message-events'
import { MessageListEvents } from './message-list-events'
import { ModelSyncEvents } from './model-sync-events'
import { PushEvents } from './push-events'
import { StartEvents } from './start-events'

export class Testopithecus {
  public static startEvents: StartEvents = new StartEvents()
  public static messageViewEvents: MessageEvents = new MessageEvents()
  public static groupActionsEvents: GroupActionsEvents = new GroupActionsEvents()
  public static messageActionsEvents: MessageActionsEvents = new MessageActionsEvents()
  public static messageListEvents: MessageListEvents = new MessageListEvents()
  public static composeEvents: ComposeEvents = new ComposeEvents()
  public static pushEvents: PushEvents = new PushEvents()
  public static modelSyncEvents: ModelSyncEvents = new ModelSyncEvents()

  public static errorEvent(reason: string): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.ERROR, ValueMapBuilder.customEvent('error').addError().addReason(reason))
  }

  public static eventCreationErrorEvent(event: string, reason: string): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.ERROR, ValueMapBuilder.customEvent('error').addError().addReason(reason).addEvent(event))
  }

  public static stubEvent(): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.STUB, ValueMapBuilder.customEvent('stub'))
  }

  public static debugEvent(value: Map<string, JSONItem> = new Map()): TestopithecusEvent {
    return new TestopithecusEvent(EventNames.DEBUG, ValueMapBuilder.customEvent('debug', value).addDebug())
  }

}
