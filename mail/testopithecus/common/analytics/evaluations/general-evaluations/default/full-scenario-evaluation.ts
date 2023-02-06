import { TestopithecusEvent } from '../../../../code/mail/logging/testopithecus-event';
import { Scenario } from '../../../scenario';
import { NamedEvaluation } from '../named-evaluation';

export class FullScenarioEvaluation<C> extends NamedEvaluation<Scenario, null> {

  private scenario: Scenario = new Scenario()

  public constructor(evaluationName: string = 'full_scenario') {
    super(evaluationName)
  }

  public acceptEvent(event: TestopithecusEvent): any {
    this.scenario.thenEvent(event)
  }

  public result(): Scenario {
    return this.scenario
  }
}
