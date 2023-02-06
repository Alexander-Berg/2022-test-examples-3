import { EventNames } from '../../../../../code/mail/logging/events/event-names';
import { TestopithecusEvent } from '../../../../../code/mail/logging/testopithecus-event';
import { Nullable } from '../../../../../ys/ys';
import { FunctionEvaluation } from '../../../../evaluations/general-evaluations/function/function-evaluation';
import { InitiatorEventEvaluation } from '../../../../evaluations/general-evaluations/one-value/first-step-value/initiator-event-evaluation';

export class ComposeSrUsedEvaluation extends FunctionEvaluation<boolean, Nullable<TestopithecusEvent>, null> {

  public constructor(evaluationName: string = 'sr_used') {
    super(new InitiatorEventEvaluation(), evaluationName)
  }

  public apply(value: Nullable<TestopithecusEvent>): boolean {
    return value === null ? false : value.name === EventNames.PUSH_SMART_REPLY_MESSAGE_CLICKED;
  }

}
