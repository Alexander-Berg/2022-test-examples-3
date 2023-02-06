import { TestopithecusEvent } from '../../../code/mail/logging/testopithecus-event';
import { Nullable } from '../../../ys/ys';
import { ScenarioAttributes } from '../../scenario';
import { Evaluation } from '../evaluation';
import { NamedEvaluation } from '../general-evaluations/named-evaluation';
import { ScenarioSplitter } from './scenario-splitter';

export abstract class BaseScenarioSplitter<C> extends NamedEvaluation<ScenarioAttributes[], C> implements ScenarioSplitter<C> {

  private scenarioAttributes: ScenarioAttributes[] = []
  private currentScenarioAttributes: Nullable<ScenarioAttributes> = null
  private currentEvaluations: Array<Evaluation<ScenarioAttributes[], C>> = []

  protected constructor(
    private evaluationProviders: Array<() => Evaluation<any, C>>,
    evaluationName: string = 'scenario_splitter',
  ) {
    super(evaluationName)
  }

  public abstract isScenarioStarted(event: TestopithecusEvent): boolean

  public abstract isScenarioEnded(event: TestopithecusEvent): boolean

  public getEvaluations(): Array<Evaluation<any, C>> {
    const evaluations = []
    for (const provider of this.evaluationProviders) {
      evaluations.push(provider())
    }
    return evaluations;
  }

  public acceptEvent(event: TestopithecusEvent, context: C): any {
    // finish current scenario
    if (this.shouldCatScenario(event)) {
      if (this.currentScenarioAttributes == null) {
        throw new Error('Current scenario is not available (WTF!)');
      }
      if (this.isScenarioEnded(event)) {
        this.applyEventToEvaluations(event, context)
      }
      for (const evaluation of this.currentEvaluations) {
        this.currentScenarioAttributes.setAttribute(evaluation.name(), evaluation.result())
      }
      this.scenarioAttributes.push(this.currentScenarioAttributes)
      this.currentScenarioAttributes = null
      this.currentEvaluations = []
    }
    // start new scenario
    if (this.isScenarioStarted(event)) {
      this.currentScenarioAttributes = new ScenarioAttributes()
      this.currentEvaluations = this.getEvaluations()
    }
    // apply evaluations if scenario is active
    if (this.currentScenarioAttributes !== null) {
      this.applyEventToEvaluations(event, context)
    }
  }

  public result(): ScenarioAttributes[] {
    if (this.currentScenarioAttributes !== null) {
      // this.currentScenarioAttributes.addAttributes(this.getCutScenarioAttributes())
      for (const evaluation of this.currentEvaluations) {
        this.currentScenarioAttributes.setAttribute(evaluation.name(), evaluation.result())
      }
      this.scenarioAttributes.push(this.currentScenarioAttributes)
    }
    return this.scenarioAttributes;
  }

  private shouldCatScenario(event: TestopithecusEvent): boolean {
    return (this.isScenarioEnded(event) || this.isScenarioStarted(event)) && this.currentScenarioAttributes !== null
  }

  private applyEventToEvaluations(event: TestopithecusEvent, context: C) {
    for (const evaluation of this.currentEvaluations) {
      evaluation.acceptEvent(event, context)
    }
  }
}
