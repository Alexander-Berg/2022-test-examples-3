import { TestopithecusEvent } from '../../../../../code/mail/logging/testopithecus-event';
import { FirstStepValueEvaluation } from './first-step-value-evaluation';

export class InitiatorEventEvaluation extends FirstStepValueEvaluation<TestopithecusEvent, null> {

  public constructor(evaluationName: string = 'initiator') {
    super(evaluationName)
  }

  public extractValue(event: TestopithecusEvent): TestopithecusEvent {
    return event;
  }

}
