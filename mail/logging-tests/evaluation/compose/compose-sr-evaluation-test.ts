import { AnalyticsRunner } from '../../../../analytics/analytics-runner';
import { Evaluation } from '../../../../analytics/evaluations/evaluation';
import { MailContextApplier } from '../../../../analytics/mail/mail-context-applier';
import { ComposeScenarioSplitter } from '../../../../analytics/mail/scenarios/compose/compose-scenario-splitter';
import { ComposeSrIndexUsedEvaluation } from '../../../../analytics/mail/scenarios/compose/smart_replies/compose-sr-index-used-evaluation';
import { ComposeSrItemsCountEvaluation } from '../../../../analytics/mail/scenarios/compose/smart_replies/compose-sr-items-count-evaluation';
import { ComposeSrUsedEvaluation } from '../../../../analytics/mail/scenarios/compose/smart_replies/compose-sr-used-evaluation';
import { ComposeSrUsedExactEvaluation } from '../../../../analytics/mail/scenarios/compose/smart_replies/compose-sr-used-exact-evaluation';
import { Scenario } from '../../../../analytics/scenario';
import { Testopithecus } from '../../../../code/mail/logging/events/testopithecus';
import { int64 } from '../../../../ys/ys';
import { checkEvaluationsResults, checkSplitterEvaluationResults } from '../../utils/utils';

describe('Compose smart replies evaluations', () => {
  it('should be correct for scenario without smart replies', (done) => {
    const session = new Scenario()
      .thenEvent(Testopithecus.messageListEvents.writeNewMessage())
      .thenEvent(Testopithecus.composeEvents.editBody(10))
      .thenEvent(Testopithecus.composeEvents.editBody(44))
      .thenEvent(Testopithecus.composeEvents.sendMessage())

    const evaluations: Array<Evaluation<any, any>> = [
      new ComposeSrUsedEvaluation(),
      new ComposeSrItemsCountEvaluation(),
      new ComposeSrIndexUsedEvaluation(),
      new ComposeSrUsedExactEvaluation(),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkEvaluationsResults(evaluations, results, [false, null, null, null])
    done()
  });

  it('should be correct for scenario with not used smart replies', (done) => {
    const session = new Scenario()
      .thenEvent(Testopithecus.pushEvents.replyMessagePushClicked(int64(0), int64(0), int64(0), 3))
      .thenEvent(Testopithecus.composeEvents.editBody(10))
      .thenEvent(Testopithecus.composeEvents.sendMessage())

    const evaluations: Array<Evaluation<any, any>> = [
      new ComposeSrUsedEvaluation(),
      new ComposeSrItemsCountEvaluation(),
      new ComposeSrIndexUsedEvaluation(),
      new ComposeSrUsedExactEvaluation(),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkEvaluationsResults(evaluations, results, [false, 3, null, null])
    done()
  });

  it('should be correct for scenario with zero smart replies', (done) => {
    const session = new Scenario()
      .thenEvent(Testopithecus.pushEvents.replyMessagePushClicked(int64(0), int64(0), int64(0), 0))
      .thenEvent(Testopithecus.composeEvents.editBody(10))
      .thenEvent(Testopithecus.composeEvents.sendMessage())

    const evaluations: Array<Evaluation<any, any>> = [
      new ComposeSrUsedEvaluation(),
      new ComposeSrItemsCountEvaluation(),
      new ComposeSrIndexUsedEvaluation(),
      new ComposeSrUsedExactEvaluation(),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkEvaluationsResults(evaluations, results, [false, 0, null, null])
    done()
  });

  it('should be correct for scenario with edited smart reply', (done) => {
    const session = new Scenario()
      .thenEvent(Testopithecus.pushEvents.smartReplyMessagePushClicked(int64(0), int64(0), int64(0), 1, 3))
      .thenEvent(Testopithecus.composeEvents.editBody(10))
      .thenEvent(Testopithecus.composeEvents.sendMessage())

    const evaluations: Array<Evaluation<any, any>> = [
      new ComposeSrUsedEvaluation(),
      new ComposeSrItemsCountEvaluation(),
      new ComposeSrIndexUsedEvaluation(),
      new ComposeSrUsedExactEvaluation(),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkEvaluationsResults(evaluations, results, [true, 3, 1, false])
    done()
  });

  it('should be correct for scenario with smart reply as is', (done) => {
    const session = new Scenario()
      .thenEvent(Testopithecus.pushEvents.smartReplyMessagePushClicked(int64(0), int64(0), int64(0), 1, 3))
      .thenEvent(Testopithecus.composeEvents.addAttachments(1))
      .thenEvent(Testopithecus.composeEvents.sendMessage())

    const evaluations: Array<Evaluation<any, any>> = [
      new ComposeSrUsedEvaluation(),
      new ComposeSrItemsCountEvaluation(),
      new ComposeSrIndexUsedEvaluation(),
      new ComposeSrUsedExactEvaluation(),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkEvaluationsResults(evaluations, results, [true, 3, 1, true])
    done()
  });

  it('should be correct for scenario with pushes', (done) => {
    const session = new Scenario()
      .thenEvent(Testopithecus.pushEvents.messagesReceivedPushShown(int64(0), int64(0), [int64(1), int64(2)], [3, 2]))
      .thenEvent(Testopithecus.pushEvents.singleMessagePushClicked(int64(0), int64(1), int64(0)))
      .thenEvent(Testopithecus.messageViewEvents.reply(0))
      .thenEvent(Testopithecus.composeEvents.sendMessage())

    const evaluations = [new ComposeScenarioSplitter([
      () => new ComposeSrUsedEvaluation(),
      () => new ComposeSrItemsCountEvaluation(),
      () => new ComposeSrIndexUsedEvaluation(),
      () => new ComposeSrUsedExactEvaluation(),
    ])]

    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkSplitterEvaluationResults(evaluations[0], results, [[false, 3, null, null]])
    done()
  });

  it('should be correct for scenario with pushes but no quick replies provided', (done) => {
    const session = new Scenario()
      .thenEvent(Testopithecus.pushEvents.messagesReceivedPushShown(int64(0), int64(0), [int64(1), int64(2)]))
      .thenEvent(Testopithecus.pushEvents.singleMessagePushClicked(int64(0), int64(1), int64(0)))
      .thenEvent(Testopithecus.messageViewEvents.reply(0))
      .thenEvent(Testopithecus.composeEvents.sendMessage())

    const evaluations = [new ComposeScenarioSplitter([
      () => new ComposeSrUsedEvaluation(),
      () => new ComposeSrItemsCountEvaluation(),
      () => new ComposeSrIndexUsedEvaluation(),
      () => new ComposeSrUsedExactEvaluation(),
    ])]

    const runner = new AnalyticsRunner()
    const results = runner.evaluateWithContext(session, evaluations, new MailContextApplier())

    checkSplitterEvaluationResults(evaluations[0], results, [[false, null, null, null]])
    done()
  });
});
