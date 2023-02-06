import { TestopithecusEvent } from '../../../../../code/mail/logging/testopithecus-event';
import { Int32, Nullable, undefinedToNull } from '../../../../../ys/ys';
import { FirstStepValueEvaluation } from '../../../../evaluations/general-evaluations/one-value/first-step-value/first-step-value-evaluation';
import { ParsingUtils } from '../../../../processing/parsing-utils';
import { MailContext } from '../../../mail-context';
import { ComposeScenarioSplitter } from '../compose-scenario-splitter';

export class ComposeSrItemsCountEvaluation extends FirstStepValueEvaluation<Int32, MailContext> {

  constructor(evaluationName: string = 'sr_items_count') {
    super(evaluationName)
  }

  public extractValue(event: TestopithecusEvent, context: MailContext): Nullable<Int32> {
    if (ComposeScenarioSplitter.startFromMessageViewEvents.includes(event.name)) {
      const order = ParsingUtils.demandOrder(event)
      const mid = context.currentMessageId
      if (order === 0 && mid !== null) {
        if (context.pushes.has(mid)) {
          return undefinedToNull(context.pushes.get(mid))
        }
      } else {
        return null
      }
    } else if (ComposeScenarioSplitter.startFromPushEvents.includes(event.name)) {
      return event.getInt32('repliesNumber')
    }
    return null
  }

}
