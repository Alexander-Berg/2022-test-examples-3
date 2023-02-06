import { AnalyticsRunner } from '../../../../analytics/analytics-runner';
import { ComposeTypeEvaluation } from '../../../../analytics/mail/scenarios/compose/compose-type-evaluation';
import { Scenario } from '../../../../analytics/scenario';
import { Testopithecus } from '../../../../code/mail/logging/events/testopithecus';
import { int64 } from '../../../../ys/ys'
import { checkEvaluationsResults} from '../../utils/utils';

describe('Compose type event evaluation', () => {
  it('should be correct for empty scenario', (done) => {
    const session = new Scenario()

    const evaluations = [new ComposeTypeEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, ['unknown'])
    done()
  });

  it('should not be overwritten', (done) => {
    const session = new Scenario()
        .thenEvent(Testopithecus.messageListEvents.writeNewMessage())
        .thenEvent(Testopithecus.messageActionsEvents.reply())

    const evaluations = [new ComposeTypeEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, ['compose'])
    done()
  });

  it('should be unknown for incorrect scenario', (done) => {
    const sessions = [
      new Scenario()
        .thenEvent(Testopithecus.startEvents.startFromMessageNotification())
        .thenEvent(Testopithecus.messageListEvents.writeNewMessage()),
      new Scenario()
        .thenEvent(Testopithecus.messageListEvents.openMessage(1, int64(0))),
    ]

    const evaluations = [new ComposeTypeEvaluation()]
    const runner = new AnalyticsRunner()
    for (const session of sessions) {
      const results = runner.evaluate(session, evaluations)
      checkEvaluationsResults(evaluations, results, ['unknown'])
    }

    done()
  });

  it('should be compose for compose scenario', (done) => {
    const sessions = [
      new Scenario()
        .thenEvent(Testopithecus.messageListEvents.writeNewMessage())
        .thenEvent(Testopithecus.messageListEvents.writeNewMessage()),
    ]

    const evaluations = [new ComposeTypeEvaluation()]
    const runner = new AnalyticsRunner()
    for (const session of sessions) {
      const results = runner.evaluate(session, evaluations)
      checkEvaluationsResults(evaluations, results, ['compose'])
    }

    done()
  });

  it('should be reply for reply scenario', (done) => {
    const sessions = [
      new Scenario()
        .thenEvent(Testopithecus.messageActionsEvents.reply()),
      new Scenario()
        .thenEvent(Testopithecus.messageViewEvents.reply(0)),
      new Scenario()
        .thenEvent(Testopithecus.pushEvents.replyMessagePushClicked(int64(0), int64(0), int64(0))),
      new Scenario()
        .thenEvent(Testopithecus.pushEvents.smartReplyMessagePushClicked(int64(0), int64(0), int64(0), 0)),
    ]

    const evaluations = [new ComposeTypeEvaluation()]
    const runner = new AnalyticsRunner()
    for (const session of sessions) {
      const results = runner.evaluate(session, evaluations)
      checkEvaluationsResults(evaluations, results, ['reply'])
    }

    done()
  });

  it('should be reply_all for reply_all scenario', (done) => {
    const sessions = [
      new Scenario()
        .thenEvent(Testopithecus.messageActionsEvents.replyAll()),
      new Scenario()
        .thenEvent(Testopithecus.messageViewEvents.replyAll(0)),
    ]

    const evaluations = [new ComposeTypeEvaluation()]
    const runner = new AnalyticsRunner()
    for (const session of sessions) {
      const results = runner.evaluate(session, evaluations)
      checkEvaluationsResults(evaluations, results, ['reply_all'])
    }

    done()
  });

  it('should be resume for resume scenario', (done) => {
    const sessions = [
      new Scenario()
        .thenEvent(Testopithecus.messageViewEvents.editDraft(0)),
    ]

    const evaluations = [new ComposeTypeEvaluation()]
    const runner = new AnalyticsRunner()
    for (const session of sessions) {
      const results = runner.evaluate(session, evaluations)
      checkEvaluationsResults(evaluations, results, ['resume'])
    }

    done()
  });

  it('should be forward for forward scenario', (done) => {
    const sessions = [
      new Scenario()
        .thenEvent(Testopithecus.messageActionsEvents.forward()),
    ]

    const evaluations = [new ComposeTypeEvaluation()]
    const runner = new AnalyticsRunner()
    for (const session of sessions) {
      const results = runner.evaluate(session, evaluations)
      checkEvaluationsResults(evaluations, results, ['forward'])
    }

    done()
  });
});
