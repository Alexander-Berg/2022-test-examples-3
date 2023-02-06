import { EventNames } from '../../../../code/mail/logging/events/event-names';
import { TestopithecusEvent } from '../../../../code/mail/logging/testopithecus-event';
import { Int64, undefinedToNull } from '../../../../ys/ys';
import { MaybeOneValueEvaluation } from '../../../evaluations/general-evaluations/one-value/one-value-evaluation';

export class ComposeBodyLengthEvaluation extends MaybeOneValueEvaluation<Int64, null> {

  constructor(evaluationName: string = 'body_length') {
    super(evaluationName);
  }

  protected updateValue(event: TestopithecusEvent, context: null): void {
    if (event.name === EventNames.COMPOSE_EDIT_BODY) {
      const length = undefinedToNull(event.getAttributes().get('length'))
      if (length !== null) {
        this.value = length
      }
    }
  }

}
