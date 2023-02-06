import { TestopithecusEvent } from '../../../../code/mail/logging/testopithecus-event';
import { Nullable } from '../../../../ys/ys';
import { NamedEvaluation } from '../named-evaluation';

export abstract class OneValueEvaluation<T, C> extends NamedEvaluation<T, C> {

  protected value: T

  protected constructor(initialValue: T, evaluationName: string) {
    super(evaluationName)
    this.value = initialValue
  }

  public acceptEvent(event: TestopithecusEvent, context: C): void {
    this.updateValue(event, context)
  }

  public result(): T {
    return this.value;
  }

  protected abstract updateValue(event: TestopithecusEvent, context: C): void

}

export abstract class MaybeOneValueEvaluation<T, C> extends OneValueEvaluation<Nullable<T>, C> {

  constructor(evaluationName: string) {
    super(null, evaluationName)
  }

  public result(): Nullable<T> {
    return this.value === null ? this.defaultValue() : this.value;
  }

  public defaultValue(): Nullable<T> {
    return null
  }

}
