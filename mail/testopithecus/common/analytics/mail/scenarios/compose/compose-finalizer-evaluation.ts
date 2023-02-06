import { Nullable } from '../../../../ys/ys';
import { BinaryFunctionEvaluation } from '../../../evaluations/general-evaluations/function/binary-function-evaluation';
import { FinalizerNameEventEvaluation } from '../../../evaluations/general-evaluations/function/default/finalizer-name-event-evaluation';
import { ComposeCompletedEvaluation } from './compose-completed-evaluation';

export class ComposeFinalizerEvaluation extends BinaryFunctionEvaluation<Nullable<string>, boolean, Nullable<string>, null> {

  constructor() {
    super(new ComposeCompletedEvaluation(), new FinalizerNameEventEvaluation(), 'finalizer')
  }

  public apply(first: boolean, second: Nullable<string>): Nullable<string> {
    return second !== null && first ? second : null;
  }

}
