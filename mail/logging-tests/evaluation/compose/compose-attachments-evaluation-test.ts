import { AnalyticsRunner } from '../../../../analytics/analytics-runner';
import { Evaluation } from '../../../../analytics/evaluations/evaluation';
import { ComposeAttachmentsCountEvaluation } from '../../../../analytics/mail/scenarios/compose/compose-attachments-count-evaluation';
import { ComposeHasAttachmentsEvaluation } from '../../../../analytics/mail/scenarios/compose/compose-has-attachments-evaluation';
import { Scenario } from '../../../../analytics/scenario';
import { Testopithecus } from '../../../../code/mail/logging/events/testopithecus';
import { checkEvaluationsResults} from '../../utils/utils';

describe('Compose attachment evaluations', () => {
  it('should be correct for scenario without attachments', (done) => {
    const session = new Scenario()
        .thenEvent(Testopithecus.messageListEvents.writeNewMessage())
        .thenEvent(Testopithecus.composeEvents.editBody(10))
        .thenEvent(Testopithecus.composeEvents.editBody(44))
        .thenEvent(Testopithecus.composeEvents.sendMessage())

    const evaluations: Array<Evaluation<any, null>> = [new ComposeAttachmentsCountEvaluation(), new ComposeHasAttachmentsEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [0, false])
    done()
  });

  it('should be correct for scenario with attachmnets', (done) => {
    const session = new Scenario()
      .thenEvent(Testopithecus.messageListEvents.writeNewMessage())
      .thenEvent(Testopithecus.composeEvents.addAttachments(2))
      .thenEvent(Testopithecus.composeEvents.addAttachments(1))
      .thenEvent(Testopithecus.composeEvents.pressBack(false))

    const evaluations: Array<Evaluation<any, null>> = [new ComposeAttachmentsCountEvaluation(), new ComposeHasAttachmentsEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [3, true])
    done()
  });

  it('should be correct for unfinished scenario', (done) => {
    const session = new Scenario()
      .thenEvent(Testopithecus.messageListEvents.writeNewMessage())
      .thenEvent(Testopithecus.composeEvents.addAttachments(2))
      .thenEvent(Testopithecus.composeEvents.addAttachments(1))

    const evaluations: Array<Evaluation<any, null>> = [new ComposeAttachmentsCountEvaluation(), new ComposeHasAttachmentsEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [3, true])
    done()
  });

  it('should be correct for scenario when not all attachments were removed', (done) => {
    const session = new Scenario()
      .thenEvent(Testopithecus.messageListEvents.writeNewMessage())
      .thenEvent(Testopithecus.composeEvents.addAttachments(2))
      .thenEvent(Testopithecus.composeEvents.removeAttachment())
      .thenEvent(Testopithecus.composeEvents.removeAttachment())
      .thenEvent(Testopithecus.composeEvents.addAttachments(1))

    const evaluations: Array<Evaluation<any, null>> = [new ComposeAttachmentsCountEvaluation(), new ComposeHasAttachmentsEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [1, true])
    done()
  });

  it('should be correct for scenario with removed attachments', (done) => {
    const session = new Scenario()
      .thenEvent(Testopithecus.messageListEvents.writeNewMessage())
      .thenEvent(Testopithecus.composeEvents.addAttachments(2))
      .thenEvent(Testopithecus.composeEvents.removeAttachment())
      .thenEvent(Testopithecus.composeEvents.removeAttachment())
      .thenEvent(Testopithecus.composeEvents.addAttachments(1))
      .thenEvent(Testopithecus.composeEvents.removeAttachment())
      .thenEvent(Testopithecus.composeEvents.editBody())
      .thenEvent(Testopithecus.composeEvents.sendMessage())

    const evaluations: Array<Evaluation<any, null>> = [new ComposeAttachmentsCountEvaluation(), new ComposeHasAttachmentsEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [0, false])
    done()
  });
});
