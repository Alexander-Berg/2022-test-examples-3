import { TestopithecusEvent } from '../../../../../code/mail/logging/testopithecus-event';
import { Nullable } from '../../../../../ys/ys';
import { BaseCounterEvaluation } from './base-counter-evaluation';

export class EventCounterEvaluation extends BaseCounterEvaluation {

  private readonly eventName: string

  constructor(eventName: string, evaluationName: Nullable<string> = null) {
    super(evaluationName !== null ? evaluationName : `$counter_${eventName}`)
    this.eventName = eventName
  }

  protected matches(event: TestopithecusEvent): boolean {
    return event.name === this.eventName;
  }

}
