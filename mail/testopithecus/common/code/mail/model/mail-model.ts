import { Int32, Int64, Nullable, range, stringToInt32 } from '../../../ys/ys'
import { ID } from '../../client/common/id'
import { FolderDTO } from '../../client/folder/folderDTO'
import { MessageMeta } from '../../client/message/message-meta'
import { FeatureID, FeatureRegistry } from '../../mbt/mbt-abstractions'
import { AppModel } from '../../mbt/walk/fixed-scenario-strategy'
import { AccountsManager } from '../../users/accounts-manager'
import { UserAccount } from '../../users/user-pool'
import { copyArray, copySet, reduced } from '../../utils/utils'
import {
  AccountSettingsNavigatorFeature,
  ArchiveMessageFeature,
  ComposeMessageFeature,
  ContextMenuFeature,
  CreatableFolderFeature,
  DeleteMessageFeature,
  ExpandableThreadsFeature,
  ExpandableThreadsModelFeature,
  FolderNavigatorFeature,
  FullMessageView, GroupBySubjectFeature,
  GroupModeFeature,
  MarkableImportantFeature,
  MarkableReadFeature,
  MessageListDisplayFeature,
  MessageNavigatorFeature,
  MessageView,
  MovableToFolderFeature,
  MultiAccountFeature,
  RotatableFeature,
  SettingsFeature,
  SettingsNavigatorFeature,
  ShortSwipeDeleteFeature,
  SpamableFeature,
  WriteMessageFeature,
  WysiwygFeature,
  YandexLoginFeature,
} from '../mail-features'
import { ArchiveMessageModel } from './archive-message-model'
import { DeleteMessageModel } from './base-models/delete-message-model'
import { MarkableReadModel } from './base-models/markable-read-model'
import { ComposeMessageModel } from './compose-message-model'
import { ContextMenuModel } from './context-menu-model'
import { CreatableFolderModel } from './creatable-folder-model'
import { ExpandableThreadsModel, ReadOnlyExpandableThreadsModel } from './expandable-threads-model'
import { FolderNavigatorModel } from './folder-navigator-model'
import { GroupModeModel } from './group-mode-model'
import { MarkableImportantModel } from './label-model'
import { LoginModel } from './login/login-model'
import { MessageListDisplayModel } from './message-list-display-model'
import { MessageNavigatorModel } from './message-navigator-model'
import { MovableToFolderModel } from './movable-to-folder-model'
import { MultiAccountModel } from './multi-account-model'
import { RotatableModel } from './rotatable-model'
import { SettingsModel } from './settings-model'
import { ShortSwipeDeleteModel } from './short-swipe-delete-model'
import { SpamableModel } from './spamable-model'
import { WriteMessageModel } from './write-message-model'
import { WysiwygModel } from './wysiwyg-model'

export type FolderName = string

export class Folder {
  public readonly name: FolderName

  constructor(name: FolderName) {
    this.name = name
  }

  public static fromDTO(dto: FolderDTO): Folder {
    const name = dto.name
    if (name === null) {
      throw new Error(`Folder with fid ${dto.fid} has no folder!`)
    }
    return new Folder(name)
  }

  public copy(): Folder {
    return new Folder(this.name)
  }

  public tostring(): string {
    return `Folder(${this.name})`
  }
}

export class DefaultFolderName {
  public static inbox: FolderName = 'Inbox'
  public static sent: FolderName = 'Sent'
  public static outgoing: FolderName = 'Outbox'
  public static trash: FolderName = 'Trash'
  public static spam: FolderName = 'Spam'
  public static draft: FolderName = 'Drafts'
  public static archive: FolderName = 'Archive'
}

export function toBackendFolderName(folderDisplayName: string): string {
  // TODO: реализовать, если мы захотим что-то делать на бэке с вложенными папками
  switch (folderDisplayName) {
    case DefaultFolderName.inbox:
      return 'Inbox'
    case DefaultFolderName.sent:
      return 'Sent'
    case DefaultFolderName.outgoing:
      return 'Outbox'
    case DefaultFolderName.trash:
      return 'Trash'
    case DefaultFolderName.spam:
      return 'Spam'
    case DefaultFolderName.draft:
      return 'Drafts'
    case DefaultFolderName.archive:
      return 'Archive'
    default:
      return folderDisplayName
  }
}

export class Message implements MessageView {
  constructor(public from: string,
              public subject: string,
              public timestamp: Int64,
              public threadCounter: Nullable<Int32> = 1,
              public read: boolean = false,
              public important: boolean = false) {
  }

