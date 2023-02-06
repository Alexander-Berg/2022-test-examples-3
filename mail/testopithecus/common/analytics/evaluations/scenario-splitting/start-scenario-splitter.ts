import { TestopithecusEvent } from '../../../code/mail/logging/testopithecus-event';
import { Evaluation } from '../evaluation';
import { BaseScenarioSplitter } from './base-scenario-splitter';

export class StartScenarioSplitter<C> extends BaseScenarioSplitter<C> {

  constructor(evaluationProviders: Array<() => Evaluation<any, C>>) {
    super(evaluationProviders)
  }

  public name(): string {
    return 'global';
  }

  public isScenarioStarted(event: TestopithecusEvent): boolean {
    return event.getAttributes().has('start_event');
  }

  public isScenarioEnded(event: TestopithecusEvent): boolean {
    return false;
  }

}
