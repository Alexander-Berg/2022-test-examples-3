import { TestopithecusEvent } from '../../../../code/mail/logging/testopithecus-event';
import { MaybeOneValueEvaluation } from './one-value-evaluation';

export class FinalizerEventEvaluation extends MaybeOneValueEvaluation<TestopithecusEvent, null> {

  constructor(evaluationName: string = 'finalizer_event') {
    super(evaluationName);
  }

  protected updateValue(event: TestopithecusEvent): void {
    this.value = event
  }

}
