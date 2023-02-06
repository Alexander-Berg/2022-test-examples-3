import { AnalyticsRunner } from '../../../../analytics/analytics-runner';
import { ComposeBodyLengthEvaluation } from '../../../../analytics/mail/scenarios/compose/compose-body-length-evaluation';
import { Scenario } from '../../../../analytics/scenario';
import { Testopithecus } from '../../../../code/mail/logging/events/testopithecus';
import { checkEvaluationsResults} from '../../utils/utils';

describe('Compose body length evaluation', () => {
  it('should be correct for edit text scenario', (done) => {
    const session = new Scenario()
        .thenEvent(Testopithecus.messageListEvents.writeNewMessage())
        .thenEvent(Testopithecus.composeEvents.editBody(10))
        .thenEvent(Testopithecus.composeEvents.editBody(44))
        .thenEvent(Testopithecus.composeEvents.sendMessage())

    const evaluations = [new ComposeBodyLengthEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [44])
    done()
  });

  it('should be correct for unsuccessful scenario', (done) => {
    const session = new Scenario()
      .thenEvent(Testopithecus.messageListEvents.writeNewMessage())
      .thenEvent(Testopithecus.composeEvents.addAttachments(1))
      .thenEvent(Testopithecus.composeEvents.pressBack(false))

    const evaluations = [new ComposeBodyLengthEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [null])
    done()
  });

  it('should be correct for empty scenario', (done) => {
    const session = new Scenario()

    const evaluations = [new ComposeBodyLengthEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [null])
    done()
  });

  it('should be false for scenario', (done) => {
    const session = new Scenario()
      .thenEvent(Testopithecus.messageListEvents.writeNewMessage())
      .thenEvent(Testopithecus.composeEvents.editBody())
      .thenEvent(Testopithecus.composeEvents.editBody())
      .thenEvent(Testopithecus.composeEvents.pressBack(false))

    const evaluations = [new ComposeBodyLengthEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [null])
    done()
  });
});
