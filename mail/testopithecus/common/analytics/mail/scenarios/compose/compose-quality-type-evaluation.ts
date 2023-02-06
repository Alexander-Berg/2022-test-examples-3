import { FunctionEvaluation } from '../../../evaluations/general-evaluations/function/function-evaluation';
import { ComposeCompletedEvaluation } from './compose-completed-evaluation';

export class ComposeQualityTypeEvaluation extends FunctionEvaluation<string, boolean, null> {

  constructor(evaluationName: string = 'qa_type') {
    super(new ComposeCompletedEvaluation(), evaluationName)
  }

  public apply(value: boolean): string {
    return value ? 'success' : 'unfinished';
  }

}
