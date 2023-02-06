import { TestopithecusEvent } from '../../../../code/mail/logging/testopithecus-event';
import { NamedEvaluation } from '../../../evaluations/general-evaluations/named-evaluation';
import { ComposeScenarioSplitter } from './compose-scenario-splitter';

export class ComposeCompletedEvaluation extends NamedEvaluation<boolean, null> {

  private hasStart = false
  private hasFinish = false

  constructor(evaluationName: string = 'is_full') {
    super(evaluationName);
  }

  public acceptEvent(event: TestopithecusEvent): any {
    if (!this.hasFinish) {
      if (ComposeScenarioSplitter.finishEvents.includes(event.name)) {
        this.hasFinish = true
      }
    }
    if (!this.hasStart) {
      if (ComposeScenarioSplitter.startEvents.includes(event.name)) {
        this.hasStart = true
      }
    }
  }

  public result(): boolean {
    return this.hasStart && this.hasFinish;
  }

}
