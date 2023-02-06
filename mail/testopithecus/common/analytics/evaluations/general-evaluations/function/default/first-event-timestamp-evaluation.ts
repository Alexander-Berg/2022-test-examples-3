import { TestopithecusEvent } from '../../../../../code/mail/logging/testopithecus-event'
import { Int64, Nullable } from '../../../../../ys/ys'
import { ParsingUtils } from '../../../../processing/parsing-utils'
import { InitiatorEventEvaluation } from '../../one-value/first-step-value/initiator-event-evaluation';
import { FunctionEvaluation } from '../function-evaluation';

export class FirstEventTimestampEvaluation extends FunctionEvaluation<Nullable<Int64>, Nullable<TestopithecusEvent>, null> {

  public constructor() {
    super(new InitiatorEventEvaluation(), 'start_timestamp_ms')
  }

  public apply(value: Nullable<TestopithecusEvent>): Nullable<Int64> {
    return value === null ? null : ParsingUtils.demandTimestamp(value)
  }

}
