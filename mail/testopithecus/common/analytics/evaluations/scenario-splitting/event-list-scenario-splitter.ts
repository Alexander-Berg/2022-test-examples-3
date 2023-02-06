import { TestopithecusEvent } from '../../../code/mail/logging/testopithecus-event';
import { Evaluation } from '../evaluation';
import { BaseScenarioSplitter } from './base-scenario-splitter';

export class EventListScenarioSplitter<C> extends BaseScenarioSplitter<C> {

  constructor(
    evaluationProviders: Array<() => Evaluation<any, C>>,
    private readonly startEventsNames: string[],
    private readonly finishEventsNames: string[],
  ) {
    super(evaluationProviders)
  }

  public name(): string {
    return 'empty_list_splitter';
  }

  public isScenarioStarted(event: TestopithecusEvent): boolean {
    return this.startEventsNames.includes(event.name);
  }

  public isScenarioEnded(event: TestopithecusEvent): boolean {
    return this.finishEventsNames.includes(event.name);
  }

}
