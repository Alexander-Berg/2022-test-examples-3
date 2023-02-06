import { EventNames } from '../../../../code/mail/logging/events/event-names';
import { TestopithecusEvent } from '../../../../code/mail/logging/testopithecus-event';
import { FirstStepValueEvaluation } from '../../../evaluations/general-evaluations/one-value/first-step-value/first-step-value-evaluation';

export class ComposeTypeEvaluation extends FirstStepValueEvaluation<string, null> {

  public constructor(evaluationName: string = 'scenario_type') {
    super(evaluationName)
  }

  public extractValue(event: TestopithecusEvent): any {
    switch (event.name) {
      case EventNames.LIST_MESSAGE_WRITE_NEW_MESSAGE:
        return  'compose'
      case EventNames.MESSAGE_VIEW_REPLY:
        return 'reply'
      case EventNames.MESSAGE_ACTION_REPLY:
        return 'reply'
      case EventNames.PUSH_REPLY_MESSAGE_CLICKED: // actually it is reply all
        return 'reply'
      case EventNames.PUSH_SMART_REPLY_MESSAGE_CLICKED: // actually it is reply all
        return 'reply'
      case EventNames.MESSAGE_VIEW_REPLY_ALL:
        return 'reply_all'
      case EventNames.MESSAGE_ACTION_REPLY_ALL:
        return 'reply_all'
      case EventNames.MESSAGE_ACTION_FORWARD:
        return 'forward'
      case EventNames.MESSAGE_VIEW_EDIT_DRAFT:
        return 'resume'
      default:
        return 'unknown'
    }
  }

  public defaultValue(): string {
    return 'unknown';
  }

}
