// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM client/mailbox-client.ts >>>

import Foundation

open class MailboxClient {
  private var platform: Platform
  public let userAccount: UserAccount
  private var oauthToken: String
  private var network: SyncNetwork
  private var jsonSerializer: JSONSerializer
  public var logger: Logger
  public init(_ platform: Platform, _ userAccount: UserAccount, _ oauthToken: String, _ network: SyncNetwork, _ jsonSerializer: JSONSerializer, _ logger: Logger) {
    self.platform = platform
    self.userAccount = userAccount
    self.oauthToken = oauthToken
    self.network = network
    self.jsonSerializer = jsonSerializer
    self.logger = logger
  }

  @discardableResult
  open func getFolderList() -> YSArray<FolderDTO> {
    let request = ContainersRequest(platform, NetworkExtra.mockExtra())
    let jsonArray = getJsonResponse(request) as! ArrayJSONItem
    let folders: YSArray<FolderDTO> = YSArray()
    jsonArray.asArray().forEach {
      folderItem in
      let fid: JSONItem! = (folderItem as! MapJSONItem).get("fid")
      if fid != nil {
        folders.push(folderFromJSONItem(folderItem)!)
      }
    }
    return folders
  }

  @discardableResult
  open func getLabelList() -> YSArray<Label> {
    let request = ContainersRequest(platform, NetworkExtra.mockExtra())
    let jsonArray = getJsonResponse(request) as! ArrayJSONItem
    let labels: YSArray<Label> = YSArray()
    jsonArray.asArray().forEach {
      labelItem in
      let lid: JSONItem! = (labelItem as! MapJSONItem).get("lid")
      if lid != nil {
        labels.push(labelFromJSONItem(labelItem)!)
      }
    }
    return labels
  }

  @discardableResult
  open func getAllContactsList(_ limit: Int32) -> YSArray<Contact> {
    let request = ABookTopRequest(limit, platform, NetworkExtra.mockExtra())
    return getContactsList(request)
  }

  @discardableResult
  open func getMessagesInFolder(_ fid: ID, _ limit: Int32) -> YSArray<MessageMeta> {
    let messageRequestItem = MessageRequestItem.messagesInFolder(fid, 0, limit)
    let request = MessagesRequestPack(YSArray(messageRequestItem), platform, NetworkExtra.mockExtra())
    return getMessagesList(request)
  }

  @discardableResult
  open func getThreadsInFolder(_ fid: ID, _ limit: Int32) -> YSArray<MessageMeta> {
    let messageRequestItem = MessageRequestItem.threads(fid, 0, limit)
    let request = MessagesRequestPack(YSArray(messageRequestItem), platform, NetworkExtra.mockExtra())
    return getMessagesList(request)
  }

  @discardableResult
  open func getMessagesInThread(_ tid: ID, _ limit: Int32) -> YSArray<MessageMeta> {
    let messageRequestItem = MessageRequestItem.messagesInThread(tid, 0, limit)
    let request = MessagesRequestPack(YSArray(messageRequestItem), platform, NetworkExtra.mockExtra())
    return getMessagesList(request)
  }

  @discardableResult
  open func getSettings() -> SettingsResponse {
    let request = SettingsRequest(platform, NetworkExtra.mockExtra())
    let response = getJsonResponse(request)
    return settingsResponseFromJSONItem(response)!
  }

  open func markMessageAsRead(_ mid: ID) {
    let request = MarkReadRequest(mid, nil, platform, NetworkExtra.mockExtra())
    executeRequest(request)
  }

  open func markMessageAsUnread(_ mid: ID) {
    let request = MarkUnreadRequest(mid, nil, platform, NetworkExtra.mockExtra())
    executeRequest(request)
  }

  open func markThreadAsRead(_ tid: ID) {
    let request = MarkReadRequest(nil, tid, platform, NetworkExtra.mockExtra())
    executeRequest(request)
  }

  open func markThreadAsUnread(_ tid: ID) {
    let request = MarkUnreadRequest(nil, tid, platform, NetworkExtra.mockExtra())
    executeRequest(request)
  }

  open func markMessageWithLabel(_ mid: ID, _ lid: LabelID) {
    let request = LabelMarkRequest(mid, nil, lid, platform, NetworkExtra.mockExtra())
    executeRequest(request)
  }

  open func unmarkMessageWithLabel(_ mid: ID, _ lid: LabelID) {
    let request = LabelUnmarkRequest(mid, nil, lid, platform, NetworkExtra.mockExtra())
    executeRequest(request)
  }

  open func markThreadWithLabel(_ tid: ID, _ lid: LabelID) {
    let request = LabelMarkRequest(nil, tid, lid, platform, NetworkExtra.mockExtra())
    executeRequest(request)
  }

