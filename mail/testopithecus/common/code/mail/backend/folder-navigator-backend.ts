import { MailboxClientHandler } from '../../client/mailbox-client';
import { FolderNavigator } from '../mail-features';
import { Folder, toBackendFolderName } from '../model/mail-model';
import { MessageListDisplayBackend } from './message-list-display-backend';

export class FolderNavigatorBackend implements FolderNavigator {

  constructor(private mailListDisplayBackend: MessageListDisplayBackend, private clientsHandler: MailboxClientHandler) {
  }

  public getFoldersList(): Folder[] {
    return this.clientsHandler.getCurrentClient().getFolderList().map((meta) => Folder.fromDTO(meta));
  }

  public goToFolder(folderDisplayName: string): void {
    const folderBackendName = toBackendFolderName(folderDisplayName)
    const folder = this.clientsHandler.getCurrentClient().getFolderList().filter((f) => f.name === folderBackendName)[0]
    this.mailListDisplayBackend.setCurrentFolderId(folder.fid)
  }

  public getCurrentFolder(): Folder {
    const currentFolderName = this.clientsHandler.getCurrentClient().getFolderList()
      .filter((meta) => meta.fid === this.mailListDisplayBackend.getCurrentFolderId())[0].name!
    return new Folder(currentFolderName);
  }
}
