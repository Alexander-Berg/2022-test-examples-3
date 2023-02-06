import { EventNames } from '../../../../code/mail/logging/events/event-names';
import { HasEventEvaluation } from '../../../evaluations/general-evaluations/one-value/counter/has-event-evaluation';

export class ComposeSimpleEvaluations {

  public static sendingEvaluation(): HasEventEvaluation {
    return new HasEventEvaluation(EventNames.COMPOSE_SEND_MESSAGE, 'sending')
  }

  public static bodyEditedEvaluation(): HasEventEvaluation {
    return new HasEventEvaluation(EventNames.COMPOSE_EDIT_BODY, 'edit_body')
  }

}
