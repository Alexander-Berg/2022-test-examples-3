import * as assert from 'assert'
import { SpamableFeature } from '../code/mail/mail-features'
import {
  AccountMailboxData,
  AccountSettings,
  DefaultFolderName,
  Folder,
  FolderName,
  FullMessage,
  MailAppModelHandler,
  MailboxModel,
  Message,
  MessageId,
} from '../code/mail/model/mail-model'
import { AppModelProvider } from '../code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../code/users/user-pool'
import { assertBooleanEquals } from '../code/utils/assert'
import { Registry } from '../code/utils/registry'
import { requireNonNull } from '../code/utils/utils'
import { Int32, int64, Nullable } from '../ys/ys'
import { ConsoleLog } from './pod/console-log'

export class MockMailboxProvider implements AppModelProvider {

  private readonly model: MailboxModel

  private constructor(accountDataHandler: MailAppModelHandler) {
    accountDataHandler.logInToAccount(new UserAccount('mock-mailbox@yandex.ru', '123456'))
    this.model = new MailboxModel(accountDataHandler)
  }

  public static emptyFoldersOneAccount(): MockMailboxProvider {
    const inbox = new Folder(DefaultFolderName.inbox)
    const trash = new Folder(DefaultFolderName.trash)
    const draft = new Folder(DefaultFolderName.draft)
    const spam = new Folder(DefaultFolderName.spam)
    const sent = new Folder(DefaultFolderName.sent)

    const folderToMessages: Map<FolderName, Set<MessageId>> = new Map<FolderName, Set<MessageId>>(
      [
        [inbox.name, new Set<MessageId>()],
        [trash.name, new Set<MessageId>()],
        [sent.name, new Set<MessageId>()],
        [draft.name, new Set<MessageId>()],
        [spam.name, new Set<MessageId>()],
      ])

    const accountData = new AccountMailboxData(
      new UserAccount('mock-mailbox@yandex.ru', '123456'),
      new Map<MessageId, FullMessage>(),
      folderToMessages,
      [],
      ['mock-mailbox@yandex.ru'],
      [],
      new AccountSettings(true),
    )
    const accountDataHandler = new MailAppModelHandler([accountData])
    accountDataHandler.logInToAccount(new UserAccount('mock-mailbox@yandex.ru', '123456'))
    return new MockMailboxProvider(accountDataHandler)
  }

