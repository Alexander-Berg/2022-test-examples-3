import { TestopithecusEvent } from '../../../../code/mail/logging/testopithecus-event';
import { int64, Int64 } from '../../../../ys/ys'
import { ParsingUtils } from '../../../processing/parsing-utils'
// import { ContextFreeEvaluation } from './context-free-evaluation';

// export class LastEventTimestampEvaluation implements ContextFreeEvaluation<Int64> {
//
//   private timestamp: Int64 = int64(-1)
//
//   public name(): string {
//     return 'finish_timestamp_ms';
//   }
//
//   public acceptEvent(event: TestopithecusEvent): any {
//     this.timestamp = ParsingUtils.demandTimestamp(event)
//   }
//
//   public result(): Int64 {
//     return this.timestamp
//   }
// }
