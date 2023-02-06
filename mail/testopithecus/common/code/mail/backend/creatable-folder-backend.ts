import { MailboxClientHandler } from '../../client/mailbox-client';
import { CreatableFolder } from '../mail-features';

export class CreatableFolderBackend implements CreatableFolder {
  constructor(private clientsHandler: MailboxClientHandler) {
  }

  public createFolder(folderDisplayName: string): void {
    this.clientsHandler.getCurrentClient().createFolder(folderDisplayName);
  }
}
