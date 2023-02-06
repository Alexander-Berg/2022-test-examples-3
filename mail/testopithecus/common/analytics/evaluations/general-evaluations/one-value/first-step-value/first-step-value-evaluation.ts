import { TestopithecusEvent } from '../../../../../code/mail/logging/testopithecus-event';
import { Nullable } from '../../../../../ys/ys';
import { MaybeOneValueEvaluation } from '../one-value-evaluation';

export abstract class FirstStepValueEvaluation<T, C> extends MaybeOneValueEvaluation<T, C> {

  private valueSet = false

  protected updateValue(event: TestopithecusEvent, context: C): void {
    if (!this.valueSet) {
      this.value = this.extractValue(event, context)
      this.valueSet = true
    }
  }

  public abstract extractValue(event: TestopithecusEvent, context: C): Nullable<T>

}
