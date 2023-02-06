import { Int32, Nullable } from '../../ys/ys';
import { ArrayJSONItem, JSONItem, MapJSONItem } from '../mail/logging/json-types'
import { AccountsManager } from '../users/accounts-manager'
import { UserAccount } from '../users/user-pool';
import { Logger } from '../utils/logger';
import { Platform } from '../utils/platform';
import { ID, LabelID } from './common/id';
import { Contact, contactFromJSONItem } from './contact/contact';
import { ContainersRequest } from './container/containers-request';
import { FolderDTO, folderFromJSONItem } from './folder/folderDTO';
import { JSONSerializer } from './json/json-serializer';
import { Label, labelFromJSONItem } from './label/label';
import { MessageMeta } from './message/message-meta';
import { MessageRequestItem } from './message/message-request-item';
import { MessagesRequestPack } from './message/messages-request-pack';
import { messageResponseFromJSONItem } from './message/messages-response';
import { NetworkExtra } from './network/network-extra';
import { NetworkRequest } from './network/network-request';
import { SyncNetwork } from './network/sync-network';
import { PublicBackendConfig } from './public-backend-config';
import { ABookTopRequest } from './requests/abook-request';
import { ArchiveRequest } from './requests/archive-request';
import { DeleteRequest } from './requests/delete-request';
import { CreateFolderRequest, MoveToFolderRequest } from './requests/folder-request';
import { MoveToSpamRequest } from './requests/foo-request';
import { LabelMarkRequest, LabelUnmarkRequest } from './requests/label-request';
import { MarkReadRequest, MarkUnreadRequest } from './requests/mark-request';
import { MessageBodyRequest } from './requests/message-body-request';
import { SendRequestBuilder } from './requests/send-request';
import { SetParametersRequest } from './requests/set-parameters';
import { Result } from './result';
import { SettingsResponse, settingsResponseFromJSONItem } from './settings/settings-entities';
import { SettingsRequest } from './settings/settings-request';

export class MailboxClient {

  constructor(
    private platform: Platform,
    public readonly userAccount: UserAccount,
    private oauthToken: string,
    private network: SyncNetwork,
    private jsonSerializer: JSONSerializer,
    public logger: Logger,
  ) { }

  public getFolderList(): FolderDTO[] {
    const request = new ContainersRequest(this.platform, NetworkExtra.mockExtra());
    const jsonArray = this.getJsonResponse(request) as ArrayJSONItem;
    const folders: FolderDTO[] = [];
    jsonArray.asArray().forEach((folderItem) => {
      const fid = (folderItem as MapJSONItem).get('fid');
      if (fid !== null) {
        folders.push(folderFromJSONItem(folderItem)!)
      }
    });
    return folders;
  }

  public getLabelList(): Label[] {
    const request = new ContainersRequest(this.platform, NetworkExtra.mockExtra());
    const jsonArray = this.getJsonResponse(request) as ArrayJSONItem;
    const labels: Label[] = [];
    jsonArray.asArray().forEach((labelItem) => {
      const lid = (labelItem as MapJSONItem).get('lid');
      if (lid !== null) {
        labels.push(labelFromJSONItem(labelItem)!)
      }
    });
    return labels;
  }

  public getAllContactsList(limit: Int32): Contact[] {
    const request = new ABookTopRequest(limit, this.platform, NetworkExtra.mockExtra());
    return this.getContactsList(request);
  }

  public getMessagesInFolder(fid: ID, limit: Int32): MessageMeta[] {
    const messageRequestItem = MessageRequestItem.messagesInFolder(fid, 0, limit);
    const request = new MessagesRequestPack([messageRequestItem], this.platform, NetworkExtra.mockExtra());
    return this.getMessagesList(request);
  }

  public getThreadsInFolder(fid: ID, limit: Int32): MessageMeta[] {
    const messageRequestItem = MessageRequestItem.threads(fid, 0, limit);
    const request = new MessagesRequestPack([messageRequestItem], this.platform, NetworkExtra.mockExtra());
    return this.getMessagesList(request);
  }

  public getMessagesInThread(tid: ID, limit: Int32): MessageMeta[] {
    const messageRequestItem = MessageRequestItem.messagesInThread(tid, 0, limit);
    const request = new MessagesRequestPack([messageRequestItem], this.platform, NetworkExtra.mockExtra());
    return this.getMessagesList(request)
  }

  public getSettings(): SettingsResponse {
    const request = new SettingsRequest(this.platform, NetworkExtra.mockExtra());
    const response = this.getJsonResponse(request);
    return settingsResponseFromJSONItem(response)!;
  }

  public markMessageAsRead(mid: ID): void {
    const request = new MarkReadRequest(mid, null, this.platform, NetworkExtra.mockExtra());
    this.executeRequest(request)
  }

  public markMessageAsUnread(mid: ID): void {
    const request = new MarkUnreadRequest(mid, null, this.platform, NetworkExtra.mockExtra());
    this.executeRequest(request)
  }

  public markThreadAsRead(tid: ID): void {
    const request = new MarkReadRequest(null, tid, this.platform, NetworkExtra.mockExtra());
    this.executeRequest(request)
  }

  public markThreadAsUnread(tid: ID): void {
    const request = new MarkUnreadRequest(null, tid, this.platform, NetworkExtra.mockExtra());
    this.executeRequest(request)
  }

