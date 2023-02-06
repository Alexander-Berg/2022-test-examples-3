import { Int32, Nullable } from '../../ys/ys'
import { Feature } from '../mbt/mbt-abstractions'
import { UserAccount } from '../users/user-pool'
import { Folder, FolderName, MessageId } from './model/mail-model'

export class MessageListDisplayFeature extends Feature<MessageListDisplay> {
  public static get: MessageListDisplayFeature = new MessageListDisplayFeature()

  private constructor() {
    super('MessageListDisplay')
  }
}

// TODO: разделить на действия и условия (ModelFeature и Feature)
export interface MessageListDisplay {
  /**
   * Этот метод должен возвращать список писем в папке, которые сейчас видит пользователь.
   * То есть, например, если мы находимся в тредовальном режиме, то мы вернем список первых писем из тредов
   * Если мы находимся не в тредовальном режиме, то мы вернем просто список писем в папке
   *
   * @param limit - сколько писем надо вернуть
   */
  getMessageList(limit: Int32): MessageView[]

  refreshMessageList(): void

  unreadCounter(): Int32

  goToAccountSwitcher(): void
}

export interface AccountSettingsNavigator {
  openAccountSettings(accountIndex: Int32): void

  closeAccountSettings(): void
}

export class AccountSettingsNavigatorFeature extends Feature<AccountSettingsNavigator> {
  public static get: AccountSettingsNavigatorFeature = new AccountSettingsNavigatorFeature()

  constructor() {
    super('AccountSettingsNavigator')
  }
}

// todo component accountsettingscomponent + new action

export class GroupBySubjectFeature extends Feature<GroupBySubject> {
  public static get: GroupBySubjectFeature = new GroupBySubjectFeature()

  private constructor() {
    super('GroupBySubject')
  }
}

export interface GroupBySubject {
  getThreadingSetting(): boolean

  toggleThreadingSetting(): void
}

export class FolderNavigatorFeature extends Feature<FolderNavigator> {
  public static get: FolderNavigatorFeature = new FolderNavigatorFeature()

  private constructor() {
    super('FolderNavigator')
  }
}

export interface FolderNavigator {
  getFoldersList(): Folder []

  goToFolder(folderDisplayName: string): void

  getCurrentFolder(): Folder
}

export class CreatableFolderFeature extends Feature<CreatableFolder> {
  public static get: CreatableFolderFeature = new CreatableFolderFeature()

  private constructor() {
    super('CreatableFolder')
  }
}

export interface CreatableFolder {
  createFolder(folderDisplayName: string): void
}

export class MovableToFolderFeature extends Feature<MovableToFolder> {
  public static get: MovableToFolderFeature = new MovableToFolderFeature()

  private constructor() {
    super('MovableToFolder')
  }
}

export interface MovableToFolder {
  moveMessageToFolder(order: Int32, folderName: string): void
}

export class MarkableReadFeature extends Feature<MarkableRead> {
  public static get: MarkableReadFeature = new MarkableReadFeature()

  private constructor() {
    super(
      'MarkableRead',
      `Дефолтная фича удаления сообщения.` +
      `В мобильных реализуется через full swipe, в Лизе через тулбар`,
    )
  }
}

export interface MarkableRead {
  markAsRead(order: Int32): void

  markAsUnread(order: Int32): void
}

export class ArchiveMessageFeature extends Feature<ArchiveMessage> {
  public static get: ArchiveMessageFeature = new ArchiveMessageFeature()

  private constructor() {
    super('ArchiveMessage')
  }
}

export interface ArchiveMessage {
  archiveMessage(order: Int32): void

  toastShown(): boolean
}

export class MarkableImportantFeature extends Feature<MarkableImportant> {
  public static get: MarkableImportantFeature = new MarkableImportantFeature()

  private constructor() {
    super('MarkableImportant')
  }
}

export interface MarkableImportant {
  markAsImportant(order: Int32): void

  markAsUnimportant(order: Int32): void
}

export class MessageNavigatorFeature extends Feature<MessageNavigator> {
  public static get: MessageNavigatorFeature = new MessageNavigatorFeature()

  private constructor() {
    super('MessageNavigator')
  }
}

export interface MessageNavigator {
  openMessage(order: Int32): void

  closeMessage(): void

  deleteCurrentMessage(): void

  getOpenedMessage(): FullMessageView
}

export class GroupModeFeature extends Feature<GroupMode> {
  public static get: GroupModeFeature = new GroupModeFeature()