  public static fromMeta(meta: MessageMeta): Message {
    return new Message(meta.sender,
      meta.subjectText,
      meta.timestamp,
      meta.threadCount === null ? null : stringToInt32(meta.threadCount),
      !meta.unread,
    )
  }

  public static matches(first: MessageView, second: MessageView): boolean {
    return first.subject === second.subject && first.read === second.read
  }

  public threadSize(): Int32 {
    const counter = this.threadCounter
    return counter !== null ? counter : 1
  }

  public tostring(): string {
    return `MessageView(from=${this.from}, subject=${this.subject}, read=${this.read}, important=${this.important})`
  }

  public copy(): Message {
    return new Message(this.from, this.subject, this.timestamp, this.threadCounter, this.read, this.important)
  }
}

export class FullMessage implements FullMessageView {
  public readonly head: MessageView
  public mutableHead: Message // sorry, swift is peace of shit

  constructor(head: Message, public to: Set<string> = new Set<string>(), public body: string = '') {
    this.head = head
    this.mutableHead = head
  }

  public static fromMeta(meta: MessageMeta): FullMessage {
    return new FullMessage(Message.fromMeta(meta))
  }

  public static matches(first: FullMessageView, second: FullMessageView): boolean {
    return Message.matches(first.head, second.head)
      && first.to === second.to
      && first.body === second.body
  }

  public tostring(): string {
    return `Message(from=${this.head.from},subject=${this.head.subject}, read=${this.head.read}, to=${this.to},` +
      `body=${this.body})`
  }

  public copy(): FullMessage {
    return new FullMessage(this.mutableHead.copy(), this.to, this.body)
  }
}

export type MessageId = ID
export type FolderId = ID

export class MailAppModelHandler {

  public accountsManager: AccountsManager

  constructor(public accountsData: AccountMailboxData[]) {
    this.accountsManager = new AccountsManager(accountsData.map((data) => data.userAccount))
  }

  public logInToAccount(account: UserAccount): void {
    this.accountsManager.logInToAccount(account)
  }

  public switchToAccount(login: string): void {
    this.accountsManager.switchToAccount(login)
  }

  public isLoggedIn(): boolean {
    return this.accountsManager.isLoggedIn()
  }

  public getCurrentAccount(): AccountMailboxData {
    if (!this.hasCurrentAccount()) {
      throw new Error('Account was requested, but is not set')
    }
    return this.accountsData[this.accountsManager.currentAccount!]
  }

  public getLoggedInAccounts(): UserAccount[] {
    return this.accountsManager.getLoggedInAccounts()
  }

  public hasCurrentAccount(): boolean {
    return this.accountsManager.currentAccount !== null &&
      this.accountsManager.currentAccount! < this.accountsData.length
  }

  public copy(): MailAppModelHandler {
    const accountsDataCopy = this.accountsData.map((acc) => acc.copy())
    const result = new MailAppModelHandler(accountsDataCopy)
    result.accountsManager = this.accountsManager.copy()
    return result
  }
}

export class AccountSettings {
  public constructor(public groupBySubject: boolean) {
  }
}

export class AccountMailboxData {

  public constructor(public readonly userAccount: UserAccount,
                     public messages: Map<MessageId, FullMessage>,
                     public folderToMessages: Map<FolderName, Set<MessageId>>,
                     public threads: Array<Set<MessageId>>,
                     public aliases: string[],
                     public contacts: string[],
                     public accountSettings: AccountSettings) {
  }

  public copy(): AccountMailboxData {
    const userAccountCopy = new UserAccount(this.userAccount.login, this.userAccount.password)
    const accountSettingsCopy = new AccountSettings(this.accountSettings.groupBySubject)
    const messagesCopy = new Map<MessageId, FullMessage>()
    for (const mid of this.messages.keys()) {
      messagesCopy.set(mid, this.messages.get(mid)!.copy())
    }
    const threadsCopy: Array<Set<MessageId>> = []
    this.threads.forEach((thread) => {
      const threadCopy = new Set(thread)
      threadsCopy.push(threadCopy)
    })
    const aliasesCopy = copyArray(this.aliases)
    const contactsCopy = copyArray(this.contacts)
    const folderToMessagesCopy: Map<FolderName, Set<MessageId>> = new Map<FolderName, Set<MessageId>>()
    this.folderToMessages.forEach((mids, folderName) => folderToMessagesCopy.set(folderName, new Set(mids)))
    return new AccountMailboxData(
      userAccountCopy,
      messagesCopy,
      folderToMessagesCopy,
      threadsCopy,
      aliasesCopy,
      contactsCopy,
      accountSettingsCopy,
    )
  }
}

