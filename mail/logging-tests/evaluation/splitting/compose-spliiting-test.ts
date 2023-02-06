import { AnalyticsRunner } from '../../../../analytics/analytics-runner';
import { FullScenarioEvaluation } from '../../../../analytics/evaluations/general-evaluations/default/full-scenario-evaluation';
import { MailContextApplier } from '../../../../analytics/mail/mail-context-applier';
import { ComposeScenarioSplitter } from '../../../../analytics/mail/scenarios/compose/compose-scenario-splitter';
import { Scenario } from '../../../../analytics/scenario';
import { Testopithecus } from '../../../../code/mail/logging/events/testopithecus';
import { int64 } from '../../../../ys/ys'
import { checkSplitterEvaluationResults } from '../../utils/utils';

describe('Compose splitting should split correctly', () => {
  it('for one full scenario', (done) => {
    const session = new Scenario()
      .thenEvent(Testopithecus.messageListEvents.writeNewMessage())
      .thenEvent(Testopithecus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Testopithecus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Testopithecus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Testopithecus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Testopithecus.composeEvents.pressBack(true))

    const evaluations = [new ComposeScenarioSplitter([
      () => new FullScenarioEvaluation(),
    ])]
    const runner = new AnalyticsRunner();
    const result = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkSplitterEvaluationResults(evaluations[0], result, [[session]])
    done()
  });

  it('for one scenario', (done) => {
    const session = new Scenario()
      .thenEvent(Testopithecus.messageListEvents.deleteMessage(1, int64(2)))
      .thenEvent(Testopithecus.messageListEvents.deleteMessage(1, int64(2)))
      .thenEvent(Testopithecus.messageListEvents.writeNewMessage())
      .thenEvent(Testopithecus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Testopithecus.messageListEvents.openMessage(0, int64(1)))
      .thenEvent(Testopithecus.composeEvents.pressBack(true))
      .thenEvent(Testopithecus.messageViewEvents.backToMailList())
      .thenEvent(Testopithecus.messageViewEvents.backToMailList())

    const evaluations = [new ComposeScenarioSplitter([
      () => new FullScenarioEvaluation(),
    ])]
    const runner = new AnalyticsRunner();
    const result = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkSplitterEvaluationResults(evaluations[0], result, [
      [new Scenario()
        .thenEvent(Testopithecus.messageListEvents.writeNewMessage())
        .thenEvent(Testopithecus.messageListEvents.openMessage(0, int64(1)))
        .thenEvent(Testopithecus.messageListEvents.openMessage(0, int64(1)))
        .thenEvent(Testopithecus.composeEvents.pressBack(true)),
      ],
    ])
    done()
  });

  it('for two scenarios', (done) => {
    const session = new Scenario()
      .thenEvent(Testopithecus.messageListEvents.markMessageAsUnread(1, int64(2)))
      .thenEvent(Testopithecus.messageListEvents.openMessageActions(1, int64(2)))
      .thenEvent(Testopithecus.messageActionsEvents.reply())
      .thenEvent(Testopithecus.messageActionsEvents.cancel())
      .thenEvent(Testopithecus.messageActionsEvents.delete())
      .thenEvent(Testopithecus.composeEvents.pressBack(true))
      .thenEvent(Testopithecus.messageActionsEvents.replyAll())
      .thenEvent(Testopithecus.composeEvents.editBody(14))
      .thenEvent(Testopithecus.composeEvents.editBody(44))
      .thenEvent(Testopithecus.composeEvents.sendMessage())
      .thenEvent(Testopithecus.messageViewEvents.backToMailList())

    const evaluations = [new ComposeScenarioSplitter([
      () => new FullScenarioEvaluation(),
    ])]
    const runner = new AnalyticsRunner();
    const result = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkSplitterEvaluationResults(evaluations[0], result, [
      [new Scenario()
        .thenEvent(Testopithecus.messageActionsEvents.reply())
        .thenEvent(Testopithecus.messageActionsEvents.cancel())
        .thenEvent(Testopithecus.messageActionsEvents.delete())
        .thenEvent(Testopithecus.composeEvents.pressBack(true)),
      ],
      [new Scenario()
        .thenEvent(Testopithecus.messageActionsEvents.replyAll())
        .thenEvent(Testopithecus.composeEvents.editBody(14))
        .thenEvent(Testopithecus.composeEvents.editBody(44))
        .thenEvent(Testopithecus.composeEvents.sendMessage()),
      ],
    ])
    done()
  });

  it('for unfinished scenario', (done) => {
    const session = new Scenario()
      .thenEvent(Testopithecus.messageViewEvents.editDraft(1))
      .thenEvent(Testopithecus.groupActionsEvents.selectMessage(0, int64(1)))
      .thenEvent(Testopithecus.groupActionsEvents.selectMessage(1, int64(1)))
      .thenEvent(Testopithecus.messageListEvents.openMessage(0, int64(1)))

    const evaluations = [new ComposeScenarioSplitter([
      () => new FullScenarioEvaluation(),
    ])]
    const runner = new AnalyticsRunner();
    const result = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkSplitterEvaluationResults(evaluations[0], result, [
      [new Scenario()
        .thenEvent(Testopithecus.messageViewEvents.editDraft(1))
        .thenEvent(Testopithecus.groupActionsEvents.selectMessage(0, int64(1)))
        .thenEvent(Testopithecus.groupActionsEvents.selectMessage(1, int64(1)))
        .thenEvent(Testopithecus.messageListEvents.openMessage(0, int64(1))),
      ],
    ])
    done()
  });

});
