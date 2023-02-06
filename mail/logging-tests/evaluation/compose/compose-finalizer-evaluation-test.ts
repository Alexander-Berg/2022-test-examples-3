import { AnalyticsRunner } from '../../../../analytics/analytics-runner';
import { Evaluation } from '../../../../analytics/evaluations/evaluation';
import { ComposeCompletedEvaluation } from '../../../../analytics/mail/scenarios/compose/compose-completed-evaluation';
import { ComposeFinalizerEvaluation } from '../../../../analytics/mail/scenarios/compose/compose-finalizer-evaluation';
import { ComposeQualityTypeEvaluation } from '../../../../analytics/mail/scenarios/compose/compose-quality-type-evaluation';
import { Scenario } from '../../../../analytics/scenario';
import { EventNames } from '../../../../code/mail/logging/events/event-names';
import { Testopithecus } from '../../../../code/mail/logging/events/testopithecus';
import { checkEvaluationsResults} from '../../utils/utils';

describe('Compose finalizer evaluation', () => {
  it('should be correct for empty scenario', (done) => {
    const session = new Scenario()

    const evaluations: Array<Evaluation<any, null>> = [
      new ComposeFinalizerEvaluation(),
      new ComposeCompletedEvaluation(),
      new ComposeQualityTypeEvaluation(),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [null, false, 'unfinished'])
    done()
  });

  it('should be correct for finished with back scenario', (done) => {
    const session = new Scenario()
        .thenEvent(Testopithecus.messageListEvents.writeNewMessage())
        .thenEvent(Testopithecus.composeEvents.editBody())
        .thenEvent(Testopithecus.composeEvents.pressBack(false))

    const evaluations: Array<Evaluation<any, null>> = [
      new ComposeFinalizerEvaluation(),
      new ComposeCompletedEvaluation(),
      new ComposeQualityTypeEvaluation(),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [EventNames.COMPOSE_BACK, true, 'success'])
    done()
  });

  it('should be correct for finished with send scenario', (done) => {
    const session = new Scenario()
      .thenEvent(Testopithecus.messageListEvents.writeNewMessage())
      .thenEvent(Testopithecus.composeEvents.addAttachments(10))
      .thenEvent(Testopithecus.composeEvents.sendMessage())

    const evaluations: Array<Evaluation<any, null>> = [
      new ComposeFinalizerEvaluation(),
      new ComposeCompletedEvaluation(),
      new ComposeQualityTypeEvaluation(),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [EventNames.COMPOSE_SEND_MESSAGE, true, 'success'])
    done()
  });

  it('should be correct for unfinished scenario', (done) => {
    const session = new Scenario()
      .thenEvent(Testopithecus.messageListEvents.writeNewMessage())
      .thenEvent(Testopithecus.composeEvents.addAttachments(10))
      .thenEvent(Testopithecus.composeEvents.editBody())

    const evaluations: Array<Evaluation<any, null>> = [
      new ComposeFinalizerEvaluation(),
      new ComposeCompletedEvaluation(),
      new ComposeQualityTypeEvaluation(),
    ]
    const runner = new AnalyticsRunner()
    const results = runner.evaluate(session, evaluations)

    checkEvaluationsResults(evaluations, results, [null, false, 'unfinished'])
    done()
  });

});
