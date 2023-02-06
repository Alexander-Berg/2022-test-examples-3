import { Int32, int64ToInt32, Nullable, range, setToArray, undefinedToNull } from '../../../ys/ys'
import { valuesArray } from '../../utils/utils'
import { GroupBySubject, MessageListDisplay, MessageView } from '../mail-features'
import { DefaultFolderName, Folder, FolderName, FullMessage, MailAppModelHandler, MessageId } from './mail-model'

export class MessageListDisplayModel implements MessageListDisplay {
  private currentFolder: Folder = new Folder(DefaultFolderName.inbox)

  constructor(public accountDataHandler: MailAppModelHandler, private  groupBySubjectModel: GroupBySubject) {
  }

  public getCurrentFolder(): Folder {
    if (this.currentFolder === null) {
      this.currentFolder = new Folder(DefaultFolderName.inbox)
    }
    return this.currentFolder
  }

  public setCurrentFolder(folder: Folder): void {
    this.currentFolder = folder
  }

  public getMessageList(limit: Int32): MessageView[] {
    return this.getMessageIdList(limit).map((mid) =>
      this.isInThreadMode() ? this.makeMessageThreadView(mid).head : this.storedMessage(mid).head)
  }

  public getFolderList(): Folder[] {
    const folders = new Map<string, Folder>()
    this.accountDataHandler.getCurrentAccount().folderToMessages
      .forEach((mids, folder) => folders.set(folder, new Folder(folder)))
    return valuesArray(folders)
  }

  public refreshMessageList(): void {
    return
  }

  public unreadCounter(): Int32 {
    throw new Error('Not implemented')
    // let unreadCounter = 0;
    // // const mids = this.getMessageIdList(folder, this.messages.size);
    // for (const mid of this.messageToFolder.keys()) {
    //   const folder = this.messageToFolder.get(mid);
    //   if (folder != null && folder.name === folderView.name) {
    //     const message = this.messages.get(mid);
    //     if (message != null && !message.read) {
    //       unreadCounter++;
    //     }
    //   }
    // }
    // return unreadCounter;
  }

  public getMessageIdList(limit: Int32): MessageId[] {
    const folderMessages: MessageId[] = []
    this.accountDataHandler.getCurrentAccount().folderToMessages.forEach((mids, messageFolder) => {
      if (this.getCurrentFolder().name === messageFolder) {
        for (const mid of mids.values()) {
          folderMessages.push(mid)
        }
      }
    })
    folderMessages.sort((mid1, mid2) => {
      const diff = int64ToInt32(this.makeMessageThreadView(mid2).mutableHead.timestamp -
        this.makeMessageThreadView(mid1).mutableHead.timestamp)
      if (diff !== 0) {
        return diff
      }
      return int64ToInt32(mid1 - mid2)
    })
    if (!this.isInThreadMode()) {
      return folderMessages.slice(0, limit)
    }
    const filteredFolderMessages: MessageId[] = []
    const currentAddedThreads = new Set<Int32>()
    for (const mid of folderMessages) {
      const threadOrder = this.findThread(mid)
      if (threadOrder === null) {
        filteredFolderMessages.push(mid)
      } else if (!currentAddedThreads.has(threadOrder)) {
        filteredFolderMessages.push(mid)
        currentAddedThreads.add(threadOrder)
      }
    }
    return filteredFolderMessages.slice(0, limit)
  }

  public makeMessageThreadView(threadMid: MessageId): FullMessage {
    const threadView = this.storedMessage(threadMid).copy()
    threadView.mutableHead.read = this.getMessagesInThreadByMid(threadMid)
      .filter((mid) => !this.storedMessage(mid).head.read)
      .length === 0
    return threadView
  }

  public getMessageId(order: Int32): MessageId {
    const messageIds = this.getMessageIdList(order + 1)
    if (order >= messageIds.length) {
      throw new Error(`No message with order ${order}`)
    }
    return messageIds[order]
  }