  public static exampleOneAccount(): MockMailboxProvider {
    Registry.get().logger = ConsoleLog.LOGGER
    const messages = new Map<MessageId, FullMessage>()
    messages.set(int64(6), new FullMessage(new Message('from4', 'subject5', int64(5), 4)))
    messages.set(int64(5), new FullMessage(new Message('from4', 'subject4', int64(4), 4, true)))
    messages.set(int64(3), new FullMessage(new Message('from3', 'subject3', int64(2), 4, true)))
    messages.set(int64(1), new FullMessage(new Message('from1', 'subject1', int64(0), 4)))
    messages.set(int64(4), new FullMessage(new Message('from3', 'subject3', int64(3), 1, true)))
    messages.set(int64(2), new FullMessage(new Message('from2', 'subject2', int64(1), 1)))
    messages.set(int64(8), new FullMessage(new Message('from7', 'subject6', int64(7), 2)))
    messages.set(int64(7), new FullMessage(new Message('from7', 'subject6', int64(6), 2)))
    messages.set(int64(10), new FullMessage(new Message('from8', 'subject7', int64(9), 2)))
    messages.set(int64(9), new FullMessage(new Message('from8', 'subject7', int64(8), 2)))
    messages.set(int64(11), new FullMessage(new Message('from11', 'subject11', int64(11), 1)))

    const inbox = new Folder(DefaultFolderName.inbox)
    const trash = new Folder(DefaultFolderName.trash)
    const draft = new Folder(DefaultFolderName.draft)
    const spam = new Folder(DefaultFolderName.spam)
    const sent = new Folder(DefaultFolderName.sent)

    const sentSet: Set<MessageId> = new Set<MessageId>([int64(7), int64(8)])
    const inboxSet: Set<MessageId> = new Set<MessageId>([int64(1), int64(2), int64(3), int64(4), int64(5), int64(6)])
    const trashSet: Set<MessageId> = new Set<MessageId>([int64(9), int64(10)])
    const threads = [
      new Set([int64(1), int64(3), int64(5), int64(6)]),
      new Set([int64(7), int64(8)]),
      new Set([int64(9), int64(10)]),
    ]
    const folderToMessages: Map<FolderName, Set<MessageId>> = new Map<FolderName, Set<MessageId>>(
      [
        [inbox.name, inboxSet],
        [trash.name, trashSet],
        [sent.name, sentSet],
        [draft.name, new Set<MessageId>()],
        [spam.name, new Set<MessageId>()],
      ])

    const contacts = ['testtest@yandex.ru', 'checktest@yandex.ru', 'maintest@yandex.ru']
    const userAccount = new UserAccount('mock-mailbox@yandex.ru', '123456')
    const accountSettings = new AccountSettings(true)
    const accountData = new AccountMailboxData(
      userAccount,
      messages,
      folderToMessages,
      threads,
      ['mock-mailbox@yandex.ru'],
      contacts,
      accountSettings,
    )
    const accountDataHandler = new MailAppModelHandler([accountData])
    accountDataHandler.logInToAccount(new UserAccount('mock-mailbox@yandex.ru', '123456'))
    return new MockMailboxProvider(accountDataHandler)
  }

  public takeAppModel(): MailboxModel {
    return this.model
  }

}

