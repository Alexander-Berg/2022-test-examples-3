import { Scenario } from '../../../analytics/scenario';
import { Testopithecus } from '../../../code/mail/logging/events/testopithecus';
import { MessageDTO } from '../../../code/mail/logging/objects/message';
import { int64 } from '../../../ys/ys'
import { checkScenario } from '../utils/utils';

describe('Message list sync aggregator', () => {
  it('should aggregate only sync events', (done) => {
    checkScenario(
      new Scenario()
        .thenEvent(Testopithecus.messageViewEvents.backToMailList())
        .thenEvent(Testopithecus.messageViewEvents.backToMailList()),
      new Scenario()
        .thenEvent(Testopithecus.messageViewEvents.backToMailList())
        .thenEvent(Testopithecus.messageViewEvents.backToMailList()),
    )
    done()
  });
  it('should aggregate one sync event', (done) => {
    checkScenario(
      new Scenario()
        .thenEvent(Testopithecus.modelSyncEvents.updateMessageList([new MessageDTO(int64(0), int64(0))])),
      new Scenario()
        .thenEvent(Testopithecus.modelSyncEvents.updateMessageList([new MessageDTO(int64(0), int64(0))])),
    )
    done()
  });
  it('should aggregate distinct sync event', (done) => {
    checkScenario(
      new Scenario()
        .thenEvent(Testopithecus.modelSyncEvents.updateMessageList([new MessageDTO(int64(0), int64(0))]))
        .thenEvent(Testopithecus.modelSyncEvents.updateMessageList([new MessageDTO(int64(1), int64(1))])),
      new Scenario()
        .thenEvent(Testopithecus.modelSyncEvents.updateMessageList([new MessageDTO(int64(0), int64(0))]))
        .thenEvent(Testopithecus.modelSyncEvents.updateMessageList([new MessageDTO(int64(1), int64(1))])),
    )
    done()
  });
  it('should not send duplicate sync event', (done) => {
    checkScenario(
      new Scenario()
        .thenEvent(Testopithecus.modelSyncEvents.updateMessageList([new MessageDTO(int64(0), int64(0))]))
        .thenEvent(Testopithecus.modelSyncEvents.updateMessageList([new MessageDTO(int64(1), int64(1))]))
        .thenEvent(Testopithecus.modelSyncEvents.updateMessageList([new MessageDTO(int64(0), int64(2)), new MessageDTO(int64(1), int64(2))])),
      new Scenario()
        .thenEvent(Testopithecus.modelSyncEvents.updateMessageList([new MessageDTO(int64(0), int64(0))]))
        .thenEvent(Testopithecus.modelSyncEvents.updateMessageList([new MessageDTO(int64(1), int64(1))])),
    )
    done()
  });
  it('should send updates only for sync event', (done) => {
    checkScenario(
      new Scenario()
        .thenEvent(Testopithecus.modelSyncEvents.updateMessageList([new MessageDTO(int64(0), int64(0))]))
        .thenEvent(Testopithecus.modelSyncEvents.updateMessageList([new MessageDTO(int64(1), int64(1)), new MessageDTO(int64(0), int64(0))])),
      new Scenario()
        .thenEvent(Testopithecus.modelSyncEvents.updateMessageList([new MessageDTO(int64(0), int64(0))]))
        .thenEvent(Testopithecus.modelSyncEvents.updateMessageList([new MessageDTO(int64(1), int64(1))])),
    )
    done()
  });
});
