import { CreatableFolder } from '../mail-features';
import { FolderName, MessageId } from './mail-model';
import { MessageListDisplayModel } from './message-list-display-model';

export class CreatableFolderModel implements CreatableFolder {
  constructor(public model: MessageListDisplayModel) {
  }

  public createFolder(folderDisplayName: FolderName): void {
    this.model.accountDataHandler.getCurrentAccount().folderToMessages.set(folderDisplayName, new Set<MessageId>());
  }
}
