import { FolderNavigator } from '../mail-features';
import { Folder } from './mail-model';
import { MessageListDisplayModel } from './message-list-display-model';

export class FolderNavigatorModel implements FolderNavigator {

  constructor(public model: MessageListDisplayModel) {
  }

  public getFoldersList(): Folder[] {
    const folders: Folder[] = [];
    this.model.accountDataHandler.getCurrentAccount().folderToMessages
      .forEach((msgIds, folder) => folders.push(new Folder(folder)));
    return folders;
  }

  public goToFolder(folderDisplayName: string): void {
    this.model.setCurrentFolder(new Folder(folderDisplayName));
  }

  public getCurrentFolder(): Folder {
    return this.model.getCurrentFolder()
  }
}
