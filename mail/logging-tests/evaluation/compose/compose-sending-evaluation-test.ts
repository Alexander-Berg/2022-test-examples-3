import { AnalyticsRunner } from '../../../../analytics/analytics-runner';
import { ComposeSimpleEvaluations } from '../../../../analytics/mail/scenarios/compose/compose-simple-evaluations';
import { Scenario } from '../../../../analytics/scenario';
import { Testopithecus } from '../../../../code/mail/logging/events/testopithecus';
import { checkEvaluationsResults} from '../../utils/utils';

describe('Compose sending event evaluation', () => {
  it('should be correct for successful scenario', (done) => {
    const session = new Scenario()
        .thenEvent(Testopithecus.messageListEvents.writeNewMessage())
        .thenEvent(Testopithecus.composeEvents.editBody())
        .thenEvent(Testopithecus.composeEvents.editBody())
        .thenEvent(Testopithecus.composeEvents.sendMessage())

    const evaluations = [ComposeSimpleEvaluations.sendingEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [true])
    done()
  });

  it('should be correct for unsuccessful scenario', (done) => {
    const session = new Scenario()
      .thenEvent(Testopithecus.messageListEvents.writeNewMessage())
      .thenEvent(Testopithecus.composeEvents.editBody())
      .thenEvent(Testopithecus.composeEvents.editBody())
      .thenEvent(Testopithecus.composeEvents.pressBack(false))

    const evaluations = [ComposeSimpleEvaluations.sendingEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [false])
    done()
  });

  it('should be correct for empty scenario', (done) => {
    const session = new Scenario()

    const evaluations = [ComposeSimpleEvaluations.sendingEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [false])
    done()
  });

  it('should be false for unfinished scenario', (done) => {
    const session = new Scenario()
      .thenEvent(Testopithecus.messageListEvents.writeNewMessage())
      .thenEvent(Testopithecus.composeEvents.editBody())
      .thenEvent(Testopithecus.composeEvents.editBody())

    const evaluations = [ComposeSimpleEvaluations.sendingEvaluation()]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [false])
    done()
  });
});
