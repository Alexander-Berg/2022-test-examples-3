import { Int32} from '../../../../ys/ys';
import { FunctionEvaluation } from '../../../evaluations/general-evaluations/function/function-evaluation';
import { ComposeAttachmentsCountEvaluation } from './compose-attachments-count-evaluation';

export class ComposeHasAttachmentsEvaluation extends FunctionEvaluation<boolean, Int32, null> {

  constructor(evaluationName: string = '_has_attachments') {
    super(new ComposeAttachmentsCountEvaluation(), evaluationName);
  }

  public apply(value: Int32): boolean {
    return value > 0;
  }

}