export class MailboxModel implements AppModel {
  public static allSupportedFeatures: FeatureID[] = [
    MessageListDisplayFeature.get.name,
    FolderNavigatorFeature.get.name,
    MessageNavigatorFeature.get.name,
    MarkableReadFeature.get.name,
    MarkableImportantFeature.get.name,
    GroupModeFeature.get.name,
    SettingsFeature.get.name,
    RotatableFeature.get.name,
    ExpandableThreadsModelFeature.get.name,
    ExpandableThreadsFeature.get.name,
    WriteMessageFeature.get.name,
    DeleteMessageFeature.get.name,
    ComposeMessageFeature.get.name,
    SpamableFeature.get.name,
    MovableToFolderFeature.get.name,
    CreatableFolderFeature.get.name,
    WysiwygFeature.get.name,
    ArchiveMessageFeature.get.name,
    YandexLoginFeature.get.name,
    MultiAccountFeature.get.name,
    ContextMenuFeature.get.name,
    ShortSwipeDeleteFeature.get.name,
    GroupBySubjectFeature.get.name,
    SettingsNavigatorFeature.get.name,
    AccountSettingsNavigatorFeature.get.name,
  ]

  public supportedFeatures: FeatureID[] = copyArray(MailboxModel.allSupportedFeatures)

  public readonly messageListDisplay: MessageListDisplayModel
  public readonly markableRead: MarkableReadModel
  public readonly markableImportant: MarkableImportantModel
  public readonly messageNavigator: MessageNavigatorModel
  public readonly deletableMessages: DeleteMessageModel
  public readonly groupMode: GroupModeModel
  public readonly rotatable: RotatableModel
  public readonly readOnlyExpandableThreads: ReadOnlyExpandableThreadsModel
  public readonly expandableThreads: ExpandableThreadsModel
  public readonly writable: WriteMessageModel
  public readonly composable: ComposeMessageModel
  public readonly folderNavigator: FolderNavigatorModel
  public readonly movableToFolder: MovableToFolderModel
  public readonly creatableFolder: CreatableFolderModel
  public readonly wysiwyg: WysiwygModel
  public readonly login: LoginModel
  public readonly multiAccount: MultiAccountModel
  public readonly contextMenu: ContextMenuModel
  public readonly shortSwipeDelete: ShortSwipeDeleteModel
  public readonly archiveMessage: ArchiveMessageModel
  public readonly settingsModel: SettingsModel
  public readonly spammable: SpamableModel

  constructor(public readonly mailAppModelHandler: MailAppModelHandler) {
    const accountsSettings: AccountSettings[] = mailAppModelHandler.accountsData.map((account) => {
      return account.accountSettings
    })

    this.settingsModel = new SettingsModel(accountsSettings, this.mailAppModelHandler.accountsManager)
    this.messageListDisplay = new MessageListDisplayModel(mailAppModelHandler, this.settingsModel)
    this.folderNavigator = new FolderNavigatorModel(this.messageListDisplay)
    this.markableRead = new MarkableReadModel(this.messageListDisplay)
    this.markableImportant = new MarkableImportantModel(this.messageListDisplay)
    this.messageNavigator = new MessageNavigatorModel(this.markableRead, this.messageListDisplay)
    this.deletableMessages = new DeleteMessageModel(this.messageListDisplay)
    this.rotatable = new RotatableModel()
    this.readOnlyExpandableThreads = new ReadOnlyExpandableThreadsModel(this.messageListDisplay)
    this.expandableThreads = new ExpandableThreadsModel(this.readOnlyExpandableThreads, this.messageListDisplay)
    this.wysiwyg = new WysiwygModel()
    this.composable = new ComposeMessageModel(this.wysiwyg, mailAppModelHandler)
    this.writable = new WriteMessageModel(this.messageListDisplay, this.messageNavigator, this.composable, mailAppModelHandler, this.wysiwyg)
    this.movableToFolder = new MovableToFolderModel(this.messageListDisplay)
    this.creatableFolder = new CreatableFolderModel(this.messageListDisplay)
    this.contextMenu = new ContextMenuModel(
      this.deletableMessages,
      this.markableImportant,
      this.markableRead,
      this.movableToFolder,
    )
    this.shortSwipeDelete = new ShortSwipeDeleteModel(this.deletableMessages)
    this.login = new LoginModel(mailAppModelHandler)
    this.multiAccount = new MultiAccountModel(mailAppModelHandler)
    this.archiveMessage = new ArchiveMessageModel(this.messageListDisplay)
    this.spammable = new SpamableModel(this.messageListDisplay, this.markableRead)
    this.groupMode = new GroupModeModel(
      this.markableRead,
      this.deletableMessages,
      this.archiveMessage,
      this.markableImportant,
      this.spammable,
      this.movableToFolder,
    )
  }

