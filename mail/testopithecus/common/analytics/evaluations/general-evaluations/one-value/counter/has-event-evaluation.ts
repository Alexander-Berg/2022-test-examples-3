import { Int32, Nullable } from '../../../../../ys/ys';
import { FunctionEvaluation } from '../../function/function-evaluation';
import { EventCounterEvaluation } from './event-counter-evaluation';

export class HasEventEvaluation extends FunctionEvaluation<boolean, Int32, null> {

  public constructor(eventName: string, evaluationName: Nullable<string> = null) {
    super(new EventCounterEvaluation(eventName), evaluationName !== null ? evaluationName : `indicator_${eventName}`);
  }

  public apply(value: Int32): boolean {
    return value !== 0;
  }
}
