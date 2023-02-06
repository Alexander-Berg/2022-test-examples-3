import { EventNames } from '../../../../code/mail/logging/events/event-names';
import { TestopithecusEvent } from '../../../../code/mail/logging/testopithecus-event';
import { Int32} from '../../../../ys/ys';
import { OneValueEvaluation } from '../../../evaluations/general-evaluations/one-value/one-value-evaluation';
import { ParsingUtils } from '../../../processing/parsing-utils';

export class ComposeAttachmentsCountEvaluation extends OneValueEvaluation<Int32, null> {

  constructor(evaluationName: string = '_attachments_count') {
    super(0, evaluationName);
  }

  protected updateValue(event: TestopithecusEvent, context: null): void {
    if (event.name === EventNames.COMPOSE_ADD_ATTACHMENTS) {
      this.value += ParsingUtils.demandInt32(event, 'count')
    }
    if (event.name === EventNames.COMPOSE_REMOVE_ATTACHMENT) {
      this.value -= 1
    }
  }

}
