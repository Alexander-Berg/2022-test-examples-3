import { Nullable } from '../../../../../ys/ys';
import { BinaryFunctionEvaluation } from '../../../../evaluations/general-evaluations/function/binary-function-evaluation';
import { ComposeSimpleEvaluations } from '../compose-simple-evaluations';
import { ComposeSrUsedEvaluation } from './compose-sr-used-evaluation';

export class ComposeSrUsedExactEvaluation extends BinaryFunctionEvaluation<Nullable<boolean>, boolean, boolean, null> {

  public constructor(evaluationName: string = 'sr_used_exact') {
    super(new ComposeSrUsedEvaluation(), ComposeSimpleEvaluations.bodyEditedEvaluation(), evaluationName)
  }

  public apply(first: boolean, second: boolean): Nullable<boolean> {
    if (!first) {
      return null
    }
    return !second;
  }

}
