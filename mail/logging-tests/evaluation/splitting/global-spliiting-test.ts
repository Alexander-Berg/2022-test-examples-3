import { AnalyticsRunner } from '../../../../analytics/analytics-runner';
import { FullScenarioEvaluation } from '../../../../analytics/evaluations/general-evaluations/default/full-scenario-evaluation';
import { StartScenarioSplitter } from '../../../../analytics/evaluations/scenario-splitting/start-scenario-splitter';
import { MailContextApplier } from '../../../../analytics/mail/mail-context-applier';
import { Scenario } from '../../../../analytics/scenario';
import { Testopithecus } from '../../../../code/mail/logging/events/testopithecus';
import { int64 } from '../../../../ys/ys'
import { checkSplitterEvaluationResults } from '../../utils/utils';

describe('Global splitting should split correctly', () => {
  it('for one start event in scenario', (done) => {
    const session = new Scenario()
      .thenEvent(Testopithecus.startEvents.startWithMessageListShow())
      .thenEvent(Testopithecus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Testopithecus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Testopithecus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Testopithecus.messageListEvents.openMessage(0, int64(1)))

    const evaluations = [new StartScenarioSplitter([
      () => new FullScenarioEvaluation(),
    ])]
    const runner = new AnalyticsRunner();
    const result = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkSplitterEvaluationResults(evaluations[0], result, [[session]])
    done()
  });

  it('for one event', (done) => {
    const session = new Scenario()
      .thenEvent(Testopithecus.startEvents.startFromMessageNotification())

    const evaluations = [new StartScenarioSplitter([
      () => new FullScenarioEvaluation(),
    ])]
    const runner = new AnalyticsRunner();
    const result = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkSplitterEvaluationResults(evaluations[0], result, [[session]])
    done()
  });

  it('for session without start event', (done) => {
    const session = new Scenario()
      .thenEvent(Testopithecus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Testopithecus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Testopithecus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Testopithecus.messageListEvents.openMessage(0, int64(1)))

    const evaluations = [new StartScenarioSplitter([
      () => new FullScenarioEvaluation(),
    ])]
    const runner = new AnalyticsRunner();
    const result = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkSplitterEvaluationResults(evaluations[0], result, [])
    done()
  });

  it('for session with two events', (done) => {
    const session = new Scenario()
      .thenEvent(Testopithecus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Testopithecus.startEvents.startFromMessageNotification())
      .thenEvent(Testopithecus.messageListEvents.refreshMessageList())
      .thenEvent(Testopithecus.startEvents.startFromMessageNotification())
      .thenEvent(Testopithecus.messageListEvents.deleteMessage(0, int64(1)))
      .thenEvent(Testopithecus.messageListEvents.openMessage(0, int64(1)))

    const evaluations = [new StartScenarioSplitter([
      () => new FullScenarioEvaluation(),
    ])]
    const runner = new AnalyticsRunner();
    const result = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkSplitterEvaluationResults(evaluations[0], result, [
      [new Scenario()
        .thenEvent(Testopithecus.startEvents.startFromMessageNotification())
        .thenEvent(Testopithecus.messageListEvents.refreshMessageList()),
      ],
      [new Scenario()
        .thenEvent(Testopithecus.startEvents.startFromMessageNotification())
        .thenEvent(Testopithecus.messageListEvents.deleteMessage(0, int64(1)))
        .thenEvent(Testopithecus.messageListEvents.openMessage(0, int64(1))),
      ],
    ])
    done()
  });

});
