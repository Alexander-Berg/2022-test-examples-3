import * as assert from 'assert';
import { ImapMailbox, ImapMessage, ImapUser, MailboxPreparer } from '../code/mail/mailbox-preparer';
import { DefaultFolderName } from '../code/mail/model/mail-model'
import { ConsoleLog } from './pod/console-log';
import { DefaultSyncNetwork } from './pod/default-http';
import { DefaultJSONSerializer } from './pod/default-json';
import { SyncSleepImpl } from './pod/sleep'
import { PRIVATE_BACKEND_CONFIG } from './private-backend-config';

describe('IMAP model builder', () => {
  it('should create model correctly', (done) => {
    const mailbox = ImapMailbox.builder(PRIVATE_BACKEND_CONFIG.account)
      .nextMessage('subj')
      .build();
    const item = mailbox.toJson();
    const actual = new DefaultJSONSerializer().serialize(item).getValue();

    const expected = '{"folders": [{"name": "Inbox", "messages": [{"sender": {"email": "other.user@ya.ru", "name": "Other User"}, "subject": "subj", "textBody": "first line", "timestamp": "2019-12-05T22:21:00.000Z", "toReceivers": []}]}], "mailAccount": {"host": "imap.yandex.ru", "login": "yandex-team-47907-42601@yandex.ru", "password": "simple123456"}}';
    assert.strictEqual(actual, expected);
    done()
  });
});

describe('IMAP model preparer', function() {
  this.timeout(30000);
  it('should prepare model correctly', (done) => {

    const mailbox = ImapMailbox.builder(PRIVATE_BACKEND_CONFIG.account)
      .addMessageToFolder(DefaultFolderName.inbox, ImapMessage.builder().withSender(new ImapUser('katya@yandex.ru', 'Катя')).withSubject('Важные дела').withTextBody('Будут завтра').withTimestamp('2019-09-11T17:03:06.504Z').build())
      .addMessageToFolder(DefaultFolderName.inbox, ImapMessage.builder().withSender(new ImapUser('dasha@yandex.ru', 'Даша')).withSubject('Ты где?').withTextBody('Все уже тут').withTimestamp('2019-09-11T17:03:06.504Z').build())
      .addMessageToFolder(DefaultFolderName.inbox, ImapMessage.builder().withSender(new ImapUser('masha@yandex.ru', 'Маша')).withSubject('Привет').withTextBody('Как дела?').withTimestamp('2019-09-10T17:03:06.504Z').build())
      .build();

    const jsonSerializer = new DefaultJSONSerializer();
    const network = new DefaultSyncNetwork(jsonSerializer, ConsoleLog.LOGGER);

    const preparer = new MailboxPreparer(network, jsonSerializer, SyncSleepImpl.instance, ConsoleLog.LOGGER);
    preparer.prepare(mailbox);
    done();
  });
});
