import { TestopithecusEvent } from '../../../../code/mail/logging/testopithecus-event';
import { Nullable } from '../../../../ys/ys';
// import { MailSingleValueEvaluation } from '../../evaluations/mail-evaluations';
import { MailContext } from '../../mail-context';

// export abstract class ComposeMessageDtoValueExtractor<T> extends MailSingleValueEvaluation<Nullable<T>> {
//
//   protected constructor(evaluationName: string, private fieldName: string) {
//     super(evaluationName)
//   }
//
//   public extractValue(event: TestopithecusEvent, context: MailContext): Nullable<T> {
//     const messageId = context.currentMessageId
//     if (messageId != null) {
//       const messageDTO = context.messages.get(messageId)
//       if (messageDTO) {
//         return messageDTO.toMap().get(this.fieldName)
//       }
//     }
//     return null
//   }
//
// }
