import { EventNames } from '../../../../../code/mail/logging/events/event-names';
import { TestopithecusEvent } from '../../../../../code/mail/logging/testopithecus-event';
import { Int32, Nullable } from '../../../../../ys/ys';
import { FirstStepValueEvaluation } from '../../../../evaluations/general-evaluations/one-value/first-step-value/first-step-value-evaluation';
import { ParsingUtils } from '../../../../processing/parsing-utils';
import { MailContext } from '../../../mail-context';

export class ComposeSrIndexUsedEvaluation extends FirstStepValueEvaluation<Int32, MailContext> {

  constructor(evaluationName: string = 'sr_index_used') {
    super(evaluationName)
  }

  public extractValue(event: TestopithecusEvent, context: MailContext): Nullable<Int32> {
    if (event.name === EventNames.PUSH_SMART_REPLY_MESSAGE_CLICKED) {
      return ParsingUtils.demandOrder(event)
    }
    return null;
  }

}
