import { Int32, Nullable, range } from '../../ys/ys'
import { JSONSerializer } from '../client/json/json-serializer';
import {
  JsonRequestEncoding,
  NetworkAPIVersions,
  NetworkMethod,
  NetworkRequest,
  RequestEncoding,
  UrlRequestEncoding,
} from '../client/network/network-request';
import { SyncNetwork } from '../client/network/sync-network';
import { PublicBackendConfig } from '../client/public-backend-config';
import { Result } from '../client/result';
import { UserAccount } from '../users/user-pool';
import { Logger } from '../utils/logger';
import { SyncSleep } from '../utils/sync-sleep'
import { requireNonNull } from '../utils/utils';
import { ArrayJSONItem, BooleanJSONItem, JSONItem, MapJSONItem, StringJSONItem } from './logging/json-types'
import { MessageTimeProvider } from './message-time-provider'
import { DefaultFolderName, FolderName } from './model/mail-model'

export class ImapFolder {
  constructor(public name: string, public messages: ImapMessage[]) {
  }

  public toJson(): JSONItem {
    return new MapJSONItem()
      .putString('name', this.name)
      .put('messages', new ArrayJSONItem(this.messages.map((m) => m.toJson())));
  }
}

export class ImapUser {
  constructor(private email: string, private name: string) {
  }

  public toJson(): JSONItem {
    return new MapJSONItem()
      .putString('email', this.email)
      .putString('name', this.name)
  }
}

export class ImapMessage {
  private sender: ImapUser;
  private subject: string;
  private textBody: string;
  private timestamp: string;
  private toReceivers: ImapUser[];

  constructor(builder: ImapMessageBuilder) {
    this.sender = requireNonNull(builder.sender, 'Sender required!');
    this.subject = requireNonNull(builder.subject, 'Subject required!');
    this.textBody = requireNonNull(builder.textBody, 'Body text required!');
    this.timestamp = requireNonNull(builder.timestamp, 'Timestamp required!');
    this.toReceivers = builder.toReceivers;
  }

  public static builder(): ImapMessageBuilder {
    return new ImapMessageBuilder();
  }

  public static create(subject: string, timestamp: Nullable<string> = null): ImapMessage {
    return this.builder()
      .withSender(new ImapUser('other.user@ya.ru', 'Other User'))
      .withSubject(subject)
      .withTextBody('first line')
      .withTimestamp(timestamp !== null ? timestamp : `2019-07-20T17:03:06.000Z`)
      .build()
  }

  public toJson(): JSONItem {
    return new MapJSONItem()
      .put('sender', this.sender.toJson())
      .putString('subject', this.subject)
      .putString('textBody', this.textBody)
      .putString('timestamp', this.timestamp)
      .put('toReceivers', new ArrayJSONItem(this.toReceivers.map((r) => r.toJson())))
  }
}

export class ImapMessageBuilder {
  public sender: Nullable<ImapUser> = null;
  public subject: Nullable<string> = null;
  public textBody: Nullable<string> = null;
  public timestamp: Nullable<string> = null;
  public toReceivers: ImapUser[] = [];

  public withSender(sender: ImapUser): ImapMessageBuilder {
    this.sender = sender;
    return this;
  }

  public withSubject(subject: string): ImapMessageBuilder {
    this.subject = subject;
    return this;
  }

  public withTextBody(textBody: string): ImapMessageBuilder {
    this.textBody = textBody;
    return this;
  }

  public withTimestamp(timestamp: string): ImapMessageBuilder {
    this.timestamp = timestamp;
    return this;
  }

  public addReceiver(receiver: ImapUser): ImapMessageBuilder {
    this.toReceivers.push(receiver);
    return this;
  }

  public build(): ImapMessage {
    return new ImapMessage(this);
  }
}

export class ImapMailAccount {
  constructor(
    private host: string,
    public login: string,
    public readonly password: string) {
  }

  public toJson(): JSONItem {
    return new MapJSONItem()
      .putString('host', this.host)
      .putString('login', this.login)
      .putString('password', this.password)
  }
}

export class ImapMailbox {
  public mailAccount: ImapMailAccount;
  public folders: ImapFolder[] = [];

  constructor(builder: ImapMailboxBuilder) {
    this.mailAccount = builder.mailAccount;
    builder.folders.forEach((messages, name) => {
      this.folders.push(new ImapFolder(name, messages));
    });
  }

  public static builder(account: UserAccount): ImapMailboxBuilder {
    return new ImapMailboxBuilder(account.login, account.password);
  }

  public toJson(): MapJSONItem {
    return new MapJSONItem()
      .put('folders', new ArrayJSONItem(this.folders.map((f) => f.toJson())))
      .put('mailAccount', this.mailAccount.toJson())

  }
}

