import { TestopithecusEvent } from '../../../../../code/mail/logging/testopithecus-event'
import { Int64, Nullable } from '../../../../../ys/ys'
import { ParsingUtils } from '../../../../processing/parsing-utils'
import { FinalizerEventEvaluation } from '../../one-value/finalizer-event-evaluation';
import { FunctionEvaluation } from '../function-evaluation';

export class LastEventTimestampEvaluation extends FunctionEvaluation<Nullable<Int64>, Nullable<TestopithecusEvent>, null> {

  public constructor() {
    super(new FinalizerEventEvaluation(), 'last_timestamp_ms')
  }

  public apply(value: Nullable<TestopithecusEvent>): Nullable<Int64> {
    return value === null ? null : ParsingUtils.demandTimestamp(value)
  }

}
