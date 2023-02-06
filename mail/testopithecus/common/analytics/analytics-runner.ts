import { ContextApplier, NullContextApplier } from './context-applier';
import { Evaluation} from './evaluations/evaluation';
import { Scenario} from './scenario';

export class AnalyticsRunner {

  constructor() { }

  public evaluate(session: Scenario, evaluations: Array<Evaluation<any, null>>): Map<string, any> { // returns values for all metrics
    const contextApplier = new NullContextApplier();
    return this.evaluateWithContext(session, evaluations, contextApplier)
  }

  public evaluateWithContext<T, C>(session: Scenario, evaluations: Array<Evaluation<T, C>>, contextApplier: ContextApplier<C>): Map<string, T> {
    const result: Map<string, T> = new Map<string, T>()
    let context: C = contextApplier.init()
    for (const event of session.events) {
      for (const evaluation of evaluations) {
        evaluation.acceptEvent(event, context)
      }
      context = contextApplier.apply(event, context)
    }
    for (const evaluation of evaluations) {
      result.set(evaluation.name(), evaluation.result())
    }
    return result
  }

}