  public isInThreadMode(): boolean {
    const notThreadableFolders: string[] = [DefaultFolderName.outgoing, DefaultFolderName.draft, DefaultFolderName.trash, DefaultFolderName.spam, DefaultFolderName.archive]
    if (notThreadableFolders.includes(this.getCurrentFolder().name)) {
      return false
    }
    return this.accountDataHandler.getCurrentAccount().accountSettings.groupBySubject
  }

  public toggleThreadMode(): void {
    this.groupBySubjectModel.toggleThreadingSetting()
  }

  public removeMessage(id: MessageId): void {
    if (!this.accountDataHandler.getCurrentAccount().messages.has(id)) {
      throw new Error('No messages with target id')
    }

    // remove message permanently or move to trash folder
    const isInTrash = this.demandFolderMessages(DefaultFolderName.trash).has(id)
    if (isInTrash) {
      this.accountDataHandler.getCurrentAccount().folderToMessages.forEach((msgIds, folderName) => msgIds.delete(id))
      this.accountDataHandler.getCurrentAccount().messages.delete(id)
      for (const index of range(0, this.accountDataHandler.getCurrentAccount().threads.length)) {
        this.accountDataHandler.getCurrentAccount().threads[index].delete(id)
      }
      this.accountDataHandler.getCurrentAccount().threads =
        this.accountDataHandler.getCurrentAccount().threads.filter((thread) => thread.size !== 0)
    } else {
      this.moveMessageToFolder(id, DefaultFolderName.trash)
    }
  }

  public findThread(mid: MessageId): Nullable<Int32> {
    for (const i of range(0, this.accountDataHandler.getCurrentAccount().threads.length)) {
      if (this.accountDataHandler.getCurrentAccount().threads[i].has(mid)) {
        return i
      }
    }
    return null
  }

  public storedMessage(mid: MessageId): FullMessage {
    const message = undefinedToNull(this.accountDataHandler.getCurrentAccount().messages.get(mid))
    if (message === null) {
      throw new Error(`No message with mid ${mid} in model!`)
    }
    return message!
  }

  public getMessageThreadByOrder(order: Int32): MessageId[] {
    const firstMsgMid = this.getMessageId(order)
    return this.getMessagesInThreadByMid(firstMsgMid)
  }

  public getMessagesInThreadByMid(mid: MessageId): MessageId[] {
    const orderInThreads = this.findThread(mid)
    if (orderInThreads === null) {
      return [mid]
    }
    const threadMids = this.accountDataHandler.getCurrentAccount().threads[orderInThreads]
    const sortedMids = setToArray(threadMids)
    sortedMids.sort((m1, m2) => {
      return int64ToInt32(this.storedMessage(m2).mutableHead.timestamp - this.storedMessage(m1).mutableHead.timestamp)
    })
    return sortedMids
  }

  public storedFolder(mid: MessageId): Folder {
    let folderName: Nullable<FolderName> = null
    this.accountDataHandler.getCurrentAccount().folderToMessages.forEach((msgIds, folder) => {
      for (const msgId of msgIds.values()) {
        if (msgId === mid) {
          folderName = folder
        }
      }
    })

    if (folderName === null) {
      throw new Error(`No folder for message with mid ${mid} in model!`)
    }
    return new Folder(folderName!)
  }

  public moveMessageToFolder(mid: MessageId, folderName: FolderName): void {
    this.accountDataHandler.getCurrentAccount().folderToMessages.forEach((msgIds, folder) => msgIds.delete(mid))
    this.demandFolderMessages(folderName).add(mid)
  }

  public goToAccountSwitcher(): void {
  }

  public getMidsByOrders(orders: Set<Int32>): MessageId[] {
    const mids: MessageId[] = []
    for (const order of orders.values()) {
      for (const mid of this.getMessageThreadByOrder(order)) {
        mids.push(mid)
      }
    }
    return mids
  }

  private demandFolderMessages(folderName: FolderName): Set<MessageId> {
    const messages = undefinedToNull(this.accountDataHandler.getCurrentAccount().folderToMessages.get(folderName))
    if (messages === null) {
      throw new Error(`Модель не знает про папку '${folderName}'! Сначала ее надо создать.`)
    }
    return messages!
  }
}
