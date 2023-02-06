import { Scenario } from '../../../analytics/scenario';
import { Testopithecus } from '../../../code/mail/logging/events/testopithecus';
import { int64 } from '../../../ys/ys'
import { checkScenario } from '../utils/utils';

describe('Report should report events', () => {
  it('in one empty scenario', (done) => {
    checkScenario(
      new Scenario(),
      new Scenario(),
    )
    done()
  });
  it('in one event scenario', (done) => {
    checkScenario(
      new Scenario().thenEvent(Testopithecus.messageListEvents.openMessage(1, int64(1))),
      new Scenario().thenEvent(Testopithecus.messageListEvents.openMessage(1, int64(1))),
    )
    done()
  });
  it('in two event scenario', (done) => {
    checkScenario(
      new Scenario()
        .thenEvent(Testopithecus.messageListEvents.openMessage(1, int64(1)))
        .thenEvent(Testopithecus.messageViewEvents.reply(0)),
      new Scenario()
        .thenEvent(Testopithecus.messageListEvents.openMessage(1, int64(1)))
        .thenEvent(Testopithecus.messageViewEvents.reply(0)),
    )
    done()
  });
});
