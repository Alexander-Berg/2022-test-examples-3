import { TestopithecusEvent } from '../../../../code/mail/logging/testopithecus-event';
import { Int64, Nullable } from '../../../../ys/ys';
import { FirstStepValueEvaluation } from '../../../evaluations/general-evaluations/one-value/first-step-value/first-step-value-evaluation';
import { MailContext } from '../../mail-context';

export class ComposeReceiveTimestampValueExtractor<T> extends FirstStepValueEvaluation<Int64, MailContext> {

  public constructor(evaluationName: string = 'receive_timestamp') {
    super(evaluationName)
  }

  public extractValue(event: TestopithecusEvent, context: MailContext): Nullable<Int64> {
    const messageId = context.currentMessageId
    if (messageId != null) {
      const messageDTO = context.messages.get(messageId)
      if (messageDTO) {
        return messageDTO.timestamp
      }
    }
    return null
  }
}