  open func unmarkThreadWithLabel(_ tid: ID, _ lid: LabelID) {
    let request = LabelUnmarkRequest(nil, tid, lid, platform, NetworkExtra.mockExtra())
    executeRequest(request)
  }

  open func removeMessageByThreadId(_ fid: ID, _ tid: ID) {
    let request = DeleteRequest(nil, tid, fid, platform, NetworkExtra.mockExtra())
    executeRequest(request)
  }

  open func moveThreadToFolder(_ tid: ID, _ fid: ID) {
    let request = MoveToFolderRequest(nil, tid, fid, platform, NetworkExtra.mockExtra())
    executeRequest(request)
  }

  open func moveMessageToFolder(_ mid: ID, _ fid: ID) {
    let request = MoveToFolderRequest(mid, nil, fid, platform, NetworkExtra.mockExtra())
    executeRequest(request)
  }

  open func createFolder(_ name: String) {
    let request = CreateFolderRequest(name, nil, nil, platform, NetworkExtra.mockExtra())
    executeRequest(request)
  }

  open func sendMessage(_ to: String, _ subject: String, _ text: String, _ references: String! = nil) {
    let settings = getSettings()
    let composeCheck = settings.payload!.accountInformation.composeCheck
    let builder = SendRequestBuilder(platform, NetworkExtra.mockExtra()).to(to).composeCheck(composeCheck).subject(subject).send(text)
    if references != nil {
      builder.references(references)
    }
    getJsonResponse(builder.build())
  }

  @discardableResult
  open func getMessageReference(_ mid: ID) -> String {
    let request = MessageBodyRequest(platform, NetworkExtra.mockExtra(), YSArray(mid))
    let json = getJsonResponse(request)
    return (((json as! ArrayJSONItem).get(0) as! MapJSONItem).get("info") as! MapJSONItem).getString("ext_msg_id")!
  }

  open func setParameter(_ key: String, _ value: String) {
    let request = SetParametersRequest(key, value, platform, NetworkExtra.mockExtra())
    executeRequest(request)
  }

  open func moveToSpam(_ fid: ID, _ tid: ID) {
    let request = MoveToSpamRequest(nil, tid, fid, platform, NetworkExtra.mockExtra())
    executeRequest(request)
  }

  open func archive(_ local: String, _ tid: ID) {
    let request = ArchiveRequest(local, nil, tid, platform, NetworkExtra.mockExtra())
    executeRequest(request)
  }

  @discardableResult
  private func getMessagesList(_ request: NetworkRequest) -> YSArray<MessageMeta> {
    let response = getJsonResponse(request)
    let messageResponse = messageResponseFromJSONItem(response)!
    let messages: YSArray<MessageMeta> = YSArray()
    messageResponse.payload![0].items.forEach {
      message in
      messages.push(message)
    }
    return messages
  }

  @discardableResult
  private func getContactsList(_ request: NetworkRequest) -> YSArray<Contact> {
    let jsonMap = getJsonResponse(request) as! MapJSONItem
    let contacts: YSArray<Contact> = YSArray()
    let jsonContactArray = jsonMap.getMap("contacts")!.get("contact") as! ArrayJSONItem
    jsonContactArray.asArray().forEach {
      contactItem in
      let contact: Contact! = contactFromJSONItem(contactItem)
      if contact != nil {
        contacts.push(contact)
      }
    }
    return contacts
  }

  @discardableResult
  private func getJsonResponse(_ request: NetworkRequest) -> JSONItem {
    let jsonString = executeRequest(request)
    let response = jsonSerializer.deserialize(jsonString) {
      item in
      Result(item, nil)
    }
    return response.getValue()
  }

  @discardableResult
  private func executeRequest(_ request: NetworkRequest) -> String {
    return network.syncExecute(PublicBackendConfig.mailBaseUrl, request, oauthToken)
  }
}

open class MailboxClientHandler {
  public var clientsManager: AccountsManager
  public var mailboxClients: YSArray<MailboxClient>
  public init(_ mailboxClients: YSArray<MailboxClient>) {
    self.mailboxClients = mailboxClients
    clientsManager = AccountsManager(mailboxClients.map {
      client in
      client.userAccount
    })
  }

  open func loginToAccount(_ account: UserAccount) {
    clientsManager.logInToAccount(account)
  }

  open func switchToClientForAccountWithLogin(_ login: String) {
    clientsManager.switchToAccount(login)
  }

  @discardableResult
  open func getCurrentClient() -> MailboxClient {
    return mailboxClients[self.clientsManager.currentAccount!]
  }

  @discardableResult
  open func getLoggedInAccounts() -> YSArray<UserAccount> {
    return clientsManager.getLoggedInAccounts()
  }
}