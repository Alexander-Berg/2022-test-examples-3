import { castToAny } from '../../../ys/ys';
import { MailboxClientHandler } from '../../client/mailbox-client';
import { App, FeatureID, FeatureRegistry } from '../../mbt/mbt-abstractions';
import { reduced } from '../../utils/utils';
import {
  ArchiveMessageFeature,
  ComposeMessageFeature,
  CreatableFolderFeature,
  DeleteMessageFeature,
  ExpandableThreadsFeature,
  FolderNavigatorFeature,
  MarkableImportantFeature,
  MarkableReadFeature,
  MessageListDisplayFeature,
  MovableToFolderFeature,
  MultiAccountFeature,
  SpamableFeature,
  WriteMessageFeature,
  YandexLoginFeature,
} from '../mail-features';
import { Message } from '../model/mail-model';
import { ArchiveMessageBackend } from './archive-message-backend';
import { ComposeMessageBackend } from './compose-message-backend';
import { CreatableFolderBackend } from './creatable-folder-backend';
import { DeleteMessageBackend } from './delete-message-backend';
import { ExpandableThreadsBackend } from './expandable-threads-backend';
import { FolderNavigatorBackend } from './folder-navigator-backend';
import { MarkableImportantBackend } from './labeled-backend';
import { MarkableBackend } from './markable-backend';
import { MessageListDisplayBackend } from './message-list-display-backend';
import { MovableToFolderBackend } from './movable-to-folder-backend';
import { MultiAccountBackend } from './multi-account-backend';
import { SpamableBackend } from './spamable-backend';
import { WriteMessageBackend } from './write-message-backend';
import { YandexLoginBackend } from './yandex-login-backend';

export class MobileMailBackend implements App {

  public static allSupportedFeatures: FeatureID[] = [
    ArchiveMessageFeature.get.name,
    MessageListDisplayFeature.get.name,
    MarkableReadFeature.get.name,
    MarkableImportantFeature.get.name,
    ExpandableThreadsFeature.get.name,
    WriteMessageFeature.get.name,
    DeleteMessageFeature.get.name,
    SpamableFeature.get.name,
    MovableToFolderFeature.get.name,
    CreatableFolderFeature.get.name,
    FolderNavigatorFeature.get.name,
    YandexLoginFeature.get.name,
    MultiAccountFeature.get.name,
  ]

  public supportedFeatures: FeatureID[] = MobileMailBackend.allSupportedFeatures

  public archive: ArchiveMessageBackend
  public messageListDisplay: MessageListDisplayBackend
  public folderNavigator: FolderNavigatorBackend
  public markable: MarkableBackend
  public markableImportant: MarkableImportantBackend
  public writeMessage: WriteMessageBackend
  public deleteMessage: DeleteMessageBackend
  public composeMessage: ComposeMessageBackend
  public spamable: SpamableBackend
  public movableToFolder: MovableToFolderBackend
  public creatableFolder: CreatableFolderBackend
  public expandableThreads: ExpandableThreadsBackend
  public yandexLogin: YandexLoginBackend
  public multiAccount: MultiAccountBackend

  private cache: Map<string, any> = new Map();

  constructor(readonly clientsHandler: MailboxClientHandler) {
    this.messageListDisplay = new MessageListDisplayBackend(clientsHandler)
    this.folderNavigator = new FolderNavigatorBackend(this.messageListDisplay, clientsHandler)
    this.markable = new MarkableBackend(this.messageListDisplay, clientsHandler)
    this.markableImportant = new MarkableImportantBackend(this.messageListDisplay, clientsHandler)
    this.writeMessage = new WriteMessageBackend(this.messageListDisplay, clientsHandler)
    this.deleteMessage  = new DeleteMessageBackend(this.messageListDisplay, clientsHandler)
    this.composeMessage = new ComposeMessageBackend(clientsHandler)
    this.spamable = new SpamableBackend(this.messageListDisplay, clientsHandler)
    this.movableToFolder = new MovableToFolderBackend(this.messageListDisplay, clientsHandler)
    this.creatableFolder = new CreatableFolderBackend(clientsHandler)
    this.expandableThreads = new ExpandableThreadsBackend(this.messageListDisplay, clientsHandler)
    this.yandexLogin = new YandexLoginBackend(clientsHandler)
    this.archive = new ArchiveMessageBackend(this.messageListDisplay, clientsHandler)
    this.multiAccount = new MultiAccountBackend(clientsHandler)
  }

  public getFeature(feature: FeatureID): any {
    return new FeatureRegistry()
      .register(MessageListDisplayFeature.get, this.messageListDisplay)
      .register(MarkableReadFeature.get, this.markable)
      .register(MarkableImportantFeature.get, this.markableImportant)
      .register(ExpandableThreadsFeature.get, this.expandableThreads)
      .register(WriteMessageFeature.get, this.writeMessage)
      .register(DeleteMessageFeature.get, this.deleteMessage)
      .register(ComposeMessageFeature.get, this.composeMessage)
      .register(SpamableFeature.get, this.spamable)
      .register(FolderNavigatorFeature.get, this.folderNavigator)
      .register(MovableToFolderFeature.get, this.movableToFolder)
      .register(CreatableFolderFeature.get, this.creatableFolder)
      .register(YandexLoginFeature.get, this.yandexLogin)
      .register(ArchiveMessageFeature.get, this.archive)
      .register(MultiAccountFeature.get, this.multiAccount)
      .get(feature)
  }

  public dump(): string {
    let s = `${this.messageListDisplay.getCurrentFolder().name}\n`;
    const threads = this.messageListDisplay.getMessageDTOList(3)
    for (const thread of threads) {
      const threadSelector = thread.threadCount !== null ? `${thread.threadCount!}v` : '';
      s += `${reduced(thread.mid)} ${thread.sender}\t${thread.unread ? '*' : 'o'}\t${thread.subjectText}\t${threadSelector}\t${thread.timestamp}\n`;
      const threadSize = Message.fromMeta(thread).threadCounter;
      if (threadSize !== null) {
        for (const message of this.clientsHandler.getCurrentClient().getMessagesInThread(thread.tid!, threadSize)) {
          s += `\t\t${reduced(message.mid)} ${message.sender}\t${message.unread ? '*' : 'o'}\t${message.subjectText}\t${message.fid}\t${thread.timestamp}\n`;
        }
      }
    }
    return s;
  }

  private getCached<T>(key: string, f: () => T): T {
    if (this.cache.has(key)) {
      return this.cache.get(key) as T;
    }
    const gotValue = f();
    this.cache.set(key, castToAny(gotValue));
    return gotValue;
  }
}
