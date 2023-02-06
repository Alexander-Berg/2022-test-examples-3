import { Int32, Nullable } from '../../../ys/ys';
import { FolderDTO, FolderType, isFolderOfThreadedType } from '../../client/folder/folderDTO';
import { MailboxClient, MailboxClientHandler } from '../../client/mailbox-client';
import { MessageMeta } from '../../client/message/message-meta';
import { MessageListDisplay, MessageView } from '../mail-features';
import { FolderId, Message } from '../model/mail-model';

export class MessageListDisplayBackend implements MessageListDisplay {
  private currentFolderId: Nullable<FolderId> = null

  constructor(private clientsHandler: MailboxClientHandler) {
    // this.currentFolderId = MessageListDisplayBackend.getInbox(this.clientsHandler.currentClient!).fid
  }

  private static getInbox(client: MailboxClient): FolderDTO {
    return MessageListDisplayBackend.getFolderByType(client, FolderType.inbox)
  }

  private static getFolderByType(client: MailboxClient, type: FolderType): FolderDTO {
    return client.getFolderList().filter((f) => f.type === type)[0]
  }

  public getCurrentFolderId(): FolderId {
    if (this.currentFolderId === null) {
      this.currentFolderId = MessageListDisplayBackend.getInbox(this.clientsHandler.getCurrentClient()).fid
    }

    return this.currentFolderId!
  }

  public setCurrentFolderId(folderId: FolderId): void {
    this.currentFolderId = folderId
  }

  public getMessageList(limit: Int32): MessageView[] {
    return this.getMessageDTOList(limit).map((meta) => Message.fromMeta(meta))
  }

  public getMessageDTOList(limit: Int32): MessageMeta[] {
    return this.isInThreadMode() ?
      this.clientsHandler.getCurrentClient().getThreadsInFolder(this.getCurrentFolderId(), limit)
      : this.clientsHandler.getCurrentClient().getMessagesInFolder(this.getCurrentFolderId(), limit)
  }

  public unreadCounter(): Int32 {
    return this.getCurrentFolder().unreadCounter;
  }

  public refreshMessageList(): void {
    return
  }

  public getThreadMessage(byOrder: Int32): MessageMeta {
    const threads = this.getMessageDTOList(byOrder + 1)
    const threadsCount = threads.length
    if (threadsCount <= byOrder) {
      throw new Error(`No thread in folder ${this.getCurrentFolderId()} by order ${byOrder}, there are ${threadsCount} threads`)
    }
    return threads[byOrder]
  }

  public getFolder(id: FolderId): FolderDTO {
    return this.clientsHandler.getCurrentClient().getFolderList().filter((f) => f.fid === id)[0]
  }

  public getCurrentFolder(): FolderDTO {
    return this.getFolder(this.getCurrentFolderId())
  }

  public getFolderByType(type: FolderType): FolderDTO {
    return MessageListDisplayBackend.getFolderByType(this.clientsHandler.getCurrentClient(), type)
  }

  public getInbox(): FolderDTO {
    return this.getFolderByType(FolderType.inbox)
  }

  public isInThreadMode(): boolean {
    const folderType = this.getCurrentFolder().type
    return isFolderOfThreadedType(folderType)
  }

  public goToAccountSwitcher(): void {
  }
}
