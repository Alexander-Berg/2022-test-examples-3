import { TestopithecusEvent } from '../../../code/mail/logging/testopithecus-event';
import { Evaluation } from '../evaluation';

export abstract class NamedEvaluation<T, C> implements Evaluation<T, C> {

  protected constructor(private evaluationName: string) { }

  public name(): string {
    return this.evaluationName;
  }

  public abstract acceptEvent(event: TestopithecusEvent, context: C): void

  public abstract result(): T

}

export abstract class ContextFreeNamedEvaluation<T> extends NamedEvaluation<T, null> implements ContextFreeNamedEvaluation<T> { }
