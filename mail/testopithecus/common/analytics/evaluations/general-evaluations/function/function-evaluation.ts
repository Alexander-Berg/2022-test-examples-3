import { TestopithecusEvent } from '../../../../code/mail/logging/testopithecus-event';
import { Evaluation } from '../../evaluation';
import { NamedEvaluation } from '../named-evaluation';

export abstract class FunctionEvaluation<T, U, C> extends NamedEvaluation<T, C> {

  protected constructor(private parentEvaluation: Evaluation<U, C>, evaluationName: string = 'function') {
    super(evaluationName)
  }

  public acceptEvent(event: TestopithecusEvent, context: C): void {
    this.parentEvaluation.acceptEvent(event, context)
  }

  public result(): T {
    return this.apply(this.parentEvaluation.result());
  }

  public abstract apply(value: U): T

}

// export abstract class ContextFreeWrapEvaluation<T, U> extends FunctionEvaluation<T, U, null> implements ContextFreeEvaluation<T> {
//
//   public acceptEvent(event: TestopithecusEvent): void {
//     super.acceptEvent(event, null)
//   }
//
// }