describe('Model unit tests', () => {
  Registry.get().logger = ConsoleLog.LOGGER
  it('should create folders in model', (done) => {
    const firstFolderName = 'New Folder'
    const secondFolderName = 'Even newer folder'
    const model = MockMailboxProvider.exampleOneAccount().takeAppModel()
    const foldersInitialSize = model.folderNavigator.getFoldersList().length
    model.creatableFolder.createFolder(firstFolderName)
    assert.strictEqual(model.folderNavigator.getFoldersList().length, foldersInitialSize + 1)
    model.creatableFolder.createFolder(secondFolderName)
    assert.strictEqual(model.folderNavigator.getFoldersList().length, foldersInitialSize + 2)
    assertBooleanEquals(
      true,
      model.folderNavigator.getFoldersList().filter((f) => f.name === firstFolderName).length === 1,
      'no first added folder',
    )
    assertBooleanEquals(
      true,
      model.folderNavigator.getFoldersList().filter((f) => f.name === secondFolderName).length === 1,
      'no second added folder',
    )
    done()
  })
  it('should move message to another folder', (done) => {
    const folderToCreate = 'another folder'
    const model = MockMailboxProvider.exampleOneAccount().takeAppModel()
    model.creatableFolder.createFolder(folderToCreate)
    const msgId = model.messageListDisplay.getMessageThreadByOrder(0)[0]!
    model.movableToFolder.moveMessageToFolder(0, folderToCreate)
    assert.strictEqual(
      model.messageListDisplay.accountDataHandler.getCurrentAccount().folderToMessages.get(folderToCreate)!.has(msgId),
      true,
      'message was not moved',
    )
    done()
  })

  it('should see messages from different folders', (done) => {
    const model = MockMailboxProvider.exampleOneAccount().takeAppModel()
    const messagesInbox = model.messageListDisplay.getMessageList(100)
    assert.strictEqual(messagesInbox.length, 3)
    model.folderNavigator.goToFolder(DefaultFolderName.sent)
    const sentMessages = model.messageListDisplay.getMessageList(100)
    assert.strictEqual(sentMessages.length, 1)
    model.folderNavigator.goToFolder(DefaultFolderName.trash)
    const trashMessages = model.messageListDisplay.getMessageList(100)
    assert.strictEqual(trashMessages.length, 2)
    done()
  })
  it('model should list display messages correct', (done) => {
    const model = MockMailboxProvider.exampleOneAccount().takeAppModel()
    const messages = model.messageListDisplay.getMessageList(6)
    assert.strictEqual(messages[0].subject, 'subject5')
    assert.strictEqual(messages[1].subject, 'subject3')
    assert.strictEqual(messages[2].subject, 'subject2')
    assert.strictEqual(messages.length, 3)
    done()
  })
  it('model should mark as unread only one message in thread', (done) => {
    const model = MockMailboxProvider.exampleOneAccount().takeAppModel()
    const threadMsg0 = model.readOnlyExpandableThreads.getThreadMessage(0, 0)
    const threadMsg1 = model.readOnlyExpandableThreads.getThreadMessage(0, 1)
    const threadMsg2 = model.readOnlyExpandableThreads.getThreadMessage(0, 2)
    const threadMsg3 = model.readOnlyExpandableThreads.getThreadMessage(0, 3)
    model.expandableThreads.markThreadMessageAsUnRead(0, 1)
    assert.strictEqual(model.messageListDisplay.getMessageList(6)[0].read, false)
    assert.strictEqual(threadMsg0.head.read, false)
    assert.strictEqual(threadMsg1.head.read, false)
    assert.strictEqual(threadMsg2.head.read, true)
    assert.strictEqual(threadMsg3.head.read, false)
    done()
  })
  it('model should mark as read only one message in thread', (done) => {
    const model = MockMailboxProvider.exampleOneAccount().takeAppModel()
    const threadMsg0 = model.readOnlyExpandableThreads.getThreadMessage(0, 0)
    const threadMsg1 = model.readOnlyExpandableThreads.getThreadMessage(0, 1)
    const threadMsg2 = model.readOnlyExpandableThreads.getThreadMessage(0, 2)
    const threadMsg3 = model.readOnlyExpandableThreads.getThreadMessage(0, 3)
    model.expandableThreads.markThreadMessageAsRead(0, 0)
    assert.strictEqual(model.messageListDisplay.getMessageList(6)[0].read, false)
    assert.strictEqual(threadMsg0.head.read, true)
    assert.strictEqual(threadMsg1.head.read, true)
    assert.strictEqual(threadMsg2.head.read, true)
    assert.strictEqual(threadMsg3.head.read, false)
    done()
  })
  it('model should mark simple message correct', (done) => {
    const model = MockMailboxProvider.exampleOneAccount().takeAppModel()
    model.markableRead.markAsRead(2)
    assert.strictEqual(model.messageListDisplay.getMessageList(6)[2].read, true)
    model.markableRead.markAsUnread(2)
    assert.strictEqual(model.messageListDisplay.getMessageList(6)[2].read, false)
    done()
  })
  it('model should mark thread as simple message correct', (done) => {
    const model = MockMailboxProvider.exampleOneAccount().takeAppModel()
    model.markableRead.markAsRead(0)
    assert.strictEqual(model.messageListDisplay.getMessageList(6)[0].read, true)
    for (let i = 0; i < 4; i++) {
      assert.strictEqual(model.readOnlyExpandableThreads.getThreadMessage(0, i).head.read, true)
    }
    model.markableRead.markAsUnread(0)
    assert.strictEqual(model.messageListDisplay.getMessageList(6)[0].read, false)
    for (let i = 0; i < 4; i++) {
      assert.strictEqual(model.readOnlyExpandableThreads.getThreadMessage(0, i).head.read, false)
    }
    done()
  })
  it('model should label simple message as important correct', (done) => {
    const model = MockMailboxProvider.exampleOneAccount().takeAppModel()
    model.markableImportant.markAsImportant(2)
    assert.strictEqual(model.messageListDisplay.getMessageList(6)[2].important, true)
    model.markableImportant.markAsUnimportant(2)
    assert.strictEqual(model.messageListDisplay.getMessageList(6)[2].important, false)
    done()
  })
  it('model should label thread as simple message important correct', (done) => {
    const model = MockMailboxProvider.exampleOneAccount().takeAppModel()
    model.markableImportant.markAsImportant(0)
    assert.strictEqual(model.messageListDisplay.getMessageList(6)[0].important, true)
    for (let i = 0; i < 4; i++) {
      assert.strictEqual(model.readOnlyExpandableThreads.getThreadMessage(0, i).head.important, true)
    }
    model.markableImportant.markAsUnimportant(0)
    assert.strictEqual(model.messageListDisplay.getMessageList(6)[0].important, false)
    for (let i = 0; i < 4; i++) {
      assert.strictEqual(model.readOnlyExpandableThreads.getThreadMessage(0, i).head.important, false)
    }
    done()
  })
  it('model should label as important only one message in thread', (done) => {
    const model = MockMailboxProvider.exampleOneAccount().takeAppModel()
    const threadMsg0 = model.readOnlyExpandableThreads.getThreadMessage(0, 0)
    const threadMsg1 = model.readOnlyExpandableThreads.getThreadMessage(0, 1)
    const threadMsg2 = model.readOnlyExpandableThreads.getThreadMessage(0, 2)
    const threadMsg3 = model.readOnlyExpandableThreads.getThreadMessage(0, 3)
    model.expandableThreads.markThreadMessageAsImportant(0, 0)
    assert.strictEqual(model.messageListDisplay.getMessageList(6)[0].important, true)
    assert.strictEqual(threadMsg0.head.important, true)
    assert.strictEqual(threadMsg1.head.important, false)
    assert.strictEqual(threadMsg2.head.important, false)
    assert.strictEqual(threadMsg3.head.important, false)
    done()
  })
  it('should add sent and received message to model', (done) => {
    const model = MockMailboxProvider.exampleOneAccount().takeAppModel()
    model.writable.sendMessage(model.writable.accountDataHandler.getCurrentAccount().aliases[0], 'test_message')
    assert.strictEqual(model.messageListDisplay.getMessageList(8)[0].subject, 'test_message')
    assert.strictEqual(model.messageListDisplay.getMessageList(8)[0].read, false)
    assert.strictEqual(model.messageListDisplay.getMessageList(8)[0].from, 'mock-mailbox@yandex.ru')
    model.folderNavigator.goToFolder(DefaultFolderName.sent)
    assert.strictEqual(model.messageListDisplay.getMessageList(8)[0].subject, 'test_message')
    assert.strictEqual(model.messageListDisplay.getMessageList(8)[0].read, false)
    assert.strictEqual(model.messageListDisplay.getMessageList(8)[0].from, 'mock-mailbox@yandex.ru')
    done()
  })
  it('should compose correct draft', (done) => {
    const to = ['to1@yandex.ru', 'to2@yandex.ru']
    const subject = 'test_subject'
    const body = 'test_body'
    const model = MockMailboxProvider.exampleOneAccount().takeAppModel()
    model.writable.openCompose()
    model.composable.addTo(to[0])
    model.composable.addTo(to[1])
    model.composable.setSubject(subject)
    model.composable.setBody(body)
    const draft = model.composable.getDraft()
    assert.deepStrictEqual(draft.to, new Set(to))
    assert.strictEqual(draft.subject, subject)
    assert.strictEqual(draft.getWysiwyg().getRichBody(), body)
    done()
  })
  it('should not have same addresses at to', (done) => {
    const to = ['to1@yandex.ru', 'to1@yandex.ru']
    const model = MockMailboxProvider.exampleOneAccount().takeAppModel()
    model.writable.openCompose()
    model.composable.addTo(to[0])
    model.composable.addTo(to[1])
    const draft = model.composable.getDraft()
    assert.deepStrictEqual(draft.to, new Set(to))
    done()
  })
  it('should change draft', (done) => {
    const subject = 'test_subject'
    const body = 'test_body'
    const newSubject = 'new_test_subject'
    const newBody = 'new_test_body'
    const model = MockMailboxProvider.exampleOneAccount().takeAppModel()
    model.writable.openCompose()
    model.composable.setSubject(subject)
    model.composable.setBody(body)
    model.composable.setSubject(newSubject)
    model.composable.setBody(newBody)
    const draft = model.composable.getDraft()
    assert.strictEqual(draft.subject, newSubject)
    assert.strictEqual(draft.getWysiwyg().getRichBody(), newBody)
    done()
  })
  it('should add to using suggest', (done) => {
    const model = MockMailboxProvider.exampleOneAccount().takeAppModel()
    model.writable.openCompose()
    model.composable.addToUsingSuggest('testtest')
    assert.strictEqual(model.composable.getDraft().to.size, 1)
    assert.strictEqual(model.composable.getDraft().to.has(`testtest@yandex.ru`), true)
    model.composable.addToUsingSuggest(`checktest`)
    assert.strictEqual(model.composable.getDraft().to.size, 2)
    assert.strictEqual(model.composable.getDraft().to.has(`checktest@yandex.ru`), true)
    done()
  })
  it('should send to spam folder if move to spam and than mark as not spam', (done) => {
    const model = MockMailboxProvider.exampleOneAccount().takeAppModel()
    SpamableFeature.get.forceCast(model).moveToSpam(0)
    model.folderNavigator.goToFolder(DefaultFolderName.spam)
    const messagesInSpam = model.messageListDisplay.getMessageList(10)
    assert.strictEqual(messagesInSpam.length, 4)
    assert.strictEqual(Message.matches(new Message('from4', 'subject5', int64(5), 4, true), messagesInSpam[0]), true)
    const messageToUnspam = messagesInSpam[0]
    SpamableFeature.get.forceCast(model).moveFromSpam(0)
    model.folderNavigator.goToFolder(DefaultFolderName.inbox)
    assert.strictEqual(Message.matches(messageToUnspam, model.messageListDisplay.getMessageList(1)[0]), true)
    done()
  })
  it('should compose correct body', (done) => {
    const model = MockMailboxProvider.exampleOneAccount().takeAppModel()
    model.writable.openCompose()
    model.composable.setBody('0123456789')
    model.wysiwyg.setItalic(0, 3)
    model.wysiwyg.setStrong(2, 7)
    assert.strictEqual(
      model.composable.getDraft().getWysiwyg().getRichBody(),
      '<em>01<strong>2</em>3456</strong>789',
    )
    done()
  })
  it('should add styled text', (done) => {
    const model = MockMailboxProvider.exampleOneAccount().takeAppModel()
    model.writable.openCompose()
    model.composable.setBody('0123456789')
    model.wysiwyg.setItalic(0, 3)
    model.wysiwyg.setStrong(2, 7)
    model.wysiwyg.appendText(3, '777')
    assert.strictEqual(
      model.composable.getDraft().getWysiwyg().getRichBody(),
      '<em>01<strong>2777</em>3456</strong>789',
    )
    done()
  })
  it('should send to archive folder if move to archive', (done) => {
    const model = MockMailboxProvider.exampleOneAccount().takeAppModel()
    model.archiveMessage.archiveMessage(0)
    model.folderNavigator.goToFolder(DefaultFolderName.archive)
    const messagesInArchive = model.messageListDisplay.getMessageList(10)
    assert.strictEqual(messagesInArchive.length, 4)
    assert.strictEqual(Message.matches(new Message('from4', 'subject5', int64(5), 4, false), messagesInArchive[0]), true)
    done()
  })
  it('should change group mode', (done) => {
    const model = MockMailboxProvider.exampleOneAccount().takeAppModel()
    model.settingsModel.openAccountSettings(requireNonNull(model.mailAppModelHandler.accountsManager.currentAccount, 'Необходимо зайти в настройки аккаунта'))
    assert.strictEqual(model.messageListDisplay.isInThreadMode(), true)
    model.messageListDisplay.toggleThreadMode()
    assert.strictEqual(model.messageListDisplay.isInThreadMode(), false)
    done()
  })
  it('should group messages', (done) => {
    const model = MockMailboxProvider.exampleOneAccount().takeAppModel()
    assert.strictEqual(model.messageListDisplay.getMessageId(1), int64(4))
    model.settingsModel.openAccountSettings(requireNonNull(model.mailAppModelHandler.accountsManager.currentAccount, 'Необходимо зайти в настройки аккаунта'))
    model.messageListDisplay.toggleThreadMode()
    assert.strictEqual(model.messageListDisplay.getMessageId(1), int64(5))
    done()
  })
  it('should send to trash if delete by short swipe', (done) => {
    const model = MockMailboxProvider.exampleOneAccount().takeAppModel()
    model.shortSwipeDelete.deleteMessageByShortSwipe(0)
    model.folderNavigator.goToFolder(DefaultFolderName.trash)
    const messagesInTrash = model.messageListDisplay.getMessageList(10)
    assert.strictEqual(messagesInTrash.length, 6)
    assert.strictEqual(Message.matches(new Message('from4', 'subject5', int64(5), 4, false), messagesInTrash[2]), true)
    done()
  })
  it('should mark as read and unread from group operations', (done) => {
    const model = MockMailboxProvider.exampleOneAccount().takeAppModel()
    let messages = model.messageListDisplay.getMessageList(2)
    assert.strictEqual(messages[0].read, false)
    assert.strictEqual(messages[1].read, true)
    model.groupMode.initialMessageSelect(0)
    model.groupMode.selectMessage(1)
    assert.strictEqual(model.groupMode.getSelectedMessages().size, 2)
    model.groupMode.markAsReadSelectedMessages()
    messages = model.messageListDisplay.getMessageList(2)
    assert.strictEqual(messages[0].read, true)
    assert.strictEqual(messages[1].read, true)
    assert.strictEqual(model.groupMode.getSelectedMessages().size, 0)
    model.groupMode.initialMessageSelect(0)
    model.groupMode.selectMessage(1)
    model.groupMode.selectMessage(2)
    model.groupMode.markAsUnreadSelectedMessages()
    messages = model.messageListDisplay.getMessageList(2)
    assert.strictEqual(messages[0].read, false)
    assert.strictEqual(messages[1].read, false)
    assert.strictEqual(model.groupMode.getSelectedMessages().size, 0)
    done()
  })
  it('should deselect some messages after select', (done) => {
    const model = MockMailboxProvider.exampleOneAccount().takeAppModel()
    assert.strictEqual(model.groupMode.getSelectedMessages().size, 0)
    model.groupMode.initialMessageSelect(0)
    model.groupMode.selectMessage(1)
    model.groupMode.selectMessage(2)
    assert.strictEqual(model.groupMode.getSelectedMessages().size, 3)
    model.groupMode.unselectMessage(1)
    assert.strictEqual(model.groupMode.getSelectedMessages().size, 2)
    model.groupMode.unselectMessage(0)
    assert.strictEqual(model.groupMode.getSelectedMessages().size, 1)
    assert.deepStrictEqual(model.groupMode.getSelectedMessages(), new Set<Int32>([2]))
    done()
  })
  it('should deselect all messages after select', (done) => {
    const model = MockMailboxProvider.exampleOneAccount().takeAppModel()
    assert.strictEqual(model.groupMode.getSelectedMessages().size, 0)
    model.groupMode.initialMessageSelect(0)
    model.groupMode.selectMessage(1)
    model.groupMode.selectMessage(2)
    assert.strictEqual(model.groupMode.getSelectedMessages().size, 3)
    model.groupMode.unselectAllMessages()
    assert.strictEqual(model.groupMode.getSelectedMessages().size, 0)
    done()
  })
})
