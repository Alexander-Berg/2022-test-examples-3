import { TestopithecusEvent } from '../../../../../code/mail/logging/testopithecus-event';
import { Int32 } from '../../../../../ys/ys';
import { OneValueEvaluation } from '../one-value-evaluation';

export abstract class BaseCounterEvaluation extends OneValueEvaluation<Int32, null> {

  protected constructor(evaluationName: string) {
    super(0, evaluationName);
  }

  protected updateValue(event: TestopithecusEvent, context: null): void {
    if (this.matches(event)) {
      this.value += 1
    }
  }

  protected abstract matches(event: TestopithecusEvent): boolean

}
