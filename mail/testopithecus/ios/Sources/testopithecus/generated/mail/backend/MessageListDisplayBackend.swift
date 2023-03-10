// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/backend/message-list-display-backend.ts >>>

import Foundation

open class MessageListDisplayBackend: MessageListDisplay {
  private var currentFolderId: FolderId!
  private var clientsHandler: MailboxClientHandler
  public init(_ clientsHandler: MailboxClientHandler) {
    self.clientsHandler = clientsHandler
  }

  @discardableResult
  private class func getInbox(_ client: MailboxClient) -> FolderDTO {
    return MessageListDisplayBackend.getFolderByType(client, FolderType.inbox)
  }

  @discardableResult
  private class func getFolderByType(_ client: MailboxClient, _ type: FolderType) -> FolderDTO {
    return client.getFolderList().filter {
      f in
      f.type == type
    }[0]
  }

  @discardableResult
  open func getCurrentFolderId() -> FolderId {
    if currentFolderId == nil {
      currentFolderId = MessageListDisplayBackend.getInbox(clientsHandler.getCurrentClient()).fid
    }
    return currentFolderId!
  }

  open func setCurrentFolderId(_ folderId: FolderId) {
    currentFolderId = folderId
  }

  @discardableResult
  open func getMessageList(_ limit: Int32) -> YSArray<MessageView> {
    return getMessageDTOList(limit).map {
      meta in
      Message.fromMeta(meta)
    }
  }

  @discardableResult
  open func getMessageDTOList(_ limit: Int32) -> YSArray<MessageMeta> {
    return isInThreadMode() ? clientsHandler.getCurrentClient().getThreadsInFolder(getCurrentFolderId(), limit) : clientsHandler.getCurrentClient().getMessagesInFolder(getCurrentFolderId(), limit)
  }

  @discardableResult
  open func unreadCounter() -> Int32 {
    return getCurrentFolder().unreadCounter
  }

  open func refreshMessageList() {
    return
  }

  @discardableResult
  open func getThreadMessage(_ byOrder: Int32) -> MessageMeta {
    let threads = getMessageDTOList(byOrder + 1)
    let threadsCount = threads.length
    if threadsCount <= byOrder {
      fatalError("No thread in folder \(getCurrentFolderId()) by order \(byOrder), there are \(threadsCount) threads")
    }
    return threads[byOrder]
  }

  @discardableResult
  open func getFolder(_ id: FolderId) -> FolderDTO {
    return clientsHandler.getCurrentClient().getFolderList().filter {
      f in
      f.fid == id
    }[0]
  }

  @discardableResult
  open func getCurrentFolder() -> FolderDTO {
    return getFolder(getCurrentFolderId())
  }

  @discardableResult
  open func getFolderByType(_ type: FolderType) -> FolderDTO {
    return MessageListDisplayBackend.getFolderByType(clientsHandler.getCurrentClient(), type)
  }

  @discardableResult
  open func getInbox() -> FolderDTO {
    return getFolderByType(FolderType.inbox)
  }

  @discardableResult
  open func isInThreadMode() -> Bool {
    let folderType = getCurrentFolder().type
    return isFolderOfThreadedType(folderType)
  }

  open func goToAccountSwitcher() {}
}