  private constructor() {
    super(
      'GroupMode',
      `Действия с письмами в режиме групповых операций.` +
      `InitialSelectMessage переводит в компонент GroupMode и производится по лонг тапу.` +
      `SelectMessage выделяет письма, если мы уже в режиме групповых операций по обычному тапу`,
  )
  }
}

export interface GroupMode {
  isInGroupMode(): boolean

  selectMessage(byOrder: Int32): void

  initialMessageSelect(byOrder: Int32): void

  getSelectedMessages(): Set<Int32>

  markAsReadSelectedMessages(): void

  markAsUnreadSelectedMessages(): void

  deleteSelectedMessages(): void

  markAsImportantSelectedMessages(): void

  markAsUnImportantSelectedMessages(): void

  markAsSpamSelectedMessages(): void

  markAsNotSpamSelectedMessages(): void

  moveToFolderSelectedMessages(folderName: FolderName): void

  archiveSelectedMessages(): void

  unselectMessage(byOrder: Int32): void

  unselectAllMessages(): void
}

export class SettingsFeature extends Feature<Settings> {
  public static get: SettingsFeature = new SettingsFeature()

  private constructor() {
    super('Settings')
  }
}

export interface Settings {
  clearCache(): void
}

export class SettingsNavigatorFeature extends Feature<SettingsNavigator> {
  public static get: SettingsNavigatorFeature = new SettingsNavigatorFeature()

  private constructor() {
    super('SettingsNavigator')
  }
}

export interface SettingsNavigator {
  openSettings(): void
}

export class RotatableFeature extends Feature<Rotatable> {
  public static get: RotatableFeature = new RotatableFeature()

  private constructor() {
    super('Rotatable')
  }
}

export interface Rotatable {
  isInLandscape(): boolean

  rotateToLandscape(): void

  rotateToPortrait(): void
}

export class ExpandableThreadsModelFeature extends Feature<ReadOnlyExpandableThreads> {
  public static get: ExpandableThreadsModelFeature = new ExpandableThreadsModelFeature()

  private constructor() {
    super('ReadOnlyExpandableThreads')
  }
}

export interface ReadOnlyExpandableThreads {
  isExpanded(threadOrder: Int32): boolean

  isRead(threadOrder: Int32, messageOrder: Int32): boolean

  getMessagesInThread(threadOrder: Int32): MessageView[]
}

export class ExpandableThreadsFeature extends Feature<ExpandableThreads> {
  public static get: ExpandableThreadsFeature = new ExpandableThreadsFeature()

  private constructor() {
    super('ExpandableThreads')
  }
}

export interface ExpandableThreads {
  markThreadMessageAsRead(threadOrder: Int32, messageOrder: Int32): void

  markThreadMessageAsUnRead(threadOrder: Int32, messageOrder: Int32): void

  expandThread(order: Int32): void

  collapseThread(order: Int32): void
}

export class DeleteMessageFeature extends Feature<DeleteMessage> {
  public static get: DeleteMessageFeature = new DeleteMessageFeature()

  private constructor() {
    super(
      'DeleteMessage',
      `Дефолтная фича удаления сообщения.` +
      `В мобильных реализуется через full swipe, в Лизе через тулбар`,
    )
  }
}

export interface DeleteMessage {
  deleteMessage(order: Int32): void
}

export class ContextMenuFeature extends Feature<ContextMenu> {
  public static get: ContextMenuFeature = new ContextMenuFeature()

  private constructor() {
    super('ContextMenu')
  }
}

export interface ContextMenu {
  deleteMessageFromContextMenu(order: Int32): void

  markAsReadFromContextMenu(order: Int32): void

  markAsUnreadFromContextMenu(order: Int32): void

  markAsImportantFromContextMenu(order: Int32): void

  markAsUnImportantFromContextMenu(order: Int32): void

  moveToFolderFromContextMenu(order: Int32, folderName: string): void
}

export class ShortSwipeDeleteFeature extends Feature<ShortSwipeDelete> {
  public static get: ShortSwipeDeleteFeature = new ShortSwipeDeleteFeature()

  private constructor() {
    super('ShortSwipeDelete')
  }
}

export interface ShortSwipeDelete {
  deleteMessageByShortSwipe(order: Int32): void

  toastShown(): boolean
}

export class WriteMessageFeature extends Feature<WriteMessage> {
  public static get: WriteMessageFeature = new WriteMessageFeature()

  private constructor() {
    super('WriteMessage')
  }
}

export interface WriteMessage {
  openCompose(): void

  sendMessage(to: string, subject: string): void // TODO: remove

  replyMessage(): void

  sendPrepared(): void
}

export class ComposeMessageFeature extends Feature<ComposeMessage> {
  public static get: ComposeMessageFeature = new ComposeMessageFeature()

