import { EventNames } from '../../../../code/mail/logging/events/event-names';
import { Nullable } from '../../../../ys/ys';
import { Evaluation } from '../../../evaluations/evaluation';
import { EventListScenarioSplitter } from '../../../evaluations/scenario-splitting/event-list-scenario-splitter';
import { MailContext } from '../../mail-context';

/**
 * Дополнительные поля о сценарии:
 *
 * <ul>
 *   <li>mid идентификатор исходного письма</li>
 *   <li>receive_datetime время получения исходного письма</li>
 *   <li>start_timestamp_ms время начала сценария</li>
 *   <li>finish_timestamp_ms время завершения сценария</li>
 *   <li>sending была попытка отправки</li>
 * </ul>
 */
export class ComposeScenarioSplitter extends EventListScenarioSplitter<MailContext> {

  public static startEvents = [
    EventNames.LIST_MESSAGE_WRITE_NEW_MESSAGE,
    EventNames.MESSAGE_ACTION_REPLY,
    EventNames.MESSAGE_ACTION_REPLY_ALL,
    EventNames.MESSAGE_VIEW_REPLY,
    EventNames.MESSAGE_VIEW_REPLY_ALL,
    EventNames.MESSAGE_ACTION_FORWARD,
    EventNames.MESSAGE_VIEW_EDIT_DRAFT,
    EventNames.PUSH_REPLY_MESSAGE_CLICKED,
    EventNames.PUSH_SMART_REPLY_MESSAGE_CLICKED,
  ]
  public static finishEvents = [
    EventNames.COMPOSE_BACK,
    EventNames.COMPOSE_SEND_MESSAGE,
  ]

  public static startFromMessageViewEvents = [
    EventNames.MESSAGE_VIEW_REPLY,
    EventNames.MESSAGE_VIEW_REPLY_ALL,
//    EventNames.MESSAGE_VIEW_EDIT_DRAFT, TODO
  ]
  public static startFromPushEvents = [
    EventNames.PUSH_REPLY_MESSAGE_CLICKED,
    EventNames.PUSH_SMART_REPLY_MESSAGE_CLICKED,
  ]

  constructor(evaluationProviders: Array<() => Evaluation<any, Nullable<MailContext>>>) {
    super(evaluationProviders, ComposeScenarioSplitter.startEvents, ComposeScenarioSplitter.finishEvents)
  }

  public name(): string {
    return 'compose';
  }

}
