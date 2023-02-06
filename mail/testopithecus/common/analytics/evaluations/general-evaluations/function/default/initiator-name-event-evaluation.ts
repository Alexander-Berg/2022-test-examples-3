import { TestopithecusEvent } from '../../../../../code/mail/logging/testopithecus-event';
import { Nullable } from '../../../../../ys/ys';
import { InitiatorEventEvaluation } from '../../one-value/first-step-value/initiator-event-evaluation';
import { FunctionEvaluation } from '../function-evaluation';

export class InitiatorNameEventEvaluation extends FunctionEvaluation<Nullable<string>, Nullable<TestopithecusEvent>, null> {

  public constructor() {
    super(new InitiatorEventEvaluation(), 'initiator_name')
  }

  public apply(value: Nullable<TestopithecusEvent>): Nullable<string> {
    return value === null ? null : value.name;
  }

}
