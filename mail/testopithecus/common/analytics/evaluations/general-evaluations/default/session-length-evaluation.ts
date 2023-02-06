import { TestopithecusEvent } from '../../../../code/mail/logging/testopithecus-event';
import { Int32, int64, Int64 } from '../../../../ys/ys'
import { ParsingUtils } from '../../../processing/parsing-utils'
import { NamedEvaluation } from '../named-evaluation';

export class SessionLengthEvaluation extends NamedEvaluation<Int64, null> {

  private eventNumber: Int32 = 0
  private initTimestamp: Int64 = int64(0)
  private lastTimestamp: Int64 = int64(0)

  public constructor(evaluationName: string = 'session_length') {
    super(evaluationName);
  }

  public acceptEvent(action: TestopithecusEvent): any {
    if (this.eventNumber === 0) {
      this.initTimestamp = ParsingUtils.demandTimestamp(action)
    }
    this.lastTimestamp = ParsingUtils.demandTimestamp(action)
    this.eventNumber += 1
  }

  public result(): Int64 {
    return this.lastTimestamp - this.initTimestamp
  }
}
