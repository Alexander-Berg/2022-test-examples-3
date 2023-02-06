import { TestopithecusEvent } from '../../../../code/mail/logging/testopithecus-event';
import { Evaluation } from '../../evaluation';
import { NamedEvaluation } from '../named-evaluation';

export abstract class BinaryFunctionEvaluation<T, U, V, C> extends NamedEvaluation<T, C> {

  protected constructor(
    private firstEvaluation: Evaluation<U, C>,
    private secondEvaluation: Evaluation<V, C>,
    evaluationName: string = 'binary_function',
  ) {
    super(evaluationName)
  }

  public acceptEvent(event: TestopithecusEvent, context: C): any {
    this.firstEvaluation.acceptEvent(event, context)
    this.secondEvaluation.acceptEvent(event, context)
  }

  public result(): T {
    return this.apply(this.firstEvaluation.result(), this.secondEvaluation.result());
  }

  public abstract apply(first: U, second: V): T

}

// export abstract class ContextFreeBinaryWrapEvaluation<T, U, V> extends BinaryFunctionEvaluation<T, U, V, null> implements ContextFreeEvaluation<T> {
//
//   public acceptEvent(event: TestopithecusEvent): void {
//     super.acceptEvent(event, null)
//   }
//
// }