  private constructor() {
    super('SendMessage')
  }
}

export interface ComposeMessage {
  goToMessageReply(): void

  addTo(to: string): void

  addToUsingSuggest(to: string): void

  removeTo(order: number): void

  setSubject(subject: string): void

  clearSubject(): void

  setBody(body: string): void

  clearBody(): void

  getTo(): Set<string>

  getDraft(): DraftView

}

export class WysiwygFeature extends Feature<WYSIWIG> {
  public static get: WysiwygFeature = new WysiwygFeature()

  private constructor() {
    super('WYSIWYG')
  }
}

export interface WYSIWIG {
  setStrong(from: Int32, to: Int32): void

  setItalic(from: Int32, to: Int32): void

  clearFormatting(from: Int32, to: Int32): void

  appendText(index: Int32, text: string): void
}

export class SpamableFeature extends Feature<Spamable> {
  public static get: SpamableFeature = new SpamableFeature()

  private constructor() {
    super('Spamable')
  }
}

export interface Spamable {
  moveToSpam(order: Int32): void

  moveFromSpam(order: Int32): void

  toastShown(): boolean
}

export class MultiAccountFeature extends Feature<MultiAccount> {
  public static get: MultiAccountFeature = new MultiAccountFeature()

  private constructor() {
    super('MultiAccount')
  }
}

export type Login = string

export interface MultiAccount {
  switchToAccount(login: string): void

  addNewAccount(): void

  getLoggedInAccountsList(): Login []
}

export class YandexLoginFeature extends Feature<YandexLogin> {
  public static get: YandexLoginFeature = new YandexLoginFeature()

  private constructor() {
    super('YandexLogin')
  }
}

// В вебе процесс авторизации первого пользователя и процесс мультиавторизации несколько отличаются.
// Поэтому в интерфейсе есть два метода.
export interface YandexLogin {
  loginWithYandexAccount(account: UserAccount): void
}

export class MailRuLoginFeature extends Feature<MailRuLogin> {
  public static get: MailRuLoginFeature = new MailRuLoginFeature()

  private constructor() {
    super('MailRuLogin')
  }
}

export interface MailRuLogin {
  loginWithMailRuAccount(account: UserAccount): void
}

export class GoogleLoginFeature extends Feature<GoogleLogin> {
  public static get: GoogleLoginFeature = new GoogleLoginFeature()

  private constructor() {
    super('GoogleLogin')
  }
}

export interface GoogleLogin {
  loginWithGoogleAccount(account: UserAccount): void
}

export class OutlookLoginFeature extends Feature<OutlookLogin> {
  public static get: OutlookLoginFeature = new OutlookLoginFeature()

  private constructor() {
    super('OutlookLogin')
  }
}

export interface OutlookLogin {
  loginWithOutlookAccount(account: UserAccount): void
}

export class HotmailLoginFeature extends Feature<HotmailLogin> {
  public static get: HotmailLoginFeature = new HotmailLoginFeature()

  private constructor() {
    super('HotmailLogin')
  }
}

export interface HotmailLogin {
  loginWithHotmailAccount(account: UserAccount): void
}

export class RamblerLoginFeature extends Feature<RamblerLogin> {
  public static get: RamblerLoginFeature = new RamblerLoginFeature()

  private constructor() {
    super('RamblerLogin')
  }
}

export interface RamblerLogin {
  loginWithRamblerAccount(account: UserAccount): void
}

export class YahooLoginFeature extends Feature<YahooLogin> {
  public static get: YahooLoginFeature = new YahooLoginFeature()

  private constructor() {
    super('YahooLogin')
  }
}

export interface YahooLogin {
  loginWithYahooAccount(account: UserAccount): void
}

export class CustomMailServiceLoginFeature extends Feature<CustomMailServiceLogin> {
  public static get: CustomMailServiceLoginFeature = new CustomMailServiceLoginFeature()

  private constructor() {
    super('CustomMailService')
  }
}

export interface CustomMailServiceLogin {
  loginWithCustomMailServiceAccount(account: UserAccount): void
}

export interface MessageView {
  readonly from: string
  readonly subject: string
  readonly read: boolean
  readonly important: boolean
  readonly threadCounter: Nullable<Int32>

  tostring(): string
}

export interface FullMessageView {
  readonly head: MessageView
  readonly to: Set<string>
  readonly body: string

  tostring(): string
}

export interface DraftView {
  to: Set<string>
  subject: Nullable<string>

  getWysiwyg(): WysiwygView

  tostring(): string
}

export interface WysiwygView {
  getText(): string

  getStyles(i: Int32): Set<string>

  getRichBody(): string
}
