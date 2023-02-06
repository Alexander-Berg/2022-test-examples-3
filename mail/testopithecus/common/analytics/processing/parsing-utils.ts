import { TestopithecusEvent } from '../../code/mail/logging/testopithecus-event'
import { requireNonNull } from '../../code/utils/utils'
import { Int32, Int64 } from '../../ys/ys'

export class ParsingUtils {
  public static demandTimestamp(event: TestopithecusEvent): Int64 {
    return requireNonNull(event.getInt64('timestamp'), 'No timestamp in event!')
  }

  public static demandOrder(event: TestopithecusEvent): Int32 {
    return requireNonNull(event.getInt32('order'), 'No order in event!')
  }

  public static demandInt32(event: TestopithecusEvent, name: string): Int32 {
    return requireNonNull(event.getInt32(name), `No ${name} in event!`)
  }

}
