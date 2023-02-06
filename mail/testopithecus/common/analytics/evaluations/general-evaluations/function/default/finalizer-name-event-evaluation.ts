import { TestopithecusEvent } from '../../../../../code/mail/logging/testopithecus-event';
import { Nullable } from '../../../../../ys/ys';
import { FinalizerEventEvaluation } from '../../one-value/finalizer-event-evaluation';
import { FunctionEvaluation } from '../function-evaluation';

export class FinalizerNameEventEvaluation extends FunctionEvaluation<Nullable<string>, Nullable<TestopithecusEvent>, null> {

  public constructor() {
    super(new FinalizerEventEvaluation(), 'finalizer_name')
  }

  public apply(value: Nullable<TestopithecusEvent>): Nullable<string> {
    return value === null ? null : value.name;
  }

}
