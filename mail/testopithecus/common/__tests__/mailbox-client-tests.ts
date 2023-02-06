import * as assert from 'assert';
import { MailboxClientHandler } from '../code/client/mailbox-client';
import { MobileMailBackend } from '../code/mail/backend/mobile-mail-backend';
import { MailboxDownloader } from '../code/mail/mailbox-downloader';
import { ImapMailbox, ImapMessage } from '../code/mail/mailbox-preparer';
import { DefaultFolderName } from '../code/mail/model/mail-model';
import { int64, range } from '../ys/ys'
import { ConsoleLog } from './pod/console-log';
import { PRIVATE_BACKEND_CONFIG } from './private-backend-config';
import { createMailboxPreparer, createNetworkClient } from './test-utils';

// These tests almost do not depend on platform
describe('Mailbox client should', function() {
  this.timeout(30000);
  setUpRegistry();
  const client = createNetworkClient();

  it('download all folders', (done) => {
    client.getFolderList();
    done()
  });

  it('download all messages in folders', (done) => {
    const folders = client.getFolderList();
    folders.forEach((folder) => client.getMessagesInFolder(folder.fid, 5));
    done()
  });

  it('download all threads in folders', (done) => {
    const folders = client.getFolderList();
    folders.forEach((folder) => client.getThreadsInFolder(folder.fid, 5));
    done()
  });

  it('not fall on not existing folder', (done) => {
    const messages = client.getThreadsInFolder(int64(0), 5);
    assert.strictEqual(messages.length, 0);
    done()
  });

  it('get settings', (done) => {
    client.getSettings();
    done()
  });

  it('send message with text', (done) => {
    client.sendMessage('yandex-team-15929.11479@yandex.ru', 'Как ', 'Привет, как дела?');
    done()
  });

  it('send mark nonexisting as read', (done) => {
    client.markMessageAsRead(int64(1));
    done()
  });

  it('send mark nonexisting as unread', (done) => {
    client.markMessageAsUnread(int64(1));
    done()
  });

});

describe('Mailbox downloader should', () => {
  it('download model', (done) => {
    setUpRegistry();
    const client = createNetworkClient();
    const downloader = new MailboxDownloader([client], ConsoleLog.LOGGER);
    const model = downloader.takeAppModel();
    done()
  });
});

describe('Mailbox backend', function() {
  this.timeout(30000);
  const client = createNetworkClient();
  const clientsHandler = new MailboxClientHandler([client]);
  clientsHandler.clientsManager.currentAccount = 0;
  const backend = new MobileMailBackend(clientsHandler);
  const preparer = createMailboxPreparer()
  const mailbox = ImapMailbox.builder(PRIVATE_BACKEND_CONFIG.account)
    .nextMessage('subj')
    .build();

  beforeEach((done) => {
    preparer.prepare(mailbox)
    done()
  });

  it('should create new thread', (done) => {
    backend.writeMessage.createThread('yandex-team-15929.11479@yandex.ru', 'Test', 8, null);
    done()
  });

  it('should delete top message', (done) => {
    backend.deleteMessage.deleteMessage(0);
    done()
  });

  it('should mark top message as undread', (done) => {
    backend.markable.markAsUnread(0);
    done()
  });

  it('should label top message as important', (done) => {
    backend.markableImportant.markAsImportant(0);
    done()
  });

  it('should create folder', (done) => {
    backend.creatableFolder.createFolder('New Folder1');
    done()
  });

  it ('should move message to folder', (done) => {
    backend.creatableFolder.createFolder('New Folder1')
    backend.movableToFolder.moveMessageToFolder(0, 'New Folder1');
    done()
  });

  it('should create new thread with texts', (done) => {
    const texts = [];
    for (const i of range(0, 8)) {
      texts.push(`DEMO_TEXT_${i}`)
    }
    backend.writeMessage.createThread('yandex-team-15929.11479@yandex.ru', 'Test', 8, texts);
    done()
  })

  it('should move to spam by order', (done) => {
    backend.spamable.moveToSpam(0)
    done()
  });

  it('should move to archive by order', (done) => {
    backend.archive.archiveMessage(0)
    done()
  });

  it('should show all messages from delete folder', (done) => {
    backend.movableToFolder.moveMessageToFolder(0, DefaultFolderName.trash)
    backend.folderNavigator.goToFolder(DefaultFolderName.trash)
    assert.strictEqual(backend.messageListDisplay.isInThreadMode(), false)
    const msg = backend.messageListDisplay.getMessageList(10);
    assert.strictEqual(msg.length, 1)
    done()
  });
});

function setUpRegistry() {
  // Registry.get().logger = new ConsoleLog();
}