  public markMessageWithLabel(mid: ID, lid: LabelID): void {
    const request = new LabelMarkRequest(mid, null, lid, this.platform, NetworkExtra.mockExtra());
    this.executeRequest(request)
  }

  public unmarkMessageWithLabel(mid: ID, lid: LabelID): void {
    const request = new LabelUnmarkRequest(mid, null, lid, this.platform, NetworkExtra.mockExtra());
    this.executeRequest(request)
  }

  public markThreadWithLabel(tid: ID, lid: LabelID): void {
    const request = new LabelMarkRequest(null, tid, lid, this.platform, NetworkExtra.mockExtra());
    this.executeRequest(request)
  }

  public unmarkThreadWithLabel(tid: ID, lid: LabelID): void {
    const request = new LabelUnmarkRequest(null, tid, lid, this.platform, NetworkExtra.mockExtra());
    this.executeRequest(request)
  }

  public removeMessageByThreadId(fid: ID, tid: ID): void {
    const request = new DeleteRequest(null, tid, fid, this.platform, NetworkExtra.mockExtra());
    this.executeRequest(request)
  }

  public moveThreadToFolder(tid: ID, fid: ID): void {
    const request = new MoveToFolderRequest(null, tid, fid, this.platform, NetworkExtra.mockExtra());
    this.executeRequest(request)
  }

  public moveMessageToFolder(mid: ID, fid: ID): void {
    const request = new MoveToFolderRequest(mid, null, fid, this.platform, NetworkExtra.mockExtra());
    this.executeRequest(request)
  }

  public createFolder(name: string): void {
    const request = new CreateFolderRequest(name, null, null, this.platform, NetworkExtra.mockExtra());
    this.executeRequest(request)
  }

  public sendMessage(to: string, subject: string, text: string, references: Nullable<string> = null): void {
    const settings = this.getSettings();
    const composeCheck = settings.payload!.accountInformation.composeCheck
    const builder = new SendRequestBuilder(this.platform, NetworkExtra.mockExtra())
      .to(to)
      .composeCheck(composeCheck)
      .subject(subject)
      .send(text);
    if (references !== null) {
      builder.references(references);
    }
    this.getJsonResponse(builder.build());
  }

  public getMessageReference(mid: ID): string {
    const request = new MessageBodyRequest(this.platform, NetworkExtra.mockExtra(), [mid]);
    const json = this.getJsonResponse(request);
    // TODO parse response
    return (((json as ArrayJSONItem).get(0) as MapJSONItem).get('info') as MapJSONItem).getString('ext_msg_id')!
  }

  public setParameter(key: string, value: string): void {
    const request = new SetParametersRequest(key, value, this.platform, NetworkExtra.mockExtra());
    this.executeRequest(request);
  }

  public moveToSpam(fid: ID, tid: ID): void {
    const request = new MoveToSpamRequest(null, tid, fid, this.platform, NetworkExtra.mockExtra())
    this.executeRequest(request)
  }

  public archive(local: string, tid: ID): void {
    const request = new ArchiveRequest(local, null, tid, this.platform, NetworkExtra.mockExtra())
    this.executeRequest(request)
  }

  private getMessagesList(request: NetworkRequest): MessageMeta[] {
    const response = this.getJsonResponse(request);
    const messageResponse = messageResponseFromJSONItem(response)!;
    const messages: MessageMeta[] = [];
    messageResponse.payload![0].items.forEach((message) => {
      messages.push(message)
    });
    // messages.sort((m1, m2) => int64ToInt32(m2.timestamp - m1.timestamp));
    return messages
  }

  private getContactsList(request: NetworkRequest): Contact[] {
    const jsonMap = this.getJsonResponse(request) as MapJSONItem;
    const contacts: Contact[] = [];
    const jsonContactArray = jsonMap.getMap('contacts')!.get('contact') as ArrayJSONItem
    jsonContactArray.asArray().forEach((contactItem) => {
      const contact = contactFromJSONItem(contactItem);
      if (contact !== null) {
        contacts.push(contact);
      }
    });
    return contacts;
  }

  private getJsonResponse(request: NetworkRequest): JSONItem {
    const jsonString = this.executeRequest(request);
    const response = this.jsonSerializer.deserialize(jsonString, (item) => new Result(item, null));
    return response.getValue();
  }

  private executeRequest(request: NetworkRequest): string {
    return this.network.syncExecute(PublicBackendConfig.mailBaseUrl, request, this.oauthToken);
  }
}

export class MailboxClientHandler {
  public clientsManager: AccountsManager;

  constructor(public mailboxClients: MailboxClient[]) {
    this.clientsManager = new AccountsManager(mailboxClients.map((client) => client.userAccount))
  }

  public loginToAccount(account: UserAccount): void {
    this.clientsManager.logInToAccount(account)
  }

  public switchToClientForAccountWithLogin(login: string): void {
    this.clientsManager.switchToAccount(login)
  }

  public getCurrentClient(): MailboxClient {
    return this.mailboxClients[this.clientsManager.currentAccount!]
  }

  public getLoggedInAccounts(): UserAccount[] {
    return this.clientsManager.getLoggedInAccounts()
  }
}
