import { Int32 } from '../../../ys/ys'
import { MovableToFolder } from '../mail-features'
import { FolderName, MessageId } from './mail-model'
import { MessageListDisplayModel } from './message-list-display-model'

export class MovableToFolderModel implements MovableToFolder {
  constructor(public model: MessageListDisplayModel) {
  }

  public moveMessageToFolder(order: Int32, folderName: FolderName): void {
    this.moveMessagesToFolder(new Set<Int32>([order]), folderName)
  }

  public moveMessagesToFolder(orders: Set<Int32>, folderName: FolderName): void {
    for (const mid of this.model.getMidsByOrders(orders)) {
      this.model.moveMessageToFolder(mid, folderName)
    }
  }
}
