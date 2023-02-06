import { ID } from '../client/common/id';
import { emailStringFromContact } from '../client/contact/contact';
import { FolderDTO, isFolderOfThreadedType } from '../client/folder/folderDTO';
import { MailboxClient } from '../client/mailbox-client';
import { AppModel, AppModelProvider } from '../mbt/walk/fixed-scenario-strategy';
import { Logger } from '../utils/logger';
import { display, valuesArray } from '../utils/utils';
import {
  AccountMailboxData, AccountSettings,
  DefaultFolderName,
  Folder,
  FolderId,
  FolderName,
  FullMessage,
  MailAppModelHandler,
  MailboxModel,
  MessageId,
} from './model/mail-model'

export class MailboxDownloader implements AppModelProvider {
  constructor(private clients: MailboxClient[], private logger: Logger) {
  }

  public takeAppModel(): AppModel {
    this.logger.log('Downloading mailbox started');
    const accountsData: AccountMailboxData[] = [];
    for (const client of this.clients) {
      this.logger.log(`Downloading account (${client.userAccount.login}) started`);
      const folderList = client.getFolderList().filter((folder) => folder.name !== DefaultFolderName.outgoing);
      const fidToFolder = new Map<FolderId, FolderDTO>();
      folderList.forEach((folder) => fidToFolder.set(folder.fid, folder));
      const messages = new Map<MessageId, FullMessage>();
      const messageToFolder = new Map<MessageId, Folder>();
      const threads = new Map<ID, Set<MessageId>>();
      const aliases = client.getSettings().payload!.accountInformation.emails.map((email) => display(email));
      const contacts = client.getAllContactsList(1000).map((contact) => emailStringFromContact(contact))
      const accountSettings = new AccountSettings(client.getSettings().payload!.settingsSetup.folderThreadView)
      for (const folder of folderList) { // Inbox only for speed
        const messagesDTO = isFolderOfThreadedType(folder.type) ?
          client.getThreadsInFolder(folder.fid, 10) : client.getMessagesInFolder(folder.fid, 10);
        messagesDTO.forEach((messageDTO) => {
          const messageModel = FullMessage.fromMeta(messageDTO);
          const tid = messageDTO.tid
          const threadSize = messageModel.head.threadCounter;
          if (tid !== null && threadSize !== null) {
            for (const threadMessageDTO of client.getMessagesInThread(tid, threadSize)) {
              const mid = threadMessageDTO.mid;
              if (!threads.has(tid)) {
                threads.set(tid, new Set());
              }
              threads.get(tid)!.add(mid);
              messages.set(mid, FullMessage.fromMeta(threadMessageDTO));
              messageToFolder.set(mid, Folder.fromDTO(fidToFolder.get(threadMessageDTO.fid)!));
            }
          } else {
            const mid = messageDTO.mid;
            messages.set(mid, messageModel);
            messageToFolder.set(mid, Folder.fromDTO(fidToFolder.get(messageDTO.fid)!));
          }
        });
      }

      const folderToMessages: Map<FolderName, Set<MessageId>> = new Map<FolderName, Set<MessageId>>();
      for (const folderDTO of folderList) {
        const folder = Folder.fromDTO(folderDTO);
        folderToMessages.set(folder.name, new Set<MessageId>());
      }

      messageToFolder.forEach((folder, msg) => {
        folderToMessages.get(folder.name)!.add(msg);
      });

      const accountData = new AccountMailboxData(
        client.userAccount,
        messages,
        folderToMessages,
        valuesArray(threads),
        aliases,
        contacts,
        accountSettings,
      );
      accountsData.push(accountData);
      this.logger.log(`Downloading account (${client.userAccount.login}) finished`);
    }

    this.logger.log('Downloading mailbox finished');
    this.logger.log('\n');

    const accountDataHandler = new MailAppModelHandler(accountsData);
    return new MailboxModel(accountDataHandler);
  }
}
