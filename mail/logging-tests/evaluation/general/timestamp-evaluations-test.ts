import { AnalyticsRunner } from '../../../../analytics/analytics-runner';
import { FirstEventTimestampEvaluation } from '../../../../analytics/evaluations/general-evaluations/function/default/first-event-timestamp-evaluation';
import { LastEventTimestampEvaluation } from '../../../../analytics/evaluations/general-evaluations/function/default/last-event-timestamp-evaluation';
import { FirstEventEvaluation } from '../../../../analytics/evaluations/general-evaluations/default/first-event-evaluation';
import { Scenario } from '../../../../analytics/scenario';
import { Testopithecus } from '../../../../code/mail/logging/events/testopithecus';
import { int64 } from '../../../../ys/ys'
import { checkEvaluationsResults, setTimeline } from '../../utils/utils';

describe('First and last evaluation timestamp event evaluation', () => {
  it('should be correct for usual scenario', (done) => {
    const session = new Scenario()
      .thenEvent(Testopithecus.startEvents.startWithMessageListShow())
      .thenEvent(Testopithecus.messageListEvents.markMessageAsRead(0, int64(1)))
      .thenEvent(Testopithecus.messageListEvents.deleteMessage(0, int64(2)))
    setTimeline(session, [10, 100, 1000])

    const evaluations = [new FirstEventEvaluation(), new FirstEventTimestampEvaluation(), new LastEventTimestampEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [int64(90), int64(10), int64(1000)])
    done()
  });
  it('should be correct for empty scenario', (done) => {
    const session = new Scenario()

    const evaluations = [new FirstEventEvaluation(), new FirstEventTimestampEvaluation(), new LastEventTimestampEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [int64(0), null, null])
    done()
  });
  it('should be correct for one event scenario', (done) => {
    const session = new Scenario()
      .thenEvent(Testopithecus.startEvents.startWithMessageListShow())
    setTimeline(session, [10])

    const evaluations = [new FirstEventEvaluation(), new FirstEventTimestampEvaluation(), new LastEventTimestampEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [int64(0), int64(10), int64(10)])
    done()
  });
});
