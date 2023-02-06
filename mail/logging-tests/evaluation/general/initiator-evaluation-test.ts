import { AnalyticsRunner } from '../../../../analytics/analytics-runner';
import { InitiatorNameEventEvaluation } from '../../../../analytics/evaluations/general-evaluations/function/default/initiator-name-event-evaluation';
import { Scenario } from '../../../../analytics/scenario';
import { EventNames } from '../../../../code/mail/logging/events/event-names';
import { Testopithecus } from '../../../../code/mail/logging/events/testopithecus';
import { int64 } from '../../../../ys/ys'
import { checkEvaluationsResults } from '../../utils/utils';

describe('Initiator evaluation timestamp event evaluation', () => {
  it('should be correct for usual scenario', (done) => {
    const session = new Scenario()
      .thenEvent(Testopithecus.startEvents.startWithMessageListShow())
      .thenEvent(Testopithecus.messageListEvents.markMessageAsRead(0, int64(1)))
      .thenEvent(Testopithecus.messageListEvents.deleteMessage(0, int64(2)))

    const evaluations = [new InitiatorNameEventEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [EventNames.START_WITH_MESSAGE_LIST])
    done()
  });
  it('should be correct for empty scenario', (done) => {
    const session = new Scenario()

    const evaluations = [new InitiatorNameEventEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [null])
    done()
  });
});
