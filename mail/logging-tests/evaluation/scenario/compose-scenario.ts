import { AnalyticsRunner } from '../../../../analytics/analytics-runner';
import { MailContextApplier } from '../../../../analytics/mail/mail-context-applier';
import { DefaultScenarios } from '../../../../analytics/mail/scenarios/default-scenarios';
import { Scenario } from '../../../../analytics/scenario';
import { Testopithecus } from '../../../../code/mail/logging/events/testopithecus';
import { MessageDTO } from '../../../../code/mail/logging/objects/message';
import { int64 } from '../../../../ys/ys'
import { setIncrementalTimeline } from '../../utils/utils';

describe('Compose scenario', () => {
  it('should evaluate all fields', (done) => {
    const sessions = [
      new Scenario()
        .thenEvent(Testopithecus.startEvents.startWithMessageListShow())
        .thenEvent(Testopithecus.messageListEvents.writeNewMessage())
        .thenEvent(Testopithecus.composeEvents.editBody())
        .thenEvent(Testopithecus.composeEvents.addAttachments(2))
        .thenEvent(Testopithecus.composeEvents.pressBack(false)),
      new Scenario()
        .thenEvent(Testopithecus.startEvents.startWithMessageListShow())
        .thenEvent(Testopithecus.modelSyncEvents.updateMessageList([new MessageDTO(int64(0), int64(10))]))
        .thenEvent(Testopithecus.messageListEvents.openMessage(0, int64(0)))
        .thenEvent(Testopithecus.messageViewEvents.replyAll(1))
        .thenEvent(Testopithecus.composeEvents.editBody())
        .thenEvent(Testopithecus.composeEvents.addAttachments(2))
        .thenEvent(Testopithecus.composeEvents.editBody())
        .thenEvent(Testopithecus.composeEvents.sendMessage())
        .thenEvent(Testopithecus.messageListEvents.deleteMessage(0, int64(0))),
    ]
    const runner = new AnalyticsRunner()
    const requiredAttributes = [
      'start_timestamp_ms',
      'finish_timestamp_ms',
      'duration_scenario_ms',
      'scenario_type',
      'mid',
      'receive_timestamp',
      'sending',
    ]

    for (const session of sessions) {
      setIncrementalTimeline(session)
      const evaluations = [DefaultScenarios.composeScenario()]

      const results = runner.evaluateWithContext(session, evaluations, new MailContextApplier())
      const attributes = results.get(evaluations[0].name())!
      for (const scenarioAttribute of attributes) {
        for (const attribute of requiredAttributes) {
          scenarioAttribute.attributes.has(attribute)
        }
      }
    }
    done()
  });
});
