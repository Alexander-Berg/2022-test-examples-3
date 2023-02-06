import { TestopithecusEvent } from '../../../../code/mail/logging/testopithecus-event';
import { Int32, int64, Int64 } from '../../../../ys/ys'
import { ParsingUtils } from '../../../processing/parsing-utils'
import { NamedEvaluation } from '../named-evaluation';

export class FirstEventEvaluation extends NamedEvaluation<Int64, null> {

  private eventNumber: Int32 = 0
  private initTimestamp: Int64 = int64(0)
  private timestampDiff: Int64 = int64(0)

  constructor(evaluationName: string = 'first_event') {
    super(evaluationName);
  }

  public acceptEvent(action: TestopithecusEvent): any {
    const timestamp = ParsingUtils.demandTimestamp(action)
    if (this.eventNumber === 0 && timestamp !== null) {
      this.initTimestamp = timestamp
    } else if (this.eventNumber === 1 && timestamp !== null) {
      this.timestampDiff = timestamp - this.initTimestamp
    }
    this.eventNumber += 1
  }

  public result(): Int64 {
    return this.timestampDiff
  }
}