  public getFeature(feature: FeatureID): any {
    return new FeatureRegistry()
      .register(MessageListDisplayFeature.get, this.messageListDisplay)
      .register(FolderNavigatorFeature.get, this.folderNavigator)
      .register(MessageNavigatorFeature.get, this.messageNavigator)
      .register(MarkableReadFeature.get, this.markableRead)
      .register(MarkableImportantFeature.get, this.markableImportant)
      .register(GroupModeFeature.get, this.groupMode)
      .register(SettingsFeature.get, this.settingsModel)
      .register(RotatableFeature.get, this.rotatable)
      .register(ExpandableThreadsModelFeature.get, this.readOnlyExpandableThreads)
      .register(ExpandableThreadsFeature.get, this.expandableThreads)
      .register(WriteMessageFeature.get, this.writable)
      .register(DeleteMessageFeature.get, this.deletableMessages)
      .register(ComposeMessageFeature.get, this.composable)
      .register(SpamableFeature.get, this.spammable)
      .register(MovableToFolderFeature.get, this.movableToFolder)
      .register(CreatableFolderFeature.get, this.creatableFolder)
      .register(WysiwygFeature.get, this.wysiwyg)
      .register(YandexLoginFeature.get, this.login)
      .register(ContextMenuFeature.get, this.contextMenu)
      .register(ShortSwipeDeleteFeature.get, this.shortSwipeDelete)
      .register(ArchiveMessageFeature.get, this.archiveMessage)
      .register(MultiAccountFeature.get, this.multiAccount)
      .register(SettingsNavigatorFeature.get, this.settingsModel)
      .register(GroupBySubjectFeature.get, this.settingsModel)
      .register(AccountSettingsNavigatorFeature.get, this.settingsModel)
      .get(feature)
  }

  public copy(): AppModel {
    const accountDataHandlerCopy = this.mailAppModelHandler.copy()
    const model: MailboxModel = new MailboxModel(accountDataHandlerCopy)
    model.supportedFeatures = this.supportedFeatures
    model.messageNavigator.openedMessage = this.messageNavigator.openedMessage
    model.rotatable.landscape = this.rotatable.landscape
    if (this.groupMode.selectedOrders !== null) {
      model.groupMode.selectedOrders = copySet(this.groupMode.selectedOrders)
    }
    for (const id of this.readOnlyExpandableThreads.expanded.values()) {
      model.readOnlyExpandableThreads.expanded.add(id)
    }
    if (this.composable.composeDraft !== null) {
      model.composable.composeDraft = this.composable.composeDraft!.copy()
    }
    return model
  }

  public dump(): string {
    let s = ''
    const threadMids = this.messageListDisplay.getMessageIdList(10)
    s += `${this.messageListDisplay.getCurrentFolder().name}\n`
    for (const i of range(0, threadMids.length)) {
      const threadMid = threadMids[i]
      const thread = this.messageListDisplay.makeMessageThreadView(threadMid)
      const msgHead = thread.mutableHead
      const threadSelector = msgHead.threadCounter !== null ? `${msgHead.threadCounter!}v` : ''
      s += `${reduced(threadMid)} ${msgHead.from}\t${msgHead.read ? 'o' : '*'}` +
        `\t${msgHead.subject}\t${threadSelector}\t${msgHead.timestamp}\n`
      if (msgHead.threadCounter !== null) {
        for (const mid of this.messageListDisplay.getMessagesInThreadByMid(threadMid)) {
          const message = this.messageListDisplay.storedMessage(mid)
          s += `\t\t${reduced(mid)} ${message.head.from}\t${message.head.read ? 'o' : '*'}\t${message.head.subject}` +
            `\t${this.messageListDisplay.storedFolder(mid).name}\t${msgHead.timestamp}\n`
        }
      }
    }
    return s
  }
}