export class ImapMailboxBuilder {
  public readonly mailAccount: ImapMailAccount
  public readonly folders: Map<string, ImapMessage[]> = new Map<string, ImapMessage[]>()
  private readonly timestampProvider: MessageTimeProvider = new MessageTimeProvider()
  private currentFolder: FolderName = DefaultFolderName.inbox

  constructor(login: string, password: string) {
    this.mailAccount = new ImapMailAccount('imap.yandex.ru', login, password)
  }

  public nextMessage(subject: string): ImapMailboxBuilder {
    const timestamp = this.timestampProvider.nextTime()
    this.addMessageToFolder(this.currentFolder, ImapMessage.create(subject, timestamp))
    return this
  }

  public nextThread(subject: string, threadSize: Int32): ImapMailboxBuilder {
    const timestamp = this.timestampProvider.nextTime()
    for (const i of range(0, threadSize)) {
      this.addMessageToFolder(this.currentFolder, ImapMessage.create(subject, timestamp))
    }
    return this
  }

  public switchFolder(folder: FolderName): ImapMailboxBuilder {
    this.currentFolder = folder
    return this
  }

  public addMessageToFolder(folder: FolderName, message: ImapMessage): ImapMailboxBuilder {
    if (!this.folders.has(folder)) {
      this.folders.set(folder, [])
    }
    const folderMessage = this.folders.get(folder)!
    folderMessage.push(message)
    return this
  }

  public build(): ImapMailbox {
    return new ImapMailbox(this)
  }
}

export abstract class BasePrepareMailboxRequest implements NetworkRequest {
  constructor(private mailbox: ImapMailbox) {
  }

  public encoding(): RequestEncoding {
    return new JsonRequestEncoding();
  }

  public method(): NetworkMethod {
    return NetworkMethod.post;
  }

  public params(): MapJSONItem {
    return this.mailbox.toJson();
  }

  public urlExtra(): MapJSONItem {
    return new MapJSONItem();
  }

  public version(): NetworkAPIVersions {
    return NetworkAPIVersions.unspecified;
  }

  public abstract path(): string
}

export class SyncPrepareMailboxRequest extends BasePrepareMailboxRequest {
  constructor(mailbox: ImapMailbox) {
    super(mailbox);
  }

  public path(): string {
    return 'sync';
  }
}

export class AsyncPrepareMailboxRequest extends BasePrepareMailboxRequest {
  constructor(mailbox: ImapMailbox) {
    super(mailbox)
  }

  public path(): string {
    return 'async';
  }
}

export class PrepareMailboxStatusRequest implements NetworkRequest {
  constructor(private id: string) {
  }

  public encoding(): RequestEncoding {
    return new UrlRequestEncoding()
  }

  public method(): NetworkMethod {
    return NetworkMethod.get
  }

  public params(): MapJSONItem {
    return new MapJSONItem();
  }

  public urlExtra(): MapJSONItem {
    return new MapJSONItem();
  }

  public version(): NetworkAPIVersions {
    return NetworkAPIVersions.unspecified;
  }

  public path(): string {
    return `async/${this.id}`;
  }
}

export class MailboxPreparer {
  public constructor(private network: SyncNetwork,
                     private jsonSerializer: JSONSerializer,
                     private sleep: SyncSleep,
                     private logger: Logger) {
  }

  public prepare(mailbox: ImapMailbox): void {
    this.logger.log(`Preparing mailbox:`)
    for (const folder of mailbox.folders) {
      for (const message of folder.messages) {
        this.logger.log(`${message.toJson()}`)
      }
    }
    const ticket = this.startPreparing(mailbox)
    this.logger.log(`Mailbox preparing started, id=${ticket}`)
    while (true) {
      const done = this.finished(ticket)
      if (done) {
        this.logger.log('Mailbox prepared')
        return
      }
      this.sleep.sleepMs(3000)
    }
  }

  private syncRequest(networkRequest: NetworkRequest): MapJSONItem {
    const response = this.network.syncExecute(PublicBackendConfig.mailboxPreparerUrl, networkRequest, null)
    const json = this.jsonSerializer.deserialize(response, (item) => new Result(item, null)).getValue()
    return json as MapJSONItem
  }

  private startPreparing(mailbox: ImapMailbox): string {
    const preparingStartedResponse = this.syncRequest(new AsyncPrepareMailboxRequest(mailbox))
    return requireNonNull(preparingStartedResponse.getString('id'), 'No "id" field in response!')
  }

  private finished(ticket: string): boolean {
    const statusResponse = this.syncRequest(new PrepareMailboxStatusRequest(ticket))
    const status = requireNonNull(statusResponse.getMap('status'), 'No "status" field in response!')
    const message = (status.get('message') as StringJSONItem).value
    this.logger.log(`Mailbox preparing status: ${message}`)
    return (status.get('done') as BooleanJSONItem).value
  }
}
