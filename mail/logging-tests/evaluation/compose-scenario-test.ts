import { AnalyticsRunner } from '../../../analytics/analytics-runner';
import { FirstEventEvaluation } from '../../../analytics/evaluations/general-evaluations/default/first-event-evaluation';
import { SessionLengthEvaluation } from '../../../analytics/evaluations/general-evaluations/default/session-length-evaluation';
import { MailContextApplier } from '../../../analytics/mail/mail-context-applier';
import { ComposeScenarioSplitter } from '../../../analytics/mail/scenarios/compose/compose-scenario-splitter';
import { Scenario } from '../../../analytics/scenario';
import { Testopithecus } from '../../../code/mail/logging/events/testopithecus';
import { int64 } from '../../../ys/ys'
import { checkSplitterEvaluationResults, setTimeline } from '../utils/utils';

describe('Compose scenario length and first event', () => {
  it('should be correct for one scenario session', (done) => {
    const session = new Scenario()
      .thenEvent(Testopithecus.startEvents.startWithMessageListShow())
      .thenEvent(Testopithecus.messageListEvents.writeNewMessage())
      .thenEvent(Testopithecus.composeEvents.pressBack(false))

    setTimeline(session, [10, 100, 1000])

    const scenarioEvaluation = [new ComposeScenarioSplitter([
      () => new SessionLengthEvaluation(),
      () => new FirstEventEvaluation(),
    ])]
    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, scenarioEvaluation, new MailContextApplier())

    checkSplitterEvaluationResults(scenarioEvaluation[0], results, [[int64(900), int64(900)]])
    done()
  });

  it('should be correct for unfinished scenario session', (done) => {
    const session = new Scenario()
      .thenEvent(Testopithecus.startEvents.startWithMessageListShow())
      .thenEvent(Testopithecus.messageListEvents.writeNewMessage())
      .thenEvent(Testopithecus.composeEvents.editBody(100))
      .thenEvent(Testopithecus.composeEvents.editBody(200))
    setTimeline(session, [10, 100, 1000, 10000])

    const scenarioEvaluation = [new ComposeScenarioSplitter([
      () => new SessionLengthEvaluation(),
      () => new FirstEventEvaluation(),
    ])]
    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, scenarioEvaluation, new MailContextApplier())

    checkSplitterEvaluationResults(scenarioEvaluation[0], results, [[int64(9900), int64(900)]])
    done()
  });

  it('should be correct for zero scenario session', (done) => {
    const session = new Scenario()
      .thenEvent(Testopithecus.startEvents.startWithMessageListShow())
      .thenEvent(Testopithecus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Testopithecus.messageViewEvents.backToMailList())
      .thenEvent(Testopithecus.messageListEvents.deleteMessage(0, int64(2)))
    setTimeline(session, [10, 100, 1000, 10000])

    const scenarioEvaluation = [new ComposeScenarioSplitter([
      () => new SessionLengthEvaluation(),
      () => new FirstEventEvaluation(),
    ])]
    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, scenarioEvaluation, new MailContextApplier())

    checkSplitterEvaluationResults(scenarioEvaluation[0], results, [])
    done()
  });

  it('should be correct for two scenario session', (done) => {
    const session = new Scenario()
      .thenEvent(Testopithecus.startEvents.startWithMessageListShow())
      .thenEvent(Testopithecus.messageListEvents.writeNewMessage())
      .thenEvent(Testopithecus.composeEvents.pressBack(false))
      .thenEvent(Testopithecus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Testopithecus.messageViewEvents.reply(0))
      .thenEvent(Testopithecus.composeEvents.editBody(100))
      .thenEvent(Testopithecus.composeEvents.sendMessage())
      .thenEvent(Testopithecus.messageListEvents.deleteMessage(0, int64(2)))
    setTimeline(session, [1, 3, 6, 10, 30, 60, 100, 300])

    const scenarioEvaluation = [new ComposeScenarioSplitter([
      () => new SessionLengthEvaluation(),
      () => new FirstEventEvaluation(),
    ])]
    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, scenarioEvaluation, new MailContextApplier())

    checkSplitterEvaluationResults(scenarioEvaluation[0], results, [[int64(3), int64(3)], [int64(70), int64(30)]])
    done()
  });
});
